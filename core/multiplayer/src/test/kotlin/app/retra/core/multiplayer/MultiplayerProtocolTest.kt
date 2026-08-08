package app.retra.core.multiplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates multiplayer compatibility gates, packet framing, CRC, and ordered buffering.
 */
class MultiplayerProtocolTest {

    @Test
    fun exactRomAndCoreCompatibilityMatching() {
        val host = MultiplayerCompatibility(
            protocolVersion = 1,
            romSha256 = "a".repeat(64),
            coreId = "mgba-libretro",
            coreVersion = "0.10.5",
            maxPlayers = 2
        )

        val matchingGuest = host.copy()
        assertTrue(MultiplayerCompatibilityGate.compare(host, matchingGuest).compatible)

        val mismatchedGuest = host.copy(romSha256 = "b".repeat(64))
        assertFalse(MultiplayerCompatibilityGate.compare(host, mismatchedGuest).compatible)
    }

    @Test
    fun packetCodecAndOrderedBuffer() {
        val code = RoomCode.normalize("ABC234")
        val packet0 = MultiplayerPacket(MultiplayerPacketType.LINK_DATA, code, 0, 0, byteArrayOf(1, 2))
        val packet1 = MultiplayerPacket(MultiplayerPacketType.LINK_DATA, code, 1, 1, byteArrayOf(3, 4))

        val encoded0 = MultiplayerPacketCodec.encode(packet0)
        val decoded0 = MultiplayerPacketCodec.decode(encoded0)
        assertEquals(packet0.type, decoded0.type)
        assertEquals(packet0.sequence, decoded0.sequence)

        val buffer = OrderedPacketBuffer()
        val earlyResult = buffer.offer(packet1)
        assertTrue(earlyResult.isEmpty()) // Waiting for packet 0

        val readyResult = buffer.offer(packet0)
        assertEquals(listOf(0L, 1L), readyResult.map { it.sequence })
    }
}
