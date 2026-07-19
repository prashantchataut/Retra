
package app.retra.core.rom

import app.retra.core.model.CatalogEntry
import app.retra.core.model.CatalogManifest
import app.retra.core.model.CompatibilityStatus
import kotlin.test.Test
import kotlin.test.assertIs

class CatalogValidatorTest {
    @Test
    fun acceptsAuthorizedHttpsCatalog() {
        assertIs<CatalogValidationResult.Valid>(CatalogValidator.validate(validManifest()))
    }

    @Test
    fun rejectsHttpDownload() {
        val manifest = validManifest().copy(
            games = validManifest().games.map { it.copy(downloadUrl = "http://example.com/test.gba") }
        )
        assertIs<CatalogValidationResult.Invalid>(CatalogValidator.validate(manifest))
    }

    private fun validManifest() = CatalogManifest(
        catalogVersion = 1,
        catalogId = "retra-test",
        name = "Retra Test Catalog",
        description = "Synthetic test content only.",
        owner = "Retra",
        sourceUrl = "https://example.com/catalog.json",
        contentPolicy = "AUTHORIZED_ONLY",
        games = listOf(
            CatalogEntry(
                id = "test-cartridge",
                title = "Header Test Cartridge",
                description = "Synthetic parser fixture.",
                creator = "Retra",
                version = "1.0",
                downloadUrl = "https://example.com/test.gba",
                sha256 = "a".repeat(64),
                fileSize = 1024,
                license = "CC0-1.0",
                distributionPermission = "Author permits redistribution.",
                tags = listOf("test"),
                compatibility = CompatibilityStatus.UNKNOWN
            )
        )
    )
}
