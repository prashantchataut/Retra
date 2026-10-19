package app.retra.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.input.InputManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.retra.AppContainer
import app.retra.data.Cheat
import app.retra.data.ScreenFilter
import app.retra.data.Settings
import app.retra.emu.InputMapper
import app.retra.emu.InputRouter
import app.retra.emu.SurfaceRenderer
import app.retra.engine.GbaKey
import app.retra.ui.LocalAppActions
import app.retra.ui.components.AmbientSky
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.CoverImages
import app.retra.ui.components.CoverModel
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassIconButton
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.Mochi
import app.retra.ui.components.RetraButton
import app.retra.ui.components.Tag
import app.retra.ui.components.glassHighlights
import app.retra.ui.components.pressScale
import app.retra.ui.components.sharedGameBounds
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything the player UI can do; one object keeps the stateless content easy to preview/test. */
class PlayerCallbacks(
    val onKeys: (Int) -> Unit = {},
    val onMenu: (Boolean) -> Unit = {},
    val onFfToggle: () -> Unit = {},
    val onFfHold: (Boolean) -> Unit = {},
    val onRewind: (Boolean) -> Unit = {},
    val onSaveState: (Int) -> Unit = {},
    val onLoadState: (Int) -> Unit = {},
    val onScreenshot: () -> Unit = {},
    val onShare: (String) -> Unit = {},
    val onReset: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onCheats: (List<Cheat>) -> Unit = {},
    val onSettings: ((Settings) -> Settings) -> Unit = {},
)

@Composable
fun PlayerScreen(container: AppContainer, gameId: String, slot: Int, origin: String) {
    val vm: PlayerViewModel = viewModel(key = "player-$gameId-$slot", factory = PlayerViewModel.factory(container, gameId, slot))
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val renderer = remember { SurfaceRenderer() }
    val firstFrame by (vm.session?.firstFrameShown ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val fps by (vm.session?.fps ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0f) }).collectAsState()

    ImmersiveMode()
    renderer.smooth = settings.screenFilter == ScreenFilter.SMOOTH
    renderer.integerScale = settings.integerScale
    LaunchedEffect(state.status) { vm.session?.frameSink = renderer }

    // Pause when the app goes to the background; resume when it comes back.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> vm.onHostResume()
                Lifecycle.Event.ON_PAUSE -> vm.onHostPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.onHostResume()
        onDispose {
            lifecycle.removeObserver(obs)
            vm.onHostPause()
        }
    }

    val gamepad = rememberGamepadConnected()
    PhysicalInput(vm)

    val exit: () -> Unit = {
        scope.launch {
            vm.finish()
            actions.back()
        }
    }
    BackHandler(enabled = !state.exiting) { vm.openMenu(!state.menuOpen) }

    PlayerContent(
        state = state,
        settings = settings,
        gamepadConnected = gamepad,
        fps = fps,
        origin = origin,
        gameId = gameId,
        callbacks = PlayerCallbacks(
            onKeys = vm::setTouchKeys,
            onMenu = vm::openMenu,
            onFfToggle = vm::toggleFastForward,
            onFfHold = vm::holdFastForward,
            onRewind = vm::setRewinding,
            onSaveState = { vm.saveState(it) },
            onLoadState = { vm.loadState(it) },
            onScreenshot = { vm.screenshot(context) },
            onShare = { path ->
                val uri = vm.shareUri(context, path)
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Share screenshot"))
            },
            onReset = { vm.reset() },
            onExit = exit,
            onCheats = { vm.setCheats(it) },
            onSettings = { t -> container.settings.update(t) },
        ),
        screen = { mod ->
            Box(mod.background(Color(0xFF0E0C1A))) {
                AndroidView(
                    factory = { ctx -> SurfaceView(ctx).apply { holder.addCallback(renderer) } },
                    modifier = Modifier.fillMaxSize(),
                )
                renderer.background = Color(0xFF0E0C1A).toArgb()
                if (!firstFrame) ScreenPlaceholder(state, Modifier.fillMaxSize())
            }
        },
    )
}

@Composable
private fun ScreenPlaceholder(state: PlayerUiState, modifier: Modifier) {
    Box(modifier) {
        val model = CoverModel(state.title, "", state.title.hashCode().toLong(), assetPath = state.coverAsset, filePath = state.lastScreenPath)
        GameCover(model, Modifier.fillMaxSize(), showTitle = false)
        if (state.status is PlayerStatus.Loading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Mochi(mood = MochiMood.LOADING, size = 72.dp, description = "Loading the game")
            }
        }
    }
}

