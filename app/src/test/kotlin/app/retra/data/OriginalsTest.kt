package app.retra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Keeps the bundled Retra Originals, the APK assets and the catalog's download metadata in sync. */
class OriginalsTest {
    private val catalog: Catalog =
        (CatalogParser.parse(File("../catalog/manifest.json").readText()) as CatalogParser.Result.Ok).catalog

    @Test
    fun everyOriginalShipsWithItsRomAndCover() {
        assertTrue("Retra should ship at least one original", Originals.all.isNotEmpty())
        for (original in Originals.all) {
            val rom = File("src/main/assets", original.romAsset)
            val cover = File("src/main/assets", original.coverAsset)
            assertTrue("${original.title} ROM is missing at ${rom.path}", rom.isFile && rom.length() > 0)
            assertTrue("${original.title} cover is missing at ${cover.path}", cover.isFile && cover.length() > 0)
            val entry = requireNotNull(catalog.entry(original.catalogId)) { "${original.title} is not in the catalog" }
            assertTrue("${entry.title} is in the catalog but not marked bundled", entry.bundled)
        }
    }

    @Test
    fun everyBundledCatalogEntryShipsInTheApk() {
        val bundled = catalog.entries.filter { it.bundled }
        assertEquals(
            "Every bundled catalog entry needs an Original and vice versa",
            Originals.all.map { it.catalogId }.toSet(),
            bundled.map { it.id }.toSet(),
        )
        for (entry in bundled) {
            val original = requireNotNull(Originals.byCatalogId(entry.id)) { "${entry.title} is marked bundled but has no APK asset" }
            val rom = File("src/main/assets", original.romAsset)
            assertTrue("${entry.title} ROM is missing at ${rom.path}", rom.isFile && rom.length() > 0)
            val download = requireNotNull(entry.download) { "${entry.title} is bundled but has no download metadata" }
            assertEquals("${entry.title} download size must match the shipped ROM", rom.length(), download.size)
            assertEquals("${entry.title} download SHA-256 must match the shipped ROM", sha256(rom), download.sha256)
            val fileName = download.url.substringAfterLast('/')
            assertTrue(
                "${entry.title} download URL should point at catalog/games/$fileName",
                download.url.endsWith("/catalog/games/$fileName"),
            )
            val repoRom = File("../catalog/games/$fileName")
            assertTrue("${entry.title} download is missing at ${repoRom.path}", repoRom.isFile)
            assertEquals("${entry.title} download must match the shipped ROM", sha256(rom), sha256(repoRom))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
