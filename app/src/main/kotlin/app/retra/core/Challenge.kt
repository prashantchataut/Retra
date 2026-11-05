package app.retra.core

import kotlinx.serialization.Serializable

/**
 * Challenge runs. A run is bound to one game and lives in `challenges/<gameId>.json`.
 *
 * - [ChallengeMode.HONEST] ("No-Cheat Run"): cheats, rewind, loading save states and resetting are locked
 *   while the run is active. Fast-forward and in-game saving stay available. The run keeps a clock and a seal.
 * - [ChallengeMode.NUZLOCKE]: everything in Honest, plus an encounter log with the classic rules
 *   (first encounter per area only, fainted Pokémon are gone, nickname everything, wipe ends the run).
 *
 * The emulator can lock the *tools*; it can't see inside the game. So the Nuzlocke log is an honour-system
 * tracker that's quick enough to keep up with in the middle of a game.
 */
@Serializable
enum class ChallengeMode(val label: String) { OFF("Off"), HONEST("No-Cheat Run"), NUZLOCKE("Nuzlocke") }

@Serializable
enum class RunState { ACTIVE, COMPLETED, FAILED, ABANDONED }

/** The things a challenge may forbid. */
enum class PlayerTool(val label: String, val locked: String) {
    CHEATS("Cheats", "Cheats are locked"),
    REWIND("Rewind", "Rewind is locked"),
    LOAD_STATE("Loading save states", "Loading save states is locked"),
    RESET("Reset", "Reset is locked"),
}

@Serializable
enum class EncounterStatus(val label: String) {
    /** Caught and still in the team or box. */
    ALIVE("Alive"),
    /** Fainted; the run keeps it in the graveyard. */
    DEAD("Fainted"),
    /** Fled, KO'd or missed: this area's one chance is spent. */
    MISSED("Missed"),
    /** Skipped under the dupes or shiny clause, or never met. */
    SKIPPED("Skipped"),
}

@Serializable
data class NuzlockeRules(
    val dupesClause: Boolean = true,
    val shinyClause: Boolean = true,
    val mustNickname: Boolean = true,
    val wipeEndsRun: Boolean = true,
    val noItemsInBattle: Boolean = false,
)

@Serializable
data class Encounter(
    val id: String,
    val area: String,
    val species: String = "",
    val nickname: String = "",
    val status: EncounterStatus = EncounterStatus.ALIVE,
    val level: Int? = null,
    val note: String = "",
)

@Serializable
data class ChallengeRun(
    val mode: ChallengeMode,
    val startedAt: Long,
    val state: RunState = RunState.ACTIVE,
    val endedAt: Long? = null,
    /** Play time on the game when the run started, so the run clock is play time since then. */
    val playMsAtStart: Long = 0,
    val rules: NuzlockeRules = NuzlockeRules(),
    val encounters: List<Encounter> = emptyList(),
    /** How many times the player tried a locked tool (shown on the run card, never punished). */
    val blockedAttempts: Int = 0,
    val badges: Int = 0,
) {
    val active get() = state == RunState.ACTIVE && mode != ChallengeMode.OFF
    val alive get() = encounters.filter { it.status == EncounterStatus.ALIVE }
    val graveyard get() = encounters.filter { it.status == EncounterStatus.DEAD }
    val usedAreas get() = encounters.map { it.area.trim().lowercase() }.toSet()
}

object ChallengePolicy {
    /** Whether [tool] may be used in [run]. Runs that are over or absent lock nothing. */
    fun allows(run: ChallengeRun?, tool: PlayerTool): Boolean {
        if (run == null || !run.active) return true
        return when (run.mode) {
            ChallengeMode.OFF -> true
            ChallengeMode.HONEST, ChallengeMode.NUZLOCKE -> false
        }
    }

    /** One short sentence for the toast shown when a locked tool is tapped. */
    fun lockedMessage(run: ChallengeRun?, tool: PlayerTool): String =
        "${tool.locked} during your ${run?.mode?.label ?: "challenge"}. End the run on the game page to unlock it."

