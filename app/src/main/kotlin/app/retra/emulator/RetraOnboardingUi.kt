package app.retra.emulator

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.emulator.auth.AuthOperation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// =============================================================================
// MIDNIGHT MEMORY ONBOARDING TOKENS
// =============================================================================

private object OnboardingTokens {
    val MidnightBlack = Color(0xFF0A0810)
    val PlumAtmosphere1 = Color(0xFF130D1C)
    val PlumAtmosphere2 = Color(0xFF1B1026)
    val PlumAtmosphere3 = Color(0xFF21132D)
    val GlassSurface = Color(0xFF161024)

    val TextPrimary = Color(0xFFF7F4FF)     // 18.3:1 contrast against MidnightBlack
    val TextSecondary = Color(0xFFC8C2D8)   // 11.5:1 contrast against MidnightBlack
    val TextMuted = Color(0xFF7E7692)

    val ElectricLavender = Color(0xFFB998FF) // Chapter 1 & 3 Hero
    val MemoryPink = Color(0xFFFF5CA8)       // Chapter 2 Hero
    val AcidNostalgia = Color(0xFFD7FF4F)    // Playful accents
    val DreamCyan = Color(0xFF64E6D2)        // Chapter 3 complementary
    val CartridgeCoral = Color(0xFFFF8A65)   // Warm cartridge accent

    val WordmarkHero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 68.sp,
        lineHeight = 68.sp,
        letterSpacing = (-2.5).sp,
        color = TextPrimary
    )

    val ChapterTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
        color = TextPrimary
    )

    val ChapterSubtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        color = TextSecondary
    )

    val FloatingWord = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    val ButtonCta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    )

    val MicroLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}

// =============================================================================
// MASTER ONBOARDING CONTAINER
// =============================================================================

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

                // Skip Action
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
    authOperation: AuthOperation,
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

// =============================================================================
// MEMORY TRAIL INDICATOR
// =============================================================================

@Composable
private fun MemoryTrailIndicator(
    chapterIndex: Int,
    totalChapters: Int = 4,
    activeColor: Color = OnboardingTokens.ElectricLavender,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalChapters) {
            val isActive = i == chapterIndex
            val animatedWidth by animateDpAsState(
                targetValue = if (isActive) 32.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "trailWidth_$i"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isActive) activeColor else OnboardingTokens.TextMuted.copy(alpha = 0.35f),
                label = "trailColor_$i"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}

// =============================================================================
// RETRA BLOB MASCOT COMPONENT
// =============================================================================

