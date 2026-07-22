package app.retra.emulator

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord

@Composable
fun GameArtwork(
    game: GameRecord,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val image = remember(game.coverArtPath) {
        game.coverArtPath
            ?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    val fallbackColor = remember(game.sha256, game.title) { gameArtworkColor(game) }
    Box(modifier.background(fallbackColor), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Cover art for ${game.title}",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RetraLogo(
                    size = 58.dp,
                    markColor = Color.White.copy(alpha = 0.90f),
                    cutoutColor = fallbackColor
                )
                Text(
                    text = game.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun gameArtworkColor(game: GameRecord): Color {
    val seed = game.sha256.take(8).toLongOrNull(16) ?: game.title.hashCode().toLong()
    val colors = listOf(
        Color(0xFF12303A),
        Color(0xFF18343E),
        Color(0xFF20362F),
        Color(0xFF3A3025),
        Color(0xFF24313A)
    )
    return colors[kotlin.math.abs(seed % colors.size).toInt()]
}
