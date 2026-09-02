package app.retra.emulator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Default.Palette),
    PLAYER("Player", Icons.Default.Speed),
    CONTROLS("Controls", Icons.Default.Gamepad),
    SAVES("Saves", Icons.Default.Save),
    PRIVACY("Privacy", Icons.Default.Lock),
    DIAGNOSTICS("Diagnostics", Icons.Default.Tune),
    ABOUT("About & Developer", Icons.Default.Info)
}

@Composable
fun RetraSettingsScreen(
    settings: AppSettings,
    viewModel: RetraViewModel,
    onBack: () -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val vaultHealth by viewModel.vaultHealth.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Customization, controls, and diagnostics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RetraLogoTile(size = 40.dp)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Chips Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (item in SettingsCategory.entries) {
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item.label) },
                        leadingIcon = { Icon(item.icon, null, Modifier.size(16.dp)) }
                    )
                }
            }

            // Category Details
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (category) {
                    SettingsCategory.APPEARANCE -> {
                        item {
                            RetraSettingsCard("Theme & Color", Icons.Default.Palette) {
                                RetraChoiceRow(ThemeMode.entries, settings.themeMode, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, viewModel::setThemeMode)
                                RetraToggle("System dynamic colors", "Adapt to Android Material You palette on supported devices.", settings.dynamicColor, viewModel::setDynamicColor)
                                RetraToggle("High contrast", "Emphasize borders and maximum readability text.", settings.highContrast, viewModel::setHighContrast)
                                RetraToggle("Reduce transparency", "Replace translucent liquid glass with solid surfaces.", settings.reduceTransparency, viewModel::setReduceTransparency)
                                RetraToggle("Reduce motion", "Disable spring transitions and crossfades.", settings.reduceMotion, viewModel::setReduceMotion)
                            }
                        }
                        item {
                            RetraSettingsCard("Archive Glass & Material", Icons.Default.Tune) {
                                RetraChoiceRow(AccentPalette.entries, settings.accentPalette, ::accentLabel, viewModel::setAccentPalette)
                                RetraSlider("Glass intensity", settings.glassIntensity, 0f..1f, viewModel::setGlassIntensity)
                                RetraSlider("Corner scale", settings.cornerScale, 0.75f..1.35f, viewModel::setCornerScale, decimal = true, suffix = "×")
                                RetraSlider("Font scale", settings.fontScale, 0.85f..1.3f, viewModel::setFontScale, decimal = true, suffix = "×")
                            }
                        }
                        item {
                            RetraSettingsCard("Library Presentation", Icons.Default.Settings) {
                                RetraChoiceRow(LibraryLayout.entries, settings.libraryLayout, ::libraryLabel, viewModel::setLibraryLayout)
                                RetraChoiceRow(StartupDestination.entries, settings.startupDestination, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setStartupDestination)
                                RetraToggle("Online recommendations", "Show verified creator pages in Discover.", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
                                RetraToggle("Library statistics", "Show archive counts and save milestones.", settings.showStatistics, viewModel::setShowStatistics)
                            }
                        }
                    }

                    SettingsCategory.PLAYER -> {
                        item {
                            RetraSettingsCard("Display & Video", Icons.Default.Speed) {
                                RetraChoiceRow(ScreenScalingMode.entries, settings.screenScalingMode, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setScreenScalingMode)
                                RetraToggle("Integer scaling", "Scale by whole integer factors for sharp pixel art.", settings.integerScaling, viewModel::setIntegerScaling)
                                RetraToggle("Display smoothing", "Filter the game canvas when non-integer scaled.", settings.displaySmoothing, viewModel::setDisplaySmoothing)
                                RetraToggle("Immersive mode", "Hide system status and navigation bars during play.", settings.playerImmersiveMode, viewModel::setPlayerImmersiveMode)
                                RetraToggle("Performance overlay", "Show live FPS, frame pacing, and engine metrics.", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
                            }
                        }
                        item {
                            RetraSettingsCard("Performance Profile", Icons.Default.Speed) {
                                RetraChoiceRow(PerformanceProfile.entries, settings.performanceProfile, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setPerformanceProfile)
                                RetraSlider("Fast-forward multiplier", settings.fastForwardSpeed, 1f..8f, viewModel::setFastForwardSpeed, decimal = true, suffix = "×")
                                RetraToggle("Suspend in background", "Automatically snapshot and pause when leaving the app.", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
                            }
                        }
                        item { PerformanceAdvisorPanel(viewModel, games) }
                        item {
                            RetraSettingsCard("Audio & Sound", Icons.Default.VolumeUp) {
                                RetraToggle("Game audio", "Enable emulator audio synthesizer.", settings.audioEnabled, viewModel::setAudioEnabled)
                                RetraSlider("Master volume", settings.masterVolume, 0f..1f, viewModel::setMasterVolume)
                                RetraToggle("Pause on headphone disconnect", "Pause emulation if audio device disconnects.", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
                            }
                        }
                    }

                    SettingsCategory.CONTROLS -> {
                        item {
                            RetraSettingsCard("Touch Overlay", Icons.Default.Gamepad) {
                                RetraToggle("Show touch controls", "Display on-screen touch controller overlay.", settings.showTouchControls, viewModel::setShowTouchControls)
                                RetraChoiceRow(ControlLayoutPreset.entries, settings.controlLayoutPreset, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setControlLayoutPreset)
                                RetraChoiceRow(ControlVisualStyle.entries, settings.controlVisualStyle, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) }, viewModel::setControlVisualStyle)
                                RetraSlider("Control opacity", settings.touchControlOpacity, 0.20f..1f, viewModel::setTouchControlOpacity)
                                RetraSlider("Control size", settings.touchControlScale, 0.75f..1.45f, viewModel::setTouchControlScale, decimal = true, suffix = "×")
                                RetraSlider("Control spacing", settings.touchControlSpacing, 0.75f..1.4f, viewModel::setTouchControlSpacing, decimal = true, suffix = "×")
                                RetraSlider("Dead zone", settings.touchDeadZone, 0.05f..0.4f, viewModel::setTouchDeadZone)
                                RetraToggle("Shoulder buttons (L / R)", "Show shoulder buttons on the screen overlay.", settings.showShoulderButtons, viewModel::setShowShoulderButtons)
                                RetraToggle("Quick actions bar", "Show save, load, rewind, and fast-forward shortcuts.", settings.showQuickActions, viewModel::setShowQuickActions)
                                RetraToggle("Haptic feedback", "Tactile confirmation on button press.", settings.hapticsEnabled, viewModel::setHapticsEnabled)
                            }
                        }
                        item { ControllerStudioPanel(viewModel) }
                    }

                    SettingsCategory.SAVES -> {
                        item {
                            RetraSettingsCard("Save Policy", Icons.Default.Save) {
                                RetraToggle("Quick save button", "Enable quick state shortcut in the player.", settings.quickSaveEnabled, viewModel::setQuickSaveEnabled)
                                Text("Auto-save interval", fontWeight = FontWeight.SemiBold)
                                RetraChoiceRow(listOf(0, 2, 5, 10, 15), settings.autoSaveIntervalMinutes, { if (it == 0) "Off" else "$it min" }, viewModel::setAutoSaveIntervalMinutes)
                                Text("SRAM battery saves and save states are verified on creation to prevent data loss.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item { SaveTimelinePanel(viewModel, games) }
                    }

                    SettingsCategory.PRIVACY -> {
                        item {
                            RetraSettingsCard("Local-first boundary", Icons.Default.Lock) {
                                Text(
                                    "ROMs, patches, cheats, save records, and screenshots remain strictly on this device unless you explicitly export or share them.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RetraToggle("Notifications", "Allow local status notifications.", settings.notificationsEnabled, viewModel::setNotificationsEnabled)
                                RetraToggle("Achievement alerts", "Notify when a local milestone unlocks.", settings.notifyAchievements, viewModel::setNotifyAchievements)
                                RetraToggle("Download alerts", "Notify when verified homebrew finishes downloading.", settings.notifyDownloads, viewModel::setNotifyDownloads)
                                RetraToggle("Multiplayer alerts", "Notify on local multiplayer requests.", settings.notifyMultiplayer, viewModel::setNotifyMultiplayer)
                            }
                        }
                        item {
                            RetraSettingsCard("Interface Feedback", Icons.Default.Notifications) {
                                RetraToggle("Interface sounds", "Play original Retra audio cues for UI actions.", settings.soundEffectsEnabled, viewModel::setSoundEffectsEnabled)
                                RetraSlider("Sound volume", settings.soundEffectsVolume, 0f..1f, viewModel::setSoundEffectsVolume)
                            }
                        }
                    }

                    SettingsCategory.DIAGNOSTICS -> {
                        item {
                            RetraSettingsCard("Runtime & Diagnostics", Icons.Default.Tune) {
                                RetraAboutLine("App Version", "Retra 3.0.0 (Build 3000)")
                                RetraAboutLine("Device ABI", Build.SUPPORTED_ABIS.joinToString(", "))
                                RetraAboutLine("Emulator Core", if (viewModel.coreAvailable) "mGBA Libretro Engine (Native)" else "Native Reference Bridge")
                                RetraAboutLine("Core Status", viewModel.coreStatus)
                                RetraAboutLine("Save Health", if (vaultHealth.corruptedRecords == 0) "All records valid" else "${vaultHealth.corruptedRecords} records corrupted")
                                RetraAboutLine("Renderer", "Hardware Compose Canvas (60 FPS)")

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val diag = "Retra 3.0.0 | ABI: ${Build.SUPPORTED_ABIS.firstOrNull()} | Core: ${viewModel.coreStatus} | Android: ${Build.VERSION.SDK_INT}"
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Retra Diagnostics", diag))
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy System Diagnostics")
                                }
                            }
                        }
                    }

                    SettingsCategory.ABOUT -> {
                        item {
                            RetraSettingsCard("About Retra", Icons.Default.Info) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    RetraLogoTile(size = 64.dp)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Retra 3.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("Liquid Glass Handheld Archive", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        RetraBadge("PRASHANT CHATAUT", ElectricLilac)
                                    }
                                }

                                RetraAboutLine("Developer", "Prashant Chataut")
                                RetraAboutLine("Built-in game", "Retra Drift · original open-source GBA homebrew")
                                RetraAboutLine("Patch formats", "UPS, IPS, BPS")
                                RetraAboutLine("Core Engine", "mGBA Libretro Core (GPL v3)")
                                RetraAboutLine("Privacy", "Local by default")

                                // External Links
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://knowprashant.vercel.app")))
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Language, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Developer Website (knowprashant.vercel.app)")
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/prashantchataut")))
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Person, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("GitHub Profile (@prashantchataut)")
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/prashantchataut/Retra")))
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Code, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Retra Source Code Repository")
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                    }
                                }

                                Text(
                                    "Retra is an independent emulator project not affiliated with Nintendo, The Pokémon Company, or Game Freak. Game titles and formats are referenced for identification and local file compatibility only.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.size(30.dp)) }
            }
        }
    }
}

@Composable
private fun RetraSettingsCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    RetraPanel(
        tier = GlassTier.REGULAR,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
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
        for (value in values) {
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
        }
    }
}

@Composable
private fun RetraAboutLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

private fun accentLabel(value: AccentPalette): String = when (value) {
    AccentPalette.RETRA_INDIGO -> "Electric Lilac"
    AccentPalette.GRAPHITE -> "Graphite"
    AccentPalette.SOFT_VIOLET -> "Soft Violet"
    AccentPalette.CLASSIC_GRAY -> "Classic Gray"
}

private fun libraryLabel(value: LibraryLayout): String = when (value) {
    LibraryLayout.LARGE_GRID -> "Large covers"
    LibraryLayout.COMPACT_GRID -> "Compact covers"
    LibraryLayout.DETAILED_LIST -> "Detailed list"
}
