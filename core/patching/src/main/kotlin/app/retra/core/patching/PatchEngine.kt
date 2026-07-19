package app.retra.core.patching

import app.retra.core.rom.Sha256
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class InvalidPatchException(message: String) : IllegalArgumentException(message)

enum class PatchFormat(val extension: String) {
    IPS("ips"),
    UPS("ups"),
    BPS("bps")
}

data class PatchApplicationResult(
    val format: PatchFormat,
    val output: ByteArray,
    val sourceSha256: String,
    val patchSha256: String,
    val outputSha256: String
)

data class PatchDescriptor(
    val format: PatchFormat,
    val patchSizeBytes: Int,
    val sourceSizeBytes: Long?,
    val targetSizeBytes: Long?,
    val sourceCrc32: Long?,
    val targetCrc32: Long?,
    val patchCrc32: Long?,
    val patchIntegrityValid: Boolean,
    val patchSha256: String
)

object PatchEngine {
    const val MAX_SOURCE_SIZE_BYTES: Int = 64 * 1024 * 1024
    const val MAX_PATCH_SIZE_BYTES: Int = 32 * 1024 * 1024
    const val MAX_OUTPUT_SIZE_BYTES: Int = 64 * 1024 * 1024
    private const val FOOTER_SIZE = 12

    fun detect(patch: ByteArray): PatchFormat = when {
        patch.startsWithAscii("PATCH") -> PatchFormat.IPS
        patch.startsWithAscii("UPS1") -> PatchFormat.UPS
        patch.startsWithAscii("BPS1") -> PatchFormat.BPS
        else -> throw InvalidPatchException("Unsupported patch signature. Retra accepts IPS, UPS, and BPS files.")
    }

    fun inspect(patch: ByteArray): PatchDescriptor {
        if (patch.size > MAX_PATCH_SIZE_BYTES) {
            throw InvalidPatchException("Patch exceeds Retra's 32 MiB safety limit.")
        }
        val format = detect(patch)
        return when (format) {
            PatchFormat.IPS -> PatchDescriptor(
                format = format,
                patchSizeBytes = patch.size,
                sourceSizeBytes = null,
                targetSizeBytes = null,
                sourceCrc32 = null,
                targetCrc32 = null,
                patchCrc32 = null,
                patchIntegrityValid = true,
                patchSha256 = Sha256.of(patch)
            )
            PatchFormat.UPS, PatchFormat.BPS -> {
                if (patch.size < 4 + FOOTER_SIZE) throw InvalidPatchException("Patch is truncated.")
                val patchCrcValid = runCatching {
                    validatePatchCrc(patch)
                    true
                }.getOrDefault(false)
                val cursor = Cursor(patch, 4, patch.size - FOOTER_SIZE)
                val sourceSize = cursor.readVariableInteger()
                val targetSize = cursor.readVariableInteger()
                if (format == PatchFormat.BPS) {
                    val metadataSize = cursor.readVariableInteger().toBoundedInt("BPS metadata size")
                    if (metadataSize > cursor.remaining) throw InvalidPatchException("BPS metadata is truncated.")
                }
                PatchDescriptor(
                    format = format,
                    patchSizeBytes = patch.size,
                    sourceSizeBytes = sourceSize,
                    targetSizeBytes = targetSize,
                    sourceCrc32 = patch.readUnsigned32LittleEndian(patch.size - 12),
                    targetCrc32 = patch.readUnsigned32LittleEndian(patch.size - 8),
                    patchCrc32 = patch.readUnsigned32LittleEndian(patch.size - 4),
                    patchIntegrityValid = patchCrcValid,
                    patchSha256 = Sha256.of(patch)
                )
            }
        }
    }

    fun crc32Of(bytes: ByteArray): Long = crc32(bytes)

