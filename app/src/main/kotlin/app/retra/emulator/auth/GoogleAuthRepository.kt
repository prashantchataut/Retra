package app.retra.emulator.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.retra.emulator.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.retraIdentityStore by preferencesDataStore(name = "retra_identity")

/**
 * Android-side Google identity integration using Credential Manager.
 *
 * A Google ID token is intentionally returned only in memory. Retra's future server must validate
 * audience, issuer, signature, expiry, and the nonce before granting cloud or social privileges.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val providerId = stringPreferencesKey("provider_id")
        val email = stringPreferencesKey("email")
        val displayName = stringPreferencesKey("display_name")
        val pictureUri = stringPreferencesKey("picture_uri")
        val connectedAt = longPreferencesKey("connected_at")
        val trustLevel = stringPreferencesKey("trust_level")
        val tokenFingerprint = stringPreferencesKey("token_fingerprint")
    }

    val isConfigured: Boolean
        get() = BuildConfig.RETRA_GOOGLE_WEB_CLIENT_ID.isNotBlank()

    val account: Flow<RetraAccount?> = context.retraIdentityStore.data.map { preferences ->
        val providerId = preferences[Keys.providerId] ?: return@map null
        val email = preferences[Keys.email] ?: return@map null
        RetraAccount(
            providerId = providerId,
            email = email,
            displayName = preferences[Keys.displayName],
            profilePictureUri = preferences[Keys.pictureUri],
            connectedAtEpochMillis = preferences[Keys.connectedAt] ?: 0L,
            trustLevel = preferences[Keys.trustLevel]
                ?.let { runCatching { AccountTrustLevel.valueOf(it) }.getOrNull() }
                ?: AccountTrustLevel.GOOGLE_CREDENTIAL_RECEIVED,
            tokenFingerprint = preferences[Keys.tokenFingerprint]
        )
    }

    suspend fun signIn(activityContext: Context): GoogleSignInResult {
        val clientId = BuildConfig.RETRA_GOOGLE_WEB_CLIENT_ID.trim()
        if (clientId.isBlank()) return GoogleSignInResult.MissingConfiguration
        val nonce = secureNonce()
        val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return try {
            val response = CredentialManager.create(activityContext).getCredential(
                request = request,
                context = activityContext
            )
            val credential = response.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return GoogleSignInResult.Failed("The credential provider returned an unsupported credential type.")
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val token = google.idToken
            val account = RetraAccount(
                providerId = "google",
                email = google.id,
                displayName = google.displayName,
                profilePictureUri = google.profilePictureUri?.toString(),
                connectedAtEpochMillis = System.currentTimeMillis(),
                trustLevel = AccountTrustLevel.GOOGLE_CREDENTIAL_RECEIVED,
                tokenFingerprint = token.sha256().take(16)
            )
            persist(account)
            GoogleSignInResult.Connected(account, token, nonce)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.NoCredential
        } catch (error: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failed("Google returned an ID credential that Retra could not parse. Update Google Identity libraries and try again.")
        } catch (error: GetCredentialException) {
            GoogleSignInResult.Failed(error.message?.takeIf(String::isNotBlank) ?: "Google sign-in could not be completed.")
        } catch (error: Exception) {
            GoogleSignInResult.Failed(error.message ?: "Google sign-in could not be completed.")
        }
    }

    suspend fun signOut(activityContext: Context) {
        runCatching {
            CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest())
        }
        context.retraIdentityStore.edit { it.clear() }
    }

    suspend fun markServerVerified() {
        context.retraIdentityStore.edit { preferences ->
            if (preferences[Keys.providerId] != null) {
                preferences[Keys.trustLevel] = AccountTrustLevel.SERVER_VERIFIED.name
            }
        }
    }

    private suspend fun persist(account: RetraAccount) {
        context.retraIdentityStore.edit { preferences ->
            preferences[Keys.providerId] = account.providerId
            preferences[Keys.email] = account.email
            account.displayName?.let { preferences[Keys.displayName] = it } ?: preferences.remove(Keys.displayName)
            account.profilePictureUri?.let { preferences[Keys.pictureUri] = it } ?: preferences.remove(Keys.pictureUri)
            preferences[Keys.connectedAt] = account.connectedAtEpochMillis
            preferences[Keys.trustLevel] = account.trustLevel.name
            account.tokenFingerprint?.let { preferences[Keys.tokenFingerprint] = it }
                ?: preferences.remove(Keys.tokenFingerprint)
        }
    }

    private fun secureNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
