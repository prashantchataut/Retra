package app.retra.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CheatImportTest {
    @Test fun regroupsPlusSeparatedPieces() {
        assertEquals("82025838 0000", CheatImport.regroup("82025838+0000"))
        assertEquals("D8BAE4D9 4864DCE5\nA86CDBA5 19BA49B3", CheatImport.regroup("D8BAE4D9+4864DCE5+A86CDBA5+19BA49B3"))
        assertEquals("D8BAE4D9 4864DCE5", CheatImport.regroup("D8BAE4D94864DCE5"))
        assertEquals("02000000:FF", CheatImport.regroup("02000000:FF"))
    }

    @Test fun parsesRetroArchChtFiles() {
        val cht = """
            cheats = 3

            cheat0_desc = "Master Code"
            cheat0_code = "D8BAE4D9+4864DCE5+A86CDBA5+19BA49B3"
            cheat0_enable = false

            cheat1_desc = "Infinite   Money"
            cheat1_code = "82025838+0000"
            cheat1_enable = false

            cheat2_desc = "Broken"
            cheat2_code = "not a code"
        """.trimIndent()
        val r = CheatImport.parse(cht)
        assertEquals(listOf("Master Code", "Infinite Money"), r.cheats.map { it.name })
        assertEquals("82025838 0000", r.cheats[1].code)
        assertEquals(1, r.skipped)
    }

    @Test fun parsesNamedBlocksAndBareCodes() {
        val text = """
            Max money:
            82025838 0000
            82025840 98967F

            # Walk through walls
            509197D3 AAAAAAAA
            
            03001234:63
        """.trimIndent()
        val r = CheatImport.parse(text) { "Cheat ${it + 1}" }
        assertEquals(listOf("Walk through walls", "Cheat 2"), r.cheats.map { it.name })
        assertEquals(1, r.skipped) // the 6-digit CodeBreaker parameter row isn't a code on its own
        assertEquals("03001234:63", r.cheats[1].code)
    }
}
