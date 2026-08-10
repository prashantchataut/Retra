package app.retra.core.download

import app.retra.core.model.CatalogContentKind
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CompatibilityStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDownloadPolicyTest {
    @Test
    fun acceptsVerifiedGbaZipAndPatchUrls() {
        for (extension in listOf(".gba", ".zip", ".ups", ".ips", ".bps")) {
            val entry = sampleEntry(
                url = "https://cdn.example.org/files/fixture$extension",
                kind = when (extension) {
                    ".gba" -> CatalogContentKind.GAME
                    ".zip" -> CatalogContentKind.ARCHIVE
                    else -> CatalogContentKind.PATCH
                }
            )
            val uri = CatalogDownloadPolicy.validateEntry(entry)
            assertTrue(uri.path.endsWith(extension))
        }
    }

    @Test(expected = UnsafeDownloadException::class)
    fun blocksExternalKindFromInAppDownload() {
        CatalogDownloadPolicy.validateEntry(
            sampleEntry(
                url = "https://cdn.example.org/files/fixture.gba",
                kind = CatalogContentKind.EXTERNAL
            )
        )
    }

    @Test(expected = UnsafeDownloadException::class)
    fun blocksPrivateNetworkTargets() {
        CatalogDownloadPolicy.validateEntry(
            sampleEntry(url = "https://127.0.0.1/files/fixture.gba")
        )
    }

    @Test
    fun allowsGithubAssetRedirectHop() {
        val origin = CatalogDownloadPolicy.validateEntry(
            sampleEntry(url = "https://github.com/org/repo/releases/download/v1/fixture.gba")
        )
        val next = CatalogDownloadPolicy.validateRedirect(
            origin,
            origin,
            "https://objects.githubusercontent.com/github-production-release-asset/1/fixture.gba",
            setOf(origin)
        )
        assertTrue(next.host.contains("githubusercontent"))
    }

    private fun sampleEntry(
        url: String,
        kind: CatalogContentKind = CatalogContentKind.GAME
    ) = CatalogEntry(
        id = "fixture-1",
        title = "Fixture",
        description = "Policy test fixture",
        creator = "Retra",
        version = "1.0.0",
        downloadUrl = url,
        sha256 = "a".repeat(64),
        fileSize = 1_024,
        license = "CC0-1.0",
        distributionPermission = "Redistribution permitted for this synthetic fixture.",
        tags = listOf("test"),
        compatibility = CompatibilityStatus.UNKNOWN,
        contentKind = kind,
        sourcePageUrl = if (kind == CatalogContentKind.EXTERNAL) "https://example.org/releases" else null
    )
}
