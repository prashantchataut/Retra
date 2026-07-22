# Retra 2.2.0 Build and Validation Report

Date: 2026-07-22

## Reported CI failure

The supplied CI log reached `:app:kaptReleaseKotlin` and failed while Room was exporting schema version 6. The underlying JSON exception reported end-of-file inside the `database` object. The committed schema was truncated rather than merely formatted incorrectly.

## Repair applied

- Replaced Room schema 6 with complete parseable JSON.
- Applied the Room Gradle plugin and a single schema directory.
- Added schema files to Android instrumentation-test assets.
- Added a Room 5→6 migration instrumentation test.
- Disabled Gradle parallel execution and uses `--no-parallel` in CI.
- Added schema parsing before and after Android compilation.
- Separated debug tests, migration-test compilation, debug APK assembly, release-variant compilation, and lint.
- Changed CI output to an installable debug APK for private testing; no unsigned build is published as a production release.

## Implemented 2.2 scope

- Rebuilt active Material 3 Archive Glass UI, onboarding, Profile, Settings, Discover, Library, and player.
- Portrait/landscape player with customizable layout, visual style, size, spacing, opacity, dead zone, scaling, save/load, screenshots, rewind, speed, audio, and cheats.
- Legal Homebrew Hub catalog with real screenshots and provenance-preserving install.
- Exact user-owned game recognition and patch workflows.
- Bundled, SHA-256-validated user-supplied Heart & Soul v1.2.1 UPS patch; no base ROM.
- Working local lifecycle achievements.
- Save Health, rotating-backup restore, and ROM-free portable backup import/export.
- Review-before-import for Android VIEW/SEND intents.
- About and Profile developer attribution: Prashant Chataut.

## Verification executed in this container

| Suite | Result |
|---|---|
| Platform-neutral core | PASS — 43 passed, 0 failed |
| Native reference runtime | PASS |
| mGBA/libretro mock adapter | PASS |
| Room schema parser | PASS — schemas 5 and 6 |
| Project/TOML/XML/static checks | PASS |
| Shell syntax | PASS |
| JNI bridge C++20 host compilation | PASS |
| mGBA/libretro adapter C++20 host compilation | PASS |

These suites are rerun after final packaging.

## Android Gradle boundary

The project requests Gradle 9.5.0. This container has no cached distribution and outbound DNS cannot resolve the distribution host, so Android/Compose/Hilt/Room compilation and APK installation could not be executed here.

A provisioned runner must execute the checked-in workflow or at minimum:

```bash
./scripts/fetch-mgba-archive.sh
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
./gradlew --no-daemon --no-parallel :app:testDebugUnitTest
./gradlew --no-daemon --no-parallel :app:compileDebugAndroidTestKotlin
./gradlew --no-daemon --no-parallel :app:assembleDebug
./gradlew --no-daemon --no-parallel :app:assembleRelease
./gradlew --no-daemon --no-parallel :app:lintDebug
```

Then install the debug APK and run 30-, 60-, and 120-minute smoke sessions on representative hardware.

## Release integrity

No reusable signing key or password is included. Production signing must be injected from private CI or a store. Generated mGBA binaries are not committed; CI builds them from the pinned source archive and refuses packaging if an ABI is missing. No commercial ROM, BIOS, piracy index, or scraped commercial artwork is included.
