package app.retra.emulator.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ChapterThreeYours(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Interactive Demonstration State
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

    // Subtle Float Animation
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
        val w = maxWidth.value

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Typography
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
                // Background Mascot Peeking
                RetraBlobMascot(
                    size = 90.dp,
                    primaryAccent = activeColor,
                    secondaryAccent = OnboardingTokens.MemoryPink,
                    interactive = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-20).dp, y = (-20).dp)
                )

                // Glass Handheld Body
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(210.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = OnboardingTokens.GlassSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.55f)),
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
                            // Left Side: Interactive Morphing D-Pad
                            DPadCluster(
                                activeColor = activeColor,
                                compact = layoutStyleIndex % 2 == 1
                            )

                            // Center: Virtual Gameplay Display
                            CenterDisplayScreen(
                                filterName = filterNames[screenFilterIndex % filterNames.size],
                                activeColor = activeColor,
                                modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                            )

                            // Right Side: Interactive Action Buttons
                            ActionButtonsCluster(
                                activeColor = activeColor,
                                compact = layoutStyleIndex % 2 == 1
                            )
                        }
                    }
                }
            }

            // 3 Interactive Orbs: Touch to Transform Device
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
                    icon = Icons.Default.Tv,
                    glowColor = OnboardingTokens.DreamCyan,
                    onClick = { screenFilterIndex++ }
                )
            }

            // Floating 3D Micro-features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "save anywhere",
                    style = OnboardingTokens.MicroLabel,
                    color = OnboardingTokens.TextMuted
                )
                Text(
                    "fast-forward 8×",
                    style = OnboardingTokens.MicroLabel,
                    color = activeColor
                )
                Text(
                    "custom shaders",
                    style = OnboardingTokens.MicroLabel,
                    color = OnboardingTokens.TextMuted
                )
            }
        }
    }
}

@Composable
private fun DPadCluster(
    activeColor: Color,
    compact: Boolean
) {
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
private fun ActionButtonsCluster(
    activeColor: Color,
    compact: Boolean
) {
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
                .background(
                    Brush.radialGradient(
                        listOf(activeColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
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