    fun apply(source: ByteArray, patch: ByteArray): PatchApplicationResult {
        requireBounded(source, patch)
        val format = detect(patch)
        val output = when (format) {
            PatchFormat.IPS -> applyIps(source, patch)
            PatchFormat.UPS -> applyUps(source, patch)
            PatchFormat.BPS -> applyBps(source, patch)
        }
        if (output.size > MAX_OUTPUT_SIZE_BYTES) {
            throw InvalidPatchException("Patched output exceeds Retra's 64 MiB GBA safety limit.")
        }
        return PatchApplicationResult(
            format = format,
            output = output,
            sourceSha256 = Sha256.of(source),
            patchSha256 = Sha256.of(patch),
            outputSha256 = Sha256.of(output)
        )
    }

    private fun applyIps(source: ByteArray, patch: ByteArray): ByteArray {
        val cursor = Cursor(patch, 5)
        var output = source.copyOf()
        var sawFooter = false
        while (cursor.remaining >= 3) {
            val offset = cursor.readUnsigned24BigEndian()
            if (offset == 0x454F46) {
                sawFooter = true
                break
            }
            val size = cursor.readUnsigned16BigEndian()
            if (size == 0) {
                val runLength = cursor.readUnsigned16BigEndian()
                if (runLength == 0) throw InvalidPatchException("IPS RLE record has zero length.")
                val value = cursor.readUnsignedByte().toByte()
                output = ensureOutputCapacity(output, checkedEnd(offset, runLength))
                output.fill(value, offset, offset + runLength)
            } else {
                val end = checkedEnd(offset, size)
                output = ensureOutputCapacity(output, end)
                cursor.readBytes(size).copyInto(output, offset)
            }
        }
        if (!sawFooter) throw InvalidPatchException("IPS footer is missing.")
        when (cursor.remaining) {
            0 -> Unit
            3 -> {
                val finalSize = cursor.readUnsigned24BigEndian()
                if (finalSize > MAX_OUTPUT_SIZE_BYTES) throw InvalidPatchException("IPS truncate size is too large.")
                output = output.copyOf(finalSize)
            }
            else -> throw InvalidPatchException("IPS patch has unexpected trailing data.")
        }
        return output
    }

    private fun applyUps(source: ByteArray, patch: ByteArray): ByteArray {
        if (patch.size < 4 + FOOTER_SIZE) throw InvalidPatchException("UPS patch is truncated.")
        validatePatchCrc(patch)
        val cursor = Cursor(patch, 4, patch.size - FOOTER_SIZE)
        val sourceSize = cursor.readVariableInteger().toBoundedInt("UPS source size")
        val targetSize = cursor.readVariableInteger().toBoundedInt("UPS target size")
        if (sourceSize != source.size) {
            throw InvalidPatchException("UPS patch expects a $sourceSize-byte base ROM, but the selected ROM is ${source.size} bytes.")
        }
        if (targetSize > MAX_OUTPUT_SIZE_BYTES) throw InvalidPatchException("UPS target is too large.")
        val output = source.copyOf(targetSize)
        var outputOffset = 0
        while (cursor.hasRemaining()) {
            val relative = cursor.readVariableInteger().toBoundedInt("UPS relative offset")
            outputOffset = checkedEnd(outputOffset, relative)
            if (outputOffset >= targetSize) throw InvalidPatchException("UPS record starts outside the target ROM.")
            while (true) {
                if (!cursor.hasRemaining()) throw InvalidPatchException("UPS XOR record is not terminated.")
                val xor = cursor.readUnsignedByte()
                if (xor == 0) {
                    outputOffset = checkedEnd(outputOffset, 1)
                    break
                }
                if (outputOffset >= targetSize) throw InvalidPatchException("UPS record writes outside the target ROM.")
                val sourceByte = if (outputOffset < source.size) source[outputOffset].toInt() and 0xFF else 0
                output[outputOffset] = (sourceByte xor xor).toByte()
                outputOffset++
            }
        }
        validateFooterChecksums(source, output, patch)
        return output
    }

