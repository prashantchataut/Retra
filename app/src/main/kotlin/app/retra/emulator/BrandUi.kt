package app.retra.emulator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.VoidBlack
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class MascotState {
    IDLE,
    IMPORTING,
    LOADING,
    EMPTY,
    SUCCESS,
    DUPLICATE,
    UNSUPPORTED,
    PATCH_READY,
    ERROR
}

/**
 * Retra Sprite: The living memory-creature made from liquid glass.
 *
 * Provides spring physics reaction on touch/drag, state-dependent expressions,
 * internal refractive lighting, specular highlights, and charming eye interactions.
 */
@Composable
fun RetraMascot(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    state: MascotState = MascotState.IDLE,
    interactive: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "RetraMascotBreathing")
    val breathOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathOffset"
    )
    val eyeScan by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyeScan"
    )

    val dragModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    coroutineScope.launch { scale.animateTo(1.08f, spring(dampingRatio = 0.6f)) }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 400f))
                    }
                    coroutineScope.launch {
                        offsetY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 400f))
                    }
                    coroutineScope.launch {
                        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f))
                    }
                },
                onDragCancel = {
                    coroutineScope.launch { offsetX.animateTo(0f) }
                    coroutineScope.launch { offsetY.animateTo(0f) }
                    coroutineScope.launch { scale.animateTo(1f) }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount.x * 0.45f) }
                    coroutineScope.launch { offsetY.snapTo(offsetY.value + dragAmount.y * 0.45f) }
                }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .offset { IntOffset(offsetX.value.roundToInt(), (offsetY.value + breathOffset).roundToInt()) }
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Organic Asymmetric Liquid Glass Body
            val bodyPath = Path().apply {
                moveTo(w * 0.50f, h * 0.20f)
                cubicTo(w * 0.68f, h * 0.18f, w * 0.82f, h * 0.32f, w * 0.82f, h * 0.50f)
                cubicTo(w * 0.82f, h * 0.68f, w * 0.70f, h * 0.82f, w * 0.50f, h * 0.82f)
                cubicTo(w * 0.30f, h * 0.82f, w * 0.18f, h * 0.68f, w * 0.18f, h * 0.50f)
                cubicTo(w * 0.18f, h * 0.32f, w * 0.32f, h * 0.22f, w * 0.50f, h * 0.20f)
                close()
            }
            // Signature Lobe on Top-Right
            val lobePath = Path().apply {
                moveTo(w * 0.62f, h * 0.23f)
                cubicTo(w * 0.75f, h * 0.17f, w * 0.84f, h * 0.25f, w * 0.78f, h * 0.38f)
                cubicTo(w * 0.72f, h * 0.42f, w * 0.66f, h * 0.35f, w * 0.62f, h * 0.23f)
                close()
            }

            val bodyBrush = Brush.linearGradient(
                colors = when (state) {
                    MascotState.SUCCESS -> listOf(SaveMint, ElectricLilac, RaspberryPink)
                    MascotState.ERROR -> listOf(MemoryCoral, RaspberryPink, NightPlum)
                    MascotState.PATCH_READY -> listOf(ElectricLilac, SaveMint, MemoryCoral)
                    else -> listOf(ElectricLilac, Color(0xFF9366E8), MemoryCoral)
                },
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.8f, h * 0.8f)
            )

            // Draw body with shadow
            drawPath(lobePath, bodyBrush)
            drawPath(bodyPath, bodyBrush)

            // 2. Refractive internal core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SaveMint.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.56f),
                    radius = w * 0.28f
                ),
                radius = w * 0.28f,
                center = Offset(w * 0.50f, h * 0.56f)
            )

            // 3. Specular upper highlight
            val highlightPath = Path().apply {
                moveTo(w * 0.30f, h * 0.32f)
                cubicTo(w * 0.42f, h * 0.22f, w * 0.64f, h * 0.22f, w * 0.72f, h * 0.30f)
                cubicTo(w * 0.64f, h * 0.25f, w * 0.44f, h * 0.25f, w * 0.30f, h * 0.32f)
                close()
            }
            drawPath(highlightPath, Color.White.copy(alpha = 0.55f))

            // 4. Cheerful Cheek Blush
            drawCircle(
                color = MemoryCoral.copy(alpha = 0.40f),
                radius = w * 0.055f,
                center = Offset(w * 0.33f, h * 0.54f)
            )
            drawCircle(
                color = MemoryCoral.copy(alpha = 0.40f),
                radius = w * 0.055f,
                center = Offset(w * 0.67f, h * 0.54f)
            )

            // 5. Expressive Eyes based on MascotState
            val eyeShift = if (state == MascotState.IMPORTING || state == MascotState.LOADING) eyeScan else 0f
            when (state) {
                MascotState.SUCCESS -> {
                    // Joyful Squint Arcs
                    drawArc(
                        color = NightPlum,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(w * 0.36f + eyeShift, h * 0.46f),
                        size = Size(w * 0.09f, h * 0.06f),
                        style = Stroke(width = w * 0.024f)
                    )
                    drawArc(
                        color = NightPlum,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(w * 0.55f + eyeShift, h * 0.46f),
                        size = Size(w * 0.09f, h * 0.06f),
                        style = Stroke(width = w * 0.024f)
                    )
                }
                MascotState.ERROR -> {
                    // Worried soft angled eyes
                    drawOval(
                        color = NightPlum,
                        topLeft = Offset(w * 0.38f, h * 0.48f),
                        size = Size(w * 0.07f, h * 0.09f)
                    )
                    drawOval(
                        color = NightPlum,
                        topLeft = Offset(w * 0.55f, h * 0.48f),
                        size = Size(w * 0.07f, h * 0.09f)
                    )
                }
                else -> {
                    // Standard Oversized Expressive Glossy Eyes
                    val eyeW = w * 0.075f
                    val eyeH = h * 0.105f
                    // Left Eye
                    drawOval(
                        color = NightPlum,
                        topLeft = Offset(w * 0.38f + eyeShift, h * 0.46f),
                        size = Size(eyeW, eyeH)
                    )
                    // Left Eye Glint
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.39f + eyeShift, h * 0.47f),
                        size = Size(eyeW * 0.45f, eyeH * 0.45f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = eyeW * 0.15f,
                        center = Offset(w * 0.43f + eyeShift, h * 0.53f)
                    )

                    // Right Eye
                    drawOval(
                        color = NightPlum,
                        topLeft = Offset(w * 0.55f + eyeShift, h * 0.46f),
                        size = Size(eyeW, eyeH)
                    )
                    // Right Eye Glint
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.56f + eyeShift, h * 0.47f),
                        size = Size(eyeW * 0.45f, eyeH * 0.45f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = eyeW * 0.15f,
                        center = Offset(w * 0.60f + eyeShift, h * 0.53f)
                    )
                }
            }

            // 6. Charming Mouth
            val smilePath = Path().apply {
                moveTo(w * 0.47f, h * 0.58f)
                quadraticBezierTo(w * 0.50f, h * 0.61f, w * 0.53f, h * 0.58f)
            }
            drawPath(
                smilePath,
                color = NightPlum,
                style = Stroke(width = w * 0.022f)
            )
        }
    }
}

/**
 * Retra Logo Mark Vector.
 */
@Composable
fun RetraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = null,
    markColor: Color = MaterialTheme.colorScheme.onSurface,
    cutoutColor: Color = MaterialTheme.colorScheme.surface,
    sparkColor: Color = MemoryCoral
) {
    val semanticsModifier = if (contentDescription == null) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Box(
        modifier = semanticsModifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        RetraMascot(
            size = size,
            state = MascotState.IDLE,
            interactive = false
        )
    }
}

@Composable
fun RetraLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val radius = size * 0.28f
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(radius),
        color = NightPlum,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            RetraMascot(
                modifier = Modifier.padding(size * 0.08f),
                size = size * 0.84f,
                state = MascotState.IDLE,
                interactive = false
            )
        }
    }
}

@Composable
fun RetraBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    RetraLogo(modifier = modifier, size = size, contentDescription = "Retra")
}
