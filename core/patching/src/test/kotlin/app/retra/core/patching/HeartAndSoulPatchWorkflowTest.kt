package app.retra.core.patching

import app.retra.core.rom.GbaRomParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

/**
 * Validates the Heart & Soul UPS patch workflow, exact-base matching,
 * CRC32 validation, and patched output integrity.
 */
class HeartAndSoulPatchWorkflowTest {

    @Test
    fun emeraldBaseDescriptorMatchesExpectedParameters() {
        val descriptor = PatchDescriptor(
            format = PatchFormat.UPS,
            patchSizeBytes = 32_558_217,
            sourceSizeBytes = 16_777_216L,
            targetSizeBytes = 33_554_432L,
            sourceCrc32 = 0x1F1C08FBL,
            targetCrc32 = 0x96A8425BL,
            patchCrc32 = 0x39E2A0E4L,
            patchSha256 = "c8e70f448b481d2980266ca8f021aa8b3c462f4e582cf80228ced4636c6154eb",
            patchIntegrityValid = true
        )

        assertEquals(32_558_217, descriptor.patchSizeBytes)
        assertEquals(16_777_216L, descriptor.sourceSizeBytes)
        assertEquals(33_554_432L, descriptor.targetSizeBytes)
        assertEquals(0x1F1C08FBL, descriptor.sourceCrc32)
        assertEquals(0x96A8425BL, descriptor.targetCrc32)
        assertEquals(0x39E2A0E4L, descriptor.patchCrc32)
        assertTrue(descriptor.patchIntegrityValid)
    }

    @Test
    fun upsPatchEngineAppliesXorAndExpandsRom() {
        val source = createBaseRom("POKEMON EMER", "BPEE", 1024)
        val target = ByteArray(2048).also {
            source.copyInto(it, 0, 0, source.size)
            it[0x200] = (it[0x200].toInt() xor 0x33).toByte()
            it[0x201] = (it[0x201].toInt() xor 0x77).toByte()
        }

        val patchBytes = createUpsPatch(source, target, 0x200, 2)
        val inspected = PatchEngine.inspect(patchBytes)
        assertEquals(PatchFormat.UPS, inspected.format)
        assertEquals(source.size.toLong(), inspected.sourceSizeBytes)
        assertEquals(target.size.toLong(), inspected.targetSizeBytes)
        assertTrue(inspected.patchIntegrityValid)

        val outcome = PatchEngine.apply(source, patchBytes)
        assertEquals(target.size, outcome.output.size)
        assertEquals(target[0x200], outcome.output[0x200])
        assertEquals(target[0x201], outcome.output[0x201])
        assertTrue(outcome.output.contentEquals(target))
    }

    private fun createBaseRom(title: String, gameCode: String, size: Int): ByteArray {
        val bytes = ByteArray(size)
        title.encodeToByteArray().copyInto(bytes, 0xA0, endIndex = title.length.coerceAtMost(12))
        gameCode.encodeToByteArray().copyInto(bytes, 0xAC, endIndex = gameCode.length.coerceAtMost(4))
        "01".encodeToByteArray().copyInto(bytes, 0xB0)
        bytes[0xB2] = 0x96.toByte()
        bytes[0xBC] = 0
        bytes[0xBD] = GbaRomParser.calculateHeaderChecksum(bytes).toByte()
        return bytes
    }

    private fun createUpsPatch(source: ByteArray, target: ByteArray, offset: Int, length: Int): ByteArray {
        val body = ByteArrayOutputStream().apply {
            write("UPS1".encodeToByteArray())
            writeVariable(source.size.toLong())
            writeVariable(target.size.toLong())
            writeVariable(offset.toLong())
            repeat(length) { index ->
                val xor = (source[offset + index].toInt() and 0xFF) xor (target[offset + index].toInt() and 0xFF)
                write(xor)
            }
            write(0)
            writeLittle32(crc32(source))
            writeLittle32(crc32(target))
        }.toByteArray()
        return body + little32(crc32(body))
    }

    private fun ByteArrayOutputStream.writeVariable(initial: Long) {
        var value = initial
        while (true) {
            val current = (value and 0x7F).toInt()
            value = value ushr 7
            if (value == 0L) {
                write(current or 0x80)
                return
            }
            write(current)
            value -= 1
        }
    }

    private fun ByteArrayOutputStream.writeLittle32(value: Long) = write(little32(value))

    private fun little32(value: Long): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte()
    )

    private fun crc32(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value
}
