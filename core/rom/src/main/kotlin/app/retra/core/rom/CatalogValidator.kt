
package app.retra.core.rom

import app.retra.core.model.CatalogEntry
import app.retra.core.model.CatalogManifest
import java.net.URI

sealed interface CatalogValidationResult {
    data object Valid : CatalogValidationResult
    data class Invalid(val reasons: List<String>) : CatalogValidationResult
}

object CatalogValidator {
    private const val MAX_DOWNLOAD_SIZE = 64L * 1024L * 1024L
    private val sha256Pattern = Regex("^[0-9a-fA-F]{64}$")
    private val safeIdPattern = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$")

    fun validate(manifest: CatalogManifest): CatalogValidationResult {
        val reasons = mutableListOf<String>()
        if (manifest.catalogVersion != 1) reasons += "Unsupported catalog version."
        if (!safeIdPattern.matches(manifest.catalogId)) reasons += "Catalog ID must use 1-80 safe ASCII characters."
        if (manifest.name.isBlank()) reasons += "Catalog name is required."
        if (manifest.owner.isBlank()) reasons += "Catalog owner is required."
        if (manifest.contentPolicy != "AUTHORIZED_ONLY") {
            reasons += "Catalog contentPolicy must be AUTHORIZED_ONLY."
        }
        if (!manifest.sourceUrl.isHttps()) reasons += "Catalog source URL must use HTTPS."
        if (manifest.games.map { it.id }.distinct().size != manifest.games.size) {
            reasons += "Catalog contains duplicate game IDs."
        }
        manifest.games.forEach { entry -> reasons += validateEntry(entry) }
        return if (reasons.isEmpty()) CatalogValidationResult.Valid else CatalogValidationResult.Invalid(reasons)
    }

    private fun validateEntry(entry: CatalogEntry): List<String> {
        val reasons = mutableListOf<String>()
        val prefix = "${entry.id.ifBlank { "<missing-id>" }}: "
        if (!safeIdPattern.matches(entry.id)) reasons += prefix + "ID must use 1-80 safe ASCII characters."
        if (entry.title.isBlank()) reasons += prefix + "title is required."
        if (entry.creator.isBlank()) reasons += prefix + "creator is required."
        if (entry.license.isBlank()) reasons += prefix + "license is required."
        if (entry.distributionPermission.isBlank()) {
            reasons += prefix + "distribution permission is required."
        }
        if (!entry.downloadUrl.isHttps()) reasons += prefix + "download URL must use HTTPS."
        if (!entry.downloadUrl.substringBefore('?').lowercase().endsWith(".gba")) {
            reasons += prefix + "download URL must identify a .gba file."
        }
        if (entry.fileSize <= 0 || entry.fileSize > MAX_DOWNLOAD_SIZE) {
            reasons += prefix + "file size must be between 1 byte and 64 MiB."
        }
        if (!sha256Pattern.matches(entry.sha256)) reasons += prefix + "SHA-256 is invalid."
        return reasons
    }

    private fun String.isHttps(): Boolean = runCatching {
        val uri = URI(this)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
