package app.retra.data

import app.retra.core.AtomicFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Optional user-supplied GBA BIOS (exactly 16 KiB). Retra never ships or downloads one. */
class BiosRepository(root: File) {
    private val file = File(File(root, "bios"), "gba_bios.bin")
    private val _present = MutableStateFlow(file.exists())
    val present: StateFlow<Boolean> = _present

    companion object { const val SIZE = 16 * 1024 }

    fun read(): ByteArray? = file.takeIf { it.exists() && it.length() == SIZE.toLong() }?.readBytes()

    /** Returns an error message, or null on success. */
    fun import(bytes: ByteArray): String? {
        if (bytes.size != SIZE) return "A GBA BIOS is exactly 16 KB; this file is ${bytes.size} bytes."
        AtomicFiles.write(file, bytes)
        _present.value = true
        return null
    }

    fun remove() {
        file.delete()
        _present.value = false
    }
}
