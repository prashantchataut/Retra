package app.retra.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupAndCheatsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun backupRoundTrip() {
        val src = tmp.newFolder("src")
        SaveRepository(src).writeBatterySave("abc", byteArrayOf(1, 2), backup = false)
        CheatRepository(src).save("abc", listOf(Cheat("1", "Money", "82025BC4 FFFF")))
        val zip = ByteArrayOutputStream().also { assertEquals(1, BackupManager(src).backup(it)) }.toByteArray()

        val dst = tmp.newFolder("dst")
        val r = BackupManager(dst).restore(ByteArrayInputStream(zip))
        assertEquals(1, r.saves)
        assertEquals(1, r.cheats)
        assertArrayEquals(byteArrayOf(1, 2), SaveRepository(dst).readBatterySave("abc"))
        assertEquals("Money", CheatRepository(dst).cheats("abc").single().name)
    }

    @Test fun restoreIgnoresPathTraversal() {
        val zip = zipOf("saves/../../evil.sav" to byteArrayOf(1), "../x.sav" to byteArrayOf(1), "saves/ok.sav" to byteArrayOf(5))
        val dst = tmp.newFolder("dst2")
        val r = BackupManager(dst).restore(ByteArrayInputStream(zip))
        assertEquals(1, r.saves)
        assertFalse(tmp.root.resolve("evil.sav").exists())
        assertTrue(dst.resolve("saves/ok.sav").exists())
    }

    @Test fun cheatRepositoryKeepsOrderAndEnabledCodes() {
        val repo = CheatRepository(tmp.root)
        repo.save("g", listOf(Cheat("1", "A", "0300203C:03", true), Cheat("2", "B", "82025BC4 FFFF", false)))
        assertEquals(listOf("0300203C:03"), repo.enabledCodes("g"))
        repo.save("g", emptyList())
        assertTrue(repo.cheats("g").isEmpty())
    }
}
