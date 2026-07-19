package app.retra.core.emulation

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Crash-conscious file storage used by both save states and suspend snapshots.
 * Writes are staged, flushed, and then atomically moved when the filesystem supports it.
 */
class AtomicSaveStore(
    private val root: File,
    private val backupCount: Int = 3
) {
    init {
        require(backupCount in 0..10)
    }

    fun write(relativePath: String, bytes: ByteArray) {
        val target = resolveSafe(relativePath)
        target.parentFile?.mkdirs()
        rotateBackups(target)
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        try {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun read(relativePath: String): ByteArray? {
        val target = resolveSafe(relativePath)
        return if (target.isFile) target.readBytes() else null
    }

    fun delete(relativePath: String): Boolean = resolveSafe(relativePath).delete()

    fun list(relativeDirectory: String): List<File> {
        val directory = resolveSafe(relativeDirectory)
        return directory.listFiles()?.filter(File::isFile)?.sortedByDescending(File::lastModified).orEmpty()
    }

    private fun rotateBackups(target: File) {
        if (!target.exists() || backupCount == 0) return
        for (index in backupCount downTo 2) {
            val previous = File(target.parentFile, "${target.name}.bak${index - 1}")
            val next = File(target.parentFile, "${target.name}.bak$index")
            if (previous.exists()) previous.copyTo(next, overwrite = true)
        }
        target.copyTo(File(target.parentFile, "${target.name}.bak1"), overwrite = true)
    }

    private fun resolveSafe(relativePath: String): File {
        require(relativePath.isNotBlank())
        val canonicalRoot = root.canonicalFile
        val resolved = File(canonicalRoot, relativePath).canonicalFile
        require(resolved.path == canonicalRoot.path || resolved.path.startsWith(canonicalRoot.path + File.separator)) {
            "Save path escapes the configured root."
        }
        return resolved
    }
}
