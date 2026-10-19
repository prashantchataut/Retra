package app.retra.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String, val mood: MochiMood, val badge: ImageVector)

private val pages = listOf(
    Page("Hi, I'm Mochi!", "Retra plays Game Boy Advance games: the ones you own, free homebrew, and Pokémon fan games.", MochiMood.HAPPY, Icons.Rounded.Favorite),
    Page("Fan games, made easy", "Pick a fan game, add your own cartridge dump, and Retra patches it for you. We never hand out commercial games.", MochiMood.SURPRISED, Icons.Rounded.AutoFixHigh),
    Page("Your progress is safe", "Retra saves as you play, keeps backups of your saves, and remembers your spot so you can continue in one tap.", MochiMood.SLEEPY, Icons.Rounded.Save),
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onImport: () -> Unit,
    onExplore: () -> Unit,
    initialPage: Int = 0,
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
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                val p = pages[i]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        GlassSurface(Modifier.size(232.dp), shape = Radius.pillShape) {}
                        Mochi(mood = p.mood, size = 176.dp, description = "Mochi")
                        GlassSurface(Modifier.align(Alignment.BottomEnd).size(56.dp), shape = Radius.pillShape, wash = c.accentSoft) {
                            Icon(p.badge, null, tint = c.onAccentSoft, modifier = Modifier.align(Alignment.Center).size(26.dp))
                        }
                    }
                    Spacer(Modifier.height(Space.xl))
                    Text(p.title, style = RetraType.display, color = c.ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Space.s))
                    Text(p.body, style = RetraType.body, color = c.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp))
                }
            }
            Dots(pager.currentPage, pages.size)
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
    }
}

@Composable
private fun Dots(current: Int, count: Int) {
    val c = Retra.colors
    Row(Modifier.semantics { contentDescription = "Page ${current + 1} of $count" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 28.dp else 8.dp, Motion.smooth(Retra.reduceMotion), label = "dot")
            Box(Modifier.height(8.dp).width(w).clip(Radius.pillShape).background(if (i == current) c.accent else c.inkMuted.copy(alpha = 0.3f)))
        }
    }
}
