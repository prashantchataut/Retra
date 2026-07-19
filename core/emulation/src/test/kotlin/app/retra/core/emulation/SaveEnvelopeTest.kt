package app.retra.core.emulation

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class SaveEnvelopeTest {
    @Test
    fun roundTripPreservesIdentityAndPayload() {
        val original = SaveEnvelope(SaveKind.STATE, "a".repeat(64), "test-core", "1.0", 2, 42L, byteArrayOf(1, 2, 3))
        val decoded = SaveEnvelope.decode(original.encode())
        assertEquals(original.kind, decoded.kind)
        assertEquals(original.gameSha256, decoded.gameSha256)
        assertEquals(original.slot, decoded.slot)
        assertContentEquals(original.payload, decoded.payload)
    }

    @Test
    fun payloadCorruptionIsRejected() {
        val bytes = SaveEnvelope(SaveKind.SUSPEND, "b".repeat(64), "test-core", "1.0", -1, 42L, byteArrayOf(1, 2, 3)).encode()
        bytes[bytes.lastIndex] = 8
        assertFails { SaveEnvelope.decode(bytes) }
    }
}
