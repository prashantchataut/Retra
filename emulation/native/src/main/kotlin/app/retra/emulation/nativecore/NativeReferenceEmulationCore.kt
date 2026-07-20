package app.retra.emulation.nativecore

import android.content.Context
import android.net.Uri
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.emulation.InputSnapshot
import app.retra.core.emulation.RewindBuffer
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
 * A real JNI execution path used to verify Retra's native lifecycle, input, video-frame,
 * and state-storage pipeline. It intentionally does not claim to execute GBA instructions.
 */
class NativeReferenceEmulationCore(context: Context) : EmulationCore, AutoCloseable {
    override val descriptor = CoreDescriptor(
        id = "retra-reference-native",
        displayName = "Retra Native Reference Pipeline",
        version = "0.6.0",
        tier = CoreTier.DIAGNOSTIC_PIPELINE,
        supportsBatterySaves = false,
        supportsSaveStates = true,
        supportsAudio = true,
        supportsCheats = false,
        supportsRewind = true,
        legalNotice = "Diagnostic renderer only. A separately reviewed MPL-2.0 mGBA integration is required for GBA gameplay."
    )
    override val isAvailable: Boolean = true
    override val unavailableReason: String? = null

    private val applicationContext = context.applicationContext
    private val saveStore = AtomicSaveStore(File(applicationContext.filesDir, "emulation"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val nativeLock = Any()
    private var nativeHandle: Long = NativeBridge.nativeCreate()
    private var frameJob: Job? = null
    private var gameHash: String? = null
    private var input = InputSnapshot()
    private var speedMultiplier = 1f
    private var profile = PerformanceProfile.BALANCED
    private var sequence = 0L
    private val closed = AtomicBoolean(false)
    private val rewindBuffer = RewindBuffer(MAX_REWIND_BYTES)

    private val mutableSession = MutableStateFlow(SessionSnapshot())
    override val session: StateFlow<SessionSnapshot> = mutableSession
    private val mutableFrame = MutableStateFlow<VideoFrame?>(null)
    override val latestFrame: StateFlow<VideoFrame?> = mutableFrame
    private val mutableAudio = MutableSharedFlow<AudioPacket>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val audioPackets = mutableAudio
    private val mutableMetrics = MutableStateFlow(RuntimeMetrics(0f, 0f, 100f, 0, 0))
    override val metrics: StateFlow<RuntimeMetrics> = mutableMetrics

    override suspend fun loadGame(game: GameFile): LoadGameResult {
        if (closed.get()) return LoadGameResult.Failed("Native pipeline is closed.")
        frameJob?.cancel()
        frameJob = null
        mutableFrame.value = null
        clearRewind()
        gameHash = null
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.BeginLoad(game.sha256))
        return runCatching {
            val rom = readAndVerifyRom(Uri.parse(game.uri), game.sha256)
            val loaded = synchronized(nativeLock) { NativeBridge.nativeLoadRom(nativeHandle, rom, game.sha256) }
            check(loaded) { "The native reference pipeline rejected the ROM container." }
            gameHash = game.sha256.lowercase()
            mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.LoadSucceeded)
            LoadGameResult.Loaded(restoredSuspendState = restoreSuspendIfCompatible(game.sha256.lowercase()))
        }.getOrElse { error ->
            mutableSession.value = SessionReducer.reduce(
                mutableSession.value,
                SessionCommand.LoadFailed(error.message ?: "Unable to load the selected file.")
            )
            LoadGameResult.Failed(error.message ?: "Unable to load the selected file.")
        }
    }

