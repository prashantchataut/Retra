package app.retra.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.retra.data.AppPalette
import app.retra.data.Settings
import app.retra.ui.components.AmbientSky
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.LogoTile
import app.retra.ui.components.Mochi
import app.retra.ui.components.MochiMood
import app.retra.ui.components.RetraButton
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import app.retra.ui.theme.paletteColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class Page(val title: String, val body: String)

private val pages = listOf(
    Page("Hi, I'm Mochi!", "Retra plays Game Boy Advance games: the ones you own, free homebrew, and Pokémon fan games. I'll show you around."),
    Page("Bring your whole shelf", "Import games, saves or a whole folder in one go. A .sav next to a game is loaded for you."),
    Page("Make it yours", "Pick a colour world, restyle the buttons, map your controller. Try a colour right here."),
    Page("Play your way", "Rewind a few seconds in one tap, find cheats with search, or take on a sealed No-Cheat Run or Nuzlocke."),
    Page("Your progress travels", "Saves are backed up as you play. Connect Google Drive to carry them to another phone, and host a room for friends."),
)

/**
 * First-run tour. Each page has its own small animated scene driven by one shared clock; the pager adds parallax
 * (scene and text drift at different speeds), the text rises in once its page settles, and the last page ends with
 * a confetti burst. Reduced motion freezes every scene on a pleasant still frame and removes the confetti.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onImport: () -> Unit,
    onExplore: () -> Unit,
    initialPage: Int = 0,
    settings: Settings = Settings(),
    onUpdate: ((Settings) -> Settings) -> Unit = {},
) {
    val c = Retra.colors
    val pager = rememberPagerState(initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        AmbientSky()
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Space.gutter, vertical = Space.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LogoTile(size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text("Retra", style = RetraType.title, color = c.ink, modifier = Modifier.weight(1f))
                if (pager.currentPage < pages.lastIndex) RetraButton("Skip", onFinish, style = ButtonStyle.TERTIARY, compact = true)
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { i ->
                val offset = ((pager.currentPage - i) + pager.currentPageOffsetFraction)
                PageContent(i, offset, pager.currentPage == i, settings, onUpdate)
            }
            Dots(pager)
            Spacer(Modifier.height(Space.l))
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                if (pager.currentPage == pages.lastIndex) {
                    RetraButton("Import your games", { onFinish(); onImport() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.AddCircle)
                    Spacer(Modifier.height(Space.xs))
                    RetraButton("Explore the library", { onFinish(); onExplore() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Storefront, style = ButtonStyle.SECONDARY)
                } else {
                    RetraButton("Next", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
                    Spacer(Modifier.height(Space.xs))
                    Spacer(Modifier.height(52.dp))
                }
            }
        }
        if (pager.currentPage == pages.lastIndex) Confetti(Modifier.fillMaxSize())
    }
}

@Composable
private fun PageContent(index: Int, offset: Float, active: Boolean, settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val c = Retra.colors
    val reduce = Retra.reduceMotion
    val p = pages[index]
    // Text rises in once its page is the current one.
    val rise by animateFloatAsState(if (active) 1f else 0f, if (reduce) tween(0) else spring(0.8f, 260f), label = "rise")
    val clock = rememberClock()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(272.dp).graphicsLayer {
                translationX = -offset * size.width * 0.45f
                val s = 1f - abs(offset).coerceAtMost(1f) * 0.12f
                scaleX = s; scaleY = s; alpha = 1f - abs(offset).coerceAtMost(1f) * 0.6f
            },
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(Modifier.size(248.dp), shape = Radius.pillShape) {}
            when (index) {
                0 -> HelloScene(clock)
                1 -> ShelfScene(clock)
                2 -> PaletteScene(settings.palette, clock) { v -> onUpdate { it.copy(palette = v) } }
                3 -> PlayScene(clock)
                else -> CloudScene(clock)
            }
        }
        Spacer(Modifier.height(Space.xl))
        Column(
            Modifier.graphicsLayer {
                translationX = -offset * size.width * 0.2f
                alpha = (1f - abs(offset)).coerceIn(0f, 1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(p.title, style = RetraType.display, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { translationY = (1f - rise) * 28f; alpha = rise })
            Spacer(Modifier.height(Space.s))
            Text(
                p.body, style = RetraType.body, color = c.inkMuted, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp).graphicsLayer { translationY = (1f - rise) * 44f; alpha = rise * rise },
            )
        }
    }
}

/** One looping 0..1 clock (6 s) shared by a scene's parts; a calm still frame under reduced motion. */
@Composable
private fun rememberClock(): Float {
    if (Retra.reduceMotion) return 0.3f
    val t = rememberInfiniteTransition(label = "clock")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return v
}

