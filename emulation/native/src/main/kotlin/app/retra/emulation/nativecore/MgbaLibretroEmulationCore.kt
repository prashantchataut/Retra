package app.retra.emulation.nativecore

import android.content.Context
import android.net.Uri
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.emulation.InputSnapshot
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.emulation.SaveKind
import app.retra.core.emulation.SessionCommand
import app.retra.core.emulation.SessionPhase
import app.retra.core.emulation.SessionReducer
import app.retra.core.emulation.SessionSnapshot
import app.retra.core.model.PerformanceProfile
import app.retra.emulation.api.ActiveCheat
import app.retra.emulation.api.AudioPacket
import app.retra.emulation.api.CoreDescriptor
import app.retra.emulation.api.CoreTier
import app.retra.emulation.api.EmulationCore
import app.retra.emulation.api.EmulatorInputState
import app.retra.emulation.api.GameFile
import app.retra.emulation.api.LoadGameResult
import app.retra.emulation.api.RuntimeMetrics
import app.retra.emulation.api.SaveSlot
import app.retra.emulation.api.SaveStateMetadata
import app.retra.emulation.api.VideoFrame
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gameplay-capable adapter for a separately bundled, reviewed mGBA 0.10.5 libretro shared library.
 * The adapter remains unavailable unless libmgba_libretro.so is actually packaged and exposes the
 * complete required ABI. It never downloads or executes native code at runtime.
 */
class MgbaLibretroEmulationCore(context: Context) : EmulationCore, AutoCloseable {
    override val descriptor = CoreDescriptor(
        id = "mgba-libretro",
        displayName = "mGBA",
        version = "0.10.5",
        tier = CoreTier.GBA_GAMEPLAY,
        supportsBatterySaves = true,
        supportsSaveStates = true,
        supportsAudio = true,
        supportsCheats = true,
        supportsRewind = false,
        legalNotice = "mGBA is distributed under MPL-2.0. Retra requires the corresponding bundled source notices and source offer."
    )

    private val applicationContext = context.applicationContext
    private val saveStore = AtomicSaveStore(File(applicationContext.filesDir, "emulation"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val nativeLock = Any()
    private var nativeHandle: Long = MgbaBridge.nativeCreate()
    private var frameJob: Job? = null
    private var gameHash: String? = null
    private var input = InputSnapshot()
    private var speedMultiplier = 1f
    private var profile = PerformanceProfile.BALANCED
    private var sequence = 0L
    private var lastBatteryFlushMillis = 0L
    private val closed = AtomicBoolean(false)

    override val isAvailable: Boolean get() = nativeHandle != 0L && !closed.get()
    override val unavailableReason: String?
        get() = if (isAvailable) null else "A reviewed libmgba_libretro.so is not bundled for this device ABI."

    private val mutableSession = MutableStateFlow(SessionSnapshot())
    override val session: StateFlow<SessionSnapshot> = mutableSession
    private val mutableFrame = MutableStateFlow<VideoFrame?>(null)
    override val latestFrame: StateFlow<VideoFrame?> = mutableFrame
    private val mutableAudio = MutableSharedFlow<AudioPacket>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val audioPackets = mutableAudio
    private val mutableMetrics = MutableStateFlow(RuntimeMetrics(0f, 0f, 100f, 0, 0))
    override val metrics: StateFlow<RuntimeMetrics> = mutableMetrics

    override suspend fun loadGame(game: GameFile): LoadGameResult {
        if (!isAvailable) return LoadGameResult.Failed(unavailableReason ?: "mGBA is unavailable.")
        frameJob?.cancel()
        frameJob = null
        mutableFrame.value = null
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.BeginLoad(game.sha256))
        return runCatching {
            if (gameHash != null) saveBattery().getOrThrow()
            gameHash = null
            val normalizedHash = game.sha256.lowercase()
            val rom = readAndVerifyRom(Uri.parse(game.uri), normalizedHash)
            check(synchronized(nativeLock) { MgbaBridge.nativeLoadRom(nativeHandle, rom) }) {
                "mGBA rejected the selected ROM."
            }
            restoreBatteryOrThrow(normalizedHash)
            gameHash = normalizedHash
            val restoredSuspend = restoreSuspendIfCompatible(normalizedHash)
            mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.LoadSucceeded)
            LoadGameResult.Loaded(restoredSuspendState = restoredSuspend)
        }.getOrElse { error ->
            gameHash = null
            mutableSession.value = SessionReducer.reduce(
                mutableSession.value,
                SessionCommand.LoadFailed(error.message ?: "Unable to load the selected ROM with mGBA.")
            )
            LoadGameResult.Failed(error.message ?: "Unable to load the selected ROM with mGBA.")
        }
    }

