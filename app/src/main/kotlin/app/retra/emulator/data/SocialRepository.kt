package app.retra.emulator.data

import android.content.Context
import android.content.Intent
import app.retra.core.social.FriendCode
import app.retra.core.social.PlayerProfile
import app.retra.core.social.ShareCard
import app.retra.core.social.SocialConnection
import app.retra.core.social.SocialConnectionState
import app.retra.core.social.SocialProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class SocialRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences = context.getSharedPreferences("retra_social", Context.MODE_PRIVATE)
    private val profileId = preferences.getString("profile_id", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("profile_id", it).apply()
    }
    private val mutableProfile = MutableStateFlow(loadProfile())
    val profile: StateFlow<PlayerProfile> = mutableProfile
    private val mutableConnections = MutableStateFlow(loadConnections())
    val connections: StateFlow<List<SocialConnection>> = mutableConnections

    fun updateProfile(displayName: String, bio: String): Result<Unit> = runCatching {
        val updated = PlayerProfile(
            profileId = profileId,
            displayName = displayName.trim(),
            bio = bio.trim(),
            friendCode = FriendCode.fromProfileId(profileId),
            publicAchievementIds = mutableProfile.value.publicAchievementIds,
            showPlaytime = mutableProfile.value.showPlaytime,
            showLibraryCount = mutableProfile.value.showLibraryCount
        )
        preferences.edit().putString("display_name", updated.displayName).putString("bio", updated.bio).apply()
        mutableProfile.value = updated
    }

    fun share(card: ShareCard): Result<Unit> = runCatching {
        val content = buildString {
            append(card.body)
            card.deepLink?.let { append("\n\n").append(it) }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, card.title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share from Retra").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun setProviderConfiguration(provider: SocialProvider, displayName: String?, profileUrl: String?): Result<Unit> = runCatching {
        if (profileUrl != null) require(profileUrl.startsWith("https://")) { "Social profile URLs must use HTTPS." }
        val safeName = displayName?.trim()?.take(80)
        val safeUrl = profileUrl?.trim()?.take(512)
        preferences.edit()
            .putString("provider_${provider.name}_name", safeName)
            .putString("provider_${provider.name}_url", safeUrl)
            .apply()
        mutableConnections.value = mutableConnections.value.map { connection ->
            if (connection.provider == provider) {
                SocialConnection(provider, SocialConnectionState.CONNECTED, safeName, safeUrl)
            } else connection
        }
    }


    private fun loadConnections(): List<SocialConnection> = SocialProvider.entries.map { provider ->
        val name = preferences.getString("provider_${provider.name}_name", null)
        val url = preferences.getString("provider_${provider.name}_url", null)
        if (!name.isNullOrBlank() || !url.isNullOrBlank()) {
            SocialConnection(provider, SocialConnectionState.CONNECTED, name, url)
        } else {
            SocialConnection(provider, SocialConnectionState.REQUIRES_CONFIGURATION)
        }
    }

    private fun loadProfile(): PlayerProfile = PlayerProfile(
        profileId = profileId,
        displayName = preferences.getString("display_name", "Retra Player") ?: "Retra Player",
        bio = preferences.getString("bio", "Reliving the games that made me.") ?: "",
        friendCode = FriendCode.fromProfileId(profileId)
    )
}
