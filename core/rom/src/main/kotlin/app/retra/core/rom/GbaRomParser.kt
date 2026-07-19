
package app.retra.core.rom

import app.retra.core.model.RomHeader
import java.io.InputStream

class InvalidRomException(message: String) : IllegalArgumentException(message)

object GbaRomParser {
    const val MIN_HEADER_SIZE: Int = 0xC0
    const val MAX_ROM_SIZE_BYTES: Int = 64 * 1024 * 1024

    fun parse(input: InputStream): RomHeader = parse(input.readBytesLimited(MAX_ROM_SIZE_BYTES))

    fun parse(bytes: ByteArray): RomHeader {
        if (bytes.size < MIN_HEADER_SIZE) {
            throw InvalidRomException("The file is too small to contain a GBA header.")
        }
        if (bytes.size > MAX_ROM_SIZE_BYTES) {
            throw InvalidRomException("The file exceeds Retra's 64 MiB import safety limit.")
        }

        val fixedValueValid = bytes[0xB2].toInt() and 0xFF == 0x96
        if (!fixedValueValid) {
            throw InvalidRomException("The GBA fixed-value byte is invalid.")
        }

        val title = bytes.ascii(0xA0, 12).ifBlank { "Untitled GBA Game" }
        val gameCode = bytes.ascii(0xAC, 4)
        val makerCode = bytes.ascii(0xB0, 2)
        val version = bytes[0xBC].toInt() and 0xFF
        val storedChecksum = bytes[0xBD].toInt() and 0xFF
        val calculatedChecksum = calculateHeaderChecksum(bytes)

        return RomHeader(
            title = title,
            gameCode = gameCode,
            makerCode = makerCode,
            softwareVersion = version,
            fixedValueValid = fixedValueValid,
            headerChecksumValid = storedChecksum == calculatedChecksum
        )
    }

    fun calculateHeaderChecksum(bytes: ByteArray): Int {
        if (bytes.size < MIN_HEADER_SIZE) {
            throw InvalidRomException("The file is too small to calculate a GBA header checksum.")
        }
        var sum = 0
        for (index in 0xA0..0xBC) {
            sum += bytes[index].toInt() and 0xFF
        }
        return (-(sum + 0x19)) and 0xFF
    }

    private fun ByteArray.ascii(offset: Int, length: Int): String {
        return copyOfRange(offset, offset + length)
            .takeWhile { it.toInt() != 0 }
            .map { byte ->
                val value = byte.toInt() and 0xFF
                if (value in 32..126) value.toChar() else ' '
            }
            .joinToString("")
            .trim()
    }

    private fun InputStream.readBytesLimited(limit: Int): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) {
                throw InvalidRomException("The file exceeds Retra's 64 MiB import safety limit.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