    override fun start() {
        if (mutableSession.value.phase !in setOf(SessionPhase.READY, SessionPhase.PAUSED, SessionPhase.SUSPENDED)) return
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Start)
        ensureFrameLoop()
    }

    override fun pause() {
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Pause)
    }

    override fun resume() {
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Resume)
        ensureFrameLoop()
    }

    override fun suspendSession() {
        val saveFailure = if (mutableSession.value.phase == SessionPhase.RUNNING || mutableSession.value.phase == SessionPhase.PAUSED) {
            runCatching { saveInternal(SaveKind.SUSPEND, -1) }.exceptionOrNull()
        } else {
            null
        }
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Suspend)
            .copy(errorMessage = saveFailure?.message)
    }

    override fun reset() {
        synchronized(nativeLock) { NativeBridge.nativeReset(nativeHandle) }
        sequence = 0
        clearRewind()
        mutableSession.value = SessionReducer.reduce(mutableSession.value, SessionCommand.Reset)
        start()
    }

    override fun stop() {
        frameJob?.cancel()
        frameJob = null
        mutableFrame.value = null
        clearRewind()
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

    override fun saveBattery(): Result<Unit> = Result.failure(
        UnsupportedOperationException("Battery saves require the gameplay-capable mGBA adapter.")
    )

    override fun saveState(slot: SaveSlot): Result<SaveStateMetadata> = runCatching {
        require(slot.number in 0..99) { "Save-state slot must be between 0 and 99." }
        saveInternal(SaveKind.STATE, slot.number)
        SaveStateMetadata(slot, System.currentTimeMillis(), coreVersion = descriptor.version)
    }

    override fun loadState(slot: SaveSlot): Result<Unit> = runCatching {
        val hash = requireNotNull(gameHash) { "No game is loaded." }
        val bytes = requireNotNull(saveStore.read(statePath(hash, SaveKind.STATE, slot.number))) { "Save slot ${slot.number} is empty." }
        val envelope = SaveEnvelope.decode(bytes)
        check(envelope.gameSha256.equals(hash, ignoreCase = true)) { "Save state belongs to a different ROM." }
        check(envelope.kind == SaveKind.STATE && envelope.slot == slot.number) { "Save-state metadata does not match this slot." }
        check(envelope.coreId == descriptor.id) { "Save state was created by a different core." }
        check(envelope.coreVersion == descriptor.version) { "Save state was created by an incompatible core version." }
        check(synchronized(nativeLock) { NativeBridge.nativeDeserialize(nativeHandle, envelope.payload) }) { "Native state data was rejected." }
        mutableSession.value = mutableSession.value.copy(phase = SessionPhase.PAUSED, errorMessage = null)
    }

    override fun applyCheats(cheats: List<ActiveCheat>): Result<Unit> = Result.failure(
        UnsupportedOperationException("Cheats require the gameplay-capable mGBA adapter.")
    )

    override fun clearCheats(): Result<Unit> = Result.success(Unit)

    override fun rewind(steps: Int): Result<Int> = runCatching {
        require(steps in 1..120) { "Rewind steps must be between 1 and 120." }
        requireNotNull(gameHash) { "No game is loaded." }
        val target = rewindBuffer.rewind(steps)
        check(synchronized(nativeLock) { NativeBridge.nativeDeserialize(nativeHandle, target) }) {
            "The native pipeline rejected the rewind snapshot."
        }
        mutableSession.value = mutableSession.value.copy(phase = SessionPhase.PAUSED, errorMessage = null)
        rewindBuffer.snapshotCount
    }

    private fun restoreSuspendIfCompatible(hash: String): Boolean {
        val bytes = saveStore.read(statePath(hash, SaveKind.SUSPEND, -1)) ?: return false
        return runCatching {
            val envelope = SaveEnvelope.decode(bytes)
            check(envelope.kind == SaveKind.SUSPEND && envelope.slot == -1)
            check(envelope.gameSha256.equals(hash, ignoreCase = true))
            check(envelope.coreId == descriptor.id && envelope.coreVersion == descriptor.version)
            check(synchronized(nativeLock) { NativeBridge.nativeDeserialize(nativeHandle, envelope.payload) })
            true
        }.getOrDefault(false)
    }

    private fun saveInternal(kind: SaveKind, slot: Int) {
        val hash = requireNotNull(gameHash) { "No game is loaded." }
        val state = synchronized(nativeLock) { NativeBridge.nativeSerialize(nativeHandle) }
        val envelope = SaveEnvelope(kind, hash, descriptor.id, descriptor.version, slot, System.currentTimeMillis(), state)
        saveStore.write(statePath(hash, kind, slot), envelope.encode())
    }

    private fun statePath(hash: String, kind: SaveKind, slot: Int): String = when (kind) {
        SaveKind.STATE -> "$hash/states/slot-$slot.rsv"
        SaveKind.SUSPEND -> "$hash/suspend/latest.rsv"
        SaveKind.BATTERY -> "$hash/battery/game.sav"
    }

    private fun ensureFrameLoop() {
        if (frameJob?.isActive == true) return
        frameJob = scope.launch {
            var lastFrameNanos = System.nanoTime()
            var framesInWindow = 0
            var windowStartNanos = lastFrameNanos
            while (isActive && !closed.get()) {
                if (mutableSession.value.phase != SessionPhase.RUNNING) {
                    delay(12)
                    continue
                }
                val start = System.nanoTime()
                val pixels = synchronized(nativeLock) {
                    NativeBridge.nativeStep(nativeHandle, input.mask, speedMultiplier)
                }
                sequence += 1
                val now = System.nanoTime()
                mutableFrame.value = VideoFrame(240, 160, pixels, sequence, now)
                mutableAudio.tryEmit(createDiagnosticAudio(sequence, input.mask))
                framesInWindow += 1
                if (now - windowStartNanos >= 1_000_000_000L) {
                    val seconds = (now - windowStartNanos) / 1_000_000_000f
                    val presented = framesInWindow / max(seconds, 0.001f)
                    mutableMetrics.value = RuntimeMetrics(
                        emulatedFps = presented * speedMultiplier,
                        presentedFps = presented,
                        speedPercent = speedMultiplier * 100f,
                        droppedFrames = 0,
                        audioUnderruns = 0,
                        frameTimeMillis = (now - start) / 1_000_000f
                    )
                    framesInWindow = 0
                    windowStartNanos = now
                }
                val frameBudget = when (profile) {
                    PerformanceProfile.BATTERY_SAVER -> 20_000_000L
                    else -> 16_666_667L
                }
                val elapsed = System.nanoTime() - start
                val delayNanos = frameBudget - elapsed
                if (delayNanos > 0) delay(delayNanos / 1_000_000L)
                lastFrameNanos = now
            }
        }
    }

    private fun captureRewindSnapshot() {
        val state = synchronized(nativeLock) { NativeBridge.nativeSerialize(nativeHandle) }
        if (state.isEmpty()) return
        rewindBuffer.push(state)
    }

    private fun clearRewind() {
        rewindBuffer.clear()
    }

    private fun createDiagnosticAudio(frameSequence: Long, inputMask: Int): AudioPacket {
        val sampleRate = 48_000
        val frames = sampleRate / 60
        val samples = ShortArray(frames * 2)
        val frequency = if (inputMask and 1 != 0) 440.0 else 220.0
        val amplitude = 1_200.0
        val baseSample = frameSequence * frames
        for (index in 0 until frames) {
            val angle = 2.0 * Math.PI * frequency * (baseSample + index) / sampleRate
            val sample = (kotlin.math.sin(angle) * amplitude).toInt().toShort()
            samples[index * 2] = sample
            samples[index * 2 + 1] = sample
        }
        return AudioPacket(sampleRate, 2, samples, frameSequence)
    }

    private fun readAndVerifyRom(uri: Uri, expectedHash: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val output = ByteArrayOutputStream()
        val maximum = 64 * 1024 * 1024
        applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Android could not open the selected ROM." }
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = inputStream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maximum) { "ROM exceeds Retra's 64 MiB safety limit." }
                digest.update(buffer, 0, read)
                output.write(buffer, 0, read)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        require(actualHash.equals(expectedHash, ignoreCase = true)) { "ROM hash changed since import." }
        return output.toByteArray()
    }

    private companion object {
        const val REWIND_CAPTURE_INTERVAL_FRAMES = 30L
        const val MAX_REWIND_BYTES = 32 * 1024 * 1024
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        frameJob?.cancel()
        scope.cancel()
        synchronized(nativeLock) {
            if (nativeHandle != 0L) NativeBridge.nativeDestroy(nativeHandle)
            nativeHandle = 0L
        }
    }
}