private fun twoPi(x: Float) = (x * 2 * PI).toFloat()

// ------------------------------------------------------------------ scenes

@Composable
private fun HelloScene(t: Float) {
    val c = Retra.colors
    val bob = sin(twoPi(t * 2)) * 6f
    Canvas(Modifier.size(272.dp)) {
        val r = size.minDimension * 0.42f
        repeat(6) { i ->
            val a = twoPi(t + i / 6f)
            val rr = r + (if (i % 2 == 0) 8f else -6f)
            val pulse = 0.6f + 0.4f * sin(twoPi(t * 3 + i / 6f))
            sparkle(center + Offset(cos(a) * rr, sin(a) * rr * 0.9f), 7.dp.toPx() * pulse, if (i % 2 == 0) c.brand else c.accent)
        }
    }
    Mochi(mood = MochiMood.HAPPY, size = 176.dp, description = "Mochi", modifier = Modifier.graphicsLayer { translationY = bob })
}

@Composable
private fun ShelfScene(t: Float) {
    val c = Retra.colors
    val labels = listOf(".gba", ".sav", ".zip")
    val tints = listOf(c.accentSoft, c.brand.copy(alpha = 0.25f), c.inkFaint)
    Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
        // A folder-ish tray the cards drop into.
        Canvas(Modifier.size(200.dp, 120.dp).graphicsLayer { translationY = 52f }) {
            drawRoundRect(c.accent.copy(alpha = 0.16f), cornerRadius = CornerRadius(28.dp.toPx()))
            drawRoundRect(c.accent.copy(alpha = 0.5f), cornerRadius = CornerRadius(28.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        labels.forEachIndexed { i, label ->
            val phase = (t + i / 3f) % 1f
            val drop = if (phase < 0.5f) phase * 2f else 1f
            val ease = 1f - (1f - drop) * (1f - drop)
            val dy = -86f + ease * 118f + (if (phase >= 0.5f) sin(twoPi(phase * 2)) * 3f else 0f)
            val dx = (i - 1) * 62f
            val tilt = (i - 1) * 7f * (1f - ease)
            Box(
                Modifier.graphicsLayer { translationX = dx.dp.toPx() * 0.9f; translationY = dy.dp.toPx() * 0.9f; rotationZ = tilt }
                    .size(64.dp, 82.dp).clip(Radius.smallShape).background(tints[i]),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp, 24.dp).clip(Radius.smallShape).background(c.accent.copy(alpha = 0.55f)))
                    Spacer(Modifier.height(8.dp))
                    Text(label, style = RetraType.caption, color = c.ink)
                }
            }
        }
    }
}

@Composable
private fun PaletteScene(selected: AppPalette, t: Float, onSelect: (AppPalette) -> Unit) {
    val c = Retra.colors
    val all = AppPalette.entries
    Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
        all.forEachIndexed { i, p ->
            val look = paletteColors(p, c.isDark)
            val sel = p == selected
            val s by animateFloatAsState(if (sel) 1.25f else 1f, spring(0.55f, 500f), label = "orb")
            val a = twoPi(i / all.size.toFloat() + t * 0.08f) - (PI / 2).toFloat()
            Box(
                Modifier
                    .graphicsLayer { translationX = cos(a) * 96.dp.toPx(); translationY = sin(a) * 96.dp.toPx(); scaleX = s; scaleY = s }
                    .size(48.dp).clip(CircleShape).background(look.accent)
                    .clickable { onSelect(p) }
                    .semantics { contentDescription = "${p.label}${if (sel) ", selected" else ""}" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(if (sel) 20.dp else 14.dp).clip(CircleShape).background(look.sky))
            }
        }
        Mochi(mood = MochiMood.HAPPY, size = 112.dp, description = "Mochi in ${selected.label}")
    }
}

