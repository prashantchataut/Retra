package app.retra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space

/** Squish-on-press: scales to [pressed] while the interaction source is pressed (snappy spring). */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val reduce = Retra.reduceMotion
    val s by animateFloatAsState(if (isPressed && !reduce) pressed else 1f, Motion.snappy(false), label = "press")
    return graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** Clickable with squish + ripple. */
@Composable
fun Modifier.squishClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role = Role.Button,
    label: String? = null,
    pressed: Float = 0.96f,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return pressScale(source, pressed).combinedClickable(
        interactionSource = source,
        indication = ripple(),
        enabled = enabled,
        role = role,
        onClickLabel = label,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

enum class ButtonStyle { PRIMARY, SECONDARY, TERTIARY, DANGER }

@Composable
fun RetraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = Retra.colors
    val source = remember { MutableInteractionSource() }
    val (bg, fg) = when (style) {
        ButtonStyle.PRIMARY -> c.accent to c.onAccent
        ButtonStyle.SECONDARY -> Color.Transparent to c.ink
        ButtonStyle.TERTIARY -> Color.Transparent to c.accent
        ButtonStyle.DANGER -> c.danger to (if (c.isDark) c.sky else Color.White)
    }
    val shape = Radius.pillShape
    val content: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = if (compact) Space.m else Space.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.xs))
            }
            Text(text, style = RetraType.bodyStrong, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val base = modifier
        .alpha(if (enabled) 1f else 0.38f)
        .pressScale(source)
        .defaultMinSize(minHeight = if (compact) 44.dp else 52.dp)
    val click = Modifier.clickable(interactionSource = source, indication = ripple(color = fg), enabled = enabled, role = Role.Button, onClick = onClick)
    when (style) {
        ButtonStyle.SECONDARY -> GlassSurface(base.height(if (compact) 44.dp else 52.dp), shape = shape, elevation = Elevation.raised / 2) {
            Box(Modifier.matchParentSizeCompat().clip(shape).then(click), contentAlignment = Alignment.Center) { content() }
        }
        else -> Box(
            base.height(if (compact) 44.dp else 52.dp).clip(shape).background(bg).then(click),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Round glass icon button, 48 dp touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = Retra.colors.ink,
    selected: Boolean = false,
    blur: Boolean = false,
) {
    val c = Retra.colors
    val source = remember { MutableInteractionSource() }
    GlassSurface(
        modifier.size(size).pressScale(source, 0.9f),
        shape = Radius.pillShape,
        elevation = Elevation.raised / 2,
        blur = blur,
        wash = if (selected) c.accentSoft else null,
    ) {
        Box(
            Modifier
                .matchParentSizeCompat()
                .clickable(interactionSource = source, indication = ripple(bounded = false), role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = if (selected) c.onAccentSoft else tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** Plain (no glass) icon button with a 48 dp target. */
@Composable
fun PlainIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Retra.colors.ink) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .pressScale(source, 0.9f)
            .clip(Radius.pillShape)
            .clickable(interactionSource = source, indication = ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** Small pill chip for filters and segmented choices. */
@Composable
fun RetraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = Retra.colors
    val source = remember { MutableInteractionSource() }
    val shape = Radius.pillShape
    Row(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .pressScale(source)
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(if (selected) c.accent else c.glassFill)
            .glassHighlights(shape, c)
            .clickable(interactionSource = source, indication = ripple(), role = Role.Tab, onClick = onClick)
            .padding(PaddingValues(horizontal = Space.m, vertical = 9.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) c.onAccent else c.inkMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = RetraType.label, color = if (selected) c.onAccent else c.ink, maxLines = 1)
    }
}

/** Box.matchParentSize equivalent usable from any modifier chain inside a Box scope-less lambda. */
fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxSizeCompat())

private fun Modifier.fillMaxSizeCompat(): Modifier = androidx.compose.foundation.layout.fillMaxSize().then(this)