@Composable
private fun RetraBlobMascot(
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

    val gestureModifier = if (interactive) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    reactionCount++
                    onTapReaction?.invoke()
                    scope.launch {
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

            // Asymmetric Organic Blob Path
            val blobPath = Path().apply {
                moveTo(cx, cy - h * 0.38f)
                cubicTo(cx + w * 0.22f, cy - h * 0.44f, cx + w * 0.44f, cy - h * 0.24f, cx + w * 0.40f, cy - h * 0.05f)
                cubicTo(cx + w * 0.46f, cy + h * 0.16f, cx + w * 0.32f, cy + h * 0.42f, cx + w * 0.05f, cy + h * 0.42f)
                cubicTo(cx - w * 0.25f, cy + h * 0.44f, cx - w * 0.42f, cy + h * 0.22f, cx - w * 0.38f, cy - h * 0.02f)
                cubicTo(cx - w * 0.40f, cy - h * 0.24f, cx - w * 0.20f, cy - h * 0.36f, cx, cy - h * 0.38f)
                close()
            }

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

            // Inner Spectral Core Glow
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

            // Refractive Specular Highlight
            val highlightPath = Path().apply {
                moveTo(cx - w * 0.25f, cy - h * 0.22f)
                cubicTo(cx - w * 0.08f, cy - h * 0.34f, cx + w * 0.22f, cy - h * 0.32f, cx + w * 0.30f, cy - h * 0.16f)
                cubicTo(cx + w * 0.20f, cy - h * 0.24f, cx - w * 0.05f, cy - h * 0.26f, cx - w * 0.25f, cy - h * 0.22f)
                close()
            }
            drawPath(highlightPath, Color.White.copy(alpha = 0.65f))

            // Soft Rim Light Border
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
                style = Stroke(width = 1.5f.dp.toPx())
            )

            // Expressive Gaze Tracking Eyes
            val eyeSpacing = w * 0.14f
            val eyeY = cy - h * 0.02f + gazeTargetY
            val eyeW = w * 0.085f
            val eyeH = h * 0.12f

            val leftEyeCenter = Offset(cx - eyeSpacing + gazeTargetX, eyeY)
            drawOval(
                color = OnboardingTokens.MidnightBlack,
                topLeft = Offset(leftEyeCenter.x - eyeW / 2f, leftEyeCenter.y - eyeH / 2f),
                size = Size(eyeW, eyeH)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(leftEyeCenter.x - eyeW * 0.28f, leftEyeCenter.y - eyeH * 0.36f),
                size = Size(eyeW * 0.50f, eyeH * 0.45f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eyeW * 0.18f,
                center = Offset(leftEyeCenter.x + eyeW * 0.16f, leftEyeCenter.y + eyeH * 0.22f)
            )

            val rightEyeCenter = Offset(cx + eyeSpacing + gazeTargetX, eyeY)
            drawOval(
                color = OnboardingTokens.MidnightBlack,
                topLeft = Offset(rightEyeCenter.x - eyeW / 2f, rightEyeCenter.y - eyeH / 2f),
                size = Size(eyeW, eyeH)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(rightEyeCenter.x - eyeW * 0.28f, rightEyeCenter.y - eyeH * 0.36f),
                size = Size(eyeW * 0.50f, eyeH * 0.45f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eyeW * 0.18f,
                center = Offset(rightEyeCenter.x + eyeW * 0.16f, rightEyeCenter.y + eyeH * 0.22f)
            )

            // Smile
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
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Cheek Blush
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

// =============================================================================
// CHAPTER ONE: REMEMBER?
// =============================================================================

private data class FloatingMemoryWord(
    val text: String,
    val initialX: Float,
    val initialY: Float,
    val rotation: Float,
    val sizeSp: Int,
    val depth: Float,
    val color: Color
)

@Composable
private fun ChapterOneRemember(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val wordmarkReveal = remember { Animatable(0f) }
    val blobReveal = remember { Animatable(0f) }
    val wordsReveal = remember { Animatable(0f) }

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
        wordmarkReveal.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        blobReveal.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 320f))
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

        // Kinetic Memory Words
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

        // Center Hero
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
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

                RetraBlobMascot(
                    size = ((w * 0.34f).coerceIn(110f, 150f)).dp,
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

// =============================================================================
// CHAPTER TWO: THE GAMES THAT MADE YOU
// =============================================================================

private data class MemoryTileData(
    val title: String,
    val system: String,
    val subtitle: String,
    val baseGradient: List<Color>,
    val icon: ImageVector,
    val widthDp: Int,
    val heightDp: Int
)

@Composable
private fun ChapterTwoGames(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userDragOffset by remember { mutableFloatStateOf(0f) }
    var selectedMicroLabel by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "RailMarquee")
    val railPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "railPhase"
    )

    val topTiles = remember {
        listOf(
            MemoryTileData("Kanto Journey", "GBA", "1996 · First starter", listOf(Color(0xFF2C1038), Color(0xFFFF5CA8)), Icons.Default.Gamepad, 140, 96),
            MemoryTileData("Emerald Frontier", "GBA", "Battle Tower 100 streak", listOf(Color(0xFF0F2B22), Color(0xFF5EEAD4)), Icons.Default.AutoAwesome, 160, 96),
            MemoryTileData("Golden Sun World", "GBA", "Djinn alchemy", listOf(Color(0xFF33200D), Color(0xFFFFD166)), Icons.Default.Shield, 150, 96),
            MemoryTileData("Retra Drift", "HOMEBREW", "Built-in 64 KiB", listOf(Color(0xFF1B1433), Color(0xFFB998FF)), Icons.Default.Gamepad, 145, 96)
        )
    }

    val middleTiles = remember {
        listOf(
            MemoryTileData("Heart & Soul v1.2", "UPS PATCH", "Emerald Base · 32 MiB", listOf(Color(0xFF3B0B1E), Color(0xFFFF5CA8), Color(0xFFFF8A65)), Icons.Default.AutoAwesome, 185, 120),
            MemoryTileData("Custom Adventure", "LOCAL ROM", "Save file 184h", listOf(Color(0xFF141F36), Color(0xFF64E6D2)), Icons.Default.Favorite, 175, 120),
            MemoryTileData("Radical Red Hack", "UPS", "Physical/Special split", listOf(Color(0xFF381014), Color(0xFFFF5CA8)), Icons.Default.Gamepad, 190, 120),
            MemoryTileData("Unbound Kingdom", "GBA", "Boras Region", listOf(Color(0xFF22113D), Color(0xFFB998FF)), Icons.Default.Shield, 180, 120)
        )
    }

    val bottomTiles = remember {
        listOf(
            MemoryTileData("RetroArch Pack", "CHEATS", "Verified CRC32", listOf(Color(0xFF15222E), Color(0xFF64E6D2)), Icons.Default.AutoAwesome, 150, 96),
            MemoryTileData("Save Timeline", "VAULT", "Snapshot rotation", listOf(Color(0xFF2B1238), Color(0xFFFF8A65)), Icons.Default.Shield, 160, 96),
            MemoryTileData("Link Cable Net", "LOCAL LAN", "2-Player verified", listOf(Color(0xFF0E2822), Color(0xFF5EEAD4)), Icons.Default.Gamepad, 145, 96)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        userDragOffset += dragAmount.x * 0.6f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                Text(
                    text = "the games that made your childhood.",
                    style = OnboardingTokens.ChapterTitle.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        color = OnboardingTokens.TextPrimary
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "The worlds you remember. The hacks you discovered. The saves you refused to lose.",
                    style = OnboardingTokens.ChapterSubtitle.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = OnboardingTokens.TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MemoryRailRow(topTiles, -1f, railPhase, userDragOffset * 0.7f) { selectedMicroLabel = it }
                MemoryRailRow(middleTiles, 1.25f, railPhase, userDragOffset * 1.0f, 1.05f) { selectedMicroLabel = it }
                MemoryRailRow(bottomTiles, -1.15f, railPhase, userDragOffset * 0.85f) { selectedMicroLabel = it }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = CircleShape,
                    color = OnboardingTokens.GlassSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, OnboardingTokens.MemoryPink.copy(alpha = 0.40f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp), tint = OnboardingTokens.MemoryPink)
                        Text(
                            text = selectedMicroLabel ?: "Drag to control memory flow · Hold to inspect",
                            style = OnboardingTokens.MicroLabel,
                            color = OnboardingTokens.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryRailRow(
    tiles: List<MemoryTileData>,
    speedMultiplier: Float,
    phase: Float,
    userOffset: Float,
    heroScale: Float = 1.0f,
    onInspect: (String) -> Unit
) {
    val totalWidthPx = tiles.sumOf { it.widthDp + 16 } * 3f
    val currentX = ((phase * speedMultiplier * 400f) + userOffset) % (totalWidthPx / 3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(currentX.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (tile in tiles + tiles) {
            MemoryTileCard(data = tile, scale = heroScale, onInspect = onInspect)
        }
    }
}

@Composable
private fun MemoryTileCard(
    data: MemoryTileData,
    scale: Float,
    onInspect: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .size(width = (data.widthDp * scale).dp, height = (data.heightDp * scale).dp)
            .scale(if (isPressed) 1.08f else 1.0f)
            .pointerInput(data.title) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onInspect("${data.title} · ${data.subtitle}")
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        color = OnboardingTokens.MidnightBlack,
        border = BorderStroke(
            width = if (isPressed) 1.5f.dp else 1.dp,
            color = if (isPressed) OnboardingTokens.MemoryPink else OnboardingTokens.PlumAtmosphere3
        ),
        shadowElevation = if (isPressed) 12.dp else 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(data.baseGradient))
                .padding(12.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 14.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y)
                    )
                    y += step
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = data.system,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = OnboardingTokens.MicroLabel.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Icon(
                        data.icon,
                        null,
                        Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = data.title,
                        style = OnboardingTokens.FloatingWord.copy(
                            fontSize = 13.sp,
                            color = Color.White
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = data.subtitle,
                        style = OnboardingTokens.MicroLabel.copy(
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =============================================================================
// CHAPTER THREE: MAKE IT YOURS
// =============================================================================

@Composable
private fun ChapterThreeYours(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var colorThemeIndex by remember { mutableIntStateOf(0) }
    var layoutStyleIndex by remember { mutableIntStateOf(0) }
    var screenFilterIndex by remember { mutableIntStateOf(0) }

    val themeColors = listOf(
        OnboardingTokens.ElectricLavender,
        OnboardingTokens.MemoryPink,
        OnboardingTokens.AcidNostalgia,
        OnboardingTokens.DreamCyan
    )
    val activeColor by animateColorAsState(
        targetValue = themeColors[colorThemeIndex % themeColors.size],
        animationSpec = spring(dampingRatio = 0.65f),
        label = "themeMorphColor"
    )

    val filterNames = listOf("Clean Modern", "Crisp Integer Pixel", "Nostalgic Color Curve")

    val infiniteTransition = rememberInfiniteTransition(label = "HandheldFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "your handheld. your rules.",
                    style = OnboardingTokens.ChapterTitle.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Shape the controls, feel, colors, and play style around you.",
                    style = OnboardingTokens.ChapterSubtitle.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                )
            }

            // Central Interactive Liquid-Glass Handheld Device
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .offset { IntOffset(0, floatOffset.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                RetraBlobMascot(
                    size = 90.dp,
                    primaryAccent = activeColor,
                    secondaryAccent = OnboardingTokens.MemoryPink,
                    interactive = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-20).dp, y = (-20).dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(210.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = OnboardingTokens.GlassSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.5f.dp, activeColor.copy(alpha = 0.55f)),
                    shadowElevation = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        activeColor.copy(alpha = 0.18f),
                                        Color.Transparent,
                                        activeColor.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DPadCluster(activeColor = activeColor, compact = layoutStyleIndex % 2 == 1)
                            CenterDisplayScreen(
                                filterName = filterNames[screenFilterIndex % filterNames.size],
                                activeColor = activeColor,
                                modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                            )
                            ActionButtonsCluster(activeColor = activeColor, compact = layoutStyleIndex % 2 == 1)
                        }
                    }
                }
            }

            // 3 Interactive Orbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InteractiveOrbButton(
                    label = "Color Liquid",
                    icon = Icons.Default.Palette,
                    glowColor = activeColor,
                    onClick = { colorThemeIndex++ }
                )

                InteractiveOrbButton(
                    label = "Controls Morph",
                    icon = Icons.Default.Gamepad,
                    glowColor = OnboardingTokens.ElectricLavender,
                    onClick = { layoutStyleIndex++ }
                )

                InteractiveOrbButton(
                    label = "Screen Shader",
                    icon = Icons.Default.Tune,
                    glowColor = OnboardingTokens.DreamCyan,
                    onClick = { screenFilterIndex++ }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("save anywhere", style = OnboardingTokens.MicroLabel, color = OnboardingTokens.TextMuted)
                Text("fast-forward 8×", style = OnboardingTokens.MicroLabel, color = activeColor)
                Text("custom shaders", style = OnboardingTokens.MicroLabel, color = OnboardingTokens.TextMuted)
            }
        }
    }
}

@Composable
private fun DPadCluster(activeColor: Color, compact: Boolean) {
    val dpadSize by animateDpAsState(
        targetValue = if (compact) 32.dp else 42.dp,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "dpadSize"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = OnboardingTokens.MidnightBlack,
            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
            modifier = Modifier.size(dpadSize, dpadSize * 0.7f)
        ) { Box(contentAlignment = Alignment.Center) { Text("▲", color = activeColor, fontSize = 9.sp) } }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = OnboardingTokens.MidnightBlack,
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                modifier = Modifier.size(dpadSize * 0.7f, dpadSize)
            ) { Box(contentAlignment = Alignment.Center) { Text("◀", color = activeColor, fontSize = 9.sp) } }

            Box(Modifier.size(dpadSize * 0.7f).background(OnboardingTokens.MidnightBlack, CircleShape))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = OnboardingTokens.MidnightBlack,
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                modifier = Modifier.size(dpadSize * 0.7f, dpadSize)
            ) { Box(contentAlignment = Alignment.Center) { Text("▶", color = activeColor, fontSize = 9.sp) } }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = OnboardingTokens.MidnightBlack,
            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
            modifier = Modifier.size(dpadSize, dpadSize * 0.7f)
        ) { Box(contentAlignment = Alignment.Center) { Text("▼", color = activeColor, fontSize = 9.sp) } }
    }
}

