
package app.retra.core.model

enum class CompatibilityStatus {
    UNKNOWN,
    PLAYABLE,
    PERFECT,
    BROKEN
}

enum class PerformanceProfile {
    AUTHENTIC,
    BALANCED,
    BOOSTED,
    EXTREME,
    BATTERY_SAVER
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED
}

enum class LibraryLayout {
    LARGE_GRID,
    COMPACT_GRID,
    DETAILED_LIST
}

enum class AccentPalette {
    RETRA_PRISM,
    AURORA,
    EMERALD_CARTRIDGE,
    SUNSET_GOLD,
    ATOMIC_PURPLE,
    GLACIER,
    CLASSIC_GRAY
}

enum class ContentDensity {
    COMFORTABLE,
    BALANCED,
    COMPACT
}

enum class StartupDestination {
    HOME,
    LIBRARY,
    CONTINUE_PLAYING
}

data class RomHeader(
    val title: String,
    val gameCode: String,
    val makerCode: String,
    val softwareVersion: Int,
    val fixedValueValid: Boolean,
    val headerChecksumValid: Boolean
)

data class GameRecord(
    val id: Long = 0,
    val uri: String,
    val displayName: String,
    val title: String,
    val gameCode: String,
    val makerCode: String,
    val softwareVersion: Int,
    val sha256: String,
    val sizeBytes: Long,
    val importedAtEpochMillis: Long,
    val lastPlayedAtEpochMillis: Long? = null,
    val compatibility: CompatibilityStatus = CompatibilityStatus.UNKNOWN,
    val origin: String = "LOCAL_IMPORT",
    val baseSha256: String? = null,
    val patchSha256: String? = null,
    val patchFormat: String? = null,
    val patchDisplayName: String? = null,
    val creator: String? = null,
    val sourceUrl: String? = null,
    val license: String? = null,
    val distributionPermission: String? = null,
    val favorite: Boolean = false,
    val notes: String? = null,
    val coverArtPath: String? = null
)

data class CatalogEntry(
    val id: String,
    val title: String,
    val description: String,
    val creator: String,
    val version: String,
    val downloadUrl: String,
    val sha256: String,
    val fileSize: Long,
    val license: String,
    val distributionPermission: String,
    val artworkUrl: String? = null,
    val tags: List<String>,
    val compatibility: CompatibilityStatus
)

data class CatalogManifest(
    val catalogVersion: Int,
    val catalogId: String,
    val name: String,
    val description: String,
    val owner: String,
    val sourceUrl: String,
    val contentPolicy: String,
    val games: List<CatalogEntry>
)

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val libraryLayout: LibraryLayout = LibraryLayout.LARGE_GRID,
    val dynamicColor: Boolean = false,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val fastForwardSpeed: Float = 2f,
    val performanceProfile: PerformanceProfile = PerformanceProfile.BALANCED,
    val accentPalette: AccentPalette = AccentPalette.RETRA_PRISM,
    val contentDensity: ContentDensity = ContentDensity.BALANCED,
    val startupDestination: StartupDestination = StartupDestination.HOME,
    val glassIntensity: Float = 0.62f,
    val cornerScale: Float = 1f,
    val fontScale: Float = 1f,
    val touchControlOpacity: Float = 0.72f,
    val hapticsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val soundEffectsVolume: Float = 0.55f,
    val notificationsEnabled: Boolean = true,
    val notifyAchievements: Boolean = true,
    val notifyDownloads: Boolean = true,
    val notifyMultiplayer: Boolean = true,
    val highContrast: Boolean = false,
    val showOnlineRecommendations: Boolean = true,
    val showStatistics: Boolean = true,
    val integerScaling: Boolean = true,
    val displaySmoothing: Boolean = false,
    val showPerformanceOverlay: Boolean = false,
    val showTouchControls: Boolean = true,
    val audioEnabled: Boolean = true,
    val masterVolume: Float = 1f,
    val autoSuspendOnBackground: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true
)
