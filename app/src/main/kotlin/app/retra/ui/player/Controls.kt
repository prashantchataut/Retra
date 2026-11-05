package app.retra.ui.player

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.engine.GbaKey
import app.retra.ui.theme.Nunito
import androidx.compose.ui.text.font.FontWeight
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import kotlin.math.atan2
import kotlin.math.hypot

/** Control ids for hit testing; GBA keys plus the D-pad region. */
private const val DPAD = -1

/**
 * Hit-test registry shared by the control visuals (which report their bounds) and the single
 * multi-touch pointer handler on the layer. Rects are in the layer's coordinate space.
 */
class ControlRegistry {
    val rects = HashMap<Int, Rect>()
    var origin = Offset.Zero

    fun place(id: Int, coords: LayoutCoordinates) {
        val p = coords.positionInRoot() - origin
        rects[id] = Rect(p, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
    }

    /** Computes the GBA key mask for all pointer positions. [slop] widens buttons a little. */
    fun maskFor(points: List<Offset>, slop: Float): Int {
        var mask = 0
        for (p in points) {
            for ((id, r) in rects) {
                if (id == DPAD) {
                    val c = r.center
                    val radius = r.width / 2 + slop
                    val dx = p.x - c.x
                    val dy = p.y - c.y
                    val dist = hypot(dx, dy)
                    if (dist > radius * 1.25f || dist < r.width * 0.12f) continue
                    mask = mask or dpadBits(dx, dy)
                } else if (r.inflate(slop).contains(p)) {
                    mask = mask or id
                }
            }
        }
        return mask
    }

    companion object {
        /** 8-way D-pad by angle: 45° sectors centred on the 4 directions and the 4 diagonals. */
        fun dpadBits(dx: Float, dy: Float): Int {
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).let { if (it < 0) it + 360 else it }
            val sector = ((angle + 22.5) / 45).toInt() % 8
            return when (sector) {
                0 -> GbaKey.RIGHT
                1 -> GbaKey.RIGHT or GbaKey.DOWN
                2 -> GbaKey.DOWN
                3 -> GbaKey.DOWN or GbaKey.LEFT
                4 -> GbaKey.LEFT
                5 -> GbaKey.LEFT or GbaKey.UP
                6 -> GbaKey.UP
                else -> GbaKey.UP or GbaKey.RIGHT
            }
        }
    }
}

/**
 * The touch gamepad layer. One pointer handler tracks every finger, so you can slide from B to A
 * or roll across the D-pad, and press several buttons at once. Haptic tick on each newly pressed key.
 */
@Composable
fun GamepadLayer(
    landscape: Boolean,
    opacity: Float,
    scale: Float,
    haptics: Boolean,
    onKeys: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pressedPreview: Int = 0,
    skin: SkinConfig = SkinConfig(),
    centre: @Composable BoxScope.() -> Unit = {},
    leftAux: @Composable () -> Unit = {},
    rightAux: @Composable () -> Unit = {},
) {
    val registry = remember { ControlRegistry() }
    val pressed = remember { mutableIntStateOf(pressedPreview) }
    val view = LocalView.current
    val slop = with(LocalDensity.current) { 10.dp.toPx() }
    val keysCallback by rememberUpdatedState(onKeys)
    val hapticsOn by rememberUpdatedState(haptics)
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { registry.origin = it.positionInRoot() }
            .semantics { contentDescription = "Game controls" }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val points = event.changes.filter { it.pressed }.map { it.position }
                        event.changes.forEach { it.consume() }
                        val mask = registry.maskFor(points, slop)
                        val old = pressed.intValue
                        if (mask != old) {
                            if (hapticsOn && (mask and old.inv()) != 0) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            pressed.intValue = mask
                            keysCallback(mask)
                        }
                    }
                }
            },
    ) {
        centre()
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity }) {
            androidx.compose.runtime.CompositionLocalProvider(LocalSkin provides skin) {
                if (landscape) LandscapeControls(registry, pressed, scale) else PortraitControls(registry, pressed, scale)
            }
        }
        if (landscape) {
            // Menu / rewind / fast-forward live between the shoulder buttons and the pads.
            Box(Modifier.align(Alignment.CenterStart).padding(start = 16.dp).offset(y = -(148.dp * scale / 2 + 46.dp))) { leftAux() }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(y = -(148.dp * scale / 2 + 46.dp))) { rightAux() }
        }
    }
}

