package app.retra.emulator.data

import android.content.Context
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.emulation.SaveKind
import app.retra.core.emulation.VaultSaveRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VaultHealthSummary(
    val readableRecords: Int = 0,
    val corruptedRecords: Int = 0,
    val backupFiles: Int = 0,
    val timelineSnapshots: Int = 0,
    val totalBytes: Long = 0,
    val latestWriteAtEpochMillis: Long? = null
)

data class SaveTimelineEntry(
    val id: String,
    val gameSha256: String,
    val title: String,
    val kind: SaveKind,
    val slot: Int,
    val createdAtEpochMillis: Long,
    val coreId: String,
    val coreVersion: String,
    val sizeBytes: Long,
    val snapshotRelativePath: String,
    val restoreTargetRelativePath: String,
    val cheatsActive: Boolean,
    val screenshotPath: String? = null,
    val automatic: Boolean = false
)

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root = File(context.filesDir, "emulation")
    private val saveStore = AtomicSaveStore(root)
    private val mutableRecords = MutableStateFlow<List<VaultSaveRecord>>(emptyList())
    val records: StateFlow<List<VaultSaveRecord>> = mutableRecords
    private val mutableTimeline = MutableStateFlow<Map<String, List<SaveTimelineEntry>>>(emptyMap())
    val timeline: StateFlow<Map<String, List<SaveTimelineEntry>>> = mutableTimeline
    private val mutableHealth = MutableStateFlow(VaultHealthSummary())
    val health: StateFlow<VaultHealthSummary> = mutableHealth

    init {
        refresh()
    }

    fun refresh() {
        val scan = scanRecords()
        mutableRecords.value = scan.records
        mutableTimeline.value = scan.timeline.groupBy(SaveTimelineEntry::gameSha256)
            .mapValues { (_, entries) -> entries.sortedByDescending(SaveTimelineEntry::createdAtEpochMillis) }
        mutableHealth.value = scan.health
    }

    fun delete(record: VaultSaveRecord): Boolean {
        val file = resolveSafe(record.relativePath)
        val deleted = file.isFile && file.delete()
        if (deleted) {
            file.parentFile?.listFiles()?.filter { it.name.startsWith(file.name + ".bak") }?.forEach(File::delete)
        }
        refresh()
        return deleted
    }

    fun restorePrevious(record: VaultSaveRecord): Boolean {
        val target = resolveSafe(record.relativePath)
        val previous = (1..10)
            .map { File(target.parentFile, "${target.name}.bak$it") }
            .firstOrNull { backup ->
                backup.isFile && runCatching {
                    val envelope = SaveEnvelope.decode(backup.readBytes())
                    envelope.gameSha256.equals(record.gameSha256, true) &&
                        envelope.kind == record.kind && envelope.slot == record.slot
                }.getOrDefault(false)
            } ?: return false
        saveStore.write(record.relativePath, previous.readBytes())
        refresh()
        return true
    }

    /** Freezes the current save into a separate, named timeline file before later writes can rotate it away. */
    fun createTimelineSnapshot(
        record: VaultSaveRecord,
        title: String,
        cheatsActive: Boolean,
        screenshotPath: String? = null,
        automatic: Boolean = false
    ): Result<SaveTimelineEntry> = runCatching {
        val source = resolveSafe(record.relativePath)
        require(source.isFile) { "The selected save no longer exists." }
        val bytes = source.readBytes()
        val envelope = SaveEnvelope.decode(bytes)
        require(envelope.gameSha256.equals(record.gameSha256, ignoreCase = true))
        val timestamp = System.currentTimeMillis()
        val nonce = System.nanoTime().toString(36)
        val id = "$timestamp-$nonce-${record.kind.name.lowercase()}-${record.slot}"
        val relative = "${record.gameSha256.lowercase()}/timeline/$id.rsv"
        val target = resolveSafe(relative)
        writeFileAtomically(target, bytes)
        val entry = SaveTimelineEntry(
            id = id,
            gameSha256 = record.gameSha256.lowercase(),
            title = sanitizeTitle(title, automatic),
            kind = envelope.kind,
            slot = envelope.slot,
            createdAtEpochMillis = timestamp,
            coreId = envelope.coreId,
            coreVersion = envelope.coreVersion,
            sizeBytes = bytes.size.toLong(),
            snapshotRelativePath = relative,
            restoreTargetRelativePath = canonicalSavePath(envelope),
            cheatsActive = cheatsActive,
            screenshotPath = screenshotPath?.takeIf { it.length <= 1024 },
            automatic = automatic
        )
        writeTimelineMetadata(entry)
        pruneTimeline(entry.gameSha256)
        refresh()
        entry
    }

    fun restoreTimeline(entry: SaveTimelineEntry): Result<Unit> = runCatching {
        val snapshot = resolveSafe(entry.snapshotRelativePath)
        require(snapshot.isFile) { "The timeline snapshot is missing." }
        val bytes = snapshot.readBytes()
        val envelope = SaveEnvelope.decode(bytes)
        require(envelope.gameSha256.equals(entry.gameSha256, ignoreCase = true))
        require(envelope.kind == entry.kind && envelope.slot == entry.slot)
        require(canonicalSavePath(envelope) == entry.restoreTargetRelativePath)
        // AtomicSaveStore rotates the current target first, so rollback itself is reversible.
        saveStore.write(entry.restoreTargetRelativePath, bytes)
        refresh()
    }

    fun deleteTimeline(entry: SaveTimelineEntry): Boolean {
        val snapshot = resolveSafe(entry.snapshotRelativePath)
        val metadata = File(snapshot.parentFile, "${snapshot.name}.meta")
        val deleted = snapshot.delete()
        if (deleted) metadata.delete()
        refresh()
        return deleted
    }

    fun timelineFor(gameSha256: String): List<SaveTimelineEntry> =
        mutableTimeline.value[gameSha256.lowercase()].orEmpty()

    private fun scanRecords(): VaultScan {
        if (!root.isDirectory) return VaultScan(emptyList(), emptyList(), VaultHealthSummary())
        val currentFiles = root.walkTopDown()
            .maxDepth(4)
            .filter { file ->
                file.isFile && file.extension == "rsv" && !file.name.startsWith(".") &&
                    "/timeline/" !in file.invariantSeparatorsPath
            }
            .toList()
        val timelineFiles = root.walkTopDown()
            .maxDepth(5)
            .filter { file -> file.isFile && file.extension == "rsv" && "/timeline/" in file.invariantSeparatorsPath }
            .toList()
        val backupFiles = root.walkTopDown()
            .maxDepth(4)
            .count { file -> file.isFile && BACKUP_NAME.matches(file.name) }
        var corrupted = 0
        var totalBytes = 0L
        val records = currentFiles.mapNotNull { file ->
            totalBytes += file.length()
            runCatching {
                val envelope = SaveEnvelope.decode(file.readBytes())
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val directoryHash = relative.substringBefore('/')
                require(directoryHash.equals(envelope.gameSha256, ignoreCase = true))
                val backups = file.parentFile?.listFiles()?.count { it.isFile && it.name.matches(Regex("${Regex.escape(file.name)}\\.bak[1-9][0-9]?")) } ?: 0
                VaultSaveRecord(
                    relativePath = relative,
                    kind = envelope.kind,
                    gameSha256 = envelope.gameSha256,
                    coreId = envelope.coreId,
                    coreVersion = envelope.coreVersion,
                    slot = envelope.slot,
                    createdAtEpochMillis = envelope.createdAtEpochMillis,
                    sizeBytes = file.length(),
                    backupCount = backups
                )
            }.getOrElse {
                corrupted++
                null
            }
        }.sortedByDescending(VaultSaveRecord::createdAtEpochMillis)
        val timeline = timelineFiles.mapNotNull { file ->
            totalBytes += file.length()
            runCatching { readTimelineEntry(file) }.getOrElse {
                corrupted++
                null
            }
        }
        val latest = (records.map(VaultSaveRecord::createdAtEpochMillis) + timeline.map(SaveTimelineEntry::createdAtEpochMillis)).maxOrNull()
        val health = VaultHealthSummary(
            readableRecords = records.size,
            corruptedRecords = corrupted,
            backupFiles = backupFiles,
            timelineSnapshots = timeline.size,
            totalBytes = totalBytes,
            latestWriteAtEpochMillis = latest
        )
        return VaultScan(records, timeline, health)
    }

    private fun readTimelineEntry(file: File): SaveTimelineEntry {
        require(file.length() in 1..MAX_SNAPSHOT_BYTES)
        val envelope = SaveEnvelope.decode(file.readBytes())
        val relative = file.relativeTo(root).invariantSeparatorsPath
        require(relative.substringBefore('/').equals(envelope.gameSha256, ignoreCase = true))
        val meta = readMetadata(File(file.parentFile, "${file.name}.meta"))
        return SaveTimelineEntry(
            id = file.nameWithoutExtension,
            gameSha256 = envelope.gameSha256.lowercase(),
            title = meta["title"]?.let(::decode)?.take(80)?.ifBlank { null } ?: defaultTimelineTitle(envelope),
            kind = envelope.kind,
            slot = envelope.slot,
            createdAtEpochMillis = meta["created"]?.toLongOrNull() ?: envelope.createdAtEpochMillis,
            coreId = envelope.coreId,
            coreVersion = envelope.coreVersion,
            sizeBytes = file.length(),
            snapshotRelativePath = relative,
            restoreTargetRelativePath = canonicalSavePath(envelope),
            cheatsActive = meta["cheats"] == "1",
            screenshotPath = meta["screenshot"]?.let(::decode)?.ifBlank { null },
            automatic = meta["automatic"] == "1"
        )
    }

    private fun writeTimelineMetadata(entry: SaveTimelineEntry) {
        val snapshot = resolveSafe(entry.snapshotRelativePath)
        val metadata = File(snapshot.parentFile, "${snapshot.name}.meta")
        val text = buildString {
            appendLine(META_MAGIC)
            appendLine("title=${encode(entry.title)}")
            appendLine("created=${entry.createdAtEpochMillis}")
            appendLine("cheats=${if (entry.cheatsActive) 1 else 0}")
            appendLine("automatic=${if (entry.automatic) 1 else 0}")
            entry.screenshotPath?.let { appendLine("screenshot=${encode(it)}") }
        }
        writeFileAtomically(metadata, text.toByteArray())
    }

    private fun readMetadata(file: File): Map<String, String> {
        if (!file.isFile || file.length() !in 1..MAX_METADATA_BYTES) return emptyMap()
        val lines = file.readLines()
        if (lines.firstOrNull() != META_MAGIC) return emptyMap()
        return lines.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
        }.toMap()
    }

    private fun pruneTimeline(gameSha256: String) {
        val directory = resolveSafe("${gameSha256.lowercase()}/timeline")
        val entries = directory.listFiles()?.filter { it.isFile && it.extension == "rsv" }
            ?.mapNotNull { file -> runCatching { readTimelineEntry(file) }.getOrNull() }
            ?.sortedByDescending(SaveTimelineEntry::createdAtEpochMillis)
            .orEmpty()
        val automatic = entries.filter(SaveTimelineEntry::automatic)
        val named = entries.filterNot(SaveTimelineEntry::automatic)
        (automatic.drop(MAX_AUTOMATIC_SNAPSHOTS) + named.drop(MAX_NAMED_SNAPSHOTS)).forEach { entry ->
            val file = resolveSafe(entry.snapshotRelativePath)
            file.delete()
            File(file.parentFile, "${file.name}.meta").delete()
        }
    }

    private fun canonicalSavePath(envelope: SaveEnvelope): String = when (envelope.kind) {
        SaveKind.STATE -> "${envelope.gameSha256.lowercase()}/states/slot-${envelope.slot}.rsv"
        SaveKind.SUSPEND -> "${envelope.gameSha256.lowercase()}/suspend/latest.rsv"
        SaveKind.BATTERY -> "${envelope.gameSha256.lowercase()}/battery/game.rsv"
    }

    private fun sanitizeTitle(value: String, automatic: Boolean): String = value.trim()
        .replace(Regex("[\\p{Cntrl}]"), " ")
        .replace(Regex("\\s+"), " ")
        .take(80)
        .ifBlank { if (automatic) "Automatic checkpoint" else "Named checkpoint" }

    private fun defaultTimelineTitle(envelope: SaveEnvelope): String = when (envelope.kind) {
        SaveKind.STATE -> "State slot ${envelope.slot}"
        SaveKind.SUSPEND -> "Suspend checkpoint"
        SaveKind.BATTERY -> "Battery save"
    }

    private fun resolveSafe(relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val resolved = File(canonicalRoot, relativePath).canonicalFile
        require(resolved.path == canonicalRoot.path || resolved.path.startsWith(canonicalRoot.path + File.separator)) {
            "Vault path escapes internal storage."
        }
        return resolved
    }

    private fun writeFileAtomically(target: File, bytes: ByteArray) {
        require(bytes.size.toLong() <= MAX_SNAPSHOT_BYTES)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun decode(value: String): String = Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

    private data class VaultScan(
        val records: List<VaultSaveRecord>,
        val timeline: List<SaveTimelineEntry>,
        val health: VaultHealthSummary
    )

    private companion object {
        const val META_MAGIC = "RETRA-TIMELINE-1"
        const val MAX_METADATA_BYTES = 16 * 1024L
        const val MAX_SNAPSHOT_BYTES = 64L * 1024L * 1024L
        const val MAX_AUTOMATIC_SNAPSHOTS = 20
        const val MAX_NAMED_SNAPSHOTS = 40
        val BACKUP_NAME = Regex(".+\\.rsv\\.bak[1-9][0-9]?")
    }
}
