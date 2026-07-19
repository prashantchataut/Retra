package app.retra.core.social

import java.security.MessageDigest

enum class SocialProvider { DISCORD, BLUESKY, MASTODON, REDDIT, CUSTOM }
enum class SocialConnectionState { DISCONNECTED, REQUIRES_CONFIGURATION, CONNECTING, CONNECTED, ERROR }
enum class SharePrivacy { TITLE_ONLY, SUMMARY, FULL_PUBLIC_PROFILE }

data class SocialConnection(
    val provider: SocialProvider,
    val state: SocialConnectionState,
    val displayName: String? = null,
    val profileUrl: String? = null,
    val errorMessage: String? = null
)

data class PlayerProfile(
    val profileId: String,
    val displayName: String,
    val bio: String = "",
    val friendCode: String,
    val publicAchievementIds: Set<String> = emptySet(),
    val showPlaytime: Boolean = false,
    val showLibraryCount: Boolean = true
) {
    init {
        require(profileId.matches(Regex("[a-zA-Z0-9._-]{3,64}")))
        require(displayName.isNotBlank() && displayName.length <= 40)
        require(bio.length <= 240)
        require(FriendCode.isValid(friendCode))
    }
}

data class ShareCard(
    val title: String,
    val body: String,
    val deepLink: String? = null
)

object FriendCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val pattern = Regex("RETRA-[A-Z2-9]{4}-[A-Z2-9]{4}")

    fun fromProfileId(profileId: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(profileId.encodeToByteArray())
        val chars = CharArray(8) { index -> ALPHABET[(hash[index].toInt() and 0xFF) % ALPHABET.length] }
        return "RETRA-${chars.concatToString(0, 4)}-${chars.concatToString(4, 8)}"
    }

    fun isValid(value: String): Boolean = pattern.matches(value)
}

object SocialShareFactory {
    fun achievement(
        profile: PlayerProfile,
        achievementTitle: String,
        achievementDescription: String,
        points: Int,
        privacy: SharePrivacy,
        deepLink: String? = null
    ): ShareCard {
        require(achievementTitle.isNotBlank())
        require(points >= 0)
        val safeTitle = sanitize(achievementTitle, 100)
        val body = when (privacy) {
            SharePrivacy.TITLE_ONLY -> "${profile.displayName} unlocked $safeTitle in Retra."
            SharePrivacy.SUMMARY -> "${profile.displayName} unlocked $safeTitle ($points points) in Retra. ${sanitize(achievementDescription, 180)}"
            SharePrivacy.FULL_PUBLIC_PROFILE -> "${profile.displayName} unlocked $safeTitle ($points points) in Retra. Friend code: ${profile.friendCode}. ${sanitize(achievementDescription, 160)}"
        }
        return ShareCard("Retra achievement: $safeTitle", body, validateDeepLink(deepLink))
    }

    fun multiplayerInvite(profile: PlayerProfile, roomCode: String, deepLink: String): ShareCard {
        require(roomCode.matches(Regex("[A-Z2-9]{6}"))) { "Invalid room code." }
        return ShareCard(
            title = "Join ${profile.displayName} in Retra",
            body = "Join my Retra multiplayer room with code $roomCode. ROM and core compatibility are verified before play.",
            deepLink = validateDeepLink(deepLink)
        )
    }

    private fun sanitize(value: String, maximum: Int): String = value
        .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(maximum)

    private fun validateDeepLink(value: String?): String? {
        if (value == null) return null
        require(value.startsWith("retra://") || value.startsWith("https://")) { "Unsupported share link." }
        require(value.length <= 512)
        return value
    }
}
