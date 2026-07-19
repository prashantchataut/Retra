package app.retra.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.emulator.ui.theme.MemoryViolet
import app.retra.emulator.ui.theme.PrismCyan
import app.retra.emulator.ui.theme.RetraIndigo

@Composable
fun RetraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String = "Retra"
) {
    Image(
        painter = painterResource(R.drawable.retra_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

@Composable
fun RetraLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(
                        RetraIndigo.copy(alpha = 0.32f),
                        MemoryViolet.copy(alpha = 0.18f),
                        PrismCyan.copy(alpha = 0.12f)
                    )
                )
            )
            .padding(size * 0.05f),
        contentAlignment = Alignment.Center
    ) {
        RetraLogo(size = size * 0.9f)
    }
}

fun retraAuroraBrush(): Brush = Brush.radialGradient(
    colors = listOf(
        RetraIndigo.copy(alpha = 0.32f),
        MemoryViolet.copy(alpha = 0.12f),
        PrismCyan.copy(alpha = 0.06f),
        Color.Transparent
    ),
    radius = 1200f
)
