package app.retra.emulator.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) {
    suspend fun importCoverArt(gameId: Long, gameSha256: String, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use(::readLimited)
                ?: error("Android could not open the selected artwork.")
            persistCoverArt(gameId, gameSha256, bytes)
        }
    }

    /**
     * Stores artwork obtained from an explicitly licensed catalog source. The same decoder,
     * dimension limits, down-sampling, and atomic write path are used for user-selected artwork.
     */
    suspend fun importCoverArtBytes(gameId: Long, gameSha256: String, bytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { persistCoverArt(gameId, gameSha256, bytes.copyOf()) }
        }

    suspend fun removeCoverArt(gameId: Long, path: String?): Boolean = withContext(Dispatchers.IO) {
        val deleted = path?.let(::File)?.let { !it.exists() || it.delete() } ?: true
        if (deleted) gameDao.setCoverArt(gameId, null)
        deleted
    }

    suspend fun deleteFile(path: String?) = withContext(Dispatchers.IO) {
        path?.let(::File)?.takeIf(File::exists)?.delete()
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_SOURCE_BYTES) { "Cover art must be 12 MiB or smaller." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private suspend fun persistCoverArt(gameId: Long, gameSha256: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty() && bytes.size <= MAX_SOURCE_BYTES) { "Cover art must be between 1 byte and 12 MiB." }
        require(gameSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "A valid game SHA-256 is required for artwork storage." }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..MAX_SOURCE_DIMENSION && bounds.outHeight in 1..MAX_SOURCE_DIMENSION) {
            "Cover art dimensions are invalid or too large."
        }
        val sample = calculateSample(bounds.outWidth, bounds.outHeight, MAX_OUTPUT_DIMENSION)
        val bitmap = requireNotNull(
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ) { "The selected file is not a supported image." }
        try {
            val directory = File(context.filesDir, "artwork").apply { mkdirs() }
            val target = File(directory, "${gameSha256.lowercase()}.jpg")
            val temporary = File(directory, ".${target.name}.tmp")
            try {
                FileOutputStream(temporary).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "Cover art encoding failed." }
                    output.fd.sync()
                }
                try {
                    Files.move(
                        temporary.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: Exception) {
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                gameDao.setCoverArt(gameId, target.absolutePath)
                return target.absolutePath
            } finally {
                if (temporary.exists()) temporary.delete()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun calculateSample(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / sample > target || height / sample > target) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_SOURCE_BYTES = 12 * 1024 * 1024
        const val MAX_SOURCE_DIMENSION = 12_000
        const val MAX_OUTPUT_DIMENSION = 1_600
    }
}
