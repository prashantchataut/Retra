package app.retra.data

import app.retra.core.AtomicFiles
import app.retra.core.GameClassifier
import app.retra.core.RetraJson
import app.retra.core.RomHeader
import app.retra.core.RomImport
import app.retra.patcher.Checksums
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

sealed interface ImportResult {
    data class Added(val game: Game) : ImportResult
    data class Duplicate(val game: Game) : ImportResult
    data class Error(val message: String) : ImportResult
}

/** Extra facts about a ROM being added (where it came from, catalog link, provenance). */
data class ImportOrigin(
    val source: GameSource = GameSource.IMPORTED,
    val catalogId: String? = null,
    val title: String? = null,
    val provenance: Provenance? = null,
    val coverAsset: String? = null,
    val homebrew: Boolean = false,
)

/**
 * The user's games: ROM files in `games/<id>.gba` and metadata in `library.json`.
 * Game ids are the first 16 hex chars of the ROM's SHA-1, so importing the same ROM twice is a no-op.
 */
class LibraryRepository(
    private val root: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Looks up catalog homebrew by sha256, so known homebrew gets the right category. */
    private val homebrewLookup: (sha256: String) -> String? = { null },
) {
    private val libraryFile = File(root, "library.json")
    private val gamesDir = File(root, "games")
    private val mutex = Mutex()
    private val serializer = ListSerializer(Game.serializer())
    private val _games = MutableStateFlow(load())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private fun load(): List<Game> = runCatching {
        if (libraryFile.exists()) RetraJson.decodeFromString(serializer, libraryFile.readText()) else emptyList()
    }.getOrElse {
        // Keep a copy of an unreadable library instead of silently overwriting it.
        runCatching { libraryFile.copyTo(File(root, "library.corrupt.json"), overwrite = true) }
        emptyList()
    }

    private fun persist(list: List<Game>) {
        AtomicFiles.writeText(libraryFile, RetraJson.encodeToString(serializer, list))
        _games.value = list
    }

    fun game(id: String): Game? = _games.value.firstOrNull { it.id == id }

    fun romFile(id: String) = File(gamesDir, "$id.gba")

    suspend fun readRom(id: String): ByteArray? = withContext(io) {
        romFile(id).takeIf { it.exists() }?.readBytes()
    }

    /** Imports a ROM or a zip containing one. */
    suspend fun import(bytes: ByteArray, fileName: String, origin: ImportOrigin = ImportOrigin()): ImportResult = withContext(io) {
        when (val r = RomImport.extract(bytes, fileName)) {
            is RomImport.Result.Error -> ImportResult.Error(r.message)
            is RomImport.Result.Rom -> addRom(r.bytes, r.name, origin)
        }
    }

    suspend fun addRom(rom: ByteArray, fileName: String, origin: ImportOrigin = ImportOrigin()): ImportResult = withContext(io) {
        if (rom.size > RomHeader.MAX_SIZE) return@withContext ImportResult.Error("This file is larger than 32 MB, so it can't be a GBA game.")
        val header = RomHeader.parse(rom)
            ?: return@withContext ImportResult.Error("This file is too small to be a GBA game.")
        if (!header.looksValid && !fileName.lowercase().endsWith(".gba") && !fileName.lowercase().endsWith(".agb")) {
            return@withContext ImportResult.Error("This doesn't look like a GBA game. Retra opens .gba files (or a .zip with one inside).")
        }
        val sha1 = Checksums.sha1Hex(rom)
        val id = sha1.substring(0, 16)
        mutex.withLock {
            _games.value.firstOrNull { it.id == id }?.let { return@withContext ImportResult.Duplicate(it) }
            val catalogHomebrew = if (origin.catalogId == null) homebrewLookup(Checksums.sha256Hex(rom)) else null
            val homebrew = origin.homebrew || catalogHomebrew != null
            val game = Game(
                id = id,
                title = origin.title ?: GameClassifier.prettyTitle(header.title, fileName),
                romTitle = header.title,
                gameCode = header.gameCode,
                makerCode = header.makerCode,
                romVersion = header.version,
                sizeBytes = rom.size.toLong(),
                crc32 = Checksums.crc32Hex(rom),
                sha1 = sha1,
                addedAt = clock(),
                source = origin.source,
                category = GameClassifier.classify(header.gameCode, header.title, origin.source, homebrew),
                catalogId = origin.catalogId ?: catalogHomebrew,
                provenance = origin.provenance,
                coverAsset = origin.coverAsset,
            )
            AtomicFiles.write(romFile(id), rom)
            persist(_games.value + game)
            ImportResult.Added(game)
        }
    }

    suspend fun update(id: String, transform: (Game) -> Game) = withContext(io) {
        mutex.withLock {
            val list = _games.value
            if (list.none { it.id == id }) return@withLock
            persist(list.map { if (it.id == id) transform(it) else it })
        }
    }

    suspend fun rename(id: String, title: String) {
        val t = title.trim()
        if (t.isNotEmpty()) update(id) { it.copy(title = t.take(80)) }
    }

    suspend fun setFavorite(id: String, favorite: Boolean) = update(id) { it.copy(favorite = favorite) }

    suspend fun recordSession(id: String, playedMs: Long) = update(id) {
        it.copy(playTimeMs = it.playTimeMs + playedMs.coerceAtLeast(0), lastPlayedAt = clock())
    }

    suspend fun recordRomInfo(id: String, saveType: String, hasRtc: Boolean) = update(id) {
        if (it.saveType == saveType && it.hasRtc == hasRtc) it else it.copy(saveType = saveType, hasRtc = hasRtc)
    }

    suspend fun delete(id: String) = withContext(io) {
        mutex.withLock {
            romFile(id).delete()
            persist(_games.value.filterNot { it.id == id })
        }
    }

    fun findByCrc(crc32: String): Game? = _games.value.firstOrNull { it.crc32.equals(crc32, ignoreCase = true) }

    fun findByCatalogId(catalogId: String): Game? = _games.value.firstOrNull { it.catalogId == catalogId }

    fun storageBytes(): Long = gamesDir.listFiles()?.sumOf { it.length() } ?: 0L
}
