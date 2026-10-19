package app.retra.ui.store

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.core.AtomicFiles
import app.retra.data.CatalogEntry
import app.retra.data.CatalogSource
import app.retra.data.Catalog
import app.retra.data.GameSource
import app.retra.data.ImportOrigin
import app.retra.data.ImportResult
import app.retra.data.MessageTone
import app.retra.data.Provenance
import app.retra.patcher.Checksums
import app.retra.patcher.PatchResult
import app.retra.patcher.Patcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Progress of one catalog entry. */
sealed interface EntryJob {
    data object Idle : EntryJob
    data class Downloading(val progress: Float) : EntryJob
    data class Patching(val progress: Float) : EntryJob
    data class Failed(val message: String) : EntryJob
    data class Done(val gameId: String) : EntryJob
}

/** The base ROM the user picked for a patch (when it isn't already matched in the library). */
@Immutable
data class BaseCheck(val name: String, val crc32: String, val matches: Boolean, val gameId: String)

@Immutable
data class StoreState(
    val catalog: Catalog? = null,
    val source: CatalogSource = CatalogSource.BUNDLED,
    val refreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    /** catalog entry id -> library game id */
    val installed: Map<String, String> = emptyMap(),
    /** base crc32 -> owned game (id to title) */
    val ownedBases: Map<String, Pair<String, String>> = emptyMap(),
    val patchesReady: Set<String> = emptySet(),
    val jobs: Map<String, EntryJob> = emptyMap(),
    val pickedBases: Map<String, BaseCheck> = emptyMap(),
) {
    fun results(): List<CatalogEntry> {
        val q = query.trim()
        val entries = catalog?.entries ?: return emptyList()
        if (q.isEmpty()) return entries
        return entries.filter { e ->
            e.title.contains(q, true) || e.author.contains(q, true) || e.tags.any { it.contains(q, true) } ||
                e.base?.title?.contains(q, true) == true
        }
    }
}

