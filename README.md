# Retra

**Relive the games that made you.**

Retra 2.3 is a privacy-first Android Game Boy Advance emulator and personal game archive built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, JNI/CMake, and a pinned mGBA/libretro build path. Developer: **Prashant Chataut**.

## What changed in 2.3

Retra 2.3 converts the previous roadmap into working product systems rather than adding showcase cards:

- **Controller Studio:** real Android gamepad input, per-device and per-game mappings, live input testing, analog dead-zone and trigger calibration, remapping, and stable local profile files.
- **Save Timeline:** automatic and named immutable checkpoints, retention limits, cheat/core/hash metadata, reversible restore, and integration with Save Health.
- **Measured performance advisor:** local frame-time percentiles, FPS, speed, dropped-frame, audio-underrun, thermal, and battery evidence. Advice is withheld until at least two minutes of active measurements exist.
- **Per-game launch profiles:** game-specific scaling, display smoothing, control layout, touch-control visibility, fast-forward speed, and performance profile.
- **Compatibility notebook:** local per-game status and observations, separate from canonical game identity.
- **Player input hardening:** touch, touch-axis, hardware-key, and hardware-axis states are merged independently so releasing one source cannot cancel a button still held by another source.
- **Frame pacing correction:** fast-forward now changes the frame budget; performance metrics report against the GBA target rate instead of presenting a decorative speed value.

## Build-failure repair

The reported message:

```text
A reusable app signing key or password must not be committed
```

came from Retra's own source-policy script. It incorrectly rejected secure Gradle signing configuration merely because `storePassword` or signing-related code appeared in the build script.

Retra 2.3 replaces that check with `tools/signing-verification/run.sh`:

- only committed key material and literal passwords are rejected;
- Gradle Provider/environment-variable signing is explicitly allowed;
- local or CI keys live outside the repository;
- debug builds use Android's normal debug signing;
- release builds compile unsigned when no signing variables are present;
- release signing activates only when all four required variables are nonblank;
- CI can optionally reconstruct a keystore in `$RUNNER_TEMP` from repository secrets.

The exact previously failing verification command now passes:

```bash
set -euo pipefail
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

## Core product areas

### Library and content

- Content-addressed imports with SHA-256, SHA-1, CRC-32, game code, revision, and canonical metadata.
- Artwork, favorites, tags, collections, creator/license/source provenance, patch lineage, and compatibility notes.
- Legal creator-published Homebrew Hub releases with real provider imagery and bounded HTTPS installation.
- Commercial games are imported from user-owned backups. Retra does not bundle or download commercial Pokémon ROMs.
- The supplied Pokémon Heart & Soul UPS patch remains patch-only and requires a compatible user-owned base ROM.

### Player

- Portrait and landscape screen-first layouts.
- Fit, fill, and integer scaling.
- Classic, compact, left-handed, and controller-first touch layouts.
- Glass, solid, and minimal controls with adjustable scale, spacing, opacity, and dead zone.
- Save/load, automatic saves, screenshots, rewind, cheats, reset, speed control, audio, and immersive mode.
- Hardware buttons, D-pad, sticks, hats, and analog triggers.

### Progress and safety

- Rotating save backups and Save Health diagnostics.
- Named and automatic Save Timeline checkpoints.
- ROM-free portable backup bundles.
- Working local achievements for verifiable Retra events.
- Review-before-import for Android VIEW/SEND intents.

## Build

Prerequisites:

- JDK 17
- Gradle 9.5.0
- Android SDK 37.0 and build-tools 37.0.0
- Android NDK 28.2.13676358
- CMake 3.22.1 and Ninja
- Google Maven and Maven Central access

```bash
./scripts/fetch-mgba-archive.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew --no-daemon --no-parallel :app:testDebugUnitTest
./gradlew --no-daemon --no-parallel :app:compileDebugAndroidTestKotlin
./gradlew --no-daemon --no-parallel :app:assembleDebug
```

Optional release signing uses environment variables and a keystore outside the repository:

```bash
export RETRA_SIGNING_STORE_FILE="$HOME/.signing/retra-release.jks"
export RETRA_SIGNING_STORE_PASSWORD="..."
export RETRA_SIGNING_KEY_ALIAS="retra"
export RETRA_SIGNING_KEY_PASSWORD="..."
./gradlew --no-daemon --no-parallel :app:assembleRelease
```

Optional Google identity:

```bash
./gradlew --no-daemon --no-parallel :app:assembleDebug \
  -PRETRA_GOOGLE_WEB_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

## Verification

```bash
./tools/signing-verification/run.sh
./tools/core-verification/run.sh
./tools/native-verification/run.sh
./tools/libretro-verification/run.sh
./tools/schema-verification/run.sh
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

The current container cannot resolve `services.gradle.org`, so Android/Compose/Hilt/Room compilation and an APK are not claimed here. The checked-in GitHub Actions workflow runs the complete provisioned build.

## Content boundary

Retra includes no commercial ROMs, proprietary BIOS files, piracy indexes, scraped commercial cover art, runtime-downloaded native cores, reusable signing credentials, or pre-patched copyrighted games. Patches require a compatible user-supplied base ROM. Portable backups exclude ROM bytes.

## Key documents

- `docs/V2_3_IMPLEMENTATION.md`
- `docs/V2_3_BUILD_FAILURE_AND_FIX.md`
- `docs/V2_4_RECOMMENDATIONS.md`
- `docs/V2_2_CONTENT_AND_ARTWORK_POLICY.md`
- `docs/RETRA_FINAL_UI_UX_SPEC.md`
- `docs/RETRA_CHEAT_INDEX.md`
- `docs/CONTENT_AND_CHEATS_POLICY.md`
- `THREAT_MODEL.md`
- `BUILD_REPORT.md`
