# Retra

**Relive the games that made you.**

Retra 2.2 is a privacy-first Android Game Boy Advance emulator and personal game archive built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, JNI/CMake, and a pinned mGBA/libretro build path. Developer: **Prashant Chataut**.

## What 2.2 focuses on

Retra is organized around working systems rather than showcase screens:

- **Archive:** checksum-addressed GBA imports, exact metadata, artwork, favorites, tags, collections, patch lineage, and provenance.
- **Player:** real emulator surface, portrait/landscape layouts, scaling, save/load, autosave, rewind, screenshots, speed controls, audio, cheats, and customizable touch controls.
- **Vault:** save-health scanning, rotating backup recovery, local achievements, and ROM-free portable backup bundles.
- **Discover:** legal GBA homebrew with real screenshots, plus owned-game and patch guides.

## UI and player

- Material 3 **Archive Glass** design using mineral black, navy, graphite, ice, aqua, coral, and mint—without purple/rainbow gradient chrome.
- Rebuilt onboarding, Home, Library, Discover, Profile, Settings, game details, patch review, external-import review, achievements, and player screens.
- Separate Profile and Settings pages. About identifies **Prashant Chataut** as developer.
- Adaptive bottom navigation/rail and two-pane Settings.
- Portrait and landscape player layouts.
- Classic, compact, left-handed, and controller-first control presets.
- Glass, solid, and minimal control styles.
- Adjustable scale, spacing, opacity, dead zone, shoulder buttons, and quick actions.
- Fit, fill, and integer scaling; immersive mode; high contrast; reduced motion/transparency; scalable typography.

## Real playable content

Retra does not use commercial ROM mirrors. Attribution does not grant redistribution rights.

The live playable catalog uses Homebrew Hub's documented GBA API. Eligible creator-published releases can be installed in one tap when they expose a playable `.gba` file and usable license metadata. Retra shows real provider screenshots, validates the downloaded game, computes its SHA-256, and retains creator, license, and source provenance.

Commercial Pokémon games such as FireRed and Emerald are supported through user-owned local backups. Retra identifies exact revisions using checksums, size, game code, and revision; then keeps saves, patches, cheats, and artwork bound to that exact game.

The user-supplied Pokémon Heart & Soul v1.2.1 UPS patch is included with creator credit, upstream reference, and SHA-256 validation. It contains no commercial base ROM and requires a compatible user-owned Pokémon Emerald backup.

## Achievements

Retra includes working local achievements for verifiable app events such as:

- importing games;
- creating saves;
- applying a patch;
- taking screenshots;
- using rewind;
- completing sessions and accumulating playtime;
- exporting a portable backup.

Retra does not fabricate game-memory achievements. A future rcheevos integration must use exact game identity, audited memory definitions, and clear network/offline semantics.

## Build-failure repair

The reported CI failure was caused by a truncated Room schema 6 JSON file. Retra 2.2 replaces it with a complete schema, applies the Room Gradle plugin, adds a 5→6 migration instrumentation test, validates every committed schema, disables parallel schema generation, and splits CI into explicit verification/build stages.

CI now:

1. validates structure and Room schemas;
2. runs native and libretro host tests;
3. builds pinned mGBA cores for arm64-v8a, armeabi-v7a, and x86_64;
4. runs unit tests and compiles migration tests;
5. assembles an installable debug APK for private testing;
6. compiles the release variant;
7. runs Android lint;
8. uploads the APK and SHA-256 without publishing an unsigned production release.

## Build

Prerequisites:

- JDK 17;
- Gradle 9.5.0;
- Android SDK 37.0 and build-tools 37.0.0;
- Android NDK 28.2.13676358;
- CMake 3.22.1 and Ninja;
- Google Maven and Maven Central access.

```bash
./scripts/fetch-mgba-archive.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew --no-daemon --no-parallel :app:testDebugUnitTest
./gradlew --no-daemon --no-parallel :app:assembleDebug
```

Optional Google identity:

```bash
./gradlew --no-daemon --no-parallel :app:assembleDebug \
  -PRETRA_GOOGLE_WEB_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

## Verification

```bash
./tools/core-verification/run.sh
./tools/native-verification/run.sh
./tools/libretro-verification/run.sh
./tools/schema-verification/run.sh
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

The current container could not download the Gradle distribution, so no APK is claimed here. See `BUILD_REPORT.md` and `docs/V2_2_BUILD_FAILURE_AND_FIX.md`.

## Content boundary

Retra includes no commercial ROMs, proprietary BIOS files, piracy indexes, scraped commercial cover art, runtime-downloaded native cores, reusable signing credentials, or pre-patched copyrighted games. Patches require a compatible user-supplied base ROM. Portable backups exclude ROM bytes.

## Key documents

- `docs/V2_2_PRODUCT_REVIEW.md`
- `docs/V2_2_BUILD_FAILURE_AND_FIX.md`
- `docs/V2_2_CONTENT_AND_ARTWORK_POLICY.md`
- `docs/V2_2_ROADMAP_STATUS.md`
- `docs/V2_3_FEATURE_RECOMMENDATIONS.md`
- `docs/RETRA_FINAL_UI_UX_SPEC.md`
- `docs/RETRA_CHEAT_INDEX.md`
- `docs/CONTENT_AND_CHEATS_POLICY.md`
- `docs/ROM_PLAYBACK_SETUP.md`
- `THREAT_MODEL.md`
- `BUILD_REPORT.md`
