package app.retra.core.patching

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class PatchEngineTest {
    @Test
    fun appliesIpsLiteralAndRle() {
        val source = ByteArray(0x400)
        val patch = byteArrayOf(
            'P'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(), 'C'.code.toByte(), 'H'.code.toByte(),
            0, 2, 0, 0, 3, 1, 2, 3,
            0, 3, 0, 0, 0, 0, 4, 0x7F,
            'E'.code.toByte(), 'O'.code.toByte(), 'F'.code.toByte()
        )
        val output = PatchEngine.apply(source, patch).output
        assertContentEquals(byteArrayOf(1, 2, 3), output.copyOfRange(0x200, 0x203))
        assertContentEquals(ByteArray(4) { 0x7F }, output.copyOfRange(0x300, 0x304))
    }

    @Test
    fun rejectsUnknownSignature() {
        assertFailsWith<InvalidPatchException> { PatchEngine.apply(ByteArray(0xC0), byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun appliesBpsTargetRead() {
        val source = ByteArray(0x400) { it.toByte() }
        val target = source.copyOf().also { it[0x210] = 0x55; it[0x211] = 0x66 }
        assertContentEquals(target, PatchEngine.apply(source, bpsTargetRead(source, target)).output)
    }

    private fun bpsTargetRead(source: ByteArray, target: ByteArray): ByteArray {
        val body = ByteArrayOutputStream().apply {
            write("BPS1".encodeToByteArray())
            writeVariable(source.size.toLong())
            writeVariable(target.size.toLong())
            writeVariable(0)
            writeVariable(((target.size.toLong() - 1) shl 2) or 1)
            write(target)
            writeLittle32(crc32(source))
            writeLittle32(crc32(target))
        }
        return body.toByteArray().let { beforePatchCrc ->
            beforePatchCrc + little32(crc32(beforePatchCrc))
        }
    }

    private fun ByteArrayOutputStream.writeVariable(initial: Long) {
        var value = initial
        while (true) {
            var current = (value and 0x7F).toInt()
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
    private fun little32(value: Long) = byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte())
    private fun crc32(bytes: ByteArray) = CRC32().apply { update(bytes) }.value
}
