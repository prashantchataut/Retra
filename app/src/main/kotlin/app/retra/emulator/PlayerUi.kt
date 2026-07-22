package app.retra.emulator

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.SessionPhase
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.ControlVisualStyle
import app.retra.core.model.GameRecord
import app.retra.core.model.ScreenScalingMode
import app.retra.emulation.api.CoreTier
import app.retra.emulation.api.VideoFrame
import kotlinx.coroutines.delay

/**
 * Retra 2.3 player shell.
 *
 * The video surface remains the visual priority. Session actions are reachable in one tap,
 * touch controls adapt to compact and wide windows, and every visual control preference is
 * persisted through SettingsRepository rather than held as a one-off preview state.
 */
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
    val baseSettings by viewModel.settings.collectAsStateWithLifecycle()
    val launchProfiles by viewModel.gameLaunchProfiles.collectAsStateWithLifecycle()
    val settings = remember(baseSettings, launchProfiles, game.sha256) {
        viewModel.effectivePlayerSettings(game, baseSettings)
    }
    val activity = LocalContext.current as? Activity

    DisposableEffect(activity, settings.playerImmersiveMode) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (settings.playerImmersiveMode) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val gamePacks = packsByGame[game.sha256.lowercase()].orEmpty()
    var menuOpen by remember { mutableStateOf(false) }
    var customizationOpen by remember { mutableStateOf(false) }
    var cheatsOpen by remember { mutableStateOf(false) }
    var quickActionsVisible by remember { mutableStateOf(true) }
    var selectedSlot by remember { mutableIntStateOf(0) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(game.sha256, settings.autoSaveIntervalMinutes) {
        val minutes = settings.autoSaveIntervalMinutes
        if (minutes <= 0 || !viewModel.coreDescriptor.supportsSaveStates) return@LaunchedEffect
        while (true) {
            delay(minutes * 60_000L)
            if (viewModel.session.value.phase == SessionPhase.RUNNING) {
                viewModel.saveState(AUTO_SAVE_SLOT)
            }
        }
    }

    BackHandler {
        when {
            customizationOpen -> customizationOpen = false
            cheatsOpen -> cheatsOpen = false
            menuOpen -> menuOpen = false
            else -> menuOpen = true
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (!settings.playerImmersiveMode || menuOpen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(game.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                session.phase.name.lowercase().replaceFirstChar(Char::titlecase),
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
                        IconButton(onClick = { customizationOpen = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Customize player")
                        }
                        IconButton(onClick = viewModel::togglePause) {
                            Icon(
                                if (session.phase == SessionPhase.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (session.phase == SessionPhase.RUNNING) "Pause" else "Resume"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            val isWide = maxWidth > maxHeight * 1.12f
            if (isWide) {
                LandscapePlayerLayout(
                    frame = frame,
                    settings = settings,
                    sessionPhase = session.phase,
                    fps = metrics.presentedFps.toInt(),
                    speedPercent = metrics.speedPercent.toInt(),
                    quickActionsVisible = quickActionsVisible,
                    onToggleQuickActions = { quickActionsVisible = !quickActionsVisible },
                    onMenu = { menuOpen = true },
                    onCustomize = { customizationOpen = true },
                    onSave = { viewModel.saveState(selectedSlot) },
                    onLoad = { viewModel.loadState(selectedSlot) },
                    onScreenshot = viewModel::captureScreenshot,
                    onPause = viewModel::togglePause,
                    onPressed = viewModel::setButtonPressed
                )
            } else {
                PortraitPlayerLayout(
                    frame = frame,
                    settings = settings,
                    sessionPhase = session.phase,
                    fps = metrics.presentedFps.toInt(),
                    speedPercent = metrics.speedPercent.toInt(),
                    quickActionsVisible = quickActionsVisible,
                    onToggleQuickActions = { quickActionsVisible = !quickActionsVisible },
                    onMenu = { menuOpen = true },
                    onCustomize = { customizationOpen = true },
                    onSave = { viewModel.saveState(selectedSlot) },
                    onLoad = { viewModel.loadState(selectedSlot) },
                    onScreenshot = viewModel::captureScreenshot,
                    onPause = viewModel::togglePause,
                    onPressed = viewModel::setButtonPressed
                )
            }

            if (viewModel.coreDescriptor.tier == CoreTier.DIAGNOSTIC_PIPELINE) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Build, null)
                        Text(
                            "Diagnostic core: input, rendering, audio, and state plumbing only.",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }

    if (menuOpen) {
        SessionMenu(
            gameTitle = game.title,
            isRunning = session.phase == SessionPhase.RUNNING,
            canSaveState = viewModel.coreDescriptor.supportsSaveStates,
            selectedSlot = selectedSlot,
            onSlotSelected = { selectedSlot = it },
            canRewind = viewModel.coreDescriptor.supportsRewind,
            hasCheats = gamePacks.isNotEmpty(),
            activeCheatCount = activeCheatIds.size,
            selectedSpeed = selectedSpeed,
            speedOptions = listOf(0.5f, 1f, 2f, 3f, settings.fastForwardSpeed).distinct().sorted(),
            onSpeedSelected = {
                selectedSpeed = it
                viewModel.setSessionSpeed(it)
            },
            onDismiss = { menuOpen = false },
            onTogglePause = viewModel::togglePause,
            onSave = { viewModel.saveState(selectedSlot) },
            onLoad = { viewModel.loadState(selectedSlot) },
            onRewind = { viewModel.rewindSession() },
            onScreenshot = viewModel::captureScreenshot,
            onReset = viewModel::resetGame,
            onCustomize = {
                menuOpen = false
                customizationOpen = true
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

    if (customizationOpen) {
        PlayerCustomizationSheet(
            game = game,
            settings = settings,
            hasPerGameProfile = launchProfiles.containsKey(game.sha256.lowercase()),
            viewModel = viewModel,
            onDismiss = { customizationOpen = false }
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
private fun PortraitPlayerLayout(
    frame: VideoFrame?,
    settings: app.retra.core.model.AppSettings,
    sessionPhase: SessionPhase,
    fps: Int,
    speedPercent: Int,
    quickActionsVisible: Boolean,
    onToggleQuickActions: () -> Unit,
    onMenu: () -> Unit,
    onCustomize: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onScreenshot: () -> Unit,
    onPause: () -> Unit,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerChrome(
            phase = sessionPhase,
            fps = fps,
            speedPercent = speedPercent,
            showMetrics = settings.showPerformanceOverlay,
            quickActionsVisible = quickActionsVisible,
            onToggleQuickActions = onToggleQuickActions,
            onMenu = onMenu,
            onCustomize = onCustomize,
            onPause = onPause
        )
        GameViewport(
            frame = frame,
            scalingMode = settings.screenScalingMode,
            smoothing = settings.displaySmoothing,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
        )
        if (quickActionsVisible && settings.showQuickActions) {
            QuickActionBar(
                quickSaveEnabled = settings.quickSaveEnabled,
                onSave = onSave,
                onLoad = onLoad,
                onScreenshot = onScreenshot
            )
        }
        Spacer(Modifier.weight(1f))
        if (settings.showTouchControls && settings.controlLayoutPreset != ControlLayoutPreset.CONTROLLER_FIRST) {
            TouchController(
                preset = settings.controlLayoutPreset,
                style = settings.controlVisualStyle,
                opacity = settings.touchControlOpacity,
                scale = settings.touchControlScale,
                spacing = settings.touchControlSpacing,
                showShoulders = settings.showShoulderButtons,
                hapticsEnabled = settings.hapticsEnabled,
                wide = false,
                onPressed = onPressed
            )
        } else {
            ControllerFirstHint()
        }
    }
}

@Composable
private fun LandscapePlayerLayout(
    frame: VideoFrame?,
    settings: app.retra.core.model.AppSettings,
    sessionPhase: SessionPhase,
    fps: Int,
    speedPercent: Int,
    quickActionsVisible: Boolean,
    onToggleQuickActions: () -> Unit,
    onMenu: () -> Unit,
    onCustomize: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onScreenshot: () -> Unit,
    onPause: () -> Unit,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    Box(Modifier.fillMaxSize().padding(8.dp)) {
        GameViewport(
            frame = frame,
            scalingMode = settings.screenScalingMode,
            smoothing = settings.displaySmoothing,
            modifier = Modifier.fillMaxSize()
        )
        PlayerChrome(
            phase = sessionPhase,
            fps = fps,
            speedPercent = speedPercent,
            showMetrics = settings.showPerformanceOverlay,
            quickActionsVisible = quickActionsVisible,
            onToggleQuickActions = onToggleQuickActions,
            onMenu = onMenu,
            onCustomize = onCustomize,
            onPause = onPause,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(6.dp)
        )
        if (quickActionsVisible && settings.showQuickActions) {
            QuickActionBar(
                quickSaveEnabled = settings.quickSaveEnabled,
                onSave = onSave,
                onLoad = onLoad,
                onScreenshot = onScreenshot,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            )
        }
        if (settings.showTouchControls && settings.controlLayoutPreset != ControlLayoutPreset.CONTROLLER_FIRST) {
            TouchController(
                preset = settings.controlLayoutPreset,
                style = settings.controlVisualStyle,
                opacity = settings.touchControlOpacity,
                scale = settings.touchControlScale,
                spacing = settings.touchControlSpacing,
                showShoulders = settings.showShoulderButtons,
                hapticsEnabled = settings.hapticsEnabled,
                wide = true,
                onPressed = onPressed,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PlayerChrome(
    phase: SessionPhase,
    fps: Int,
    speedPercent: Int,
    showMetrics: Boolean,
    quickActionsVisible: Boolean,
    onToggleQuickActions: () -> Unit,
    onMenu: () -> Unit,
    onCustomize: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Session menu") }
            Text(
                phase.name.lowercase().replaceFirstChar(Char::titlecase),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (showMetrics) {
                Text("$fps fps · $speedPercent%", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleQuickActions) {
                Icon(if (quickActionsVisible) Icons.Default.SkipPrevious else Icons.Default.FastForward, "Toggle quick actions")
            }
            IconButton(onClick = onCustomize) { Icon(Icons.Default.Settings, "Customize controls") }
            FilledIconButton(onClick = onPause) {
                Icon(if (phase == SessionPhase.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow, "Pause or resume")
            }
        }
    }
}

@Composable
private fun GameViewport(
    frame: VideoFrame?,
    scalingMode: ScreenScalingMode,
    smoothing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 8.dp
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context -> EmulationSurfaceView(context) },
                modifier = Modifier.fillMaxSize(),
                update = { surface ->
                    surface.configure(scalingMode, smoothing)
                    frame?.let(surface::submitFrame)
                }
            )
            if (frame == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Gamepad, null, tint = Color.White.copy(alpha = 0.64f))
                    Text("Preparing video…", color = Color.White.copy(alpha = 0.72f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionBar(
    quickSaveEnabled: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (quickSaveEnabled) {
                FilledTonalButton(onClick = onSave, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                    Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
                FilledTonalButton(onClick = onLoad, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Load")
                }
            }
            IconButton(onClick = onScreenshot) { Icon(Icons.Default.CameraAlt, "Screenshot") }
        }
    }
}

@Composable
private fun TouchController(
    preset: ControlLayoutPreset,
    style: ControlVisualStyle,
    opacity: Float,
    scale: Float,
    spacing: Float,
    showShoulders: Boolean,
    hapticsEnabled: Boolean,
    wide: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val leftHanded = preset == ControlLayoutPreset.LEFT_HANDED
    val compact = preset == ControlLayoutPreset.COMPACT
    val base = if (compact) 44.dp else 52.dp
    val action = if (compact) 54.dp else 64.dp
    val scaledBase = base * scale
    val scaledAction = action * scale
    val gap = (8.dp * spacing).coerceAtLeast(4.dp)

    if (wide) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dpad: @Composable () -> Unit = {
                DPad(scaledBase, gap, style, opacity, hapticsEnabled, onPressed)
            }
            val actions: @Composable () -> Unit = {
                ActionCluster(scaledAction, gap, style, opacity, hapticsEnabled, onPressed)
            }
            if (leftHanded) actions() else dpad()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
                if (showShoulders) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        TouchButton("L", EmulatorButton.L, scaledBase, style, opacity, hapticsEnabled, onPressed)
                        TouchButton("R", EmulatorButton.R, scaledBase, style, opacity, hapticsEnabled, onPressed)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    TouchButton("Select", EmulatorButton.SELECT, scaledBase * 1.45f, style, opacity, hapticsEnabled, onPressed, pill = true)
                    TouchButton("Start", EmulatorButton.START, scaledBase * 1.45f, style, opacity, hapticsEnabled, onPressed, pill = true)
                }
            }
            if (leftHanded) dpad() else actions()
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dpad: @Composable () -> Unit = {
                    DPad(scaledBase, gap, style, opacity, hapticsEnabled, onPressed)
                }
                val actions: @Composable () -> Unit = {
                    ActionCluster(scaledAction, gap, style, opacity, hapticsEnabled, onPressed)
                }
                if (leftHanded) actions() else dpad()
                if (leftHanded) dpad() else actions()
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showShoulders) {
                    TouchButton("L", EmulatorButton.L, scaledBase, style, opacity, hapticsEnabled, onPressed)
                    Spacer(Modifier.width(gap))
                }
                TouchButton("Select", EmulatorButton.SELECT, scaledBase * 1.4f, style, opacity, hapticsEnabled, onPressed, pill = true)
                Spacer(Modifier.width(gap))
                TouchButton("Start", EmulatorButton.START, scaledBase * 1.4f, style, opacity, hapticsEnabled, onPressed, pill = true)
                if (showShoulders) {
                    Spacer(Modifier.width(gap))
                    TouchButton("R", EmulatorButton.R, scaledBase, style, opacity, hapticsEnabled, onPressed)
                }
            }
        }
    }
}

@Composable
private fun DPad(
    size: Dp,
    gap: Dp,
    style: ControlVisualStyle,
    opacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TouchButton("↑", EmulatorButton.UP, size, style, opacity, hapticsEnabled, onPressed)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            TouchButton("←", EmulatorButton.LEFT, size, style, opacity, hapticsEnabled, onPressed)
            Spacer(Modifier.size(size))
            TouchButton("→", EmulatorButton.RIGHT, size, style, opacity, hapticsEnabled, onPressed)
        }
        TouchButton("↓", EmulatorButton.DOWN, size, style, opacity, hapticsEnabled, onPressed)
    }
}

@Composable
private fun ActionCluster(
    size: Dp,
    gap: Dp,
    style: ControlVisualStyle,
    opacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        TouchButton("B", EmulatorButton.B, size * 0.9f, style, opacity, hapticsEnabled, onPressed)
        TouchButton("A", EmulatorButton.A, size, style, opacity, hapticsEnabled, onPressed)
    }
}

@Composable
private fun TouchButton(
    label: String,
    button: EmulatorButton,
    size: Dp,
    style: ControlVisualStyle,
    opacity: Float,
    hapticsEnabled: Boolean,
    onPressed: (EmulatorButton, Boolean) -> Unit,
    pill: Boolean = false
) {
    val feedback = LocalRetraFeedback.current
    val alpha = opacity.coerceIn(0.25f, 1f)
    val background = when (style) {
        ControlVisualStyle.GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.42f + alpha * 0.32f)
        ControlVisualStyle.SOLID -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
        ControlVisualStyle.MINIMAL -> Color.Transparent
    }
    val borderAlpha = when (style) {
        ControlVisualStyle.MINIMAL -> 0.34f
        else -> 0.16f
    }
    Surface(
        modifier = Modifier
            .then(if (pill) Modifier.width(size).height(size * 0.58f) else Modifier.size(size))
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        if (hapticsEnabled) feedback(FeedbackCue.GAME_BUTTON)
                        onPressed(button, true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressed(button, false)
                        }
                    }
                )
            },
        shape = if (pill) RoundedCornerShape(50) else CircleShape,
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha)),
        shadowElevation = if (style == ControlVisualStyle.SOLID) 3.dp else 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = if (pill) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(0.94f)
            )
        }
    }
}

