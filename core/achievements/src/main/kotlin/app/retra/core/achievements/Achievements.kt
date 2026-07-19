package app.retra.core.achievements

enum class AchievementEventType {
    GAME_IMPORTED,
    GAME_STARTED,
    PLAY_SECONDS,
    SAVE_CREATED,
    PATCH_APPLIED,
    CHEAT_PACK_IMPORTED,
    ACHIEVEMENT_SHARED,
    MULTIPLAYER_SESSION_COMPLETED,
    GAME_COMPLETED
}

data class AchievementEvent(
    val type: AchievementEventType,
    val amount: Long = 1,
    val uniqueKey: String? = null,
    val occurredAtEpochMillis: Long
) {
    init {
        require(amount >= 0) { "Achievement event amount cannot be negative." }
        require(uniqueKey == null || uniqueKey.length <= 128) { "Achievement unique key is too long." }
    }
}

enum class AchievementIntegrityPolicy {
    ANY,
    NO_ACTIVE_CHEATS,
    VERIFIED_UNPATCHED_ROM,
    PURE_RUN
}

data class AchievementIntegrity(
    val cheatsActive: Boolean = false,
    val patchedRom: Boolean = false,
    val verifiedRom: Boolean = true,
    val pureRun: Boolean = false
)

sealed interface AchievementRule {
    val eventType: AchievementEventType

    data class Count(
        override val eventType: AchievementEventType,
        val target: Long
    ) : AchievementRule {
        init { require(target > 0) }
    }

    data class Sum(
        override val eventType: AchievementEventType,
        val target: Long
    ) : AchievementRule {
        init { require(target > 0) }
    }

    data class Unique(
        override val eventType: AchievementEventType,
        val target: Int
    ) : AchievementRule {
        init { require(target > 0) }
    }
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val points: Int,
    val hidden: Boolean,
    val integrityPolicy: AchievementIntegrityPolicy,
    val rule: AchievementRule
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{0,63}"))) { "Invalid achievement ID." }
        require(title.isNotBlank() && title.length <= 100)
        require(description.isNotBlank() && description.length <= 300)
        require(points in 1..10_000)
    }
}

data class AchievementProgress(
    val achievementId: String,
    val count: Long = 0,
    val sum: Long = 0,
    val uniqueKeys: Set<String> = emptySet(),
    val unlockedAtEpochMillis: Long? = null
)

data class AchievementEvaluation(
    val progress: AchievementProgress,
    val newlyUnlocked: Boolean,
    val eligible: Boolean,
    val ineligibleReason: String? = null
)

object AchievementEngine {
    fun evaluate(
        definition: AchievementDefinition,
        current: AchievementProgress?,
        event: AchievementEvent,
        integrity: AchievementIntegrity
    ): AchievementEvaluation {
        val existing = current ?: AchievementProgress(definition.id)
        if (existing.unlockedAtEpochMillis != null) {
            return AchievementEvaluation(existing, newlyUnlocked = false, eligible = true)
        }
        val reason = ineligibleReason(definition.integrityPolicy, integrity)
        if (reason != null) {
            return AchievementEvaluation(existing, newlyUnlocked = false, eligible = false, ineligibleReason = reason)
        }
        if (definition.rule.eventType != event.type) {
            return AchievementEvaluation(existing, newlyUnlocked = false, eligible = true)
        }

        val updated = when (definition.rule) {
            is AchievementRule.Count -> existing.copy(count = safeAdd(existing.count, event.amount.coerceAtLeast(1)))
            is AchievementRule.Sum -> existing.copy(sum = safeAdd(existing.sum, event.amount))
            is AchievementRule.Unique -> {
                val key = event.uniqueKey?.trim()?.takeIf(String::isNotEmpty)
                    ?: return AchievementEvaluation(existing, false, true)
                existing.copy(uniqueKeys = existing.uniqueKeys + key)
            }
        }
        val reached = when (val rule = definition.rule) {
            is AchievementRule.Count -> updated.count >= rule.target
            is AchievementRule.Sum -> updated.sum >= rule.target
            is AchievementRule.Unique -> updated.uniqueKeys.size >= rule.target
        }
        val finalProgress = if (reached) updated.copy(unlockedAtEpochMillis = event.occurredAtEpochMillis) else updated
        return AchievementEvaluation(finalProgress, newlyUnlocked = reached, eligible = true)
    }

