package app.retra.core.cheats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetraCheatCatalogTest {
    private val gameHash = "a".repeat(64)
    private val packHash = "b".repeat(64)

    @Test
    fun parsesAndMatchesExactRom() {
        val catalog = RetraCheatCatalogParser.parse(validCatalog().toByteArray())
        assertEquals("community", catalog.catalogId)
        assertEquals(1, catalog.entries.size)
        assertTrue(RetraCheatCatalogParser.matches(catalog.entries.single(), gameHash, "BPEE", 0))
        assertFalse(RetraCheatCatalogParser.matches(catalog.entries.single(), "c".repeat(64), "BPEE", 0))
    }

    @Test(expected = InvalidCheatCatalogException::class)
    fun rejectsInsecurePackUrl() {
        RetraCheatCatalogParser.parse(validCatalog().replace("https://example.com/pack.rcc", "http://example.com/pack.rcc").toByteArray())
    }

    @Test(expected = InvalidCheatCatalogException::class)
    fun rejectsDuplicateFields() {
        RetraCheatCatalogParser.parse(validCatalog().replace("name=Community packs", "name=Community packs\nname=Duplicate").toByteArray())
    }

    private fun validCatalog() = """
        RETRA-CHEAT-INDEX-1
        catalogId=community
        name=Community packs
        provider=Example provider
        sourcePageUrl=https://example.com/catalog
        [pack]
        id=emerald-qol
        title=Quality of life
        description=Small reversible helpers.
        gameSha256=$gameHash
        gameCode=BPEE
        revision=0
        downloadUrl=https://example.com/pack.rcc
        packSha256=$packHash
        license=CC0
        distributionPermission=The provider authorizes redistribution of this declarative pack.
        sourcePageUrl=https://example.com/pack
        [/pack]
    """.trimIndent()
}
