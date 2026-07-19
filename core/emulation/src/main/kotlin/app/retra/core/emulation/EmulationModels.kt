package app.retra.core.emulation

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

enum class EmulatorButton(val bit: Int) {
    A(1 shl 0),
    B(1 shl 1),
    SELECT(1 shl 2),
    START(1 shl 3),
    RIGHT(1 shl 4),
    LEFT(1 shl 5),
    UP(1 shl 6),
    DOWN(1 shl 7),
    R(1 shl 8),
    L(1 shl 9),
    FAST_FORWARD(1 shl 10),
    REWIND(1 shl 11),
    MENU(1 shl 12)
}

data class InputSnapshot(val mask: Int = 0) {
    fun isPressed(button: EmulatorButton): Boolean = mask and button.bit != 0

    fun with(button: EmulatorButton, pressed: Boolean): InputSnapshot =
        if (pressed) copy(mask = mask or button.bit) else copy(mask = mask and button.bit.inv())

    companion object {
        fun from(buttons: Set<EmulatorButton>): InputSnapshot =
            InputSnapshot(buttons.fold(0) { mask, button -> mask or button.bit })
    }
}

enum class SessionPhase {
    IDLE,
    LOADING,
    READY,
    RUNNING,
    PAUSED,
    SUSPENDED,
    STOPPED,
    ERROR
}

data class SessionSnapshot(
    val phase: SessionPhase = SessionPhase.IDLE,
    val gameSha256: String? = null,
    val errorMessage: String? = null
)

sealed interface SessionCommand {
    data class BeginLoad(val gameSha256: String) : SessionCommand
    data object LoadSucceeded : SessionCommand
    data class LoadFailed(val reason: String) : SessionCommand
    data object Start : SessionCommand
    data object Pause : SessionCommand
    data object Resume : SessionCommand
    data object Suspend : SessionCommand
    data object Stop : SessionCommand
    data object Reset : SessionCommand
}

object SessionReducer {
    fun reduce(current: SessionSnapshot, command: SessionCommand): SessionSnapshot = when (command) {
        is SessionCommand.BeginLoad -> SessionSnapshot(SessionPhase.LOADING, command.gameSha256)
        SessionCommand.LoadSucceeded -> if (current.phase == SessionPhase.LOADING) current.copy(phase = SessionPhase.READY) else current
        is SessionCommand.LoadFailed -> current.copy(phase = SessionPhase.ERROR, errorMessage = command.reason)
        SessionCommand.Start -> when (current.phase) {
            SessionPhase.READY, SessionPhase.PAUSED, SessionPhase.SUSPENDED -> current.copy(phase = SessionPhase.RUNNING, errorMessage = null)
            else -> current
        }
        SessionCommand.Pause -> if (current.phase == SessionPhase.RUNNING) current.copy(phase = SessionPhase.PAUSED) else current
        SessionCommand.Resume -> if (current.phase == SessionPhase.PAUSED || current.phase == SessionPhase.SUSPENDED) current.copy(phase = SessionPhase.RUNNING) else current
        SessionCommand.Suspend -> if (current.phase == SessionPhase.RUNNING || current.phase == SessionPhase.PAUSED) current.copy(phase = SessionPhase.SUSPENDED) else current
        SessionCommand.Stop -> SessionSnapshot(SessionPhase.STOPPED, current.gameSha256)
        SessionCommand.Reset -> if (current.gameSha256 == null) SessionSnapshot() else current.copy(phase = SessionPhase.READY, errorMessage = null)
    }
}

enum class SaveKind { BATTERY, STATE, SUSPEND }

