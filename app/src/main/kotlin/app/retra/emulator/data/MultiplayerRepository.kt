package app.retra.emulator.data

import app.retra.core.model.GameRecord
import app.retra.core.multiplayer.MultiplayerCommand
import app.retra.core.multiplayer.MultiplayerCompatibility
import app.retra.core.multiplayer.MultiplayerMode
import app.retra.core.multiplayer.MultiplayerSession
import app.retra.core.multiplayer.MultiplayerSessionReducer
import app.retra.core.multiplayer.RoomCode
import app.retra.emulation.api.CoreDescriptor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MultiplayerRepository @Inject constructor() {
    private val mutableSession = MutableStateFlow(MultiplayerSession())
    val session: StateFlow<MultiplayerSession> = mutableSession

    fun host(game: GameRecord?, core: CoreDescriptor, mode: MultiplayerMode): Result<String> = runCatching {
        require(game != null) { "Select and launch a verified game before hosting multiplayer." }
        require(core.supportsLinkCable) { "The active emulator core does not expose GBA link-cable callbacks yet." }
        val code = RoomCode.generate()
        compatibility(game, core)
        mutableSession.value = MultiplayerSessionReducer.reduce(mutableSession.value, MultiplayerCommand.Host(mode, code))
        code
    }

    fun join(game: GameRecord?, core: CoreDescriptor, mode: MultiplayerMode, code: String): Result<Unit> = runCatching {
        require(game != null) { "Select and launch a verified game before joining multiplayer." }
        require(core.supportsLinkCable) { "The active emulator core does not expose GBA link-cable callbacks yet." }
        compatibility(game, core)
        mutableSession.value = MultiplayerSessionReducer.reduce(mutableSession.value, MultiplayerCommand.Join(mode, RoomCode.normalize(code)))
    }

    fun reset() {
        mutableSession.value = MultiplayerSession()
    }

    fun compatibility(game: GameRecord, core: CoreDescriptor): MultiplayerCompatibility = MultiplayerCompatibility(
        protocolVersion = MultiplayerCompatibility.PROTOCOL_VERSION,
        romSha256 = game.sha256,
        coreId = core.id,
        coreVersion = core.version,
        patchSha256 = game.patchSha256,
        cheatsEnabled = false,
        maxPlayers = 2
    )
}