@Composable
private fun PortraitControls(reg: ControlRegistry, pressed: IntState, scale: Float) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Shoulder("L", GbaKey.L, reg, pressed, scale)
            Shoulder("R", GbaKey.R, reg, pressed, scale)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DPad(reg, pressed, 156.dp * scale)
            FaceButtons(reg, pressed, scale)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Pill("SELECT", GbaKey.SELECT, reg, pressed, scale)
            Box(Modifier.size(24.dp))
            Pill("START", GbaKey.START, reg, pressed, scale)
        }
    }
}

@Composable
private fun LandscapeControls(reg: ControlRegistry, pressed: IntState, scale: Float) {
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start) {
            Shoulder("L", GbaKey.L, reg, pressed, scale)
            DPad(reg, pressed, 148.dp * scale)
            Pill("SELECT", GbaKey.SELECT, reg, pressed, scale)
        }
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            Shoulder("R", GbaKey.R, reg, pressed, scale)
            FaceButtons(reg, pressed, scale)
            Pill("START", GbaKey.START, reg, pressed, scale)
        }
    }
}

@Composable
private fun FaceButtons(reg: ControlRegistry, pressed: IntState, scale: Float) {
    val size = 68.dp * scale
    // B low-left, A high-right, like the real console.
    Box(Modifier.size(size * 2.25f, size * 1.75f)) {
        RoundButton("B", GbaKey.B, reg, pressed, size, Modifier.align(Alignment.BottomStart))
        RoundButton("A", GbaKey.A, reg, pressed, size, Modifier.align(Alignment.TopEnd))
    }
}

/** The player's choice of button look, from Settings. [tint] null follows the palette's accent. */
@androidx.compose.runtime.Immutable
data class SkinConfig(val skin: app.retra.data.ControlSkin = app.retra.data.ControlSkin.GLASS, val tint: Color? = null, val labels: Boolean = true)

val LocalSkin = androidx.compose.runtime.staticCompositionLocalOf { SkinConfig() }

/** Resolved colours and finish for the current skin. */
@androidx.compose.runtime.Immutable
private class SkinSpec(
    val fill: Color,
    val rim: Color,
    val ink: Color,
    val pressed: Color,
    val onPressed: Color,
    val shadow: Color,
    val gloss: Float,
    val glow: Color?,
    val faceA: Color?,
    val faceB: Color?,
    val rimWidth: Float,
    val labels: Boolean,
)

@Composable
private fun skinSpec(): SkinSpec {
    val c = Retra.colors
    val cfg = LocalSkin.current
    val tint = cfg.tint ?: c.accent
    val onTint = if (tint.luminance() > 0.5f) Color(0xFF14231E) else Color.White
    return when (cfg.skin) {
        app.retra.data.ControlSkin.GLASS -> SkinSpec(
            fill = if (c.isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f),
            rim = Color.White.copy(alpha = if (c.isDark) 0.45f else 0.95f),
            ink = if (c.isDark) Color.White.copy(alpha = 0.9f) else c.ink.copy(alpha = 0.8f),
            pressed = tint, onPressed = onTint, shadow = c.shadow, gloss = 0.4f, glow = null, faceA = null, faceB = null, rimWidth = 1.5f, labels = cfg.labels,
        )
        // Solid plastic like a handheld: dark body, coloured face buttons, a hard rim light.
        app.retra.data.ControlSkin.CLASSIC -> SkinSpec(
            fill = Color(0xFF353849), rim = Color.White.copy(alpha = 0.22f), ink = Color(0xFFEDEFF7),
            pressed = tint.copy(alpha = 1f), onPressed = onTint, shadow = Color.Black.copy(alpha = 0.5f), gloss = 0.12f, glow = null,
            faceA = Color(0xFFC84B86), faceB = Color(0xFFA23D70), rimWidth = 1.5f, labels = cfg.labels,
        )
        app.retra.data.ControlSkin.CANDY -> SkinSpec(
            fill = Color(0xFFFFF3E8), rim = Color.White, ink = Color(0xFF47283A),
            pressed = tint, onPressed = onTint, shadow = Color(0xFF8A4F66).copy(alpha = 0.28f), gloss = 0.55f, glow = null,
            faceA = Color(0xFFFF9DBB), faceB = Color(0xFF9BE8C9), rimWidth = 2f, labels = cfg.labels,
        )
        app.retra.data.ControlSkin.NEON -> SkinSpec(
            fill = Color.Black.copy(alpha = 0.18f), rim = tint, ink = tint,
            pressed = tint, onPressed = if (tint.luminance() > 0.5f) Color(0xFF06201A) else Color.White,
            shadow = Color.Transparent, gloss = 0f, glow = tint.copy(alpha = 0.35f), faceA = null, faceB = null, rimWidth = 2f, labels = cfg.labels,
        )
        app.retra.data.ControlSkin.FLAT -> SkinSpec(
            fill = if (c.isDark) Color.White.copy(alpha = 0.18f) else Color(0xFF14231E).copy(alpha = 0.16f), rim = Color.Transparent,
            ink = if (c.isDark) Color.White else Color(0xFF14231E),
            pressed = tint, onPressed = onTint, shadow = Color.Transparent, gloss = 0f, glow = null, faceA = null, faceB = null, rimWidth = 0f, labels = cfg.labels,
        )
    }
}

