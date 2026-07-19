package app.retra.emulator

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.SessionPhase
import app.retra.core.model.GameRecord
import app.retra.emulation.api.CoreTier
import app.retra.emulation.api.VideoFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    game: GameRecord,
    viewModel: RetraViewModel,
    onExit: () -> Unit
) {
    val frame by viewModel.latestFrame.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val metrics by viewModel.runtimeMetrics.collectAsStateWithLifecycle()
    val packsByGame by viewModel.cheatPacks.collectAsStateWithLifecycle()
    val activeCheatIds by viewModel.activeCheatIds.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    val gamePacks = packsByGame[game.sha256.lowercase()].orEmpty()
    var menuOpen by remember { mutableStateOf(false) }
    var cheatsOpen by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }
    var selectedSlot by remember { mutableIntStateOf(0) }

    BackHandler {
        if (menuOpen) menuOpen = false else menuOpen = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(game.title, maxLines = 1)
                        Text(
                            if (viewModel.coreDescriptor.tier == CoreTier.GBA_GAMEPLAY) "Gameplay session" else "Native pipeline diagnostics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open session menu")
                    }
                },
                actions = {
                    if (gamePacks.isNotEmpty()) {
                        IconButton(onClick = { cheatsOpen = true }) {
                            Icon(Icons.Default.Code, contentDescription = "Open Retra Codes")
                        }
                    }
                    IconButton(onClick = viewModel::togglePause) {
                        Icon(
                            if (session.phase == SessionPhase.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (session.phase == SessionPhase.RUNNING) "Pause" else "Resume"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.coreDescriptor.tier == CoreTier.DIAGNOSTIC_PIPELINE) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Gamepad, null)
                        Text(
                            "This verifies native frames, controls, lifecycle, state files, and synthetic PCM audio. It does not execute GBA instructions.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            FrameSurface(
                frame = frame,
                integerScaling = settings.integerScaling,
                smoothing = settings.displaySmoothing,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
            )

            if (settings.showPerformanceOverlay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${metrics.presentedFps.toInt()} fps", style = MaterialTheme.typography.labelLarge)
                    Text("${metrics.speedPercent.toInt()}%", style = MaterialTheme.typography.labelLarge)
                    Text(session.phase.name.lowercase().replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.labelLarge)
                }
            }

            if (settings.showTouchControls) {
                PlayerControls(settings.touchControlOpacity, settings.hapticsEnabled, onPressed = viewModel::setButtonPressed)
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Gamepad, null)
                        Text("Touch controls are hidden. Use a connected controller or enable them in You → Controls.")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 1f, 2f, 4f, settings.fastForwardSpeed).distinct().sorted().forEach { option ->
                    FilledTonalButton(
                        onClick = {
                            speed = option
                            viewModel.setSessionSpeed(option)
                        },
                        enabled = speed != option
                    ) { Text("${option.toString().removeSuffix(".0")}×") }
                }
            }
        }
    }

    if (menuOpen) {
        SessionMenu(
            isRunning = session.phase == SessionPhase.RUNNING,
            canSaveState = viewModel.coreDescriptor.supportsSaveStates,
            selectedSlot = selectedSlot,
            onSlotSelected = { selectedSlot = it },
            canRewind = viewModel.coreDescriptor.supportsRewind,
            hasCheats = gamePacks.isNotEmpty(),
            activeCheatCount = activeCheatIds.size,
            onDismiss = { menuOpen = false },
            onTogglePause = {
                viewModel.togglePause()
                menuOpen = false
            },
            onSave = {
                viewModel.saveState(selectedSlot)
                menuOpen = false
            },
            onLoad = {
                viewModel.loadState(selectedSlot)
                menuOpen = false
            },
            onRewind = {
                viewModel.rewindSession()
                menuOpen = false
            },
            onScreenshot = {
                viewModel.captureScreenshot()
                menuOpen = false
            },
            onReset = {
                viewModel.resetGame()
                menuOpen = false
            },
            onCheats = {
                menuOpen = false
                cheatsOpen = true
            },
            onExit = {
                menuOpen = false
                onExit()
            }
        )
    }
    if (cheatsOpen) {
        RetraCodesDialog(
            packs = gamePacks,
            activeCheatIds = activeCheatIds,
            onActivate = viewModel::activateCheat,
            onClear = viewModel::clearCheats,
            onDismiss = { cheatsOpen = false }
        )
    }
}

