package app.retra.patcher

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object PatchArchive {
    /** Entries larger than this are ignored (zip-bomb protection). */
    private const val MAX_ENTRY = 256L * 1024 * 1024

    private val PRIORITY = mapOf(
        "bps" to 0, "ups" to 1, "xdelta" to 2, "vcdiff" to 2, "xdelta3" to 2, "delta" to 2, "ips" to 3, "gba" to 4,
    )

    /**
     * Picks the best entry of a zip archive: a patch (.bps/.ups/.xdelta/.vcdiff/.ips) or, failing that,
     * a ROM (.gba). Entries with unknown extensions are used only if their content looks like a patch.
     * Among equal candidates the largest wins. Returns (file name without directories, bytes) or null.
     */
    fun extractSingle(zip: ByteArray): Pair<String, ByteArray>? {
        var best: Triple<Int, String, ByteArray>? = null
        try {
            ZipInputStream(ByteArrayInputStream(zip)).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    val path = e.name.replace('\\', '/')
                    val name = path.substringAfterLast('/')
                    if (e.isDirectory || name.isEmpty() || name.startsWith("._") || path.startsWith("__MACOSX/")) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    var prio = PRIORITY[ext]
                    if (prio == null && e.size > MAX_ENTRY) continue
                    val bytes = readCapped(zin) ?: continue
                    if (prio == null) {
                        prio = if (Patcher.detect(bytes) != null) 5 else continue
                    }
                    val cur = best
                    if (cur == null || prio < cur.first || (prio == cur.first && bytes.size > cur.third.size)) {
                        best = Triple(prio, name, bytes)
                    }
                }
            }
        } catch (e: java.io.IOException) {
            // Corrupt or truncated archive: fall through with whatever we found so far.
        } catch (e: IllegalArgumentException) {
            // Malformed entry names (bad encoding).
        }
        return best?.let { it.second to it.third }
    }

    private fun readCapped(zin: ZipInputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = zin.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
