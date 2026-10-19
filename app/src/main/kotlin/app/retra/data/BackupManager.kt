package app.retra.data

import app.retra.core.AtomicFiles
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zips all battery saves (+ cheats and the library list for reference) and restores them.
 * Restoring only ever writes `saves/*.sav` and `cheats/*.json`, with sanitised names.
 */
class BackupManager(private val root: File) {
    data class RestoreResult(val saves: Int, val cheats: Int)

    fun backup(out: OutputStream): Int {
        var count = 0
        ZipOutputStream(out).use { zip ->
            File(root, "saves").listFiles { f -> f.isFile && f.name.endsWith(".sav") }?.forEach { f ->
                zip.putNextEntry(ZipEntry("saves/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                count++
            }
            File(root, "cheats").listFiles { f -> f.isFile && f.name.endsWith(".json") }?.forEach { f ->
                zip.putNextEntry(ZipEntry("cheats/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            File(root, "library.json").takeIf { it.exists() }?.let { f ->
                zip.putNextEntry(ZipEntry("library.json"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return count
    }

    private val safeName = Regex("^[A-Za-z0-9._-]{1,96}$")

    fun restore(input: InputStream): RestoreResult {
        var saves = 0
        var cheats = 0
        ZipInputStream(input).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (e.isDirectory) continue
                val parts = e.name.replace('\\', '/').split('/')
                if (parts.size != 2) continue
                val (dir, name) = parts
                if (!safeName.matches(name) || name.startsWith(".")) continue
                val bytes = zin.readNBytesCompat(1024 * 1024 + 1)
                if (bytes.size > 1024 * 1024) continue
                when {
                    dir == "saves" && name.endsWith(".sav") -> { AtomicFiles.write(File(File(root, "saves"), name), bytes); saves++ }
                    dir == "cheats" && name.endsWith(".json") -> { AtomicFiles.write(File(File(root, "cheats"), name), bytes); cheats++ }
                }
            }
        }
        return RestoreResult(saves, cheats)
    }
}
