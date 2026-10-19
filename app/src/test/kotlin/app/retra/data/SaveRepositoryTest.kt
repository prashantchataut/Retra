package app.retra.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 100L
    private fun repo() = SaveRepository(tmp.root) { now }

    @Test fun writesAtomicallyAndReadsBack() {
        val r = repo()
        assertNull(r.readBatterySave("g"))
        r.writeBatterySave("g", byteArrayOf(1, 2, 3), backup = false)
        assertArrayEquals(byteArrayOf(1, 2, 3), r.readBatterySave("g"))
        assertFalse(tmp.root.resolve("saves/.g.sav.tmp").exists())
    }

    @Test fun rotatesBackupsKeepingFive() {
        val r = repo()
        for (i in 1..9) {
            now += 10
            r.writeBatterySave("g", byteArrayOf(i.toByte()), backup = true)
        }
        val backups = r.backups("g")
        assertEquals(SaveRepository.MAX_BACKUPS, backups.size)
        // Newest backup holds the previous save (8), oldest kept is 4.
        assertArrayEquals(byteArrayOf(8), backups.first().readBytes())
        assertArrayEquals(byteArrayOf(4), backups.last().readBytes())
        assertArrayEquals(byteArrayOf(9), r.readBatterySave("g"))
    }

    @Test fun noBackupWhenUnchangedOrNotRequested() {
        val r = repo()
        r.writeBatterySave("g", byteArrayOf(1), backup = true) // nothing to back up yet
        r.writeBatterySave("g", byteArrayOf(1), backup = true) // identical
        r.writeBatterySave("g", byteArrayOf(2), backup = false)
        assertTrue(r.backups("g").isEmpty())
    }

    @Test fun backupsWithSameTimestampDontCollide() {
        val r = repo()
        r.writeBatterySave("g", byteArrayOf(1), backup = true)
        r.writeBatterySave("g", byteArrayOf(2), backup = true)
        r.writeBatterySave("g", byteArrayOf(3), backup = true)
        assertEquals(2, r.backups("g").size)
    }

    @Test fun statesAndSlots() {
        val r = repo()
        r.writeState("g", 0, byteArrayOf(9), byteArrayOf(1))
        r.writeState("g", 3, byteArrayOf(7), null)
        val slots = r.slots("g")
        assertEquals(SaveRepository.SLOT_COUNT + 1, slots.size)
        assertTrue(slots[0].exists && slots[0].thumbnailPath != null)
        assertTrue(slots[3].exists && slots[3].thumbnailPath == null)
        assertFalse(slots[1].exists)
        assertTrue(r.hasAutoState("g"))
        r.deleteState("g", 3)
        assertNull(r.readState("g", 3))
        r.deleteAll("g")
        assertFalse(r.hasAutoState("g"))
    }
}
