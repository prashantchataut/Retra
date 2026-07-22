
package app.retra.emulator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.retraDataStore by preferencesDataStore(name = "retra_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val themeMode = stringPreferencesKey("theme_mode")
        val libraryLayout = stringPreferencesKey("library_layout")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val reduceTransparency = booleanPreferencesKey("reduce_transparency")
        val fastForwardSpeed = floatPreferencesKey("fast_forward_speed")
        val performanceProfile = stringPreferencesKey("performance_profile")
        val accentPalette = stringPreferencesKey("accent_palette")
        val contentDensity = stringPreferencesKey("content_density")
        val startupDestination = stringPreferencesKey("startup_destination")
        val glassIntensity = floatPreferencesKey("glass_intensity")
        val cornerScale = floatPreferencesKey("corner_scale")
        val fontScale = floatPreferencesKey("font_scale")
        val touchControlOpacity = floatPreferencesKey("touch_control_opacity")
        val touchControlScale = floatPreferencesKey("touch_control_scale")
        val touchControlSpacing = floatPreferencesKey("touch_control_spacing")
        val touchDeadZone = floatPreferencesKey("touch_dead_zone")
        val controlLayoutPreset = stringPreferencesKey("control_layout_preset")
        val controlVisualStyle = stringPreferencesKey("control_visual_style")
        val screenScalingMode = stringPreferencesKey("screen_scaling_mode")
        val showShoulderButtons = booleanPreferencesKey("show_shoulder_buttons")
        val showQuickActions = booleanPreferencesKey("show_quick_actions")
        val quickSaveEnabled = booleanPreferencesKey("quick_save_enabled")
        val autoSaveIntervalMinutes = intPreferencesKey("auto_save_interval_minutes")
        val playerImmersiveMode = booleanPreferencesKey("player_immersive_mode")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val soundEffectsEnabled = booleanPreferencesKey("sound_effects_enabled")
        val soundEffectsVolume = floatPreferencesKey("sound_effects_volume")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val notifyAchievements = booleanPreferencesKey("notify_achievements")
        val notifyDownloads = booleanPreferencesKey("notify_downloads")
        val notifyMultiplayer = booleanPreferencesKey("notify_multiplayer")
        val highContrast = booleanPreferencesKey("high_contrast")
        val showOnlineRecommendations = booleanPreferencesKey("show_online_recommendations")
        val showStatistics = booleanPreferencesKey("show_statistics")
        val integerScaling = booleanPreferencesKey("integer_scaling")
        val displaySmoothing = booleanPreferencesKey("display_smoothing")
        val showPerformanceOverlay = booleanPreferencesKey("show_performance_overlay")
        val showTouchControls = booleanPreferencesKey("show_touch_controls")
        val audioEnabled = booleanPreferencesKey("audio_enabled")
        val masterVolume = floatPreferencesKey("master_volume")
        val autoSuspendOnBackground = booleanPreferencesKey("auto_suspend_on_background")
        val pauseOnHeadphoneDisconnect = booleanPreferencesKey("pause_on_headphone_disconnect")
    }

    val settings: Flow<AppSettings> = context.retraDataStore.data.map { preferences ->
        AppSettings(
            onboardingComplete = preferences[Keys.onboardingComplete] ?: false,
            themeMode = preferences[Keys.themeMode].enumOrDefault(ThemeMode.SYSTEM),
            libraryLayout = preferences[Keys.libraryLayout].enumOrDefault(LibraryLayout.LARGE_GRID),
            dynamicColor = preferences[Keys.dynamicColor] ?: false,
            reduceMotion = preferences[Keys.reduceMotion] ?: false,
            reduceTransparency = preferences[Keys.reduceTransparency] ?: false,
            fastForwardSpeed = preferences[Keys.fastForwardSpeed] ?: 2f,
            performanceProfile = preferences[Keys.performanceProfile].performanceProfileOrDefault(),
            accentPalette = preferences[Keys.accentPalette].mapAccentPalette(),
            contentDensity = preferences[Keys.contentDensity].enumOrDefault(ContentDensity.COMFORTABLE),
            startupDestination = preferences[Keys.startupDestination].enumOrDefault(StartupDestination.HOME),
            glassIntensity = (preferences[Keys.glassIntensity] ?: 0.28f).coerceIn(0f, 1f),
            cornerScale = (preferences[Keys.cornerScale] ?: 1f).coerceIn(0.75f, 1.35f),
            fontScale = (preferences[Keys.fontScale] ?: 1f).coerceIn(0.85f, 1.3f),
            touchControlOpacity = (preferences[Keys.touchControlOpacity] ?: 0.72f).coerceIn(0.25f, 1f),
            touchControlScale = (preferences[Keys.touchControlScale] ?: 1f).coerceIn(0.72f, 1.35f),
            touchControlSpacing = (preferences[Keys.touchControlSpacing] ?: 1f).coerceIn(0.72f, 1.4f),
            touchDeadZone = (preferences[Keys.touchDeadZone] ?: 0.18f).coerceIn(0.05f, 0.5f),
            controlLayoutPreset = preferences[Keys.controlLayoutPreset].enumOrDefault(ControlLayoutPreset.CLASSIC),
            controlVisualStyle = preferences[Keys.controlVisualStyle].enumOrDefault(ControlVisualStyle.GLASS),
            screenScalingMode = preferences[Keys.screenScalingMode].enumOrDefault(ScreenScalingMode.INTEGER),
            showShoulderButtons = preferences[Keys.showShoulderButtons] ?: true,
            showQuickActions = preferences[Keys.showQuickActions] ?: true,
            quickSaveEnabled = preferences[Keys.quickSaveEnabled] ?: true,
            autoSaveIntervalMinutes = (preferences[Keys.autoSaveIntervalMinutes] ?: 5).coerceIn(0, 60),
            playerImmersiveMode = preferences[Keys.playerImmersiveMode] ?: true,
            hapticsEnabled = preferences[Keys.hapticsEnabled] ?: true,
            soundEffectsEnabled = preferences[Keys.soundEffectsEnabled] ?: true,
            soundEffectsVolume = (preferences[Keys.soundEffectsVolume] ?: 0.45f).coerceIn(0f, 1f),
            notificationsEnabled = preferences[Keys.notificationsEnabled] ?: true,
            notifyAchievements = preferences[Keys.notifyAchievements] ?: true,
            notifyDownloads = preferences[Keys.notifyDownloads] ?: true,
            notifyMultiplayer = preferences[Keys.notifyMultiplayer] ?: true,
            highContrast = preferences[Keys.highContrast] ?: false,
            showOnlineRecommendations = preferences[Keys.showOnlineRecommendations] ?: true,
            showStatistics = preferences[Keys.showStatistics] ?: true,
            integerScaling = preferences[Keys.integerScaling] ?: true,
            displaySmoothing = preferences[Keys.displaySmoothing] ?: false,
            showPerformanceOverlay = preferences[Keys.showPerformanceOverlay] ?: false,
            showTouchControls = preferences[Keys.showTouchControls] ?: true,
            audioEnabled = preferences[Keys.audioEnabled] ?: true,
            masterVolume = (preferences[Keys.masterVolume] ?: 1f).coerceIn(0f, 1f),
            autoSuspendOnBackground = preferences[Keys.autoSuspendOnBackground] ?: true,
            pauseOnHeadphoneDisconnect = preferences[Keys.pauseOnHeadphoneDisconnect] ?: true
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) = edit { it[Keys.onboardingComplete] = value }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }
    suspend fun setLibraryLayout(value: LibraryLayout) = edit { it[Keys.libraryLayout] = value.name }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.dynamicColor] = value }
    suspend fun setReduceMotion(value: Boolean) = edit { it[Keys.reduceMotion] = value }
    suspend fun setReduceTransparency(value: Boolean) = edit { it[Keys.reduceTransparency] = value }
    suspend fun setFastForwardSpeed(value: Float) = edit { it[Keys.fastForwardSpeed] = value.coerceIn(1f, 16f) }
    suspend fun setPerformanceProfile(value: PerformanceProfile) = edit { it[Keys.performanceProfile] = value.stableProfile().name }
    suspend fun setAccentPalette(value: AccentPalette) = edit { it[Keys.accentPalette] = value.name }
    suspend fun setContentDensity(value: ContentDensity) = edit { it[Keys.contentDensity] = value.name }
    suspend fun setStartupDestination(value: StartupDestination) = edit { it[Keys.startupDestination] = value.name }
    suspend fun setGlassIntensity(value: Float) = edit { it[Keys.glassIntensity] = value.coerceIn(0f, 1f) }
    suspend fun setCornerScale(value: Float) = edit { it[Keys.cornerScale] = value.coerceIn(0.75f, 1.35f) }
    suspend fun setFontScale(value: Float) = edit { it[Keys.fontScale] = value.coerceIn(0.85f, 1.3f) }
    suspend fun setTouchControlOpacity(value: Float) = edit { it[Keys.touchControlOpacity] = value.coerceIn(0.25f, 1f) }
    suspend fun setTouchControlScale(value: Float) = edit { it[Keys.touchControlScale] = value.coerceIn(0.72f, 1.35f) }
    suspend fun setTouchControlSpacing(value: Float) = edit { it[Keys.touchControlSpacing] = value.coerceIn(0.72f, 1.4f) }
    suspend fun setTouchDeadZone(value: Float) = edit { it[Keys.touchDeadZone] = value.coerceIn(0.05f, 0.5f) }
    suspend fun setControlLayoutPreset(value: ControlLayoutPreset) = edit { it[Keys.controlLayoutPreset] = value.name }
    suspend fun setControlVisualStyle(value: ControlVisualStyle) = edit { it[Keys.controlVisualStyle] = value.name }
    suspend fun setScreenScalingMode(value: ScreenScalingMode) = edit { it[Keys.screenScalingMode] = value.name }
    suspend fun setShowShoulderButtons(value: Boolean) = edit { it[Keys.showShoulderButtons] = value }
    suspend fun setShowQuickActions(value: Boolean) = edit { it[Keys.showQuickActions] = value }
    suspend fun setQuickSaveEnabled(value: Boolean) = edit { it[Keys.quickSaveEnabled] = value }
    suspend fun setAutoSaveIntervalMinutes(value: Int) = edit { it[Keys.autoSaveIntervalMinutes] = value.coerceIn(0, 60) }
    suspend fun setPlayerImmersiveMode(value: Boolean) = edit { it[Keys.playerImmersiveMode] = value }
    suspend fun setHapticsEnabled(value: Boolean) = edit { it[Keys.hapticsEnabled] = value }
    suspend fun setSoundEffectsEnabled(value: Boolean) = edit { it[Keys.soundEffectsEnabled] = value }
    suspend fun setSoundEffectsVolume(value: Float) = edit { it[Keys.soundEffectsVolume] = value.coerceIn(0f, 1f) }
    suspend fun setNotificationsEnabled(value: Boolean) = edit { it[Keys.notificationsEnabled] = value }
    suspend fun setNotifyAchievements(value: Boolean) = edit { it[Keys.notifyAchievements] = value }
    suspend fun setNotifyDownloads(value: Boolean) = edit { it[Keys.notifyDownloads] = value }
    suspend fun setNotifyMultiplayer(value: Boolean) = edit { it[Keys.notifyMultiplayer] = value }
    suspend fun setHighContrast(value: Boolean) = edit { it[Keys.highContrast] = value }
    suspend fun setShowOnlineRecommendations(value: Boolean) = edit { it[Keys.showOnlineRecommendations] = value }
    suspend fun setShowStatistics(value: Boolean) = edit { it[Keys.showStatistics] = value }
    suspend fun setIntegerScaling(value: Boolean) = edit { it[Keys.integerScaling] = value }
    suspend fun setDisplaySmoothing(value: Boolean) = edit { it[Keys.displaySmoothing] = value }
    suspend fun setShowPerformanceOverlay(value: Boolean) = edit { it[Keys.showPerformanceOverlay] = value }
    suspend fun setShowTouchControls(value: Boolean) = edit { it[Keys.showTouchControls] = value }
    suspend fun setAudioEnabled(value: Boolean) = edit { it[Keys.audioEnabled] = value }
    suspend fun setMasterVolume(value: Float) = edit { it[Keys.masterVolume] = value.coerceIn(0f, 1f) }
    suspend fun setAutoSuspendOnBackground(value: Boolean) = edit { it[Keys.autoSuspendOnBackground] = value }
    suspend fun setPauseOnHeadphoneDisconnect(value: Boolean) = edit { it[Keys.pauseOnHeadphoneDisconnect] = value }

    /** Replaces user-visible settings after a validated Retra backup import. */
    suspend fun replace(value: AppSettings) = edit { preferences ->
        preferences[Keys.onboardingComplete] = value.onboardingComplete
        preferences[Keys.themeMode] = value.themeMode.name
        preferences[Keys.libraryLayout] = value.libraryLayout.name
        preferences[Keys.dynamicColor] = value.dynamicColor
        preferences[Keys.reduceMotion] = value.reduceMotion
        preferences[Keys.reduceTransparency] = value.reduceTransparency
        preferences[Keys.fastForwardSpeed] = value.fastForwardSpeed.coerceIn(1f, 16f)
        preferences[Keys.performanceProfile] = value.performanceProfile.stableProfile().name
        preferences[Keys.accentPalette] = value.accentPalette.name
        preferences[Keys.contentDensity] = value.contentDensity.name
        preferences[Keys.startupDestination] = value.startupDestination.name
        preferences[Keys.glassIntensity] = value.glassIntensity.coerceIn(0f, 1f)
        preferences[Keys.cornerScale] = value.cornerScale.coerceIn(0.75f, 1.35f)
        preferences[Keys.fontScale] = value.fontScale.coerceIn(0.85f, 1.3f)
        preferences[Keys.touchControlOpacity] = value.touchControlOpacity.coerceIn(0.25f, 1f)
        preferences[Keys.touchControlScale] = value.touchControlScale.coerceIn(0.72f, 1.35f)
        preferences[Keys.touchControlSpacing] = value.touchControlSpacing.coerceIn(0.72f, 1.4f)
        preferences[Keys.touchDeadZone] = value.touchDeadZone.coerceIn(0.05f, 0.5f)
        preferences[Keys.controlLayoutPreset] = value.controlLayoutPreset.name
        preferences[Keys.controlVisualStyle] = value.controlVisualStyle.name
        preferences[Keys.screenScalingMode] = value.screenScalingMode.name
        preferences[Keys.showShoulderButtons] = value.showShoulderButtons
        preferences[Keys.showQuickActions] = value.showQuickActions
        preferences[Keys.quickSaveEnabled] = value.quickSaveEnabled
        preferences[Keys.autoSaveIntervalMinutes] = value.autoSaveIntervalMinutes.coerceIn(0, 60)
        preferences[Keys.playerImmersiveMode] = value.playerImmersiveMode
        preferences[Keys.hapticsEnabled] = value.hapticsEnabled
        preferences[Keys.soundEffectsEnabled] = value.soundEffectsEnabled
        preferences[Keys.soundEffectsVolume] = value.soundEffectsVolume.coerceIn(0f, 1f)
        preferences[Keys.notificationsEnabled] = value.notificationsEnabled
        preferences[Keys.notifyAchievements] = value.notifyAchievements
        preferences[Keys.notifyDownloads] = value.notifyDownloads
        preferences[Keys.notifyMultiplayer] = value.notifyMultiplayer
        preferences[Keys.highContrast] = value.highContrast
        preferences[Keys.showOnlineRecommendations] = value.showOnlineRecommendations
        preferences[Keys.showStatistics] = value.showStatistics
        preferences[Keys.integerScaling] = value.integerScaling
        preferences[Keys.displaySmoothing] = value.displaySmoothing
        preferences[Keys.showPerformanceOverlay] = value.showPerformanceOverlay
        preferences[Keys.showTouchControls] = value.showTouchControls
        preferences[Keys.audioEnabled] = value.audioEnabled
        preferences[Keys.masterVolume] = value.masterVolume.coerceIn(0f, 1f)
        preferences[Keys.autoSuspendOnBackground] = value.autoSuspendOnBackground
        preferences[Keys.pauseOnHeadphoneDisconnect] = value.pauseOnHeadphoneDisconnect
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.retraDataStore.edit { preferences -> block(preferences) }
    }

    private fun PerformanceProfile.stableProfile(): PerformanceProfile = when (this) {
        PerformanceProfile.BOOSTED, PerformanceProfile.EXTREME -> PerformanceProfile.BALANCED
        else -> this
    }

    private fun String?.performanceProfileOrDefault(): PerformanceProfile = when (this) {
        PerformanceProfile.AUTHENTIC.name -> PerformanceProfile.AUTHENTIC
        PerformanceProfile.BATTERY_SAVER.name -> PerformanceProfile.BATTERY_SAVER
        PerformanceProfile.BALANCED.name,
        PerformanceProfile.BOOSTED.name,
        PerformanceProfile.EXTREME.name,
        null -> PerformanceProfile.BALANCED
        else -> PerformanceProfile.BALANCED
    }

    private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun String?.mapAccentPalette(): AccentPalette = when (this) {
        null -> AccentPalette.RETRA_INDIGO
        "RETRA_PRISM", "AURORA", "ATOMIC_PURPLE" -> AccentPalette.RETRA_INDIGO
        "EMERALD_CARTRIDGE", "GLACIER" -> AccentPalette.GRAPHITE
        "SUNSET_GOLD" -> AccentPalette.SOFT_VIOLET
        else -> enumOrDefault(AccentPalette.RETRA_INDIGO)
    }
}
