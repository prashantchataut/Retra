# Google Drive saves: setup for your own build

Retra backs battery saves up to the hidden **app data folder** of the player's own Google Drive
(scope `https://www.googleapis.com/auth/drive.appdata`). There is no Retra server. Google identifies the app by
package name plus signing certificate, so a build you sign yourself needs its own OAuth client:

1. In [Google Cloud Console](https://console.cloud.google.com/) create a project and enable the **Google Drive API**.
2. Configure the OAuth consent screen and add the `drive.appdata` scope.
3. Create an OAuth client ID of type **Android** with package `app.retra.emulator` and the SHA-1 of your signing key
   (`./gradlew :app:signingReport`). Add one per key (debug and release).
4. Install the build. Settings → Cheats, cloud and friends → Google Drive saves → Connect.

Sync rules (`core/DriveSyncPlanner.kt`, unit-tested): a save only in one place is copied across; identical saves (MD5) are
skipped; if both differ the newest wins and the replaced local save is kept in Retra's rolling backups.
