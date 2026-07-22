package app.retra.emulator.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CompatibilityStatus
import app.retra.core.model.GameRecord
import app.retra.core.patching.InvalidPatchException
import app.retra.core.patching.PatchEngine
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
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val metadataRepository: LibretroMetadataRepository
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val libraryRoot = File(context.filesDir, "library-roms")
    private val patchInbox = File(context.filesDir, "patch-inbox")
    private val importMutex = Mutex()

    fun observeGames(): Flow<List<GameRecord>> = gameDao.observeAll().map { list -> list.map(GameEntity::toRecord) }

    suspend fun getById(id: Long): GameRecord? = withContext(Dispatchers.IO) {
        gameDao.getById(id)?.toRecord()
    }

    suspend fun importFile(uri: Uri): ImportOutcome = withContext(Dispatchers.IO) {
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "imported-file"
        val lower = displayName.lowercase()
        val mimeType = resolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase()
        when {
            lower.endsWith(".nds") || mimeType in NDS_MIME_TYPES -> ImportOutcome.Rejected(
                "This is a Nintendo DS (.nds) game. Pokémon HeartGold and SoulSilver are DS titles, while Retra currently plays Game Boy Advance (.gba) games only."
            )
            lower.endsWith(".gba") || mimeType in GBA_MIME_TYPES -> importGba(uri, displayName)
            lower.endsWith(".zip") || mimeType in ZIP_MIME_TYPES -> importZip(uri, displayName)
            lower.endsWith(".ups") || lower.endsWith(".ips") || lower.endsWith(".bps") ||
                mimeType in PATCH_MIME_TYPES -> importPatch(uri, displayName)
            mimeType == "application/octet-stream" -> {
                when (val parsed = importGba(uri, displayName)) {
                    is ImportOutcome.Rejected -> ImportOutcome.Rejected(
                        "Android did not provide a recognizable file type, and the file is not a valid GBA ROM. Retra accepts .gba, .zip, .ups, .ips, and .bps files."
                    )
                    else -> parsed
                }
            }
            else -> ImportOutcome.Rejected("Retra accepts .gba, .zip, .ups, .ips, and .bps files.")
        }
    }

    suspend fun importVerifiedCatalogFile(uri: Uri, entry: CatalogEntry): ImportOutcome = withContext(Dispatchers.IO) {
        val extension = entry.downloadUrl.substringBefore('?').substringAfterLast('.', "").lowercase()
        val displayName = "${entry.title.take(150).ifBlank { entry.id }}.$extension"
        when (extension) {
            "gba" -> {
                val bytes = try {
                    readUriLimited(uri, GbaRomParser.MAX_ROM_SIZE_BYTES)
                } catch (error: Exception) {
                    return@withContext ImportOutcome.Rejected(error.message ?: "The verified catalog file could not be read.")
                }
                importGbaBytes(
                    bytes = bytes,
                    displayName = displayName,
                    origin = "LEGAL_CATALOG",
                    creator = entry.creator,
                    sourceUrl = entry.sourcePageUrl ?: entry.downloadUrl,
                    license = entry.license,
                    distributionPermission = entry.distributionPermission
                )
            }
            "zip" -> importZip(uri, displayName, entry)
            "ups", "ips", "bps" -> importPatch(uri, displayName)
            else -> ImportOutcome.Rejected("The verified catalog file type is unsupported.")
        }
    }

    suspend fun importFolder(treeUri: Uri): FolderImportSummary = withContext(Dispatchers.IO) {
        runCatching { resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext FolderImportSummary(0, 0, 1, emptyList(), false)

        var imported = 0
        var duplicates = 0
        var rejected = 0
        val pending = mutableListOf<PendingPatch>()
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
            } else if (node.isFile) {
                val name = node.name ?: continue
                val lower = name.lowercase()
                if (
                    lower.endsWith(".gba") || lower.endsWith(".zip") ||
                    lower.endsWith(".ups") || lower.endsWith(".ips") || lower.endsWith(".bps") ||
                    lower.endsWith(".nds")
                ) {
                    inspected++
                    when (val outcome = importFile(node.uri)) {
                        is ImportOutcome.Imported -> imported++
                        is ImportOutcome.Duplicate -> duplicates++
                        is ImportOutcome.Rejected -> rejected++
                        is ImportOutcome.PatchDetected -> pending += outcome.pending
                        is ImportOutcome.Batch -> {
                            imported += outcome.imported
                            duplicates += outcome.duplicates
                            rejected += outcome.rejected
                            pending += outcome.pendingPatches
                        }
                    }
                }
            }
        }
        FolderImportSummary(imported, duplicates, rejected, pending, queue.isNotEmpty())
    }

    suspend fun markPlayed(id: Long) = withContext(Dispatchers.IO) {
        gameDao.markPlayed(id, System.currentTimeMillis())
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        gameDao.setFavorite(id, favorite)
    }

    suspend fun updateMetadata(id: Long, title: String, notes: String?) = withContext(Dispatchers.IO) {
        gameDao.updateMetadata(id, title.trim().take(120), notes?.trim()?.take(4_000)?.ifBlank { null })
    }

    suspend fun updateCompatibilityNotebook(id: Long, compatibility: CompatibilityStatus, notes: String?) = withContext(Dispatchers.IO) {
        gameDao.updateCompatibilityNotebook(
            id = id,
            compatibility = compatibility.name,
            notes = notes?.trim()?.replace(Regex("""[\p{Cntrl}&&[^\n\t]]"""), "")?.take(4_000)?.ifBlank { null }
        )
    }

    suspend fun updateOrganization(id: Long, collections: List<String>, tags: List<String>) = withContext(Dispatchers.IO) {
        gameDao.updateOrganization(id, GameEntity.encodeCsv(collections), GameEntity.encodeCsv(tags))
    }

    suspend fun delete(id: Long): DeleteGameResult = withContext(Dispatchers.IO) {
        val entity = gameDao.getById(id) ?: return@withContext DeleteGameResult(false, false, false)
        gameDao.deleteById(id)
        val managed = entity.managedPath?.let(::File)
        val wasManaged = managed != null
        val managedDeleted = managed?.let { file ->
            runCatching {
                val approvedRoots = listOf(libraryRoot, File(context.filesDir, "patched-roms"))
                val canonical = file.canonicalFile
                val approved = approvedRoots.any { root ->
                    canonical.path.startsWith(root.canonicalFile.path + File.separator)
                }
                approved && (!canonical.exists() || canonical.delete())
            }.getOrDefault(false)
        } ?: false
        DeleteGameResult(true, wasManaged, managedDeleted)
    }

    suspend fun discardPendingPatch(pending: PendingPatch): Boolean = withContext(Dispatchers.IO) {
        val path = pending.storedPath ?: return@withContext true
        runCatching {
            val file = File(path).canonicalFile
            val root = patchInbox.canonicalFile
            file.path.startsWith(root.path + File.separator) && (!file.exists() || file.delete())
        }.getOrDefault(false)
    }

    data class DeleteGameResult(
        val removedFromLibrary: Boolean,
        val hadManagedFile: Boolean,
        val managedFileDeleted: Boolean
    )

    suspend fun ensureManaged(game: GameRecord): GameRecord = withContext(Dispatchers.IO) {
        if (!game.managedPath.isNullOrBlank()) return@withContext game
        val entity = gameDao.getById(game.id) ?: return@withContext game
        val bytes = runCatching { readUriLimited(Uri.parse(entity.uri), GbaRomParser.MAX_ROM_SIZE_BYTES) }.getOrNull()
            ?: return@withContext game
        val hash = Sha256.of(bytes)
        if (!hash.equals(entity.sha256, ignoreCase = true)) return@withContext game
        val managed = writeManagedRom(hash, bytes)
        val crc = PatchEngine.crc32Of(bytes)
        val sha1 = Sha1.of(bytes)
        gameDao.updateManagedStorage(entity.id, managed.toURI().toString(), managed.absolutePath, crc, sha1)
        entity.copy(uri = managed.toURI().toString(), managedPath = managed.absolutePath, crc32 = crc, sha1 = sha1).toRecord()
    }

    private suspend fun importGba(uri: Uri, displayName: String): ImportOutcome {
        val bytes = try {
            readUriLimited(uri, GbaRomParser.MAX_ROM_SIZE_BYTES)
        } catch (error: Exception) {
            return ImportOutcome.Rejected(error.message ?: "The selected file could not be read.")
        }
        return importGbaBytes(bytes, displayName, origin = "LOCAL_IMPORT")
    }

    internal suspend fun importGbaBytes(
        bytes: ByteArray,
        displayName: String,
        origin: String,
        creator: String? = null,
        sourceUrl: String? = null,
        license: String? = null,
        distributionPermission: String? = null
    ): ImportOutcome = importMutex.withLock {
        val header = try {
            GbaRomParser.parse(bytes)
        } catch (error: InvalidRomException) {
            return@withLock ImportOutcome.Rejected(error.message ?: "Invalid GBA file.")
        }
        val hash = Sha256.of(bytes)
        if (gameDao.countBySha256(hash) > 0) {
            return@withLock ImportOutcome.Duplicate(header.title)
        }

        val expectedPath = File(libraryRoot, "$hash.gba")
        val existedBeforeImport = expectedPath.exists()
        val managed = try {
            writeManagedRom(hash, bytes)
        } catch (error: Exception) {
            return@withLock ImportOutcome.Rejected(error.message ?: "Retra could not store the imported game safely.")
        }
        val entity = GameEntity(
            uri = managed.toURI().toString(),
            displayName = displayName.take(180),
            title = header.title,
            gameCode = header.gameCode,
            makerCode = header.makerCode,
            softwareVersion = header.softwareVersion,
            sha256 = hash,
            sizeBytes = bytes.size.toLong(),
            importedAtEpochMillis = System.currentTimeMillis(),
            origin = origin,
            creator = creator,
            sourceUrl = sourceUrl,
            license = license,
            distributionPermission = distributionPermission,
            crc32 = PatchEngine.crc32Of(bytes),
            sha1 = Sha1.of(bytes),
            managedPath = managed.absolutePath
        )
        val id = try {
            gameDao.insert(entity)
        } catch (error: Exception) {
            val duplicateNowExists = runCatching { gameDao.countBySha256(hash) > 0 }.getOrDefault(false)
            if (!existedBeforeImport && !duplicateNowExists) runCatching { managed.delete() }
            return@withLock if (duplicateNowExists) {
                ImportOutcome.Duplicate(header.title)
            } else {
                ImportOutcome.Rejected(error.message ?: "Retra could not add the game to its library database.")
            }
        }
        val enriched = metadataRepository.enrich(entity.copy(id = id))
        ImportOutcome.Imported(enriched.toRecord())
    }

    private suspend fun importPatch(uri: Uri, displayName: String): ImportOutcome {
        val bytes = try {
            readUriLimited(uri, PatchEngine.MAX_PATCH_SIZE_BYTES)
        } catch (error: Exception) {
            return ImportOutcome.Rejected(error.message ?: "The patch file could not be read.")
        }
        val descriptor = try {
            PatchEngine.inspect(bytes)
        } catch (error: InvalidPatchException) {
            return ImportOutcome.Rejected(error.message ?: "The patch is invalid.")
        }
        if (!descriptor.patchIntegrityValid && descriptor.format.name != "IPS") {
            return ImportOutcome.Rejected("Patch CRC verification failed.")
        }
        patchInbox.mkdirs()
        val stored = File(patchInbox, "${descriptor.patchSha256}.${descriptor.format.extension}")
        writeAtomically(stored, bytes)
        val hint = KnownPatchHints.match(descriptor)
        return ImportOutcome.PatchDetected(
            PendingPatch(
                uri = Uri.parse(stored.toURI().toString()),
                displayName = displayName.take(180),
                descriptor = descriptor,
                storedPath = stored.absolutePath,
                knownHint = hint?.label
            )
        )
    }

    private suspend fun importZip(uri: Uri, displayName: String, catalogEntry: CatalogEntry? = null): ImportOutcome {
        val stream = openUriStream(uri) ?: return ImportOutcome.Rejected("Android could not open the archive.")
        val gbaEntries = mutableListOf<Pair<String, ByteArray>>()
        val patchEntries = mutableListOf<Pair<String, ByteArray>>()
        var entries = 0
        var totalUncompressed = 0
        stream.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    if (entries > 128) return ImportOutcome.Rejected("Archive contains too many files.")
                    if (entry.isDirectory) continue
                    val name = File(entry.name).name
                    if (name.contains("..") || entry.name.contains("\\")) {
                        return ImportOutcome.Rejected("Archive path traversal was blocked.")
                    }
                    val bytes = zip.readBytesLimited(GbaRomParser.MAX_ROM_SIZE_BYTES)
                    totalUncompressed += bytes.size
                    if (totalUncompressed > 96 * 1024 * 1024) {
                        return ImportOutcome.Rejected("Archive expands beyond Retra's safety limit.")
                    }
                    val lower = name.lowercase()
                    when {
                        lower.endsWith(".gba") -> gbaEntries += name to bytes
                        lower.endsWith(".ups") || lower.endsWith(".ips") || lower.endsWith(".bps") -> patchEntries += name to bytes
                        lower.endsWith(".nds") -> return ImportOutcome.Rejected(
                            "This archive contains a Nintendo DS (.nds) game. Pokémon HeartGold and SoulSilver are DS titles; Retra currently supports Game Boy Advance content only."
                        )
                    }
                }
            }
        }
        if (gbaEntries.isEmpty() && patchEntries.isEmpty()) {
            return ImportOutcome.Rejected("No supported .gba or patch files were found in $displayName.")
        }
        var imported = 0
        var duplicates = 0
        var rejected = 0
        val pending = mutableListOf<PendingPatch>()
        for ((name, bytes) in gbaEntries) {
            when (
                val outcome = importGbaBytes(
                    bytes = bytes,
                    displayName = name,
                    origin = if (catalogEntry == null) "LOCAL_ARCHIVE" else "LEGAL_CATALOG_ARCHIVE",
                    creator = catalogEntry?.creator,
                    sourceUrl = catalogEntry?.sourcePageUrl ?: catalogEntry?.downloadUrl,
                    license = catalogEntry?.license,
                    distributionPermission = catalogEntry?.distributionPermission
                )
            ) {
                is ImportOutcome.Imported -> imported++
                is ImportOutcome.Duplicate -> duplicates++
                is ImportOutcome.Rejected -> rejected++
                else -> rejected++
            }
        }
        for ((name, bytes) in patchEntries) {
            patchInbox.mkdirs()
            val descriptor = try {
                PatchEngine.inspect(bytes)
            } catch (error: InvalidPatchException) {
                rejected++
                continue
            }
            if (!descriptor.patchIntegrityValid && descriptor.format.name != "IPS") {
                rejected++
                continue
            }
            val stored = File(patchInbox, "${descriptor.patchSha256}.${descriptor.format.extension}")
            writeAtomically(stored, bytes)
            pending += PendingPatch(
                uri = Uri.parse(stored.toURI().toString()),
                displayName = name,
                descriptor = descriptor,
                storedPath = stored.absolutePath,
                knownHint = KnownPatchHints.match(descriptor)?.label
            )
        }
        return if (imported == 1 && duplicates == 0 && rejected == 0 && pending.isEmpty()) {
            // Prefer a single Imported result for the common one-ROM archive case.
            val games = gameDao.getBySha256(Sha256.of(gbaEntries.first().second))
            if (games != null) ImportOutcome.Imported(games.toRecord())
            else ImportOutcome.Batch(imported, duplicates, rejected, pending)
        } else {
            ImportOutcome.Batch(imported, duplicates, rejected, pending)
        }
    }

    private fun writeManagedRom(sha256: String, bytes: ByteArray): File {
        libraryRoot.mkdirs()
        val target = File(libraryRoot, "$sha256.gba")
        if (!target.exists()) writeAtomically(target, bytes)
        return target
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

    private fun openUriStream(uri: Uri) = when (uri.scheme?.lowercase()) {
        ContentResolver.SCHEME_FILE -> {
            val path = uri.path ?: return null
            FileInputStream(File(path))
        }
        else -> resolver.openInputStream(uri)
    }

    private fun readUriLimited(uri: Uri, maximum: Int): ByteArray {
        val input = openUriStream(uri) ?: throw IllegalArgumentException("Android could not open the selected file.")
        return input.use { it.readBytesLimited(maximum) }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw InvalidRomException("The selected file is larger than ${limit / (1024 * 1024)} MiB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        val GBA_MIME_TYPES = setOf("application/x-gba-rom", "application/vnd.gba-rom")
        val NDS_MIME_TYPES = setOf("application/x-nintendo-ds-rom", "application/x-nds-rom")
        val ZIP_MIME_TYPES = setOf("application/zip", "application/x-zip-compressed")
        val PATCH_MIME_TYPES = setOf(
            "application/x-ups-patch",
            "application/x-ips-patch",
            "application/x-bps-patch"
        )
    }
}
