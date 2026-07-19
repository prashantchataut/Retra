# Retra

**Relive the games that made you.**

Retra is a privacy-first Android Game Boy Advance emulator built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Credential Manager, WorkManager, JNI, CMake, and a pinned mGBA/libretro integration path.

## 0.7.0 milestone — CI release automation

Retra 0.7.0 rebuilds the product shell and hardens the import/Discover pipelines while moving release packaging into GitHub Actions.

### Product
- Four permanent destinations: Home, Library, Discover, Settings
- Managed multi-format imports (`.gba` / `.zip` / `.ups` / `.ips` / `.bps`), guided CRC patch apply, Room schema 5 migration
- Discover source groups: Retra Curated links, official creator releases, SHA-256-pinned custom manifests
- Graphite/off-white surfaces with a single indigo accent and a forward-leaning white **R** mark

### CI / packaging
- CI provisions Java 17, Gradle 9.5.0, Android SDK platform 37, build-tools, NDK 28.2.13676358, and CMake 3.22.1.
- CI fetches the pinned, SHA-256-verified mGBA 0.10.5 DFSG source and builds the libretro core for `arm64-v8a`, `armeabi-v7a`, and `x86_64` before assembling the APK.
- CI runs the unit tests and release assembly, attempts Android lint and the host verification suites best-effort, then packages the APK with a SHA-256 sum, uploads it as a workflow artifact, and publishes a GitHub release.
- Per-ABI `ANDROID_BINARY_HASHES.txt` is attached to the release when the native core is staged.

The APK is debug-signed for personal / open-source sideload testing and is not built for the Play Store. Local checkouts without the Android SDK, NDK, and CMake cannot reproduce the APK; use the CI workflow (or a fully provisioned workstation) for that.

## 0.6.0 milestone — Prism Glass

This release repairs the reported Compose compilation failure and rebuilds the interaction layer around a minimal, premium, accessible glass system.

### Build repair

The failed release build imported Compose's internal `RowColumnParentData.weight` symbol from six screens. Retra now uses the public `RowScope`/`ColumnScope` `Modifier.weight` API without importing the internal implementation. The project verifier rejects that import if it is reintroduced.

### Refined experience

- one coherent Prism Glass component system for onboarding, navigation, home, library, Discover, Vault, profile, community, settings, and player status;
- translucent surfaces with subtle edge highlights while text fields, dialogs, and dense content stay crisp;
- decorative background blur only, with opaque fallbacks for reduced-transparency mode and older Android versions;
- settings reorganized into focused Appearance, Library, Player, Feel, Alerts, Controls, Boost, and Privacy categories;
- less saturated dark surfaces, quieter typography, consistent spacing, semantic hierarchy, and 48dp-friendly controls;
- responsive phone/tablet navigation retained.

### Feel, sound, and notifications

- semantic haptic cues for taps, game buttons, confirmation, saves, achievements, invitations, and errors;
- six original short Retra sound cues loaded through `SoundPool`;
- independent haptic, UI-sound, and UI-sound-volume settings;
- Android notification channels for achievements, verified downloads, multiplayer, and save protection;
- contextual Android 13+ notification permission request instead of prompting on first launch;
- channel and app notification settings remain user-controlled;
- background suspend-state notification when enabled.

### Emulator and library foundation retained

- mGBA 0.10.5 libretro frontend, memory-only ROM delivery, frame/audio/input/state/battery/cheat plumbing;
- touch, keyboard, Bluetooth and USB controllers;
- five state slots, rewind, screenshots, fast-forward and slow motion;
- ROM import, legal HTTPS catalogs, secure downloads, patching, Retra Codes, achievements, social profile, and multiplayer foundations;
- library search, favorites, notes, titles, and custom cover art;
- optional Google identity that never gates offline play.

## Honest build status

| Area | Status |
|---|---|
| Reported `weight` compilation failure | Source fix complete; exact forbidden import is statically gated |
| Platform-neutral logic | 36 checks pass |
| Native diagnostic JNI pipeline | Host verification passes |
| mGBA/libretro frontend | Host mock-core verification passes |
| Project structure / UI / notifications / feedback | Static verifier passes |
| Full Android Gradle build | Runs in GitHub Actions CI (Java 17, Gradle 9.5.0, SDK 37); a local checkout without the Android SDK cannot reproduce it |
| Android mGBA ABI libraries | Built from pinned source in CI and staged into the APK; reproducible scripts included, binaries never committed |
| Device UX, audio, haptics, and notification testing | Required before production release |

A debug-signed APK is produced by CI for sideload testing. No Play Store-signed APK or AAB is claimed, and this local sandbox still has no Gradle or Android SDK.

## Build

Prerequisites:

- JDK 21
- Android SDK matching `compileSdk 37`
- Android NDK and CMake
- Gradle 9.5 or compatible wrapper runtime
- access to Google Maven and Maven Central
- reviewed mGBA 0.10.5 source/core binaries

```bash
./scripts/fetch-mgba-archive.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
gradle --no-daemon :app:assembleRelease
```

Optional Google identity:

```bash
gradle --no-daemon :app:assembleRelease \
  -PRETRA_GOOGLE_WEB_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

## Verification

```bash
./tools/core-verification/run.sh
./tools/native-verification/run.sh
./tools/libretro-verification/run.sh
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

## Content policy

Retra includes no commercial ROMs, proprietary BIOS files, piracy indexes, pre-patched copyrighted games, executable cheat scripts, credentials, or runtime-downloaded native code. Remote content must be explicit, authorized, bounded, and hash-verified.

## Key documents

- `BUILD_REPORT.md`
- `TEST_RESULTS.md`
- `IMPLEMENTATION_STATUS.md`
- `PROJECT_STATE.md`
- `KNOWN_ISSUES.md`
- `NEXT_ACTIONS.md`
- `docs/BUILD_FAILURE_0.5_ANALYSIS.md`
- `docs/UI_UX_AUDIT_0.6.md`
- `docs/FEEDBACK_AND_NOTIFICATIONS.md`
- `docs/ROM_PLAYBACK_SETUP.md`
- `docs/GOOGLE_SIGN_IN_SETUP.md`
