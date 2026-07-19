package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings

val LocalRetraFeedback = staticCompositionLocalOf<(FeedbackCue) -> Unit> { { } }
val LocalRetraSettings = staticCompositionLocalOf<AppSettings?> { null }

/**
 * Retra's restrained liquid-glass language.
 *
 * Compose blur is used only for decorative background light on supported devices; content surfaces stay
 * crisp and use translucent layers plus an edge highlight so text never becomes blurry.
 */
@Composable
fun RetraBackdrop(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val showAtmosphere = !settings.reduceTransparency && settings.glassIntensity > 0.08f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.background,
                        colors.surface.copy(alpha = 0.92f)
                    )
                )
            )
    ) {
        if (showAtmosphere) {
            val alpha = (0.05f + settings.glassIntensity * 0.10f).coerceAtMost(0.16f)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 110.dp, y = (-120).dp)
                    .size(360.dp)
                    .blur(96.dp, BlurredEdgeTreatment.Unbounded)
                    .background(colors.primary.copy(alpha = alpha), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-120).dp, y = 130.dp)
                    .size(320.dp)
                    .blur(110.dp, BlurredEdgeTreatment.Unbounded)
                    .background(colors.secondary.copy(alpha = alpha * 0.7f), CircleShape)
            )
        }
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val resolvedSettings = settings ?: LocalRetraSettings.current
    val intensity = resolvedSettings?.glassIntensity?.coerceIn(0f, 1f) ?: 0.58f
    val opaque = resolvedSettings?.reduceTransparency == true
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val container = if (opaque) {
        colors.surface
    } else {
        colors.surface.copy(alpha = (0.50f + intensity * 0.24f).coerceIn(0.50f, 0.76f))
    }
    val edgeBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (opaque) 0.12f else 0.22f),
            colors.primary.copy(alpha = 0.12f + intensity * 0.12f),
            Color.White.copy(alpha = 0.04f)
        )
    )
    Surface(
        modifier = modifier,
        shape = shape,
        color = container,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = if (opaque) 0.dp else 2.dp,
        border = BorderStroke(0.75.dp, edgeBrush)
    ) {
        Box(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (opaque) 0.02f else 0.075f),
                            Color.Transparent,
                            colors.primary.copy(alpha = if (opaque) 0f else intensity * 0.025f)
                        )
                    )
                )
                .padding(contentPadding)
        ) {
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
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primary.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) colors.primary else colors.onSurfaceVariant,
        border = if (selected) BorderStroke(0.75.dp, colors.primary.copy(alpha = 0.24f)) else null
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
        androidx.compose.animation.AnimatedContent(
            targetState = targetState,
            label = label,
            modifier = modifier
        ) { state ->
            content(state)
        }
    }
}
