
package app.retra.core.rom

import java.security.MessageDigest

object Sha256 {
    fun of(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
}
