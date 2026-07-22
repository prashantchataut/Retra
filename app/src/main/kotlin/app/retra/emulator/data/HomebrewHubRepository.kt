package app.retra.emulator.data

import android.content.Context
import app.retra.core.rom.GbaRomParser
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class HomebrewHubFile(
    val filename: String,
    val playable: Boolean,
    val isDefault: Boolean
)

data class HomebrewHubEntry(
    val slug: String,
    val title: String,
    val developer: String,
    val license: String,
    val platform: String,
    val typeTag: String,
    val repository: String?,
    val screenshots: List<String>,
    val tags: List<String>,
    val files: List<HomebrewHubFile>
) {
    val defaultPlayableGba: HomebrewHubFile?
        get() = files.firstOrNull { it.playable && it.isDefault && it.filename.endsWith(".gba", true) }
            ?: files.firstOrNull { it.playable && it.filename.endsWith(".gba", true) }

    val directInstallEligible: Boolean
        get() = defaultPlayableGba != null &&
            typeTag.lowercase() in setOf("game", "homebrew", "demo", "music") &&
            license.isNotBlank() && !license.equals("unknown", true)

    fun sourcePageUrl(): String = "${HomebrewHubRepository.WEB_BASE}/game/${encodePath(slug)}/"
    fun screenshotUrl(file: String): String = "${HomebrewHubRepository.API_BASE}/entry/${encodePath(slug)}/${encodePath(file)}"

    companion object {
        private fun encodePath(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

data class HomebrewHubPage(
    val entries: List<HomebrewHubEntry>,
    val resultCount: Int,
    val pageCurrent: Int,
    val pageTotal: Int,
    val pageElements: Int
)

data class HomebrewHubState(
    val loading: Boolean = false,
    val query: String = "",
    val page: HomebrewHubPage = HomebrewHubPage(emptyList(), 0, 1, 1, 10),
    val installingSlug: String? = null,
    val error: String? = null,
    val lastUpdatedAtEpochMillis: Long? = null
)

sealed interface HomebrewInstallOutcome {
    data class Imported(val game: app.retra.core.model.GameRecord) : HomebrewInstallOutcome
    data class Duplicate(val title: String) : HomebrewInstallOutcome
    data class Rejected(val reason: String) : HomebrewInstallOutcome
}

@Singleton
class HomebrewHubRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
    private val artworkRepository: ArtworkRepository
) {
    private val mutableState = MutableStateFlow(HomebrewHubState())
    val state: StateFlow<HomebrewHubState> = mutableState

    suspend fun refresh(query: String = mutableState.value.query, page: Int = 1) = withContext(Dispatchers.IO) {
        val safeQuery = query.trim().take(120)
        mutableState.value = mutableState.value.copy(loading = true, query = safeQuery, error = null)
        val updated = runCatching {
            val endpoint = buildSearchUrl(safeQuery, page.coerceAtLeast(1))
            val bytes = get(endpoint, MAX_JSON_BYTES, "application/json")
            val parsed = parsePage(bytes)
            mutableState.value.copy(
                loading = false,
                query = safeQuery,
                page = parsed,
                error = null,
                lastUpdatedAtEpochMillis = System.currentTimeMillis()
            )
        }.getOrElse { error ->
            mutableState.value.copy(
                loading = false,
                query = safeQuery,
                error = error.message ?: "Homebrew Hub could not be loaded."
            )
        }
        mutableState.value = updated
    }

    suspend fun loadPreview(entry: HomebrewHubEntry): ByteArray? = withContext(Dispatchers.IO) {
        val screenshot = entry.screenshots.firstOrNull() ?: return@withContext null
        val url = entry.screenshotUrl(screenshot)
        val key = Sha256.of(url.toByteArray(StandardCharsets.UTF_8))
        val directory = File(context.cacheDir, "homebrew-previews").apply { mkdirs() }
        val cached = File(directory, "$key.img")
        if (cached.isFile && cached.length() in 1..MAX_ARTWORK_BYTES.toLong()) {
            return@withContext runCatching { cached.readBytes() }.getOrNull()
        }
        runCatching {
            val bytes = get(url, MAX_ARTWORK_BYTES, "image/avif,image/webp,image/png,image/jpeg")
            require(bytes.isNotEmpty()) { "Homebrew preview was empty." }
            val temporary = File(directory, ".$key.tmp")
            try {
                FileOutputStream(temporary).use { output ->
                    output.write(bytes)
                    output.fd.sync()
                }
                try {
                    Files.move(
                        temporary.toPath(),
                        cached.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: Exception) {
                    Files.move(temporary.toPath(), cached.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                if (temporary.exists()) temporary.delete()
            }
            bytes
        }.getOrNull()
    }

    suspend fun install(entry: HomebrewHubEntry): HomebrewInstallOutcome = withContext(Dispatchers.IO) {
        val file = entry.defaultPlayableGba
            ?: return@withContext HomebrewInstallOutcome.Rejected("This Homebrew Hub entry does not expose a playable GBA file.")
        if (entry.platform != "GBA") {
            return@withContext HomebrewInstallOutcome.Rejected("Retra 2 currently installs GBA releases only.")
        }
        if (!entry.directInstallEligible) {
            return@withContext HomebrewInstallOutcome.Rejected(
                "Retra only installs creator-published homebrew, games, demos, or music with usable license metadata. Hack-ROM entries remain source-page only."
            )
        }
        mutableState.value = mutableState.value.copy(installingSlug = entry.slug, error = null)
        try {
            val url = "$API_BASE/entry/${encodePath(entry.slug)}/${encodePath(file.filename)}"
            val bytes = get(url, GbaRomParser.MAX_ROM_SIZE_BYTES, "application/octet-stream")
            val sha256 = Sha256.of(bytes)
            when (
                val outcome = gameRepository.importGbaBytes(
                    bytes = bytes,
                    displayName = file.filename,
                    origin = "HOMEBREW_HUB",
                    creator = entry.developer,
                    sourceUrl = entry.sourcePageUrl(),
                    license = entry.license,
                    distributionPermission = "Playable file served by Homebrew Hub for a non-hack-ROM entry with declared license metadata; Retra recorded local SHA-256 $sha256."
                )
            ) {
                is ImportOutcome.Imported -> {
                    val withArtwork = entry.screenshots.firstOrNull()?.let { screenshot ->
                        runCatching {
                            val artworkBytes = get(
                                entry.screenshotUrl(screenshot),
                                MAX_ARTWORK_BYTES,
                                "image/avif,image/webp,image/png,image/jpeg"
                            )
                            val path = artworkRepository.importCoverArtBytes(
                                outcome.game.id,
                                outcome.game.sha256,
                                artworkBytes
                            ).getOrThrow()
                            outcome.game.copy(coverArtPath = path)
                        }.getOrNull()
                    } ?: outcome.game
                    HomebrewInstallOutcome.Imported(withArtwork)
                }
                is ImportOutcome.Duplicate -> HomebrewInstallOutcome.Duplicate(outcome.title)
                is ImportOutcome.Rejected -> HomebrewInstallOutcome.Rejected(outcome.reason)
                else -> HomebrewInstallOutcome.Rejected("The downloaded release did not resolve to one GBA game.")
            }
        } catch (error: Exception) {
            HomebrewInstallOutcome.Rejected(error.message ?: "Homebrew installation failed.")
        } finally {
            mutableState.value = mutableState.value.copy(installingSlug = null)
        }
    }

    private fun buildSearchUrl(query: String, page: Int): String {
        val parameters = mutableListOf(
            "platform=GBA",
            "page_elements=10",
            "page=$page",
            "order_by=title",
            "sort=asc"
        )
        if (query.isNotBlank()) parameters += "title=${encodeQuery(query)}"
        return "$API_BASE/search?${parameters.joinToString("&")}" 
    }

    private fun parsePage(bytes: ByteArray): HomebrewHubPage {
        val text = bytes.toString(StandardCharsets.UTF_8)
        val root = JSONObject(text)
        val array = when {
            root.optJSONArray("entries") != null -> root.getJSONArray("entries")
            root.optJSONArray("games") != null -> root.getJSONArray("games")
            root.optJSONArray("data") != null -> root.getJSONArray("data")
            root.optJSONArray("items") != null -> root.getJSONArray("items")
            root.opt("results") is JSONArray -> root.getJSONArray("results")
            else -> throw IllegalArgumentException("Homebrew Hub returned an unsupported response shape.")
        }
        if (array.length() > 10) throw IllegalArgumentException("Homebrew Hub returned too many entries for one page.")
        val entries = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                parseEntry(item)?.let(::add)
            }
        }
        val resultCount = when (val results = root.opt("results")) {
            is Number -> results.toInt()
            else -> root.optInt("count", entries.size)
        }.coerceAtLeast(entries.size)
        return HomebrewHubPage(
            entries = entries,
            resultCount = resultCount,
            pageCurrent = root.optInt("page_current", 1).coerceAtLeast(1),
            pageTotal = root.optInt("page_total", 1).coerceAtLeast(1),
            pageElements = root.optInt("page_elements", 10).coerceIn(1, 10)
        )
    }

    private fun parseEntry(item: JSONObject): HomebrewHubEntry? {
        val slug = item.optString("slug").trim().take(160)
        val title = item.optString("title").trim().take(240)
        if (!SAFE_SLUG.matches(slug) || title.isBlank()) return null
        val platform = item.optString("platform").uppercase()
        if (platform != "GBA") return null
        val files = item.optJSONArray("files").toFileList()
        return HomebrewHubEntry(
            slug = slug,
            title = title,
            developer = item.optString("developer", "Unknown creator").trim().take(200),
            license = item.optString("license", "Unknown").trim().take(200),
            platform = platform,
            typeTag = item.optString("typetag", "homebrew").trim().take(40),
            repository = item.optString("repository").trim().takeIf { it.startsWith("https://") }?.take(2_048),
            screenshots = item.optJSONArray("screenshots").toStringList(12, 240),
            tags = item.optJSONArray("tags").toStringList(24, 80),
            files = files
        )
    }

    private fun JSONArray?.toFileList(): List<HomebrewHubFile> {
        if (this == null) return emptyList()
        val output = ArrayList<HomebrewHubFile>()
        for (index in 0 until minOf(length(), 32)) {
            val item = optJSONObject(index) ?: continue
            val filename = item.optString("filename").trim().take(240)
            if (!SAFE_FILE.matches(filename)) continue
            output += HomebrewHubFile(
                filename = filename,
                playable = item.optBoolean("playable", false),
                isDefault = item.optBoolean("default", false)
            )
        }
        return output
    }

    private fun JSONArray?.toStringList(maximum: Int, maxChars: Int): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until minOf(length(), maximum)) {
                val value = optString(index).trim().take(maxChars)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun get(url: String, maximumBytes: Int, accept: String): ByteArray {
        val uri = URI(url)
        require(uri.scheme.equals("https", true) && uri.host.equals(API_HOST, true)) {
            "Homebrew Hub request was blocked because its origin changed."
        }
        val connection = uri.toURL().openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("Homebrew Hub request did not use HTTPS.")
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("User-Agent", "Retra/2.2 Android")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalArgumentException("Homebrew Hub returned HTTP $code.")
            }
            if (connection.contentLengthLong > maximumBytes) {
                throw IllegalArgumentException("Homebrew Hub response exceeds Retra's safety limit.")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maximumBytes) throw IllegalArgumentException("Homebrew Hub response exceeds Retra's safety limit.")
                    output.write(buffer, 0, read)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun encodePath(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    companion object {
        const val API_HOST = "hh3.gbdev.io"
        const val API_BASE = "https://hh3.gbdev.io/api"
        const val WEB_BASE = "https://hh.gbdev.io"
        private const val MAX_JSON_BYTES = 2 * 1024 * 1024
        private const val MAX_ARTWORK_BYTES = 8 * 1024 * 1024
        private val SAFE_SLUG = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,159}")
        private val SAFE_FILE = Regex("[A-Za-z0-9][A-Za-z0-9 ._()'&+,-]{0,239}")
    }
}
