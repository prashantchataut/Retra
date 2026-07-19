# Known Issues

- The repaired Android project could not be recompiled here because the sandbox lacks Gradle and Android SDK/NDK tooling.
- The archive excludes compiled mGBA libraries; source acquisition/build scripts are provided.
- SoundPool output, notification sounds, haptic strength, Bluetooth routing, and OEM notification behavior need physical-device testing.
- Compose blur is decorative and ignored by older Android versions; the UI remains usable through translucent/opaque surfaces.
- Notification channel sound/vibration choices become system-controlled after channel creation, as required by Android.
- Google sign-in requires a real Web OAuth client ID; cloud trust additionally requires backend token/nonce verification.
- Rewind cadence, touch ergonomics, controller matrix, screenshots, custom artwork, and save compatibility require device profiling.
- Drag-to-position touch controls, cloud sync, provider OAuth, hosted relay, and real GBA link-cable callbacks remain incomplete.
- Room schema export and migrations must be exercised by a real AGP build.