@Composable
private fun RoundButton(label: String, key: Int, reg: ControlRegistry, pressed: IntState, size: Dp, modifier: Modifier = Modifier) {
    val sk = skinSpec()
    val down = pressed.intValue and key != 0
    val face = if (key == GbaKey.A) sk.faceA else sk.faceB
    val scale by androidx.compose.animation.core.animateFloatAsState(if (down) 0.9f else 1f, androidx.compose.animation.core.spring(0.55f, 900f), label = "press")
    Box(
        modifier
            .size(size)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { skinCircle(sk, if (down) sk.pressed else face ?: sk.fill, down) }
        if (sk.labels) {
            val labelInk = when {
                down -> sk.onPressed
                face != null -> if (face.luminance() > 0.5f) Color(0xFF47283A) else Color.White
                else -> sk.ink
            }
            Text(label, style = RetraType.title.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold), color = labelInk)
        }
    }
}

@Composable
private fun Shoulder(label: String, key: Int, reg: ControlRegistry, pressed: IntState, scale: Float) {
    val sk = skinSpec()
    val down = pressed.intValue and key != 0
    val s by androidx.compose.animation.core.animateFloatAsState(if (down) 0.93f else 1f, androidx.compose.animation.core.spring(0.55f, 900f), label = "press")
    Box(
        Modifier
            .size(104.dp * scale, 48.dp * scale)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { skinPill(sk, if (down) sk.pressed else sk.fill, down) }
        if (sk.labels) Text(label, style = RetraType.headline, color = if (down) sk.onPressed else sk.ink)
    }
}

@Composable
private fun Pill(label: String, key: Int, reg: ControlRegistry, pressed: IntState, scale: Float) {
    val sk = skinSpec()
    val down = pressed.intValue and key != 0
    val s by androidx.compose.animation.core.animateFloatAsState(if (down) 0.93f else 1f, androidx.compose.animation.core.spring(0.55f, 900f), label = "press")
    Box(
        Modifier
            .size(92.dp * scale, 40.dp * scale)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { skinPill(sk, if (down) sk.pressed else sk.fill, down) }
        if (sk.labels) Text(label, style = RetraType.caption, color = if (down) sk.onPressed else sk.ink, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DPad(reg: ControlRegistry, pressed: IntState, size: Dp) {
    val sk = skinSpec()
    val mask = pressed.intValue
    Box(Modifier.size(size).onGloballyPositioned { reg.place(DPAD, it) }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val arm = w * 0.36f
            val r = arm * 0.32f
            val cross = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect((w - arm) / 2, 0f, (w + arm) / 2, w), CornerRadius(r)))
                addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(0f, (w - arm) / 2, w, (w + arm) / 2), CornerRadius(r)))
            }
            sk.glow?.let { drawPath(cross, it, style = Stroke(10.dp.toPx())) }
            if (sk.shadow != Color.Transparent) drawPath(cross, sk.shadow, style = Stroke(6.dp.toPx()))
            drawPath(cross, sk.fill)
            if (sk.gloss > 0f) drawPath(cross, Brush.verticalGradient(listOf(Color.White.copy(alpha = sk.gloss * 0.9f), Color.Transparent), endY = w * 0.5f))
            if (sk.rimWidth > 0f) drawPath(cross, sk.rim, style = Stroke(sk.rimWidth.dp.toPx()))
            fun arm(rect: Rect) = drawRoundRect(sk.pressed, rect.topLeft, rect.size, CornerRadius(r))
            val pad = 2.dp.toPx()
            if (mask and GbaKey.UP != 0) arm(Rect((w - arm) / 2 + pad, pad, (w + arm) / 2 - pad, w * 0.4f))
            if (mask and GbaKey.DOWN != 0) arm(Rect((w - arm) / 2 + pad, w * 0.6f, (w + arm) / 2 - pad, w - pad))
            if (mask and GbaKey.LEFT != 0) arm(Rect(pad, (w - arm) / 2 + pad, w * 0.4f, (w + arm) / 2 - pad))
            if (mask and GbaKey.RIGHT != 0) arm(Rect(w * 0.6f, (w - arm) / 2 + pad, w - pad, (w + arm) / 2 - pad))
            val nub = w * 0.06f
            fun tri(cx: Float, cy: Float, dx: Float, dy: Float, on: Boolean) {
                val p = Path().apply {
                    moveTo(cx + dx * nub, cy + dy * nub)
                    lineTo(cx - dy * nub - dx * nub * 0.2f, cy + dx * nub - dy * nub * 0.2f)
                    lineTo(cx + dy * nub - dx * nub * 0.2f, cy - dx * nub - dy * nub * 0.2f)
                    close()
                }
                drawPath(p, if (on) sk.onPressed else sk.ink.copy(alpha = 0.7f))
            }
            tri(w / 2, w * 0.14f, 0f, -1f, mask and GbaKey.UP != 0)
            tri(w / 2, w * 0.86f, 0f, 1f, mask and GbaKey.DOWN != 0)
            tri(w * 0.14f, w / 2, -1f, 0f, mask and GbaKey.LEFT != 0)
            tri(w * 0.86f, w / 2, 1f, 0f, mask and GbaKey.RIGHT != 0)
            drawCircle(sk.ink.copy(alpha = 0.12f), radius = arm * 0.22f, center = Offset(w / 2, w / 2))
        }
    }
}

