package app.retra.emulator.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
fun ChapterTwoGames(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
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
        val w = maxWidth.value

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Typography: Bold Emotional Statement
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

            // Cinematic 3-Layer Moving Marquee Rails
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Rail: Moves Right -> Left
                MemoryRailRow(
                    tiles = topTiles,
                    speedMultiplier = -1f,
                    phase = railPhase,
                    userOffset = userDragOffset * 0.7f,
                    onInspect = { selectedMicroLabel = it }
                )

                // Middle Hero Rail: Moves Left -> Right (Larger)
                MemoryRailRow(
                    tiles = middleTiles,
                    speedMultiplier = 1.25f,
                    phase = railPhase,
                    userOffset = userDragOffset * 1.0f,
                    heroScale = 1.05f,
                    onInspect = { selectedMicroLabel = it }
                )

                // Bottom Rail: Moves Right -> Left
                MemoryRailRow(
                    tiles = bottomTiles,
                    speedMultiplier = -1.15f,
                    phase = railPhase,
                    userOffset = userDragOffset * 0.85f,
                    onInspect = { selectedMicroLabel = it }
                )
            }

            // Micro-label inspect status / footer hint
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
        // Repeat tiles twice to guarantee seamless infinite scroll
        for (tile in tiles + tiles) {
            MemoryTileCard(
                data = tile,
                scale = heroScale,
                onInspect = onInspect
            )
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
            width = if (isPressed) 1.5.dp else 1.dp,
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
            // Subtle pixel grid pattern background
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