@Composable
private fun ControllerFirstHint() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Gamepad, null)
            Column {
                Text("Controller-first mode", fontWeight = FontWeight.SemiBold)
                Text("Touch controls stay out of the way. Open player settings to bring them back.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerCustomizationSheet(
    game: GameRecord,
    settings: app.retra.core.model.AppSettings,
    hasPerGameProfile: Boolean,
    viewModel: RetraViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Player setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Changes apply immediately and persist for future sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasPerGameProfile) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Per-game profile active", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Some values shown here are overridden for ${game.title}. Clear the profile to use global player settings only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FilledTonalButton(onClick = { viewModel.clearGameLaunchProfile(game) }) {
                            Text("Use global settings")
                        }
                    }
                }
            }

            SettingLabel("Layout")
            FlowChips(
                values = ControlLayoutPreset.entries,
                selected = settings.controlLayoutPreset,
                label = {
                    when (it) {
                        ControlLayoutPreset.CLASSIC -> "Classic"
                        ControlLayoutPreset.COMPACT -> "Compact"
                        ControlLayoutPreset.LEFT_HANDED -> "Left-handed"
                        ControlLayoutPreset.CONTROLLER_FIRST -> "Controller first"
                    }
                },
                onSelected = viewModel::setControlLayoutPreset
            )

            SettingLabel("Control surface")
            FlowChips(
                values = ControlVisualStyle.entries,
                selected = settings.controlVisualStyle,
                label = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                onSelected = viewModel::setControlVisualStyle
            )

            SliderSetting("Control size", settings.touchControlScale, 0.72f..1.35f, viewModel::setTouchControlScale)
            SliderSetting("Control spacing", settings.touchControlSpacing, 0.72f..1.4f, viewModel::setTouchControlSpacing)
            SliderSetting("Control opacity", settings.touchControlOpacity, 0.25f..1f, viewModel::setTouchControlOpacity)

            SettingLabel("Screen scaling")
            FlowChips(
                values = ScreenScalingMode.entries,
                selected = settings.screenScalingMode,
                label = {
                    when (it) {
                        ScreenScalingMode.FIT -> "Fit"
                        ScreenScalingMode.FILL -> "Fill / crop"
                        ScreenScalingMode.INTEGER -> "Pixel-perfect"
                    }
                },
                onSelected = viewModel::setScreenScalingMode
            )

            ToggleRow("Touch controls", "Show the on-screen controller.", settings.showTouchControls, viewModel::setShowTouchControls)
            ToggleRow("Shoulder buttons", "Show L and R on the main controller.", settings.showShoulderButtons, viewModel::setShowShoulderButtons)
            ToggleRow("Quick actions", "Keep save, load, and capture within one tap.", settings.showQuickActions, viewModel::setShowQuickActions)
            ToggleRow("Quick save", "Show quick save and load in the action bar.", settings.quickSaveEnabled, viewModel::setQuickSaveEnabled)
            ToggleRow("Immersive player", "Hide system bars while playing.", settings.playerImmersiveMode, viewModel::setPlayerImmersiveMode)
            ToggleRow("Performance overlay", "Show frame rate and emulation speed.", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
            ToggleRow("Image smoothing", "Smooth scaled pixels. Disable for crisp pixel art.", settings.displaySmoothing, viewModel::setDisplaySmoothing)

            SettingLabel("Automatic save-state")
            FlowChips(
                values = listOf(0, 3, 5, 10, 15),
                selected = settings.autoSaveIntervalMinutes,
                label = { if (it == 0) "Off" else "$it min" },
                onSelected = viewModel::setAutoSaveIntervalMinutes
            )
            Text(
                "Automatic saves use a dedicated rotating slot and never overwrite your selected manual slot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text("Hardware controller", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ControllerStudioPanel(viewModel)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Surface(
        onClick = { onChecked(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun <T> FlowChips(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(values) { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value), maxLines = 1) }
            )
        }
    }
}

