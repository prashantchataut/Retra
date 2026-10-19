package app.retra.ui.patcher

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.core.RomHeader
import app.retra.core.RomImport
import app.retra.data.GameSource
import app.retra.data.ImportOrigin
import app.retra.data.ImportResult
import app.retra.data.MessageTone
import app.retra.data.Provenance
import app.retra.patcher.Checksums
import app.retra.patcher.PatchArchive
import app.retra.patcher.PatchFormat
import app.retra.patcher.PatchResult
import app.retra.patcher.Patcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class BaseInfo(val name: String, val size: Long, val crc32: String, val gameCode: String, val gameId: String?)

@Immutable
data class PatchUi(
    val name: String,
    val size: Long,
    val format: String,
    val sourceSize: Long?,
    val targetSize: Long?,
    val sourceCrc32: String?,
    val targetCrc32: String?,
    val metadata: String?,
)

enum class CrcMatch { MATCH, MISMATCH, UNKNOWN }

sealed interface PatchPhase {
    data object Idle : PatchPhase
    data object Loading : PatchPhase
    data class Applying(val progress: Float) : PatchPhase
    data class Done(val size: Long, val crc32: String, val warnings: List<String>, val gameId: String?) : PatchPhase
    data class Failed(val message: String) : PatchPhase
}

@Immutable
data class PatcherState(
    val base: BaseInfo? = null,
    val patch: PatchUi? = null,
    val strict: Boolean = true,
    val outputName: String = "",
    val phase: PatchPhase = PatchPhase.Idle,
) {
    val match: CrcMatch
        get() {
            val b = base ?: return CrcMatch.UNKNOWN
            val expected = patch?.sourceCrc32 ?: return CrcMatch.UNKNOWN
            return if (expected.equals(b.crc32, true)) CrcMatch.MATCH else CrcMatch.MISMATCH
        }
    val canApply get() = base != null && patch != null && phase !is PatchPhase.Applying && phase !is PatchPhase.Loading &&
        !(strict && match == CrcMatch.MISMATCH)
}

class PatcherViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(PatcherState())
    val state: StateFlow<PatcherState> = _state.asStateFlow()
    private var baseBytes: ByteArray? = null
    private var patchBytes: ByteArray? = null
    private var output: ByteArray? = null
    private var consumedPatchPath: String? = null
    private var consumedBaseId: String? = null

    val games get() = container.library.games

    fun chooseLibraryBase(gameId: String) {
        val g = container.library.game(gameId) ?: return
        baseBytes = null
        _state.update { it.copy(base = BaseInfo(g.title, g.sizeBytes, g.crc32, g.gameCode, g.id), phase = PatchPhase.Idle) }
    }

    fun preselect(baseGameId: String?, patchPath: String?, patchName: String?) {
        if (baseGameId != null && baseGameId != consumedBaseId) {
            consumedBaseId = baseGameId
            chooseLibraryBase(baseGameId)
        }
        if (patchPath != null && patchPath != consumedPatchPath) {
            consumedPatchPath = patchPath
            viewModelScope.launch {
                val bytes = withContext(Dispatchers.IO) { runCatching { File(patchPath).readBytes() }.getOrNull() }
                if (bytes != null) setPatch(bytes, patchName ?: File(patchPath).name)
            }
        }
    }

    fun chooseBaseFile(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(phase = PatchPhase.Loading) }
        runCatching {
            val name = container.importer.displayName(uri)
            val raw = container.importer.readBytes(uri)
            val rom = when (val r = RomImport.extract(raw, name)) {
                is RomImport.Result.Rom -> r.bytes
                is RomImport.Result.Error -> error(r.message)
            }
            val header = RomHeader.parse(rom) ?: error("This file is too small to be a GBA game.")
            baseBytes = rom
            BaseInfo(name, rom.size.toLong(), withContext(Dispatchers.Default) { Checksums.crc32Hex(rom) }, header.gameCode, null)
        }.onSuccess { b -> _state.update { it.copy(base = b, phase = PatchPhase.Idle) } }
            .onFailure { e -> _state.update { it.copy(phase = PatchPhase.Failed(e.message ?: "Couldn't read that file.")) } }
    }

    fun choosePatchFile(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(phase = PatchPhase.Loading) }
        runCatching {
            val name = container.importer.displayName(uri)
            name to container.importer.readBytes(uri)
        }.onSuccess { (name, bytes) -> setPatch(bytes, name) }
            .onFailure { e -> _state.update { it.copy(phase = PatchPhase.Failed(e.message ?: "Couldn't read that file.")) } }
    }

    private suspend fun setPatch(raw: ByteArray, rawName: String) {
        var name = rawName
        var bytes = raw
        if (RomImport.isZip(raw)) {
            val entry = withContext(Dispatchers.IO) { PatchArchive.extractSingle(raw) }
            if (entry == null || Patcher.detect(entry.second) == null) {
                _state.update { it.copy(phase = PatchPhase.Failed("There's no IPS, UPS, BPS or xdelta patch inside this zip.")) }
                return
            }
            name = entry.first
            bytes = entry.second
        }
        val info = withContext(Dispatchers.Default) { Patcher.inspect(bytes) }
        if (info == null) {
            _state.update { it.copy(phase = PatchPhase.Failed("This file isn't a patch Retra can read. Supported formats are IPS, UPS, BPS and xdelta/VCDIFF.")) }
            return
        }
        patchBytes = bytes
        val ui = PatchUi(
            name = name,
            size = bytes.size.toLong(),
            format = when (info.format) { PatchFormat.IPS -> "IPS"; PatchFormat.UPS -> "UPS"; PatchFormat.BPS -> "BPS"; PatchFormat.VCDIFF -> "xdelta" },
            sourceSize = info.sourceSize,
            targetSize = info.targetSize,
            sourceCrc32 = info.sourceCrc32?.let { "%08X".format(it and 0xFFFFFFFFL) },
            targetCrc32 = info.targetCrc32?.let { "%08X".format(it and 0xFFFFFFFFL) },
            metadata = info.metadata?.takeIf { it.isNotBlank() && it.length < 400 },
        )
        _state.update { it.copy(patch = ui, outputName = name.substringBeforeLast('.'), phase = PatchPhase.Idle) }
    }

    fun setStrict(strict: Boolean) = _state.update { it.copy(strict = strict) }
    fun setOutputName(name: String) = _state.update { it.copy(outputName = name.take(80)) }

    fun apply() {
        val s = _state.value
        val base = s.base ?: return
        val patch = patchBytes ?: return
        viewModelScope.launch {
            _state.update { it.copy(phase = PatchPhase.Applying(0f)) }
            val src = baseBytes ?: base.gameId?.let { container.library.readRom(it) }
            if (src == null) {
                _state.update { it.copy(phase = PatchPhase.Failed("The base game file is missing.")) }
                return@launch
            }
            val result = withContext(Dispatchers.Default) {
                Patcher.apply(src, patch, strict = s.strict) { p -> _state.update { it.copy(phase = PatchPhase.Applying(p)) } }
            }
            when (result) {
                is PatchResult.Error -> _state.update { it.copy(phase = PatchPhase.Failed(result.message)) }
                is PatchResult.Ok -> {
                    output = result.output
                    _state.update { it.copy(phase = PatchPhase.Done(result.output.size.toLong(), Checksums.crc32Hex(result.output), result.warnings, null)) }
                }
            }
        }
    }

    fun addToLibrary() {
        val out = output ?: return
        val s = _state.value
        viewModelScope.launch {
            val name = s.outputName.ifBlank { "Patched game" }
            val r = container.library.addRom(
                out, "$name.gba",
                ImportOrigin(
                    GameSource.PATCHED, title = name,
                    provenance = Provenance(
                        patchName = s.patch?.name ?: "patch", patchFormat = s.patch?.format ?: "?",
                        baseTitle = s.base?.name ?: "base", baseCrc32 = s.base?.crc32 ?: "",
                        appliedAt = System.currentTimeMillis(), forced = !s.strict && s.match == CrcMatch.MISMATCH,
                    ),
                ),
            )
            val id = when (r) {
                is ImportResult.Added -> r.game.id.also { container.messages.show("$name is in your games", MessageTone.SUCCESS) }
                is ImportResult.Duplicate -> r.game.id.also { container.messages.show("You already have this game") }
                is ImportResult.Error -> null.also { container.messages.show(r.message, MessageTone.ERROR) }
            }
            _state.update { st -> (st.phase as? PatchPhase.Done)?.let { st.copy(phase = it.copy(gameId = id)) } ?: st }
        }
    }

    fun export(uri: Uri, resolver: ContentResolver) {
        val out = output ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { resolver.openOutputStream(uri, "wt")?.use { it.write(out) } ?: error("") } }
                .onSuccess { container.messages.show("Saved the patched game", MessageTone.SUCCESS) }
                .onFailure { container.messages.show("Couldn't save the file there.", MessageTone.ERROR) }
        }
    }

    fun reset() {
        baseBytes = null; patchBytes = null; output = null
        _state.value = PatcherState()
    }

    fun dismissError() = _state.update { it.copy(phase = PatchPhase.Idle) }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { PatcherViewModel(c) } }
    }
}
