package app.retra.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.BuildConfig
import app.retra.core.formatBytes
import app.retra.data.MessageTone
import app.retra.data.ScreenFilter
import app.retra.data.Settings
import app.retra.data.ThemeMode
import app.retra.ui.LocalAppActions
import app.retra.ui.gutter
import app.retra.ui.listPadding
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.InfoRow
import app.retra.ui.components.LogoTile
import app.retra.ui.components.NavRow
import app.retra.ui.components.RetraButton
import app.retra.ui.components.ScreenHeader
import app.retra.ui.components.SectionHeader
import app.retra.ui.components.Segmented
import app.retra.ui.components.SliderRow
import app.retra.ui.components.SwitchRow
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class StorageUsage(val games: Long = 0, val saves: Long = 0, val states: Long = 0, val screens: Long = 0, val cache: Long = 0)

@Immutable
data class LicenceItem(val title: String, val detail: String, val license: String, val asset: String? = null, val url: String? = null)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings = container.settings.settings
    val biosPresent = container.bios.present
    private val _storage = MutableStateFlow(StorageUsage())
    val storage = _storage.asStateFlow()

    fun update(t: (Settings) -> Settings) = container.settings.update(t)

    fun refreshStorage() = viewModelScope.launch {
        _storage.value = withContext(Dispatchers.IO) {
            StorageUsage(
                games = container.library.storageBytes(),
                saves = container.saves.savesBytes(),
                states = container.saves.statesBytes(),
                screens = container.saves.screensBytes(),
                cache = container.filesRoot.parentFile?.resolve("cache")?.walkBottomUp()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L,
            )
        }
    }

    fun importBios(uri: Uri) = viewModelScope.launch {
        runCatching { container.importer.readBytes(uri, 64 * 1024) }
            .onSuccess { bytes ->
                val err = withContext(Dispatchers.IO) { container.bios.import(bytes) }
                if (err == null) container.messages.show("BIOS added. Games use it from the next launch.", MessageTone.SUCCESS)
                else container.messages.show(err, MessageTone.ERROR)
            }
            .onFailure { container.messages.show("Couldn't read that file.", MessageTone.ERROR) }
    }

    fun removeBios() = viewModelScope.launch {
        withContext(Dispatchers.IO) { container.bios.remove() }
        container.messages.show("Using Retra's built-in BIOS again")
    }

    fun backup(uri: Uri, resolver: android.content.ContentResolver) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { resolver.openOutputStream(uri, "wt")!!.use { container.backups.backup(it) } } }
            .onSuccess { container.messages.show(if (it == 1) "Backed up 1 save" else "Backed up $it saves", MessageTone.SUCCESS) }
            .onFailure { container.messages.show("The backup couldn't be written.", MessageTone.ERROR) }
    }

    fun restore(uri: Uri, resolver: android.content.ContentResolver) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { resolver.openInputStream(uri)!!.use { container.backups.restore(it) } } }
            .onSuccess { r ->
                container.messages.show(if (r.saves == 0) "No saves found in that file" else "Restored ${r.saves} saves", if (r.saves == 0) MessageTone.INFO else MessageTone.SUCCESS)
                refreshStorage()
            }
            .onFailure { container.messages.show("That isn't a Retra backup.", MessageTone.ERROR) }
    }

    fun licences(): List<LicenceItem> {
        val base = listOf(
            LicenceItem("Fraunces", "Display typeface by the Fraunces Project Authors", "SIL Open Font License 1.1", asset = "licenses/OFL-Fraunces.txt"),
            LicenceItem("Nunito", "Body typeface by the Nunito Project Authors", "SIL Open Font License 1.1", asset = "licenses/OFL-Nunito.txt"),
            LicenceItem("Retra engine", "Original C++ GBA core and replacement BIOS written for Retra. No code from other emulators; behaviour implemented from public documentation (GBATEK, Tonc).", "Apache-2.0"),
            LicenceItem("Mochi Hop", "Original game, art and music made for Retra", "Apache-2.0"),
            LicenceItem("AndroidX, Jetpack Compose, Kotlin", "Google and JetBrains", "Apache-2.0"),
            LicenceItem("Haze", "Backdrop blur and glass by Chris Banes", "Apache-2.0", url = "https://github.com/chrisbanes/haze"),
        )
        val homebrew = container.catalog.current?.entries.orEmpty().filter { it.isRom && it.license != null && it.id != "mochi-hop" }
            .map { LicenceItem(it.title, "Homebrew by ${it.author}", it.license!!, url = it.source ?: it.homepage) }
        return base + homebrew
    }

    fun readAsset(path: String): String = runCatching { String(container.appAsset(path)) }.getOrDefault("")

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { SettingsViewModel(c) } }
    }
}

