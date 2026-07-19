package app.retra.emulation.api

import app.retra.core.emulation.SessionPhase
import app.retra.core.emulation.SessionSnapshot
import app.retra.core.model.PerformanceProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UnavailableEmulationCore(
    override val unavailableReason: String = "Native GBA core is not integrated in this source slice."
) : EmulationCore {
    override val isAvailable: Boolean = false
    override val descriptor = CoreDescriptor(
        id = "unavailable",
        displayName = "Unavailable core",
        version = "0",
        tier = CoreTier.DIAGNOSTIC_PIPELINE,
        supportsBatterySaves = false,
        supportsSaveStates = false,
        supportsAudio = false,
        supportsCheats = false,
        supportsRewind = false,
        legalNotice = "No emulator core is currently loaded."
    )
    private val mutableSession = MutableStateFlow(SessionSnapshot(SessionPhase.ERROR, errorMessage = unavailableReason))
    override val session: StateFlow<SessionSnapshot> = mutableSession
    override val latestFrame: StateFlow<VideoFrame?> = MutableStateFlow(null)
    override val audioPackets = MutableSharedFlow<AudioPacket>()
    override val metrics: StateFlow<RuntimeMetrics> = MutableStateFlow(RuntimeMetrics(0f, 0f, 0f, 0, 0))
    override suspend fun loadGame(game: GameFile): LoadGameResult = LoadGameResult.Failed(unavailableReason)
    override fun start() = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun suspendSession() = Unit
    override fun reset() = Unit
    override fun stop() = Unit
    override fun setInputState(input: EmulatorInputState) = Unit
    override fun setEmulationSpeed(multiplier: Float) = Unit
    override fun setPerformanceProfile(profile: PerformanceProfile) = Unit
    override fun saveBattery(): Result<Unit> = Result.failure(IllegalStateException(unavailableReason))
    override fun saveState(slot: SaveSlot): Result<SaveStateMetadata> = Result.failure(IllegalStateException(unavailableReason))
    override fun loadState(slot: SaveSlot): Result<Unit> = Result.failure(IllegalStateException(unavailableReason))
    override fun applyCheats(cheats: List<ActiveCheat>): Result<Unit> = Result.failure(IllegalStateException(unavailableReason))
    override fun clearCheats(): Result<Unit> = Result.failure(IllegalStateException(unavailableReason))
    override fun rewind(steps: Int): Result<Int> = Result.failure(IllegalStateException(unavailableReason))
}
