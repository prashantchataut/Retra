package app.retra.emulator

import app.retra.emulator.data.HomebrewHubEntry
import app.retra.emulator.data.HomebrewHubFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Homebrew Hub direct-install eligibility policies,
 * checksum pinning, and source-page only boundaries.
 */
class HomebrewHubAndCatalogPolicyTest {

    @Test
    fun eligibleHomebrewEntryPermitsDirectInstall() {
        val entry = HomebrewHubEntry(
            slug = "anguna-gba",
            title = "Anguna",
            developer = "gauauu",
            license = "Free distribution permitted by author",
            platform = "GBA",
            typeTag = "game",
            repository = "https://gauauu.itch.io/anguna",
            distributionPermission = "Author permits redistribution for non-commercial play.",
            screenshots = listOf("anguna_1.png"),
            tags = listOf("adventure", "homebrew"),
            files = listOf(
                HomebrewHubFile(
                    filename = "anguna.gba",
                    playable = true,
                    isDefault = true,
                    publishedSha256 = "a".repeat(64),
                    publishedSizeBytes = 4 * 1024 * 1024L
                )
            )
        )

        assertTrue(entry.directInstallEligible)
        assertNotNull(entry.defaultPlayableGba)
        assertEquals("anguna.gba", entry.defaultPlayableGba?.filename)
    }

    @Test
    fun missingDistributionPermissionRequiresSourcePage() {
        val entry = HomebrewHubEntry(
            slug = "unauthorized-rom-hack",
            title = "Unauthorized Hack",
            developer = "Anonymous",
            license = "Unknown",
            platform = "GBA",
            typeTag = "game",
            repository = "https://example.com/hack",
            distributionPermission = null, // No explicit permission
            screenshots = emptyList(),
            tags = listOf("hack"),
            files = listOf(
                HomebrewHubFile(
                    filename = "hack.gba",
                    playable = true,
                    isDefault = true,
                    publishedSha256 = "b".repeat(64),
                    publishedSizeBytes = 16 * 1024 * 1024L
                )
            )
        )

        assertFalse(entry.directInstallEligible)
    }

    @Test
    fun nonGbaEntryIsNotDirectInstallEligible() {
        val entry = HomebrewHubEntry(
            slug = "ds-homebrew",
            title = "DS Homebrew",
            developer = "Author",
            license = "MIT",
            platform = "NDS", // Not GBA
            typeTag = "homebrew",
            repository = "https://example.com/ds",
            distributionPermission = "Permission granted",
            screenshots = emptyList(),
            tags = listOf("ds"),
            files = listOf(
                HomebrewHubFile(
                    filename = "app.nds",
                    playable = true,
                    isDefault = true,
                    publishedSha256 = "c".repeat(64),
                    publishedSizeBytes = 8 * 1024 * 1024L
                )
            )
        )

        assertFalse(entry.directInstallEligible)
    }

    private fun assertNotNull(value: Any?) {
        assertTrue(value != null)
    }
}
