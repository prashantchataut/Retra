package app.retra.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.VoidBlack

/**
 * Retra's vault-aperture mark.
 *
 * The geometry is deliberately centered and non-letterform: a protected archive
 * ring surrounds a small memory prism. Four cardinal cuts suggest a D-pad and an
 * aperture without turning the symbol into a literal controller or cartridge.
 */
@Composable
fun RetraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = null,
    markColor: Color = MaterialTheme.colorScheme.onSurface,
    cutoutColor: Color = MaterialTheme.colorScheme.surface,
    sparkColor: Color = MemoryCoral
) {
    val semanticsModifier = if (contentDescription == null) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(semanticsModifier.size(size)) {
        val side = this.size.minDimension
        val resolvedMark = markColor
        val resolvedCutout = cutoutColor

        // Rounded archive ring.
        drawRoundRect(
            color = resolvedMark,
            topLeft = Offset(side * 0.10f, side * 0.10f),
            size = Size(side * 0.80f, side * 0.80f),
            cornerRadius = CornerRadius(side * 0.24f, side * 0.24f)
        )
        drawRoundRect(
            color = resolvedCutout,
            topLeft = Offset(side * 0.235f, side * 0.235f),
            size = Size(side * 0.53f, side * 0.53f),
            cornerRadius = CornerRadius(side * 0.15f, side * 0.15f)
        )

        // Cardinal cuts give the ring a precise aperture / D-pad rhythm.
        val slotLong = side * 0.22f
        val slotShort = side * 0.075f
        drawRoundRect(
            color = resolvedCutout,
            topLeft = Offset((side - slotLong) / 2f, side * 0.075f),
            size = Size(slotLong, slotShort),
            cornerRadius = CornerRadius(slotShort / 2f, slotShort / 2f)
        )
        drawRoundRect(
            color = resolvedCutout,
            topLeft = Offset((side - slotLong) / 2f, side * 0.85f),
            size = Size(slotLong, slotShort),
            cornerRadius = CornerRadius(slotShort / 2f, slotShort / 2f)
        )
        drawRoundRect(
            color = resolvedCutout,
            topLeft = Offset(side * 0.075f, (side - slotLong) / 2f),
            size = Size(slotShort, slotLong),
            cornerRadius = CornerRadius(slotShort / 2f, slotShort / 2f)
        )
        drawRoundRect(
            color = resolvedCutout,
            topLeft = Offset(side * 0.85f, (side - slotLong) / 2f),
            size = Size(slotShort, slotLong),
            cornerRadius = CornerRadius(slotShort / 2f, slotShort / 2f)
        )

        // Memory prism: a centered diamond with a protected core.
        rotate(45f, pivot = Offset(side / 2f, side / 2f)) {
            drawRoundRect(
                color = resolvedMark,
                topLeft = Offset(side * 0.355f, side * 0.355f),
                size = Size(side * 0.29f, side * 0.29f),
                cornerRadius = CornerRadius(side * 0.055f, side * 0.055f)
            )
            drawRoundRect(
                color = resolvedCutout,
                topLeft = Offset(side * 0.425f, side * 0.425f),
                size = Size(side * 0.15f, side * 0.15f),
                cornerRadius = CornerRadius(side * 0.028f, side * 0.028f)
            )
        }

        // A single centered spark is the only warm accent.
        drawCircle(
            color = sparkColor,
            radius = side * 0.045f,
            center = Offset(side * 0.50f, side * 0.50f)
        )
    }
}

@Composable
fun RetraLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val radius = size * 0.28f
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(radius),
        color = VoidBlack,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            RetraLogo(
                modifier = Modifier.padding(size * 0.17f),
                size = size * 0.66f,
                contentDescription = "Retra",
                markColor = Color.White,
                cutoutColor = VoidBlack
            )
        }
    }
}

@Composable
fun RetraBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    RetraLogo(modifier = modifier, size = size, contentDescription = "Retra")
}
