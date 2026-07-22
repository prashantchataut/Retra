# Retra 2.0.0 Build and Validation Report

Date: 2026-07-22

## Scope

- Real Homebrew Hub discovery and eligible homebrew install flow.
- Exact Libretro metadata matching and canonical identity.
- Libretro/RetroArch cheat lookup, conversion, placeholder filtering, and exact-ROM binding.
- Room schema 6 with SHA-1, canonical title, and metadata source.
- Serialized and failure-safe ROM imports.
- Four-stage onboarding, separate Profile and Settings, and About information for developer Prashant Chataut.
- Removal of committed signing key and credentials.

## Verification

| Suite | Result |
|---|---|
| Platform-neutral core | PASS — 43 passed, 0 failed |
| Native reference runtime | PASS |
| mGBA/libretro mock-core runtime | PASS |
| Project/TOML/XML/static checks | PASS |
| Shell syntax | PASS |
| JNI bridge C++20 `-Werror` host compilation | PASS |
| mGBA/libretro adapter C++20 `-Werror` host compilation | PASS |

## Android Gradle build

The command below was attempted:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

The wrapper could not obtain Gradle 9.5.0 because DNS failed for `services.gradle.org` with `java.net.UnknownHostException`. Therefore Android/Compose/Hilt/Room compilation did not begin.

A provisioned CI machine must run:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

It must also export Room schema 6, run migration tests, stage reviewed mGBA libraries for every supported ABI, install the APK, and execute real-device smoke tests.

## Release integrity

No signing key or password is included. Release signing must be injected from private CI or the distribution store. No commercial ROM, BIOS, piracy index, runtime-downloaded native code, or copyrighted pre-patched game is included.
