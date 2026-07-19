
package app.retra.emulator.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import app.retra.core.model.GameRecord
import app.retra.core.rom.GbaRomParser
import app.retra.core.rom.InvalidRomException
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface ImportOutcome {
    data class Imported(val game: GameRecord) : ImportOutcome
    data class Duplicate(val title: String) : ImportOutcome
    data class Rejected(val reason: String) : ImportOutcome
}

data class FolderImportSummary(
    val imported: Int,
    val duplicates: Int,
    val rejected: Int,
    val limitReached: Boolean
)

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) {
    private val resolver: ContentResolver get() = context.contentResolver

    fun observeGames(): Flow<List<GameRecord>> = gameDao.observeAll().map { list -> list.map(GameEntity::toRecord) }

    suspend fun importFile(uri: Uri): ImportOutcome = withContext(Dispatchers.IO) {
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val displayName = queryDisplayName(uri) ?: "Imported game.gba"
        if (!displayName.endsWith(".gba", ignoreCase = true)) {
            return@withContext ImportOutcome.Rejected("Retra only imports .gba files in this build.")
        }

        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytesLimited(GbaRomParser.MAX_ROM_SIZE_BYTES) }
                ?: return@withContext ImportOutcome.Rejected("Android could not open the selected file.")
        } catch (error: Exception) {
            return@withContext ImportOutcome.Rejected(error.message ?: "The selected file could not be read.")
        }

        val header = try {
            GbaRomParser.parse(bytes)
        } catch (error: InvalidRomException) {
            return@withContext ImportOutcome.Rejected(error.message ?: "Invalid GBA file.")
        }

        val hash = Sha256.of(bytes)
        if (gameDao.countBySha256(hash) > 0) {
            return@withContext ImportOutcome.Duplicate(header.title)
        }

        val entity = GameEntity(
            uri = uri.toString(),
            displayName = displayName,
            title = header.title,
            gameCode = header.gameCode,
            makerCode = header.makerCode,
            softwareVersion = header.softwareVersion,
            sha256 = hash,
            sizeBytes = bytes.size.toLong(),
            importedAtEpochMillis = System.currentTimeMillis()
        )
        val id = gameDao.insert(entity)
        ImportOutcome.Imported(entity.copy(id = id).toRecord())
    }

    suspend fun importFolder(treeUri: Uri): FolderImportSummary = withContext(Dispatchers.IO) {
        runCatching { resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext FolderImportSummary(0, 0, 1, false)

        var imported = 0
        var duplicates = 0
        var rejected = 0
        var inspected = 0
        var visitedNodes = 0
        val maxFiles = 500
        val maxNodes = 2_000
        val maxDepth = 5
        val queue = ArrayDeque<Pair<DocumentFile, Int>>()
        queue.add(root to 0)

        while (queue.isNotEmpty() && inspected < maxFiles && visitedNodes < maxNodes) {
            val (node, depth) = queue.removeFirst()
            visitedNodes++
            if (node.isDirectory && depth < maxDepth) {
                node.listFiles().forEach { queue.add(it to depth + 1) }
            } else if (node.isFile && node.name?.endsWith(".gba", ignoreCase = true) == true) {
                inspected++
                when (importFile(node.uri)) {
                    is ImportOutcome.Imported -> imported++
                    is ImportOutcome.Duplicate -> duplicates++
                    is ImportOutcome.Rejected -> rejected++
                }
            }
        }
        FolderImportSummary(imported, duplicates, rejected, queue.isNotEmpty())
    }

    suspend fun markPlayed(id: Long) = withContext(Dispatchers.IO) {
        gameDao.markPlayed(id, System.currentTimeMillis())
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { gameDao.deleteById(id) }

    private fun queryDisplayName(uri: Uri): String? = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw InvalidRomException("The selected file is larger than 64 MiB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
