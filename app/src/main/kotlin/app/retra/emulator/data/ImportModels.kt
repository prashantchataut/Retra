package app.retra.emulator.data

import android.net.Uri
import app.retra.core.model.GameRecord
import app.retra.core.patching.PatchDescriptor
import app.retra.core.patching.PatchFormat

data class PendingPatch(
    val uri: Uri,
    val displayName: String,
    val descriptor: PatchDescriptor,
    val storedPath: String?,
    val knownHint: String? = null
)

sealed interface ImportOutcome {
    data class Imported(val game: GameRecord) : ImportOutcome
    data class Duplicate(val title: String) : ImportOutcome
    data class Batch(
        val imported: Int,
        val duplicates: Int,
        val rejected: Int,
        val pendingPatches: List<PendingPatch> = emptyList()
    ) : ImportOutcome
    data class PatchDetected(val pending: PendingPatch) : ImportOutcome
    data class Rejected(val reason: String) : ImportOutcome
}

data class FolderImportSummary(
    val imported: Int,
    val duplicates: Int,
    val rejected: Int,
    val pendingPatches: List<PendingPatch> = emptyList(),
    val limitReached: Boolean
)

object KnownPatchHints {
    data class Hint(
        val label: String,
        val resultTitle: String,
        val sourceSize: Long,
        val sourceCrc32: Long,
        val targetSize: Long,
        val targetCrc32: Long,
        val patchCrc32: Long
    )

    val HEART_AND_SOUL = Hint(
        label = "Pokémon Emerald Version (U)",
        resultTitle = "Pokémon Heart & Soul",
        sourceSize = 16_777_216L,
        sourceCrc32 = 0x1F1C08FBL,
        targetSize = 33_554_432L,
        targetCrc32 = 0xEF902242L,
        patchCrc32 = 0x58BD22C9L
    )

    fun match(descriptor: PatchDescriptor): Hint? {
        if (descriptor.format != PatchFormat.UPS && descriptor.format != PatchFormat.BPS) return null
        val candidates = listOf(HEART_AND_SOUL)
        return candidates.firstOrNull { hint ->
            descriptor.sourceSizeBytes == hint.sourceSize &&
                descriptor.targetSizeBytes == hint.targetSize &&
                descriptor.sourceCrc32 == hint.sourceCrc32 &&
                descriptor.targetCrc32 == hint.targetCrc32 &&
                (descriptor.patchCrc32 == null || descriptor.patchCrc32 == hint.patchCrc32)
        }
    }
}