private fun DrawScope.skinCircle(sk: SkinSpec, fill: Color, down: Boolean) {
    val r = size.minDimension / 2
    sk.glow?.let { drawCircle(it.copy(alpha = if (down) 0.6f else it.alpha), radius = r + 2.dp.toPx(), style = Stroke(7.dp.toPx())) }
    if (sk.shadow != Color.Transparent) drawCircle(sk.shadow, radius = r, center = center + Offset(0f, 3.dp.toPx()))
    drawCircle(fill, radius = r)
    if (sk.gloss > 0f) drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = sk.gloss), Color.Transparent), startY = 0f, endY = size.height * 0.55f), radius = r)
    if (sk.rimWidth > 0f) drawCircle(sk.rim, radius = r - 0.75.dp.toPx(), style = Stroke(sk.rimWidth.dp.toPx()))
}

private fun DrawScope.skinPill(sk: SkinSpec, fill: Color, down: Boolean) {
    val cr = CornerRadius(size.height / 2)
    sk.glow?.let { drawRoundRect(it.copy(alpha = if (down) 0.6f else it.alpha), cornerRadius = cr, style = Stroke(7.dp.toPx())) }
    if (sk.shadow != Color.Transparent) drawRoundRect(sk.shadow, topLeft = Offset(0f, 3.dp.toPx()), size = size, cornerRadius = cr)
    drawRoundRect(fill, cornerRadius = cr)
    if (sk.gloss > 0f) drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = sk.gloss), Color.Transparent), endY = size.height * 0.6f), cornerRadius = cr)
    if (sk.rimWidth > 0f) drawRoundRect(sk.rim, topLeft = Offset(0.75.dp.toPx(), 0.75.dp.toPx()), size = Size(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()), cornerRadius = cr, style = Stroke(sk.rimWidth.dp.toPx()))
}

/** A small live preview of a skin (used by Settings): the buttons press themselves in a loop. */
@Composable
fun SkinPreview(cfg: SkinConfig, modifier: Modifier = Modifier) {
    val reg = remember { ControlRegistry() }
    val pressed = remember { mutableIntStateOf(0) }
    val reduce = Retra.reduceMotion
    androidx.compose.runtime.LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        val steps = listOf(GbaKey.RIGHT, 0, GbaKey.A, 0, GbaKey.UP, 0, GbaKey.B, 0, GbaKey.START, 0)
        var i = 0
        while (true) {
            pressed.intValue = steps[i % steps.size]
            i++
            kotlinx.coroutines.delay(if (steps[(i - 1) % steps.size] == 0) 260 else 520)
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalSkin provides cfg) {
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            DPad(reg, pressed, 92.dp)
            Pill("START", GbaKey.START, reg, pressed, 0.8f)
            FaceButtons(reg, pressed, 0.72f)
        }
    }
}
