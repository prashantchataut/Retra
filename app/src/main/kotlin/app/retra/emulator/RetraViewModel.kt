package app.retra.emulator

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.retra.core.achievements.AchievementEvent
import app.retra.core.achievements.AchievementEventType
import app.retra.core.achievements.AchievementIntegrity
import app.retra.core.cheats.CheatCategory
import app.retra.core.cheats.CheatConflictAnalyzer
import app.retra.core.cheats.CheatFormat
import app.retra.core.cheats.CheatProfile
import app.retra.core.cheats.CheatRisk
import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.SessionPhase
import app.retra.core.emulation.VaultSaveRecord
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ContentDensity
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import app.retra.core.multiplayer.MultiplayerMode
import app.retra.core.social.SharePrivacy
import app.retra.core.social.SocialProvider
import app.retra.core.social.SocialShareFactory
import app.retra.emulator.data.AchievementRepository
import app.retra.emulator.data.ArtworkRepository
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.data.CatalogDownloadOutcome
import app.retra.emulator.data.CatalogDownloadRepository
import app.retra.emulator.data.CatalogImportOutcome
import app.retra.emulator.data.CatalogRepository
import app.retra.emulator.data.CheatPackImportOutcome
import app.retra.emulator.data.CheatRepository
import app.retra.emulator.data.GameRepository
import app.retra.emulator.data.ImportOutcome
import app.retra.emulator.data.MultiplayerRepository
import app.retra.emulator.data.PatchOutcome
import app.retra.emulator.data.PatchRepository
import app.retra.emulator.data.SettingsRepository
import app.retra.emulator.data.ScreenshotRepository
import app.retra.emulator.data.SocialRepository
import app.retra.emulator.data.StoredCheatPack
import app.retra.emulator.data.StoredCatalog
import app.retra.emulator.data.VaultRepository
import app.retra.emulator.auth.AuthOperation
import app.retra.emulator.auth.GoogleAuthRepository
import app.retra.emulator.auth.GoogleSignInResult
import app.retra.emulator.auth.RetraAccount
import app.retra.emulation.api.ActiveCheat
import app.retra.emulation.api.CoreTier
import app.retra.emulation.api.EmulationCore
import app.retra.emulation.api.EmulatorInputState
import app.retra.emulation.api.GameFile
import app.retra.emulation.api.LoadGameResult
import app.retra.emulation.api.SaveSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RetraViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val artworkRepository: ArtworkRepository,
    private val settingsRepository: SettingsRepository,
    val catalogRepository: CatalogRepository,
    private val catalogDownloadRepository: CatalogDownloadRepository,
    private val vaultRepository: VaultRepository,
    private val patchRepository: PatchRepository,
    private val cheatRepository: CheatRepository,
    private val achievementRepository: AchievementRepository,
    private val socialRepository: SocialRepository,
    private val multiplayerRepository: MultiplayerRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val screenshotRepository: ScreenshotRepository,
    private val audioOutput: AudioOutput,
    private val emulationCore: EmulationCore
) : ViewModel() {
    val games: StateFlow<List<GameRecord>> = gameRepository.observeGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val selectedGame = MutableStateFlow<GameRecord?>(null)
    val activeGame = MutableStateFlow<GameRecord?>(null)
    val activeCheatIds = MutableStateFlow<Set<String>>(emptySet())
    val session = emulationCore.session
    val latestFrame = emulationCore.latestFrame
    val runtimeMetrics = emulationCore.metrics
    val vaultRecords = vaultRepository.records
    val cheatPacks = cheatRepository.packs
    val catalogDownloads = catalogDownloadRepository.progress
    val catalogSources = catalogRepository.catalogs
    val achievements = achievementRepository.statuses
    val socialProfile = socialRepository.profile
    val socialConnections = socialRepository.connections
    val multiplayerSession = multiplayerRepository.session
    val account: StateFlow<RetraAccount?> = googleAuthRepository.account
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val googleAuthConfigured: Boolean get() = googleAuthRepository.isConfigured
    private val mutableAuthOperation = MutableStateFlow(AuthOperation.IDLE)
    val authOperation: StateFlow<AuthOperation> = mutableAuthOperation
    val coreDescriptor = emulationCore.descriptor
    val coreAvailable: Boolean get() = emulationCore.isAvailable
    val gameplayAvailable: Boolean get() = emulationCore.descriptor.tier == CoreTier.GBA_GAMEPLAY
    val coreStatus: String
        get() = emulationCore.unavailableReason ?: when (emulationCore.descriptor.tier) {
            CoreTier.GBA_GAMEPLAY -> "${emulationCore.descriptor.displayName} ${emulationCore.descriptor.version}"
            CoreTier.DIAGNOSTIC_PIPELINE -> "Native diagnostics are active because a reviewed mGBA gameplay library is not bundled for this device ABI."
        }

    private val inputLock = Any()
    private val pressedButtons = linkedSetOf<EmulatorButton>()
    private val mutableControllerInput = MutableStateFlow<Set<EmulatorButton>>(emptySet())
    val controllerInput: StateFlow<Set<EmulatorButton>> = mutableControllerInput
    private val mutableControllerTestEnabled = MutableStateFlow(false)
    val controllerTestEnabled: StateFlow<Boolean> = mutableControllerTestEnabled
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 24)
    val messages = _messages.asSharedFlow()
    private var sessionStartedAtEpochMillis: Long? = null
    private var selectedSessionSpeed = 1f
    private var pausedByHost = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { value ->
                audioOutput.configure(value.audioEnabled, value.masterVolume)
            }
        }
        viewModelScope.launch {
            emulationCore.audioPackets.collect { packet ->
                runCatching { audioOutput.write(packet) }
                    .onFailure { _messages.tryEmit(it.message ?: "Audio output failed.") }
            }
        }
    }

    fun finishOnboarding() = viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }

    fun signInWithGoogle(context: Context) = viewModelScope.launch {
        if (mutableAuthOperation.value != AuthOperation.IDLE) return@launch
        mutableAuthOperation.value = AuthOperation.SIGNING_IN
        try {
            when (val result = googleAuthRepository.signIn(context)) {
                is GoogleSignInResult.Connected -> {
                    // The raw ID token is deliberately not persisted. A production Retra backend must
                    // validate it and the nonce before enabling cloud or social account privileges.
                    _messages.emit("Google account connected on this device. Server verification is still required for cloud services.")
                }
                GoogleSignInResult.Cancelled -> _messages.emit("Google sign-in was cancelled.")
                GoogleSignInResult.NoCredential -> _messages.emit("No eligible Google credential was available on this device.")
                GoogleSignInResult.MissingConfiguration -> _messages.emit("Google sign-in needs RETRA_GOOGLE_WEB_CLIENT_ID in the build configuration.")
                is GoogleSignInResult.Failed -> _messages.emit(result.message)
            }
        } finally {
            mutableAuthOperation.value = AuthOperation.IDLE
        }
    }

    fun signOutGoogle(context: Context) = viewModelScope.launch {
        if (mutableAuthOperation.value != AuthOperation.IDLE) return@launch
        mutableAuthOperation.value = AuthOperation.SIGNING_OUT
        try {
            googleAuthRepository.signOut(context)
            _messages.emit("Disconnected the Google account from this Retra installation.")
        } finally {
            mutableAuthOperation.value = AuthOperation.IDLE
        }
    }

    fun importFile(uri: Uri) = viewModelScope.launch {
        when (val result = gameRepository.importFile(uri)) {
            is ImportOutcome.Imported -> {
                _messages.emit("Imported ${result.game.title}.")
                recordAchievement(AchievementEventType.GAME_IMPORTED, uniqueKey = result.game.sha256, game = result.game)
            }
            is ImportOutcome.Duplicate -> _messages.emit("${result.title} is already in the library.")
            is ImportOutcome.Rejected -> _messages.emit(result.reason)
        }
    }

    fun importFolder(uri: Uri) = viewModelScope.launch {
        val result = gameRepository.importFolder(uri)
        if (result.imported > 0) recordAchievement(AchievementEventType.GAME_IMPORTED, amount = result.imported.toLong())
        _messages.emit(
            "Folder scan: ${result.imported} imported, ${result.duplicates} duplicates, ${result.rejected} rejected" +
                if (result.limitReached) "; safety limit reached." else "."
        )
    }

    fun downloadCatalogEntry(entry: app.retra.core.model.CatalogEntry) = viewModelScope.launch {
        if (!catalogRepository.isDownloadable(entry)) {
            _messages.emit("This entry is not eligible for download. Import a reviewed manifest with valid HTTPS, hash, size, and licensing metadata.")
            return@launch
        }
        when (val outcome = catalogDownloadRepository.download(entry)) {
            is CatalogDownloadOutcome.Imported -> {
                selectedGame.value = outcome.game
                recordAchievement(AchievementEventType.GAME_IMPORTED, uniqueKey = outcome.game.sha256, game = outcome.game)
                _messages.emit("Downloaded, verified, and imported ${outcome.game.title}.")
            }
            is CatalogDownloadOutcome.Duplicate -> _messages.emit("${outcome.title} is already in the library.")
            is CatalogDownloadOutcome.Rejected -> _messages.emit(outcome.reason)
        }
    }

    fun importCatalog(uri: Uri) = viewModelScope.launch {
        emitCatalogOutcome(catalogRepository.importManifest(uri))
    }

    fun importCatalogFromUrl(url: String, expectedSha256: String) = viewModelScope.launch {
        emitCatalogOutcome(catalogRepository.importManifestFromUrl(url, expectedSha256))
    }

    private suspend fun emitCatalogOutcome(outcome: CatalogImportOutcome) {
        when (outcome) {
            is CatalogImportOutcome.Imported -> _messages.emit(
                if (outcome.replacedExisting) {
                    "Updated ${outcome.catalog.manifest.name} after strict URL, hash, schema, and download-policy validation."
                } else {
                    "Imported ${outcome.catalog.manifest.name} with ${outcome.catalog.manifest.games.size} verified entries."
                }
            )
            is CatalogImportOutcome.Rejected -> _messages.emit(outcome.reason)
        }
    }

    fun deleteCatalog(catalog: StoredCatalog) {
        val deleted = catalogRepository.deleteCatalog(catalog)
        _messages.tryEmit(if (deleted) "Removed ${catalog.manifest.name} from Retra." else "The built-in Retra preview catalog cannot be removed.")
    }

    fun selectGame(game: GameRecord?) {
        selectedGame.value = game
    }

    fun launchGame(game: GameRecord) = viewModelScope.launch {
        if (!emulationCore.isAvailable) {
            _messages.emit(coreStatus)
            return@launch
        }
        emulationCore.setPerformanceProfile(settings.value.performanceProfile)
        emulationCore.setEmulationSpeed(1f)
        when (val result = emulationCore.loadGame(GameFile(game.uri, game.sha256))) {
            is LoadGameResult.Loaded -> {
                gameRepository.markPlayed(game.id)
                activeGame.value = game
                selectedGame.value = null
                activeCheatIds.value = emptySet()
                sessionStartedAtEpochMillis = System.currentTimeMillis()
                selectedSessionSpeed = 1f
                emulationCore.setEmulationSpeed(selectedSessionSpeed)
                emulationCore.start()
                if (emulationCore.descriptor.supportsAudio && settings.value.audioEnabled) audioOutput.start()
                recordAchievement(AchievementEventType.GAME_STARTED, uniqueKey = game.sha256, game = game)
                _messages.emit(
                    when {
                        result.restoredSuspendState && gameplayAvailable -> "Game loaded from the latest suspend state."
                        result.restoredSuspendState -> "Native diagnostics restored the latest suspend state."
                        gameplayAvailable -> "Game loaded."
                        else -> "Native pipeline opened in diagnostic mode; gameplay is not claimed."
                    }
                )
            }
            is LoadGameResult.Failed -> _messages.emit(result.reason)
        }
    }

    fun closePlayer() {
        val game = activeGame.value
        recordElapsedPlaytime(game)
        audioOutput.pause()
        emulationCore.stop()
        clearInput()
        activeCheatIds.value = emptySet()
        multiplayerRepository.reset()
        activeGame.value = null
    }

    fun togglePause() {
        when (session.value.phase) {
            SessionPhase.RUNNING -> {
                recordElapsedPlaytime(activeGame.value)
                emulationCore.pause()
                audioOutput.pause()
            }
            SessionPhase.PAUSED, SessionPhase.SUSPENDED, SessionPhase.READY -> {
                sessionStartedAtEpochMillis = System.currentTimeMillis()
                emulationCore.resume()
                if (emulationCore.descriptor.supportsAudio && settings.value.audioEnabled) audioOutput.start()
            }
            else -> Unit
        }
    }

    fun resetGame() {
        emulationCore.reset()
        _messages.tryEmit("Session reset.")
    }

    fun rewindSession(steps: Int = 1) {
        audioOutput.pause()
        emulationCore.rewind(steps)
            .onSuccess { remaining ->
                _messages.tryEmit("Rewound the session. $remaining snapshots remain in memory.")
            }
            .onFailure { error ->
                _messages.tryEmit(error.message ?: "Rewind is not available yet.")
                if (session.value.phase == SessionPhase.RUNNING && emulationCore.descriptor.supportsAudio && settings.value.audioEnabled) {
                    audioOutput.start()
                }
            }
    }

    fun captureScreenshot() = viewModelScope.launch {
        val game = activeGame.value ?: run {
            _messages.emit("Launch a game before taking a screenshot.")
            return@launch
        }
        val frame = latestFrame.value ?: run {
            _messages.emit("The emulator has not produced a frame yet.")
            return@launch
        }
        screenshotRepository.save(game.title, frame)
            .onSuccess { result -> _messages.emit("Saved ${result.displayName} to ${result.location}.") }
            .onFailure { error -> _messages.emit(error.message ?: "Screenshot capture failed.") }
    }

    fun saveState(slotNumber: Int = 0) {
        emulationCore.saveState(SaveSlot(slotNumber, "Quick Save"))
            .onSuccess {
                vaultRepository.refresh()
                recordAchievement(AchievementEventType.SAVE_CREATED, game = activeGame.value)
                _messages.tryEmit("Saved state to slot $slotNumber.")
            }
            .onFailure { _messages.tryEmit(it.message ?: "Save state failed.") }
    }

    fun loadState(slotNumber: Int = 0) {
        emulationCore.loadState(SaveSlot(slotNumber, "Quick Save"))
            .onSuccess { _messages.tryEmit("Loaded state from slot $slotNumber.") }
            .onFailure { _messages.tryEmit(it.message ?: "Load state failed.") }
    }

    fun setSessionSpeed(multiplier: Float) {
        selectedSessionSpeed = multiplier.coerceIn(0.25f, 16f)
        emulationCore.setEmulationSpeed(selectedSessionSpeed)
    }

    fun setButtonPressed(button: EmulatorButton, pressed: Boolean) {
        when (button) {
            EmulatorButton.FAST_FORWARD -> {
                emulationCore.setEmulationSpeed(if (pressed) settings.value.fastForwardSpeed else selectedSessionSpeed)
                return
            }
            EmulatorButton.REWIND -> {
                if (pressed) rewindSession()
                return
            }
            EmulatorButton.MENU -> return
            else -> Unit
        }
        synchronized(inputLock) {
            if (pressed) pressedButtons += button else pressedButtons -= button
            val snapshot = pressedButtons.toSet()
            mutableControllerInput.value = snapshot
            emulationCore.setInputState(EmulatorInputState(snapshot))
        }
    }

    fun setDirectionalAxes(horizontal: Float, vertical: Float) {
        synchronized(inputLock) {
            updateButton(EmulatorButton.LEFT, horizontal < -0.45f)
            updateButton(EmulatorButton.RIGHT, horizontal > 0.45f)
            updateButton(EmulatorButton.UP, vertical < -0.45f)
            updateButton(EmulatorButton.DOWN, vertical > 0.45f)
            val snapshot = pressedButtons.toSet()
            mutableControllerInput.value = snapshot
            emulationCore.setInputState(EmulatorInputState(snapshot))
        }
    }

    fun onHostBackgrounded() {
        if (activeGame.value == null) return
        pausedByHost = session.value.phase == SessionPhase.RUNNING
        recordElapsedPlaytime(activeGame.value)
        clearInput()
        audioOutput.pause()
        if (settings.value.autoSuspendOnBackground) {
            emulationCore.suspendSession()
            vaultRepository.refresh()
        } else if (pausedByHost) {
            emulationCore.pause()
            emulationCore.saveBattery()
        }
    }

    fun onHostForegrounded() {
        if (activeGame.value != null && pausedByHost && session.value.phase in setOf(SessionPhase.SUSPENDED, SessionPhase.PAUSED)) {
            pausedByHost = false
            sessionStartedAtEpochMillis = System.currentTimeMillis()
            emulationCore.resume()
            if (emulationCore.descriptor.supportsAudio && settings.value.audioEnabled) audioOutput.start()
        }
    }

    fun onAudioBecomingNoisy() {
        if (!settings.value.pauseOnHeadphoneDisconnect || activeGame.value == null || session.value.phase != SessionPhase.RUNNING) return
        recordElapsedPlaytime(activeGame.value)
        emulationCore.pause()
        audioOutput.pause()
        _messages.tryEmit("Paused because the audio output disconnected.")
    }

    fun onControllerDisconnected() {
        clearInput()
        if (activeGame.value != null) _messages.tryEmit("Controller disconnected; held inputs were cleared.")
    }

    fun setControllerTestEnabled(enabled: Boolean) {
        mutableControllerTestEnabled.value = enabled
        if (!enabled && activeGame.value == null) clearInput()
    }

    fun deleteVaultRecord(record: VaultSaveRecord) {
        val deleted = runCatching { vaultRepository.delete(record) }.getOrDefault(false)
        _messages.tryEmit(if (deleted) "Deleted the local save snapshot." else "The save snapshot could not be deleted.")
    }

    fun applyPatch(base: GameRecord, patchUri: Uri) = viewModelScope.launch {
        when (val outcome = patchRepository.apply(base, patchUri)) {
            is PatchOutcome.Applied -> {
                selectedGame.value = outcome.game
                recordAchievement(AchievementEventType.PATCH_APPLIED, uniqueKey = outcome.game.patchSha256, game = outcome.game)
                val checksumWarning = if (outcome.headerChecksumValid) "" else " The resulting ROM header checksum is nonstandard."
                _messages.emit("Applied ${outcome.format.name} patch ${outcome.patchDisplayName} and added a separate library entry.$checksumWarning")
            }
            is PatchOutcome.Duplicate -> _messages.emit("That patched result is already in the library as ${outcome.existingTitle}.")
            is PatchOutcome.Rejected -> _messages.emit(outcome.reason)
        }
    }

    fun importCheatPack(game: GameRecord, uri: Uri) = viewModelScope.launch {
        emitCheatImport(game, cheatRepository.import(game, uri))
    }

    fun createCustomCheat(game: GameRecord, name: String, format: CheatFormat, codeText: String) = viewModelScope.launch {
        val codes = codeText.lineSequence().flatMap { it.split('+').asSequence() }.map(String::trim).filter(String::isNotBlank).toList()
        emitCheatImport(
            game,
            cheatRepository.createCustom(game, name, format, codes, CheatCategory.QUALITY_OF_LIFE, CheatRisk.CAUTION)
        )
    }

    fun downloadCheatPack(game: GameRecord, url: String, expectedSha256: String) = viewModelScope.launch {
        emitCheatImport(game, cheatRepository.importFromUrl(game, url, expectedSha256))
    }

    private suspend fun emitCheatImport(game: GameRecord, outcome: CheatPackImportOutcome) {
        when (outcome) {
            is CheatPackImportOutcome.Imported -> {
                recordAchievement(AchievementEventType.CHEAT_PACK_IMPORTED, uniqueKey = outcome.stored.sha256, game = game)
                _messages.emit("Imported ${outcome.stored.cheatCount} declarative cheats from ${outcome.stored.provider}.")
            }
            is CheatPackImportOutcome.Duplicate -> _messages.emit("That Retra Codes pack is already stored for this ROM.")
            is CheatPackImportOutcome.Rejected -> _messages.emit(outcome.reason)
        }
    }

    fun activateCheat(stored: StoredCheatPack, cheatId: String) {
        val game = activeGame.value
        if (game == null || !stored.gameSha256.equals(game.sha256, ignoreCase = true)) {
            _messages.tryEmit("Launch the matching game before activating this cheat.")
            return
        }
        if (!emulationCore.descriptor.supportsCheats) {
            _messages.tryEmit("The active emulator core does not expose cheat activation.")
            return
        }
        val byId = stored.pack.cheats.associateBy { it.id }
        val requested = byId[cheatId] ?: run {
            _messages.tryEmit("The selected cheat no longer exists in this pack.")
            return
        }
        val enabled = linkedSetOf<String>()
        fun include(id: String) {
            if (!enabled.add(id)) return
            byId[id]?.dependencies?.forEach(::include)
        }
        include(requested.id)
        val validation = CheatConflictAnalyzer.validate(stored.pack, CheatProfile("Active session", enabled))
        if (!validation.valid) {
            _messages.tryEmit(validation.errors.joinToString(" "))
            return
        }
        val selected = enabled.mapNotNull(byId::get)
        if (selected.any { it.format == CheatFormat.RAW }) {
            _messages.tryEmit("RAW memory-write translation is intentionally disabled until the mGBA address-width bridge is device-verified.")
            return
        }
        val active = selected.map { cheat ->
            ActiveCheat("${stored.sha256}:${cheat.id}", cheat.codeLines.joinToString("+"))
        }
        emulationCore.applyCheats(active)
            .onSuccess {
                activeCheatIds.value = enabled
                _messages.tryEmit("Activated ${requested.name}. Retra created a protected pre-cheat state first.")
            }
            .onFailure { _messages.tryEmit(it.message ?: "Cheat activation failed.") }
    }

    fun clearCheats() {
        emulationCore.clearCheats()
            .onSuccess {
                activeCheatIds.value = emptySet()
                _messages.tryEmit("Cleared all active cheats.")
            }
            .onFailure { _messages.tryEmit(it.message ?: "Cheats could not be cleared.") }
    }

    fun deleteCheatPack(stored: StoredCheatPack) {
        val deleted = runCatching { cheatRepository.delete(stored) }.getOrDefault(false)
        _messages.tryEmit(if (deleted) "Deleted the local Retra Codes pack." else "The Retra Codes pack could not be deleted.")
    }

    fun updateSocialProfile(displayName: String, bio: String) {
        socialRepository.updateProfile(displayName, bio)
            .onSuccess { _messages.tryEmit("Updated your local Retra profile.") }
            .onFailure { _messages.tryEmit(it.message ?: "Profile update failed.") }
    }

    fun configureSocialProvider(provider: SocialProvider, displayName: String, profileUrl: String) {
        socialRepository.setProviderConfiguration(provider, displayName.ifBlank { null }, profileUrl.ifBlank { null })
            .onSuccess { _messages.tryEmit("Saved ${provider.name.lowercase().replaceFirstChar(Char::titlecase)} profile configuration locally.") }
            .onFailure { _messages.tryEmit(it.message ?: "Social provider configuration failed.") }
    }

    fun shareAchievement(status: AchievementStatus) {
        if (status.progress.unlockedAtEpochMillis == null) {
            _messages.tryEmit("Unlock this achievement before sharing it.")
            return
        }
        val card = SocialShareFactory.achievement(
            profile = socialProfile.value,
            achievementTitle = status.definition.title,
            achievementDescription = status.definition.description,
            points = status.definition.points,
            privacy = SharePrivacy.SUMMARY,
            deepLink = "retra://achievement/${status.definition.id}"
        )
        socialRepository.share(card)
            .onSuccess { recordAchievement(AchievementEventType.ACHIEVEMENT_SHARED, uniqueKey = status.definition.id) }
            .onFailure { _messages.tryEmit(it.message ?: "The Android share sheet could not be opened.") }
    }

    fun hostMultiplayer(mode: MultiplayerMode) {
        multiplayerRepository.host(activeGame.value, coreDescriptor, mode)
            .onSuccess { code -> _messages.tryEmit("Hosted ${mode.name.lowercase().replace('_', ' ')} room $code.") }
            .onFailure { _messages.tryEmit(it.message ?: "Multiplayer hosting failed.") }
    }

    fun joinMultiplayer(mode: MultiplayerMode, roomCode: String) {
        multiplayerRepository.join(activeGame.value, coreDescriptor, mode, roomCode)
            .onSuccess { _messages.tryEmit("Joined room ${roomCode.trim().uppercase()} and started compatibility negotiation.") }
            .onFailure { _messages.tryEmit(it.message ?: "Multiplayer join failed.") }
    }

    fun resetMultiplayer() = multiplayerRepository.reset()

    fun toggleFavorite(game: GameRecord) = viewModelScope.launch {
        gameRepository.setFavorite(game.id, !game.favorite)
        selectedGame.value = game.copy(favorite = !game.favorite)
        _messages.emit(if (game.favorite) "Removed ${game.title} from favorites." else "Added ${game.title} to favorites.")
    }

    fun updateGameMetadata(game: GameRecord, title: String, notes: String?) = viewModelScope.launch {
        val normalizedTitle = title.trim().take(120)
        if (normalizedTitle.isBlank()) {
            _messages.emit("A game title cannot be blank.")
            return@launch
        }
        val normalizedNotes = notes?.trim()?.take(4_000)?.ifBlank { null }
        gameRepository.updateMetadata(game.id, normalizedTitle, normalizedNotes)
        selectedGame.value = game.copy(title = normalizedTitle, notes = normalizedNotes)
        _messages.emit("Updated the library details for $normalizedTitle.")
    }

    fun importCoverArt(game: GameRecord, uri: Uri) = viewModelScope.launch {
        artworkRepository.importCoverArt(game.id, game.sha256, uri)
            .onSuccess { path ->
                selectedGame.value = game.copy(coverArtPath = path)
                _messages.emit("Updated the cover art for ${game.title}.")
            }
            .onFailure { error -> _messages.emit(error.message ?: "Cover art could not be imported.") }
    }

    fun removeCoverArt(game: GameRecord) = viewModelScope.launch {
        if (artworkRepository.removeCoverArt(game.id, game.coverArtPath)) {
            selectedGame.value = game.copy(coverArtPath = null)
            _messages.emit("Removed the custom cover art for ${game.title}.")
        } else {
            _messages.emit("The custom cover art could not be removed.")
        }
    }

    fun deleteGame(game: GameRecord) = viewModelScope.launch {
        artworkRepository.deleteFile(game.coverArtPath)
        gameRepository.delete(game.id)
        selectedGame.value = null
        _messages.emit("Removed ${game.title} from the Retra library. The source file was not deleted.")
    }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(value) }
    fun setLibraryLayout(value: LibraryLayout) = viewModelScope.launch { settingsRepository.setLibraryLayout(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(value) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { settingsRepository.setReduceMotion(value) }
    fun setReduceTransparency(value: Boolean) = viewModelScope.launch { settingsRepository.setReduceTransparency(value) }
    fun setAccentPalette(value: AccentPalette) = viewModelScope.launch { settingsRepository.setAccentPalette(value) }
    fun setContentDensity(value: ContentDensity) = viewModelScope.launch { settingsRepository.setContentDensity(value) }
    fun setStartupDestination(value: StartupDestination) = viewModelScope.launch { settingsRepository.setStartupDestination(value) }
    fun setGlassIntensity(value: Float) = viewModelScope.launch { settingsRepository.setGlassIntensity(value) }
    fun setCornerScale(value: Float) = viewModelScope.launch { settingsRepository.setCornerScale(value) }
    fun setFontScale(value: Float) = viewModelScope.launch { settingsRepository.setFontScale(value) }
    fun setTouchControlOpacity(value: Float) = viewModelScope.launch { settingsRepository.setTouchControlOpacity(value) }
    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch { settingsRepository.setHapticsEnabled(value) }
    fun setHighContrast(value: Boolean) = viewModelScope.launch { settingsRepository.setHighContrast(value) }
    fun setShowOnlineRecommendations(value: Boolean) = viewModelScope.launch { settingsRepository.setShowOnlineRecommendations(value) }
    fun setShowStatistics(value: Boolean) = viewModelScope.launch { settingsRepository.setShowStatistics(value) }
    fun setIntegerScaling(value: Boolean) = viewModelScope.launch { settingsRepository.setIntegerScaling(value) }
    fun setDisplaySmoothing(value: Boolean) = viewModelScope.launch { settingsRepository.setDisplaySmoothing(value) }
    fun setShowPerformanceOverlay(value: Boolean) = viewModelScope.launch { settingsRepository.setShowPerformanceOverlay(value) }
    fun setShowTouchControls(value: Boolean) = viewModelScope.launch { settingsRepository.setShowTouchControls(value) }
    fun setAudioEnabled(value: Boolean) = viewModelScope.launch { settingsRepository.setAudioEnabled(value) }
    fun setMasterVolume(value: Float) = viewModelScope.launch { settingsRepository.setMasterVolume(value) }
    fun setAutoSuspendOnBackground(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoSuspendOnBackground(value) }
    fun setPauseOnHeadphoneDisconnect(value: Boolean) = viewModelScope.launch { settingsRepository.setPauseOnHeadphoneDisconnect(value) }
    fun setFastForwardSpeed(value: Float) = viewModelScope.launch {
        settingsRepository.setFastForwardSpeed(value)
        emulationCore.setEmulationSpeed(value)
    }
    fun setPerformanceProfile(value: PerformanceProfile) = viewModelScope.launch {
        settingsRepository.setPerformanceProfile(value)
        emulationCore.setPerformanceProfile(value)
    }

    private fun recordElapsedPlaytime(game: GameRecord?) {
        val started = sessionStartedAtEpochMillis ?: return
        sessionStartedAtEpochMillis = null
        val seconds = ((System.currentTimeMillis() - started) / 1_000L).coerceAtLeast(0)
        if (seconds > 0) recordAchievement(AchievementEventType.PLAY_SECONDS, amount = seconds, game = game)
    }

    private fun recordAchievement(
        type: AchievementEventType,
        amount: Long = 1,
        uniqueKey: String? = null,
        game: GameRecord? = activeGame.value
    ) {
        viewModelScope.launch {
            val previouslyUnlocked = achievementRepository.statuses.value
                .filter { it.progress.unlockedAtEpochMillis != null }
                .mapTo(mutableSetOf()) { it.definition.id }
            val updated = achievementRepository.record(
                AchievementEvent(type, amount, uniqueKey, System.currentTimeMillis()),
                AchievementIntegrity(
                    cheatsActive = activeCheatIds.value.isNotEmpty(),
                    patchedRom = game?.origin == "LOCAL_PATCH",
                    verifiedRom = game != null || type in setOf(AchievementEventType.GAME_IMPORTED, AchievementEventType.ACHIEVEMENT_SHARED),
                    pureRun = false
                )
            )
            updated.firstOrNull { it.definition.id !in previouslyUnlocked && it.progress.unlockedAtEpochMillis != null }
                ?.let { _messages.emit("Achievement unlocked: ${it.definition.title} · ${it.definition.points} points") }
        }
    }

    private fun updateButton(button: EmulatorButton, pressed: Boolean) {
        if (pressed) pressedButtons += button else pressedButtons -= button
    }

    private fun clearInput() {
        synchronized(inputLock) {
            pressedButtons.clear()
            mutableControllerInput.value = emptySet()
            emulationCore.setInputState(EmulatorInputState(emptySet()))
        }
    }

    override fun onCleared() {
        recordElapsedPlaytime(activeGame.value)
        clearInput()
        audioOutput.close()
        (emulationCore as? AutoCloseable)?.close()
        super.onCleared()
    }
}
