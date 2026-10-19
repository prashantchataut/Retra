package app.retra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.LightColors
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType

/** The app mark: Mochi on a glossy liquid-glass tile (same art as the launcher icon). */
@Composable
fun LogoTile(modifier: Modifier = Modifier, size: Dp = 72.dp, mood: MochiMood = MochiMood.HAPPY) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier
            .size(size)
            .shadow(size * 0.12f, shape, ambientColor = Retra.colors.shadow, spotColor = Retra.colors.shadow)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFFFE3EC), Color(0xFFFFC6D8), Color(0xFFE6D9FF))))
            .glassHighlights(shape, LightColors, rimWidth = (size.value / 72f).coerceAtLeast(1f).dp),
        contentAlignment = Alignment.Center,
    ) {
        Mochi(mood = mood, size = size * 0.82f, modifier = Modifier.offset(y = size * 0.03f))
    }
}

/** Tile + "Retra" wordmark. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, tileSize: Dp = 40.dp) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Retra" }, verticalAlignment = Alignment.CenterVertically) {
        LogoTile(size = tileSize)
        Spacer(Modifier.width(10.dp))
        Text("Retra", style = RetraType.title, color = Retra.colors.ink)
    }
}