@Composable
private fun SessionMenu(
    gameTitle: String,
    isRunning: Boolean,
    canSaveState: Boolean,
    selectedSlot: Int,
    onSlotSelected: (Int) -> Unit,
    canRewind: Boolean,
    hasCheats: Boolean,
    activeCheatCount: Int,
    selectedSpeed: Float,
    speedOptions: List<Float>,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    onTogglePause: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onRewind: () -> Unit,
    onScreenshot: () -> Unit,
    onReset: () -> Unit,
    onCustomize: () -> Unit,
    onCheats: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Session")
                Text(gameTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onTogglePause()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }

                Text("Save-state slot", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..4).forEach { slot ->
                        FilterChip(
                            selected = slot == selectedSlot,
                            onClick = { onSlotSelected(slot) },
                            label = { Text(slot.toString()) },
                            enabled = canSaveState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onSave, enabled = canSaveState, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                    FilledTonalButton(onClick = onLoad, enabled = canSaveState, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Load")
                    }
                }

                Text("Speed", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(speedOptions) { speed ->
                        FilterChip(
                            selected = speed == selectedSpeed,
                            onClick = { onSpeedSelected(speed) },
                            label = { Text("${speed.toString().removeSuffix(".0")}×") }
                        )
                    }
                }

                HorizontalDivider()
                MenuAction("Rewind one snapshot", Icons.Default.Replay, canRewind) {
                    onRewind(); onDismiss()
                }
                MenuAction("Capture screenshot", Icons.Default.CameraAlt, true) {
                    onScreenshot(); onDismiss()
                }
                MenuAction("Customize player", Icons.Default.Settings, true, onCustomize)
                MenuAction(
                    if (activeCheatCount > 0) "Retra Codes · $activeCheatCount active" else "Retra Codes",
                    Icons.Default.Code,
                    hasCheats,
                    onCheats
                )
                MenuAction("Reset game", Icons.Default.RestartAlt, true) {
                    onReset(); onDismiss()
                }
            }
        },
        confirmButton = {
            Button(onClick = onExit) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(6.dp))
                Text("Save and exit")
            }
        },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Back to game") } }
    )
}

@Composable
private fun MenuAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Retra creates a protected pre-cheat state before activation. Integrity-restricted achievements pause while codes are active.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                packs.forEach { stored ->
                    Text(stored.provider, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    stored.pack.cheats.forEach { cheat ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(cheat.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${cheat.format.name.lowercase().replace('_', ' ')} · ${cheat.risk.name.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(onClick = { onActivate(stored, cheat.id) }, enabled = cheat.id !in activeCheatIds) {
                                    Text(if (cheat.id in activeCheatIds) "Active" else "Activate")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClear, enabled = activeCheatIds.isNotEmpty()) {
                Icon(Icons.Default.DeleteSweep, null)
                Spacer(Modifier.width(6.dp))
                Text("Clear all")
            }
        },
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Close") } }
    )
}

private const val AUTO_SAVE_SLOT = 9
