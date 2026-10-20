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
            if (landscape) LandscapeControls(registry, pressed, scale) else PortraitControls(registry, pressed, scale)
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

@Composable
private fun glassColors(): Triple<Color, Color, Color> {
    val c = Retra.colors
    // Controls sit over a dark game frame or the sky: a light, translucent glass in both themes.
    val fill = if (c.isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)
    val rim = Color.White.copy(alpha = if (c.isDark) 0.45f else 0.95f)
    val ink = if (c.isDark) Color.White.copy(alpha = 0.9f) else c.ink.copy(alpha = 0.8f)
    return Triple(fill, rim, ink)
}

@Composable
private fun RoundButton(label: String, key: Int, reg: ControlRegistry, pressed: IntState, size: Dp, modifier: Modifier = Modifier) {
    val c = Retra.colors
    val (fill, rim, ink) = glassColors()
    val down = pressed.intValue and key != 0
    Box(
        modifier
            .size(size)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { val s = if (down) 0.92f else 1f; scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { glassCircle(if (down) c.accent else fill, rim, c.shadow) }
        Text(label, style = RetraType.title.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold), color = if (down) c.onAccent else ink)
    }
}

@Composable
private fun Shoulder(label: String, key: Int, reg: ControlRegistry, pressed: IntState, scale: Float) {
    val c = Retra.colors
    val (fill, rim, ink) = glassColors()
    val down = pressed.intValue and key != 0
    Box(
        Modifier
            .size(104.dp * scale, 48.dp * scale)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { val s = if (down) 0.94f else 1f; scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { glassPill(if (down) c.accent else fill, rim, c.shadow) }
        Text(label, style = RetraType.headline, color = if (down) c.onAccent else ink)
    }
}

@Composable
private fun Pill(label: String, key: Int, reg: ControlRegistry, pressed: IntState, scale: Float) {
    val c = Retra.colors
    val (fill, rim, ink) = glassColors()
    val down = pressed.intValue and key != 0
    Box(
        Modifier
            .size(92.dp * scale, 40.dp * scale)
            .onGloballyPositioned { reg.place(key, it) }
            .graphicsLayer { val s = if (down) 0.94f else 1f; scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { glassPill(if (down) c.accent else fill, rim, c.shadow) }
        Text(label, style = RetraType.caption, color = if (down) c.onAccent else ink, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DPad(reg: ControlRegistry, pressed: IntState, size: Dp) {
    val c = Retra.colors
    val (fill, rim, ink) = glassColors()
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
            // Soft shadow + glass cross.
            drawPath(cross, c.shadow, style = Stroke(6.dp.toPx()))
            drawPath(cross, fill)
            drawPath(cross, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), endY = w * 0.5f))
            drawPath(cross, rim, style = Stroke(1.5.dp.toPx()))
            // Pressed arms.
            fun arm(rect: Rect) = drawRoundRect(c.accent, rect.topLeft, rect.size, CornerRadius(r))
            val pad = 2.dp.toPx()
            if (mask and GbaKey.UP != 0) arm(Rect((w - arm) / 2 + pad, pad, (w + arm) / 2 - pad, w * 0.4f))
            if (mask and GbaKey.DOWN != 0) arm(Rect((w - arm) / 2 + pad, w * 0.6f, (w + arm) / 2 - pad, w - pad))
            if (mask and GbaKey.LEFT != 0) arm(Rect(pad, (w - arm) / 2 + pad, w * 0.4f, (w + arm) / 2 - pad))
            if (mask and GbaKey.RIGHT != 0) arm(Rect(w * 0.6f, (w - arm) / 2 + pad, w - pad, (w + arm) / 2 - pad))
            // Arrow nubs.
            val nub = w * 0.06f
            fun tri(cx: Float, cy: Float, dx: Float, dy: Float, on: Boolean) {
                val p = Path().apply {
                    moveTo(cx + dx * nub, cy + dy * nub)
                    lineTo(cx - dy * nub - dx * nub * 0.2f, cy + dx * nub - dy * nub * 0.2f)
                    lineTo(cx + dy * nub - dx * nub * 0.2f, cy - dx * nub - dy * nub * 0.2f)
                    close()
                }
                drawPath(p, if (on) c.onAccent else ink.copy(alpha = 0.7f))
            }
            tri(w / 2, w * 0.14f, 0f, -1f, mask and GbaKey.UP != 0)
            tri(w / 2, w * 0.86f, 0f, 1f, mask and GbaKey.DOWN != 0)
            tri(w * 0.14f, w / 2, -1f, 0f, mask and GbaKey.LEFT != 0)
            tri(w * 0.86f, w / 2, 1f, 0f, mask and GbaKey.RIGHT != 0)
            drawCircle(ink.copy(alpha = 0.12f), radius = arm * 0.22f, center = Offset(w / 2, w / 2))
        }
    }
}

private fun DrawScope.glassCircle(fill: Color, rim: Color, shadow: Color) {
    val r = size.minDimension / 2
    drawCircle(shadow, radius = r, center = center + Offset(0f, 3.dp.toPx()))
    drawCircle(fill, radius = r)
    drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), startY = 0f, endY = size.height * 0.55f), radius = r)
    drawCircle(rim, radius = r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.glassPill(fill: Color, rim: Color, shadow: Color) {
    val cr = CornerRadius(size.height / 2)
    drawRoundRect(shadow, topLeft = Offset(0f, 3.dp.toPx()), size = size, cornerRadius = cr)
    drawRoundRect(fill, cornerRadius = cr)
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), endY = size.height * 0.6f), cornerRadius = cr)
    drawRoundRect(rim, topLeft = Offset(0.75.dp.toPx(), 0.75.dp.toPx()), size = Size(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()), cornerRadius = cr, style = Stroke(1.5.dp.toPx()))
}