data class SaveEnvelope(
    val kind: SaveKind,
    val gameSha256: String,
    val coreId: String,
    val coreVersion: String,
    val slot: Int,
    val createdAtEpochMillis: Long,
    val payload: ByteArray
) {
    fun encode(): ByteArray {
        require(gameSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Expected a 64-character SHA-256." }
        require(coreId.isNotBlank() && coreId.length <= 128) { "Core ID is invalid." }
        require(coreVersion.isNotBlank() && coreVersion.length <= 64) { "Core version is invalid." }
        require(slot in -1..999) { "Slot is outside the supported range." }
        val gameHash = gameSha256.lowercase().toByteArray(Charsets.US_ASCII)
        val coreIdBytes = coreId.toByteArray(Charsets.UTF_8)
        val coreVersionBytes = coreVersion.toByteArray(Charsets.UTF_8)
        require(coreIdBytes.size <= 255 && coreVersionBytes.size <= 255)
        val payloadHash = sha256(payload)
        val headerSize = MAGIC.size + 1 + 1 + 4 + 8 + gameHash.size +
            1 + coreIdBytes.size + 1 + coreVersionBytes.size + 4 + payloadHash.size
        return ByteBuffer.allocate(headerSize + payload.size).order(ByteOrder.BIG_ENDIAN).apply {
            put(MAGIC)
            put(VERSION)
            put(kind.ordinal.toByte())
            putInt(slot)
            putLong(createdAtEpochMillis)
            put(gameHash)
            put(coreIdBytes.size.toByte())
            put(coreIdBytes)
            put(coreVersionBytes.size.toByte())
            put(coreVersionBytes)
            putInt(payload.size)
            put(payloadHash)
            put(payload)
        }.array()
    }

    companion object {
        private val MAGIC = "RETRASV".toByteArray(Charsets.US_ASCII)
        private const val VERSION: Byte = 2
        private const val MINIMUM_SIZE = 7 + 1 + 1 + 4 + 8 + 64 + 1 + 1 + 4 + 32

        fun decode(bytes: ByteArray): SaveEnvelope {
            require(bytes.size >= MINIMUM_SIZE) { "Save envelope is truncated." }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = ByteArray(MAGIC.size).also(buffer::get)
            require(magic.contentEquals(MAGIC)) { "Invalid save envelope magic." }
            require(buffer.get() == VERSION) { "Unsupported save envelope version." }
            val kindOrdinal = buffer.get().toInt()
            val kind = SaveKind.entries.getOrNull(kindOrdinal) ?: error("Unknown save kind.")
            val slot = buffer.int
            val timestamp = buffer.long
            val gameHashBytes = ByteArray(64).also(buffer::get)
            val coreId = readBoundedString(buffer, 128, "core ID")
            val coreVersion = readBoundedString(buffer, 64, "core version")
            require(buffer.remaining() >= 36) { "Save envelope is truncated before payload metadata." }
            val payloadSize = buffer.int
            require(payloadSize >= 0 && payloadSize <= buffer.remaining() - 32) { "Invalid save payload size." }
            val expectedHash = ByteArray(32).also(buffer::get)
            val payload = ByteArray(payloadSize).also(buffer::get)
            require(!buffer.hasRemaining()) { "Unexpected trailing save data." }
            require(MessageDigest.isEqual(expectedHash, sha256(payload))) { "Save payload checksum mismatch." }
            return SaveEnvelope(
                kind = kind,
                gameSha256 = gameHashBytes.toString(Charsets.US_ASCII),
                coreId = coreId,
                coreVersion = coreVersion,
                slot = slot,
                createdAtEpochMillis = timestamp,
                payload = payload
            )
        }

        private fun readBoundedString(buffer: ByteBuffer, maximum: Int, label: String): String {
            require(buffer.hasRemaining()) { "Save envelope is truncated before $label." }
            val length = buffer.get().toInt() and 0xFF
            require(length in 1..maximum && buffer.remaining() >= length) { "Invalid $label length." }
            return ByteArray(length).also(buffer::get).toString(Charsets.UTF_8)
        }

        private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    }
}

data class VaultSaveRecord(
    val relativePath: String,
    val kind: SaveKind,
    val gameSha256: String,
    val coreId: String,
    val coreVersion: String,
    val slot: Int,
    val createdAtEpochMillis: Long,
    val sizeBytes: Long
)
