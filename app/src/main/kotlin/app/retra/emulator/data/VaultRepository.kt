package app.retra.emulator.data

import android.content.Context
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.emulation.VaultSaveRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root = File(context.filesDir, "emulation")
    private val mutableRecords = MutableStateFlow<List<VaultSaveRecord>>(emptyList())
    val records: StateFlow<List<VaultSaveRecord>> = mutableRecords

    init {
        refresh()
    }

    fun refresh() {
        mutableRecords.value = scanRecords()
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

    private fun scanRecords(): List<VaultSaveRecord> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .maxDepth(4)
            .filter { file -> file.isFile && file.extension == "rsv" && !file.name.startsWith(".") }
            .mapNotNull { file ->
                runCatching {
                    val envelope = SaveEnvelope.decode(file.readBytes())
                    val relative = file.relativeTo(root).invariantSeparatorsPath
                    val directoryHash = relative.substringBefore('/')
                    require(directoryHash.equals(envelope.gameSha256, ignoreCase = true))
                    VaultSaveRecord(
                        relativePath = relative,
                        kind = envelope.kind,
                        gameSha256 = envelope.gameSha256,
                        coreId = envelope.coreId,
                        coreVersion = envelope.coreVersion,
                        slot = envelope.slot,
                        createdAtEpochMillis = envelope.createdAtEpochMillis,
                        sizeBytes = file.length()
                    )
                }.getOrNull()
            }
            .sortedByDescending(VaultSaveRecord::createdAtEpochMillis)
            .toList()
    }

    private fun resolveSafe(relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val resolved = File(canonicalRoot, relativePath).canonicalFile
        require(resolved.path.startsWith(canonicalRoot.path + File.separator)) { "Vault path escapes internal storage." }
        return resolved
    }
}
