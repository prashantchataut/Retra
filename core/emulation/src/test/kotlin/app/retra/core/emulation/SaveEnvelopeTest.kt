package app.retra.core.emulation

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveEnvelopeTest {
    @Test
    fun roundTripPreservesIdentityAndPayload() {
        val original = SaveEnvelope(SaveKind.STATE, "a".repeat(64), "test-core", "1.0", 2, 42L, byteArrayOf(1, 2, 3))
        val decoded = SaveEnvelope.decode(original.encode())
        assertEquals(original.kind, decoded.kind)
        assertEquals(original.gameSha256, decoded.gameSha256)
        assertEquals(original.slot, decoded.slot)
        assertArrayEquals(original.payload, decoded.payload)
    }

    @Test
    fun payloadCorruptionIsRejected() {
        val bytes = SaveEnvelope(SaveKind.SUSPEND, "b".repeat(64), "test-core", "1.0", -1, 42L, byteArrayOf(1, 2, 3)).encode()
        bytes[bytes.lastIndex] = 8
        assertThrows(IllegalArgumentException::class.java) { SaveEnvelope.decode(bytes) }
    }
}
