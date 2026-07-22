# Known Issues and Validation Gaps — Retra 2.0.0

- Android/Compose compilation was not run because the Gradle 9.5.0 distribution was not cached and DNS could not resolve `services.gradle.org`.
- Room schema 6 export and 5→6 migration instrumentation tests still require a successful Android Gradle build.
- Final mGBA libraries must be staged and verified for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Homebrew Hub's documented file objects do not expose an expected digest. Retra records a local SHA-256 after TLS download and GBA validation; this is not equivalent to a checksum-pinned release.
- Homebrew Hub and Libretro integrations need live Android tests for response changes, rate limits, timeouts, offline behavior, and malformed upstream data.
- Libretro cheat filenames are title-based and community-maintained. Exact ROM binding prevents cross-ROM installation, but it does not certify code quality.
- Canonical metadata synchronization should receive instrumentation coverage proving that established user-edited titles are preserved.
- Sound, haptics, notifications, controller ergonomics, Bluetooth/headset routing, thermal behavior, and long sessions need physical-device testing.
- Google sign-in needs a real OAuth client ID; trusted cloud identity additionally requires backend token and nonce verification.
- Nintendo DS games, including HeartGold, SoulSilver, and Platinum, are not supported by the current GBA core.
