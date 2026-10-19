package app.retra.patcher

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bo = ByteArrayOutputStream()
        ZipOutputStream(bo).use { z ->
            for ((n, b) in entries) {
                z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry()
            }
        }
        return bo.toByteArray()
    }

    @Test fun picksPatchOverReadme() {
        val patch = "BPS1....".toByteArray()
        val (name, bytes) = PatchArchive.extractSingle(zip("readme.txt" to "hi".toByteArray(), "Game v1/game.BPS" to patch))!!
        assertEquals("game.BPS", name)
        assertArrayEquals(patch, bytes)
    }

    @Test fun prefersPatchOverRomAndIgnoresMacJunk() {
        val r = PatchArchive.extractSingle(zip(
            "__MACOSX/._x.ups" to "UPS1junk".toByteArray(),
            "rom.gba" to ByteArray(100),
            "hack.xdelta" to byteArrayOf(0xD6.toByte(), 0xC3.toByte(), 0xC4.toByte(), 0),
        ))!!
        assertEquals("hack.xdelta", r.first)
        assertEquals("rom.gba", PatchArchive.extractSingle(zip("rom.gba" to ByteArray(10), "a.txt" to ByteArray(3)))!!.first)
    }

    @Test fun detectsByContentAndHandlesGarbage() {
        assertEquals("patch.bin", PatchArchive.extractSingle(zip("patch.bin" to "PATCHxxxEOF".toByteArray()))!!.first)
        assertNull(PatchArchive.extractSingle(zip("a.txt" to "x".toByteArray())))
        assertNull(PatchArchive.extractSingle("not a zip".toByteArray()))
        assertNull(PatchArchive.extractSingle(ByteArray(0)))
    }
}
