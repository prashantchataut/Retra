package app.retra.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates domain game records, compatibility statuses, and model transformations.
 */
class GameModelMappingTest {

    @Test
    fun gameRecordPreservesAllDomainFields() {
        val record = GameRecord(
            id = 42L,
            uri = "content://media/external/123",
            displayName = "Custom Game.gba",
            title = "Custom Game",
            gameCode = "CSTM",
            makerCode = "01",
            softwareVersion = 1,
            sha256 = "a".repeat(64),
            sizeBytes = 16_777_216L,
            importedAtEpochMillis = 1000L,
            lastPlayedAtEpochMillis = 2000L,
            compatibility = CompatibilityStatus.PLAYABLE,
            origin = "LOCAL_IMPORT",
            baseSha256 = "b".repeat(64),
            patchSha256 = "c".repeat(64),
            patchFormat = "UPS",
            patchDisplayName = "Custom Patch.ups",
            creator = "Test Creator",
            sourceUrl = "https://example.com",
            license = "CC-BY-4.0",
            distributionPermission = "Author allows redistribution",
            favorite = true,
            notes = "Runs at 60 FPS",
            coverArtPath = "/data/user/0/cover.png",
            crc32 = 0x12345678L,
            sha1 = "d".repeat(40),
            canonicalTitle = "Canonical Game Title",
            metadataSource = "Libretro DAT",
            managedPath = "/data/user/0/rom.gba",
            collections = listOf("RPG", "Favorites"),
            tags = listOf("retro", "handheld")
        )

        assertEquals(42L, record.id)
        assertEquals("content://media/external/123", record.uri)
        assertEquals("Custom Game.gba", record.displayName)
        assertEquals("Custom Game", record.title)
        assertEquals("CSTM", record.gameCode)
        assertEquals(CompatibilityStatus.PLAYABLE, record.compatibility)
        assertEquals("LOCAL_IMPORT", record.origin)
        assertEquals("b".repeat(64), record.baseSha256)
        assertEquals("c".repeat(64), record.patchSha256)
        assertEquals("UPS", record.patchFormat)
        assertEquals("Custom Patch.ups", record.patchDisplayName)
        assertEquals("Test Creator", record.creator)
        assertEquals("https://example.com", record.sourceUrl)
        assertEquals("CC-BY-4.0", record.license)
        assertEquals("Author allows redistribution", record.distributionPermission)
        assertTrue(record.favorite)
        assertEquals("Runs at 60 FPS", record.notes)
        assertEquals("/data/user/0/cover.png", record.coverArtPath)
        assertEquals(0x12345678L, record.crc32)
        assertEquals("d".repeat(40), record.sha1)
        assertEquals("Canonical Game Title", record.canonicalTitle)
        assertEquals("Libretro DAT", record.metadataSource)
        assertEquals("/data/user/0/rom.gba", record.managedPath)
        assertEquals(listOf("RPG", "Favorites"), record.collections)
        assertEquals(listOf("retro", "handheld"), record.tags)
    }

    @Test
    fun compatibilityStatusEnumValuesAreComplete() {
        val values = CompatibilityStatus.entries
        assertTrue(values.contains(CompatibilityStatus.PLAYABLE))
        assertTrue(values.contains(CompatibilityStatus.UNKNOWN))
        assertTrue(values.contains(CompatibilityStatus.PERFECT))
        assertTrue(values.contains(CompatibilityStatus.BROKEN))
    }
}
