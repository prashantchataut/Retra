package app.retra.emulation.api

import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.SessionSnapshot
import app.retra.core.model.PerformanceProfile
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class GameFile(val uri: String, val sha256: String)
data class EmulatorInputState(val buttons: Set<EmulatorButton>)
data class SaveSlot(val number: Int, val label: String? = null)
data class SaveStateMetadata(
    val slot: SaveSlot,
    val createdAtEpochMillis: Long,
    val screenshotAvailable: Boolean = false,
    val coreVersion: String? = null
)
data class ActiveCheat(val id: String, val code: String)

data class RuntimeMetrics(
    val emulatedFps: Float,
    val presentedFps: Float,
    val speedPercent: Float,
    val droppedFrames: Long,
    val audioUnderruns: Long,
    val frameTimeMillis: Float = 0f
)

data class AudioPacket(
    val sampleRate: Int,
    val channelCount: Int,
    val pcm16: ShortArray,
    val sequence: Long
) {
    init {
        require(sampleRate in 8_000..192_000)
        require(channelCount in 1..2)
        require(pcm16.size % channelCount == 0)
    }
}

data class VideoFrame(
    val width: Int,
    val height: Int,
    val argb: IntArray,
    val sequence: Long,
    val presentationTimeNanos: Long
) {
    init {
        require(width > 0 && height > 0)
        require(argb.size == width * height)
    }
}

enum class CoreTier {
    DIAGNOSTIC_PIPELINE,
    GBA_GAMEPLAY
}

data class CoreDescriptor(
    val id: String,
    val displayName: String,
    val version: String,
    val tier: CoreTier,
    val supportsBatterySaves: Boolean,
    val supportsSaveStates: Boolean,
    val supportsAudio: Boolean,
    val supportsCheats: Boolean,
    val supportsRewind: Boolean,
    val supportsLinkCable: Boolean = false,
    val legalNotice: String
)

sealed interface LoadGameResult {
    data class Loaded(val restoredSuspendState: Boolean = false) : LoadGameResult
    data class Failed(val reason: String) : LoadGameResult
}

interface EmulationCore {
    val descriptor: CoreDescriptor
    val session: StateFlow<SessionSnapshot>
    val latestFrame: StateFlow<VideoFrame?>
    val audioPackets: SharedFlow<AudioPacket>
    val metrics: StateFlow<RuntimeMetrics>

    suspend fun loadGame(game: GameFile): LoadGameResult
    fun start()
    fun pause()
    fun resume()
    fun suspendSession()
    fun reset()
    fun stop()
    fun setInputState(input: EmulatorInputState)
    fun setEmulationSpeed(multiplier: Float)
    fun setPerformanceProfile(profile: PerformanceProfile)
    fun saveBattery(): Result<Unit>
    fun saveState(slot: SaveSlot): Result<SaveStateMetadata>
    fun loadState(slot: SaveSlot): Result<Unit>
    fun applyCheats(cheats: List<ActiveCheat>): Result<Unit>
    fun clearCheats(): Result<Unit>
    fun rewind(steps: Int = 1): Result<Int>
    fun getRuntimeMetrics(): RuntimeMetrics = metrics.value
    val isAvailable: Boolean
    val unavailableReason: String?
}
