package app.retra.patcher

import app.retra.patcher.TestUtil.ok
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 16 MiB base -> 32 MiB hack, like most Pokemon fan games. */
class PerformanceTest {
    private fun timed(label: String, block: () -> Unit): Long {
        val t0 = System.nanoTime(); block(); val ms = (System.nanoTime() - t0) / 1_000_000
        println("$label: $ms ms"); return ms
    }

    @Test fun bigRomPatches() {
        val src = TestUtil.random(16 shl 20, 1)
        val tgt = TestUtil.mutate(src, 2, newSize = 32 shl 20, edits = 20_000)
        var bps: ByteArray? = null
        timed("BPS create 32 MiB") { bps = BpsCreator.create(src, tgt) }
        println("BPS patch size: ${bps!!.size}")
        val ups = TestUtil.ups(src, tgt)
        for ((name, patch) in listOf("BPS" to bps, "UPS" to ups)) {
            ok(Patcher.apply(src, patch)) // warm-up
            var out: ByteArray? = null
            val ms = timed("$name apply 32 MiB") { out = ok(Patcher.apply(src, patch)).output }
            assertArrayEquals(tgt, out)
            assertTrue("$name took $ms ms", ms < 2000)
        }
        // IPS offsets are 24-bit, so records can only start in the first 16 MiB; apply to a 32 MiB ROM anyway.
        val src32 = TestUtil.random(32 shl 20, 3)
        val tgt32 = src32.copyOf().also { t ->
            val m = TestUtil.mutate(src32.copyOf(16 shl 20), 4, edits = 20_000)
            System.arraycopy(m, 0, t, 0, m.size)
        }
        val ips = TestUtil.ips(src32, tgt32)
        ok(Patcher.apply(src32, ips))
        val ms = timed("IPS apply 32 MiB") { assertArrayEquals(tgt32, ok(Patcher.apply(src32, ips)).output) }
        assertTrue(ms < 2000)
    }
}
