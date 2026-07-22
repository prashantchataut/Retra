# Retra 2.3.0 Build and Validation Report

Date: 2026-07-22

## Reported failure

The latest workflow stopped in Retra's source verification with:

```text
A reusable app signing key or password must not be committed
```

This was a false positive in the project verifier. The previous rule rejected signing-related Gradle source rather than distinguishing committed secrets from secure environment/provider configuration.

## Repair

- Added a dedicated signing source-policy verifier.
- Scan committed files with `git ls-files` in CI.
- Reject committed keystores/private keys, committed signing-property passwords, and literal Gradle passwords.
- Allow Provider/environment-backed `storePassword`, `keyPassword`, aliases, and keystore paths.
- Configure release signing only when all four `RETRA_SIGNING_*` values are present and nonblank.
- Keep debug signing on Android's standard debug key.
- Compile the release variant unsigned when secrets are absent.
- Added optional GitHub Secrets materialization into `$RUNNER_TEMP`; the keystore is never written into the checkout.
- Added Gradle wrapper validation and changed Android build steps to `./gradlew`.

## Implemented 2.3 scope

- Controller Studio with real key, D-pad, joystick, hat, and trigger input.
- Per-device and optional per-game profiles with remapping and calibration.
- Independent input-source merging to prevent cross-source release bugs.
- Save Timeline with named/automatic immutable checkpoints and reversible restore.
- Measured performance evidence and per-game recommendations.
- Per-game launch profiles.
- Local compatibility notebook.
- Corrected fast-forward frame pacing and GBA-target runtime metrics.
- Legacy Boosted/Extreme profiles are normalized to Balanced rather than exposed as gimmick settings.
- Responsive chip rows in controller and game-profile tools.
- About/version attribution remains **Developer — Prashant Chataut**.

## Verification executed in this container

| Suite | Result |
|---|---|
| Exact failing source-verification command | PASS |
| Signing source policy | PASS |
| Platform-neutral core | PASS — 43 passed, 0 failed |
| Native reference runtime | PASS |
| mGBA/libretro mock adapter | PASS |
| Room schema parser | PASS — schemas 5 and 6 |
| Project/TOML/XML/static checks | PASS |
| Shell syntax | PASS |
| JNI bridge C++20 host compilation | PASS |
| mGBA/libretro adapter C++20 host compilation | PASS |

## Android Gradle boundary

`./gradlew --version` attempted to obtain Gradle 9.5.0, but this container cannot resolve `services.gradle.org`. Therefore Android/Compose/Hilt/Room compilation and APK installation could not be executed here.

A provisioned runner must execute:

```bash
./scripts/fetch-mgba-archive.sh
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew --no-daemon --no-parallel :app:testDebugUnitTest
./gradlew --no-daemon --no-parallel :app:compileDebugAndroidTestKotlin
./gradlew --no-daemon --no-parallel :app:assembleDebug
./gradlew --no-daemon --no-parallel :app:assembleRelease
./gradlew --no-daemon --no-parallel :app:lintDebug
```

## Release integrity

No reusable signing key or password is included. Production signing is injected from a private local path, private CI, or an app store. Generated mGBA binaries are not committed; CI builds them from pinned source and rejects missing ABIs. No commercial ROM, BIOS, piracy index, or scraped commercial artwork is included.
