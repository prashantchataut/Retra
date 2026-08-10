package app.retra.emulator.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Retra Blob / Memory Creature:
 *
 * A living memory with liquid-glass materiality, deformable spring physics,
 * eye gaze tracking, and authored micro-reactions.
 */
@Composable
fun RetraBlobMascot(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    primaryAccent: Color = OnboardingTokens.ElectricLavender,
    secondaryAccent: Color = OnboardingTokens.MemoryPink,
    interactive: Boolean = true,
    onTapReaction: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val squashScaleX = remember { Animatable(1f) }
    val squashScaleY = remember { Animatable(1f) }

    var gazeTargetX by remember { mutableFloatStateOf(0f) }
    var gazeTargetY by remember { mutableFloatStateOf(0f) }
    var reactionCount by remember { mutableIntStateOf(0) }

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "BlobBreath")
    val breath by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val gestureModifier = if (interactive) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    reactionCount++
                    onTapReaction?.invoke()
                    scope.launch {
                        // Deterministic squash & stretch micro-reaction
                        when (reactionCount % 3) {
                            0 -> {
                                squashScaleX.animateTo(1.22f, spring(dampingRatio = 0.4f, stiffness = 600f))
                                squashScaleY.animateTo(0.82f, spring(dampingRatio = 0.4f, stiffness = 600f))
                            }
                            1 -> {
                                squashScaleX.animateTo(0.85f, spring(dampingRatio = 0.45f, stiffness = 700f))
                                squashScaleY.animateTo(1.20f, spring(dampingRatio = 0.45f, stiffness = 700f))
                            }
                            else -> {
                                squashScaleX.animateTo(1.15f, spring(dampingRatio = 0.5f, stiffness = 800f))
                                squashScaleY.animateTo(0.88f, spring(dampingRatio = 0.5f, stiffness = 800f))
                            }
                        }
                        squashScaleX.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f))
                        squashScaleY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f))
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        scope.launch {
                            squashScaleX.animateTo(1.08f, spring(dampingRatio = 0.6f))
                            squashScaleY.animateTo(1.08f, spring(dampingRatio = 0.6f))
                        }
                    },
                    onDragEnd = {
                        scope.launch { dragX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                        scope.launch { dragY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                        scope.launch {
                            squashScaleX.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
                            squashScaleY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
                        }
                        gazeTargetX = 0f
                        gazeTargetY = 0f
                    },
                    onDragCancel = {
                        scope.launch { dragX.animateTo(0f) }
                        scope.launch { dragY.animateTo(0f) }
                        scope.launch { squashScaleX.animateTo(1f) }
                        scope.launch { squashScaleY.animateTo(1f) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { dragX.snapTo((dragX.value + dragAmount.x * 0.4f).coerceIn(-90f, 90f)) }
                        scope.launch { dragY.snapTo((dragY.value + dragAmount.y * 0.4f).coerceIn(-90f, 90f)) }
                        gazeTargetX = (dragX.value / 25f).coerceIn(-6f, 6f)
                        gazeTargetY = (dragY.value / 25f).coerceIn(-5f, 5f)
                    }
                )
            }
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .offset { IntOffset(dragX.value.roundToInt(), (dragY.value + breath).roundToInt()) }
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width * squashScaleX.value
            val h = this.size.height * squashScaleY.value
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // 1. Asymmetric Organic Blob Silhouette Path
            val blobPath = Path().apply {
                moveTo(cx, cy - h * 0.38f)
                // Top-Right Signature Crown Lobe
                cubicTo(cx + w * 0.22f, cy - h * 0.44f, cx + w * 0.44f, cy - h * 0.24f, cx + w * 0.40f, cy - h * 0.05f)
                // Right Flank
                cubicTo(cx + w * 0.46f, cy + h * 0.16f, cx + w * 0.32f, cy + h * 0.42f, cx + w * 0.05f, cy + h * 0.42f)
                // Bottom Base
                cubicTo(cx - w * 0.25f, cy + h * 0.44f, cx - w * 0.42f, cy + h * 0.22f, cx - w * 0.38f, cy - h * 0.02f)
                // Left Curve to Crest
                cubicTo(cx - w * 0.40f, cy - h * 0.24f, cx - w * 0.20f, cy - h * 0.36f, cx, cy - h * 0.38f)
                close()
            }

            // 2. Liquid Glass Gradient Shading
            val liquidBrush = Brush.linearGradient(
                colors = listOf(
                    primaryAccent.copy(alpha = 0.92f),
                    primaryAccent.copy(alpha = 0.70f),
                    secondaryAccent.copy(alpha = 0.85f),
                    OnboardingTokens.CartridgeCoral.copy(alpha = 0.60f)
                ),
                start = Offset(cx - w * 0.35f, cy - h * 0.35f),
                end = Offset(cx + w * 0.40f, cy + h * 0.40f)
            )

            drawPath(blobPath, liquidBrush)

            // 3. Inner Spectral Core Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        OnboardingTokens.DreamCyan.copy(alpha = 0.45f),
                        primaryAccent.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(cx - w * 0.05f, cy + h * 0.08f),
                    radius = w * 0.32f
                ),
                radius = w * 0.32f,
                center = Offset(cx - w * 0.05f, cy + h * 0.08f)
            )

            // 4. Refractive Specular Highlight Arc
            val highlightPath = Path().apply {
                moveTo(cx - w * 0.25f, cy - h * 0.22f)
                cubicTo(cx - w * 0.08f, cy - h * 0.34f, cx + w * 0.22f, cy - h * 0.32f, cx + w * 0.30f, cy - h * 0.16f)
                cubicTo(cx + w * 0.20f, cy - h * 0.24f, cx - w * 0.05f, cy - h * 0.26f, cx - w * 0.25f, cy - h * 0.22f)
                close()
            }
            drawPath(highlightPath, Color.White.copy(alpha = 0.65f))

            // 5. Soft Rim Light Border
            drawPath(
                blobPath,
                brush = Brush.sweepGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.Transparent,
                        primaryAccent.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.45f)
                    ),
                    center = Offset(cx, cy)
                ),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 6. Expressive Gaze Tracking Eyes
            val eyeSpacing = w * 0.14f
            val eyeY = cy - h * 0.02f + gazeTargetY
            val eyeW = w * 0.085f
            val eyeH = h * 0.12f

            // Left Eye (Black with glossy glints)
            val leftEyeCenter = Offset(cx - eyeSpacing + gazeTargetX, eyeY)
            drawOval(
                color = OnboardingTokens.MidnightBlack,
                topLeft = Offset(leftEyeCenter.x - eyeW / 2f, leftEyeCenter.y - eyeH / 2f),
                size = Size(eyeW, eyeH)
            )
            // Primary Glint
            drawOval(
                color = Color.White,
                topLeft = Offset(leftEyeCenter.x - eyeW * 0.28f, leftEyeCenter.y - eyeH * 0.36f),
                size = Size(eyeW * 0.50f, eyeH * 0.45f)
            )
            // Secondary Spark
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eyeW * 0.18f,
                center = Offset(leftEyeCenter.x + eyeW * 0.16f, leftEyeCenter.y + eyeH * 0.22f)
            )

            // Right Eye
            val rightEyeCenter = Offset(cx + eyeSpacing + gazeTargetX, eyeY)
            drawOval(
                color = OnboardingTokens.MidnightBlack,
                topLeft = Offset(rightEyeCenter.x - eyeW / 2f, rightEyeCenter.y - eyeH / 2f),
                size = Size(eyeW, eyeH)
            )
            // Primary Glint
            drawOval(
                color = Color.White,
                topLeft = Offset(rightEyeCenter.x - eyeW * 0.28f, rightEyeCenter.y - eyeH * 0.36f),
                size = Size(eyeW * 0.50f, eyeH * 0.45f)
            )
            // Secondary Spark
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eyeW * 0.18f,
                center = Offset(rightEyeCenter.x + eyeW * 0.16f, rightEyeCenter.y + eyeH * 0.22f)
            )

            // 7. Charming Minimal Smile
            val smilePath = Path().apply {
                moveTo(cx - w * 0.04f + gazeTargetX * 0.5f, cy + h * 0.13f + gazeTargetY * 0.5f)
                quadraticBezierTo(
                    cx + gazeTargetX * 0.5f,
                    cy + h * 0.17f + gazeTargetY * 0.5f,
                    cx + w * 0.04f + gazeTargetX * 0.5f,
                    cy + h * 0.13f + gazeTargetY * 0.5f
                )
            }
            drawPath(
                smilePath,
                color = OnboardingTokens.MidnightBlack,
                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // 8. Cheerful Cheek Blush
            drawCircle(
                color = OnboardingTokens.MemoryPink.copy(alpha = 0.35f),
                radius = w * 0.055f,
                center = Offset(cx - w * 0.24f, cy + h * 0.09f)
            )
            drawCircle(
                color = OnboardingTokens.MemoryPink.copy(alpha = 0.35f),
                radius = w * 0.055f,
                center = Offset(cx + w * 0.24f, cy + h * 0.09f)
            )
        }
    }
}
