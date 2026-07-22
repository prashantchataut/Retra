package app.retra.emulator.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.retra.core.cheats.CheatCatalog
import app.retra.core.cheats.CheatCatalogEntry
import app.retra.core.cheats.InvalidCheatCatalogException
import app.retra.core.cheats.RetraCheatCatalogParser
import app.retra.core.model.GameRecord
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class StoredCheatCatalog(
    val fileName: String,
    val sha256: String,
    val importedAtEpochMillis: Long,
    val catalog: CheatCatalog
)

sealed interface CheatCatalogImportOutcome {
    data class Imported(val catalog: StoredCheatCatalog) : CheatCatalogImportOutcome
    data class Duplicate(val name: String) : CheatCatalogImportOutcome
    data class Rejected(val reason: String) : CheatCatalogImportOutcome
}

@Singleton
class CheatCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val root = File(context.filesDir, "cheat-catalogs")
    private val mutableCatalogs = MutableStateFlow<List<StoredCheatCatalog>>(emptyList())
    val catalogs: StateFlow<List<StoredCheatCatalog>> = mutableCatalogs

    init {
        refresh()
    }

    fun compatibleEntries(game: GameRecord): List<CheatCatalogEntry> = mutableCatalogs.value
        .asSequence()
        .flatMap { it.catalog.entries.asSequence() }
        .filter { entry ->
            RetraCheatCatalogParser.matches(entry, game.sha256, game.gameCode, game.softwareVersion)
        }
        .distinctBy { it.packSha256 }
        .sortedWith(compareBy<CheatCatalogEntry> { it.provider.lowercase() }.thenBy { it.title.lowercase() })
        .toList()

    suspend fun import(uri: Uri): CheatCatalogImportOutcome = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri)?.take(160) ?: "Retra cheat index.rci"
        val bytes = try {
            readLimited(uri)
        } catch (error: Exception) {
            return@withContext CheatCatalogImportOutcome.Rejected(error.message ?: "The cheat index could not be read.")
        }
        val catalog = try {
            RetraCheatCatalogParser.parse(bytes)
        } catch (error: InvalidCheatCatalogException) {
            return@withContext CheatCatalogImportOutcome.Rejected(error.message ?: "The cheat index is invalid.")
        }
        val hash = Sha256.of(bytes)
        val destination = File(root, "$hash.rci")
        if (destination.isFile) return@withContext CheatCatalogImportOutcome.Duplicate(catalog.name)
        try {
            writeAtomically(destination, bytes)
        } catch (error: Exception) {
            return@withContext CheatCatalogImportOutcome.Rejected(error.message ?: "The cheat index could not be stored safely.")
        }
        val stored = StoredCheatCatalog(displayName, hash, System.currentTimeMillis(), catalog)
        refresh()
        CheatCatalogImportOutcome.Imported(stored)
    }

    fun delete(stored: StoredCheatCatalog): Boolean {
        val target = File(root, "${stored.sha256}.rci")
        val deleted = !target.exists() || target.delete()
        if (deleted) refresh()
        return deleted
    }

    private fun refresh() {
        root.mkdirs()
        mutableCatalogs.value = root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("rci", ignoreCase = true) }
            .mapNotNull { file ->
                runCatching {
                    val bytes = file.readBytes()
                    val catalog = RetraCheatCatalogParser.parse(bytes)
                    StoredCheatCatalog(
                        fileName = "${catalog.name}.rci",
                        sha256 = Sha256.of(bytes),
                        importedAtEpochMillis = file.lastModified(),
                        catalog = catalog
                    )
                }.getOrNull()
            }
            .sortedByDescending(StoredCheatCatalog::importedAtEpochMillis)
    }

    private fun readLimited(uri: Uri): ByteArray {
        val input = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Android could not open the cheat index.")
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > RetraCheatCatalogParser.MAX_CATALOG_BYTES) {
                    throw IllegalArgumentException("Cheat index exceeds 1 MiB.")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        root.mkdirs()
        val temporary = File(root, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
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
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