@Composable
private fun FrameSurface(
    frame: VideoFrame?,
    integerScaling: Boolean,
    smoothing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(3f / 2f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 5.dp,
        shadowElevation = 8.dp
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context -> EmulationSurfaceView(context) },
                modifier = Modifier.fillMaxSize(),
                update = { surface ->
                    surface.configure(integerScaling, smoothing)
                    frame?.let(surface::submitFrame)
                }
            )
            if (frame == null) {
                Text("Waiting for the first native frame…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    controlOpacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 560.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DPad(controlOpacity, hapticsEnabled, onPressed)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PressControl("B", EmulatorButton.B, Modifier.size(62.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("A", EmulatorButton.A, Modifier.size(70.dp), controlOpacity, hapticsEnabled, onPressed)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PressControl("L", EmulatorButton.L, Modifier.size(48.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("Select", EmulatorButton.SELECT, Modifier.size(width = 68.dp, height = 36.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("Start", EmulatorButton.START, Modifier.size(width = 68.dp, height = 36.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("R", EmulatorButton.R, Modifier.size(48.dp), controlOpacity, hapticsEnabled, onPressed)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PressControl("↶ Rewind", EmulatorButton.REWIND, Modifier.size(width = 110.dp, height = 38.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("FF", EmulatorButton.FAST_FORWARD, Modifier.size(width = 76.dp, height = 38.dp), controlOpacity, hapticsEnabled, onPressed)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DPad(controlOpacity, hapticsEnabled, onPressed)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PressControl("Select", EmulatorButton.SELECT, Modifier.size(width = 72.dp, height = 38.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("Start", EmulatorButton.START, Modifier.size(width = 72.dp, height = 38.dp), controlOpacity, hapticsEnabled, onPressed)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PressControl("L", EmulatorButton.L, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("↶", EmulatorButton.REWIND, Modifier.size(48.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("FF", EmulatorButton.FAST_FORWARD, Modifier.size(48.dp), controlOpacity, hapticsEnabled, onPressed)
                        PressControl("R", EmulatorButton.R, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PressControl("B", EmulatorButton.B, Modifier.size(64.dp), controlOpacity, hapticsEnabled, onPressed)
                    PressControl("A", EmulatorButton.A, Modifier.size(72.dp), controlOpacity, hapticsEnabled, onPressed)
                }
            }
        }
    }
}

@Composable
private fun DPad(
    controlOpacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PressControl("↑", EmulatorButton.UP, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
        Row {
            PressControl("←", EmulatorButton.LEFT, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
            Spacer(Modifier.size(54.dp))
            PressControl("→", EmulatorButton.RIGHT, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
        }
        PressControl("↓", EmulatorButton.DOWN, Modifier.size(54.dp), controlOpacity, hapticsEnabled, onPressed)
    }
}

@Composable
private fun PressControl(
    label: String,
    button: EmulatorButton,
    modifier: Modifier,
    controlOpacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier.pointerInput(button) {
            detectTapGestures(
                onPress = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPressed(button, true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onPressed(button, false)
                    }
                }
            )
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = controlOpacity.coerceIn(0.25f, 1f)),
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionMenu(
    isRunning: Boolean,
    canSaveState: Boolean,
    selectedSlot: Int,
    onSlotSelected: (Int) -> Unit,
    canRewind: Boolean,
    hasCheats: Boolean,
    activeCheatCount: Int,
    onDismiss: () -> Unit,
    onTogglePause: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onRewind: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit,
    onCheats: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session controls") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTogglePause, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }
                Text("Save-state slot", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..4).forEach { slot ->
                        FilledTonalButton(
                            onClick = { onSlotSelected(slot) },
                            enabled = canSaveState,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (slot == selectedSlot) "• $slot" else slot.toString())
                        }
                    }
                }
                Button(onClick = onSave, enabled = canSaveState, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Save to slot $selectedSlot")
                }
                Button(onClick = onLoad, enabled = canSaveState, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Load slot $selectedSlot")
                }
                Button(onClick = onRewind, enabled = canRewind, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Replay, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Rewind one snapshot")
                }
                Button(onClick = onScreenshot, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Capture screenshot")
                }
                Button(onClick = onCheats, enabled = hasCheats, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Code, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (activeCheatCount > 0) "Retra Codes · $activeCheatCount active" else "Retra Codes")
                }
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.RestartAlt, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Reset session")
                }
            }
        },
        confirmButton = {
            Button(onClick = onExit) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.size(8.dp))
                Text("Exit")
            }
        },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Close menu") } }
    )
}

@Composable
private fun RetraCodesDialog(
    packs: List<app.retra.emulator.data.StoredCheatPack>,
    activeCheatIds: Set<String>,
    onActivate: (app.retra.emulator.data.StoredCheatPack, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retra Codes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Activating a code creates a protected pre-cheat save state. Achievements with integrity requirements are paused while cheats are active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                packs.forEach { stored ->
                    Text(stored.provider, style = MaterialTheme.typography.titleMedium)
                    stored.pack.cheats.forEach { cheat ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cheat.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${cheat.format.name.lowercase().replace('_', ' ')} · ${cheat.risk.name.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onActivate(stored, cheat.id) }) {
                                Text(if (cheat.id in activeCheatIds) "Active" else "Activate")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            Button(onClick = onClear, enabled = activeCheatIds.isNotEmpty()) {
                Icon(Icons.Default.ClearAll, null)
                Spacer(Modifier.size(8.dp))
                Text("Clear all")
            }
        },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Close") } }
    )
}
