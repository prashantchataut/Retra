package app.retra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Parses the real catalog/manifest.json shipped in the repo (and bundled in the APK). */
class CatalogParserTest {
    private val json = File("../catalog/manifest.json").readText()
    private val catalog = (CatalogParser.parse(json) as CatalogParser.Result.Ok).catalog

    @Test fun parsesRealManifest() {
        assertEquals(1, catalog.schema)
        assertTrue(catalog.entries.size >= 20)
        assertEquals(listOf("featured", "pokemon", "challenge", "homebrew"), catalog.collections.map { it.id })
    }

    @Test fun everyShelfEntryResolves() {
        for (c in catalog.collections) {
            assertEquals("shelf ${c.id}", c.entryIds.size, catalog.shelf(c).size)
        }
    }

    @Test fun contentRulesHold() {
        for (e in catalog.entries) {
            assertTrue(e.homepage.startsWith("https://"))
            e.download?.let { d ->
                assertTrue(d.url.startsWith("https://"))
                assertEquals(64, d.sha256.length)
                assertTrue(d.size > 0)
                if (d.format == "zip") assertNotNull("zip needs archiveEntry: ${e.id}", d.archiveEntry)
            }
            if (e.isPatch) {
                val base = assertNotNullAnd(e.base)
                assertTrue(base.crc32.matches(Regex("[0-9A-F]{8}")))
                assertEquals(4, base.gameCode.length)
                assertEquals(null, e.license) // ROM hacks are never redistributed
            }
        }
    }

    @Test fun mochiHopIsBundledHomebrew() {
        val m = catalog.entry("mochi-hop")!!
        assertTrue(m.isRom && m.bundled && m.featured)
        assertEquals(217088L, m.download!!.size)
        assertEquals(0xFFFFD6E0.toInt(), m.accentArgb)
    }

    @Test fun patchWithZipDownloadKeepsArchiveFields() {
        val clover = catalog.entry("pokemon-clover")!!
        assertEquals("zip", clover.download!!.format)
        assertEquals("Clover v1.3.3.bps", clover.download!!.archiveEntry)
        assertEquals("DD88761C", clover.base!!.crc32)
        val rr = catalog.entry("pokemon-radical-red")!!
        assertEquals(null, rr.download) // open the creator's page instead
    }

    @Test fun rejectsNewerSchemaAndGarbage() {
        assertTrue(CatalogParser.parse("""{"schema":2,"entries":[]}""") is CatalogParser.Result.Error)
        assertTrue(CatalogParser.parse("not json") is CatalogParser.Result.Error)
    }

    @Test fun dropsUnsafeEntries() {
        val r = CatalogParser.parse(
            """{"schema":1,"collections":[],"entries":[
              {"id":"ok","kind":"rom","title":"A","author":"x","homepage":"https://a","download":null},
              {"id":"http","kind":"rom","title":"B","author":"x","homepage":"https://a","download":{"url":"http://insecure","size":1,"sha256":"00","format":"gba"}},
              {"id":"nobase","kind":"patch","title":"C","author":"x","homepage":"https://a","base":null}
            ]}""",
        ) as CatalogParser.Result.Ok
        assertEquals(listOf("ok"), r.catalog.entries.map { it.id })
        assertFalse(r.catalog.entries.any { it.id == "http" })
    }

    @Test fun bundledAssetCopyMatchesSource() {
        // The Gradle task copies catalog/manifest.json into the APK assets; make sure nobody parks a stale copy in src.
        assertFalse(File("src/main/assets/catalog/manifest.json").exists())
    }

    private fun <T> assertNotNullAnd(v: T?): T { assertNotNull(v); return v!! }
}
