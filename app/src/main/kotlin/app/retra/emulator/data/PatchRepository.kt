package app.retra.emulator.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import app.retra.core.model.GameRecord
import app.retra.core.patching.InvalidPatchException
import app.retra.core.patching.PatchDescriptor
import app.retra.core.patching.PatchEngine
import app.retra.core.patching.PatchFormat
import app.retra.core.rom.GbaRomParser
import app.retra.core.rom.InvalidRomException
import app.retra.core.rom.Sha1
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PatchOutcome {
    data class Applied(
        val game: GameRecord,
        val format: PatchFormat,
        val patchDisplayName: String,
        val headerChecksumValid: Boolean
    ) : PatchOutcome

    data class Duplicate(val existingTitle: String) : PatchOutcome
    data class Rejected(val reason: String) : PatchOutcome
}

@Singleton
class PatchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val outputRoot = File(context.filesDir, "patched-roms")

    suspend fun inspect(patchUri: Uri): Result<Pair<String, PatchDescriptor>> = withContext(Dispatchers.IO) {
        runCatching {
            runCatching { resolver.takePersistableUriPermission(patchUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = queryDisplayName(patchUri) ?: "Imported patch"
            val patch = readUriLimited(patchUri, PatchEngine.MAX_PATCH_SIZE_BYTES, "Patch")
            name to PatchEngine.inspect(patch)
        }
    }

    suspend fun compatibleGames(descriptor: PatchDescriptor, games: List<GameRecord>): List<GameRecord> =
        withContext(Dispatchers.IO) {
            val byCrc = descriptor.sourceCrc32?.let { crc ->
                gameDao.findByCrc32(crc).map(GameEntity::toRecord)
            }.orEmpty()
            val bySize = games.filter { game ->
                descriptor.sourceSizeBytes == null || game.sizeBytes == descriptor.sourceSizeBytes
            }
            (byCrc + bySize)
                .distinctBy { it.id }
                .filter { game ->
                    descriptor.sourceSizeBytes == null || game.sizeBytes == descriptor.sourceSizeBytes
                }
                .filter { game ->
                    val crc = game.crc32 ?: runCatching {
                        PatchEngine.crc32Of(readUriLimited(Uri.parse(game.uri), PatchEngine.MAX_SOURCE_SIZE_BYTES, "Base ROM"))
                    }.getOrNull()
                    descriptor.sourceCrc32 == null || crc == descriptor.sourceCrc32
                }
        }

    suspend fun apply(base: GameRecord, patchUri: Uri, preferredTitle: String? = null): PatchOutcome =
        withContext(Dispatchers.IO) {
            runCatching { resolver.takePersistableUriPermission(patchUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val patchName = queryDisplayName(patchUri) ?: "Imported patch"
            val source = try {
                readUriLimited(Uri.parse(base.uri), PatchEngine.MAX_SOURCE_SIZE_BYTES, "Base ROM")
            } catch (error: Exception) {
                return@withContext PatchOutcome.Rejected(error.message ?: "The base ROM could not be read.")
            }
            if (!Sha256.of(source).equals(base.sha256, ignoreCase = true)) {
                return@withContext PatchOutcome.Rejected("The base ROM changed after it was imported. Re-import it before applying a patch.")
            }
            val patch = try {
                readUriLimited(patchUri, PatchEngine.MAX_PATCH_SIZE_BYTES, "Patch")
            } catch (error: Exception) {
                return@withContext PatchOutcome.Rejected(error.message ?: "The patch file could not be read.")
            }

            val result = try {
                PatchEngine.apply(source, patch)
            } catch (error: InvalidPatchException) {
                return@withContext PatchOutcome.Rejected(error.message ?: "The patch is invalid or incompatible.")
            }
            val header = try {
                GbaRomParser.parse(result.output)
            } catch (error: InvalidRomException) {
                return@withContext PatchOutcome.Rejected(
                    "The patch produced a file that is not a valid GBA ROM: ${error.message ?: "invalid header"}"
                )
            }
            if (gameDao.countBySha256(result.outputSha256) > 0) {
                return@withContext PatchOutcome.Duplicate(header.title)
            }

            val outputFile = File(outputRoot, "${result.outputSha256}.gba")
            try {
                writeAtomically(outputFile, result.output)
            } catch (error: Exception) {
                return@withContext PatchOutcome.Rejected(error.message ?: "The patched ROM could not be stored safely.")
            }

            val cleanPatchName = patchName.take(160)
            val hint = KnownPatchHints.match(
                PatchEngine.inspect(patch)
            )
            val title = preferredTitle
                ?: hint?.resultTitle
                ?: if (header.title.equals(base.title, ignoreCase = true)) "${base.title} (Patched)" else header.title
            val displayName = buildDisplayName(base.displayName, patchName)
            val entity = GameEntity(
                uri = outputFile.toURI().toString(),
                displayName = displayName,
                title = title,
                gameCode = header.gameCode,
                makerCode = header.makerCode,
                softwareVersion = header.softwareVersion,
                sha256 = result.outputSha256,
                sizeBytes = result.output.size.toLong(),
                importedAtEpochMillis = System.currentTimeMillis(),
                origin = "LOCAL_PATCH",
                baseSha256 = result.sourceSha256,
                patchSha256 = result.patchSha256,
                patchFormat = result.format.name,
                patchDisplayName = cleanPatchName,
                crc32 = PatchEngine.crc32Of(result.output),
                sha1 = Sha1.of(result.output),
                managedPath = outputFile.absolutePath,
                tagsCsv = GameEntity.encodeCsv(listOf("patched"))
            )
            val id = try {
                gameDao.insert(entity)
            } catch (error: Exception) {
                if (gameDao.countBySha256(result.outputSha256) > 0) {
                    return@withContext PatchOutcome.Duplicate(title)
                }
                outputFile.delete()
                return@withContext PatchOutcome.Rejected(error.message ?: "The patched ROM metadata could not be stored.")
            }
            PatchOutcome.Applied(
                game = entity.copy(id = id).toRecord(),
                format = result.format,
                patchDisplayName = cleanPatchName,
                headerChecksumValid = header.headerChecksumValid
            )
        }

    private fun readUriLimited(uri: Uri, maximum: Int, label: String): ByteArray {
        val input = when (uri.scheme?.lowercase()) {
            ContentResolver.SCHEME_FILE -> {
                val path = requireNotNull(uri.path) { "$label file path is missing." }
                FileInputStream(File(path))
            }
            else -> resolver.openInputStream(uri)
        } ?: throw IllegalArgumentException("Android could not open the $label.")

        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > maximum) throw IllegalArgumentException("$label exceeds the ${maximum / (1024 * 1024)} MiB safety limit.")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        outputRoot.mkdirs()
        val temporary = File(outputRoot, ".${target.name}.tmp")
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
        when (uri.scheme?.lowercase()) {
            ContentResolver.SCHEME_FILE -> File(uri.path ?: return@runCatching null).name
            else -> resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }.getOrNull()

    private fun buildDisplayName(baseName: String, patchName: String): String {
        val base = baseName.substringBeforeLast('.').ifBlank { "patched-game" }
        val patch = patchName.substringBeforeLast('.').ifBlank { "patch" }
        return "${base.take(72)}-${patch.take(72)}.gba"
    }
}
