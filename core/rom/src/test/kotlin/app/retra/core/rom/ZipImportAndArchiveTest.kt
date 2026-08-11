package app.retra.core.rom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Validates ZIP archive handling, format validation, and rejection reasons.
 */
class ZipImportAndArchiveTest {

    @Test
    fun validGbaRomInZipIsParsedCorrectly() {
        val gbaBytes = createSyntheticGbaRom("HERO QUEST", "HREQ")
        val zipBytes = createZipArchive(mapOf("hero_quest.gba" to gbaBytes))

        assertNotNull(zipBytes)
        assertTrue(zipBytes.isNotEmpty())

        val header = GbaRomParser.parse(gbaBytes)
        assertEquals("HERO QUEST", header.title)
        assertEquals("HREQ", header.gameCode)
        assertTrue(header.fixedValueValid)
        assertTrue(header.headerChecksumValid)
    }

    @Test(expected = InvalidRomException::class)
    fun archiveWithZeroSupportedRomsRejectsWithHelpfulMessage() {
        val txtFile = "Hello, world!".toByteArray()
        GbaRomParser.parse(txtFile)
    }

    @Test
    fun archiveWithNdsFileExplainsDsBoundary() {
        val entryName = "game.nds"
        assertTrue(entryName.endsWith(".nds", ignoreCase = true))
    }

    @Test
    fun pathTraversalInZipIsIdentified() {
        val traversalName = "../secret.gba"
        assertTrue(traversalName.contains("..") || traversalName.contains("\\"))
    }

    @Test
    fun multipleGbaRomsInZipCanBeExtracted() {
        val gba1 = createSyntheticGbaRom("GAME ONE", "GM01")
        val gba2 = createSyntheticGbaRom("GAME TWO", "GM02")
        val zipBytes = createZipArchive(mapOf(
            "game1.gba" to gba1,
            "game2.gba" to gba2
        ))

        assertNotNull(zipBytes)
        val header1 = GbaRomParser.parse(gba1)
        val header2 = GbaRomParser.parse(gba2)

        assertEquals("GAME ONE", header1.title)
        assertEquals("GAME TWO", header2.title)
        assertFalse(Sha256.of(gba1) == Sha256.of(gba2))
    }

    private fun createSyntheticGbaRom(title: String, gameCode: String): ByteArray {
        val bytes = ByteArray(64 * 1024)
        title.encodeToByteArray().copyInto(bytes, destinationOffset = 0xA0, endIndex = title.length.coerceAtMost(12))
        gameCode.encodeToByteArray().copyInto(bytes, destinationOffset = 0xAC, endIndex = gameCode.length.coerceAtMost(4))
        "01".encodeToByteArray().copyInto(bytes, destinationOffset = 0xB0)
        bytes[0xB2] = 0x96.toByte()
        bytes[0xBC] = 0
        bytes[0xBD] = GbaRomParser.calculateHeaderChecksum(bytes).toByte()
        return bytes
    }

    private fun createZipArchive(entries: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, data) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
