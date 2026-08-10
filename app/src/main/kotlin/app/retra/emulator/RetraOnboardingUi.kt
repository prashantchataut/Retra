package app.retra.emulator

import android.content.Context
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.PeachGlow
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight
import app.retra.emulator.ui.theme.VoidBlack
import app.retra.emulator.ui.theme.WarmCream
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

    val ElectricLavender = Color(0xFFB998FF) // Chapter 0 & 2 Hero
    val MemoryPink = Color(0xFFFF5CA8)       // Chapter 1 Hero
    val AcidNostalgia = Color(0xFFD7FF4F)    // Playful accents
    val DreamCyan = Color(0xFF64E6D2)        // Chapter 2 & 3 complementary
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
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.0).sp,
        color = TextPrimary
    )

    val ChapterSubtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
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
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    )

    val MicroLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    )
}

// =============================================================================
// MASTER ONBOARDING SHELL
// =============================================================================

@Composable
internal fun RetraOnboarding(viewModel: RetraViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4
    val context = LocalContext.current
    val account by viewModel.account.collectAsStateWithLifecycle()
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()

    val chapterAccents = listOf(
        OnboardingTokens.ElectricLavender,
        OnboardingTokens.MemoryPink,
        OnboardingTokens.ElectricLavender,
        OnboardingTokens.DreamCyan
    )
    val activeAccent = chapterAccents[step % chapterAccents.size]

    Scaffold(
        containerColor = OnboardingTokens.MidnightBlack,
        contentColor = OnboardingTokens.TextPrimary
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingVisualSurface(step, activeAccent, viewModel)
                    }
                    Column(
                        Modifier
                            .weight(0.48f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        OnboardingHeader(step, totalSteps)
                        OnboardingCopy(step, account?.displayName)
                        OnboardingNavigation(
                            step = step,
                            totalSteps = totalSteps,
                            activeAccent = activeAccent,
                            authOperation = authOperation,
                            onBack = { if (step > 0) step-- },
                            onNext = { if (step < totalSteps - 1) step++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onSkipSignIn = { viewModel.finishOnboarding() }
                        )
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    OnboardingHeader(step, totalSteps)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingVisualSurface(step, activeAccent, viewModel)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OnboardingCopy(step, account?.displayName)
                        OnboardingNavigation(
                            step = step,
                            totalSteps = totalSteps,
                            activeAccent = activeAccent,
                            authOperation = authOperation,
                            onBack = { if (step > 0) step-- },
                            onNext = { if (step < totalSteps - 1) step++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onSkipSignIn = { viewModel.finishOnboarding() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RetraLogoTile(size = 36.dp)
            Text(
                "retra",
                style = OnboardingTokens.FloatingWord.copy(
                    fontSize = 18.sp,
                    color = OnboardingTokens.TextPrimary
                )
            )
        }
        Surface(
            shape = CircleShape,
            color = OnboardingTokens.PlumAtmosphere2.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
        ) {
            Text(
                "${step + 1} of $totalSteps",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = OnboardingTokens.MicroLabel.copy(color = OnboardingTokens.ElectricLavender)
            )
        }
    }
}

@Composable
private fun OnboardingVisualSurface(step: Int, activeAccent: Color, viewModel: RetraViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 400.dp),
        shape = RoundedCornerShape(28.dp),
        color = OnboardingTokens.GlassSurface.copy(alpha = 0.90f),
        border = BorderStroke(1.5.dp, activeAccent.copy(alpha = 0.40f)),
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(activeAccent.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                0 -> ChapterOneVisual()
                1 -> ChapterTwoVisual()
                2 -> ChapterThreeVisual()
                3 -> ChapterFourVisual()
            }
        }
    }
}

// =============================================================================
// CHAPTER ONE VISUAL: REMEMBER?
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
private fun ChapterOneVisual() {
    val scope = rememberCoroutineScope()
    val wordmarkReveal = remember { Animatable(0f) }
    val blobReveal = remember { Animatable(0f) }
    val wordsReveal = remember { Animatable(0f) }

    val parallaxX = remember { Animatable(0f) }
    val parallaxY = remember { Animatable(0f) }
    var wordDisplacement by remember { mutableFloatStateOf(0f) }

    val words = remember {
        listOf(
            FloatingMemoryWord("childhood", -0.32f, -0.36f, -6f, 20, 0.9f, OnboardingTokens.ElectricLavender),
            FloatingMemoryWord("after school", 0.36f, -0.32f, 5f, 16, 0.7f, OnboardingTokens.TextSecondary),
            FloatingMemoryWord("one more level", -0.36f, 0.12f, -4f, 18, 0.85f, OnboardingTokens.MemoryPink),
            FloatingMemoryWord("late nights", 0.36f, 0.08f, 7f, 18, 0.95f, OnboardingTokens.AcidNostalgia),
            FloatingMemoryWord("weekends", -0.26f, 0.32f, 4f, 15, 0.6f, OnboardingTokens.TextSecondary),
            FloatingMemoryWord("road trips", 0.28f, 0.28f, -5f, 16, 0.75f, OnboardingTokens.DreamCyan),
            FloatingMemoryWord("first starter", -0.05f, -0.42f, 3f, 14, 0.5f, OnboardingTokens.TextMuted)
        )
    }

    LaunchedEffect(Unit) {
        wordmarkReveal.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        blobReveal.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 320f))
        wordsReveal.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { parallaxX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        scope.launch { parallaxY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { parallaxX.snapTo((parallaxX.value + dragAmount.x * 0.3f).coerceIn(-35f, 35f)) }
                        scope.launch { parallaxY.snapTo((parallaxY.value + dragAmount.y * 0.3f).coerceIn(-35f, 35f)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val w = maxWidth.value
        val h = maxHeight.value

        // Kinetic Word Cloud
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

        // Center Wordmark & Mascot
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "retra",
                style = OnboardingTokens.WordmarkHero.copy(
                    fontSize = (w * 0.22f).coerceIn(54f, 84f).sp,
                    color = OnboardingTokens.TextPrimary.copy(alpha = wordmarkReveal.value)
                ),
                modifier = Modifier
                    .offset { IntOffset((parallaxX.value * 0.35f).roundToInt(), ((1f - wordmarkReveal.value) * 30f).roundToInt()) }
                    .scale(0.92f + 0.08f * wordmarkReveal.value)
            )

            RetraBlobMascot(
                size = ((w * 0.32f).coerceIn(100f, 130f)).dp,
                primaryAccent = OnboardingTokens.ElectricLavender,
                secondaryAccent = OnboardingTokens.MemoryPink,
                interactive = true,
                onTapReaction = { wordDisplacement = if (wordDisplacement == 0f) 10f else 0f },
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (parallaxX.value * 0.6f + w * 0.06f).roundToInt(),
                            (parallaxY.value * 0.6f + (1f - blobReveal.value) * 45f - 8f).roundToInt()
                        )
                    }
                    .scale(blobReveal.value)
            )
        }
    }
}

