package app.retra.emulator

import app.retra.core.patching.InvalidPatchException
import app.retra.core.patching.PatchDescriptor
import app.retra.core.patching.PatchEngine
import app.retra.core.patching.PatchFormat
import app.retra.core.rom.GbaRomParser
import app.retra.emulator.data.KnownPatchHints
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
    fun heartAndSoulHintMatchesEmeraldBaseDescriptor() {
        val descriptor = PatchDescriptor(
            format = PatchFormat.UPS,
            sourceSizeBytes = 16_777_216L,
            targetSizeBytes = 33_554_432L,
            sourceCrc32 = 0x1F1C08FBL,
            targetCrc32 = 0x96A8425BL,
            patchCrc32 = 0x39E2A0E4L,
            patchSha256 = "c8e70f448b481d2980266ca8f021aa8b3c462f4e582cf80228ced4636c6154eb",
            patchIntegrityValid = true
        )

        val hint = KnownPatchHints.match(descriptor)
        assertNotNull(hint)
        assertEquals("Pokémon Heart & Soul v1.2.1", hint?.resultTitle)
        assertEquals(16_777_216L, hint?.sourceSize)
        assertEquals(33_554_432L, hint?.targetSize)
        assertEquals(0x1F1C08FBL, hint?.sourceCrc32)
        assertEquals(0x96A8425BL, hint?.targetCrc32)
    }

    @Test
    fun incompatibleBaseRomFailsCrcMatch() {
        val wrongBaseDescriptor = PatchDescriptor(
            format = PatchFormat.UPS,
            sourceSizeBytes = 16_777_216L,
            targetSizeBytes = 33_554_432L,
            sourceCrc32 = 0xDD5E4B2EL, // FireRed CRC32
            targetCrc32 = 0x96A8425BL,
            patchCrc32 = 0x39E2A0E4L,
            patchSha256 = "dummy",
            patchIntegrityValid = true
        )

        val hint = KnownPatchHints.match(wrongBaseDescriptor)
        assertNull(hint)
    }

    @Test
    fun upsPatchEngineAppliesXorAndExpandsRom() {
        val source = createBaseRom("POKEMON EMER", "BPEE", 1024)
        val target = ByteArray(2048)
        source.copyInto(target, 0, 0, source.size)
        // Modify bytes in the target
        target[0x200] = (target[0x200].toInt() xor 0x33).toByte()
        target[0x201] = (target[0x201].toInt() xor 0x77).toByte()
        target[1500] = 0x42

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