    fun completionRatio(definition: AchievementDefinition, progress: AchievementProgress?): Float {
        val value = when (val rule = definition.rule) {
            is AchievementRule.Count -> (progress?.count ?: 0).toDouble() / rule.target
            is AchievementRule.Sum -> (progress?.sum ?: 0).toDouble() / rule.target
            is AchievementRule.Unique -> (progress?.uniqueKeys?.size ?: 0).toDouble() / rule.target
        }
        return value.coerceIn(0.0, 1.0).toFloat()
    }

    private fun ineligibleReason(policy: AchievementIntegrityPolicy, state: AchievementIntegrity): String? = when (policy) {
        AchievementIntegrityPolicy.ANY -> null
        AchievementIntegrityPolicy.NO_ACTIVE_CHEATS -> if (state.cheatsActive) "Active cheats disable this achievement." else null
        AchievementIntegrityPolicy.VERIFIED_UNPATCHED_ROM -> when {
            !state.verifiedRom -> "The ROM identity is not verified."
            state.patchedRom -> "This achievement requires an unpatched ROM."
            else -> null
        }
        AchievementIntegrityPolicy.PURE_RUN -> when {
            !state.pureRun -> "This achievement requires Pure Run."
            state.cheatsActive -> "Pure Run does not allow active cheats."
            !state.verifiedRom -> "Pure Run requires a verified ROM."
            else -> null
        }
    }

    private fun safeAdd(left: Long, right: Long): Long = if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
}

object RetraAchievements {
    val builtIns: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "library.first-memory",
            title = "First Memory",
            description = "Import your first verified game into Retra.",
            points = 10,
            hidden = false,
            integrityPolicy = AchievementIntegrityPolicy.ANY,
            rule = AchievementRule.Count(AchievementEventType.GAME_IMPORTED, 1)
        ),
        AchievementDefinition(
            id = "library.curator",
            title = "Personal Curator",
            description = "Build a library containing ten distinct verified games.",
            points = 40,
            hidden = false,
            integrityPolicy = AchievementIntegrityPolicy.ANY,
            rule = AchievementRule.Unique(AchievementEventType.GAME_IMPORTED, 10)
        ),
        AchievementDefinition(
            id = "vault.careful-adventurer",
            title = "Careful Adventurer",
            description = "Create twenty save-state snapshots.",
            points = 25,
            hidden = false,
            integrityPolicy = AchievementIntegrityPolicy.ANY,
            rule = AchievementRule.Count(AchievementEventType.SAVE_CREATED, 20)
        ),
        AchievementDefinition(
            id = "playtime.old-friend",
            title = "Old Friend",
            description = "Play for ten verified hours without active cheats.",
            points = 100,
            hidden = false,
            integrityPolicy = AchievementIntegrityPolicy.NO_ACTIVE_CHEATS,
            rule = AchievementRule.Sum(AchievementEventType.PLAY_SECONDS, 36_000)
        ),
        AchievementDefinition(
            id = "multiplayer.linked",
            title = "Linked Memories",
            description = "Complete a verified multiplayer session.",
            points = 30,
            hidden = false,
            integrityPolicy = AchievementIntegrityPolicy.VERIFIED_UNPATCHED_ROM,
            rule = AchievementRule.Count(AchievementEventType.MULTIPLAYER_SESSION_COMPLETED, 1)
        ),
        AchievementDefinition(
            id = "pure-run.complete",
            title = "As It Was",
            description = "Complete a game under Pure Run integrity rules.",
            points = 250,
            hidden = true,
            integrityPolicy = AchievementIntegrityPolicy.PURE_RUN,
            rule = AchievementRule.Count(AchievementEventType.GAME_COMPLETED, 1)
        )
    )
}
