package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings

val LocalRetraFeedback = staticCompositionLocalOf<(FeedbackCue) -> Unit> { { } }
val LocalRetraSettings = staticCompositionLocalOf<AppSettings?> { null }

/** Compatibility wrappers for the older UI. They now render as calm, opaque Material surfaces. */
@Composable
fun RetraBackdrop(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius.coerceAtMost(16.dp)) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.surface,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primaryContainer else Color.Transparent,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    ) {
        content()
    }
}

@Composable
fun <T> RetraAnimatedContent(
    targetState: T,
    reduceMotion: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    if (reduceMotion) {
        Box(modifier) {
            androidx.compose.runtime.key(targetState) { content(targetState) }
        }
    } else {
        androidx.compose.animation.Crossfade(
            targetState = targetState,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
            label = label,
            modifier = modifier
        ) { state ->
            content(state)
        }
    }
}
