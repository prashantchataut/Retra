package app.retra.emulator.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.retra.core.cheats.CheatPack
import app.retra.core.cheats.CheatCategory
import app.retra.core.cheats.CheatFormat
import app.retra.core.cheats.CheatPackDownloadRequest
import app.retra.core.cheats.CheatRisk
import app.retra.core.cheats.InvalidCheatPackException
import app.retra.core.cheats.RetraCodesDownloadPolicy
import app.retra.core.cheats.RetraCodesParser
import app.retra.core.model.GameRecord
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class StoredCheatPack(
    val relativePath: String,
    val fileName: String,
    val sha256: String,
    val provider: String,
    val gameSha256: String,
    val cheatCount: Int,
    val importedAtEpochMillis: Long,
    val pack: CheatPack
)

sealed interface CheatPackImportOutcome {
    data class Imported(val stored: StoredCheatPack) : CheatPackImportOutcome
    data class Duplicate(val fileName: String) : CheatPackImportOutcome
    data class Rejected(val reason: String) : CheatPackImportOutcome
}

@Singleton
class CheatRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val root = File(context.filesDir, "cheat-packs")
    private val mutablePacks = MutableStateFlow<Map<String, List<StoredCheatPack>>>(emptyMap())
    val packs: StateFlow<Map<String, List<StoredCheatPack>>> = mutablePacks

    init {
        refresh()
    }

    suspend fun import(game: GameRecord, uri: Uri): CheatPackImportOutcome = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(uri)?.take(160) ?: "Retra Codes pack.rcc"
        val bytes = try {
            readLimited(uri)
        } catch (error: Exception) {
            return@withContext CheatPackImportOutcome.Rejected(error.message ?: "The Retra Codes pack could not be read.")
        }
        val pack = try {
            RetraCodesParser.parse(bytes)
        } catch (error: InvalidCheatPackException) {
            return@withContext CheatPackImportOutcome.Rejected(error.message ?: "The Retra Codes pack is invalid.")
        }
        val match = RetraCodesParser.match(pack, game.sha256, game.gameCode, game.softwareVersion)
        if (!match.compatible) {
            return@withContext CheatPackImportOutcome.Rejected(
                "This Retra Codes pack does not match the selected ROM: ${match.reasons.joinToString(" ")}"
            )
        }
        val hash = Sha256.of(bytes)
        val relative = "${game.sha256.lowercase()}/$hash.rcc"
        val target = resolveSafe(relative)
        if (target.isFile) return@withContext CheatPackImportOutcome.Duplicate(fileName)
        try {
            writeAtomically(target, bytes)
        } catch (error: Exception) {
            return@withContext CheatPackImportOutcome.Rejected(error.message ?: "The Retra Codes pack could not be stored safely.")
        }
        val stored = StoredCheatPack(
            relativePath = relative,
            fileName = fileName,
            sha256 = hash,
            provider = pack.provider,
            gameSha256 = pack.gameSha256,
            cheatCount = pack.cheats.size,
            importedAtEpochMillis = System.currentTimeMillis(),
            pack = pack
        )
        refresh()
        CheatPackImportOutcome.Imported(stored)
    }

    suspend fun createCustom(
        game: GameRecord,
        name: String,
        format: CheatFormat,
        codeLines: List<String>,
        category: CheatCategory = CheatCategory.QUALITY_OF_LIFE,
        risk: CheatRisk = CheatRisk.CAUTION
    ): CheatPackImportOutcome = withContext(Dispatchers.IO) {
        val safeName = name.replace(Regex("[\\r\\n=\\[\\]]"), " ").trim().take(120)
        if (safeName.isBlank()) return@withContext CheatPackImportOutcome.Rejected("Custom cheat name is required.")
        val cleanedCodes = codeLines.map(String::trim).filter(String::isNotEmpty)
        if (cleanedCodes.isEmpty()) return@withContext CheatPackImportOutcome.Rejected("Enter at least one code line.")
        val id = "custom-${System.currentTimeMillis().toString(36)}"
        val text = buildString {
            appendLine("RETRA-CODES-1")
            appendLine("provider=User Custom")
            appendLine("gameSha256=${game.sha256.lowercase()}")
            if (game.gameCode.length == 4) appendLine("gameCode=${game.gameCode.uppercase()}")
            appendLine("revision=${game.softwareVersion}")
            appendLine()
            appendLine("[cheat]")
            appendLine("id=$id")
            appendLine("name=$safeName")
            appendLine("description=User-created code bound to this exact ROM hash.")
            appendLine("category=${category.name}")
            appendLine("format=${format.name}")
            appendLine("risk=${risk.name}")
            cleanedCodes.forEach { appendLine("code=$it") }
            appendLine("[/cheat]")
        }
        storeBytes(game, "$safeName.rcc", text.encodeToByteArray())
    }

    suspend fun importFromUrl(
        game: GameRecord,
        url: String,
        expectedSha256: String
    ): CheatPackImportOutcome = withContext(Dispatchers.IO) {
        val request = CheatPackDownloadRequest(url.trim(), expectedSha256.trim())
        val origin = try {
            RetraCodesDownloadPolicy.validate(request)
        } catch (error: Exception) {
            return@withContext CheatPackImportOutcome.Rejected(error.message ?: "Invalid cheat-pack URL.")
        }
        var current = origin
        val visited = linkedSetOf(origin)
        repeat(4) { redirectIndex ->
            val connection = current.toURL().openConnection() as? HttpsURLConnection
                ?: return@withContext CheatPackImportOutcome.Rejected("Cheat-pack URL did not create an HTTPS connection.")
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "text/plain, application/octet-stream")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("User-Agent", "Retra/0.4.0 Android")
                val code = connection.responseCode
                if (code in setOf(301, 302, 303, 307, 308)) {
                    if (redirectIndex == 3) return@withContext CheatPackImportOutcome.Rejected("Cheat-pack download exceeded three redirects.")
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext CheatPackImportOutcome.Rejected("Cheat-pack redirect omitted Location.")
                    current = try {
                        RetraCodesDownloadPolicy.validateRedirect(origin, current, location, visited)
                    } catch (error: Exception) {
                        return@withContext CheatPackImportOutcome.Rejected(error.message ?: "Unsafe cheat-pack redirect.")
                    }
                    visited += current
                    return@repeat
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    return@withContext CheatPackImportOutcome.Rejected("Cheat-pack server returned HTTP $code.")
                }
                val length = connection.contentLengthLong
                if (length > RetraCodesParser.MAX_PACK_SIZE_BYTES) {
                    return@withContext CheatPackImportOutcome.Rejected("Cheat-pack response exceeds 512 KiB.")
                }
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > RetraCodesParser.MAX_PACK_SIZE_BYTES) throw IllegalArgumentException("Cheat-pack response exceeds 512 KiB.")
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
                    return@withContext CheatPackImportOutcome.Rejected("Downloaded cheat-pack SHA-256 does not match the expected hash.")
                }
                return@withContext storeBytes(game, current.path.substringAfterLast('/').ifBlank { "online-pack.rcc" }, bytes)
            } catch (error: Exception) {
                return@withContext CheatPackImportOutcome.Rejected(error.message ?: "Cheat-pack download failed.")
            } finally {
                connection.disconnect()
            }
        }
        CheatPackImportOutcome.Rejected("Cheat-pack redirect handling failed.")
    }

    private fun storeBytes(game: GameRecord, fileName: String, bytes: ByteArray): CheatPackImportOutcome {
        val pack = try {
            RetraCodesParser.parse(bytes)
        } catch (error: InvalidCheatPackException) {
            return CheatPackImportOutcome.Rejected(error.message ?: "The Retra Codes pack is invalid.")
        }
        val match = RetraCodesParser.match(pack, game.sha256, game.gameCode, game.softwareVersion)
        if (!match.compatible) {
            return CheatPackImportOutcome.Rejected("This Retra Codes pack does not match the selected ROM: ${match.reasons.joinToString(" ")}")
        }
        val hash = Sha256.of(bytes)
        val relative = "${game.sha256.lowercase()}/$hash.rcc"
        val target = resolveSafe(relative)
        if (target.isFile) return CheatPackImportOutcome.Duplicate(fileName)
        return try {
            writeAtomically(target, bytes)
            val stored = StoredCheatPack(
                relativePath = relative,
                fileName = fileName.take(160),
                sha256 = hash,
                provider = pack.provider,
                gameSha256 = pack.gameSha256,
                cheatCount = pack.cheats.size,
                importedAtEpochMillis = System.currentTimeMillis(),
                pack = pack
            )
            refresh()
            CheatPackImportOutcome.Imported(stored)
        } catch (error: Exception) {
            CheatPackImportOutcome.Rejected(error.message ?: "The Retra Codes pack could not be stored safely.")
        }
    }

    fun refresh() {
        mutablePacks.value = scan().groupBy { it.gameSha256.lowercase() }
    }

    fun delete(stored: StoredCheatPack): Boolean {
        val deleted = resolveSafe(stored.relativePath).delete()
        refresh()
        return deleted
    }

    private fun scan(): List<StoredCheatPack> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .maxDepth(2)
            .filter { it.isFile && it.extension.equals("rcc", ignoreCase = true) }
            .mapNotNull { file ->
                runCatching {
                    val relative = file.relativeTo(root).invariantSeparatorsPath
                    val directoryHash = relative.substringBefore('/')
                    val bytes = file.readBytes()
                    val pack = RetraCodesParser.parse(bytes)
                    require(directoryHash.equals(pack.gameSha256, ignoreCase = true))
                    StoredCheatPack(
                        relativePath = relative,
                        fileName = file.name,
                        sha256 = Sha256.of(bytes),
                        provider = pack.provider,
                        gameSha256 = pack.gameSha256,
                        cheatCount = pack.cheats.size,
                        importedAtEpochMillis = file.lastModified(),
                        pack = pack
                    )
                }.getOrNull()
            }
            .sortedByDescending(StoredCheatPack::importedAtEpochMillis)
            .toList()
    }

    private fun readLimited(uri: Uri): ByteArray {
        val input = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Android could not open the Retra Codes pack.")
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > RetraCodesParser.MAX_PACK_SIZE_BYTES) {
                    throw IllegalArgumentException("Retra Codes pack exceeds 512 KiB.")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
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
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun resolveSafe(relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val resolved = File(canonicalRoot, relativePath).canonicalFile
        require(resolved.path.startsWith(canonicalRoot.path + File.separator)) { "Cheat-pack path escapes internal storage." }
        return resolved
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