@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by vm.settings.collectAsStateWithLifecycle()
    val bios by vm.biosPresent.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.refreshStorage() }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importBios) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let { u -> vm.backup(u, context.contentResolver) } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { u -> vm.restore(u, context.contentResolver) } }
    SettingsContent(
        settings = settings,
        biosPresent = bios,
        storage = storage,
        licences = remember { vm.licences() },
        onUpdate = vm::update,
        onImportBios = { biosPicker.launch(arrayOf("*/*")) },
        onRemoveBios = { vm.removeBios() },
        onBackup = { backup.launch("retra-saves-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())}.zip") },
        onRestore = { restore.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        readAsset = vm::readAsset,
    )
}

@Composable
fun SettingsContent(
    settings: Settings,
    biosPresent: Boolean,
    storage: StorageUsage,
    licences: List<LicenceItem>,
    onUpdate: ((Settings) -> Settings) -> Unit = {},
    onImportBios: () -> Unit = {},
    onRemoveBios: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    readAsset: (String) -> String = { "" },
) {
    val c = Retra.colors
    val g = gutter()
    val actions = LocalAppActions.current
    var showLicences by remember { mutableStateOf(false) }
    var licenceText by remember { mutableStateOf<Pair<String, String>?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(Space.l)) {
            item("header") { ScreenHeader("Settings", Modifier.padding(g)) }
            item("look") {
                Section("Look and feel", g) {
                    Text("Theme", style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs))
                    Spacer(Modifier.height(Space.xxs))
                    Segmented(listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"), settings.theme, { v -> onUpdate { it.copy(theme = v) } })
                    Spacer(Modifier.height(Space.xs))
                    SwitchRow("Reduce motion", settings.reduceMotion, { v -> onUpdate { it.copy(reduceMotion = v) } }, subtitle = "Calmer transitions, no drifting sky", icon = Icons.Rounded.Animation)
                    Divider()
                    SwitchRow("Haptics", settings.haptics, { v -> onUpdate { it.copy(haptics = v) } }, subtitle = "A soft tick when you press a button", icon = Icons.Rounded.Vibration)
                    Divider()
                    SwitchRow(
                        "Box art", settings.downloadCoverArt, { v -> onUpdate { it.copy(downloadCoverArt = v) } },
                        subtitle = "Fetch covers for your cartridges from libretro-thumbnails", icon = Icons.Rounded.Image,
                    )
                }
            }
            item("sound") {
                Section("Sound", g) {
                    SwitchRow("Game sound", !settings.muted, { v -> onUpdate { it.copy(muted = !v) } }, icon = Icons.Rounded.VolumeUp)
                    SliderRow("Volume", settings.volume, { v -> onUpdate { it.copy(volume = v) } }, "${(settings.volume * 100).toInt()} %")
                }
            }
            item("controls") {
                Section("Touch controls", g) {
                    SliderRow("Opacity", settings.controlsOpacity, { v -> onUpdate { it.copy(controlsOpacity = v) } }, "${(settings.controlsOpacity * 100).toInt()} %", range = 0.2f..1f, icon = Icons.Rounded.Opacity)
                    SliderRow("Size", settings.controlsScale, { v -> onUpdate { it.copy(controlsScale = v) } }, "${(settings.controlsScale * 100).toInt()} %", range = 0.8f..1.3f, icon = Icons.Rounded.OpenInFull)
                    RetraButton("Reset controls", { onUpdate { it.copy(controlsOpacity = Settings().controlsOpacity, controlsScale = 1f) } }, icon = Icons.Rounded.Restore, style = ButtonStyle.TERTIARY, compact = true)
                }
            }
            item("display") {
                Section("Screen", g) {
                    Text("Pixels", style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs))
                    Spacer(Modifier.height(Space.xxs))
                    Segmented(listOf(ScreenFilter.SHARP to "Sharp", ScreenFilter.SMOOTH to "Smooth"), settings.screenFilter, { v -> onUpdate { it.copy(screenFilter = v) } })
                    Spacer(Modifier.height(Space.xs))
                    SwitchRow("Integer scaling", settings.integerScale, { v -> onUpdate { it.copy(integerScale = v) } }, subtitle = "Whole-pixel sizes only, crisp but smaller", icon = Icons.Rounded.GridOn)
                    Divider()
                    SwitchRow("LCD colours", settings.colorCorrection, { v -> onUpdate { it.copy(colorCorrection = v) } }, subtitle = "Softer colours like the original screen", icon = Icons.Rounded.Palette)
                    Divider()
                    SwitchRow("Show FPS", settings.showFps, { v -> onUpdate { it.copy(showFps = v) } }, icon = Icons.Rounded.Speed)
                }
            }
            item("play") {
                Section("Fast-forward and rewind", g) {
                    Text("Fast-forward speed", style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs))
                    Spacer(Modifier.height(Space.xxs))
                    Segmented(listOf(2 to "2×", 3 to "3×", 4 to "4×"), settings.fastForwardSpeed, { v -> onUpdate { it.copy(fastForwardSpeed = v) } })
                    Spacer(Modifier.height(Space.xs))
                    SwitchRow("Sound while fast-forwarding", settings.fastForwardAudio, { v -> onUpdate { it.copy(fastForwardAudio = v) } }, icon = Icons.Rounded.FastForward)
                    Divider()
                    SwitchRow("Rewind", settings.rewindEnabled, { v -> onUpdate { it.copy(rewindEnabled = v) } }, subtitle = "Hold the rewind button to go back up to 10 seconds", icon = Icons.Rounded.FastRewind)
                    Divider()
                    SwitchRow(
                        "Save a Continue point on exit", settings.autoSaveOnExit, { v -> onUpdate { it.copy(autoSaveOnExit = v) } },
                        subtitle = "Off: leaving a game skips the snapshot. In-game saves are always kept.", icon = Icons.Rounded.Save,
                    )
                }
            }
            item("clock") {
                Section("Game clock", g) {
                    Text(
                        "Pokémon Ruby, Sapphire and Emerald use a real-time clock for berries, tides and daily events. Shift it here; it applies the next time a game starts.",
                        style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs),
                    )
                    InfoRow("Offset", formatOffset(settings.rtcOffsetSeconds))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs), modifier = Modifier.fillMaxWidth()) {
                        listOf(-86400L to "−1 day", -3600L to "−1 h", 3600L to "+1 h", 86400L to "+1 day").forEach { (d, label) ->
                            RetraButton(label, { onUpdate { it.copy(rtcOffsetSeconds = it.rtcOffsetSeconds + d) } }, Modifier.weight(1f), style = ButtonStyle.SECONDARY, compact = true)
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                    RetraButton("Use the real time", { onUpdate { it.copy(rtcOffsetSeconds = 0) } }, icon = Icons.Rounded.AccessTime, style = ButtonStyle.TERTIARY, compact = true, enabled = settings.rtcOffsetSeconds != 0L)
                }
            }
            item("bios") {
                Section("BIOS", g) {
                    Text(
                        if (biosPresent) "Using your BIOS file." else "Retra has its own built-in BIOS, so you don't need one. A few games sound better with the original; add yours (16 KB) if you have it.",
                        style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(vertical = Space.xs),
                    )
                    if (biosPresent) NavRow("Remove my BIOS", onRemoveBios, icon = Icons.Rounded.DeleteOutline, danger = true)
                    else NavRow("Add BIOS file", onImportBios, icon = Icons.Rounded.Memory)
                }
            }
            item("saves") {
                Section("Your saves", g) {
                    NavRow("Back up all saves", onBackup, subtitle = "One .zip with every game's save and cheats", icon = Icons.Rounded.Archive)
                    Divider()
                    NavRow("Restore from backup", onRestore, subtitle = "Replaces saves with the ones in the .zip", icon = Icons.Rounded.Unarchive)
                }
            }
            item("storage") {
                Section("Storage", g) {
                    InfoRow("Games", formatBytes(storage.games))
                    InfoRow("Saves", formatBytes(storage.saves))
                    InfoRow("Save states", formatBytes(storage.states))
                    InfoRow("Screenshots", formatBytes(storage.screens))
                    InfoRow("Downloads cache", formatBytes(storage.cache))
                }
            }
            item("about") {
                Section("About", g) {
                    Row(Modifier.padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                        LogoTile(size = 56.dp)
                        Column(Modifier.padding(start = Space.s)) {
                            Text("Retra ${BuildConfig.VERSION_NAME}", style = RetraType.headline, color = c.ink)
                            Text("Made by Prashant Chataut", style = RetraType.label, color = c.inkMuted)
                        }
                    }
                    Text(
                        "Retra plays games you own and free homebrew. It never includes or downloads commercial games or Nintendo's BIOS.",
                        style = RetraType.label, color = c.inkMuted,
                    )
                    Spacer(Modifier.height(Space.xs))
                    NavRow("Open-source licences", { showLicences = true }, icon = Icons.Rounded.Gavel)
                    Divider()
                    NavRow("Show the welcome again", { onUpdate { it.copy(onboardingDone = false) } }, icon = Icons.Rounded.WavingHand)
                    Divider()
                    NavRow("Source code", { actions.openUrl("https://github.com/prashantchataut/Retra") }, icon = Icons.Rounded.Favorite)
                }
            }
        }
        GlassSheet(visible = showLicences, onDismiss = { if (licenceText != null) licenceText = null else showLicences = false }, title = licenceText?.first ?: "Licences") {
            val text = licenceText
            if (text != null) {
                Text(text.second, style = RetraType.caption.copy(fontFamily = FontFamily.Monospace), color = c.ink)
            } else {
                licences.forEachIndexed { i, l ->
                    if (i > 0) Divider()
                    Column(Modifier.fillMaxWidth().padding(vertical = Space.s)) {
                        Text(l.title, style = RetraType.bodyStrong, color = c.ink)
                        Text(l.detail, style = RetraType.label, color = c.inkMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(l.license, style = RetraType.caption, color = c.accent, modifier = Modifier.weight(1f))
                            if (l.asset != null) RetraButton("Read", { licenceText = l.title to readAsset(l.asset) }, style = ButtonStyle.TERTIARY, compact = true)
                            else if (l.url != null) RetraButton("Website", { actions.openUrl(l.url) }, style = ButtonStyle.TERTIARY, compact = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, g: androidx.compose.foundation.layout.PaddingValues, content: @Composable () -> Unit) {
    Column(Modifier.padding(g).padding(top = Space.l).widthIn(max = 720.dp)) {
        SectionHeader(title)
        Spacer(Modifier.height(Space.xs))
        GlassGroup { content() }
    }
}

fun formatOffset(seconds: Long): String {
    if (seconds == 0L) return "None (real time)"
    val sign = if (seconds > 0) "+" else "−"
    val abs = kotlin.math.abs(seconds)
    val days = abs / 86400
    val hours = (abs % 86400) / 3600
    val parts = buildList {
        if (days > 0) add(if (days == 1L) "1 day" else "$days days")
        if (hours > 0) add("$hours h")
    }
    return sign + parts.joinToString(" ").ifEmpty { "${abs / 60} min" }
}
