package app.retra.emulator.auth

enum class AccountTrustLevel {
    LOCAL_ONLY,
    GOOGLE_CREDENTIAL_RECEIVED,
    SERVER_VERIFIED
}

data class RetraAccount(
    val providerId: String,
    val email: String,
    val displayName: String?,
    val profilePictureUri: String?,
    val connectedAtEpochMillis: Long,
    val trustLevel: AccountTrustLevel,
    val tokenFingerprint: String? = null
) {
    val initials: String
        get() = displayName
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter(String::isNotBlank)
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.joinToString("")
            ?.ifBlank { null }
            ?: email.firstOrNull()?.uppercaseChar()?.toString().orEmpty().ifBlank { "R" }
}

sealed interface GoogleSignInResult {
    data class Connected(
        val account: RetraAccount,
        /** Ephemeral ID token. Callers must send it to a trusted backend and must not persist it. */
        val idToken: String,
        val requestNonce: String
    ) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult
    data object NoCredential : GoogleSignInResult
    data object MissingConfiguration : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

enum class AuthOperation {
    IDLE,
    SIGNING_IN,
    SIGNING_OUT
}
