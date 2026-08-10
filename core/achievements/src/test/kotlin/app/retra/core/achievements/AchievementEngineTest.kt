package app.retra.core.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates achievement engine evaluation, integrity policies,
 * duplicate deduplication, and unlock triggers.
 */
class AchievementEngineTest {

    @Test
    fun firstMemoryUnlocksOnGameImport() {
        val definition = RetraAchievements.builtIns.first { it.id == "library.first-memory" }
        val result = AchievementEngine.evaluate(
            definition = definition,
            current = null,
            event = AchievementEvent(
                type = AchievementEventType.GAME_IMPORTED,
                uniqueKey = "a".repeat(64),
                occurredAtEpochMillis = 1000L
            ),
            integrity = AchievementIntegrity(cheatsActive = false)
        )

        assertTrue(result.newlyUnlocked)
        assertEquals(1000L, result.progress.unlockedAtEpochMillis)
        assertEquals(1.0f, AchievementEngine.completionRatio(definition, result.progress), 0.01f)
    }

    @Test
    fun cheatsActiveBlocksPlaytimeAchievements() {
        val definition = RetraAchievements.builtIns.first { it.id == "playtime.old-friend" }
        val result = AchievementEngine.evaluate(
            definition = definition,
            current = null,
            event = AchievementEvent(
                type = AchievementEventType.PLAY_SECONDS,
                amount = 36_000L,
                occurredAtEpochMillis = 2000L
            ),
            integrity = AchievementIntegrity(cheatsActive = true)
        )

        assertFalse(result.eligible)
        assertFalse(result.newlyUnlocked)
    }

    @Test
    fun curatorDeduplicatesSameGameImports() {
        val definition = RetraAchievements.builtIns.first { it.id == "library.curator" }
        val first = AchievementEngine.evaluate(
            definition = definition,
            current = null,
            event = AchievementEvent(
                type = AchievementEventType.GAME_IMPORTED,
                uniqueKey = "same-game-hash",
                occurredAtEpochMillis = 100L
            ),
            integrity = AchievementIntegrity()
        )

        val second = AchievementEngine.evaluate(
            definition = definition,
            current = first.progress,
            event = AchievementEvent(
                type = AchievementEventType.GAME_IMPORTED,
                uniqueKey = "same-game-hash",
                occurredAtEpochMillis = 200L
            ),
            integrity = AchievementIntegrity()
        )

        assertEquals(1, second.progress.uniqueKeys.size)
    }
}
