# Google Sign-In Setup

Retra uses Android Credential Manager and the official Google ID helper library. Google identity is optional; local ROM play, saves, cheats, patches, screenshots, rewind, and achievements must remain available offline.

## Android configuration

1. Create an OAuth 2.0 **Web application** client ID in Google Cloud Console.
2. Configure the Android package `app.retra.emulator` and release signing certificate fingerprints in the same project.
3. Provide the web client ID at build time:

```bash
./gradlew :app:assembleDebug -PRETRA_GOOGLE_WEB_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

The value can also be supplied through the `RETRA_GOOGLE_WEB_CLIENT_ID` environment variable. It is compiled into `BuildConfig` and is not a secret.

## Security boundary

Credential Manager returns a Google ID token and Retra supplies a cryptographically random nonce. The Android client:

- parses the official `GoogleIdTokenCredential`;
- persists only display identity and a short token fingerprint;
- never persists the raw ID token;
- clears provider state on disconnect;
- does not grant cloud or social privileges merely because the token was received.

A production Retra backend must verify the token signature, issuer, audience, expiry, nonce, and account status before issuing its own revocable session. The backend must never accept a user ID or email sent without a verified ID token.

Official references:

- https://developer.android.com/identity/sign-in/credential-manager-siwg
- https://developer.android.com/identity/sign-in/credential-manager
- https://developer.android.com/privacy-and-security/risks/unsafe-use-of-deeplinks
