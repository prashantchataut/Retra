package app.retra.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.ui.theme.Radius
import app.retra.ui.theme.RetraType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What to show as a game's cover, in priority order: bundled art, last screenshot, generated. */
@Immutable
data class CoverModel(
    val title: String,
    val gameCode: String,
    val seed: Long,
    val assetPath: String? = null,
    val filePath: String? = null,
    val fileStamp: Long = 0,
)

/** Small decode cache shared by all covers. */
object CoverImages {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cached(key: String): ImageBitmap? = cache.get(key)

    suspend fun load(context: Context, model: CoverModel): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = model.assetPath ?: "${model.filePath}@${model.fileStamp}"
        cache.get(key)?.let { return@withContext it }
        val bmp = runCatching {
            when {
                model.assetPath != null -> context.assets.open(model.assetPath).use { BitmapFactory.decodeStream(it) }
                model.filePath != null && File(model.filePath).exists() -> BitmapFactory.decodeFile(model.filePath)
                else -> null
            }
        }.getOrNull()?.asImageBitmap() ?: return@withContext null
        cache.put(key, bmp)
        bmp
    }

    fun loadBlocking(context: Context, path: String): ImageBitmap? =
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
}

/** Pixel-art friendly cover: images are drawn with nearest-neighbour filtering. */
@Composable
fun GameCover(model: CoverModel, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val context = LocalContext.current
    val hasImage = model.assetPath != null || model.filePath != null
    val key = model.assetPath ?: "${model.filePath}@${model.fileStamp}"
    val image by produceState(initialValue = if (hasImage) CoverImages.cached(key) else null, model) {
        if (hasImage && value == null) value = CoverImages.load(context, model)
    }
    Box(modifier) {
        val img = image
        if (img != null) {
            Image(
                img, contentDescription = null, contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize(),
            )
        } else {
            GeneratedCover(model, Modifier.fillMaxSize(), showTitle)
        }
    }
}

private val coverPalettes = listOf(
    Triple(Color(0xFFFFD6E2), Color(0xFFFFB8CE), Color(0xFF7A2E4C)),
    Triple(Color(0xFFD7E8FF), Color(0xFFB4D2FF), Color(0xFF29467A)),
    Triple(Color(0xFFDDF5D3), Color(0xFFBCE8AE), Color(0xFF2C5A2E)),
    Triple(Color(0xFFFFE9C7), Color(0xFFFFD49A), Color(0xFF6E4512)),
    Triple(Color(0xFFE8DEFF), Color(0xFFD0BEFF), Color(0xFF4A3480)),
    Triple(Color(0xFFD3F3EF), Color(0xFFAEE6DE), Color(0xFF1F5A54)),
)

/** Deterministic cute cover: pastel pattern (stripes / dots / waves) + title + game code. */
@Composable
fun GeneratedCover(model: CoverModel, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val seed = model.seed and 0x7FFFFFFF
    val (bg, fg, ink) = coverPalettes[(seed % coverPalettes.size).toInt()]
    val pattern = ((seed / 7) % 3).toInt()
    BoxWithConstraints(modifier.background(bg)) {
        val small = maxWidth < 120.dp
        Canvas(Modifier.fillMaxSize()) {
            when (pattern) {
                0 -> rotate(-24f) {
                    val step = 22.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(fg, Offset(x, -size.height), Offset(x, size.height * 2), strokeWidth = step * 0.45f)
                        x += step
                    }
                }
                1 -> {
                    val step = 18.dp.toPx()
                    var y = step / 2
                    var row = 0
                    while (y < size.height + step) {
                        var x = if (row % 2 == 0) step / 2 else step
                        while (x < size.width + step) {
                            drawCircle(fg, radius = step * 0.2f, center = Offset(x, y))
                            x += step
                        }
                        y += step * 0.87f
                        row++
                    }
                }
                else -> {
                    val amp = 6.dp.toPx()
                    val step = 20.dp.toPx()
                    var y = step / 2
                    while (y < size.height + step) {
                        val p = Path()
                        p.moveTo(0f, y)
                        var x = 0f
                        while (x < size.width) {
                            p.quadraticTo(x + step / 4, y - amp, x + step / 2, y)
                            p.quadraticTo(x + step * 3 / 4, y + amp, x + step, y)
                            x += step
                        }
                        drawPath(p, fg, style = Stroke(3.dp.toPx()))
                        y += step
                    }
                }
            }
            // Soft spotlight behind the title.
            drawCircle(Color.White.copy(alpha = 0.35f), radius = size.minDimension * 0.42f, center = Offset(size.width / 2, size.height * 0.46f))
        }
        if (showTitle) {
            Text(
                model.title,
                style = (if (small) RetraType.label else RetraType.headline).copy(fontSize = if (small) 13.sp else RetraType.headline.fontSize),
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = if (small) 8.dp else 16.dp),
            )
            if (model.gameCode.isNotBlank()) {
                Text(
                    model.gameCode,
                    style = RetraType.caption,
                    color = ink.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(Radius.pillShape)
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Suppress("unused")
private fun keep() = remember
