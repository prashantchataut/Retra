package app.retra.patcher

import app.retra.patcher.TestUtil.err
import app.retra.patcher.TestUtil.ok
import app.retra.patcher.TestUtil.resource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures generated with xdelta3 3.0.11 (Debian build: default secondary compressor is LZMA). */
class VcdiffTest {
    private val src = resource("vcdiff/src.bin")
    private val tgt = resource("vcdiff/tgt.bin")

    private fun check(name: String) {
        val patch = resource("vcdiff/$name")
        assertEquals(PatchFormat.VCDIFF, Patcher.detect(patch))
        val r = ok(Patcher.apply(src, patch))
        assertArrayEquals(name, tgt, r.output)
        assertTrue(r.warnings.isEmpty())
        assertEquals(tgt.size.toLong(), Patcher.inspect(patch)!!.targetSize)
    }

    @Test fun noSecondary() = check("none.xdelta")
    @Test fun noAppHeader() = check("none_noapp.xdelta")
    @Test fun noChecksum() = check("none_nochecksum.xdelta")
    @Test fun multiWindow() = check("multiwindow.xdelta")
    @Test fun defaultLzmaSecondary() = check("default_lzma.xdelta")
    @Test fun lzmaLevel9() = check("lzma9.xdelta")

    @Test fun noSourceWindows() {
        val text = resource("vcdiff/text.bin")
        // text_multiwindow_lzma: 3 windows sharing one LZMA stream per section (xdelta3 SYNC_FLUSH).
        for (n in listOf("text_nosrc_none.xdelta", "text_nosrc_lzma.xdelta", "text_multiwindow_lzma.xdelta")) {
            assertArrayEquals(n, text, ok(Patcher.apply(ByteArray(0), resource("vcdiff/$n"))).output)
        }
    }

    @Test fun appHeaderAsMetadata() {
        val info = Patcher.inspect(resource("vcdiff/none.xdelta"))!!
        assertEquals("tgt.bin//src.bin/", info.metadata)
        assertNull(info.sourceCrc32)
        assertNull(Patcher.inspect(resource("vcdiff/none_noapp.xdelta"))!!.metadata)
    }

    @Test fun unsupportedSecondary() {
        for ((n, label) in listOf("djw.xdelta" to "DJW", "fgk.xdelta" to "FGK")) {
            val e = err(Patcher.apply(src, resource("vcdiff/$n")))
            assertEquals(PatchError.UNSUPPORTED, e.code)
            assertTrue(e.message, e.message.contains(label))
            // still inspectable
            assertEquals(tgt.size.toLong(), Patcher.inspect(resource("vcdiff/$n"))!!.targetSize)
        }
    }

    @Test fun wrongSource() {
        val wrong = src.copyOf().also { it[100] = (it[100] + 1).toByte() }
        for (n in listOf("none.xdelta", "default_lzma.xdelta")) {
            val e = err(Patcher.apply(wrong, resource("vcdiff/$n")))
            assertEquals(PatchError.SOURCE_MISMATCH, e.code)
            val r = ok(Patcher.apply(wrong, resource("vcdiff/$n"), strict = false))
            assertEquals(1, r.warnings.size)
            assertEquals(tgt.size, r.output.size)
        }
        // Too-small source
        val e = err(Patcher.apply(src.copyOf(100), resource("vcdiff/none.xdelta")))
        assertEquals(PatchError.SOURCE_MISMATCH, e.code)
        assertTrue(e.message.contains("only"))
        ok(Patcher.apply(src.copyOf(100), resource("vcdiff/none.xdelta"), strict = false))
        assertEquals(PatchError.SOURCE_MISMATCH, err(Patcher.apply(ByteArray(0), resource("vcdiff/none.xdelta"))).code)
    }

    @Test fun truncated() {
        for (n in listOf("none.xdelta", "default_lzma.xdelta", "text_nosrc_lzma.xdelta", "text_multiwindow_lzma.xdelta")) {
            val p = resource("vcdiff/$n")
            for (cut in listOf(4, 5, 10, 30, p.size / 2, p.size - 1)) {
                val r = Patcher.apply(src, p.copyOf(cut))
                assertTrue("$n cut $cut: $r", r is PatchResult.Error)
                assertEquals(PatchError.CORRUPT_PATCH, (r as PatchResult.Error).code)
            }
        }
    }
}
