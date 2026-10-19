package app.retra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** The HazeState whose source is the scrolling screen content (null in previews/tests without it). */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Liquid-glass surface: translucent fill, top-lit sheen, specular rim (bright top, fading bottom),
 * a faint inner border and a soft tinted shadow.
 *
 * [blur] = true samples and blurs the content behind (Haze; API 31+). Use it only where content
 * scrolls underneath (dock, headers, sheets). Below API 31 Haze falls back to a denser frosted tint.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = Radius.mediumShape,
    elevation: Dp = Elevation.raised,
    blur: Boolean = false,
    strong: Boolean = false,
    wash: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Retra.colors
    val haze = LocalHazeState.current
    val fill = if (strong) c.glassFillStrong else c.glassFill
    val blurStyle = remember(c, strong) { glassBlurStyle(c, strong) }
    Box(
        modifier
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
            .clip(shape)
            .then(
                if (blur && haze != null) Modifier.hazeBlur(HazeInput.Sources(haze), blurStyle)
                else Modifier.background(fill),
            )
            .then(if (wash != null) Modifier.background(wash) else Modifier)
            .glassHighlights(shape, c),
        content = content,
    )
}

private fun glassBlurStyle(c: RetraColors, strong: Boolean) = HazeBlurStyle {
    blurRadius(if (strong) 32.dp else 24.dp)
    noiseFactor(0.04f)
    backgroundColor(c.sky)
    colorEffects(listOf(HazeColorEffect.tint((if (strong) c.glassFillStrong else c.glassFill).copy(alpha = if (c.isDark) 0.62f else 0.55f))))
    fallbackColorEffect(HazeColorEffect.tint(c.glassFillStrong))
}

/** Draws the sheen + specular rim + inner border on top of the content's background. */
fun Modifier.glassHighlights(shape: Shape, c: RetraColors, rimWidth: Dp = 1.dp): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val rimPx = rimWidth.toPx()
    val sheen = Brush.verticalGradient(
        0f to c.glassSheen,
        0.42f to c.glassSheen.copy(alpha = c.glassSheen.alpha * 0.18f),
        1f to Color.Transparent,
    )
    val rim = Brush.verticalGradient(0f to c.glassRimTop, 0.5f to c.glassRimBottom, 1f to c.glassRimBottom.copy(alpha = c.glassRimBottom.alpha * 2.5f))
    val innerInset = rimPx * 2.5f
    val innerOutline = shape.createOutline(Size(size.width - innerInset * 2, size.height - innerInset * 2), layoutDirection, this)
    val inner = Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.06f else 0.35f), 0.3f to Color.Transparent)
    onDrawWithContent {
        drawOutlineSheen(outline, sheen)
        drawContent()
        drawOutline(outline, rim, style = Stroke(rimPx * 2))
        translate(innerInset, innerInset) { drawOutline(innerOutline, inner, style = Stroke(rimPx)) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutlineSheen(outline: Outline, brush: Brush) {
    // Sheen covers the top ~45 % only; drawn under content so text stays crisp.
    drawRect(brush, topLeft = Offset.Zero, size = Size(size.width, size.height * 0.6f))
    if (outline is Outline.Generic) Unit
}
