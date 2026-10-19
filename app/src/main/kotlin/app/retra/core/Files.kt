package app.retra.core

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Crash-safe file helpers: write to a temp file, fsync, then rename over the target. */
object AtomicFiles {
    @Throws(IOException::class)
    fun write(target: File, data: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(data)
            out.flush()
            runCatching { out.fd.sync() }
        }
        if (!tmp.renameTo(target)) {
            // Some filesystems refuse rename-over; fall back to delete + rename.
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("Could not replace ${target.name}")
            }
        }
    }

    fun writeText(target: File, text: String) = write(target, text.toByteArray(Charsets.UTF_8))
}

fun File.sizeRecursive(): Long = if (isFile) length() else (listFiles()?.sumOf { it.sizeRecursive() } ?: 0L)

/** Human readable byte size, e.g. "16 MB", "512 KB". */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(java.util.Locale.ROOT, bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> {
        val mb = bytes / (1024.0 * 1024)
        if (mb >= 10 || mb % 1.0 == 0.0) "%.0f MB".format(java.util.Locale.ROOT, mb) else "%.1f MB".format(java.util.Locale.ROOT, mb)
    }
    bytes >= 1024 -> "%.0f KB".format(java.util.Locale.ROOT, bytes / 1024.0)
    else -> "$bytes B"
}

/** "3 h 12 min", "45 min", "Under a minute". */
fun formatPlaytime(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> "Under a minute"
        minutes < 60 -> "$minutes min"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "$h h" else "$h h $m min"
        }
    }
}
