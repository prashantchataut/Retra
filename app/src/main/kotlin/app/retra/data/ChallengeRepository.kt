package app.retra.data

import app.retra.core.AtomicFiles
import app.retra.core.ChallengeMode
import app.retra.core.ChallengeRun
import app.retra.core.Encounter
import app.retra.core.Nuzlocke
import app.retra.core.NuzlockeRules
import app.retra.core.RetraJson
import app.retra.core.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The challenge run of each game, in `challenges/<gameId>.json`. A game has at most one run; finished runs stay
 * on file so the game page can show how it went, and starting a new run replaces them.
 */
class ChallengeRepository(root: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val dir = File(root, "challenges")
    private val lock = Any()
    private val _runs = MutableStateFlow(loadAll())
    val runs: StateFlow<Map<String, ChallengeRun>> = _runs.asStateFlow()

    private fun file(id: String) = File(dir, "$id.json")

    private fun read(f: File): ChallengeRun? =
        runCatching { RetraJson.decodeFromString(ChallengeRun.serializer(), f.readText()) }.getOrNull()

    private fun loadAll(): Map<String, ChallengeRun> =
        dir.listFiles { f -> f.extension == "json" }?.mapNotNull { f -> read(f)?.let { f.nameWithoutExtension to it } }?.toMap() ?: emptyMap()

    fun load(id: String): ChallengeRun? = _runs.value[id]

    private fun write(id: String, run: ChallengeRun?) = synchronized(lock) {
        if (run == null) file(id).delete() else AtomicFiles.writeText(file(id), RetraJson.encodeToString(ChallengeRun.serializer(), run))
        _runs.value = if (run == null) _runs.value - id else _runs.value + (id to run)
    }

    /** Starts (or restarts) a run. [playMs] is the game's play time now, the zero point of the run clock. */
    fun start(id: String, mode: ChallengeMode, playMs: Long, rules: NuzlockeRules = NuzlockeRules()): ChallengeRun {
        require(mode != ChallengeMode.OFF)
        val run = ChallengeRun(mode = mode, startedAt = clock(), playMsAtStart = playMs, rules = rules)
        write(id, run)
        return run
    }

    fun update(id: String, transform: (ChallengeRun) -> ChallengeRun): ChallengeRun? {
        val current = load(id) ?: return null
        val next = transform(current)
        write(id, next)
        return next
    }

    fun finish(id: String, state: RunState): ChallengeRun? =
        update(id) { if (it.active) it.copy(state = state, endedAt = clock()) else it }

    fun recordBlocked(id: String): ChallengeRun? = update(id) { it.copy(blockedAttempts = it.blockedAttempts + 1) }

    fun addEncounter(id: String, e: Encounter): ChallengeRun? =
        update(id) { Nuzlocke.applyWipeRule(Nuzlocke.add(it, e), clock()) }

    fun setStatus(id: String, encounterId: String, status: app.retra.core.EncounterStatus): ChallengeRun? =
        update(id) { Nuzlocke.applyWipeRule(Nuzlocke.setStatus(it, encounterId, status), clock()) }

    fun clear(id: String) = write(id, null)
}
