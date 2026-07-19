# Retra

**Relive the games that made you.**

Retra is a privacy-first Android Game Boy Advance emulator shell built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Credential Manager, WorkManager, JNI, CMake, and a pinned mGBA/libretro integration path.

## 0.5.0 milestone

This source release turns the earlier architecture into a substantially more complete emulator product:

- multi-step adaptive onboarding with offline-first setup and optional Google identity;
- original Retra Prism launcher icon, monochrome icon, splash treatment, in-app logo, and wordmark sources;
- gameplay-capable mGBA 0.10.5 libretro frontend selected when reviewed Android ABI libraries are bundled;
- `SurfaceView` video, integer scaling, optional smoothing, immersive system bars, audio focus, volume, mute, and headphone-disconnect pause;
- touch, keyboard, Bluetooth, USB, D-pad hat, analog fallback, disconnect clearing, and a live controller tester;
- fast-forward, slow motion, five save-state slots, battery saves, checksummed suspend states, bounded rewind, screenshots, reset, and session menu;
- secure local ROM imports, legal HTTPS catalogs, exact hashes, patches, custom/internet cheat packs, local achievements, social profile, and multiplayer transport foundations;
- library search, favorites, editable title/notes, custom cover art, recent and favorite shelves;
- highly adjustable theme, palette, glass, density, typography, library layout, controls, display, audio, startup, privacy, and home behavior.

## Honest build status

| Area | Status |
|---|---|
| Pure Kotlin content/security/emulation logic | Implemented; 36 host checks pass |
| Native diagnostic JNI pipeline | Implemented; host verification passes |
| mGBA/libretro frontend ABI | Implemented; host mock-core verification passes |
| Android mGBA shared libraries | Build/staging scripts ready; binaries are not bundled in this archive |
| Android APK/AAB | Not produced in this sandbox because Gradle, Android SDK, Android NDK, and ADB are unavailable |
| Google Credential Manager UI | Implemented in source; requires a real Web OAuth client ID and device testing |
| Google-backed Retra account | Requires a production backend to verify ID tokens/nonces and issue sessions |
| LAN transport architecture | Host-tested; real GBA link gameplay still requires core link callbacks and synchronization |

Retra never labels the diagnostic renderer as GBA emulation. When `libmgba_libretro.so` is absent or incomplete, the app explicitly falls back to native pipeline diagnostics.

## Build prerequisites

- JDK 21
- Android Studio / Android SDK matching `compileSdk 37`
- Android NDK and CMake
- network access to Google Maven and Maven Central
- optional Google OAuth Web client ID
- reviewed mGBA 0.10.5 source

### Google identity

```bash
./gradlew :app:assembleDebug \
  -PRETRA_GOOGLE_WEB_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

See `docs/GOOGLE_SIGN_IN_SETUP.md`.

### Gameplay core

```bash
./scripts/fetch-mgba-archive.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew :app:assembleDebug
```

Equivalent Gradle tasks are available:

```bash
./gradlew :emulation:native:fetchMgbaSource
./gradlew :emulation:native:buildMgbaCore
```

See `docs/ROM_PLAYBACK_SETUP.md` and `docs/MGBA_INTEGRATION_PLAN.md`.

## Verification available in this archive

```bash
./tools/core-verification/run.sh
./tools/native-verification/run.sh
./tools/libretro-verification/run.sh
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

The host suites validate parsers, secure downloads, catalog and cheat rules, patch formats, save envelopes, atomic storage, rewind memory bounds, achievements, social/multiplayer logic, JNI state handling, libretro ROM/frame/audio/input/state/battery/cheat behavior, XML/TOML, Room migrations, branding, and project wiring.

## Content policy

Retra does not include commercial ROMs, proprietary BIOS files, pre-patched copyrighted games, piracy indexes, executable cheat scripts, or runtime-downloaded native code. Legal catalogs require HTTPS, creator/license/distribution details, exact size and SHA-256, GBA validation, bounded redirects, and private-network blocking. Users import personal backups or permitted homebrew.

## Key documents

- `BUILD_REPORT.md`
- `IMPLEMENTATION_STATUS.md`
- `PROJECT_STATE.md`
- `NEXT_ACTIONS.md`
- `KNOWN_ISSUES.md`
- `TEST_RESULTS.md`
- `THREAT_MODEL.md`
- `docs/ROM_PLAYBACK_SETUP.md`
- `docs/GOOGLE_SIGN_IN_SETUP.md`
- `docs/BRAND_IDENTITY.md`
- `docs/UI_UX_AUDIT_0.5.md`
- `design-system/retra/MASTER.md`
