package app.retra.emulator

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

val LocalRetraFeedback = staticCompositionLocalOf<(FeedbackCue) -> Unit> { { } }
val LocalRetraSettings = staticCompositionLocalOf<AppSettings?> { null }

/**
 * Retra's Archive Glass atmosphere.
 *
 * The background uses a few blurred, solid-color light pools rather than visible
 * multicolor gradients. Android 12+ renders the blur; older versions simply show
 * very soft translucent circles. Reduced-transparency removes the decoration.
 */
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
        if (!settings.reduceTransparency && settings.glassIntensity > 0.02f) {
            val strength = (0.035f + settings.glassIntensity.coerceIn(0f, 1f) * 0.07f)
            AmbientLight(
                color = RetraBlue.copy(alpha = strength),
                size = 410.dp,
                blurRadius = 92.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 145.dp, y = (-160).dp)
            )
            AmbientLight(
                color = MemoryAqua.copy(alpha = strength * 0.72f),
                size = 340.dp,
                blurRadius = 110.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-210).dp, y = 20.dp)
            )
            AmbientLight(
                color = MemoryCoral.copy(alpha = strength * 0.62f),
                size = 360.dp,
                blurRadius = 120.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 190.dp, y = 150.dp)
            )
            AmbientLight(
                color = SaveMint.copy(alpha = strength * 0.42f),
                size = 220.dp,
                blurRadius = 92.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-90).dp, y = 120.dp)
            )
        }
        content()
    }
}

@Composable
private fun AmbientLight(
    color: Color,
    size: Dp,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }
    Box(
        modifier
            .size(size)
            .then(blurModifier)
            .background(color, CircleShape)
    )
}

/**
 * A readable liquid-glass panel. The content itself is never blurred.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val effectiveSettings = settings ?: LocalRetraSettings.current
    val reduceTransparency = effectiveSettings?.reduceTransparency == true
    val intensity = effectiveSettings?.glassIntensity?.coerceIn(0f, 1f) ?: 0.42f
    val shape = RoundedCornerShape(cornerRadius)
    val fillAlpha = if (reduceTransparency) 1f else (0.68f + intensity * 0.16f).coerceIn(0.68f, 0.88f)
    val edgeAlpha = if (reduceTransparency) 0.88f else (0.42f + intensity * 0.20f)

    Surface(
        modifier = modifier
            .border(1.dp, colors.outlineVariant.copy(alpha = edgeAlpha), shape)
            .padding(1.dp),
        shape = shape,
        color = colors.surface.copy(alpha = fillAlpha),
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = if (reduceTransparency) 0.dp else 7.dp
    ) {
        Box(
            Modifier
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (reduceTransparency) 0f else 0.08f + intensity * 0.07f),
                    shape = shape
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
    val settings = LocalRetraSettings.current
    val opaque = settings?.reduceTransparency == true
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = when {
            selected -> colors.primaryContainer.copy(alpha = if (opaque) 1f else 0.78f)
            opaque -> colors.surfaceVariant
            else -> colors.surface.copy(alpha = 0.68f)
        },
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) colors.primary.copy(alpha = 0.42f) else colors.outlineVariant.copy(alpha = 0.58f)
        )
    ) { content() }
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
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 210),
            label = label,
            modifier = modifier
        ) { state -> content(state) }
    }
}
