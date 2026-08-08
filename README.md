# Retra

**Relive the games that made you.**

Retra 3.0 is a privacy-first Android Game Boy Advance emulator, patch workspace, save vault, and personal game archive. The app is built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, JNI/CMake, and a pinned mGBA/libretro build path. Developer: **Prashant Chataut**.

## Retra 3.0 redesign

The visible product has been rebuilt rather than reskinned:

- A new responsive shell uses bottom navigation on phones and a navigation rail on wider layouts.
- Archive Glass now has readable content colors, controlled translucency, opaque accessibility fallbacks, high-contrast support, and quieter ambient light.
- Home prioritizes continuing a game, then recent/favorite worlds, saves, and trust information.
- Library is artwork-first with search, grid/list layouts, and filters for continue, favorites, patches, homebrew, and unplayed games.
- Discover separates original homebrew, creator-owned releases, external creator pages, and patch-only projects.
- Profile and Settings are first-class destinations instead of secondary utility pages.
- Game details now expose source provenance, core readiness, technical identity, artwork, cheats, favorites, and safe deletion.
- The patch flow explains why a `.ups`, `.ips`, or `.bps` file is not a game and identifies compatible local base files.
- The launcher and in-app brand use the original **Vault Aperture / Memory Prism** mark, not a letter logo or borrowed console imagery.

## A real built-in game

A fresh install imports **Retra Drift**, an original 64 KiB GBA homebrew mini-game, through the same content-addressed repository path as any other game. It is included so the library and player can be exercised without packaging a commercial ROM.

Source and deterministic build files are in `tools/demo-rom/`. The generated ROM is at `app/src/main/assets/demo/retra_drift.gba`.

## Why the supplied Heart & Soul file did not launch

`pokemonHnS_v1.2.1.ups` is a UPS patch, not a standalone ROM. Its container is valid and expects an exact 16 MiB base with CRC-32 `1F1C08FB`; after patching, it produces a 32 MiB image with CRC-32 `96A8425B`.

Retra 3.0 keeps the patch in Patch Studio, shows the expected size/checksum, offers **Import base game**, refreshes compatible-base matches after import, and only enables local patch application when the selected base matches.

Retra does not include the required commercial base game. The user must provide a compatible backup they are entitled to use.

## Emulation core boundary

The Android source tree intentionally does not commit generated `libmgba_libretro.so` binaries. A gameplay-capable APK must stage mGBA for all release ABIs before Gradle packaging. The included GitHub Actions workflow fetches pinned mGBA source, builds `arm64-v8a`, `armeabi-v7a`, and `x86_64` cores, rejects missing binaries, and then builds the app.

A locally assembled APK that skips those steps can import and index games but cannot provide real GBA gameplay; the UI now reports that state instead of pretending the fallback is a playable core.

## Product systems retained

- Content-addressed imports with SHA-256, SHA-1, CRC-32, game code, revision, metadata, artwork, tags, collections, favorites, provenance, and compatibility notes.
- `.gba`, `.zip`, `.ups`, `.ips`, and `.bps` import; explicit rejection of Nintendo DS `.nds` files.
- Local saves, automatic backups, Save Timeline, Save Health, screenshots, rewind, verified cheats, and ROM-free backup bundles.
- Per-game launch profiles and controls; touch, keyboard/gamepad, analog/dead-zone calibration, and Controller Studio.
- Local achievements based on verifiable Retra events rather than invented in-game telemetry.
- Creator-first Homebrew Hub with validation rules for any direct download.

## Build

Prerequisites:

- JDK 17
- Android SDK/API 37 and build-tools 37.0.0
- Android NDK 28.2.13676358
- CMake 3.22.1 and Ninja
- Network access to Google Maven, Maven Central, the Gradle distribution, and the pinned mGBA source archive

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

## Verification

```bash
./tools/project-verification/run.sh
```

That umbrella suite runs the platform-neutral core tests, native reference runtime checks, libretro adapter fake-core tests, Room/static/resource policy checks, shell syntax checks, and host C++ compilation.

The supplied redesign environment had no provisioned Android SDK/NDK and could not complete a Gradle/Compose APK build. See `BUILD_REPORT.md` for the exact evidence and remaining device gates.

## Content boundary

Retra includes no commercial ROMs, proprietary BIOS files, piracy indexes, scraped commercial artwork, reusable signing credentials, or pre-patched copyrighted games. Commercial games remain user-supplied. Patch projects remain patch-only. Direct homebrew downloads are allowed only when the source, permission, size, HTTPS transport, and exact checksum satisfy Retra's validation policy.

## Important documents

- `PRODUCT.md` / `DESIGN.md`
- `docs/UI_MAP.md`
- `docs/BRAND_IDENTITY.md`
- `KNOWN_ISSUES.md`
- `THREAT_MODEL.md`
- `CHANGELOG.md`