@Composable
private fun ActionButtonsCluster(activeColor: Color, compact: Boolean) {
    val btnSize by animateDpAsState(
        targetValue = if (compact) 32.dp else 40.dp,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "btnSize"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = OnboardingTokens.MemoryPink.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, OnboardingTokens.MemoryPink),
            modifier = Modifier.size(btnSize)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("B", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
            }
        }

        Surface(
            shape = CircleShape,
            color = activeColor.copy(alpha = 0.40f),
            border = BorderStroke(1.dp, activeColor),
            modifier = Modifier.size(btnSize + 4.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("A", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CenterDisplayScreen(
    filterName: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        color = OnboardingTokens.MidnightBlack,
        border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(activeColor.copy(alpha = 0.25f), Color.Transparent)))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "60.0 FPS",
                    style = OnboardingTokens.MicroLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnboardingTokens.AcidNostalgia
                    )
                )

                Text(
                    text = "RETRA DRIFT",
                    style = OnboardingTokens.FloatingWord.copy(
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    fontWeight = FontWeight.Black
                )

                Surface(
                    shape = CircleShape,
                    color = activeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, activeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = filterName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = OnboardingTokens.MicroLabel.copy(fontSize = 9.sp),
                        color = activeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveOrbButton(
    label: String,
    icon: ImageVector,
    glowColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = OnboardingTokens.GlassSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, glowColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = glowColor)
            Text(label, style = OnboardingTokens.MicroLabel, color = OnboardingTokens.TextPrimary)
        }
    }
}

