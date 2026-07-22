package app.retra.emulator

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.emulation.EmulatorButton
import app.retra.core.model.AppSettings
import app.retra.core.model.CompatibilityStatus
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.GameRecord
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.emulator.data.GameLaunchProfile
import app.retra.emulator.data.PerformanceAdvice
import app.retra.emulator.data.SaveTimelineEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun ControllerStudioPanel(viewModel: RetraViewModel) {
    val devices by viewModel.controllerDevices.collectAsStateWithLifecycle()
    val profiles by viewModel.controllerProfiles.collectAsStateWithLifecycle()
    val captureTarget by viewModel.controllerCaptureTarget.collectAsStateWithLifecycle()
    val captureGameHash by viewModel.controllerCaptureGameSha256.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val contextGame = activeGame ?: selectedGame
    var selectedDescriptor by rememberSaveable { mutableStateOf("") }
    var gameSpecific by rememberSaveable { mutableStateOf(false) }

    val device = devices.firstOrNull { it.descriptor == selectedDescriptor } ?: devices.firstOrNull()
    LaunchedEffect(device?.descriptor) {
        if (selectedDescriptor.isBlank() && device != null) selectedDescriptor = device.descriptor
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Controller Studio stores mappings by Android device descriptor. Game-specific profiles override the device default without changing other games.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ControllerInputTester(viewModel)
        if (devices.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(
                    "Connect a USB or Bluetooth controller, then press any button. Retra will register the device here.",
                    Modifier.padding(14.dp)
                )
            }
            return@Column
        }

        Text("Controller", fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            devices.forEach { item ->
                FilterChip(
                    selected = item.descriptor == device?.descriptor,
                    onClick = { selectedDescriptor = item.descriptor },
                    label = { Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Gamepad, null, Modifier.size(18.dp)) }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = !gameSpecific, onClick = { gameSpecific = false }, label = { Text("Device default") })
            FilterChip(
                selected = gameSpecific,
                onClick = { gameSpecific = true },
                enabled = contextGame != null,
                label = { Text(contextGame?.let { "Only ${it.title}" } ?: "Select a game first") }
            )
        }

        if (device != null) {
            val profile = remember(profiles, device.descriptor, gameSpecific, contextGame?.sha256) {
                viewModel.resolveControllerProfile(device.descriptor, device.displayName, gameSpecific)
            }
            var deadZone by remember(profile.id, profile.deadZone) { mutableFloatStateOf(profile.deadZone) }
            var trigger by remember(profile.id, profile.triggerThreshold) { mutableFloatStateOf(profile.triggerThreshold) }

            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Analog calibration", fontWeight = FontWeight.SemiBold)
                    Text("Dead zone ${(deadZone * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = deadZone,
                        onValueChange = { deadZone = it },
                        onValueChangeFinished = {
                            viewModel.setControllerCalibration(device.descriptor, device.displayName, gameSpecific, deadZone, trigger)
                        },
                        valueRange = 0.05f..0.65f
                    )
                    Text("Trigger threshold ${(trigger * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = trigger,
                        onValueChange = { trigger = it },
                        onValueChangeFinished = {
                            viewModel.setControllerCalibration(device.descriptor, device.displayName, gameSpecific, deadZone, trigger)
                        },
                        valueRange = 0.15f..0.95f
                    )
                }
            }

            if (profile.alternateBindings.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${profile.alternateBindings.size} action${if (profile.alternateBindings.size == 1) " has" else "s have"} alternate physical buttons. Each physical button still resolves to exactly one emulator action.",
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text("Bindings", fontWeight = FontWeight.SemiBold)
            CONTROLLER_ACTIONS.forEach { action ->
                val keys = profile.bindings.filterValues { it == action }.keys.sorted()
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(action.name.replace('_', ' '), Modifier.width(76.dp), fontWeight = FontWeight.Bold)
                        Text(
                            keys.joinToString().ifBlank { "Unmapped" }.let { value ->
                                if (keys.isEmpty()) value else keys.joinToString { KeyEvent.keyCodeToString(it).removePrefix("KEYCODE_") }
                            },
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(onClick = { viewModel.beginControllerBindingCapture(action, gameSpecific) }) {
                            Text(if (captureTarget == action && ((captureGameHash != null) == gameSpecific)) "Press key…" else "Remap")
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (captureTarget != null) {
                    OutlinedButton(onClick = viewModel::cancelControllerBindingCapture, modifier = Modifier.weight(1f)) {
                        Text("Cancel capture")
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.resetControllerProfile(device.descriptor, device.displayName, gameSpecific) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset profile")
                }
            }
        }
    }
}

@Composable
fun SaveTimelinePanel(viewModel: RetraViewModel, games: List<GameRecord>) {
    val records by viewModel.vaultRecords.collectAsStateWithLifecycle()
    val timelineByGame by viewModel.saveTimeline.collectAsStateWithLifecycle()
    var snapshotTarget by remember { mutableStateOf<app.retra.core.emulation.VaultSaveRecord?>(null) }
    var snapshotTitle by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Timeline checkpoints are immutable copies of a valid save. Restoring one first rotates the current save, so rollback remains reversible.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (records.isEmpty()) {
            Text("No save records are available yet.")
        }
        games.filter { game -> records.any { it.gameSha256.equals(game.sha256, true) } || timelineByGame.containsKey(game.sha256.lowercase()) }
            .forEach { game ->
                val gameRecords = records.filter { it.gameSha256.equals(game.sha256, true) }
                val entries = timelineByGame[game.sha256.lowercase()].orEmpty()
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.History, null)
                            Text(game.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${entries.size} checkpoints", style = MaterialTheme.typography.labelMedium)
                        }
                        gameRecords.firstOrNull()?.let { latest ->
                            FilledTonalButton(onClick = {
                                snapshotTarget = latest
                                snapshotTitle = "Before next session"
                            }) {
                                Text("Create named checkpoint")
                            }
                        }
                        entries.take(8).forEach { entry ->
                            TimelineEntryRow(
                                entry = entry,
                                onRestore = { viewModel.restoreTimelineSnapshot(entry) },
                                onDelete = { viewModel.deleteTimelineSnapshot(entry) }
                            )
                        }
                    }
                }
            }
    }

    snapshotTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { snapshotTarget = null },
            title = { Text("Name checkpoint") },
            text = {
                OutlinedTextField(
                    value = snapshotTitle,
                    onValueChange = { snapshotTitle = it.take(80) },
                    label = { Text("Checkpoint name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createNamedTimelineSnapshot(record, snapshotTitle)
                    snapshotTarget = null
                }) { Text("Create") }
            },
            dismissButton = { FilledTonalButton(onClick = { snapshotTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TimelineEntryRow(entry: SaveTimelineEntry, onRestore: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${DateFormat.getDateTimeInstance().format(Date(entry.createdAtEpochMillis))} · ${entry.kind.name.lowercase()} · ${entry.sizeBytes / 1024} KiB" +
                        if (entry.cheatsActive) " · cheats active" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, "Restore checkpoint") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete checkpoint") }
        }
    }
}

@Composable
fun PerformanceAdvisorPanel(viewModel: RetraViewModel, games: List<GameRecord>) {
    val adviceByGame by viewModel.performanceAdvice.collectAsStateWithLifecycle()
    val profiles by viewModel.gameLaunchProfiles.collectAsStateWithLifecycle()
    val relevantGames = remember(games, adviceByGame, profiles) {
        val configured = games.filter { game ->
            game.sha256.lowercase() in adviceByGame || game.sha256.lowercase() in profiles
        }
        (if (configured.isNotEmpty()) configured else games.sortedByDescending { it.lastPlayedAtEpochMillis ?: 0L }.take(4))
            .take(12)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Retra measures frame time, presented FPS, emulation speed, dropped frames, audio underruns, thermal state, and battery level locally. It does not recommend a profile until the sample is meaningful.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (relevantGames.isEmpty()) {
            Text("Play a game for at least two minutes to create a local measurement window.")
        }
        relevantGames.forEach { game ->
            val advice = adviceByGame[game.sha256.lowercase()]
            val selected = profiles[game.sha256.lowercase()]?.performanceProfile
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Speed, null)
                        Text(game.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        selected?.let { Text("Using ${it.prettyName()}", style = MaterialTheme.typography.labelMedium) }
                    }
                    if (advice == null) {
                        Text("No completed measurement window yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val e = advice.evidence
                        Text(
                            "${e.sampledSeconds}s measured · p95 ${"%.1f".format(e.frameTimeP95Millis)} ms · ${"%.1f".format(e.averagePresentedFps)} FPS · ${"%.1f".format(e.audioUnderrunsPerMinute)} audio underruns/min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        advice.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        if (advice.ready && advice.recommendedProfile != null) {
                            Button(onClick = { viewModel.applyPerformanceRecommendation(game) }) {
                                Text("Apply ${advice.recommendedProfile.prettyName()}")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.clearPerformanceEvidence(game) }) {
                            Text("Clear evidence")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompatibilityNotebookDialog(
    game: GameRecord,
    onSave: (CompatibilityStatus, String) -> Unit,
    onDismiss: () -> Unit
) {
    var status by remember(game.id) { mutableStateOf(game.compatibility) }
    var notes by remember(game.id) { mutableStateOf(game.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compatibility notebook") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Record what you actually observed on this device and Retra version. Notes stay local and never include ROM bytes or save data.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompatibilityStatus.entries.forEach { value ->
                        FilterChip(
                            selected = status == value,
                            onClick = { status = value },
                            label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(4_000) },
                    label = { Text("Observed behavior, workarounds, or settings") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(status, notes) }) { Text("Save") } },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GameLaunchProfileDialog(
    game: GameRecord,
    globalSettings: AppSettings,
    existing: GameLaunchProfile?,
    advice: PerformanceAdvice?,
    onSave: (GameLaunchProfile) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var performance by remember(game.sha256, existing) { mutableStateOf(existing?.performanceProfile ?: globalSettings.performanceProfile) }
    var scaling by remember(game.sha256, existing) { mutableStateOf(existing?.scalingMode ?: globalSettings.screenScalingMode) }
    var smoothing by remember(game.sha256, existing) { mutableStateOf(existing?.displaySmoothing ?: globalSettings.displaySmoothing) }
    var controls by remember(game.sha256, existing) { mutableStateOf(existing?.controlLayout ?: globalSettings.controlLayoutPreset) }
    var touchControls by remember(game.sha256, existing) { mutableStateOf(existing?.showTouchControls ?: globalSettings.showTouchControls) }
    var fastForward by remember(game.sha256, existing) { mutableFloatStateOf(existing?.fastForwardSpeed ?: globalSettings.fastForwardSpeed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Launch profile · ${game.title}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Only this game uses these values. Global player settings remain unchanged.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                advice?.takeIf { it.ready && it.recommendedProfile != null }?.let {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Measured recommendation", fontWeight = FontWeight.SemiBold)
                            Text("${it.recommendedProfile?.prettyName()} · ${(it.confidence * 100).toInt()}% confidence", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text("Performance", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(PerformanceProfile.AUTHENTIC, PerformanceProfile.BALANCED, PerformanceProfile.BATTERY_SAVER).forEach { value ->
                        FilterChip(selected = performance == value, onClick = { performance = value }, label = { Text(value.prettyName()) })
                    }
                }
                Text("Scaling", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScreenScalingMode.entries.forEach { value ->
                        FilterChip(selected = scaling == value, onClick = { scaling = value }, label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase)) })
                    }
                }
                Text("Controls", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlLayoutPreset.entries.forEach { value ->
                        FilterChip(selected = controls == value, onClick = { controls = value }, label = { Text(value.name.lowercase().replace('_', ' ')) })
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = touchControls, onClick = { touchControls = !touchControls }, label = { Text("Touch controls") })
                    FilterChip(selected = smoothing, onClick = { smoothing = !smoothing }, label = { Text("Image smoothing") })
                }
                Text("Fast-forward ${"%.1f".format(fastForward)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = fastForward, onValueChange = { fastForward = it }, valueRange = 1f..8f)
                if (existing != null) {
                    HorizontalDivider()
                    OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Remove per-game profile") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    GameLaunchProfile(
                        gameSha256 = game.sha256,
                        performanceProfile = performance,
                        scalingMode = scaling,
                        displaySmoothing = smoothing,
                        controlLayout = controls,
                        showTouchControls = touchControls,
                        fastForwardSpeed = fastForward
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun PerformanceProfile.prettyName(): String = when (this) {
    PerformanceProfile.AUTHENTIC -> "Authentic"
    PerformanceProfile.BALANCED -> "Balanced"
    PerformanceProfile.BATTERY_SAVER -> "Battery saver"
    PerformanceProfile.BOOSTED -> "Boosted (legacy)"
    PerformanceProfile.EXTREME -> "Extreme (legacy)"
}

private val CONTROLLER_ACTIONS = listOf(
    EmulatorButton.UP,
    EmulatorButton.DOWN,
    EmulatorButton.LEFT,
    EmulatorButton.RIGHT,
    EmulatorButton.A,
    EmulatorButton.B,
    EmulatorButton.L,
    EmulatorButton.R,
    EmulatorButton.START,
    EmulatorButton.SELECT,
    EmulatorButton.MENU
)
