package app.retra.emulator.data

import android.content.Context
import android.net.Uri
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ContentDensity
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.ControlVisualStyle
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupExportReport(
    val gameMetadataCount: Int,
    val saveCount: Int,
    val artworkCount: Int,
    val totalEntries: Int
)

data class BackupImportReport(
    val savesRestored: Int,
    val artworkRestored: Int,
    val achievementsMerged: Int,
    val settingsRestored: Boolean,
    val skippedEntries: Int
)

/**
 * Creates and restores portable Retra progress bundles.
 *
 * ROM bytes and external source paths are deliberately excluded. A restored bundle becomes useful
 * after the player imports the matching game backup; saves and artwork are keyed by ROM SHA-256.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository,
    private val artworkRepository: ArtworkRepository,
    private val vaultRepository: VaultRepository
) {
    suspend fun export(destination: Uri, settings: AppSettings): Result<BackupExportReport> = withContext(Dispatchers.IO) {
        runCatching {
            val games = gameDao.getAll()
            val saveRoot = File(context.filesDir, "emulation")
            val artworkRoot = File(context.filesDir, "artwork").canonicalFile
            val saveFiles = if (saveRoot.isDirectory) {
                saveRoot.walkTopDown()
                    .maxDepth(4)
                    .filter { it.isFile && it.extension.equals("rsv", true) }
                    .filter { runCatching { SaveEnvelope.decode(it.readBytes()) }.isSuccess }
                    .toList()
            } else emptyList<File>()
            val artworkFiles = games.mapNotNull { game ->
                val path = game.coverArtPath ?: return@mapNotNull null
                val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@mapNotNull null
                if (!file.isFile || !file.path.startsWith(artworkRoot.path + File.separator)) return@mapNotNull null
                game.sha256.lowercase() to file
            }.distinctBy { it.first }

            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Android could not create the selected backup file.")
            var entryCount = 0
            output.buffered().use { buffered ->
                ZipOutputStream(buffered).use { zip ->
                    fun writeEntry(name: String, bytes: ByteArray) {
                        require(bytes.size <= MAX_SINGLE_ENTRY_BYTES) { "Backup entry $name is too large." }
                        val entry = ZipEntry(name).apply { time = 0L }
                        zip.putNextEntry(entry)
                        zip.write(bytes)
                        zip.closeEntry()
                        entryCount++
                    }

                    val manifest = JSONObject()
                        .put("format", FORMAT)
                        .put("version", FORMAT_VERSION)
                        .put("appVersion", "2.3.0")
                        .put("createdAtEpochMillis", System.currentTimeMillis())
                        .put("romsIncluded", false)
                        .put("gameMetadataCount", games.size)
                        .put("saveCount", saveFiles.size)
                        .put("artworkCount", artworkFiles.size)
                    writeEntry("manifest.json", manifest.toString(2).toByteArray(Charsets.UTF_8))
                    writeEntry("library/games.json", encodeGames(games).toString(2).toByteArray(Charsets.UTF_8))
                    writeEntry("settings/settings.json", encodeSettings(settings).toString(2).toByteArray(Charsets.UTF_8))
                    writeEntry("achievements/progress.tsv", achievementRepository.exportProgress())

                    saveFiles.forEach { file ->
                        val relative = file.relativeTo(saveRoot).invariantSeparatorsPath
                        require(SAFE_SAVE_PATH.matches(relative)) { "Unsafe save path was blocked." }
                        writeEntry("saves/$relative", file.readBytes())
                    }
                    artworkFiles.forEach { (hash, file) ->
                        writeEntry("artwork/$hash.jpg", file.readBytes())
                    }
                }
            }
            BackupExportReport(games.size, saveFiles.size, artworkFiles.size, entryCount)
        }
    }

    suspend fun import(source: Uri): Result<BackupImportReport> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(source)
                ?: error("Android could not open the selected backup file.")
            var manifest: JSONObject? = null
            var settingsBytes: ByteArray? = null
            var achievementBytes: ByteArray? = null
            val saves = mutableListOf<Pair<String, ByteArray>>()
            val artwork = mutableListOf<Pair<String, ByteArray>>()
            var skipped = 0
            var entries = 0
            var totalBytes = 0L

            input.buffered().use { buffered ->
                ZipInputStream(buffered).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        require(entries <= MAX_ENTRIES) { "Backup contains too many files." }
                        if (entry.isDirectory) continue
                        val name = normalizeZipPath(entry.name)
                        val bytes = zip.readEntryLimited(MAX_SINGLE_ENTRY_BYTES)
                        totalBytes += bytes.size
                        require(totalBytes <= MAX_TOTAL_IMPORT_BYTES) { "Backup expands beyond Retra's safety limit." }
                        when {
                            name == "manifest.json" -> manifest = JSONObject(bytes.toString(Charsets.UTF_8))
                            name == "settings/settings.json" -> settingsBytes = bytes
                            name == "achievements/progress.tsv" -> achievementBytes = bytes
                            name.startsWith("saves/") -> saves += name.removePrefix("saves/") to bytes
                            name.startsWith("artwork/") -> {
                                val hash = name.removePrefix("artwork/").substringBefore('.').lowercase()
                                if (SHA256.matches(hash)) artwork += hash to bytes else skipped++
                            }
                            name == "library/games.json" -> Unit // Metadata is for audit/review; no ROM-less library rows are created.
                            else -> skipped++
                        }
                    }
                }
            }

            val parsedManifest = requireNotNull(manifest) { "This is not a Retra backup: manifest.json is missing." }
            require(parsedManifest.optString("format") == FORMAT) { "Unsupported Retra backup format." }
            require(parsedManifest.optInt("version") == FORMAT_VERSION) { "Unsupported Retra backup version." }
            require(!parsedManifest.optBoolean("romsIncluded", true)) { "Backups containing ROM bytes are not accepted." }

            val saveStore = AtomicSaveStore(File(context.filesDir, "emulation"))
            var restoredSaves = 0
            for ((relative, bytes) in saves) {
                val safeRelative = normalizeZipPath(relative)
                if (!SAFE_SAVE_PATH.matches(safeRelative)) {
                    skipped++
                    continue
                }
                val envelope = runCatching { SaveEnvelope.decode(bytes) }.getOrNull()
                if (envelope == null) {
                    skipped++
                    continue
                }
                if (!safeRelative.substringBefore('/').equals(envelope.gameSha256, true)) {
                    skipped++
                    continue
                }
                saveStore.write(safeRelative, bytes)
                restoredSaves++
            }

            var restoredArtwork = 0
            for ((hash, bytes) in artwork) {
                val game = gameDao.getBySha256(hash)
                if (game == null) {
                    skipped++
                    continue
                }
                val imported = artworkRepository.importCoverArtBytes(game.id, hash, bytes).isSuccess
                if (imported) restoredArtwork++ else skipped++
            }

            val settingsRestored = settingsBytes?.let { bytes ->
                runCatching {
                    settingsRepository.replace(decodeSettings(JSONObject(bytes.toString(Charsets.UTF_8))))
                    true
                }.getOrDefault(false)
            } ?: false
            val achievementsMerged = achievementBytes?.let { bytes ->
                achievementRepository.importProgress(bytes)
            } ?: 0
            vaultRepository.refresh()

            BackupImportReport(
                savesRestored = restoredSaves,
                artworkRestored = restoredArtwork,
                achievementsMerged = achievementsMerged,
                settingsRestored = settingsRestored,
                skippedEntries = skipped
            )
        }
    }

    private fun encodeGames(games: List<GameEntity>): JSONArray = JSONArray().apply {
        games.forEach { game ->
            put(
                JSONObject()
                    .put("sha256", game.sha256)
                    .put("sha1", game.sha1)
                    .put("crc32", game.crc32)
                    .put("title", game.title)
                    .put("canonicalTitle", game.canonicalTitle)
                    .put("gameCode", game.gameCode)
                    .put("softwareVersion", game.softwareVersion)
                    .put("origin", game.origin)
                    .put("favorite", game.favorite)
                    .put("collections", JSONArray(GameEntity.decodeCsv(game.collectionsCsv)))
                    .put("tags", JSONArray(GameEntity.decodeCsv(game.tagsCsv)))
                    .put("baseSha256", game.baseSha256)
                    .put("patchSha256", game.patchSha256)
                    .put("patchFormat", game.patchFormat)
                    .put("patchDisplayName", game.patchDisplayName)
                    .put("creator", game.creator)
                    .put("sourceUrl", game.sourceUrl)
                    .put("license", game.license)
                    .put("importedAtEpochMillis", game.importedAtEpochMillis)
                    .put("lastPlayedAtEpochMillis", game.lastPlayedAtEpochMillis)
            )
        }
    }

    private fun encodeSettings(value: AppSettings): JSONObject = JSONObject()
        .put("onboardingComplete", value.onboardingComplete)
        .put("themeMode", value.themeMode.name)
        .put("libraryLayout", value.libraryLayout.name)
        .put("dynamicColor", value.dynamicColor)
        .put("reduceMotion", value.reduceMotion)
        .put("reduceTransparency", value.reduceTransparency)
        .put("fastForwardSpeed", value.fastForwardSpeed.toDouble())
        .put("performanceProfile", value.performanceProfile.name)
        .put("accentPalette", value.accentPalette.name)
        .put("contentDensity", value.contentDensity.name)
        .put("startupDestination", value.startupDestination.name)
        .put("glassIntensity", value.glassIntensity.toDouble())
        .put("cornerScale", value.cornerScale.toDouble())
        .put("fontScale", value.fontScale.toDouble())
        .put("touchControlOpacity", value.touchControlOpacity.toDouble())
        .put("touchControlScale", value.touchControlScale.toDouble())
        .put("touchControlSpacing", value.touchControlSpacing.toDouble())
        .put("touchDeadZone", value.touchDeadZone.toDouble())
        .put("controlLayoutPreset", value.controlLayoutPreset.name)
        .put("controlVisualStyle", value.controlVisualStyle.name)
        .put("screenScalingMode", value.screenScalingMode.name)
        .put("showShoulderButtons", value.showShoulderButtons)
        .put("showQuickActions", value.showQuickActions)
        .put("quickSaveEnabled", value.quickSaveEnabled)
        .put("autoSaveIntervalMinutes", value.autoSaveIntervalMinutes)
        .put("playerImmersiveMode", value.playerImmersiveMode)
        .put("hapticsEnabled", value.hapticsEnabled)
        .put("soundEffectsEnabled", value.soundEffectsEnabled)
        .put("soundEffectsVolume", value.soundEffectsVolume.toDouble())
        .put("notificationsEnabled", value.notificationsEnabled)
        .put("notifyAchievements", value.notifyAchievements)
        .put("notifyDownloads", value.notifyDownloads)
        .put("notifyMultiplayer", value.notifyMultiplayer)
        .put("highContrast", value.highContrast)
        .put("showOnlineRecommendations", value.showOnlineRecommendations)
        .put("showStatistics", value.showStatistics)
        .put("integerScaling", value.integerScaling)
        .put("displaySmoothing", value.displaySmoothing)
        .put("showPerformanceOverlay", value.showPerformanceOverlay)
        .put("showTouchControls", value.showTouchControls)
        .put("audioEnabled", value.audioEnabled)
        .put("masterVolume", value.masterVolume.toDouble())
        .put("autoSuspendOnBackground", value.autoSuspendOnBackground)
        .put("pauseOnHeadphoneDisconnect", value.pauseOnHeadphoneDisconnect)

    private fun decodeSettings(json: JSONObject): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            onboardingComplete = json.optBoolean("onboardingComplete", defaults.onboardingComplete),
            themeMode = json.optEnum("themeMode", defaults.themeMode),
            libraryLayout = json.optEnum("libraryLayout", defaults.libraryLayout),
            dynamicColor = json.optBoolean("dynamicColor", defaults.dynamicColor),
            reduceMotion = json.optBoolean("reduceMotion", defaults.reduceMotion),
            reduceTransparency = json.optBoolean("reduceTransparency", defaults.reduceTransparency),
            fastForwardSpeed = json.optDouble("fastForwardSpeed", defaults.fastForwardSpeed.toDouble()).toFloat().coerceIn(1f, 16f),
            performanceProfile = json.optEnum("performanceProfile", defaults.performanceProfile),
            accentPalette = json.optEnum("accentPalette", defaults.accentPalette),
            contentDensity = json.optEnum("contentDensity", defaults.contentDensity),
            startupDestination = json.optEnum("startupDestination", defaults.startupDestination),
            glassIntensity = json.optFloat("glassIntensity", defaults.glassIntensity, 0f..1f),
            cornerScale = json.optFloat("cornerScale", defaults.cornerScale, 0.75f..1.35f),
            fontScale = json.optFloat("fontScale", defaults.fontScale, 0.85f..1.3f),
            touchControlOpacity = json.optFloat("touchControlOpacity", defaults.touchControlOpacity, 0.25f..1f),
            touchControlScale = json.optFloat("touchControlScale", defaults.touchControlScale, 0.72f..1.35f),
            touchControlSpacing = json.optFloat("touchControlSpacing", defaults.touchControlSpacing, 0.72f..1.4f),
            touchDeadZone = json.optFloat("touchDeadZone", defaults.touchDeadZone, 0.05f..0.5f),
            controlLayoutPreset = json.optEnum("controlLayoutPreset", defaults.controlLayoutPreset),
            controlVisualStyle = json.optEnum("controlVisualStyle", defaults.controlVisualStyle),
            screenScalingMode = json.optEnum("screenScalingMode", defaults.screenScalingMode),
            showShoulderButtons = json.optBoolean("showShoulderButtons", defaults.showShoulderButtons),
            showQuickActions = json.optBoolean("showQuickActions", defaults.showQuickActions),
            quickSaveEnabled = json.optBoolean("quickSaveEnabled", defaults.quickSaveEnabled),
            autoSaveIntervalMinutes = json.optInt("autoSaveIntervalMinutes", defaults.autoSaveIntervalMinutes).coerceIn(0, 60),
            playerImmersiveMode = json.optBoolean("playerImmersiveMode", defaults.playerImmersiveMode),
            hapticsEnabled = json.optBoolean("hapticsEnabled", defaults.hapticsEnabled),
            soundEffectsEnabled = json.optBoolean("soundEffectsEnabled", defaults.soundEffectsEnabled),
            soundEffectsVolume = json.optFloat("soundEffectsVolume", defaults.soundEffectsVolume, 0f..1f),
            notificationsEnabled = json.optBoolean("notificationsEnabled", defaults.notificationsEnabled),
            notifyAchievements = json.optBoolean("notifyAchievements", defaults.notifyAchievements),
            notifyDownloads = json.optBoolean("notifyDownloads", defaults.notifyDownloads),
            notifyMultiplayer = json.optBoolean("notifyMultiplayer", defaults.notifyMultiplayer),
            highContrast = json.optBoolean("highContrast", defaults.highContrast),
            showOnlineRecommendations = json.optBoolean("showOnlineRecommendations", defaults.showOnlineRecommendations),
            showStatistics = json.optBoolean("showStatistics", defaults.showStatistics),
            integerScaling = json.optBoolean("integerScaling", defaults.integerScaling),
            displaySmoothing = json.optBoolean("displaySmoothing", defaults.displaySmoothing),
            showPerformanceOverlay = json.optBoolean("showPerformanceOverlay", defaults.showPerformanceOverlay),
            showTouchControls = json.optBoolean("showTouchControls", defaults.showTouchControls),
            audioEnabled = json.optBoolean("audioEnabled", defaults.audioEnabled),
            masterVolume = json.optFloat("masterVolume", defaults.masterVolume, 0f..1f),
            autoSuspendOnBackground = json.optBoolean("autoSuspendOnBackground", defaults.autoSuspendOnBackground),
            pauseOnHeadphoneDisconnect = json.optBoolean("pauseOnHeadphoneDisconnect", defaults.pauseOnHeadphoneDisconnect)
        )
    }

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, default: T): T =
        optString(key).takeIf(String::isNotBlank)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun JSONObject.optFloat(
        key: String,
        default: Float,
        range: ClosedFloatingPointRange<Float>
    ): Float = optDouble(key, default.toDouble()).toFloat().coerceIn(range.start, range.endInclusive)

    private fun normalizeZipPath(raw: String): String {
        require(raw.isNotBlank() && raw.length <= 512) { "Invalid backup path." }
        require('\\' !in raw && !raw.startsWith('/') && !raw.contains("../") && !raw.contains("//")) {
            "Unsafe backup path was blocked."
        }
        return raw.trim('/')
    }

    private fun ZipInputStream.readEntryLimited(maximum: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= maximum) { "Backup entry exceeds Retra's safety limit." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val FORMAT = "RETRA-BACKUP"
        const val FORMAT_VERSION = 1
        const val MAX_ENTRIES = 4_096
        const val MAX_SINGLE_ENTRY_BYTES = 128 * 1024 * 1024
        const val MAX_TOTAL_IMPORT_BYTES = 512L * 1024L * 1024L
        val SHA256 = Regex("[0-9a-f]{64}")
        val SAFE_SAVE_PATH = Regex("[0-9a-fA-F]{64}/(?:states/slot-[0-9]{1,2}|suspend/latest|battery/game)\\.rsv")
    }
}