// =============================================================================
// CHAPTER TWO VISUAL: THE GAMES THAT MADE YOU
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
private fun ChapterTwoVisual() {
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
            MemoryTileData("Kanto Journey", "GBA", "1996 · Starter", listOf(Color(0xFF2C1038), Color(0xFFFF5CA8)), Icons.Default.Gamepad, 135, 84),
            MemoryTileData("Emerald Frontier", "GBA", "Battle Tower", listOf(Color(0xFF0F2B22), Color(0xFF5EEAD4)), Icons.Default.AutoAwesome, 150, 84),
            MemoryTileData("Golden Sun", "GBA", "Djinn alchemy", listOf(Color(0xFF33200D), Color(0xFFFFD166)), Icons.Default.Shield, 140, 84),
            MemoryTileData("Retra Drift", "DEMO", "Built-in 64 KiB", listOf(Color(0xFF1B1433), Color(0xFFB998FF)), Icons.Default.Gamepad, 140, 84)
        )
    }

    val bottomTiles = remember {
        listOf(
            MemoryTileData("Heart & Soul v1.2", "UPS", "Emerald Base · 32M", listOf(Color(0xFF3B0B1E), Color(0xFFFF5CA8), Color(0xFFFF8A65)), Icons.Default.AutoAwesome, 165, 96),
            MemoryTileData("Custom Adventure", "ROM", "Save file 184h", listOf(Color(0xFF141F36), Color(0xFF64E6D2)), Icons.Default.Favorite, 160, 96),
            MemoryTileData("Radical Red", "UPS", "Physical/Special", listOf(Color(0xFF381014), Color(0xFFFF5CA8)), Icons.Default.Gamepad, 165, 96),
            MemoryTileData("Save Timeline", "VAULT", "Snapshot rotation", listOf(Color(0xFF22113D), Color(0xFFB998FF)), Icons.Default.Shield, 155, 96)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
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
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Rail 1: Moves Right -> Left
            MemoryRailRow(topTiles, -1f, railPhase, userDragOffset * 0.75f) { selectedMicroLabel = it }

            // Rail 2: Moves Left -> Right
            MemoryRailRow(bottomTiles, 1.2f, railPhase, userDragOffset * 1.0f, heroScale = 1.05f) { selectedMicroLabel = it }

            // Inspect Indicator
            Surface(
                shape = CircleShape,
                color = OnboardingTokens.MidnightBlack.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, OnboardingTokens.MemoryPink.copy(alpha = 0.45f)),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(13.dp), tint = OnboardingTokens.MemoryPink)
                    Text(
                        selectedMicroLabel ?: "Drag to control memory flow · Hold to inspect",
                        style = OnboardingTokens.MicroLabel.copy(fontSize = 10.sp),
                        color = OnboardingTokens.TextPrimary
                    )
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
    val totalWidthPx = tiles.sumOf { it.widthDp + 14 } * 3f
    val currentX = ((phase * speedMultiplier * 360f) + userOffset) % (totalWidthPx / 3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(currentX.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            .scale(if (isPressed) 1.06f else 1.0f)
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
        shape = RoundedCornerShape(16.dp),
        color = OnboardingTokens.MidnightBlack,
        border = BorderStroke(
            width = if (isPressed) 1.5f.dp else 1.dp,
            color = if (isPressed) OnboardingTokens.MemoryPink else OnboardingTokens.PlumAtmosphere3
        ),
        shadowElevation = if (isPressed) 10.dp else 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(data.baseGradient))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                        Text(
                            text = data.system,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = OnboardingTokens.MicroLabel.copy(fontSize = 8.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Icon(data.icon, null, Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.85f))
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = data.title,
                        style = OnboardingTokens.FloatingWord.copy(fontSize = 12.sp, color = Color.White),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = data.subtitle,
                        style = OnboardingTokens.MicroLabel.copy(fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.75f)),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =============================================================================
// CHAPTER THREE VISUAL: MAKE IT YOURS
// =============================================================================

@Composable
private fun ChapterThreeVisual() {
    var colorThemeIndex by remember { mutableIntStateOf(0) }
    var layoutStyleIndex by remember { mutableIntStateOf(0) }

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Handheld Console Preview
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(175.dp),
            shape = RoundedCornerShape(26.dp),
            color = OnboardingTokens.GlassSurface.copy(alpha = 0.90f),
            border = BorderStroke(1.5f.dp, activeColor.copy(alpha = 0.55f)),
            shadowElevation = 14.dp
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
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // D-Pad
                    val dpadSize by animateDpAsState(
                        targetValue = if (layoutStyleIndex % 2 == 1) 28.dp else 36.dp,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "dpadSize"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = OnboardingTokens.MidnightBlack,
                            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(dpadSize, dpadSize * 0.7f)
                        ) { Box(contentAlignment = Alignment.Center) { Text("▲", color = activeColor, fontSize = 8.sp) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = OnboardingTokens.MidnightBlack,
                                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(dpadSize * 0.7f, dpadSize)
                            ) { Box(contentAlignment = Alignment.Center) { Text("◀", color = activeColor, fontSize = 8.sp) } }
                            Box(Modifier.size(dpadSize * 0.7f).background(OnboardingTokens.MidnightBlack, CircleShape))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = OnboardingTokens.MidnightBlack,
                                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(dpadSize * 0.7f, dpadSize)
                            ) { Box(contentAlignment = Alignment.Center) { Text("▶", color = activeColor, fontSize = 8.sp) } }
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = OnboardingTokens.MidnightBlack,
                            border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(dpadSize, dpadSize * 0.7f)
                        ) { Box(contentAlignment = Alignment.Center) { Text("▼", color = activeColor, fontSize = 8.sp) } }
                    }

                    // Center Screen
                    Surface(
                        modifier = Modifier.weight(1f).height(115.dp).padding(horizontal = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = OnboardingTokens.MidnightBlack,
                        border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(activeColor.copy(alpha = 0.22f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("60 FPS", style = OnboardingTokens.MicroLabel.copy(fontSize = 9.sp, color = OnboardingTokens.AcidNostalgia))
                                Text("RETRA DRIFT", style = OnboardingTokens.FloatingWord.copy(fontSize = 12.sp, color = Color.White), fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OnboardingTokens.MemoryPink.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, OnboardingTokens.MemoryPink),
                            modifier = Modifier.size(32.dp)
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("B", fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp) } }
                        Surface(
                            shape = CircleShape,
                            color = activeColor.copy(alpha = 0.40f),
                            border = BorderStroke(1.dp, activeColor),
                            modifier = Modifier.size(36.dp)
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("A", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp) } }
                    }
                }
            }
        }

        // Interactive Orbs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { colorThemeIndex++ },
                shape = RoundedCornerShape(14.dp),
                color = OnboardingTokens.GlassSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Palette, null, Modifier.size(16.dp), tint = activeColor)
                    Text("Theme Liquid", style = OnboardingTokens.MicroLabel)
                }
            }

            Surface(
                onClick = { layoutStyleIndex++ },
                shape = RoundedCornerShape(14.dp),
                color = OnboardingTokens.GlassSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, OnboardingTokens.ElectricLavender.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Gamepad, null, Modifier.size(16.dp), tint = OnboardingTokens.ElectricLavender)
                    Text("Morph Layout", style = OnboardingTokens.MicroLabel)
                }
            }
        }
    }
}

