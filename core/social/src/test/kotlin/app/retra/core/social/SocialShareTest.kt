package app.retra.core.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates privacy-safe friend codes and social share card generation.
 */
class SocialShareTest {

    @Test
    fun friendCodeDerivationAndValidation() {
        val code = FriendCode.fromProfileId("local-player-123")
        assertTrue(FriendCode.isValid(code))
        assertFalse(FriendCode.isValid("INVALID-CODE"))
    }

    @Test
    fun achievementShareExcludesFriendCodeInSummaryMode() {
        val friendCode = FriendCode.fromProfileId("test-profile")
        val profile = PlayerProfile(
            profileId = "test-profile",
            displayName = "Player Seven",
            friendCode = friendCode
        )

        val card = SocialShareFactory.achievement(
            profile = profile,
            title = "First Memory",
            description = "Imported your first game into the archive.",
            points = 10,
            privacy = SharePrivacy.SUMMARY,
            deepLink = "retra://achievement/library.first-memory"
        )

        assertTrue(card.body.contains("First Memory"))
        assertFalse(card.body.contains(friendCode))
    }
}
