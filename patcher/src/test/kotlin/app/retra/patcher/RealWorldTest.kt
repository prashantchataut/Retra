package app.retra.patcher

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Optional checks against real patches that are too big to commit. Set the system property / env
 * RETRA_PATCH_SAMPLES to a directory containing the files; otherwise these tests are skipped.
 */
class RealWorldTest {
    private val dir = File(System.getProperty("retra.patchSamples") ?: System.getenv("RETRA_PATCH_SAMPLES") ?: "/nonexistent")

    @Test fun pokemonHnsUps() {
        val f = File(dir, "pokemonHnS_v1.2.1.ups")
        assumeTrue(f.isFile)
        val patch = f.readBytes()
        val info = Patcher.inspect(patch)!!
        assertEquals(PatchFormat.UPS, info.format)
        assertEquals("1F1C08FB", Msg.hex(info.sourceCrc32!!))
        assertEquals(16777216L, info.sourceSize)
        assertEquals(33554432L, info.targetSize)
        assertEquals("96A8425B", Msg.hex(info.targetCrc32!!))
        // We don't have the base ROM: a fake one must be rejected in strict mode with a friendly message
        // and still be force-appliable.
        val fake = ByteArray(16 shl 20)
        val e = Patcher.apply(fake, patch) as PatchResult.Error
        assertEquals(PatchError.SOURCE_MISMATCH, e.code)
        println(e.message)
        val t0 = System.nanoTime()
        val ok = Patcher.apply(fake, patch, strict = false) as PatchResult.Ok
        println("forced apply: ${(System.nanoTime() - t0) / 1_000_000} ms, warnings=${ok.warnings}")
        assertEquals(33554432, ok.output.size)
    }

    /** big_src.bin + big_*.xdelta -> big_tgt.bin (e.g. 16 MiB -> 32 MiB made with xdelta3). */
    @Test fun bigXdelta() {
        val src = File(dir, "big_src.bin")
        val tgt = File(dir, "big_tgt.bin")
        assumeTrue(src.isFile && tgt.isFile)
        val s = src.readBytes()
        val t = tgt.readBytes()
        val patches = dir.listFiles { f -> f.name.startsWith("big_") && f.name.endsWith(".xdelta") }!!.sorted()
        assumeTrue(patches.isNotEmpty())
        for (p in patches) {
            val bytes = p.readBytes()
            Patcher.apply(s, bytes) // warm-up
            val t0 = System.nanoTime()
            val r = Patcher.apply(s, bytes)
            val ms = (System.nanoTime() - t0) / 1_000_000
            println("${p.name}: $ms ms -> $r")
            org.junit.Assert.assertArrayEquals(p.name, t, (r as PatchResult.Ok).output)
            org.junit.Assert.assertTrue(ms < 2000)
        }
    }
}
