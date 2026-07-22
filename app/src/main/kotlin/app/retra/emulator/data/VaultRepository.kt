package app.retra.emulator.data

import android.content.Context
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.emulation.VaultSaveRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VaultHealthSummary(
    val readableRecords: Int = 0,
    val corruptedRecords: Int = 0,
    val backupFiles: Int = 0,
    val totalBytes: Long = 0,
    val latestWriteAtEpochMillis: Long? = null
)

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root = File(context.filesDir, "emulation")
    private val saveStore = AtomicSaveStore(root)
    private val mutableRecords = MutableStateFlow<List<VaultSaveRecord>>(emptyList())
    val records: StateFlow<List<VaultSaveRecord>> = mutableRecords
    private val mutableHealth = MutableStateFlow(VaultHealthSummary())
    val health: StateFlow<VaultHealthSummary> = mutableHealth

    init {
        refresh()
    }

    fun refresh() {
        val scan = scanRecords()
        mutableRecords.value = scan.records
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

    private fun scanRecords(): VaultScan {
        if (!root.isDirectory) return VaultScan(emptyList(), VaultHealthSummary())
        val currentFiles = root.walkTopDown()
            .maxDepth(4)
            .filter { file -> file.isFile && file.extension == "rsv" && !file.name.startsWith(".") }
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
        val health = VaultHealthSummary(
            readableRecords = records.size,
            corruptedRecords = corrupted,
            backupFiles = backupFiles,
            totalBytes = totalBytes,
            latestWriteAtEpochMillis = records.firstOrNull()?.createdAtEpochMillis
        )
        return VaultScan(records, health)
    }

    private fun resolveSafe(relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val resolved = File(canonicalRoot, relativePath).canonicalFile
        require(resolved.path.startsWith(canonicalRoot.path + File.separator)) { "Vault path escapes internal storage." }
        return resolved
    }

    private data class VaultScan(val records: List<VaultSaveRecord>, val health: VaultHealthSummary)

    private companion object {
        val BACKUP_NAME = Regex(".+\\.rsv\\.bak[1-9][0-9]?")
    }
}