class StoreViewModel(private val container: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val jobs = MutableStateFlow<Map<String, EntryJob>>(emptyMap())
    private val picked = MutableStateFlow<Map<String, BaseCheck>>(emptyMap())
    private val patchFiles = MutableStateFlow(scanPatches())
    private val running = HashMap<String, Job>()
    private val patchesDir get() = File(container.filesRoot, "patches")

    val state: StateFlow<StoreState> = combine(
        container.catalog.state, container.library.games, query, jobs, combine(picked, patchFiles) { a, b -> a to b },
    ) { cat, games, q, j, (p, ready) ->
        StoreState(
            catalog = cat.catalog,
            source = cat.source,
            refreshing = cat.refreshing,
            error = cat.error,
            query = q,
            installed = games.mapNotNull { g -> g.catalogId?.let { it to g.id } }.toMap(),
            ownedBases = games.associate { it.crc32.uppercase() to (it.id to it.title) },
            patchesReady = ready,
            jobs = j,
            pickedBases = p,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreState())

    private fun scanPatches(): Set<String> =
        File(container.filesRoot, "patches").listFiles()?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()

    fun setQuery(q: String) { query.value = q }
    fun refresh() = viewModelScope.launch { container.catalog.refresh() }

    private fun setJob(id: String, job: EntryJob) = jobs.update { it + (id to job) }

    fun cancel(id: String) {
        running.remove(id)?.cancel()
        setJob(id, EntryJob.Idle)
    }

    /** Homebrew ROM: download, verify, add to the library. Mochi Hop installs from the APK. */
    fun getRom(entry: CatalogEntry) {
        if (running[entry.id]?.isActive == true) return
        running[entry.id] = viewModelScope.launch {
            try {
                val bytes = if (entry.bundled) {
                    withContext(Dispatchers.IO) { container.appAsset(AppContainer.BUNDLED_ROM) }
                } else {
                    val d = entry.download ?: error("No download")
                    setJob(entry.id, EntryJob.Downloading(0f))
                    container.downloader.download(d, entry.id) { p -> setJob(entry.id, EntryJob.Downloading(p)) }
                }
                val result = container.library.addRom(
                    bytes, "${entry.id}.gba",
                    ImportOrigin(
                        if (entry.bundled) GameSource.BUNDLED else GameSource.DOWNLOADED, catalogId = entry.id, title = entry.title,
                        coverAsset = if (entry.bundled) AppContainer.BUNDLED_COVER else null, homebrew = true,
                    ),
                )
                when (result) {
                    is ImportResult.Added -> { setJob(entry.id, EntryJob.Done(result.game.id)); container.messages.show("${entry.title} is in your games", MessageTone.SUCCESS) }
                    is ImportResult.Duplicate -> setJob(entry.id, EntryJob.Done(result.game.id))
                    is ImportResult.Error -> setJob(entry.id, EntryJob.Failed(result.message))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                setJob(entry.id, EntryJob.Failed(friendly(e)))
            }
        }
    }

    fun patchFile(entry: CatalogEntry): File? = patchesDir.listFiles()?.firstOrNull { it.nameWithoutExtension == entry.id }

    /** Downloads (and unzips) a fan-game patch, verifying size + SHA-256. */
    fun downloadPatch(entry: CatalogEntry) {
        val d = entry.download ?: return
        if (running[entry.id]?.isActive == true) return
        running[entry.id] = viewModelScope.launch {
            try {
                setJob(entry.id, EntryJob.Downloading(0f))
                val bytes = container.downloader.download(d, entry.id) { p -> setJob(entry.id, EntryJob.Downloading(p)) }
                if (Patcher.detect(bytes) == null) error("The downloaded file isn't a patch Retra can read.")
                val ext = (d.archiveEntry?.substringAfterLast('.') ?: d.format).lowercase()
                withContext(Dispatchers.IO) { AtomicFiles.write(File(patchesDir, "${entry.id}.$ext"), bytes) }
                patchFiles.value = scanPatches()
                setJob(entry.id, EntryJob.Idle)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                setJob(entry.id, EntryJob.Failed(friendly(e)))
            }
        }
    }

    /** The user picked their own base ROM for [entry]; add it to the library and check its CRC. */
    fun pickBase(entry: CatalogEntry, uri: Uri) = viewModelScope.launch {
        val outcome = runCatching {
            val name = container.importer.displayName(uri)
            val bytes = container.importer.readBytes(uri)
            when (val r = container.library.import(bytes, name)) {
                is ImportResult.Added -> r.game
                is ImportResult.Duplicate -> r.game
                is ImportResult.Error -> error(r.message)
            }
        }
        outcome.onSuccess { game ->
            val expected = entry.base?.crc32?.uppercase() ?: ""
            picked.update { it + (entry.id to BaseCheck(game.title, game.crc32, game.crc32.equals(expected, true), game.id)) }
        }.onFailure { container.messages.show(it.message ?: "Couldn't read that file.", MessageTone.ERROR) }
    }

    /** Applies the downloaded patch to the user's base ROM and adds the result as a new game. */
    fun makeGame(entry: CatalogEntry, baseGameId: String, force: Boolean) {
        val patch = patchFile(entry) ?: return
        if (running[entry.id]?.isActive == true) return
        running[entry.id] = viewModelScope.launch {
            setJob(entry.id, EntryJob.Patching(0f))
            try {
                val base = container.library.readRom(baseGameId) ?: error("The base game file is missing.")
                val baseGame = container.library.game(baseGameId)
                val patchBytes = withContext(Dispatchers.IO) { patch.readBytes() }
                val result = withContext(Dispatchers.Default) {
                    Patcher.apply(base, patchBytes, strict = !force) { p -> setJob(entry.id, EntryJob.Patching(p)) }
                }
                when (result) {
                    is PatchResult.Error -> setJob(entry.id, EntryJob.Failed(result.message))
                    is PatchResult.Ok -> {
                        val add = container.library.addRom(
                            result.output, "${entry.title}.gba",
                            ImportOrigin(
                                GameSource.PATCHED, catalogId = entry.id, title = entry.title,
                                provenance = Provenance(
                                    patchName = entry.title + (entry.version?.let { " $it" } ?: ""),
                                    patchFormat = patch.extension,
                                    baseTitle = baseGame?.title ?: entry.base?.title ?: "Base game",
                                    baseCrc32 = Checksums.crc32Hex(base),
                                    catalogId = entry.id,
                                    appliedAt = System.currentTimeMillis(),
                                    forced = force,
                                ),
                            ),
                        )
                        when (add) {
                            is ImportResult.Added -> setJob(entry.id, EntryJob.Done(add.game.id))
                            is ImportResult.Duplicate -> setJob(entry.id, EntryJob.Done(add.game.id))
                            is ImportResult.Error -> setJob(entry.id, EntryJob.Failed(add.message))
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                setJob(entry.id, EntryJob.Failed(friendly(e)))
            }
        }
    }

    fun resetJob(id: String) = setJob(id, EntryJob.Idle)

    private fun friendly(e: Exception): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException -> "You're offline. Connect to the internet and try again."
        is java.net.SocketTimeoutException -> "The server took too long to answer. Try again."
        else -> e.message ?: "Something went wrong. Try again."
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { StoreViewModel(c) } }
    }
}
