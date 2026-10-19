package app.retra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.MochiPalette
import app.retra.ui.theme.Retra
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

enum class MochiMood { HAPPY, SLEEPY, SURPRISED, LOADING }

/**
 * Mochi, Retra's mascot: a squishy rice-cake with a leaf on top and shiny eyes.
 * Breathes (gentle squish) and blinks when idle, bounces when loading. Static with reduced motion.
 */
@Composable
fun Mochi(
    modifier: Modifier = Modifier,
    mood: MochiMood = MochiMood.HAPPY,
    size: Dp = 96.dp,
    description: String? = null,
) {
    val reduce = Retra.reduceMotion
    val motion = if (reduce) null else rememberMochiMotion(mood)
    val body = remember { mochiBodyPath() }
    Canvas(
        modifier
            .size(size)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        val squish = motion?.squish?.value ?: 0f
        val hop = motion?.hop?.value ?: 0f
        val blink = motion?.blink?.value ?: 0f
        scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) {
            drawMochi(mood, body, squish, hop, blink)
        }
    }
}

private class MochiMotion(val squish: State<Float>, val hop: State<Float>, val blink: State<Float>)

@Composable
private fun rememberMochiMotion(mood: MochiMood): MochiMotion {
    val t = rememberInfiniteTransition(label = "mochi")
    val loading = mood == MochiMood.LOADING
    val squish = t.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (loading) 380 else 1200, easing = { sin(it * Math.PI.toFloat() - Math.PI.toFloat() / 2) * 0.5f + 0.5f }), RepeatMode.Reverse),
        label = "squish",
    )
    val hop = t.animateFloat(
        initialValue = 0f, targetValue = if (loading) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(760, easing = LinearEasing), RepeatMode.Restart),
        label = "hop",
    )
    val blink = t.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                0f at 0
                0f at 3900
                1f at 3990
                0f at 4110
            },
        ),
        label = "blink",
    )
    return remember(squish, hop, blink) { MochiMotion(squish, hop, blink) }
}