@Composable
private fun PlayScene(t: Float) {
    val c = Retra.colors
    // A timeline whose playhead runs right, then a rewind chip snaps it back.
    val run = (t * 1.25f).coerceAtMost(1f)
    val back = if (t > 0.8f) ((t - 0.8f) / 0.2f).coerceIn(0f, 1f) else 0f
    val head = if (back > 0f) run * (1f - back * 0.55f) else run
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).graphicsLayer { rotationZ = -back * 360f; scaleX = 1f + back * 0.1f; scaleY = 1f + back * 0.1f }.clip(CircleShape).background(c.accent),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.FastRewind, null, tint = c.onAccent, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(Space.m))
        Canvas(Modifier.size(180.dp, 24.dp)) {
            val h = 8.dp.toPx()
            drawRoundRect(c.inkFaint, Offset(0f, (size.height - h) / 2), Size(size.width, h), CornerRadius(h))
            drawRoundRect(c.accent, Offset(0f, (size.height - h) / 2), Size(size.width * head, h), CornerRadius(h))
            drawCircle(c.brand, 10.dp.toPx(), Offset(size.width * head, size.height / 2))
        }
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, null, tint = c.accent, modifier = Modifier.size(26.dp).graphicsLayer { val s = 1f + 0.08f * sin(twoPi(t * 3)); scaleX = s; scaleY = s })
            Text("Sealed run", style = RetraType.label, color = c.inkMuted)
        }
    }
}

@Composable
private fun CloudScene(t: Float) {
    val c = Retra.colors
    Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Cloud, null, tint = c.accent, modifier = Modifier.size(120.dp).graphicsLayer { translationY = -34f - sin(twoPi(t * 2)) * 4f })
        repeat(3) { i ->
            val phase = (t * 1.5f + i / 3f) % 1f
            val y = 80f - phase * 120f
            val x = (i - 1) * 54f
            Box(
                Modifier.graphicsLayer { translationX = x.dp.toPx() * 0.9f; translationY = y.dp.toPx() * 0.9f; alpha = (1f - phase).coerceIn(0f, 1f) * 1.4f; scaleX = 1f - phase * 0.4f; scaleY = 1f - phase * 0.4f }
                    .size(30.dp, 22.dp).clip(Radius.smallShape).background(if (i == 1) c.brand else c.accent),
            )
        }
        Canvas(Modifier.size(76.dp, 44.dp).graphicsLayer { translationY = 88f }) {
            drawRoundRect(c.ink.copy(alpha = 0.8f), cornerRadius = CornerRadius(12.dp.toPx()))
            drawRoundRect(c.sky, Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()), CornerRadius(8.dp.toPx()))
        }
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r); quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
    }
    rotate(0f, c) { drawPath(p, color) }
}

// ------------------------------------------------------------------ confetti + dots

@Composable
private fun Confetti(modifier: Modifier) {
    val c = Retra.colors
    if (Retra.reduceMotion) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2200, easing = LinearEasing)) }
    val colors = listOf(c.brand, c.accent, c.success, c.warning, c.accentSoft)
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        repeat(36) { i ->
            val seed = (i * 7919 % 100) / 100f
            val seed2 = (i * 104729 % 100) / 100f
            val angle = (seed * 2f - 1f) * 1.1f - (PI / 2).toFloat()
            val speed = (0.45f + seed2 * 0.8f) * size.height * 0.55f
            val x = size.width / 2 + cos(angle) * speed * p + (seed - 0.5f) * 60f
            val y = size.height * 0.62f + sin(angle) * speed * p + p * p * size.height * 0.5f
            rotate(p * 360f * (if (i % 2 == 0) 1 else -1) + seed * 180f, Offset(x, y)) {
                drawRoundRect(colors[i % colors.size].copy(alpha = (1f - p).coerceIn(0f, 1f)), Offset(x, y), Size(8.dp.toPx(), 12.dp.toPx() * (0.6f + seed2)), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun Dots(pager: PagerState) {
    val c = Retra.colors
    Row(Modifier.semantics { contentDescription = "Page ${pager.currentPage + 1} of ${pages.size}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pages.size) { i ->
            val w by animateDpAsState(if (i == pager.currentPage) 28.dp else 8.dp, Motion.smooth(Retra.reduceMotion), label = "dot")
            Box(Modifier.height(8.dp).width(w).clip(Radius.pillShape).background(if (i == pager.currentPage) c.accent else c.inkMuted.copy(alpha = 0.3f)))
        }
    }
}