/** Stateless player UI. [screen] draws the game image (SurfaceView in the app, a bitmap in tests). */
@Composable
fun PlayerContent(
    state: PlayerUiState,
    settings: Settings,
    gamepadConnected: Boolean,
    fps: Float,
    origin: String,
    gameId: String,
    callbacks: PlayerCallbacks,
    screen: @Composable (Modifier) -> Unit,
    menuPage: MenuPage = MenuPage.MAIN,
    pressedPreview: Int = 0,
) {
    val actions = LocalAppActions.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        AmbientSky()
        val landscape = maxWidth > maxHeight
        val failed = state.status as? PlayerStatus.Failed
        if (failed != null) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
                MochiMessage(
                    "This game couldn't start", failed.message, mood = MochiMood.SURPRISED,
                    action = { RetraButton("Back to my games", actions::back, style = ButtonStyle.SECONDARY) },
                )
            }
            return@BoxWithConstraints
        }
        val showControls = !gamepadConnected
        if (landscape) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                GamepadLayer(
                    landscape = true, opacity = if (showControls) settings.controlsOpacity else 0f, scale = settings.controlsScale * 0.92f,
                    haptics = settings.haptics, onKeys = callbacks.onKeys, pressedPreview = pressedPreview,
                    centre = {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(1.5f)
                                .align(Alignment.Center)
                                .sharedGameBounds("game-$origin-$gameId")
                                .clip(Radius.smallShape),
                        ) { screen(Modifier.fillMaxSize()) }
                    },
                )
                AuxButtons(state, settings, fps, callbacks, Modifier.align(Alignment.TopCenter).padding(top = 4.dp), compact = true)
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AuxButtons(state, settings, fps, callbacks, Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.xs), compact = false)
                GlassSurface(
                    Modifier
                        .padding(horizontal = Space.s)
                        .fillMaxWidth()
                        .sharedGameBounds("game-$origin-$gameId"),
                    shape = Radius.largeShape,
                    elevation = Elevation.floating,
                ) {
                    Box(Modifier.padding(Space.xs).fillMaxWidth().aspectRatio(1.5f).clip(Radius.mediumShape)) { screen(Modifier.fillMaxSize()) }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (showControls) {
                        GamepadLayer(
                            landscape = false, opacity = settings.controlsOpacity, scale = settings.controlsScale,
                            haptics = settings.haptics, onKeys = callbacks.onKeys, pressedPreview = pressedPreview,
                        )
                    } else {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SportsEsports, null, tint = Retra.colors.inkMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(Space.xs))
                            Text("Controller connected", style = RetraType.label, color = Retra.colors.inkMuted)
                        }
                    }
                }
            }
        }
        if (state.exiting) {
            Box(Modifier.fillMaxSize().background(Retra.colors.scrim), contentAlignment = Alignment.Center) {
                GlassSurface(shape = Radius.largeShape, strong = true) {
                    Column(Modifier.padding(Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
                        Mochi(mood = MochiMood.LOADING, size = 72.dp)
                        Spacer(Modifier.height(Space.xs))
                        Text("Saving your spot…", style = RetraType.bodyStrong, color = Retra.colors.ink)
                    }
                }
            }
        }
        QuickMenu(state, settings, callbacks, initialPage = menuPage)
    }
}

@Composable
private fun AuxButtons(state: PlayerUiState, settings: Settings, fps: Float, cb: PlayerCallbacks, modifier: Modifier, compact: Boolean) {
    val c = Retra.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        GlassIconButton(Icons.Rounded.Menu, "Open menu", { cb.onMenu(true) }, size = if (compact) 44.dp else 48.dp, blur = false)
        if (!compact) {
            Column(Modifier.weight(1f)) {
                Text(state.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val status = buildList {
                    if (state.fastForward) add("${settings.fastForwardSpeed}× speed")
                    if (state.rewinding) add("Rewinding")
                    if (settings.showFps && fps > 0f) add("%.0f fps".format(fps))
                }
                if (status.isNotEmpty()) Text(status.joinToString(" · "), style = RetraType.caption, color = c.inkMuted)
            }
        } else if (settings.showFps && fps > 0f) {
            Tag("%.0f fps".format(fps))
        }
        HoldButton(Icons.Rounded.FastRewind, "Rewind (hold)", active = state.rewinding, onHold = cb.onRewind, onTap = null, compact = compact)
        HoldButton(Icons.Rounded.FastForward, "Fast-forward (tap to toggle, hold for a burst)", active = state.fastForward, onHold = cb.onFfHold, onTap = cb.onFfToggle, compact = compact)
    }
}

