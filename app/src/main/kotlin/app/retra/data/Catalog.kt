package app.retra.data

import app.retra.core.RetraJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CatalogDownload(
    val url: String,
    val size: Long,
    val sha256: String,
    val format: String,
    val archiveEntry: String? = null,
    val archiveEntrySize: Long? = null,
    val archiveEntrySha256: String? = null,
)

@Serializable
data class CatalogBase(
    val title: String,
    val gameCode: String,
    val crc32: String,
    val size: Long,
)

@Serializable
data class CatalogEntry(
    val id: String,
    val kind: String,
    val title: String,
    val author: String,
    val version: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "complete",
    val license: String? = null,
    val homepage: String,
    val source: String? = null,
    val download: CatalogDownload? = null,
    val patchFormat: String? = null,
    val base: CatalogBase? = null,
    val accent: String = "#FFD6E0",
    val featured: Boolean = false,
    val bundled: Boolean = false,
) {
    val isRom get() = kind == "rom"
    val isPatch get() = kind == "patch"

    /** Accent as ARGB int (falls back to Mochi pink). */
    val accentArgb: Int
        get() = runCatching { (0xFF000000 or accent.removePrefix("#").toLong(16)).toInt() }.getOrDefault(0xFFFFD6E0.toInt())

    /** The app can't apply some formats from the catalog directly (e.g. unknown). */
    val formatLabel: String
        get() = when (patchFormat?.lowercase()) {
            "ips" -> "IPS"; "ups" -> "UPS"; "bps" -> "BPS"; "xdelta" -> "xdelta"; null -> "Unknown"; else -> patchFormat.uppercase()
        }
}

@Serializable
data class CatalogCollection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val entryIds: List<String> = emptyList(),
)

@Serializable
data class Catalog(
    val schema: Int,
    val updated: String = "",
    val collections: List<CatalogCollection> = emptyList(),
    val entries: List<CatalogEntry> = emptyList(),
) {
    private val byId by lazy { entries.associateBy { it.id } }
    fun entry(id: String): CatalogEntry? = byId[id]
    fun shelf(collection: CatalogCollection): List<CatalogEntry> = collection.entryIds.mapNotNull { byId[it] }
}

object CatalogParser {
    const val SUPPORTED_SCHEMA = 1

    sealed interface Result {
        data class Ok(val catalog: Catalog) : Result
        data class Error(val message: String) : Result
    }

    fun parse(json: String): Result {
        val catalog = try {
            RetraJson.decodeFromString(Catalog.serializer(), json)
        } catch (e: Exception) {
            return Result.Error("The game library list couldn't be read.")
        }
        if (catalog.schema > SUPPORTED_SCHEMA) return Result.Error("Update Retra to see the newest game library.")
        // Drop entries that would break content rules or the UI.
        val clean = catalog.entries.filter { e ->
            (e.isRom || e.isPatch) && e.homepage.startsWith("https://") &&
                (e.download == null || e.download.url.startsWith("https://")) &&
                (e.isRom || e.base != null)
        }
        return Result.Ok(catalog.copy(entries = clean))
    }
}

enum class CatalogSource { BUNDLED, CACHED, ONLINE }

data class CatalogState(
    val catalog: Catalog? = null,
    val source: CatalogSource = CatalogSource.BUNDLED,
    val refreshing: Boolean = false,
    val error: String? = null,
)

/**
 * Online Game Library. Starts from the cached copy (or the bundled one), then refreshes from GitHub.
 */
class CatalogRepository(
    private val cacheFile: File,
    private val bundled: () -> String,
    private val fetch: suspend () -> String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    val current: Catalog? get() = _state.value.catalog

    /** Loads cache/bundled synchronously-ish (call once at startup on IO). */
    suspend fun loadLocal() = withContext(io) {
        if (_state.value.catalog != null) return@withContext
        val cached = runCatching { cacheFile.takeIf { it.exists() }?.readText() }.getOrNull()
            ?.let { CatalogParser.parse(it) as? CatalogParser.Result.Ok }
        val bundledCatalog = (runCatching { CatalogParser.parse(bundled()) }.getOrNull() as? CatalogParser.Result.Ok)?.catalog
        // Prefer the cache unless the bundled copy is newer (app update).
        val pick = when {
            cached != null && bundledCatalog != null && bundledCatalog.updated > cached.catalog.updated -> bundledCatalog to CatalogSource.BUNDLED
            cached != null -> cached.catalog to CatalogSource.CACHED
            else -> bundledCatalog to CatalogSource.BUNDLED
        }
        _state.update { it.copy(catalog = pick.first, source = pick.second) }
    }

    suspend fun refresh() = withContext(io) {
        loadLocal()
        _state.update { it.copy(refreshing = true, error = null) }
        val result = runCatching { CatalogParser.parse(fetch()) }
        _state.update { s ->
            when (val r = result.getOrNull()) {
                is CatalogParser.Result.Ok -> {
                    val newer = s.catalog == null || r.catalog.updated >= s.catalog.updated
                    if (newer) runCatching { app.retra.core.AtomicFiles.writeText(cacheFile, RetraJson.encodeToString(Catalog.serializer(), r.catalog)) }
                    if (newer) s.copy(catalog = r.catalog, source = CatalogSource.ONLINE, refreshing = false)
                    else s.copy(refreshing = false)
                }
                is CatalogParser.Result.Error -> s.copy(refreshing = false, error = r.message)
                null -> s.copy(refreshing = false, error = "You're offline, so this is the saved list.")
            }
        }
    }

    fun homebrewBySha256(sha256: String): String? =
        current?.entries?.firstOrNull { it.isRom && (it.download?.sha256 == sha256 || it.download?.archiveEntrySha256 == sha256) }?.id
}
