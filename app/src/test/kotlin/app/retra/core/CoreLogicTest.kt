package app.retra.core

import app.retra.emu.RewindBuffer
import app.retra.engine.GbaKey
import app.retra.ui.player.ControlRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    @Test fun cheatFormats() {
        assertEquals(CheatParser.Format.RAW, (CheatParser.parse("0300203c:03") as CheatParser.Result.Ok).format)
        assertEquals("0300203C:03", (CheatParser.parse(" 0300203c:03 ") as CheatParser.Result.Ok).normalized)
        assertEquals(CheatParser.Format.CODEBREAKER, (CheatParser.parse("82025BC4 FFFF") as CheatParser.Result.Ok).format)
        val gs = CheatParser.parse("509197d3c2a1f5f2") as CheatParser.Result.Ok
        assertEquals("509197D3 C2A1F5F2", gs.normalized)
        assertEquals(CheatParser.Format.GAMESHARK_AR, gs.format)
        val multi = CheatParser.parse("509197D3 C2A1F5F2\n1C88D3E2-7A2B1C08") as CheatParser.Result.Ok
        assertEquals(2, multi.lines)
        assertEquals("509197D3 C2A1F5F2\n1C88D3E2 7A2B1C08", multi.normalized)
        assertEquals("AR:509197D3 C2A1F5F2", (CheatParser.parse("ar:509197D3 C2A1F5F2") as CheatParser.Result.Ok).normalized)
        assertEquals(CheatParser.Format.MIXED, (CheatParser.parse("0300203C:03\n82025BC4 FFFF") as CheatParser.Result.Ok).format)
    }

    @Test fun cheatErrors() {
        assertTrue(CheatParser.parse("") is CheatParser.Result.Error)
        assertTrue(CheatParser.parse("hello world") is CheatParser.Result.Error)
        assertTrue((CheatParser.parse("0300203C:03\nnope") as CheatParser.Result.Error).message.contains("Line 2"))
        assertTrue(CheatParser.parse("GS:0300203C:03") is CheatParser.Result.Error)
    }

    @Test fun playtimeCountsOnlyRunningTime() {
        var t = 0L
        val p = PlaytimeTracker { t }
        p.start(); t = 1_000; p.pause()
        t = 5_000 // paused, not counted
        p.start(); t = 6_500
        assertEquals(2_500, p.drain())
        t = 7_000
        assertEquals(500, p.drain())
        p.pause(); t = 9_000
        assertEquals(0, p.drain())
    }

    @Test fun rewindRingKeepsNewest() {
        val r = RewindBuffer(3)
        for (i in 1..5) r.push(byteArrayOf(i.toByte()))
        assertEquals(3, r.size)
        assertArrayEquals(byteArrayOf(5), r.pop())
        assertArrayEquals(byteArrayOf(4), r.pop())
        assertArrayEquals(byteArrayOf(3), r.pop())
        assertNull(r.pop())
    }

    @Test fun dpadDiagonalsByAngle() {
        assertEquals(GbaKey.RIGHT, ControlRegistry.dpadBits(10f, 0f))
        assertEquals(GbaKey.UP, ControlRegistry.dpadBits(0f, -10f))
        assertEquals(GbaKey.UP or GbaKey.RIGHT, ControlRegistry.dpadBits(10f, -10f))
        assertEquals(GbaKey.DOWN or GbaKey.LEFT, ControlRegistry.dpadBits(-10f, 10f))
        assertEquals(GbaKey.LEFT, ControlRegistry.dpadBits(-10f, 1f))
    }

    @Test fun formatting() {
        assertEquals("Under a minute", formatPlaytime(30_000))
        assertEquals("45 min", formatPlaytime(45 * 60_000L))
        assertEquals("3 h 12 min", formatPlaytime((3 * 60 + 12) * 60_000L))
        assertEquals("16 MB", formatBytes(16L * 1024 * 1024))
        assertEquals("1.5 MB", formatBytes(1536L * 1024))
    }

    @Test fun prettyTitles() {
        assertEquals("Pokemon - Emerald", GameClassifier.prettyTitle("POKEMON EMER", "Pokemon - Emerald (USA, Europe).gba"))
        assertEquals("Pokemon Emer", GameClassifier.prettyTitle("POKEMON EMER", "a.gba"))
    }
}
