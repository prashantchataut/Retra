# Retra

**Relive the games that made you.**

Retra is a privacy-first Android Game Boy Advance emulator built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Credential Manager, WorkManager, JNI, CMake, and a pinned mGBA/libretro integration path.

## 2.0.0 — provenance-first app foundation

Retra 2.0.0 combines the Archive Glass redesign with real content, metadata, cheat, and import logic. The product is a lawful personal GBA archive—not a commercial-ROM storefront. Developer: **Prashant Chataut**.

### Product and UI

- New **Portal / Save Core** identity based on the supplied brand mark, applied to Compose, launcher, monochrome, and splash assets.
- New Material 3 **Archive Glass** system: mineral black/navy foundations, ice/aqua/coral/mint accents, no purple or rainbow-gradient chrome.
- Decorative blur is limited to static ambient shapes on Android 12+; reduced-transparency mode is fully opaque.
- Four primary destinations: Home, Library, Discover, and You. Settings is a contextual top-bar/rail destination rather than a fifth tab.
- Rebuilt onboarding, Home, Library, profile, settings, game details, and cheat surfaces.
- Adaptive bottom navigation, rail navigation, two-pane settings, scalable typography, high contrast, reduced motion, and 48 dp targets.
- System typography is used intentionally; no font binaries are bundled.

### Library and legal discovery

- User-owned `.gba` / `.zip` imports and guided `.ips` / `.ups` / `.bps` patch workflows remain local.
- Discover prioritizes official creator/project pages.
- Homebrew Hub provides live search and eligible one-tap GBA installs from its documented HTTPS API. Retra validates the GBA binary and records its local SHA-256; Homebrew Hub downloads are not described as checksum-pinned unless the provider publishes an expected digest.
- Custom/curated one-tap catalogs remain stricter: HTTPS, explicit permission, bounded size, and a published SHA-256 are required.
- Official Heart & Soul patch releases and the Radical Red patcher are represented as patch/project links, not ROM downloads.
- Minicraft GBA and Butano remain legal/open-source discovery examples; downloads are enabled only when source metadata includes a verifiable digest.
- Retra does not bundle or index Poke Harbor, commercial Pokémon ROMs, proprietary BIOS files, or pre-patched copyrighted games.

### Exact-ROM metadata and one-tap cheats

- Libretro No-Intro DAT synchronization identifies imports by exact SHA-1 or CRC-32 plus size, never by filename.
- Libretro/RetroArch `.cht` files can be found or imported, converted to Retra Codes, and bound to the selected ROM SHA-256. Unsupported placeholder definitions are skipped without discarding concrete codes.
- New `.rci` trusted cheat-index format.
- Each pack entry requires HTTPS, a SHA-256 checksum, license, distribution permission, and exact ROM identity.
- Compatibility can additionally bind to four-character game code and revision.
- Imported packs still pass the existing strict Retra Codes parser, conflict checks, risk labels, and protected pre-cheat save-state flow.
- Local `.rcc` import and custom pack creation remain available.

### Existing emulator foundation retained

- mGBA 0.10.5 libretro frontend with memory-only ROM delivery.
- Frame, audio, input, save-state, battery, rewind, screenshot, fast-forward/slow-motion, and cheat plumbing.
- Touch, keyboard, Bluetooth, and USB controller input.
- Content-addressed managed imports, Room migrations, custom cover art, favorites, notes, tags, and collections.
- Optional Google identity that never gates offline play.

## Build status

The current sandbox could run the platform-neutral, native, libretro, and static project verification suites. It could not run an Android Gradle build because the wrapper distribution was not cached and outbound DNS could not resolve `services.gradle.org`. See `BUILD_REPORT.md` for exact evidence and limitations.

## Build

Prerequisites:

- JDK 17 or newer compatible with the selected Android Gradle Plugin
- Android SDK matching `compileSdk 37`
- Android NDK and CMake
- Gradle wrapper distribution/network access or a compatible local Gradle installation
- Google Maven and Maven Central access
- reviewed mGBA 0.10.5 source/core binaries

```bash
./scripts/fetch-mgba-archive.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew --no-daemon :app:assembleDebug
```

Optional Google identity:

```bash
./gradlew --no-daemon :app:assembleDebug \
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

Retra includes no commercial ROMs, proprietary BIOS files, piracy indexes, pre-patched copyrighted games, executable cheat scripts, signing credentials, or runtime-downloaded native code. Remote content must be explicit, authorized, and bounded. Retra records local hashes and requires an expected digest whenever the provider publishes one. Patches require a user-supplied compatible base ROM.

## Key documents

- `docs/RETRA_FINAL_UI_UX_SPEC.md`
- `docs/BRAND_IDENTITY.md`
- `docs/RETRA_CHEAT_INDEX.md`
- `docs/CRITICAL_REDESIGN_REVIEW.md`
- `docs/V2_PRODUCT_REVIEW.md`
- `docs/V2_ROADMAP.md`
- `docs/CONTENT_AND_CHEATS_POLICY.md`
- `BUILD_REPORT.md`
- `PROJECT_STATE.md`
- `THREAT_MODEL.md`
- `docs/ROM_PLAYBACK_SETUP.md`
- `docs/GOOGLE_SIGN_IN_SETUP.md`
