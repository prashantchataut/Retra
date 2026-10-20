package app.retra

import android.app.Application
import android.content.Context
import app.retra.data.BackupManager
import app.retra.data.BoxArtRepository
import app.retra.data.CheatDatabase
import app.retra.data.GameIndex
import app.retra.ui.CoverArt
import kotlinx.coroutines.flow.distinctUntilChangedBy
import app.retra.data.BiosRepository
import app.retra.data.CatalogRepository
import app.retra.data.CheatRepository
import app.retra.data.Downloader
import app.retra.data.GameSource
import app.retra.data.ImportOrigin
import app.retra.data.ImportResult
import app.retra.data.IncomingFiles
import app.retra.data.LibraryRepository
import app.retra.data.Messages
import app.retra.data.Originals
import app.retra.data.SaveRepository
import app.retra.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Manual dependency container; one per process. */
class AppContainer(private val context: Context) {
    val filesRoot: File = context.filesDir
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(filesRoot)
    val downloader = Downloader(File(context.cacheDir, "downloads"))
    val catalog = CatalogRepository(
        cacheFile = File(File(filesRoot, "catalog"), "manifest.json"),
        bundled = { context.assets.open("catalog/manifest.json").bufferedReader().use { it.readText() } },
        fetch = { downloader.fetchText(CATALOG_URL) },
    )
    val library = LibraryRepository(filesRoot, homebrewLookup = { sha -> catalog.homebrewBySha256(sha) })
    val saves = SaveRepository(filesRoot)
    val cheats = CheatRepository(filesRoot)
    val bios = BiosRepository(filesRoot)
    val backups = BackupManager(filesRoot)
    val messages = Messages()
    val incoming = IncomingFiles()
    val importer = app.retra.data.ImportController(context, library, messages)
    val gameIndex = GameIndex { runCatching { context.assets.open("metadata/gba_games.tsv.gz") }.getOrNull() }
    val cheatBooks = CheatDatabase { runCatching { context.assets.open("metadata/gba_cheats.json.gz") }.getOrNull() }
    val boxArt = BoxArtRepository(filesRoot, gameIndex, fetch = { url -> downloader.fetchBytes(url) })

    init {
        CoverArt.init(context, filesRoot)
    }

    fun start() {
        scope.launch {
            catalog.loadLocal()
            installOriginals()
            catalog.refresh()
        }
        // Box art for newly added cartridges (only reacts when the set of games changes).
        scope.launch {
            library.games.distinctUntilChangedBy { list -> list.map { it.id } }.collect { list ->
                if (!settings.current.downloadCoverArt) return@collect
                list.forEach { g ->
                    if (runCatching { boxArt.fetch(g) }.getOrDefault(false)) {
                        library.update(g.id) { it.copy(artAt = System.currentTimeMillis()) }
                    }
                }
            }
        }
    }

    /** The built-in cheat book for [game], if Retra knows one. */
    fun cheatBook(game: app.retra.data.Game?): app.retra.ui.games.CheatBook? {
        game ?: return null
        val found = cheatBooks.lookup(game) ?: return null
        return app.retra.ui.games.CheatBook(game.title, found.cheats)
    }

    /** Retra Originals ship in the APK; each is added once (the user may delete it later). */
    private suspend fun installOriginals() {
        val st = settings.current
        val done = st.installedOriginals.toMutableSet()
        if (st.bundledInstalled) done += "mochi-hop"
        for (o in Originals.all) {
            if (o.catalogId in done) continue
            val rom = runCatching { context.assets.open(o.romAsset).use { it.readBytes() } }.getOrNull() ?: continue
            val result = library.addRom(
                rom, o.romAsset.substringAfterLast('/'),
                ImportOrigin(GameSource.BUNDLED, catalogId = o.catalogId, title = o.title, coverAsset = o.coverAsset, homebrew = true),
            )
            if (result is ImportResult.Added || result is ImportResult.Duplicate) done += o.catalogId
        }
        if (done.toList() != st.installedOriginals) settings.update { it.copy(installedOriginals = done.toList(), bundledInstalled = true) }
    }

    fun appAsset(path: String): ByteArray = context.assets.open(path).use { it.readBytes() }

    /** Deletes a game and everything that belongs to it. */
    suspend fun deleteGame(id: String) {
        library.delete(id)
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            saves.deleteAll(id)
            cheats.delete(id)
        }
    }

    companion object {
        const val CATALOG_URL = "https://raw.githubusercontent.com/prashantchataut/Retra/main/catalog/manifest.json"
    }
}

class RetraApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

val Context.container: AppContainer get() = (applicationContext as RetraApp).container
