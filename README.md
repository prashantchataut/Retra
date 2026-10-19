# Retra

An Android Game Boy Advance emulator built for playing Pokémon games, Pokémon fan games
(ROM-hack patches) and homebrew — with a soft "cute 2D under liquid glass" interface and an
original bundled game, **Mochi Hop**.

Everything here is written from scratch: the emulation core, the patcher, the app, the mascot
(Mochi) and the game.

## Repository layout

| Path | What it is |
|---|---|
| `app/` | Android app (Kotlin + Jetpack Compose, package `app.retra`) — see `app/DESIGN.md` |
| `engine/` | `:engine` Android library: pure C++17 GBA core, JNI bridge and the Kotlin API — see `engine/README.md` |
| `patcher/` | `:patcher` Kotlin/JVM library: IPS, UPS, BPS, VCDIFF/xdelta, ZIP — see `patcher/README.md` |
| `game/` | Mochi Hop, the original GBA game (C) and its asset/cover tools — see `game/README.md` |
| `catalog/` | The online Game Library manifest and its format — see `catalog/README.md` |
| `docs/` | `REBUILD_PLAN.md` (the architecture contract) and screenshots |

## Content rules

These are hard rules, not preferences:

- **No commercial ROMs, ROM sites, pre-patched ROMs or Nintendo BIOS.** Ever.
- Pokémon fan games ship as **patches** (IPS/UPS/BPS) that you apply to a base ROM you dumped
  yourself; the app checks the base ROM's CRC32 first.
- Direct downloads are only offered for content whose licence permits redistribution
  (homebrew, and our own game).

## Building

Requirements: JDK 21, Android SDK with `platforms;android-37.0`, `build-tools;37.0.0`,
`ndk;28.2.13676358` and `cmake;3.22.1`. Point Gradle at your SDK with `local.properties`
(`sdk.dir=...`) or `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug          # app APK (native engine for arm64-v8a, armeabi-v7a, x86_64)
./gradlew :patcher:test               # patcher tests
./gradlew :app:testDebugUnitTest      # app tests
```

The emulation core and the game also build on a plain Linux host (no Android SDK), which is how
the engine suite runs:

```sh
sh engine/host/run_tests.sh           # builds retra-headless and runs the emulator test suite
sh game/build.sh                      # rebuilds Mochi Hop from source
```

The host suite and the game build need `cmake`, `ninja`, `g++` and `arm-none-eabi-gcc`
(`apt-get install cmake ninja-build gcc-arm-none-eabi libnewlib-arm-none-eabi`).

## Release signing

Release APKs are signed with the debug key unless a keystore is supplied through the environment:
`RETRA_SIGNING_STORE_FILE`, `RETRA_SIGNING_STORE_PASSWORD`, `RETRA_SIGNING_KEY_ALIAS` and
`RETRA_SIGNING_KEY_PASSWORD`. CI materialises one from the `RETRA_SIGNING_KEYSTORE_B64`,
`RETRA_SIGNING_STORE_PASSWORD`, `RETRA_SIGNING_KEY_ALIAS` and `RETRA_SIGNING_KEY_PASSWORD`
repository secrets when they are set, and falls back to the debug key when they are not.

## Continuous integration

`.github/workflows/android.yml` runs two jobs: the engine host suite (plus a Mochi Hop rebuild
check), and the Android job (patcher and app unit tests, then debug and release APKs uploaded as
artifacts).

## Licence

Apache-2.0 — see `LICENSE`. Bundled third-party assets keep their own licences
(`app/src/main/assets/licenses/`, `catalog/README.md`).
