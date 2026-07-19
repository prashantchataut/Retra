package app.retra.core.multiplayer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.TreeMap
import java.util.zip.CRC32

enum class MultiplayerMode { LOCAL_LINK, LAN, INTERNET }
enum class MultiplayerPhase { IDLE, HOSTING, JOINING, NEGOTIATING, READY, RUNNING, RECONNECTING, ENDED, ERROR }
enum class MultiplayerPacketType { HELLO, ACCEPT, REJECT, LINK_DATA, HEARTBEAT, PAUSE, RESUME, DISCONNECT }

data class MultiplayerPeer(
    val playerId: Int,
    val displayName: String,
    val connected: Boolean = true,
    val latencyMillis: Int? = null
) {
    init {
        require(playerId in 0..3)
        require(displayName.isNotBlank() && displayName.length <= 40)
        require(latencyMillis == null || latencyMillis in 0..60_000)
    }
}

data class MultiplayerCompatibility(
    val protocolVersion: Int,
    val romSha256: String,
    val coreId: String,
    val coreVersion: String,
    val patchSha256: String? = null,
    val cheatsEnabled: Boolean = false,
    val maxPlayers: Int = 2
) {
    init {
        require(protocolVersion == PROTOCOL_VERSION)
        require(romSha256.matches(Regex("[0-9a-fA-F]{64}")))
        require(coreId.isNotBlank() && coreId.length <= 128)
        require(coreVersion.isNotBlank() && coreVersion.length <= 64)
        require(patchSha256 == null || patchSha256.matches(Regex("[0-9a-fA-F]{64}")))
        require(maxPlayers in 2..4)
    }

    companion object { const val PROTOCOL_VERSION = 1 }
}

data class CompatibilityResult(val compatible: Boolean, val reasons: List<String>)

object MultiplayerCompatibilityGate {
    fun compare(host: MultiplayerCompatibility, guest: MultiplayerCompatibility): CompatibilityResult {
        val reasons = buildList {
            if (host.protocolVersion != guest.protocolVersion) add("Protocol version differs.")
            if (!host.romSha256.equals(guest.romSha256, ignoreCase = true)) add("ROM SHA-256 differs.")
            if (host.coreId != guest.coreId || host.coreVersion != guest.coreVersion) add("Emulator core build differs.")
            if (!host.patchSha256.equalsNullable(guest.patchSha256)) add("Patch identity differs.")
            if (host.cheatsEnabled != guest.cheatsEnabled) add("Cheat state differs.")
            if (host.maxPlayers != guest.maxPlayers) add("Player-count capability differs.")
        }
        return CompatibilityResult(reasons.isEmpty(), reasons)
    }

    private fun String?.equalsNullable(other: String?): Boolean = when {
        this == null && other == null -> true
        this == null || other == null -> false
        else -> equals(other, ignoreCase = true)
    }
}

data class MultiplayerSession(
    val phase: MultiplayerPhase = MultiplayerPhase.IDLE,
    val mode: MultiplayerMode? = null,
    val roomCode: String? = null,
    val localPlayerId: Int? = null,
    val peers: List<MultiplayerPeer> = emptyList(),
    val errorMessage: String? = null
)

sealed interface MultiplayerCommand {
    data class Host(val mode: MultiplayerMode, val roomCode: String) : MultiplayerCommand
    data class Join(val mode: MultiplayerMode, val roomCode: String) : MultiplayerCommand
    data object BeginNegotiation : MultiplayerCommand
    data class Negotiated(val localPlayerId: Int, val peers: List<MultiplayerPeer>) : MultiplayerCommand
    data object Start : MultiplayerCommand
    data object ConnectionLost : MultiplayerCommand
    data object Reconnected : MultiplayerCommand
    data class Fail(val reason: String) : MultiplayerCommand
    data object End : MultiplayerCommand
}

object MultiplayerSessionReducer {
    fun reduce(current: MultiplayerSession, command: MultiplayerCommand): MultiplayerSession = when (command) {
        is MultiplayerCommand.Host -> MultiplayerSession(MultiplayerPhase.HOSTING, command.mode, RoomCode.normalize(command.roomCode))
        is MultiplayerCommand.Join -> MultiplayerSession(MultiplayerPhase.JOINING, command.mode, RoomCode.normalize(command.roomCode))
        MultiplayerCommand.BeginNegotiation -> if (current.phase in setOf(MultiplayerPhase.HOSTING, MultiplayerPhase.JOINING)) current.copy(phase = MultiplayerPhase.NEGOTIATING) else current
        is MultiplayerCommand.Negotiated -> if (current.phase == MultiplayerPhase.NEGOTIATING) current.copy(phase = MultiplayerPhase.READY, localPlayerId = command.localPlayerId, peers = command.peers, errorMessage = null) else current
        MultiplayerCommand.Start -> if (current.phase == MultiplayerPhase.READY) current.copy(phase = MultiplayerPhase.RUNNING) else current
        MultiplayerCommand.ConnectionLost -> if (current.phase == MultiplayerPhase.RUNNING) current.copy(phase = MultiplayerPhase.RECONNECTING) else current
        MultiplayerCommand.Reconnected -> if (current.phase == MultiplayerPhase.RECONNECTING) current.copy(phase = MultiplayerPhase.RUNNING, errorMessage = null) else current
        is MultiplayerCommand.Fail -> current.copy(phase = MultiplayerPhase.ERROR, errorMessage = command.reason.take(240))
        MultiplayerCommand.End -> current.copy(phase = MultiplayerPhase.ENDED, peers = emptyList())
    }
}

object RoomCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val pattern = Regex("[A-Z2-9]{6}")

    fun generate(random: SecureRandom = SecureRandom()): String = CharArray(6) {
        ALPHABET[random.nextInt(ALPHABET.length)]
    }.concatToString()

    fun normalize(value: String): String {
        val normalized = value.trim().uppercase().replace("-", "")
        require(pattern.matches(normalized)) { "Room code must contain six unambiguous letters or digits." }
        return normalized
    }
}

data class MultiplayerPacket(
    val type: MultiplayerPacketType,
    val roomCode: String,
    val sequence: Long,
    val playerId: Int,
    val payload: ByteArray
) {
    init {
        RoomCode.normalize(roomCode)
        require(sequence >= 0)
        require(playerId in 0..3)
        require(payload.size <= MAX_PAYLOAD_BYTES)
    }

    companion object { const val MAX_PAYLOAD_BYTES = 4096 }
}

object MultiplayerPacketCodec {
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'T'.code.toByte(), 'N'.code.toByte(), 'P'.code.toByte())
    private const val VERSION: Byte = 1

    fun encode(packet: MultiplayerPacket): ByteArray {
        val room = RoomCode.normalize(packet.roomCode).encodeToByteArray()
        val bodySize = MAGIC.size + 1 + 1 + 1 + 8 + 6 + 2 + packet.payload.size
        val body = ByteBuffer.allocate(bodySize).order(ByteOrder.BIG_ENDIAN).apply {
            put(MAGIC)
            put(VERSION)
            put(packet.type.ordinal.toByte())
            put(packet.playerId.toByte())
            putLong(packet.sequence)
            put(room)
            putShort(packet.payload.size.toShort())
            put(packet.payload)
        }.array()
        val crc = CRC32().apply { update(body) }.value
        return ByteBuffer.allocate(body.size + 4).order(ByteOrder.BIG_ENDIAN).apply {
            put(body)
            putInt(crc.toInt())
        }.array()
    }

    fun decode(bytes: ByteArray): MultiplayerPacket {
        require(bytes.size in MIN_PACKET_BYTES..MAX_PACKET_BYTES) { "Invalid multiplayer packet size." }
        val body = bytes.copyOf(bytes.size - 4)
        val expectedCrc = ByteBuffer.wrap(bytes, bytes.size - 4, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val actualCrc = CRC32().apply { update(body) }.value
        require(expectedCrc == actualCrc) { "Multiplayer packet checksum mismatch." }
        val buffer = ByteBuffer.wrap(body).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(4).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "Invalid multiplayer packet magic." }
        require(buffer.get() == VERSION) { "Unsupported multiplayer packet version." }
        val type = MultiplayerPacketType.entries.getOrNull(buffer.get().toInt() and 0xFF) ?: error("Unknown multiplayer packet type.")
        val playerId = buffer.get().toInt() and 0xFF
        val sequence = buffer.long
        val roomCode = ByteArray(6).also(buffer::get).decodeToString()
        val payloadSize = buffer.short.toInt() and 0xFFFF
        require(payloadSize <= MultiplayerPacket.MAX_PAYLOAD_BYTES && payloadSize == buffer.remaining()) { "Invalid multiplayer payload size." }
        val payload = ByteArray(payloadSize).also(buffer::get)
        return MultiplayerPacket(type, RoomCode.normalize(roomCode), sequence, playerId, payload)
    }

    private const val MIN_PACKET_BYTES = 4 + 1 + 1 + 1 + 8 + 6 + 2 + 4
    private const val MAX_PACKET_BYTES = MIN_PACKET_BYTES + MultiplayerPacket.MAX_PAYLOAD_BYTES
}

class OrderedPacketBuffer(private val maximumBufferedPackets: Int = 120) {
    private val queued = TreeMap<Long, MultiplayerPacket>()
    private var nextSequence = 0L

    init { require(maximumBufferedPackets in 1..10_000) }

    fun offer(packet: MultiplayerPacket): List<MultiplayerPacket> {
        if (packet.sequence < nextSequence) return emptyList()
        if (queued.size >= maximumBufferedPackets && packet.sequence !in queued) {
            throw IllegalStateException("Multiplayer jitter buffer is full.")
        }
        queued.putIfAbsent(packet.sequence, packet)
        val ready = mutableListOf<MultiplayerPacket>()
        while (true) {
            val packetAtHead = queued.remove(nextSequence) ?: break
            ready += packetAtHead
            nextSequence++
        }
        return ready
    }

    fun reset(sequence: Long = 0) {
        require(sequence >= 0)
        queued.clear()
        nextSequence = sequence
    }
}
