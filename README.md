# Retra

**Relive the games that made you.**

Retra is a premium Android Game Boy Advance library and emulator platform built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, Android Storage Access Framework, Android NDK/CMake, JNI, and a replaceable libretro-compatible emulation boundary.

This `0.4.0` source milestone expands the previous player and secure-content foundation with verified in-session cheat delivery, custom internet catalogs and cheat packs, local achievements, private-first community profiles, extensive appearance/behavior settings, and a bounded multiplayer transport architecture.

## Implemented in source

### Library and legal internet content

- Secure local `.gba` file and folder import through Android SAF.
- Bounded GBA header parsing, checksum inspection, SHA-256 identity, duplicate detection, Room persistence, and provenance.
- Restricted JSON catalog import from local files or explicit HTTPS URLs.
- User-provided catalog URLs require an expected SHA-256 and are protected by size limits, same-host redirects, private/local-target blocking, strict parsing, and atomic persistence.
- Catalog game downloads enforce HTTPS, provenance fields, exact length and SHA-256, bounded streaming, GBA validation, duplicate handling, fsync, and atomic import.
- Commercial ROMs, proprietary BIOS files, unauthorized ROM directories, and invisible website scraping are intentionally excluded.

### Retra Codes

- Create custom declarative cheat codes for an exact ROM identity.
- Import local packs or download explicit internet packs using HTTPS plus an expected SHA-256.
- Strict bounded parser; no scripts, executables, or hidden network actions.
- Exact ROM/revision matching, dependency checks, declared conflicts, and raw-memory collision analysis.
- Host-verified libretro cheat reset/set delivery.
- Automatic battery flush and protected pre-cheat state before activation.
- Raw memory-write syntax remains blocked until the final width/endianness translation contract is reviewed.

### Player, saves, and patches

- Typed session lifecycle, video/PCM/input/metrics/save contracts, touch controls, optional haptics, and Android controller mapping.
- Deterministic JNI reference engine for native pipeline diagnostics.
- Dynamically loaded mGBA/libretro adapter for memory ROM loading, video, audio, GBA input, state serialization, battery-save memory, cheats, reset, and speed control.
- Gameplay is reported only when a real reviewed `libmgba_libretro.so` is loadable; otherwise Retra exposes a clearly labeled diagnostic pipeline.
- ROM/core-bound save envelopes with payload SHA-256, safe paths, fsync, atomic replacement, and rotating backups.
- Suspend snapshots, quick save/load, battery-save transfer, and Vault timeline.
- Local IPS, UPS, and BPS patching that preserves the base ROM and records provenance in a separate library entry.

### Achievements, profiles, and sharing

- Local achievement engine with counters, unique-item progress, completion ratios, and integrity gates.
- Persistent private-first Retra profile and deterministic friend code.
- Privacy-safe Android share-sheet cards for achievements and multiplayer invitations.
- Optional public social identity labels and HTTPS profile links stored locally.
- Provider OAuth is deliberately an adapter boundary until real application credentials and redirect URIs exist.

### Customization and UI

- Retra Prism adaptive phone/tablet experience with onboarding, Home, Library, Discover/Community, Vault, Settings, game details, and player surfaces.
- Theme mode, accent palette, dynamic color, glass intensity, corner scale, font scale, high contrast, reduced motion/transparency, and content density.
- Library layout, startup destination, statistics visibility, legal online recommendations, touch opacity, haptics, performance profile, fast-forward speed, and privacy controls.
- Built-in recommendations are actually filtered when disabled; imported user catalogs remain visible.
- Controller-first ergonomics, at least 48dp primary touch targets, semantic color tokens, and restrained glass effects.

### Multiplayer architecture

- Exact ROM/core/patch compatibility gate.
- LAN and internet-relay mode models, room codes, phases, invites, checksummed packets, sequence ordering, and bounded buffering.
- Trusted-LAN length-prefixed socket transport with local-address restrictions and loopback verification.
- Internet relay remains an interface boundary; no production relay is embedded.
- Real GBA link gameplay remains gated on link-capable emulator-core callbacks and synchronized timing. The UI does not claim otherwise.

## Verification completed in this session

```text
35/35 platform-neutral checks passed
PASS native reference engine: load/frame/input/state/restore/reset/corruption
PASS libretro adapter: ABI/ROM/frame/input/audio/state/battery/cheats
PASS project structure, TOML, XML, icons, migrations, security, modules, DI
PASS requested skill snapshots and shell syntax
PASS JNI and libretro adapter host compilation with C++20 -Werror
```

Run the suites independently:

```bash
./tools/core-verification/run.sh
./tools/native-verification/run.sh
./tools/libretro-verification/run.sh
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

The combined project script can exceed constrained command-runner limits because it recompiles all suites; every constituent suite and the static pass were run successfully.

## Android build status

No APK/AAB was produced in this sandbox. It lacks an Android SDK/NDK, Gradle executable/wrapper JAR, resolved Android dependencies, emulator/device, and usable outbound DNS. Consequently, Compose/Room/Hilt/SAF/AudioTrack/device networking and real Android mGBA packaging remain unverified here.

On a prepared Android workstation:

```bash
./scripts/bootstrap-wrapper.sh
./scripts/fetch-mgba.sh
export ANDROID_NDK_HOME=/path/to/android-ndk
./scripts/build-mgba-libretro-android.sh
./gradlew :app:assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

Before distribution, complete the device/save/security/legal/link-play gates listed in `NEXT_ACTIONS.md` and `KNOWN_ISSUES.md`.

## Modules

```text
:app                  Compose UI, Room, DataStore, SAF, player, downloads, Vault, DI
:core:model           Product, catalog, and customization models
:core:rom             GBA parsing, hashing, duplicate/catalog validation
:core:emulation       Session reducer, input, save envelopes, atomic storage
:core:patching        IPS/UPS/BPS parsers and safe application
:core:cheats          Retra Codes parser, matcher, policy, conflict analysis
:core:download        HTTPS/redirect/response/download policy
:core:catalog         Restricted JSON manifest parser
:core:achievements    Local rules, progress, integrity, built-in definitions
:core:social          Profiles, friend codes, privacy-safe share payloads
:core:multiplayer     Compatibility, protocol, packet ordering, LAN transport
:emulation:api        Replaceable core/frame/audio/metrics/save/cheat contract
:emulation:native     JNI reference engine and mGBA/libretro adapter
```

The exact status is maintained in `BUILD_REPORT.md`, `IMPLEMENTATION_STATUS.md`, `TEST_RESULTS.md`, `KNOWN_ISSUES.md`, and `PROJECT_STATE.md`.
