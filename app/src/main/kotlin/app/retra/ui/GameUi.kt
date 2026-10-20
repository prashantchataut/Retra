package app.retra.ui

import androidx.compose.runtime.Immutable
import app.retra.data.CatalogEntry
import app.retra.data.Game
import app.retra.data.SaveRepository
import app.retra.ui.components.CoverModel

/** A game plus what the UI needs to draw it (resolved off the main thread). */
@Immutable
data class GameUi(
    val game: Game,
    val cover: CoverModel,
    val hasAutoState: Boolean,
    val lastScreenPath: String?,
) {
    val id get() = game.id
}

fun Game.coverModel(lastScreen: java.io.File?): CoverModel = CoverModel(
    title = title,
    gameCode = gameCode,
    seed = crc32.toLongOrNull(16) ?: title.hashCode().toLong(),
    assetPath = coverAsset,
    filePath = if (coverAsset == null) lastScreen?.takeIf { it.exists() }?.absolutePath else null,
    fileStamp = lastScreen?.lastModified() ?: 0,
)

fun Game.toUi(saves: SaveRepository): GameUi {
    val shot = saves.lastScreenFile(id).takeIf { it.exists() }
    return GameUi(this, coverModel(shot), saves.hasAutoState(id), shot?.absolutePath)
}

fun CatalogEntry.coverModel(): CoverModel = CoverModel(
    title = title,
    gameCode = base?.gameCode ?: "",
    seed = id.hashCode().toLong(),
    assetPath = if (id == "mochi-hop") "games/mochi_hop_cover.png" else null,
    accent = accentArgb,
)

/** Time-of-day greeting. */
fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..22 -> "Good evening"
    else -> "Up late?"
}

fun relativeTime(then: Long, now: Long = System.currentTimeMillis()): String {
    val d = (now - then).coerceAtLeast(0)
    val min = d / 60_000
    return when {
        min < 1 -> "just now"
        min < 60 -> "$min min ago"
        min < 60 * 24 -> "${min / 60} h ago"
        min < 60 * 24 * 2 -> "yesterday"
        min < 60 * 24 * 30 -> "${min / (60 * 24)} days ago"
        else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(then))
    }
}
