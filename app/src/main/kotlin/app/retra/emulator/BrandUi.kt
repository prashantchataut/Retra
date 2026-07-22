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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.VoidBlack

/**
 * Retra's canonical Portal / Save Core mark, reconstructed from the supplied brand board.
 * It is intentionally flat and geometric so it remains legible from launcher size down to 16dp.
 */
@Composable
fun RetraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String = "Retra",
    markColor: Color? = null,
    cutoutColor: Color? = null
) {
    val resolvedMarkColor = markColor ?: MaterialTheme.colorScheme.onSurface
    val resolvedCutoutColor = cutoutColor ?: MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val side = this.size.minDimension
        val strokeWidth = side * 0.155f
        val portal = Path().apply {
            moveTo(side * 0.76f, side * 0.31f)
            lineTo(side * 0.76f, side * 0.62f)
            cubicTo(
                side * 0.76f, side * 0.76f,
                side * 0.66f, side * 0.84f,
                side * 0.50f, side * 0.84f
            )
            lineTo(side * 0.37f, side * 0.84f)
            cubicTo(
                side * 0.23f, side * 0.84f,
                side * 0.15f, side * 0.74f,
                side * 0.15f, side * 0.59f
            )
            lineTo(side * 0.15f, side * 0.35f)
            cubicTo(
                side * 0.15f, side * 0.22f,
                side * 0.25f, side * 0.14f,
                side * 0.39f, side * 0.14f
            )
            lineTo(side * 0.58f, side * 0.14f)
        }
        drawPath(
            path = portal,
            color = resolvedMarkColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Save core: a tiny protected world inside the loop.
        drawRoundRect(
            color = resolvedMarkColor,
            topLeft = Offset(side * 0.29f, side * 0.37f),
            size = Size(side * 0.36f, side * 0.31f),
            cornerRadius = CornerRadius(side * 0.075f, side * 0.075f)
        )
        drawRoundRect(
            color = resolvedCutoutColor,
            topLeft = Offset(side * 0.365f, side * 0.505f),
            size = Size(side * 0.055f, side * 0.06f),
            cornerRadius = CornerRadius(side * 0.018f, side * 0.018f)
        )
        drawRoundRect(
            color = resolvedCutoutColor,
            topLeft = Offset(side * 0.515f, side * 0.505f),
            size = Size(side * 0.055f, side * 0.06f),
            cornerRadius = CornerRadius(side * 0.018f, side * 0.018f)
        )

        // Pixel-step interruption: two archive pixels and one living-memory spark.
        val pixel = side * 0.105f
        drawRoundRect(
            color = resolvedMarkColor,
            topLeft = Offset(side * 0.60f, side * 0.21f),
            size = Size(pixel, pixel),
            cornerRadius = CornerRadius(side * 0.018f, side * 0.018f)
        )
        drawRoundRect(
            color = resolvedMarkColor,
            topLeft = Offset(side * 0.69f, side * 0.12f),
            size = Size(pixel, pixel),
            cornerRadius = CornerRadius(side * 0.018f, side * 0.018f)
        )
        drawRoundRect(
            color = MemoryCoral,
            topLeft = Offset(side * 0.78f, side * 0.03f),
            size = Size(pixel, pixel),
            cornerRadius = CornerRadius(side * 0.018f, side * 0.018f)
        )
    }
}

@Composable
fun RetraLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val radius = size * 0.27f
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
                modifier = Modifier.padding(size * 0.15f),
                size = size * 0.70f,
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