/** Press-and-hold button; [onTap] fires instead when released quickly (if given). */
@Composable
private fun HoldButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onHold: (Boolean) -> Unit, onTap: (() -> Unit)?, compact: Boolean) {
    val c = Retra.colors
    val hold by rememberUpdatedState(onHold)
    val tap by rememberUpdatedState(onTap)
    val size = if (compact) 44.dp else 48.dp
    GlassSurface(
        Modifier
            .size(size)
            .semantics { contentDescription = label; role = Role.Button }
            .pointerInput(Unit) {
                coroutineScopeDetect(hold, { tap })
            },
        shape = Radius.pillShape,
        elevation = Elevation.raised / 2,
        wash = if (active) c.accent else null,
    ) {
        Icon(icon, null, tint = if (active) c.onAccent else c.ink, modifier = Modifier.align(Alignment.Center).size(22.dp))
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.coroutineScopeDetect(hold: (Boolean) -> Unit, tap: () -> (() -> Unit)?) {
    kotlinx.coroutines.coroutineScope {
        detectTapGestures(onPress = {
            var held = false
            val t = tap()
            val job: Job = launch {
                if (t != null) delay(280)
                held = true
                hold(true)
            }
            tryAwaitRelease()
            job.cancel()
            if (held) hold(false) else t?.invoke()
        })
    }
}

/** Hides system bars while playing (swipe to peek) and keeps the screen on. */
@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** True while a physical gamepad/joystick is connected. */
@Composable
private fun rememberGamepadConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val im = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        fun check() {
            connected = im?.inputDeviceIds?.any { InputMapper.isGamepad(im.getInputDevice(it)) } == true
        }
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) = check()
            override fun onInputDeviceRemoved(deviceId: Int) = check()
            override fun onInputDeviceChanged(deviceId: Int) = check()
        }
        check()
        im?.registerInputDeviceListener(listener, null)
        onDispose { im?.unregisterInputDeviceListener(listener) }
    }
    return connected
}

/** Routes hardware keys and sticks (via MainActivity) into the session. */
@Composable
private fun PhysicalInput(vm: PlayerViewModel) {
    DisposableEffect(vm) {
        var buttons = 0
        var axes = 0
        var triggerFf = false
        var triggerRw = false
        val handler = object : InputRouter.Handler {
            override fun onKeyEvent(event: KeyEvent): Boolean {
                if (event.repeatCount > 0 && event.action == KeyEvent.ACTION_DOWN) return InputMapper.gbaKey(event.keyCode) != 0
                val down = event.action == KeyEvent.ACTION_DOWN
                val bit = InputMapper.gbaKey(event.keyCode)
                if (bit != 0) {
                    buttons = if (down) buttons or bit else buttons and bit.inv()
                    vm.setPadKeys(buttons or axes)
                    return true
                }
                when (InputMapper.hotkey(event.keyCode)) {
                    InputMapper.Hotkey.FAST_FORWARD -> { vm.holdFastForward(down); return true }
                    InputMapper.Hotkey.REWIND -> { vm.setRewinding(down); return true }
                    InputMapper.Hotkey.MENU -> { if (down) vm.openMenu(!vm.state.value.menuOpen); return true }
                    null -> return false
                }
            }

            override fun onMotionEvent(event: MotionEvent): Boolean {
                axes = InputMapper.axesToKeys(event)
                vm.setPadKeys(buttons or axes)
                val (l, r) = InputMapper.triggers(event)
                if (r != triggerFf) { triggerFf = r; vm.holdFastForward(r) }
                if (l != triggerRw) { triggerRw = l; vm.setRewinding(l) }
                return true
            }
        }
        InputRouter.handler = handler
        onDispose {
            if (InputRouter.handler === handler) InputRouter.handler = null
            vm.setPadKeys(0)
        }
    }
}

/** Test/preview helper: draws a fixed bitmap as the game screen. */
@Composable
fun BitmapScreen(bitmap: ImageBitmap, smooth: Boolean, modifier: Modifier) {
    Image(
        bitmap, "Game screen", contentScale = ContentScale.Fit,
        filterQuality = if (smooth) FilterQuality.Low else FilterQuality.None,
        modifier = modifier.background(Color(0xFF0E0C1A)),
    )
}

@Suppress("unused")
private val keep = listOf(GbaKey.A, Modifier.widthIn().hashCode(), CoverImages.hashCode(), Modifier.glassHighlights(Radius.smallShape, app.retra.ui.theme.LightColors).hashCode())