// =============================================================================
// CHAPTER FOUR VISUAL: KEEP YOUR ADVENTURES CLOSE
// =============================================================================

@Composable
private fun ChapterFourVisual() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RetraBlobMascot(
            size = 120.dp,
            primaryAccent = OnboardingTokens.ElectricLavender,
            secondaryAccent = OnboardingTokens.DreamCyan,
            interactive = true
        )
    }
}

// =============================================================================
// ONBOARDING COPY & NAVIGATION
// =============================================================================

@Composable
private fun OnboardingCopy(step: Int, accountName: String?) {
    val titles = listOf(
        "Some adventures never really leave.",
        "the games that made your childhood.",
        "your handheld. your rules.",
        if (accountName != null) "Welcome back, $accountName." else "Keep your adventures close."
    )
    val subtitles = listOf(
        "Bring the worlds you grew up with back into your pocket with an authentic, private experience.",
        "The worlds you remember. The hacks you discovered. The saves you refused to lose.",
        "Shape touch controls, liquid themes, frame pacing, and shaders around your play style.",
        "Create a profile to personalize Retra. Emulation and saves always stay private on this device."
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = titles[step],
            style = OnboardingTokens.ChapterTitle,
            color = OnboardingTokens.TextPrimary
        )
        Text(
            text = subtitles[step],
            style = OnboardingTokens.ChapterSubtitle,
            color = OnboardingTokens.TextSecondary
        )
    }
}

