package app.retra.emulator.data

import android.content.Context
import android.net.Uri
import app.retra.core.download.CatalogDownloadPolicy
import app.retra.core.download.DownloadResponseMetadata
import app.retra.core.download.UnsafeDownloadException
import app.retra.core.model.CatalogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

enum class CatalogDownloadPhase { IDLE, CONNECTING, DOWNLOADING, VERIFYING, IMPORTING, COMPLETE, FAILED }

data class CatalogDownloadProgress(
    val entryId: String,
    val phase: CatalogDownloadPhase,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val message: String? = null
)

sealed interface CatalogDownloadOutcome {
    data class Imported(val game: app.retra.core.model.GameRecord) : CatalogDownloadOutcome
    data class Duplicate(val title: String) : CatalogDownloadOutcome
    data class Batch(
        val imported: Int,
        val duplicates: Int,
        val rejected: Int,
        val pendingPatches: List<PendingPatch>
    ) : CatalogDownloadOutcome
    data class PatchDetected(val pending: PendingPatch) : CatalogDownloadOutcome
    data class Rejected(val reason: String) : CatalogDownloadOutcome
}

@Singleton
class CatalogDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository
) {
    private val mutableProgress = MutableStateFlow<Map<String, CatalogDownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, CatalogDownloadProgress>> = mutableProgress
    private val temporaryRoot = File(context.cacheDir, "catalog-downloads")

    suspend fun download(entry: CatalogEntry): CatalogDownloadOutcome = withContext(Dispatchers.IO) {
        val progressKey = entry.sha256.lowercase()
        val origin = try {
            CatalogDownloadPolicy.validateEntry(entry)
        } catch (error: UnsafeDownloadException) {
            return@withContext reject(progressKey, error.message ?: "Catalog metadata is unsafe.")
        }
        update(progressKey, CatalogDownloadPhase.CONNECTING, total = entry.fileSize)
        temporaryRoot.mkdirs()
        val extension = origin.path.substringAfterLast('.', "bin").lowercase()
        val temporary = File(temporaryRoot, "${sanitizeFileName(entry.id)}-${System.nanoTime()}.$extension")
        try {
            streamDownload(entry, origin, temporary, progressKey)
            update(progressKey, CatalogDownloadPhase.VERIFYING, temporary.length(), entry.fileSize)
            CatalogDownloadPolicy.validateCompletedSize(entry, temporary.length())
            val actualHash = sha256(temporary)
            if (!actualHash.equals(entry.sha256, ignoreCase = true)) {
                throw UnsafeDownloadException("Downloaded SHA-256 does not match the catalog metadata.")
            }
            update(progressKey, CatalogDownloadPhase.IMPORTING, temporary.length(), entry.fileSize)
            val outcome = when (val imported = gameRepository.importVerifiedCatalogFile(Uri.fromFile(temporary), entry)) {
                is ImportOutcome.Imported -> CatalogDownloadOutcome.Imported(imported.game)
                is ImportOutcome.Duplicate -> CatalogDownloadOutcome.Duplicate(imported.title)
                is ImportOutcome.Batch -> CatalogDownloadOutcome.Batch(
                    imported.imported,
                    imported.duplicates,
                    imported.rejected,
                    imported.pendingPatches
                )
                is ImportOutcome.PatchDetected -> CatalogDownloadOutcome.PatchDetected(imported.pending)
                is ImportOutcome.Rejected -> CatalogDownloadOutcome.Rejected(imported.reason)
            }
            val message = when (outcome) {
                is CatalogDownloadOutcome.Imported -> "Imported"
                is CatalogDownloadOutcome.Duplicate -> "Already in library"
                is CatalogDownloadOutcome.Batch -> "Archive processed"
                is CatalogDownloadOutcome.PatchDetected -> "Patch ready"
                is CatalogDownloadOutcome.Rejected -> outcome.reason
            }
            update(
                progressKey,
                if (outcome is CatalogDownloadOutcome.Rejected) CatalogDownloadPhase.FAILED else CatalogDownloadPhase.COMPLETE,
                temporary.length(),
                entry.fileSize,
                message
            )
            outcome
        } catch (error: Exception) {
            reject(progressKey, error.message ?: "Catalog download failed.")
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private suspend fun streamDownload(entry: CatalogEntry, origin: URI, target: File, progressKey: String) {
        var current = origin
        val visited = linkedSetOf(origin)
        repeat(CatalogDownloadPolicy.MAX_REDIRECTS + 1) { redirectIndex ->
            coroutineContext.ensureActive()
            val connection = current.toURL().openConnection() as? HttpsURLConnection
                ?: throw UnsafeDownloadException("Catalog URL did not create an HTTPS connection.")
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "Accept",
                    "application/octet-stream, application/x-gba-rom, application/zip, " +
                        "application/x-zip-compressed, application/x-ups-patch, application/x-ips-patch, application/x-bps-patch"
                )
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("User-Agent", "Retra/2.0 Android")
                val code = connection.responseCode
                if (code in REDIRECT_CODES) {
                    if (redirectIndex >= CatalogDownloadPolicy.MAX_REDIRECTS) {
                        throw UnsafeDownloadException("Catalog download exceeded ${CatalogDownloadPolicy.MAX_REDIRECTS} redirects.")
                    }
                    val location = connection.getHeaderField("Location")
                        ?: throw UnsafeDownloadException("Catalog redirect omitted the Location header.")
                    current = CatalogDownloadPolicy.validateRedirect(origin, current, location, visited)
                    visited += current
                    return@repeat
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw UnsafeDownloadException("Catalog server returned HTTP $code.")
                }
                CatalogDownloadPolicy.validateResponse(
                    entry,
                    DownloadResponseMetadata(
                        contentType = connection.contentType,
                        contentLength = connection.contentLengthLong,
                        contentEncoding = connection.contentEncoding
                    )
                )
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                connection.inputStream.buffered().use { input ->
                    FileOutputStream(target).use { rawOutput ->
                        val output = rawOutput.buffered()
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > CatalogDownloadPolicy.MAX_DOWNLOAD_BYTES || total > entry.fileSize) {
                                throw UnsafeDownloadException("Catalog download exceeded the declared size.")
                            }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            update(progressKey, CatalogDownloadPhase.DOWNLOADING, total, entry.fileSize)
                        }
                        output.flush()
                        rawOutput.fd.sync()
                    }
                }
                if (!digest.digest().toHex().equals(entry.sha256, ignoreCase = true)) {
                    throw UnsafeDownloadException("Downloaded SHA-256 does not match the catalog metadata.")
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        throw UnsafeDownloadException("Catalog redirect handling failed.")
    }

    private fun update(
        entryId: String,
        phase: CatalogDownloadPhase,
        bytes: Long = 0,
        total: Long = 0,
        message: String? = null
    ) {
        mutableProgress.value = mutableProgress.value + (
            entryId to CatalogDownloadProgress(entryId, phase, bytes, total, message)
        )
    }

    private fun reject(entryId: String, reason: String): CatalogDownloadOutcome.Rejected {
        update(entryId, CatalogDownloadPhase.FAILED, message = reason)
        return CatalogDownloadOutcome.Rejected(reason)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sanitizeFileName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .trim()
        .ifBlank { "catalog-game" }
        .take(100)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