    private fun applyBps(source: ByteArray, patch: ByteArray): ByteArray {
        if (patch.size < 4 + FOOTER_SIZE) throw InvalidPatchException("BPS patch is truncated.")
        validatePatchCrc(patch)
        val cursor = Cursor(patch, 4, patch.size - FOOTER_SIZE)
        val sourceSize = cursor.readVariableInteger().toBoundedInt("BPS source size")
        val targetSize = cursor.readVariableInteger().toBoundedInt("BPS target size")
        val metadataSize = cursor.readVariableInteger().toBoundedInt("BPS metadata size")
        if (sourceSize != source.size) {
            throw InvalidPatchException("BPS patch expects a $sourceSize-byte base ROM, but the selected ROM is ${source.size} bytes.")
        }
        if (targetSize > MAX_OUTPUT_SIZE_BYTES) throw InvalidPatchException("BPS target is too large.")
        if (metadataSize > cursor.remaining) throw InvalidPatchException("BPS metadata is truncated.")
        cursor.skip(metadataSize)

        val output = ByteArray(targetSize)
        var outputOffset = 0
        var sourceRelativeOffset = 0
        var targetRelativeOffset = 0
        while (cursor.hasRemaining()) {
            val actionAndLength = cursor.readVariableInteger()
            val action = (actionAndLength and 3L).toInt()
            val lengthLong = (actionAndLength ushr 2) + 1L
            if (lengthLong > Int.MAX_VALUE) throw InvalidPatchException("BPS action length is too large.")
            val length = lengthLong.toInt()
            if (length > targetSize - outputOffset) throw InvalidPatchException("BPS action writes outside the target ROM.")
            when (action) {
                0 -> {
                    if (length > source.size - outputOffset) throw InvalidPatchException("BPS SourceRead exceeds the base ROM.")
                    source.copyInto(output, outputOffset, outputOffset, outputOffset + length)
                    outputOffset += length
                }
                1 -> {
                    cursor.readBytes(length).copyInto(output, outputOffset)
                    outputOffset += length
                }
                2 -> {
                    sourceRelativeOffset = checkedRelativeOffset(
                        sourceRelativeOffset,
                        decodeSignedOffset(cursor.readVariableInteger()),
                        "BPS source copy"
                    )
                    if (length > source.size - sourceRelativeOffset) throw InvalidPatchException("BPS SourceCopy exceeds the base ROM.")
                    source.copyInto(output, outputOffset, sourceRelativeOffset, sourceRelativeOffset + length)
                    sourceRelativeOffset += length
                    outputOffset += length
                }
                3 -> {
                    targetRelativeOffset = checkedRelativeOffset(
                        targetRelativeOffset,
                        decodeSignedOffset(cursor.readVariableInteger()),
                        "BPS target copy"
                    )
                    repeat(length) {
                        if (targetRelativeOffset !in 0 until outputOffset) {
                            throw InvalidPatchException("BPS TargetCopy references output that has not been produced.")
                        }
                        output[outputOffset++] = output[targetRelativeOffset++]
                    }
                }
            }
        }
        if (outputOffset != targetSize) {
            throw InvalidPatchException("BPS actions produced $outputOffset bytes instead of the declared $targetSize bytes.")
        }
        validateFooterChecksums(source, output, patch)
        return output
    }

    private fun validateFooterChecksums(source: ByteArray, output: ByteArray, patch: ByteArray) {
        val sourceCrc = patch.readUnsigned32LittleEndian(patch.size - 12)
        val targetCrc = patch.readUnsigned32LittleEndian(patch.size - 8)
        if (crc32(source) != sourceCrc) throw InvalidPatchException("Patch source CRC does not match the selected base ROM.")
        if (crc32(output) != targetCrc) throw InvalidPatchException("Patched output CRC verification failed.")
    }

    private fun validatePatchCrc(patch: ByteArray) {
        val expected = patch.readUnsigned32LittleEndian(patch.size - 4)
        val actual = crc32(patch, 0, patch.size - 4)
        if (actual != expected) throw InvalidPatchException("Patch file CRC verification failed.")
    }

