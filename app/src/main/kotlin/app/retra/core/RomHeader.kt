package app.retra.core

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Fields of the 192-byte GBA cartridge header. */
data class RomHeader(
    val title: String,
    val gameCode: String,
    val makerCode: String,
    val version: Int,
    val looksValid: Boolean,
) {
    companion object {
        const val MIN_SIZE = 0xC0
        const val MAX_SIZE = 32 * 1024 * 1024

        fun parse(rom: ByteArray): RomHeader? {
            if (rom.size < MIN_SIZE) return null
            val title = ascii(rom, 0xA0, 12)
            val code = ascii(rom, 0xAC, 4)
            val maker = ascii(rom, 0xB0, 2)
            val version = rom[0xBC].toInt() and 0xFF
            // 0xB2 must be 0x96 on licensed carts; most homebrew sets it too. Branch at 0 is ARM "B".
            val valid = (rom[0xB2].toInt() and 0xFF) == 0x96 || (rom[3].toInt() and 0xFF) == 0xEA
            return RomHeader(title, code, maker, version, valid)
        }

        private fun ascii(b: ByteArray, off: Int, len: Int): String {
            val sb = StringBuilder()
            for (i in off until off + len) {
                val c = b[i].toInt() and 0xFF
                if (c == 0) break
                if (c in 0x20..0x7E) sb.append(c.toChar())
            }
            return sb.toString().trim()
        }
    }
}

/** Turns an imported file into ROM bytes (unzipping when needed). */
object RomImport {
    private val ROM_EXT = setOf("gba", "agb", "bin")
    private const val MAX_ENTRY = 64L * 1024 * 1024

    sealed interface Result {
        data class Rom(val bytes: ByteArray, val name: String) : Result
        data class Error(val message: String) : Result
    }

    fun isZip(bytes: ByteArray) = bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
        bytes[2].toInt() == 3 && bytes[3].toInt() == 4

    fun extract(bytes: ByteArray, fileName: String): Result {
        if (!isZip(bytes)) return Result.Rom(bytes, fileName)
        var best: Pair<String, ByteArray>? = null
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    val name = e.name.replace('\\', '/').substringAfterLast('/')
                    if (e.isDirectory || name.startsWith("._") || e.name.startsWith("__MACOSX")) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in ROM_EXT) continue
                    val data = readCapped(zin) ?: continue
                    if (ext == "bin" && RomHeader.parse(data)?.looksValid != true) continue
                    if (best == null || data.size > best!!.second.size) best = name to data
                }
            }
        } catch (e: java.io.IOException) {
            return Result.Error("This zip file looks damaged. Try downloading it again.")
        } catch (e: IllegalArgumentException) {
            return Result.Error("This zip file looks damaged. Try downloading it again.")
        }
        val b = best ?: return Result.Error("There's no .gba game inside this zip.")
        return Result.Rom(b.second, b.first)
    }

    private fun readCapped(zin: ZipInputStream): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = zin.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