    override fun start() {
        if (mutableSession.value.phase !in setOf(SessionPhase.READY, SessionPhase.PAUSED, SessionPhase.SUSPENDED)) return
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Start)
        ensureFrameLoop()
    }

    override fun pause() {
        if (mutableSession.value.phase == SessionPhase.RUNNING) runCatching { saveBattery().getOrThrow() }
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Pause)
    }

    override fun resume() {
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Resume)
        ensureFrameLoop()
    }

    override fun suspendSession() {
        val failure = runCatching {
            saveBattery().getOrThrow()
            if (mutableSession.value.phase == SessionPhase.RUNNING || mutableSession.value.phase == SessionPhase.PAUSED) {
                saveInternal(SaveKind.SUSPEND, -1)
            }
        }.exceptionOrNull()
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Suspend)
            .copy(errorMessage = failure?.message)
    }

    override fun reset() {
        if (!isAvailable) return
        synchronized(nativeLock) { MgbaBridge.nativeReset(nativeHandle) }
        sequence = 0
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Reset)
        start()
    }

    override fun stop() {
        runCatching { saveBattery().getOrThrow() }
        frameJob?.cancel()
        frameJob = null
        mutableFrame.value = null
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Stop)
    }

    override fun setInputState(input: EmulatorInputState) {
        this.input = InputSnapshot.from(input.buttons)
    }

    override fun setEmulationSpeed(multiplier: Float) {
        speedMultiplier = multiplier.coerceIn(0.25f, 16f)
    }

    override fun setPerformanceProfile(profile: PerformanceProfile) {
        this.profile = profile
    }

    override fun saveBattery(): Result<Unit> = runCatching {
        val hash = requireNotNull(gameHash) { "No game is loaded." }
        val payload = synchronized(nativeLock) { MgbaBridge.nativeBatterySave(nativeHandle) }
        if (payload.isEmpty()) return@runCatching
        val envelope = SaveEnvelope(
            kind = SaveKind.BATTERY,
            gameSha256 = hash,
            coreId = descriptor.id,
            coreVersion = descriptor.version,
            slot = -1,
            createdAtEpochMillis = System.currentTimeMillis(),
            payload = payload
        )
        saveStore.write(statePath(hash, SaveKind.BATTERY, -1), envelope.encode())
        lastBatteryFlushMillis = System.currentTimeMillis()
    }

    override fun saveState(slot: SaveSlot): Result<SaveStateMetadata> = runCatching {
        require(slot.number in 0..99) { "Save-state slot must be between 0 and 99." }
        saveInternal(SaveKind.STATE, slot.number)
        SaveStateMetadata(slot, System.currentTimeMillis(), coreVersion = descriptor.version)
    }

    override fun loadState(slot: SaveSlot): Result<Unit> = runCatching {
        require(slot.number in 0..99) { "Save-state slot must be between 0 and 99." }
        val hash = requireNotNull(gameHash) { "No game is loaded." }
        val bytes = requireNotNull(saveStore.read(statePath(hash, SaveKind.STATE, slot.number))) {
            "Save slot ${slot.number} is empty."
        }
        val envelope = SaveEnvelope.decode(bytes)
        requireCompatible(envelope, hash, SaveKind.STATE, slot.number)
        check(synchronized(nativeLock) { MgbaBridge.nativeDeserialize(nativeHandle, envelope.payload) }) {
            "mGBA rejected the save-state payload."
        }
        mutableSession.value = mutableSession.value.copy(phase = SessionPhase.PAUSED, errorMessage = null)
    }

    override fun applyCheats(cheats: List<ActiveCheat>): Result<Unit> = runCatching {
        require(cheats.isNotEmpty()) { "Select at least one cheat." }
        require(cheats.size <= 512) { "Too many active cheats." }
        requireNotNull(gameHash) { "No game is loaded." }
        saveBattery().getOrThrow()
        saveInternal(SaveKind.STATE, PRE_CHEAT_BACKUP_SLOT)
        val codes = cheats.map { active ->
            require(active.id.isNotBlank() && active.id.length <= 64) { "Invalid cheat identifier." }
            require(active.code.isNotBlank() && active.code.length <= 8192) { "Invalid cheat code payload." }
            active.code
        }.toTypedArray()
        check(synchronized(nativeLock) { MgbaBridge.nativeSetCheats(nativeHandle, codes) }) {
            "mGBA rejected one or more cheat codes. The pre-cheat backup remains in Vault slot $PRE_CHEAT_BACKUP_SLOT."
        }
    }

    override fun clearCheats(): Result<Unit> = runCatching {
        synchronized(nativeLock) { MgbaBridge.nativeClearCheats(nativeHandle) }
    }

    private fun restoreBatteryOrThrow(hash: String) {
        val encoded = saveStore.read(statePath(hash, SaveKind.BATTERY, -1)) ?: return
        val envelope = SaveEnvelope.decode(encoded)
        requireCompatible(envelope, hash, SaveKind.BATTERY, -1)
        check(synchronized(nativeLock) { MgbaBridge.nativeRestoreBattery(nativeHandle, envelope.payload) }) {
            "The stored battery save size is incompatible with this ROM/core combination."
        }
    }

    private fun restoreSuspendIfCompatible(hash: String): Boolean {
        val bytes = saveStore.read(statePath(hash, SaveKind.SUSPEND, -1)) ?: return false
        return runCatching {
            val envelope = SaveEnvelope.decode(bytes)
            requireCompatible(envelope, hash, SaveKind.SUSPEND, -1)
            check(synchronized(nativeLock) { MgbaBridge.nativeDeserialize(nativeHandle, envelope.payload) })
            true
        }.getOrDefault(false)
    }

    private fun saveInternal(kind: SaveKind, slot: Int) {
        val hash = requireNotNull(gameHash) { "No game is loaded." }
        val state = synchronized(nativeLock) { MgbaBridge.nativeSerialize(nativeHandle) }
        check(state.isNotEmpty()) { "mGBA did not produce a save-state payload." }
        val envelope = SaveEnvelope(kind, hash, descriptor.id, descriptor.version, slot, System.currentTimeMillis(), state)
        saveStore.write(statePath(hash, kind, slot), envelope.encode())
    }

    private fun requireCompatible(envelope: SaveEnvelope, hash: String, kind: SaveKind, slot: Int) {
        check(envelope.gameSha256.equals(hash, ignoreCase = true)) { "Save belongs to a different ROM." }
        check(envelope.kind == kind && envelope.slot == slot) { "Save metadata does not match the requested slot." }
        check(envelope.coreId == descriptor.id) { "Save was created by a different emulator core." }
        check(envelope.coreVersion == descriptor.version) { "Save was created by an incompatible mGBA version." }
    }

    private fun statePath(hash: String, kind: SaveKind, slot: Int): String = when (kind) {
        SaveKind.STATE -> "$hash/states/slot-$slot.rsv"
        SaveKind.SUSPEND -> "$hash/suspend/latest.rsv"
        SaveKind.BATTERY -> "$hash/battery/game.rsv"
    }

    private fun ensureFrameLoop() {
        if (frameJob?.isActive == true || !isAvailable) return
        frameJob = scope.launch {
            var framesInWindow = 0
            var windowStartNanos = System.nanoTime()
            while (isActive && !closed.get()) {
                if (mutableSession.value.phase != SessionPhase.RUNNING) {
                    delay(12)
                    continue
                }
                val start = System.nanoTime()
                val nativeResult = synchronized(nativeLock) {
                    val pixels = MgbaBridge.nativeStep(nativeHandle, input.mask, speedMultiplier)
                    val audio = MgbaBridge.nativeDrainAudio(nativeHandle)
                    val width = MgbaBridge.nativeWidth(nativeHandle)
                    val height = MgbaBridge.nativeHeight(nativeHandle)
                    val sampleRate = MgbaBridge.nativeSampleRate(nativeHandle)
                    NativeFrameResult(pixels, audio, width, height, sampleRate)
                }
                if (nativeResult.width > 0 && nativeResult.height > 0 &&
                    nativeResult.pixels.size == nativeResult.width * nativeResult.height
                ) {
                    sequence += 1
                    mutableFrame.value = VideoFrame(
                        nativeResult.width,
                        nativeResult.height,
                        nativeResult.pixels,
                        sequence,
                        System.nanoTime()
                    )
                    if (nativeResult.audio.isNotEmpty() && nativeResult.sampleRate in 8_000..192_000) {
                        mutableAudio.tryEmit(AudioPacket(nativeResult.sampleRate, 2, nativeResult.audio, sequence))
                    }
                    framesInWindow += 1
                }
                val nowMillis = System.currentTimeMillis()
                if (nowMillis - lastBatteryFlushMillis >= BATTERY_FLUSH_INTERVAL_MILLIS) {
                    runCatching { saveBattery().getOrThrow() }
                }
                val nowNanos = System.nanoTime()
                if (nowNanos - windowStartNanos >= 1_000_000_000L) {
                    val seconds = (nowNanos - windowStartNanos) / 1_000_000_000f
                    val presented = framesInWindow / max(seconds, 0.001f)
                    mutableMetrics.value = RuntimeMetrics(
                        emulatedFps = presented * speedMultiplier,
                        presentedFps = presented,
                        speedPercent = speedMultiplier * 100f,
                        droppedFrames = 0,
                        audioUnderruns = 0,
                        frameTimeMillis = (nowNanos - start) / 1_000_000f
                    )
                    framesInWindow = 0
                    windowStartNanos = nowNanos
                }
                val frameBudget = when (profile) {
                    PerformanceProfile.BATTERY_SAVER -> 20_000_000L
                    else -> 16_666_667L
                }
                val remainingNanos = frameBudget - (System.nanoTime() - start)
                if (remainingNanos > 0) delay(remainingNanos / 1_000_000L)
            }
        }
    }

    private fun readAndVerifyRom(uri: Uri, expectedHash: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val output = ByteArrayOutputStream()
        applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Android could not open the selected ROM." }
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = inputStream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_ROM_BYTES) { "ROM exceeds Retra's 64 MiB safety limit." }
                digest.update(buffer, 0, read)
                output.write(buffer, 0, read)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        require(actualHash.equals(expectedHash, ignoreCase = true)) { "ROM hash changed since import." }
        return output.toByteArray()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { saveBattery().getOrThrow() }
        frameJob?.cancel()
        scope.cancel()
        synchronized(nativeLock) {
            if (nativeHandle != 0L) MgbaBridge.nativeDestroy(nativeHandle)
            nativeHandle = 0L
        }
    }

    private data class NativeFrameResult(
        val pixels: IntArray,
        val audio: ShortArray,
        val width: Int,
        val height: Int,
        val sampleRate: Int
    )

    private companion object {
        const val MAX_ROM_BYTES = 64 * 1024 * 1024
        const val BATTERY_FLUSH_INTERVAL_MILLIS = 30_000L
        const val PRE_CHEAT_BACKUP_SLOT = 99
    }
}
