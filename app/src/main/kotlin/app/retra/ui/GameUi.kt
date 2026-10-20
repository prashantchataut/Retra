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

/**
 * Cover art Retra knows about besides a game's own bundled art: covers made for catalog entries
 * (assets/covers/<catalog id>.png) and box art fetched for imported cartridges.
 */
object CoverArt {
    @Volatile private var catalogCovers: Set<String> = emptySet()
    @Volatile private var boxArtDir: java.io.File? = null

    fun init(context: android.content.Context, filesRoot: java.io.File) {
        catalogCovers = runCatching { context.assets.list("covers")?.map { it.removeSuffix(".png") }?.toSet() }.getOrNull() ?: emptySet()
        boxArtDir = java.io.File(filesRoot, "boxart")
    }

    fun catalogAsset(catalogId: String?): String? =
        catalogId?.let { id -> Originals.byCatalogId(id)?.coverAsset ?: id.takeIf { it in catalogCovers }?.let { "covers/$it.png" } }

    fun boxArt(crc32: String): java.io.File? = boxArtDir?.let { java.io.File(it, crc32.uppercase() + ".png") }?.takeIf { it.exists() }
}

fun Game.coverModel(lastScreen: java.io.File?): CoverModel {
    val asset = coverAsset ?: CoverArt.catalogAsset(catalogId)
    val box = if (asset == null) CoverArt.boxArt(crc32) else null
    val file = box ?: if (asset == null) lastScreen?.takeIf { it.exists() } else null
    return CoverModel(
        title = title,
        gameCode = gameCode,
        seed = crc32.toLongOrNull(16) ?: title.hashCode().toLong(),
        assetPath = asset,
        filePath = file?.absolutePath,
        fileStamp = file?.lastModified() ?: 0,
        boxArt = box != null,
    )
}

fun Game.toUi(saves: SaveRepository): GameUi {
    val shot = saves.lastScreenFile(id).takeIf { it.exists() }
    return GameUi(this, coverModel(shot), saves.hasAutoState(id), shot?.absolutePath)
}

fun CatalogEntry.coverModel(): CoverModel = CoverModel(
    title = title,
    gameCode = base?.gameCode ?: "",
    seed = id.hashCode().toLong(),
    assetPath = CoverArt.catalogAsset(id),
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
