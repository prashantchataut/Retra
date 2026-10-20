package app.retra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import kotlinx.coroutines.launch

data class DockItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

/**
 * Floating glass dock. The indicator is a liquid drop: when moving to another tab, its leading edge
 * springs ahead (stiff) and the trailing edge follows (softer), so it stretches and then settles.
 */
@Composable
fun Dock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Retra.colors
    val reduce = Retra.reduceMotion
    GlassSurface(
        modifier.height(68.dp).widthIn(max = 460.dp),
        shape = Radius.pillShape,
        elevation = Elevation.floating,
        blur = true,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val count = items.size
            val itemWidthPx = constraints.maxWidth.toFloat() / count
            val left = remember { Animatable(selected.toFloat()) }
            val right = remember { Animatable(selected.toFloat() + 1f) }
            LaunchedEffect(selected) {
                val target = selected.toFloat()
                val movingRight = target > left.value
                val fast = if (reduce) snap<Float>() else spring(0.72f, Motion.SNAPPY)
                val slow = if (reduce) snap<Float>() else spring(Motion.DAMPING, 260f)
                launch { left.animateTo(target, if (movingRight) slow else fast) }
                launch { right.animateTo(target + 1f, if (movingRight) fast else slow) }
            }
            Canvas(Modifier.fillMaxSize()) {
                val l = left.value * itemWidthPx
                val r = right.value * itemWidthPx
                val inset = 3.dp.toPx()
                val stretch = ((r - l) / itemWidthPx - 1f).coerceIn(0f, 2f)
                // Squeeze vertically a little while stretched, like a drop of liquid.
                val vInset = stretch * 4.dp.toPx()
                val h = size.height - vInset * 2
                // One solid capsule; the sheen lives inside its gradient (a separate highlight bar
                // used to poke out of the rounded ends as a stray white line).
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(lerp(c.accent, Color.White, if (c.isDark) 0.22f else 0.12f), c.accent), startY = vInset, endY = vInset + h),
                    topLeft = Offset(l + inset, vInset),
                    size = Size((r - l - inset * 2).coerceAtLeast(0f), h),
                    cornerRadius = CornerRadius(h / 2),
                )
            }
            Row(Modifier.fillMaxSize().selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEachIndexed { i, item ->
                    DockButton(item, i == selected, { onSelect(i) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DockButton(item: DockItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = Retra.colors
    val source = remember { MutableInteractionSource() }
    val reduce = Retra.reduceMotion
    val lift by animateFloatAsState(if (selected) 1f else 0f, Motion.smooth(reduce), label = "lift")
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxHeight()
            .pressScale(source, 0.9f)
            .semantics { this.selected = selected }
            .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = with(density) { (-1).dp.toPx() } * lift }) {
            Icon(
                if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = if (selected) c.onAccent else c.inkMuted,
                modifier = Modifier.size(24.dp),
            )
            Text(item.label, style = RetraType.caption, color = if (selected) c.onAccent else c.inkMuted, maxLines = 1)
        }
    }
}
