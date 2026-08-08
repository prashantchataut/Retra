package app.retra.emulator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.ControlVisualStyle
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import app.retra.emulator.ui.components.RetraPanel

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Default.Palette),
    PLAYER("Player", Icons.Default.Speed),
    CONTROLS("Controls", Icons.Default.Gamepad),
    SAVES("Saves", Icons.Default.Save),
    PRIVACY("Privacy", Icons.Default.Lock),
    ABOUT("About", Icons.Default.Info)
}

@Composable
fun RetraSettingsScreen(settings: AppSettings, viewModel: RetraViewModel, onBack: () -> Unit) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    Column(Modifier.weight(1f)) {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Appearance, player, controls, saves.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RetraLogoTile(size = 42.dp)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsCategory.entries.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item.label) },
                        leadingIcon = { Icon(item.icon, null, Modifier.size(17.dp)) }
                    )
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (category) {
                    SettingsCategory.APPEARANCE -> {
                        item {
                            RetraSettingsCard("Theme and contrast", Icons.Default.Palette) {
                                RetraChoiceRow(ThemeMode.entries, settings.themeMode, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, viewModel::setThemeMode)
                                RetraToggle("Use system colors", "Allow Android dynamic color on supported devices.", settings.dynamicColor, viewModel::setDynamicColor)
                                RetraToggle("High contrast", "Use stronger outlines and full-strength secondary text.", settings.highContrast, viewModel::setHighContrast)
                                RetraToggle("Reduce transparency", "Replace translucent glass with opaque panels.", settings.reduceTransparency, viewModel::setReduceTransparency)
                                RetraToggle("Reduce motion", "Use immediate transitions instead of crossfades.", settings.reduceMotion, viewModel::setReduceMotion)
                            }
                        }
                        item {
                            RetraSettingsCard("Archive Glass", Icons.Default.Tune) {
                                RetraChoiceRow(AccentPalette.entries, settings.accentPalette, ::accentLabel, viewModel::setAccentPalette)
                                RetraSlider("Glass intensity", settings.glassIntensity, 0f..1f, viewModel::setGlassIntensity)
                                RetraSlider("Corner scale", settings.cornerScale, 0.75f..1.35f, viewModel::setCornerScale, decimal = true, suffix = "×")
                                RetraSlider("Text scale", settings.fontScale, 0.85f..1.3f, viewModel::setFontScale, decimal = true, suffix = "×")
                            }
                        }
                        item {
                            RetraSettingsCard("Library behavior", Icons.Default.Settings) {
                                RetraChoiceRow(LibraryLayout.entries, settings.libraryLayout, ::libraryLabel, viewModel::setLibraryLayout)
                                RetraChoiceRow(StartupDestination.entries, settings.startupDestination, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setStartupDestination)
                                RetraToggle("Online recommendations", "Show the creator-credited Homebrew Hub gallery.", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
                                RetraToggle("Library statistics", "Show local archive and milestone counts.", settings.showStatistics, viewModel::setShowStatistics)
                            }
                        }
                    }
                    SettingsCategory.PLAYER -> {
                        item {
                            RetraSettingsCard("Display", Icons.Default.Speed) {
                                RetraChoiceRow(ScreenScalingMode.entries, settings.screenScalingMode, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setScreenScalingMode)
                                RetraToggle("Integer scaling", "Prefer crisp whole-number pixels when the screen allows it.", settings.integerScaling, viewModel::setIntegerScaling)
                                RetraToggle("Display smoothing", "Filter the game image when scaling.", settings.displaySmoothing, viewModel::setDisplaySmoothing)
                                RetraToggle("Immersive player", "Hide system chrome while playing.", settings.playerImmersiveMode, viewModel::setPlayerImmersiveMode)
                                RetraToggle("Performance overlay", "Show frame pacing and runtime diagnostics.", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
                            }
                        }
                        item {
                            RetraSettingsCard("Performance", Icons.Default.Speed) {
                                RetraChoiceRow(PerformanceProfile.entries, settings.performanceProfile, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setPerformanceProfile)
                                RetraSlider("Fast-forward", settings.fastForwardSpeed, 1f..8f, viewModel::setFastForwardSpeed, decimal = true, suffix = "×")
                                RetraToggle("Suspend in background", "Pause and snapshot when Retra leaves the foreground.", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
                            }
                        }
                        item { PerformanceAdvisorPanel(viewModel, games) }
                        item {
                            RetraSettingsCard("Audio", Icons.Default.VolumeUp) {
                                RetraToggle("Game audio", "Enable emulator audio output.", settings.audioEnabled, viewModel::setAudioEnabled)
                                RetraSlider("Master volume", settings.masterVolume, 0f..1f, viewModel::setMasterVolume)
                                RetraToggle("Pause on headphone disconnect", "Pause when wired or Bluetooth audio becomes unavailable.", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
                            }
                        }
                    }
                    SettingsCategory.CONTROLS -> {
                        item {
                            RetraSettingsCard("Touch controls", Icons.Default.Gamepad) {
                                RetraToggle("Show touch controls", "Keep the on-screen controls visible while playing.", settings.showTouchControls, viewModel::setShowTouchControls)
                                RetraChoiceRow(ControlLayoutPreset.entries, settings.controlLayoutPreset, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setControlLayoutPreset)
                                RetraChoiceRow(ControlVisualStyle.entries, settings.controlVisualStyle, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setControlVisualStyle)
                                RetraSlider("Control opacity", settings.touchControlOpacity, 0.25f..1f, viewModel::setTouchControlOpacity)
                                RetraSlider("Control size", settings.touchControlScale, 0.75f..1.45f, viewModel::setTouchControlScale, decimal = true, suffix = "×")
                                RetraSlider("Control spacing", settings.touchControlSpacing, 0.75f..1.4f, viewModel::setTouchControlSpacing, decimal = true, suffix = "×")
                                RetraSlider("Dead zone", settings.touchDeadZone, 0.05f..0.4f, viewModel::setTouchDeadZone)
                                RetraToggle("Shoulder buttons", "Show L and R on the touch overlay.", settings.showShoulderButtons, viewModel::setShowShoulderButtons)
                                RetraToggle("Quick actions", "Show save, load, fast-forward, and menu shortcuts.", settings.showQuickActions, viewModel::setShowQuickActions)
                                RetraToggle("Haptics", "Use restrained tactile confirmation.", settings.hapticsEnabled, viewModel::setHapticsEnabled)
                            }
                        }
                        item { ControllerStudioPanel(viewModel) }
                    }
                    SettingsCategory.SAVES -> {
                        item {
                            RetraSettingsCard("Save policy", Icons.Default.Save) {
                                RetraToggle("Quick save", "Enable the quick-state action in the player.", settings.quickSaveEnabled, viewModel::setQuickSaveEnabled)
                                Text("Automatic state interval", fontWeight = FontWeight.SemiBold)
                                RetraChoiceRow(listOf(0, 2, 5, 10, 15), settings.autoSaveIntervalMinutes, { if (it == 0) "Off" else "$it min" }, viewModel::setAutoSaveIntervalMinutes)
                                Text("Battery saves and save states are separate. Retra keeps rotating backups and checks record integrity before showing them as healthy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item { SaveTimelinePanel(viewModel, games) }
                    }
                    SettingsCategory.PRIVACY -> {
                        item {
                            RetraSettingsCard("Local-first boundary", Icons.Default.Lock) {
                                Text("ROMs, patches, cheats, save records, and screenshots remain on this device unless you explicitly choose an Android export or share action.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                RetraToggle("Notifications", "Allow Retra to show local status alerts.", settings.notificationsEnabled, viewModel::setNotificationsEnabled)
                                RetraToggle("Achievement alerts", "Notify when an on-device milestone unlocks.", settings.notifyAchievements, viewModel::setNotifyAchievements)
                                RetraToggle("Download alerts", "Notify when a verified homebrew install completes.", settings.notifyDownloads, viewModel::setNotifyDownloads)
                                RetraToggle("Multiplayer alerts", "Show local multiplayer session status.", settings.notifyMultiplayer, viewModel::setNotifyMultiplayer)
                            }
                        }
                        item {
                            RetraSettingsCard("Feedback", Icons.Default.Notifications) {
                                RetraToggle("Interface sounds", "Use short original Retra cues for important actions.", settings.soundEffectsEnabled, viewModel::setSoundEffectsEnabled)
                                RetraSlider("Interface sound volume", settings.soundEffectsVolume, 0f..1f, viewModel::setSoundEffectsVolume)
                            }
                        }
                    }
                    SettingsCategory.ABOUT -> {
                        item {
                            RetraSettingsCard("Retra 3.0", Icons.Default.Info) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    RetraLogoTile(size = 64.dp)
                                    Column {
                                        Text("Archive Glass", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("GBA archive and emulator", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                RetraAboutLine("Built-in game", "Retra Drift · original open-source GBA homebrew")
                                RetraAboutLine("Imported content", "Owned backups, user-selected patches, licensed homebrew")
                                RetraAboutLine("Patch formats", "UPS, IPS, BPS")
                                RetraAboutLine("Privacy", "Local by default")
                                Text("Retra is not affiliated with Nintendo, The Pokémon Company, Game Freak, or community patch projects. Product names are used only to identify user-supplied files and compatibility requirements.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
private fun RetraSettingsCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    RetraPanel(shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(17.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun RetraToggle(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        onClick = { onChecked(!checked) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun RetraSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    decimal: Boolean = false,
    suffix: String = "%"
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                if (decimal) "${"%.2f".format(value)}$suffix" else "${(value * 100).toInt()}$suffix",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun <T> RetraChoiceRow(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
        }
    }
}

@Composable
private fun RetraAboutLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

private fun accentLabel(value: AccentPalette): String = when (value) {
    AccentPalette.RETRA_INDIGO -> "Retra ice"
    AccentPalette.GRAPHITE -> "Graphite"
    AccentPalette.SOFT_VIOLET -> "Frost"
    AccentPalette.CLASSIC_GRAY -> "Classic"
}

private fun libraryLabel(value: LibraryLayout): String = when (value) {
    LibraryLayout.LARGE_GRID -> "Large covers"
    LibraryLayout.COMPACT_GRID -> "Compact covers"
    LibraryLayout.DETAILED_LIST -> "Detailed list"
}