    private fun requireBounded(source: ByteArray, patch: ByteArray) {
        if (source.size < 0xC0) throw InvalidPatchException("Base ROM is too small to contain a GBA header.")
        if (source.size > MAX_SOURCE_SIZE_BYTES) throw InvalidPatchException("Base ROM exceeds Retra's 64 MiB safety limit.")
        if (patch.size > MAX_PATCH_SIZE_BYTES) throw InvalidPatchException("Patch exceeds Retra's 32 MiB safety limit.")
    }

    private fun ensureOutputCapacity(current: ByteArray, required: Int): ByteArray {
        if (required > MAX_OUTPUT_SIZE_BYTES) throw InvalidPatchException("Patched output exceeds Retra's 64 MiB safety limit.")
        return if (required <= current.size) current else current.copyOf(required)
    }

    private fun checkedEnd(offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset > MAX_OUTPUT_SIZE_BYTES - length) {
            throw InvalidPatchException("Patch offset or length is outside the supported range.")
        }
        return offset + length
    }

    private fun checkedRelativeOffset(current: Int, delta: Long, label: String): Int {
        val result = current.toLong() + delta
        if (result !in 0..MAX_OUTPUT_SIZE_BYTES.toLong()) throw InvalidPatchException("$label offset is outside the supported range.")
        return result.toInt()
    }

    private fun decodeSignedOffset(encoded: Long): Long {
        val magnitude = encoded ushr 1
        return if (encoded and 1L == 0L) magnitude else -magnitude
    }

    private fun Long.toBoundedInt(label: String): Int {
        if (this !in 0..Int.MAX_VALUE.toLong()) throw InvalidPatchException("$label is too large.")
        return toInt()
    }

    private fun ByteArray.startsWithAscii(text: String): Boolean {
        val bytes = text.toByteArray(Charsets.US_ASCII)
        return size >= bytes.size && bytes.indices.all { this[it] == bytes[it] }
    }

    private fun ByteArray.readUnsigned32LittleEndian(offset: Int): Long {
        if (offset < 0 || offset > size - 4) throw InvalidPatchException("Patch checksum footer is truncated.")
        return (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun crc32(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Long = CRC32().run {
        update(bytes, offset, length)
        value
    }

    private class Cursor(
        private val bytes: ByteArray,
        start: Int,
        private val endExclusive: Int = bytes.size
    ) {
        private var position = start
        val remaining: Int get() = endExclusive - position

        init {
            if (start !in 0..endExclusive || endExclusive !in 0..bytes.size) {
                throw InvalidPatchException("Patch cursor range is invalid.")
            }
        }

        fun hasRemaining(): Boolean = position < endExclusive

        fun skip(count: Int) {
            requireAvailable(count)
            position += count
        }

        fun readUnsignedByte(): Int {
            requireAvailable(1)
            return bytes[position++].toInt() and 0xFF
        }

        fun readUnsigned16BigEndian(): Int = (readUnsignedByte() shl 8) or readUnsignedByte()

        fun readUnsigned24BigEndian(): Int =
            (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()

        fun readBytes(count: Int): ByteArray {
            requireAvailable(count)
            return bytes.copyOfRange(position, position + count).also { position += count }
        }

        fun readVariableInteger(): Long {
            var data = 0L
            var shift = 1L
            repeat(10) {
                val current = readUnsignedByte()
                val low = current and 0x7F
                if (low != 0 && shift > Long.MAX_VALUE / low) throw InvalidPatchException("Patch variable integer overflows.")
                val addition = low * shift
                if (data > Long.MAX_VALUE - addition) throw InvalidPatchException("Patch variable integer overflows.")
                data += addition
                if (current and 0x80 != 0) return data
                if (shift > (Long.MAX_VALUE ushr 7)) throw InvalidPatchException("Patch variable integer overflows.")
                shift = shift shl 7
                if (data > Long.MAX_VALUE - shift) throw InvalidPatchException("Patch variable integer overflows.")
                data += shift
            }
            throw InvalidPatchException("Patch variable integer is too long.")
        }

        private fun requireAvailable(count: Int) {
            if (count < 0 || count > remaining) throw InvalidPatchException("Patch data is truncated.")
        }
    }
}
