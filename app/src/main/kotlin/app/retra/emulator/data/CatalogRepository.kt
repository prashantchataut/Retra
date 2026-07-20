package app.retra.emulator.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.retra.core.catalog.CatalogManifestJson
import app.retra.core.catalog.InvalidCatalogManifestException
import app.retra.core.download.CatalogDownloadPolicy
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CatalogManifest
import app.retra.core.model.CompatibilityStatus
import app.retra.core.model.CuratedDiscoveryLink
import app.retra.core.rom.CatalogValidationResult
import app.retra.core.rom.CatalogValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class StoredCatalog(
    val manifest: CatalogManifest,
    val builtIn: Boolean,
    val storagePath: String? = null,
    val contentSha256: String? = null
)

sealed interface CatalogImportOutcome {
    data class Imported(val catalog: StoredCatalog, val replacedExisting: Boolean) : CatalogImportOutcome
    data class Rejected(val reason: String) : CatalogImportOutcome
}

@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val curatedLinks = listOf(
        CuratedDiscoveryLink(
            id = "gbadev",
            title = "GBA Jam 2024",
            description = "Browse original games made for real Game Boy Advance hardware.",
            creator = "gbadev.net community",
            sourcePageUrl = "https://itch.io/jam/gbajam24",
            license = "Varies by linked project",
            tags = listOf("homebrew", "games", "community")
        ),
        CuratedDiscoveryLink(
            id = "goodboy-galaxy-demo",
            title = "Goodboy Galaxy Demo",
            description = "Official free demo from the creators of Goodboy Galaxy.",
            creator = "Goodboy Galaxy",
            sourcePageUrl = "https://goodboygalaxy.itch.io/goodboy-galaxy-demo",
            license = "Creator-authorized download",
            tags = listOf("homebrew", "demo", "platformer")
        ),
        CuratedDiscoveryLink(
            id = "anguna",
            title = "Anguna",
            description = "Official creator page for the free GBA action-adventure homebrew.",
            creator = "gauauu",
            sourcePageUrl = "https://gauauu.itch.io/anguna",
            license = "Creator-authorized download",
            tags = listOf("homebrew", "adventure", "free")
        ),
        CuratedDiscoveryLink(
            id = "aereven-advance",
            title = "Aereven Advance",
            description = "A complete GBA Jam action-adventure tested on hardware and mGBA.",
            creator = "Dreamnoid",
            sourcePageUrl = "https://dreamnoid.itch.io/aereven-advance",
            license = "Creator-authorized download",
            tags = listOf("homebrew", "adventure", "gba-jam")
        )
    )

    val officialPreview = CatalogManifest(
        catalogVersion = 1,
        catalogId = "retra-official-preview",
        name = "Retra Legal Catalog Preview",
        description = "Schema-valid preview entry used to exercise catalog policy. The example endpoint is intentionally non-routable.",
        owner = "Retra",
        sourceUrl = "https://catalog.retra.example/catalog.json",
        contentPolicy = "AUTHORIZED_ONLY",
        games = listOf(
            CatalogEntry(
                id = "retra-header-test",
                title = "Retra Header Test Cartridge",
                description = "A synthetic diagnostic fixture reserved for parser validation; not a commercial game.",
                creator = "Retra Project",
                version = "1.0.0",
                downloadUrl = "https://catalog.retra.example/files/retra-header-test.gba",
                sha256 = "0".repeat(64),
                fileSize = 1_048_576,
                license = "CC0-1.0",
                distributionPermission = "The Retra project permits redistribution of this synthetic fixture.",
                tags = listOf("diagnostic", "test-rom"),
                compatibility = CompatibilityStatus.UNKNOWN
            )
        )
    )

    val validation: CatalogValidationResult = CatalogValidator.validate(officialPreview)
    private val catalogRoot = File(context.filesDir, "catalogs")
    private val mutableCatalogs = MutableStateFlow(loadCatalogs())
    val catalogs: StateFlow<List<StoredCatalog>> = mutableCatalogs

    suspend fun importManifest(uri: Uri): CatalogImportOutcome = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use(::readLimited)
                ?: return@withContext CatalogImportOutcome.Rejected("Android could not open the selected catalog manifest.")
        } catch (error: Exception) {
            return@withContext CatalogImportOutcome.Rejected(error.message ?: "The catalog manifest could not be read.")
        }
        storeManifest(bytes)
    }

    suspend fun importManifestFromUrl(url: String, expectedSha256: String): CatalogImportOutcome = withContext(Dispatchers.IO) {
        val expected = expectedSha256.trim().lowercase()
        if (!expected.matches(Regex("[0-9a-f]{64}"))) {
            return@withContext CatalogImportOutcome.Rejected("Expected catalog SHA-256 must contain 64 hexadecimal characters.")
        }
        val origin = try {
            validateManifestUri(url.trim())
        } catch (error: Exception) {
            return@withContext CatalogImportOutcome.Rejected(error.message ?: "The catalog URL is invalid.")
        }
        var current = origin
        val visited = linkedSetOf(origin)
        repeat(MAX_MANIFEST_REDIRECTS + 1) { redirectIndex ->
            val connection = current.toURL().openConnection() as? HttpsURLConnection
                ?: return@withContext CatalogImportOutcome.Rejected("Catalog URL did not create an HTTPS connection.")
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json, text/json, text/plain")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("User-Agent", "Retra/0.7 Android")
                val status = connection.responseCode
                if (status in REDIRECT_CODES) {
                    if (redirectIndex == MAX_MANIFEST_REDIRECTS) {
                        return@withContext CatalogImportOutcome.Rejected("Catalog download exceeded $MAX_MANIFEST_REDIRECTS redirects.")
                    }
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext CatalogImportOutcome.Rejected("Catalog redirect omitted Location.")
                    val target = try {
                        validateManifestUri(current.resolve(location).toString())
                    } catch (error: Exception) {
                        return@withContext CatalogImportOutcome.Rejected(error.message ?: "Catalog redirect is unsafe.")
                    }
                    if (!origin.host.equals(target.host, ignoreCase = true)) {
                        return@withContext CatalogImportOutcome.Rejected("Cross-host catalog redirects are blocked.")
                    }
                    if (!visited.add(target)) {
                        return@withContext CatalogImportOutcome.Rejected("Catalog redirect loop detected.")
                    }
                    current = target
                    return@repeat
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    return@withContext CatalogImportOutcome.Rejected("Catalog server returned HTTP $status.")
                }
                val encoding = connection.contentEncoding?.trim()?.lowercase()
                if (!encoding.isNullOrBlank() && encoding != "identity") {
                    return@withContext CatalogImportOutcome.Rejected("Compressed catalog transfer encoding is not accepted.")
                }
                val length = connection.contentLengthLong
                if (length > CatalogManifestJson.MAX_MANIFEST_BYTES) {
                    return@withContext CatalogImportOutcome.Rejected("Catalog response exceeds Retra's 2 MiB limit.")
                }
                val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
                if (!contentType.isNullOrBlank() && contentType !in ALLOWED_MANIFEST_TYPES) {
                    return@withContext CatalogImportOutcome.Rejected("Unexpected catalog content type: $contentType.")
                }
                val bytes = connection.inputStream.use(::readLimited)
                val actual = sha256(bytes)
                if (actual != expected) {
                    return@withContext CatalogImportOutcome.Rejected("Downloaded catalog SHA-256 does not match the expected hash.")
                }
                return@withContext storeManifest(bytes)
            } catch (error: Exception) {
                return@withContext CatalogImportOutcome.Rejected(error.message ?: "Catalog download failed.")
            } finally {
                connection.disconnect()
            }
        }
        CatalogImportOutcome.Rejected("Catalog redirect handling failed.")
    }

    fun deleteCatalog(catalog: StoredCatalog): Boolean {
        if (catalog.builtIn) return false
        val path = catalog.storagePath ?: return false
        val deleted = !File(path).exists() || File(path).delete()
        if (deleted) mutableCatalogs.value = mutableCatalogs.value.filterNot { it.storagePath == path }
        return deleted
    }

    fun isDownloadable(entry: CatalogEntry): Boolean = runCatching {
        CatalogDownloadPolicy.validateEntry(entry)
        val host = URI(entry.downloadUrl).host.orEmpty()
        host.isNotBlank() && !host.endsWith(".example", ignoreCase = true) && entry.sha256.any { it != '0' }
    }.getOrDefault(false)

    private fun storeManifest(bytes: ByteArray): CatalogImportOutcome {
        val manifest = try {
            CatalogManifestJson.parse(bytes)
        } catch (error: InvalidCatalogManifestException) {
            return CatalogImportOutcome.Rejected(error.message ?: "The catalog manifest is invalid.")
        }
        if (manifest.catalogId == officialPreview.catalogId) {
            return CatalogImportOutcome.Rejected("The built-in Retra preview catalog cannot be replaced.")
        }
        val contentHash = sha256(bytes)
        catalogRoot.mkdirs()
        val destination = File(catalogRoot, "$contentHash.json")
        val temporary = File(catalogRoot, ".$contentHash-${System.nanoTime()}.tmp")
        return try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }
            moveAtomically(temporary, destination)
            val existing = mutableCatalogs.value.firstOrNull {
                !it.builtIn && it.manifest.catalogId == manifest.catalogId
            }
            val imported = StoredCatalog(manifest, builtIn = false, destination.absolutePath, contentHash)
            mutableCatalogs.value = mutableCatalogs.value
                .filterNot { !it.builtIn && it.manifest.catalogId == manifest.catalogId }
                .plus(imported)
                .sortedWith(compareByDescending<StoredCatalog> { it.builtIn }.thenBy { it.manifest.name.lowercase() })
            existing?.storagePath?.let { oldPath ->
                if (oldPath != destination.absolutePath) File(oldPath).delete()
            }
            CatalogImportOutcome.Imported(imported, replacedExisting = existing != null)
        } catch (error: Exception) {
            CatalogImportOutcome.Rejected(error.message ?: "The catalog manifest could not be stored safely.")
        } finally {
            if (temporary.exists()) temporary.delete()
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
            if (total > CatalogManifestJson.MAX_MANIFEST_BYTES) {
                throw InvalidCatalogManifestException("Catalog manifest exceeds Retra's 2 MiB limit.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun validateManifestUri(value: String): URI {
        val uri = runCatching { URI(value) }.getOrElse { throw IllegalArgumentException("Catalog URL is malformed.") }
        require(uri.scheme.equals("https", ignoreCase = true)) { "Catalog URL must use HTTPS." }
        require(!uri.host.isNullOrBlank()) { "Catalog URL has no host." }
        require(uri.userInfo == null && uri.fragment == null) { "Catalog URL cannot contain credentials or a fragment." }
        require(!isBlockedHost(uri.host)) { "Catalog URL cannot target a local or private host." }
        val path = uri.path.orEmpty().lowercase()
        require(path.endsWith(".json") || path.endsWith(".catalog")) { "Catalog URL must identify a .json or .catalog file." }
        return uri.normalize()
    }

    private fun isBlockedHost(hostValue: String): Boolean {
        val host = hostValue.trim().trim('[', ']').lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host.endsWith(".lan") || host.endsWith(".home.arpa") ||
            (!host.contains('.') && !host.contains(':'))
        ) return true
        val looksLikeIp = host.contains(':') || host.matches(Regex("[0-9.]+"))
        if (!looksLikeIp) return false
        val address = runCatching { InetAddress.getByName(host) }.getOrElse { return true }
        return address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
    }

    private fun loadCatalogs(): List<StoredCatalog> {
        val builtIn = StoredCatalog(officialPreview, builtIn = true)
        if (!catalogRoot.exists()) return listOf(builtIn)
        val imported = catalogRoot.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .filter { it.length() in 1..CatalogManifestJson.MAX_MANIFEST_BYTES.toLong() }
            .mapNotNull { file ->
                runCatching {
                    val bytes = file.readBytes()
                    val manifest = CatalogManifestJson.parse(bytes)
                    StoredCatalog(
                        manifest = manifest,
                        builtIn = false,
                        storagePath = file.absolutePath,
                        contentSha256 = sha256(bytes)
                    )
                }.getOrNull()
            }
            .distinctBy { it.manifest.catalogId }
            .sortedBy { it.manifest.name.lowercase() }
        return listOf(builtIn) + imported
    }

    private fun moveAtomically(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_MANIFEST_REDIRECTS = 3
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val ALLOWED_MANIFEST_TYPES = setOf("application/json", "text/json", "text/plain", "application/octet-stream")
    }
}
