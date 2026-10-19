package app.retra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import app.retra.ui.theme.Retra
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The pastel sky behind every screen: a base colour lit by three soft light pools that drift on a
 * 40 s loop (updated ~15 times a second — the motion is far too slow to need more). Static when
 * motion is reduced.
 */
@Composable
fun AmbientSky(modifier: Modifier = Modifier) {
    val c = Retra.colors
    val reduce = Retra.reduceMotion
    var phase by remember { mutableFloatStateOf(0.18f) }
    if (!reduce) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            var last = 0L
            while (true) {
                withFrameMillis { now ->
                    if (now - last >= 66) {
                        last = now
                        phase = (((now - start) % 40_000L) / 40_000f + 0.18f) % 1f
                    }
                }
            }
        }
    }
    // Own layer: the ~15 fps redraw re-records only the sky, not the screen above it.
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        drawRect(c.sky)
        val t = phase * 2f * PI.toFloat()
        val w = size.width
        val h = size.height
        val r = maxOf(w, h)
        glow(c.skyGlowA, Offset(w * (0.15f + 0.08f * cos(t)), h * (0.08f + 0.05f * sin(t))), r * 0.75f, 0.95f)
        glow(c.skyGlowB, Offset(w * (0.95f + 0.06f * sin(t + 1.3f)), h * (0.42f + 0.06f * cos(t + 1.3f))), r * 0.62f, 0.85f)
        glow(c.skyGlowC, Offset(w * (0.25f + 0.07f * sin(t + 2.6f)), h * (0.98f + 0.04f * cos(t + 2.6f))), r * 0.7f, 0.9f)
    }
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float, strength: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = strength),
            0.45f to color.copy(alpha = strength * 0.45f),
            1f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
