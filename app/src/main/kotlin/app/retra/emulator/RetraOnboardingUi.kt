package app.retra.emulator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.emulator.onboarding.ChapterFourKeep
import app.retra.emulator.onboarding.ChapterOneRemember
import app.retra.emulator.onboarding.ChapterThreeYours
import app.retra.emulator.onboarding.ChapterTwoGames
import app.retra.emulator.onboarding.MemoryTrailIndicator
import app.retra.emulator.onboarding.OnboardingTokens

/**
 * Retra 3.0 Four-Chapter Emotional Onboarding: "MIDNIGHT MEMORY"
 *
 * Replaces generic carousel templates with an art-directed 4-chapter narrative:
 * 1. "REMEMBER?" — Kinetic 3D word cloud, giant wordmark, interactive living blob
 * 2. "THE GAMES THAT MADE YOU" — Cinematic multi-layer parallax memory stream
 * 3. "MAKE IT YOURS" — Interactive liquid-glass handheld with real-time morphing orbs
 * 4. "KEEP YOUR ADVENTURES CLOSE" — Serene auth culmination with Google Sign-In & Offline play
 */
@Composable
internal fun RetraOnboarding(viewModel: RetraViewModel) {
    var currentChapter by rememberSaveable { mutableIntStateOf(0) }
    val totalChapters = 4
    val context = LocalContext.current
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reduceMotion = settings.reduceMotion

    val chapterAccents = listOf(
        OnboardingTokens.ElectricLavender,
        OnboardingTokens.MemoryPink,
        OnboardingTokens.ElectricLavender,
        OnboardingTokens.DreamCyan
    )
    val activeAccent = chapterAccents[currentChapter % chapterAccents.size]

    val draggableState = rememberDraggableState { delta ->
        if (delta < -30f && currentChapter < totalChapters - 1) {
            currentChapter++
        } else if (delta > 30f && currentChapter > 0) {
            currentChapter--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingTokens.MidnightBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
    ) {
        // Atmospheric Ambient Lighting Pool
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = 0.09f),
                        OnboardingTokens.PlumAtmosphere2.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.55f
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation & Skip Affordance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Mark Glyph
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetraLogoTile(size = 36.dp)
                    Text(
                        text = "retra",
                        style = OnboardingTokens.FloatingWord.copy(
                            color = OnboardingTokens.TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                }

                // Skip Action (Discreet, does not compete with main narrative)
                if (currentChapter < totalChapters - 1) {
                    TextButton(
                        onClick = { currentChapter = totalChapters - 1 },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Skip",
                            style = OnboardingTokens.MicroLabel,
                            color = OnboardingTokens.TextMuted
                        )
                    }
                }
            }

            // Chapter Content with Animated Transitions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (reduceMotion) {
                    Crossfade(
                        targetState = currentChapter,
                        animationSpec = tween(250),
                        label = "onboardingCrossfade"
                    ) { chapter ->
                        RenderChapter(
                            chapter = chapter,
                            authOperation = authOperation,
                            onNext = { if (currentChapter < totalChapters - 1) currentChapter++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onContinueOffline = { viewModel.finishOnboarding() }
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentChapter,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                    initialOffsetX = { it / 2 }
                                ) + fadeIn(tween(300))).togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                        targetOffsetX = { -it / 3 }
                                    ) + fadeOut(tween(250))
                                )
                            } else {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                    initialOffsetX = { -it / 2 }
                                ) + fadeIn(tween(300))).togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                        targetOffsetX = { it / 3 }
                                    ) + fadeOut(tween(250))
                                )
                            }
                        },
                        label = "onboardingAnimatedContent"
                    ) { chapter ->
                        RenderChapter(
                            chapter = chapter,
                            authOperation = authOperation,
                            onNext = { if (currentChapter < totalChapters - 1) currentChapter++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onContinueOffline = { viewModel.finishOnboarding() }
                        )
                    }
                }
            }

            // Bottom Navigation Footer: Memory Trail + Contextual Glass CTA
            if (currentChapter < totalChapters - 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Luminous 4-Stage Memory Trail
                    MemoryTrailIndicator(
                        chapterIndex = currentChapter,
                        totalChapters = totalChapters,
                        activeColor = activeAccent
                    )

                    // Contextual Liquid-Glass Action Button
                    Button(
                        onClick = { currentChapter++ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeAccent,
                            contentColor = OnboardingTokens.MidnightBlack
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (currentChapter) {
                                    0 -> "keep going"
                                    1 -> "show me"
                                    else -> "let's play"
                                },
                                style = OnboardingTokens.ButtonCta.copy(fontSize = 15.sp)
                            )
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderChapter(
    chapter: Int,
    authOperation: app.retra.emulator.auth.AuthOperation,
    onNext: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onContinueOffline: () -> Unit
) {
    when (chapter) {
        0 -> ChapterOneRemember(onNext = onNext)
        1 -> ChapterTwoGames(onNext = onNext)
        2 -> ChapterThreeYours(onNext = onNext)
        3 -> ChapterFourKeep(
            authOperation = authOperation,
            onGoogleSignIn = onGoogleSignIn,
            onContinueOffline = onContinueOffline
        )
    }
}
