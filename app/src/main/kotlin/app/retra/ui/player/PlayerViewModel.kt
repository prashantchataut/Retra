package app.retra.ui.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.core.PlaytimeTracker
import app.retra.data.Cheat
import app.retra.data.MessageTone
import app.retra.data.SaveRepository
import app.retra.data.Settings
import app.retra.data.StateSlot
import app.retra.emu.AudioOutput
import app.retra.emu.EmuCore
import app.retra.emu.EmulatorSession
import app.retra.emu.NativeEmuCore
import app.retra.engine.LoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed interface PlayerStatus {
    data object Loading : PlayerStatus
    data object Running : PlayerStatus
    data class Failed(val message: String) : PlayerStatus
}

@Immutable
data class PlayerUiState(
    val status: PlayerStatus = PlayerStatus.Loading,
    val title: String = "",
    val coverAsset: String? = null,
    val lastScreenPath: String? = null,
    val menuOpen: Boolean = false,
    val fastForward: Boolean = false,
    val rewinding: Boolean = false,
    val slots: List<StateSlot> = emptyList(),
    val cheats: List<Cheat> = emptyList(),
    val lastCapture: String? = null,
    val exiting: Boolean = false,
    /** True while exiting writes the Continue snapshot (false = leaving without one). */
    val savingOnExit: Boolean = true,
)

/** Converts a 240x160 ARGB frame to PNG (optionally nearest-neighbour upscaled). */
fun framePng(pixels: IntArray, scale: Int = 1): ByteArray {
    val bmp = Bitmap.createBitmap(pixels, EmuCore.WIDTH, EmuCore.HEIGHT, Bitmap.Config.ARGB_8888)
    val out = if (scale > 1) Bitmap.createScaledBitmap(bmp, EmuCore.WIDTH * scale, EmuCore.HEIGHT * scale, false) else bmp
    return ByteArrayOutputStream().use { s -> out.compress(Bitmap.CompressFormat.PNG, 100, s); s.toByteArray() }
}