    /** Nuzlocke only makes sense for Pokémon games. */
    fun available(isPokemon: Boolean): List<ChallengeMode> =
        if (isPokemon) listOf(ChallengeMode.HONEST, ChallengeMode.NUZLOCKE) else listOf(ChallengeMode.HONEST)
}

/** Pure state transitions for the Nuzlocke log. Each returns a new run. */
object Nuzlocke {
    /** Why an encounter for [area] can't be logged, or null when it can. */
    fun blocker(run: ChallengeRun, area: String, species: String, shiny: Boolean = false): String? {
        val key = area.trim().lowercase()
        if (key.isEmpty()) return "Name the area first."
        if (key in run.usedAreas && !(shiny && run.rules.shinyClause)) return "You already used your encounter in ${area.trim()}."
        if (run.rules.dupesClause && species.isNotBlank() &&
            run.encounters.any { it.species.equals(species.trim(), ignoreCase = true) && it.status != EncounterStatus.SKIPPED }
        ) return "Dupes clause: ${species.trim()} is already in your log. Log it as Skipped."
        return null
    }

    fun add(run: ChallengeRun, e: Encounter): ChallengeRun = run.copy(encounters = run.encounters + e)

    fun setStatus(run: ChallengeRun, id: String, status: EncounterStatus): ChallengeRun =
        run.copy(encounters = run.encounters.map { if (it.id == id) it.copy(status = status) else it })

    fun remove(run: ChallengeRun, id: String): ChallengeRun = run.copy(encounters = run.encounters.filterNot { it.id == id })

    /** True when the log says every caught Pokémon has fainted (and there was at least one). */
    fun isWipe(run: ChallengeRun): Boolean {
        val caught = run.encounters.filter { it.status == EncounterStatus.ALIVE || it.status == EncounterStatus.DEAD }
        return caught.isNotEmpty() && caught.none { it.status == EncounterStatus.ALIVE }
    }

    fun applyWipeRule(run: ChallengeRun, now: Long): ChallengeRun =
        if (run.rules.wipeEndsRun && run.active && isWipe(run)) run.copy(state = RunState.FAILED, endedAt = now) else run

    fun survivalRate(run: ChallengeRun): Int {
        val caught = run.encounters.count { it.status == EncounterStatus.ALIVE || it.status == EncounterStatus.DEAD }
        return if (caught == 0) 100 else (run.alive.size * 100) / caught
    }
}

/** First-area names per generation 3 game, so the tracker can suggest the next one. */
object RouteSuggestions {
    private val kanto = listOf(
        "Starter", "Route 1", "Viridian Forest", "Route 2", "Route 3", "Mt. Moon", "Route 4", "Route 24", "Route 25", "Route 5",
        "Route 6", "Vermilion City", "Route 11", "Diglett's Cave", "Route 9", "Rock Tunnel", "Route 10", "Pokémon Tower", "Route 8", "Route 7",
        "Safari Zone", "Seafoam Islands", "Pokémon Mansion", "Victory Road",
    )
    private val hoenn = listOf(
        "Starter", "Route 101", "Route 102", "Petalburg Woods", "Route 104", "Rustboro City", "Route 116", "Dewford Town", "Granite Cave", "Route 110",
        "Route 111", "Route 112", "Fiery Path", "Jagged Pass", "Route 113", "Route 114", "Meteor Falls", "Route 119", "Route 120", "Route 121",
        "Safari Zone", "Route 122", "Mt. Pyre", "Shoal Cave", "Seafloor Cavern", "Sky Pillar", "Victory Road",
    )

    fun forGame(gameCode: String): List<String> = when (gameCode.take(3).uppercase()) {
        "BPR", "BPG" -> kanto
        "AXV", "AXP", "BPE" -> hoenn
        else -> listOf("Starter", "First route", "First cave", "First forest", "First city")
    }

    fun next(gameCode: String, run: ChallengeRun): String? =
        forGame(gameCode).firstOrNull { it.lowercase() !in run.usedAreas }
}
