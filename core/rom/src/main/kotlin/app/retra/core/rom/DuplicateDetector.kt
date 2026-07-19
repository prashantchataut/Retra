
package app.retra.core.rom

object DuplicateDetector {
    fun isDuplicate(candidateSha256: String, existingSha256: Iterable<String>): Boolean {
        val normalized = candidateSha256.trim().lowercase()
        return existingSha256.any { it.trim().lowercase() == normalized }
    }
}
