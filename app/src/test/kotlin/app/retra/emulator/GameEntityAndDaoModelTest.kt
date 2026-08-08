package app.retra.emulator

import app.retra.core.model.CompatibilityStatus
import app.retra.emulator.data.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Room entity to domain record mapping, CSV tag encoding/decoding,
 * and database v6 metadata integrity.
 */
class GameEntityAndDaoModelTest {

    @Test
    fun entityToRecordPreservesAllV6Fields() {
        val entity = GameEntity(
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
            compatibility = CompatibilityStatus.PLAYABLE.name,
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
            collectionsCsv = "RPG|Favorites",
            tagsCsv = "retro|handheld"
        )

        val record = entity.toRecord()

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
    fun csvEncodingAndDecodingHandlesSpecialCases() {
        val original = listOf("Action", "Adventure", "RPG")
        val encoded = GameEntity.encodeCsv(original)
        assertEquals("Action|Adventure|RPG", encoded)

        val decoded = GameEntity.decodeCsv(encoded)
        assertEquals(original, decoded)

        // Empty string decodes to empty list
        assertEquals(emptyList<String>(), GameEntity.decodeCsv(""))

        // Whitespace and duplicates trimmed
        val messy = listOf("  Alpha  ", "Beta", "Alpha", "")
        val cleanEncoded = GameEntity.encodeCsv(messy)
        assertEquals("Alpha|Beta", cleanEncoded)
    }
}
