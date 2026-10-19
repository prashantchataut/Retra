package app.retra.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.unit.dp
import app.retra.data.AppMessage
import app.retra.data.MessageTone
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-window glass bottom sheet (same window as the content, so the backdrop blur works):
 * scrim, drag handle, drag-down or back to dismiss.
 */
@Composable
fun GlassSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Retra.colors
    val reduce = Retra.reduceMotion
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close", onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reduce) fadeIn() else slideInVertically(spring(Motion.DAMPING, Motion.SMOOTH)) { it } + fadeIn(tween(120)),
            exit = if (reduce) fadeOut() else slideOutVertically(spring(1f, Motion.SMOOTH)) { it } + fadeOut(tween(200, 80)),
        ) {
            val offset = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.88f
            val shape = RoundedCornerShape(topStart = Radius.large, topEnd = Radius.large)
            GlassSurface(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .graphicsLayer { translationY = offset.value }
                    .semantics { if (title != null) paneTitle = title },
                shape = shape,
                elevation = Elevation.overlay,
                blur = true,
                strong = true,
            ) {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))) {
                    // Drag handle area.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceAtLeast(0f)) } },
                                onDragStopped = { v ->
                                    if (offset.value > with(density) { 120.dp.toPx() } || v > 1800f) onDismiss()
                                    else offset.animateTo(0f, spring(Motion.DAMPING, Motion.SMOOTH))
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(width = 40.dp, height = 5.dp).clip(Radius.pillShape).background(c.inkMuted.copy(alpha = 0.35f)))
                    }
                    if (title != null) {
                        Text(title, style = RetraType.title, color = c.ink, modifier = Modifier.padding(start = Space.l, end = Space.l, bottom = Space.s))
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(start = Space.l, end = Space.l, bottom = Space.l),
                        content = content,
                    )
                }
            }
            LaunchedEffect(Unit) { offset.snapTo(0f) }
        }
    }
}

/** Glass toast host; shows app messages at the top, one at a time. */
@Composable
fun BoxScope.ToastHost(message: AppMessage?, onDone: () -> Unit) {
    val c = Retra.colors
    var current by remember { mutableStateOf<AppMessage?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            current = message
            delay(2800)
            current = null
            delay(250)
            onDone()
        }
    }
    AnimatedVisibility(
        current != null,
        modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(top = Space.xs),
        enter = slideInVertically(spring(Motion.DAMPING, Motion.SMOOTH)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val m = current ?: message ?: return@AnimatedVisibility
        GlassSurface(
            Modifier.padding(horizontal = Space.m).widthIn(max = 520.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = m.text },
            shape = Radius.pillShape,
            elevation = Elevation.floating,
            blur = true,
            strong = true,
        ) {
            androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = Space.m, vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = when (m.tone) {
                    MessageTone.SUCCESS -> Icons.Rounded.CheckCircle to c.success
                    MessageTone.ERROR -> Icons.Rounded.ErrorOutline to c.danger
                    MessageTone.INFO -> Icons.Rounded.Info to c.accent
                }
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(Space.xs))
                Text(m.text, style = RetraType.label, color = c.ink)
            }
        }
    }
}

@Suppress("unused")
private val navBars = WindowInsets.Companion
