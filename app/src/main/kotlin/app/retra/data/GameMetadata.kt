package app.retra.data

import app.retra.core.AtomicFiles
import app.retra.core.RetraJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

/**
 * Bundled No-Intro index for GBA ROMs (assets/metadata/gba_games.tsv.gz, derived from
 * libretro-database, CC BY-SA 4.0): CRC32 -> cartridge serial + the exact name libretro uses.
 */
class GameIndex(private val open: () -> InputStream?) {
    data class Entry(val crc32: String, val serial: String, val name: String)

    private val byCrc: Map<String, Entry> by lazy {
        runCatching {
            open()?.let { raw ->
                GZIPInputStream(raw).bufferedReader().useLines { lines ->
                    lines.drop(1).mapNotNull { line ->
                        val p = line.split('\t')
                        if (p.size >= 3 && p[0].length == 8) Entry(p[0].uppercase(), p[1], p[2]) else null
                    }.associateBy { it.crc32 }
                }
            }
        }.getOrNull() ?: emptyMap()
    }

    fun byCrc(crc32: String): Entry? = byCrc[crc32.uppercase()]
}

/**
 * Box art for games the user imported, fetched from libretro-thumbnails by No-Intro name and kept in
 * `boxart/<CRC32>.png`. Nothing is bundled; homebrew and patched games use Retra's own covers.
 */
class BoxArtRepository(
    root: File,
    private val index: GameIndex,
    private val fetch: suspend (String) -> ByteArray,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dir = File(root, "boxart")
    private val missesFile = File(dir, "misses.tsv")

    fun file(crc32: String) = File(dir, crc32.uppercase() + ".png")

    fun existing(crc32: String): File? = file(crc32).takeIf { it.exists() }

    /** Fetches art for [game] when it's a known cartridge; true when a new image was saved. */
    suspend fun fetch(game: Game): Boolean = withContext(Dispatchers.IO) {
        val crc = game.crc32.uppercase()
        if (game.source == GameSource.PATCHED || existing(crc) != null) return@withContext false
        val name = index.byCrc(crc)?.name ?: return@withContext false
        if (recentlyMissed(crc)) return@withContext false
        for (url in urls(name)) {
            val bytes = runCatching { fetch(url) }.getOrNull() ?: continue
            if (!isPng(bytes)) continue
            AtomicFiles.write(file(crc), bytes)
            return@withContext true
        }
        recordMiss(crc)
        false
    }

    private fun misses(): Map<String, Long> = runCatching {
        missesFile.readLines().mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to (it[1].toLongOrNull() ?: 0L) } }.toMap()
    }.getOrDefault(emptyMap())

    private fun recentlyMissed(crc: String): Boolean {
        val at = misses()[crc] ?: return false
        return clock() - at < RETRY_AFTER_MS
    }

    private fun recordMiss(crc: String) {
        val all = misses() + (crc to clock())
        AtomicFiles.writeText(missesFile, all.entries.joinToString("\n") { "${it.key}\t${it.value}" })
    }

    companion object {
        private const val RETRY_AFTER_MS = 14L * 24 * 60 * 60 * 1000

        /** libretro replaces these characters with '_' in thumbnail file names. */
        fun thumbnailName(name: String): String = name.replace(Regex("[&*/:`<>?\\\\|\"]"), "_")

        fun urls(name: String): List<String> {
            val enc = URLEncoder.encode(thumbnailName(name), "UTF-8").replace("+", "%20")
            return listOf(
                "https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/$enc.png",
                "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy_Advance/master/Named_Boxarts/$enc.png",
            )
        }

        private fun isPng(b: ByteArray) = b.size > 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte()
    }
}

@Serializable
data class CheatPack(val schema: Int = 1, val source: String = "", val license: String = "", val games: List<CheatPackGame> = emptyList())

@Serializable
data class CheatPackGame(
    val title: String,
    val crc32s: List<String> = emptyList(),
    val serials: List<String> = emptyList(),
    val catalogIds: List<String> = emptyList(),
    val cheats: List<CheatPackCheat> = emptyList(),
)

/** A ready-made code. [master] codes (enablers) must stay on for the others to work. */
@Serializable
data class CheatPackCheat(val name: String, val code: String, val master: Boolean = false)

/**
 * Built-in cheat lists (assets/metadata/gba_cheats.json.gz, converted from libretro-database's cheat
 * files, CC BY-SA 4.0). Patched fan games match only by catalog id: they share their base game's
 * serial, but its RAM addresses rarely carry over.
 */
class CheatDatabase(private val open: () -> InputStream?) {
    private val pack: CheatPack by lazy {
        runCatching {
            open()?.let { raw -> RetraJson.decodeFromString(CheatPack.serializer(), GZIPInputStream(raw).bufferedReader().use { it.readText() }) }
        }.getOrNull() ?: CheatPack()
    }
    private val byCrc by lazy { pack.games.flatMap { g -> g.crc32s.map { it.uppercase() to g } }.toMap() }
    private val bySerial by lazy { pack.games.flatMap { g -> g.serials.map { it.uppercase() to g } }.groupBy({ it.first }, { it.second }) }
    private val byCatalog by lazy { pack.games.flatMap { g -> g.catalogIds.map { it to g } }.toMap() }

    val source: String get() = pack.source

    fun lookup(game: Game): CheatPackGame? {
        game.catalogId?.let { id -> byCatalog[id]?.let { return it } }
        if (game.source == GameSource.PATCHED) return null
        byCrc[game.crc32.uppercase()]?.let { return it }
        // Several files can share a serial (regions/revisions); prefer the biggest list.
        return bySerial[game.gameCode.uppercase()]?.maxByOrNull { it.cheats.size }
    }
}
