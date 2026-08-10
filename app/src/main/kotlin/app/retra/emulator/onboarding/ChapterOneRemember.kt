package app.retra.emulator.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FloatingMemoryWord(
    val text: String,
    val initialX: Float, // percentage -1f to 1f
    val initialY: Float, // percentage -1f to 1f
    val rotation: Float,
    val sizeSp: Int,
    val depth: Float,    // 0.3f (far) to 1.0f (close)
    val color: Color
)

@Composable
fun ChapterOneRemember(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Intro Choreography Timelines
    val glintAlpha = remember { Animatable(0f) }
    val wordmarkReveal = remember { Animatable(0f) }
    val blobReveal = remember { Animatable(0f) }
    val wordsReveal = remember { Animatable(0f) }

    // Interactive Parallax
    val parallaxX = remember { Animatable(0f) }
    val parallaxY = remember { Animatable(0f) }
    var wordDisplacement by remember { mutableFloatStateOf(0f) }

    val words = remember {
        listOf(
            FloatingMemoryWord("childhood", -0.32f, -0.38f, -6f, 22, 0.9f, OnboardingTokens.ElectricLavender),
            FloatingMemoryWord("after school", 0.36f, -0.34f, 5f, 18, 0.7f, OnboardingTokens.TextSecondary),
            FloatingMemoryWord("one more level", -0.38f, 0.08f, -4f, 19, 0.85f, OnboardingTokens.MemoryPink),
            FloatingMemoryWord("late nights", 0.38f, 0.05f, 7f, 20, 0.95f, OnboardingTokens.AcidNostalgia),
            FloatingMemoryWord("weekends", -0.28f, 0.28f, 4f, 17, 0.6f, OnboardingTokens.TextSecondary),
            FloatingMemoryWord("road trips", 0.30f, 0.24f, -5f, 18, 0.75f, OnboardingTokens.DreamCyan),
            FloatingMemoryWord("first starter", -0.05f, -0.44f, 3f, 15, 0.5f, OnboardingTokens.TextMuted),
            FloatingMemoryWord("secret areas", 0.08f, 0.34f, -3f, 16, 0.65f, OnboardingTokens.CartridgeCoral)
        )
    }

    LaunchedEffect(Unit) {
        // 0–300ms: Spectral glint
        glintAlpha.animateTo(1f, tween(300, easing = LinearEasing))
        glintAlpha.animateTo(0.2f, tween(400, easing = FastOutSlowInEasing))

        // 300–850ms: Wordmark reveal
        wordmarkReveal.animateTo(1f, tween(550, easing = FastOutSlowInEasing))

        // 650–1200ms: Blob emerges with elastic bounce
        blobReveal.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 320f))

        // 900–1600ms: Words drift from Z-depth
        wordsReveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { parallaxX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        scope.launch { parallaxY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { parallaxX.snapTo((parallaxX.value + dragAmount.x * 0.3f).coerceIn(-40f, 40f)) }
                        scope.launch { parallaxY.snapTo((parallaxY.value + dragAmount.y * 0.3f).coerceIn(-40f, 40f)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val w = maxWidth.value
        val h = maxHeight.value

        // 1. Floating 3D Kinetic Memory Word Cloud
        for ((index, item) in words.withIndex()) {
            val depthMultiplier = item.depth
            val offsetX = (item.initialX * w * 0.44f) + (parallaxX.value * depthMultiplier * 0.8f)
            val offsetY = (item.initialY * h * 0.38f) + (parallaxY.value * depthMultiplier * 0.8f) + (if (index % 2 == 0) wordDisplacement else -wordDisplacement)

            Text(
                text = item.text,
                style = OnboardingTokens.FloatingWord.copy(
                    fontSize = item.sizeSp.sp,
                    color = item.color.copy(alpha = (item.depth * 0.95f * wordsReveal.value).coerceIn(0f, 1f))
                ),
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .rotate(item.rotation)
                    .scale(0.85f + 0.15f * item.depth)
            )
        }

        // 2. Center Hero Composition: Enormous Wordmark + Emerging Mascot Blob
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Enormous lowercase Wordmark
                Text(
                    text = "retra",
                    style = OnboardingTokens.WordmarkHero.copy(
                        fontSize = (w * 0.22f).coerceIn(60f, 96f).sp,
                        color = OnboardingTokens.TextPrimary.copy(alpha = wordmarkReveal.value)
                    ),
                    modifier = Modifier
                        .offset { IntOffset((parallaxX.value * 0.35f).roundToInt(), ((1f - wordmarkReveal.value) * 35f).roundToInt()) }
                        .scale(0.92f + 0.08f * wordmarkReveal.value)
                )

                // Living Retra Blob emerges through the letters
                RetraBlobMascot(
                    size = (w * 0.34f).coerceIn(110f, 150f).dp,
                    primaryAccent = OnboardingTokens.ElectricLavender,
                    secondaryAccent = OnboardingTokens.MemoryPink,
                    interactive = true,
                    onTapReaction = {
                        wordDisplacement = if (wordDisplacement == 0f) 12f else 0f
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (parallaxX.value * 0.6f + w * 0.08f).roundToInt(),
                                (parallaxY.value * 0.6f + (1f - blobReveal.value) * 50f - 10f).roundToInt()
                            )
                        }
                        .scale(blobReveal.value)
                )
            }

            Spacer(Modifier.height(36.dp))

            // Emotional Hook Typography
            Text(
                text = "Some adventures never really leave.",
                style = OnboardingTokens.ChapterTitle.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                color = OnboardingTokens.TextPrimary.copy(alpha = wordsReveal.value),
                modifier = Modifier.alpha(wordsReveal.value)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Bring the worlds you grew up with back into your pocket.",
                style = OnboardingTokens.ChapterSubtitle.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = OnboardingTokens.TextSecondary.copy(alpha = wordsReveal.value),
                modifier = Modifier.alpha(wordsReveal.value)
            )
        }
    }
}
