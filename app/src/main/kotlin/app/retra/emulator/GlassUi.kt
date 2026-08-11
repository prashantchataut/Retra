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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint

val LocalRetraFeedback = staticCompositionLocalOf<(FeedbackCue) -> Unit> { { } }
val LocalRetraSettings = staticCompositionLocalOf<AppSettings?> { null }

enum class GlassTier {
    THIN,
    REGULAR,
    ELEVATED,
    STRONG,
    OPAQUE
}

/**
 * Retra's Liquid Glass atmosphere.
 *
 * Grounded in deep near-black aubergine with softly lit, background-aware light pools
 * of electric lilac, raspberry glow, aqua mint, and warm coral. Android 12+ renders
 * hardware blur; older versions and accessibility modes render gracefully.
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
            val strength = (0.04f + settings.glassIntensity.coerceIn(0f, 1f) * 0.08f)
            AmbientLight(
                color = ElectricLilac.copy(alpha = strength * 0.9f),
                size = 420.dp,
                blurRadius = 100.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 130.dp, y = (-140).dp)
            )
            AmbientLight(
                color = RaspberryPink.copy(alpha = strength * 0.65f),
                size = 360.dp,
                blurRadius = 110.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-180).dp, y = (-40).dp)
            )
            AmbientLight(
                color = SaveMint.copy(alpha = strength * 0.55f),
                size = 320.dp,
                blurRadius = 105.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 150.dp, y = 120.dp)
            )
            AmbientLight(
                color = MemoryCoral.copy(alpha = strength * 0.45f),
                size = 280.dp,
                blurRadius = 95.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-100).dp, y = 100.dp)
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
 * Retra Multi-Tier Liquid Glass surface.
 *
 * Provides optical translucency, internal highlight gradient, specular rim light,
 * and complete opaque fallback for accessibility.
 */
@Composable
fun RetraGlassSurface(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.REGULAR,
    settings: AppSettings? = null,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val effectiveSettings = settings ?: LocalRetraSettings.current
    val reduceTransparency = effectiveSettings?.reduceTransparency == true || tier == GlassTier.OPAQUE
    val intensity = effectiveSettings?.glassIntensity?.coerceIn(0f, 1f) ?: 0.35f

    val fillAlpha = when {
        reduceTransparency -> 1.0f
        tier == GlassTier.THIN -> (0.42f + intensity * 0.12f).coerceIn(0.42f, 0.65f)
        tier == GlassTier.REGULAR -> (0.68f + intensity * 0.14f).coerceIn(0.68f, 0.86f)
        tier == GlassTier.ELEVATED -> (0.78f + intensity * 0.14f).coerceIn(0.78f, 0.92f)
        tier == GlassTier.STRONG -> (0.88f + intensity * 0.08f).coerceIn(0.88f, 0.96f)
        else -> 1.0f
    }

    val rimAlpha = when {
        reduceTransparency -> 0.90f
        tier == GlassTier.THIN -> 0.35f
        tier == GlassTier.REGULAR -> 0.55f
        tier == GlassTier.ELEVATED -> 0.70f
        tier == GlassTier.STRONG -> 0.85f
        else -> 0.90f
    }

    Surface(
        modifier = modifier
            .border(1.dp, colors.outlineVariant.copy(alpha = rimAlpha), shape)
            .padding(1.dp),
        shape = shape,
        color = colors.surface.copy(alpha = fillAlpha),
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = if (reduceTransparency) 0.dp else when (tier) {
            GlassTier.THIN -> 2.dp
            GlassTier.REGULAR -> 6.dp
            GlassTier.ELEVATED -> 12.dp
            GlassTier.STRONG -> 18.dp
            GlassTier.OPAQUE -> 0.dp
        }
    ) {
        Box(
            Modifier
                .clip(shape)
                .then(
                    if (reduceTransparency) Modifier
                    else Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.09f + intensity * 0.04f),
                                Color.Transparent,
                                colors.primary.copy(alpha = 0.04f + intensity * 0.03f)
                            )
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (reduceTransparency) 0f else 0.09f + intensity * 0.06f),
                    shape = shape
                )
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Standard GlassPanel wrapper forwarding to [RetraGlassSurface].
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    RetraGlassSurface(
        modifier = modifier,
        tier = GlassTier.REGULAR,
        settings = settings,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}

/** @deprecated Prefer GlassPanel(shape = …) or RetraPanel. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    cornerRadius: Dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val mapped = when {
        cornerRadius.value <= 10f -> MaterialTheme.shapes.extraSmall
        cornerRadius.value <= 14f -> MaterialTheme.shapes.small
        cornerRadius.value <= 20f -> MaterialTheme.shapes.medium
        cornerRadius.value <= 27f -> MaterialTheme.shapes.large
        else -> MaterialTheme.shapes.extraLarge
    }
    GlassPanel(
        modifier = modifier,
        settings = settings,
        shape = mapped,
        contentPadding = contentPadding,
        content = content
    )
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
        shape = CircleShape,
        color = when {
            selected -> colors.primaryContainer.copy(alpha = if (opaque) 1f else 0.82f)
            opaque -> colors.surfaceVariant
            else -> colors.surface.copy(alpha = 0.70f)
        },
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) colors.primary.copy(alpha = 0.55f) else colors.outlineVariant.copy(alpha = 0.60f)
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
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
            label = label,
            modifier = modifier
        ) { state -> content(state) }
    }
}