class PlayerViewModel(
    private val container: AppContainer,
    private val gameId: String,
    private val startSlot: Int,
    private val coreFactory: () -> EmuCore = { NativeEmuCore() },
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    val settings: StateFlow<Settings> get() = container.settings.settings

    var session: EmulatorSession? = null
        private set
    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()
    private val _firstFrame = MutableStateFlow(false)
    val firstFrame: StateFlow<Boolean> = _firstFrame.asStateFlow()
    private val playtime = PlaytimeTracker()
    private var hostResumed = false
    private var ffToggled = false
    private var ffHeld = false
    private var finished = false
    private val saves: SaveRepository get() = container.saves

    init {
        val game = container.library.game(gameId)
        _state.update {
            it.copy(
                title = game?.title ?: "",
                coverAsset = game?.coverAsset,
                lastScreenPath = saves.lastScreenFile(gameId).takeIf { f -> f.exists() }?.absolutePath,
            )
        }
        viewModelScope.launch { boot() }
        viewModelScope.launch {
            container.settings.settings.collect { applySettings(it) }
        }
    }

    private suspend fun boot() {
        if (container.library.game(gameId) == null) return fail("This game isn't in your library anymore.")
        val rom = container.library.readRom(gameId) ?: return fail("The game file is missing. Try importing it again.")
        val core = try {
            coreFactory()
        } catch (e: Throwable) {
            return fail("Retra's emulator engine couldn't start on this device (${e.javaClass.simpleName}).")
        }
        audio = runCatching { AudioOutput() }.getOrNull()
        val s = EmulatorSession(core, audio) { data, first ->
            runCatching { saves.writeBatterySave(gameId, data, backup = first) }
        }
        session = s
        s.start()
        viewModelScope.launch { s.fps.collect { _fps.value = it } }
        viewModelScope.launch { s.firstFrameShown.collect { _firstFrame.value = it } }
        val bios = withContext(Dispatchers.IO) { container.bios.read() }
        when (val r = s.run { it.loadRom(rom, bios) }) {
            is LoadResult.Error -> {
                withContext(Dispatchers.IO) { s.stop() }
                session = null
                return fail("This game couldn't start: ${r.message}")
            }
            is LoadResult.Ok -> container.library.recordRomInfo(gameId, r.info.saveType.name, r.info.hasRtc)
        }
        val sav = withContext(Dispatchers.IO) { saves.readBatterySave(gameId) }
        val cheats = withContext(Dispatchers.IO) { container.cheats.cheats(gameId) }
        val st = container.settings.current
        s.run { c ->
            if (sav != null) c.loadBatterySave(sav)
            c.setColorCorrection(st.colorCorrection)
            c.setRtcOffsetSeconds(st.rtcOffsetSeconds)
            val codes = cheats.filter { it.enabled }.map { it.code }
            if (codes.isNotEmpty()) c.setCheats(codes)
            c.consumeSaveDirty() // loading the .sav isn't a new write
        }
        if (startSlot >= 0) {
            val data = withContext(Dispatchers.IO) { saves.readState(gameId, startSlot) }
            val ok = data != null && s.run { it.loadState(data) }
            if (!ok) container.messages.show("That save state couldn't be loaded, so the game started fresh.", MessageTone.ERROR)
        }
        applySettings(st)
        _state.update { it.copy(status = PlayerStatus.Running, cheats = cheats) }
        refreshSlots()
        updateRunning()
    }

    private fun fail(message: String) {
        _state.update { it.copy(status = PlayerStatus.Failed(message)) }
    }

    private fun applySettings(st: Settings) {
        val s = session ?: return
        s.fastForwardSpeed = st.fastForwardSpeed
        s.fastForwardAudio = st.fastForwardAudio
        s.rewindEnabled = st.rewindEnabled
        s.muted = st.muted || st.volume <= 0f
        audio?.setVolume(if (st.muted) 0f else st.volume)
        if (s.isRunning && _state.value.status == PlayerStatus.Running) {
            viewModelScope.launch { runCatching { s.run { it.setColorCorrection(st.colorCorrection) } } }
        }
    }

    private var audio: app.retra.emu.AudioSink? = null

    // ---- lifecycle ----

    fun onHostResume() {
        hostResumed = true
        updateRunning()
    }

    fun onHostPause() {
        hostResumed = false
        updateRunning()
        commitPlaytime()
    }

    private fun updateRunning() {
        val s = session ?: return
        val run = hostResumed && !_state.value.menuOpen && _state.value.status == PlayerStatus.Running && !finished
        s.isPaused = !run
        if (run) playtime.start() else playtime.pause()
    }

    private fun commitPlaytime() {
        val ms = playtime.drain()
        if (ms > 0) viewModelScope.launch { container.library.recordSession(gameId, ms) }
    }

    // ---- input ----

    fun setTouchKeys(mask: Int) { session?.touchKeys = mask }
    fun setPadKeys(mask: Int) { session?.padKeys = mask }

    fun toggleFastForward() {
        ffToggled = !ffToggled
        syncFf()
    }

    fun holdFastForward(held: Boolean) {
        ffHeld = held
        syncFf()
    }

    private fun syncFf() {
        val on = ffToggled || ffHeld
        session?.fastForward = on
        _state.update { it.copy(fastForward = on) }
    }

    fun setRewinding(on: Boolean) {
        if (on && !container.settings.current.rewindEnabled) {
            container.messages.show("Rewind is off. Turn it on in Settings.")
            return
        }
        session?.rewinding = on
        _state.update { it.copy(rewinding = on) }
    }

    // ---- menu ----

    fun openMenu(open: Boolean) {
        _state.update { it.copy(menuOpen = open, lastCapture = if (open) it.lastCapture else null) }
        if (open) {
            refreshSlots()
            commitPlaytime()
        }
        updateRunning()
    }

    private fun refreshSlots() = viewModelScope.launch {
        val slots = withContext(Dispatchers.IO) { saves.slots(gameId) }
        _state.update { it.copy(slots = slots) }
    }

    fun saveState(slot: Int) = viewModelScope.launch {
        val s = session ?: return@launch
        val (data, frame) = s.run { it.saveState() to s.snapshotFrame() }
        withContext(Dispatchers.IO) { saves.writeState(gameId, slot, data, framePng(frame)) }
        refreshSlots()
        container.messages.show(if (slot == 0) "Saved to the auto slot" else "Saved to slot $slot", MessageTone.SUCCESS)
    }

    fun loadState(slot: Int) = viewModelScope.launch {
        val s = session ?: return@launch
        val data = withContext(Dispatchers.IO) { saves.readState(gameId, slot) } ?: return@launch
        val ok = s.run { it.loadState(data) }
        s.clearRewind()
        if (ok) {
            container.messages.show(if (slot == 0) "Loaded the auto save" else "Loaded slot $slot", MessageTone.SUCCESS)
            openMenu(false)
        } else {
            container.messages.show("That state is from another version of the game and can't be loaded.", MessageTone.ERROR)
        }
    }

    fun reset() = viewModelScope.launch {
        val s = session ?: return@launch
        s.run { it.reset() }
        s.clearRewind()
        openMenu(false)
    }

    fun setCheats(list: List<Cheat>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { container.cheats.save(gameId, list) }
        _state.update { it.copy(cheats = list) }
        val s = session ?: return@launch
        val codes = list.filter { it.enabled }.map { it.code }
        val accepted = s.run { it.setCheats(codes) }
        val lines = codes.sumOf { it.lines().size }
        if (lines > 0 && accepted < lines) {
            container.messages.show("$accepted of $lines code lines work with this engine", MessageTone.INFO)
        }
    }

    fun screenshot(context: Context) = viewModelScope.launch {
        val s = session ?: return@launch
        val frame = s.run { s.snapshotFrame() }
        val png = withContext(Dispatchers.Default) { framePng(frame, scale = 3) }
        val file = saves.newCaptureFile(gameId)
        withContext(Dispatchers.IO) {
            app.retra.core.AtomicFiles.write(file, png)
            if (Build.VERSION.SDK_INT >= 29) runCatching { saveToGallery(context, png, file.name) }
        }
        _state.update { it.copy(lastCapture = file.absolutePath) }
        container.messages.show(if (Build.VERSION.SDK_INT >= 29) "Screenshot saved to Pictures/Retra" else "Screenshot saved", MessageTone.SUCCESS)
    }

    private fun saveToGallery(context: Context, png: ByteArray, name: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Retra")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { it.write(png) }
    }

    fun shareUri(context: Context, path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", java.io.File(path))

    /**
     * Stops the session. With [saveState] the auto ("Continue") state and last-session screenshot are
     * written first; without it the game is left as-is. The game's own battery save is always flushed,
     * because that's what the game itself saved.
     */
    suspend fun finish(saveState: Boolean = container.settings.current.autoSaveOnExit) {
        if (finished) return
        finished = true
        _state.update { it.copy(exiting = true, savingOnExit = saveState) }
        val s = session ?: return
        playtime.pause()
        if (_state.value.status == PlayerStatus.Running && s.isRunning) {
            runCatching {
                s.isPaused = true
                if (saveState) {
                    val (data, frame) = s.run { it.saveState() to s.snapshotFrame() }
                    withContext(Dispatchers.IO) {
                        val png = framePng(frame)
                        saves.writeState(gameId, SaveRepository.AUTO_SLOT, data, png)
                        saves.writeLastScreen(gameId, framePng(frame, 2))
                    }
                }
                s.flushBatterySave()
            }
        }
        withContext(Dispatchers.IO) { s.stop() }
        session = null
        container.library.recordSession(gameId, playtime.drain())
    }

    override fun onCleared() {
        if (!finished) container.scope.launch { finish() }
    }

    companion object {
        fun factory(c: AppContainer, id: String, slot: Int) = viewModelFactory { initializer { PlayerViewModel(c, id, slot) } }
    }
}
