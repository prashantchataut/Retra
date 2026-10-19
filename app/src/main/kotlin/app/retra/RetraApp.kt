package app.retra

import android.app.Application
import android.content.Context
import app.retra.data.BackupManager
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

    fun start() {
        scope.launch {
            catalog.loadLocal()
            installBundledGame()
            catalog.refresh()
        }
    }

    /** Mochi Hop ships in the APK; add it once on first launch (the user may delete it later). */
    private suspend fun installBundledGame() {
        if (settings.current.bundledInstalled) return
        val rom = runCatching { context.assets.open(BUNDLED_ROM).use { it.readBytes() } }.getOrNull() ?: return
        val result = library.addRom(
            rom, "mochi_hop.gba",
            ImportOrigin(GameSource.BUNDLED, catalogId = "mochi-hop", title = "Mochi Hop", coverAsset = BUNDLED_COVER, homebrew = true),
        )
        if (result is ImportResult.Added || result is ImportResult.Duplicate) settings.update { it.copy(bundledInstalled = true) }
    }

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
        const val BUNDLED_ROM = "games/mochi_hop.gba"
        const val BUNDLED_COVER = "games/mochi_hop_cover.png"
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
