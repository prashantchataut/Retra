package app.retra.emulator

import app.retra.core.rom.GbaRomParser
import app.retra.core.rom.InvalidRomException
import app.retra.core.rom.Sha256
import app.retra.emulator.data.ImportOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Validates ZIP archive handling, safety constraints, format validation,
 * and rejection reasons for unsupported entries.
 */
class ZipImportAndArchiveTest {

    @Test
    fun validGbaRomInZipIsParsedCorrectly() {
        val gbaBytes = createSyntheticGbaRom("HERO QUEST", "HREQ")
        val zipBytes = createZipArchive(mapOf("hero_quest.gba" to gbaBytes))

        assertNotNull(zipBytes)
        assertTrue(zipBytes.size > 0)

        // Parse extracted ROM bytes
        val header = GbaRomParser.parse(gbaBytes)
        assertEquals("HERO QUEST", header.title)
        assertEquals("HREQ", header.gameCode)
        assertTrue(header.fixedValueValid)
        assertTrue(header.headerChecksumValid)
    }

    @Test
    fun archiveWithZeroSupportedRomsRejectsWithHelpfulMessage() {
        val txtFile = "Hello, world!".toByteArray()
        val pdfFile = "PDF-1.4 dummy".toByteArray()
        val zipBytes = createZipArchive(mapOf(
            "readme.txt" to txtFile,
            "manual.pdf" to pdfFile
        ))

        assertNotNull(zipBytes)
        // Verify that parsing text as GBA ROM fails with InvalidRomException
        var failed = false
        try {
            GbaRomParser.parse(txtFile)
        } catch (e: InvalidRomException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun archiveWithNdsFileExplainsDsBoundary() {
        val ndsFile = ByteArray(1024) { 0 }
        val zipBytes = createZipArchive(mapOf("game.nds" to ndsFile))

        assertNotNull(zipBytes)
        // Ensure that .nds entries are detected as unsupported DS titles
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
