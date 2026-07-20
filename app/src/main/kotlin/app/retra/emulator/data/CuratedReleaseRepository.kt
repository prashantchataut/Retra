package app.retra.emulator.data

import app.retra.core.download.CatalogDownloadPolicy
import app.retra.core.model.CatalogContentKind
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CompatibilityStatus
import app.retra.core.model.CuratedDiscoveryLink
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CuratedReleaseState(
    val links: List<CuratedDiscoveryLink>,
    val downloadableEntries: List<CatalogEntry> = emptyList(),
    val refreshing: Boolean = false,
    val lastError: String? = null
)

private data class CuratedGitHubRelease(
    val id: String,
    val owner: String,
    val repository: String,
    val title: String,
    val description: String,
    val creator: String,
    val license: String,
    val distributionPermission: String,
    val tags: List<String>
) {
    val sourcePageUrl: String get() = "https://github.com/$owner/$repository/releases"
    val apiUrl: String get() = "https://api.github.com/repos/$owner/$repository/releases/latest"
}

@Singleton
class CuratedReleaseRepository @Inject constructor() {
    private val sources = listOf(
        CuratedGitHubRelease(
            id = "butano",
            owner = "GValiente",
            repository = "butano",
            title = "Butano Engine Releases",
            description = "Open-source GBA engine releases and examples. Downloads appear only when GitHub supplies an asset SHA-256 digest.",
            creator = "GValiente",
            license = "MIT",
            distributionPermission = "Assets are offered by the upstream project under its published open-source release terms.",
            tags = listOf("homebrew", "open-source", "development")
        )
    )
    private val mutableState = MutableStateFlow(
        CuratedReleaseState(links = sources.map { it.toDiscoveryLink() })
    )
    val state: StateFlow<CuratedReleaseState> = mutableState

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(refreshing = true, lastError = null)
        val entries = mutableListOf<CatalogEntry>()
        val errors = mutableListOf<String>()
        sources.forEach { source ->
            runCatching { fetchLatest(source) }
                .onSuccess(entries::addAll)
                .onFailure { errors += "${source.title}: ${it.message ?: "request failed"}" }
        }
        mutableState.value = CuratedReleaseState(
            links = sources.map { it.toDiscoveryLink() },
            downloadableEntries = entries.distinctBy(CatalogEntry::id),
            refreshing = false,
            lastError = if (errors.isEmpty()) null else errors.joinToString(" ")
        )
    }

    private fun fetchLatest(source: CuratedGitHubRelease): List<CatalogEntry> {
        val uri = URI(source.apiUrl)
        require(uri.scheme == "https" && uri.host == "api.github.com") { "Curated API URL is not an approved GitHub endpoint." }
        val connection = uri.toURL().openConnection() as HttpsURLConnection
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Retra/0.7 Android")
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) { "GitHub returned HTTP $status." }
            val encoding = connection.contentEncoding?.trim()?.lowercase()
            require(encoding.isNullOrEmpty() || encoding == "identity") { "GitHub returned an encoded metadata response." }
            val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            require(contentType.isNullOrEmpty() || contentType in ALLOWED_JSON_TYPES) {
                "GitHub returned an unexpected metadata content type."
            }
            val length = connection.contentLengthLong
            require(length <= MAX_RELEASE_JSON_BYTES) { "GitHub response exceeded the metadata limit." }
            val body = connection.inputStream.use(::readLimited)
            parseRelease(source, JSONObject(body.toString(Charsets.UTF_8)))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(source: CuratedGitHubRelease, release: JSONObject): List<CatalogEntry> {
        val version = release.optString("tag_name").ifBlank { release.optString("name").ifBlank { "latest" } }
        val sourcePage = release.optString("html_url").takeIf { it.startsWith("https://github.com/") }
            ?: source.sourcePageUrl
        val assets = release.optJSONArray("assets") ?: return emptyList()
        return buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension !in SUPPORTED_EXTENSIONS) continue
                val digest = asset.optString("digest")
                val sha256 = digest.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:") ?: continue
                if (!sha256.matches(SHA256_PATTERN)) continue
                val url = asset.optString("browser_download_url")
                val size = asset.optLong("size", -1L)
                val entry = CatalogEntry(
                    id = "${source.id}-${asset.optLong("id", index.toLong())}".take(80),
                    title = name.substringBeforeLast('.').ifBlank { source.title },
                    description = source.description,
                    creator = source.creator,
                    version = version.take(80),
                    downloadUrl = url,
                    sha256 = sha256,
                    fileSize = size,
                    license = source.license,
                    distributionPermission = source.distributionPermission,
                    tags = source.tags,
                    compatibility = CompatibilityStatus.UNKNOWN,
                    contentKind = when (extension) {
                        "gba" -> CatalogContentKind.GAME
                        "zip" -> CatalogContentKind.ARCHIVE
                        else -> CatalogContentKind.PATCH
                    },
                    sourcePageUrl = sourcePage
                )
                if (runCatching { CatalogDownloadPolicy.validateEntry(entry) }.isSuccess) add(entry)
            }
        }
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_RELEASE_JSON_BYTES) { "GitHub response exceeded the metadata limit." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun CuratedGitHubRelease.toDiscoveryLink() = CuratedDiscoveryLink(
        id = id,
        title = title,
        description = description,
        creator = creator,
        sourcePageUrl = sourcePageUrl,
        license = license,
        tags = tags
    )

    private companion object {
        const val MAX_RELEASE_JSON_BYTES = 1_048_576
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
        val SUPPORTED_EXTENSIONS = setOf("gba", "zip", "ups", "ips", "bps")
        val ALLOWED_JSON_TYPES = setOf("application/json", "application/vnd.github+json")
    }
}
