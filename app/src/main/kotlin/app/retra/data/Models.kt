package app.retra.data

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class ScreenFilter { SHARP, SMOOTH }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val controlsOpacity: Float = 0.8f,
    val controlsScale: Float = 1f,
    val screenFilter: ScreenFilter = ScreenFilter.SHARP,
    val integerScale: Boolean = false,
    val colorCorrection: Boolean = true,
    val showFps: Boolean = false,
    val fastForwardSpeed: Int = 2,
    val fastForwardAudio: Boolean = false,
    val rewindEnabled: Boolean = true,
    /** Leaving a game writes a Continue snapshot (auto state). In-game saves are kept either way. */
    val autoSaveOnExit: Boolean = true,
    /** Fetch box art for imported cartridges from libretro-thumbnails. */
    val downloadCoverArt: Boolean = true,
    val rtcOffsetSeconds: Long = 0,
    val onboardingDone: Boolean = false,
    val bundledInstalled: Boolean = false,
    /** Catalog ids of Retra Originals already added once (a deleted one isn't re-added). */
    val installedOriginals: List<String> = emptyList(),
)

@Serializable
enum class GameSource { IMPORTED, BUNDLED, DOWNLOADED, PATCHED }

@Serializable
enum class GameCategory { POKEMON, FAN_GAME, HOMEBREW, OTHER }

/** Where a patched game came from. */
@Serializable
data class Provenance(
    val patchName: String,
    val patchFormat: String,
    val baseTitle: String,
    val baseCrc32: String,
    val catalogId: String? = null,
    val appliedAt: Long,
    val forced: Boolean = false,
)

@Serializable
data class Game(
    val id: String,
    val title: String,
    val romTitle: String,
    val gameCode: String,
    val makerCode: String = "",
    val romVersion: Int = 0,
    val sizeBytes: Long,
    val crc32: String,
    val sha1: String,
    val addedAt: Long,
    val lastPlayedAt: Long? = null,
    val playTimeMs: Long = 0,
    val favorite: Boolean = false,
    val source: GameSource = GameSource.IMPORTED,
    val category: GameCategory = GameCategory.OTHER,
    val catalogId: String? = null,
    val provenance: Provenance? = null,
    val saveType: String? = null,
    val hasRtc: Boolean? = null,
    /** Asset path of bundled cover art, e.g. "games/mochi_hop_cover.png". */
    val coverAsset: String? = null,
    /** When box art was last saved for this game (boxart/<CRC32>.png); bumps the cover. */
    val artAt: Long? = null,
)

@Serializable
data class Cheat(
    val id: String,
    val name: String,
    val code: String,
    val enabled: Boolean = true,
)

/** One save-state slot. Slot 0 is the automatic "Continue" slot. */
data class StateSlot(
    val slot: Int,
    val exists: Boolean,
    val savedAt: Long?,
    val thumbnailPath: String?,
)
