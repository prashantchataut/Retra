package app.retra.emulator.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.retra.core.model.CompatibilityStatus
import app.retra.core.model.GameRecord

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["sha256"], unique = true),
        Index(value = ["gameCode"]),
        Index(value = ["crc32"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val title: String,
    val gameCode: String,
    val makerCode: String,
    val softwareVersion: Int,
    val sha256: String,
    val sizeBytes: Long,
    val importedAtEpochMillis: Long,
    val lastPlayedAtEpochMillis: Long? = null,
    val compatibility: String = CompatibilityStatus.UNKNOWN.name,
    val origin: String = "LOCAL_IMPORT",
    val baseSha256: String? = null,
    val patchSha256: String? = null,
    val patchFormat: String? = null,
    val patchDisplayName: String? = null,
    val creator: String? = null,
    val sourceUrl: String? = null,
    val license: String? = null,
    val distributionPermission: String? = null,
    val favorite: Boolean = false,
    val notes: String? = null,
    val coverArtPath: String? = null,
    val crc32: Long? = null,
    val managedPath: String? = null,
    val collectionsCsv: String = "",
    val tagsCsv: String = ""
) {
    fun toRecord(): GameRecord = GameRecord(
        id = id,
        uri = uri,
        displayName = displayName,
        title = title,
        gameCode = gameCode,
        makerCode = makerCode,
        softwareVersion = softwareVersion,
        sha256 = sha256,
        sizeBytes = sizeBytes,
        importedAtEpochMillis = importedAtEpochMillis,
        lastPlayedAtEpochMillis = lastPlayedAtEpochMillis,
        compatibility = runCatching { CompatibilityStatus.valueOf(compatibility) }
            .getOrDefault(CompatibilityStatus.UNKNOWN),
        origin = origin,
        baseSha256 = baseSha256,
        patchSha256 = patchSha256,
        patchFormat = patchFormat,
        patchDisplayName = patchDisplayName,
        creator = creator,
        sourceUrl = sourceUrl,
        license = license,
        distributionPermission = distributionPermission,
        favorite = favorite,
        notes = notes,
        coverArtPath = coverArtPath,
        crc32 = crc32,
        managedPath = managedPath,
        collections = decodeCsv(collectionsCsv),
        tags = decodeCsv(tagsCsv)
    )

    companion object {
        fun encodeCsv(values: List<String>): String = values
            .map { it.trim().take(40) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(20)
            .joinToString("|")

        fun decodeCsv(value: String): List<String> = value
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