// =============================================================================
// CHAPTER FOUR: KEEP YOUR ADVENTURES CLOSE
// =============================================================================

@Composable
private fun ChapterFourKeep(
    authOperation: AuthOperation,
    onGoogleSignIn: () -> Unit,
    onContinueOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "after school",
                    style = OnboardingTokens.FloatingWord.copy(
                        fontSize = 15.sp,
                        color = OnboardingTokens.TextMuted.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 32.dp, top = 24.dp)
                        .rotate(-8f)
                )

                Text(
                    text = "save state",
                    style = OnboardingTokens.FloatingWord.copy(
                        fontSize = 14.sp,
                        color = OnboardingTokens.ElectricLavender.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 20.dp)
                        .rotate(6f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "retra",
                        style = OnboardingTokens.WordmarkHero.copy(
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                            color = OnboardingTokens.TextPrimary
                        )
                    )

                    RetraBlobMascot(
                        size = 130.dp,
                        primaryAccent = OnboardingTokens.ElectricLavender,
                        secondaryAccent = OnboardingTokens.DreamCyan,
                        interactive = true
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color = OnboardingTokens.GlassSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    OnboardingTokens.ElectricLavender.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Keep your adventures close.",
                                style = OnboardingTokens.ChapterTitle.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Text(
                                text = "Create a profile to personalize Retra. Games and saves always stay private on this device.",
                                style = OnboardingTokens.ChapterSubtitle.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onGoogleSignIn,
                                enabled = authOperation == AuthOperation.IDLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OnboardingTokens.TextPrimary,
                                    contentColor = OnboardingTokens.MidnightBlack
                                )
                            ) {
                                if (authOperation == AuthOperation.SIGNING_IN) {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = OnboardingTokens.MidnightBlack
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Connecting Google Profile...", style = OnboardingTokens.ButtonCta)
                                } else {
                                    GoogleIcon(Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Continue with Google", style = OnboardingTokens.ButtonCta)
                                }
                            }

                            OutlinedButton(
                                onClick = onContinueOffline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
                            ) {
                                Text(
                                    "Continue without an account",
                                    style = OnboardingTokens.ButtonCta.copy(
                                        fontSize = 15.sp,
                                        color = OnboardingTokens.TextSecondary
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, null, Modifier.size(14.dp), tint = OnboardingTokens.SaveMint)
                            Text(
                                text = "100% private local storage · Zero analytics",
                                style = OnboardingTokens.MicroLabel.copy(fontSize = 11.sp),
                                color = OnboardingTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w / 2f

        val red = Color(0xFFEA4335)
        val blue = Color(0xFF4285F4)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 135f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 45f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = blue,
            startAngle = 315f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )

        drawCircle(
            color = OnboardingTokens.TextPrimary,
            radius = radius * 0.58f,
            center = Offset(cx, cy)
        )

        drawRect(
            color = blue,
            topLeft = Offset(cx - radius * 0.05f, cy - radius * 0.22f),
            size = Size(radius * 1.05f, radius * 0.44f)
        )
    }
}
