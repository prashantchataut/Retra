package app.retra.patcher

import java.security.MessageDigest
import java.util.zip.CRC32

object Checksums {
    fun crc32(b: ByteArray): Long = crc32(b, 0, b.size)

    fun crc32(b: ByteArray, off: Int, len: Int): Long {
        val c = CRC32()
        c.update(b, off, len)
        return c.value
    }

    /** CRC32 as 8 uppercase hex digits, e.g. "1F1C08FB". */
    fun crc32Hex(b: ByteArray): String = Msg.hex(crc32(b))

    fun sha1Hex(b: ByteArray): String = digestHex("SHA-1", b)

    fun sha256Hex(b: ByteArray): String = digestHex("SHA-256", b)

    private fun digestHex(alg: String, b: ByteArray): String {
        val d = MessageDigest.getInstance(alg).digest(b)
        val hex = "0123456789abcdef"
        val sb = StringBuilder(d.size * 2)
        for (x in d) {
            sb.append(hex[(x.toInt() shr 4) and 0xF]).append(hex[x.toInt() and 0xF])
        }
        return sb.toString()
    }
}
