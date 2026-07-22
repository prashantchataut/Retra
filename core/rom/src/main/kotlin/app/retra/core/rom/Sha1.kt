package app.retra.core.rom

import java.io.InputStream
import java.security.MessageDigest

object Sha1 {
    fun of(bytes: ByteArray): String = MessageDigest.getInstance("SHA-1")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    fun of(input: InputStream, maximumBytes: Long = 64L * 1024L * 1024L): String {
        require(maximumBytes > 0) { "maximumBytes must be positive." }
        val digest = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maximumBytes) {
                throw InvalidRomException("The file exceeds Retra's SHA-1 safety limit.")
            }
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
