package app.retra.emulator.data

import android.content.Context
import app.retra.core.rom.InvalidLibretroDatException
import app.retra.core.rom.LibretroDatIndex
import app.retra.core.rom.LibretroDatParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class MetadataSyncState(
    val syncing: Boolean = false,
    val indexedRecords: Int = 0,
    val matchedGames: Int = 0,
    val lastSyncedAtEpochMillis: Long? = null,
    val error: String? = null
)

@Singleton
class LibretroMetadataRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val gameDao: GameDao
) {
    private val root = File(context.filesDir, "metadata-indexes")
    private val gbaDat = File(root, "libretro-no-intro-gba.dat")
    private val mutableState = MutableStateFlow(
        MetadataSyncState(lastSyncedAtEpochMillis = gbaDat.takeIf(File::isFile)?.lastModified())
    )
    val state: StateFlow<MetadataSyncState> = mutableState

    @Volatile
    private var cachedIndex: LibretroDatIndex? = null

    suspend fun syncGba(): MetadataSyncState = withContext(Dispatchers.IO) {
        if (mutableState.value.syncing) return@withContext mutableState.value
        mutableState.value = mutableState.value.copy(syncing = true, error = null)
        val result = runCatching {
            val bytes = download(GBA_DAT_URL)
            val index = LibretroDatParser.parse(bytes)
            writeAtomically(gbaDat, bytes)
            cachedIndex = index
            val matched = applyIndex(index)
            MetadataSyncState(
                syncing = false,
                indexedRecords = index.records.size,
                matchedGames = matched,
                lastSyncedAtEpochMillis = System.currentTimeMillis(),
                error = null
            )
        }.getOrElse { error ->
            MetadataSyncState(
                syncing = false,
                indexedRecords = cachedIndex?.records?.size ?: 0,
                matchedGames = 0,
                lastSyncedAtEpochMillis = gbaDat.takeIf(File::isFile)?.lastModified(),
                error = error.message ?: "Metadata sync failed."
            )
        }
        mutableState.value = result
        result
    }

    suspend fun enrich(entity: GameEntity): GameEntity = withContext(Dispatchers.IO) {
        val index = loadCachedIndex() ?: return@withContext entity
        val match = index.match(entity.sha1, entity.crc32, entity.sizeBytes) ?: return@withContext entity
        gameDao.applyCanonicalMetadata(
            id = entity.id,
            title = match.canonicalTitle,
            canonicalTitle = match.canonicalTitle,
            metadataSource = SOURCE_LABEL
        )
        entity.copy(
            title = match.canonicalTitle,
            canonicalTitle = match.canonicalTitle,
            metadataSource = SOURCE_LABEL
        )
    }

    private suspend fun applyIndex(index: LibretroDatIndex): Int {
        var matched = 0
        gameDao.getAll().forEach { entity ->
            val record = index.match(entity.sha1, entity.crc32, entity.sizeBytes) ?: return@forEach
            gameDao.applyCanonicalMetadata(
                entity.id,
                record.canonicalTitle,
                record.canonicalTitle,
                SOURCE_LABEL
            )
            matched++
        }
        return matched
    }

    private fun loadCachedIndex(): LibretroDatIndex? {
        cachedIndex?.let { return it }
        if (!gbaDat.isFile) return null
        return runCatching { LibretroDatParser.parse(gbaDat.readBytes()) }
            .onSuccess { cachedIndex = it }
            .getOrNull()
    }

    private fun download(url: String): ByteArray {
        val connection = java.net.URI(url).toURL().openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("Metadata source must use HTTPS.")
        try {
            require(connection.url.host.equals("raw.githubusercontent.com", ignoreCase = true)) {
                "Metadata source host is not allowed."
            }
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "text/plain, application/octet-stream")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("User-Agent", "Retra/2.0 Android")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalArgumentException("Metadata server returned HTTP $code.")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > LibretroDatParser.MAX_DAT_BYTES) {
                throw IllegalArgumentException("Metadata response exceeds 32 MiB.")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > LibretroDatParser.MAX_DAT_BYTES) {
                        throw IllegalArgumentException("Metadata response exceeds 32 MiB.")
                    }
                    output.write(buffer, 0, read)
                }
            }
            return output.toByteArray()
        } catch (error: InvalidLibretroDatException) {
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    companion object {
        const val SOURCE_LABEL = "Libretro No-Intro DAT"
        const val GBA_DAT_URL = "https://raw.githubusercontent.com/libretro/libretro-database/master/metadat/no-intro/Nintendo%20-%20Game%20Boy%20Advance.dat"
    }
}