@Composable
private fun OnboardingNavigation(
    step: Int,
    totalSteps: Int,
    activeAccent: Color,
    authOperation: AuthOperation,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkipSignIn: () -> Unit
) {
    if (step == totalSteps - 1) {
        // Screen 4: Auth Landing Actions
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onGoogleSignIn,
                enabled = authOperation == AuthOperation.IDLE,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardingTokens.TextPrimary,
                    contentColor = OnboardingTokens.MidnightBlack
                )
            ) {
                if (authOperation == AuthOperation.SIGNING_IN) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnboardingTokens.MidnightBlack)
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting Google Profile...", style = OnboardingTokens.ButtonCta)
                } else {
                    GoogleIcon(Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Continue with Google", style = OnboardingTokens.ButtonCta)
                }
            }

            OutlinedButton(
                onClick = onSkipSignIn,
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
            ) {
                Text("Continue without an account", style = OnboardingTokens.ButtonCta.copy(fontSize = 14.sp, color = OnboardingTokens.TextSecondary))
            }

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, Modifier.size(13.dp), tint = OnboardingTokens.SaveMint)
                Text("100% private local storage · Zero analytics", style = OnboardingTokens.MicroLabel.copy(fontSize = 10.sp), color = OnboardingTokens.TextMuted)
            }
        }
    } else {
        // Screens 0 to 2 Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemoryTrailIndicator(
                chapterIndex = step,
                totalChapters = totalSteps,
                activeColor = activeAccent
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Back", style = OnboardingTokens.ButtonCta.copy(fontSize = 14.sp, color = OnboardingTokens.TextSecondary))
                    }
                }
                Button(
                    onClick = onNext,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeAccent,
                        contentColor = OnboardingTokens.MidnightBlack
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(if (step == 0) "keep going" else if (step == 1) "show me" else "let's play", style = OnboardingTokens.ButtonCta.copy(fontSize = 14.sp))
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(15.dp))
                }
            }
        }
    }
}

// =============================================================================
// GOOGLE ICON
// =============================================================================

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

        drawArc(red, 180f, 135f, true, Offset(0f, 0f), Size(w, h))
        drawArc(yellow, 135f, 45f, true, Offset(0f, 0f), Size(w, h))
        drawArc(green, 45f, 90f, true, Offset(0f, 0f), Size(w, h))
        drawArc(blue, 315f, 90f, true, Offset(0f, 0f), Size(w, h))

        drawCircle(OnboardingTokens.TextPrimary, radius * 0.58f, Offset(cx, cy))
        drawRect(blue, Offset(cx - radius * 0.05f, cy - radius * 0.22f), Size(radius * 1.05f, radius * 0.44f))
    }
}