/** Superellipse body in a 100x100 box (p = 2.4), slightly flatter on the bottom. */
private fun mochiBodyPath(): Path {
    val path = Path()
    val cx = 50f
    val cy = 62f
    val a = 38f
    val top = 33f
    val bottom = 28f
    val p = 2.4f
    val n = 96
    for (i in 0..n) {
        val th = (i.toFloat() / n) * 2f * Math.PI.toFloat()
        val c = cos(th)
        val s = sin(th)
        val x = cx + a * sign(c) * abs(c).pow(2f / p)
        val ry = if (s < 0) top else bottom
        val y = cy + ry * sign(s) * abs(s).pow(2f / p)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawMochi(mood: MochiMood, body: Path, squish: Float, hop: Float, blink: Float) {
    // Hop: parabola up, squash on landing.
    val hopY = if (hop > 0f) -16f * (4f * hop * (1f - hop)) else 0f
    val landing = if (hop > 0f) (1f - (4f * hop * (1f - hop))) else 0f
    val sx = 1f + 0.035f * squish + 0.06f * landing * landing
    val sy = 1f - 0.035f * squish - 0.06f * landing * landing

    // Ground shadow shrinks while airborne.
    val shadowW = 58f * (1f + hopY / 40f) * sx
    drawOval(
        color = MochiPalette.outline.copy(alpha = 0.14f),
        topLeft = Offset(50f - shadowW / 2, 90f),
        size = Size(shadowW, 7f),
    )

    translate(top = hopY) {
        scale(sx, sy, pivot = Offset(50f, 90f)) {
            // Leaf behind the body top.
            drawLeaf()
            drawPath(
                body,
                Brush.verticalGradient(
                    0f to MochiPalette.light,
                    0.45f to MochiPalette.base,
                    1f to MochiPalette.shade,
                    startY = 29f, endY = 90f,
                ),
            )
            // Soft cream belly glow.
            drawOval(
                Brush.radialGradient(
                    listOf(MochiPalette.cream.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(50f, 74f), radius = 26f,
                ),
                topLeft = Offset(24f, 56f), size = Size(52f, 34f),
            )
            drawPath(body, MochiPalette.outline, style = Stroke(2.6f))
            // Glossy highlight.
            rotate(-24f, pivot = Offset(32f, 42f)) {
                drawOval(Color.White.copy(alpha = 0.85f), topLeft = Offset(24f, 38.5f), size = Size(15f, 7f))
            }
            drawCircle(Color.White.copy(alpha = 0.8f), radius = 1.8f, center = Offset(43f, 37.5f))
            drawFace(mood, blink)
        }
    }
}

private fun DrawScope.drawLeaf() {
    val stem = Path().apply {
        moveTo(50f, 33f)
        quadraticTo(50.5f, 27f, 53f, 24f)
    }
    drawPath(stem, MochiPalette.leafDark, style = Stroke(2.4f, cap = StrokeCap.Round))
    val leaf = Path().apply {
        moveTo(52f, 25f)
        cubicTo(54f, 13f, 66f, 9f, 75f, 12f)
        cubicTo(73f, 22f, 63f, 29f, 52f, 25f)
        close()
    }
    drawPath(leaf, Brush.linearGradient(listOf(MochiPalette.leafLight, MochiPalette.leaf), start = Offset(55f, 12f), end = Offset(70f, 26f)))
    drawPath(leaf, MochiPalette.leafDark, style = Stroke(2.2f))
    val vein = Path().apply {
        moveTo(54f, 24f)
        quadraticTo(63f, 19f, 71f, 13.5f)
    }
    drawPath(vein, MochiPalette.leafDark.copy(alpha = 0.55f), style = Stroke(1.3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFace(mood: MochiMood, blink: Float) {
    val eyeY = 60f
    val lx = 38f
    val rx = 62f
    // Blush.
    drawOval(MochiPalette.blush.copy(alpha = 0.45f), topLeft = Offset(22f, 66f), size = Size(11f, 5.5f))
    drawOval(MochiPalette.blush.copy(alpha = 0.45f), topLeft = Offset(67f, 66f), size = Size(11f, 5.5f))

    when (mood) {
        MochiMood.SLEEPY -> {
            listOf(lx, rx).forEach { x ->
                val arc = Path().apply {
                    moveTo(x - 4.5f, eyeY)
                    quadraticTo(x, eyeY + 4.5f, x + 4.5f, eyeY)
                }
                drawPath(arc, MochiPalette.eye, style = Stroke(2.4f, cap = StrokeCap.Round))
            }
            drawOval(MochiPalette.mouth, topLeft = Offset(47.5f, 68f), size = Size(5f, 3.6f))
            // Little "z" puffs.
            val z = Path().apply {
                moveTo(76f, 34f); lineTo(82f, 34f); lineTo(76f, 40f); lineTo(82f, 40f)
            }
            drawPath(z, MochiPalette.outline.copy(alpha = 0.55f), style = Stroke(1.8f, cap = StrokeCap.Round))
            val z2 = Path().apply {
                moveTo(84f, 25f); lineTo(88f, 25f); lineTo(84f, 29f); lineTo(88f, 29f)
            }
            drawPath(z2, MochiPalette.outline.copy(alpha = 0.4f), style = Stroke(1.5f, cap = StrokeCap.Round))
        }
        else -> {
            val surprised = mood == MochiMood.SURPRISED
            val w = if (surprised) 8.4f else 7.2f
            val hFull = if (surprised) 11f else 9.4f
            val h = (hFull * (1f - blink * 0.88f)).coerceAtLeast(1.2f)
            val lookUp = if (mood == MochiMood.LOADING) -2.2f else 0f
            listOf(lx, rx).forEach { x ->
                drawOval(MochiPalette.eye, topLeft = Offset(x - w / 2, eyeY - h / 2 + lookUp), size = Size(w, h))
                if (blink < 0.5f) {
                    drawCircle(Color.White, radius = if (surprised) 2.3f else 2f, center = Offset(x + 1.3f, eyeY - h * 0.22f + lookUp))
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = 1f, center = Offset(x - 1.6f, eyeY + h * 0.24f + lookUp))
                }
            }
            if (surprised) {
                drawOval(MochiPalette.mouth, topLeft = Offset(46.5f, 67f), size = Size(7f, 7.5f))
                drawOval(MochiPalette.blush.copy(alpha = 0.8f), topLeft = Offset(48f, 70.5f), size = Size(4f, 3f))
            } else {
                // Small "w" smile.
                val smile = Path().apply {
                    moveTo(45f, 67f)
                    quadraticTo(47.5f, 70.5f, 50f, 67.5f)
                    quadraticTo(52.5f, 70.5f, 55f, 67f)
                }
                drawPath(smile, MochiPalette.mouth, style = Stroke(2.2f, cap = StrokeCap.Round))
            }
        }
    }
}
