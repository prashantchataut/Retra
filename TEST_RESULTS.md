# Test Results

Session date: 2026-07-19
Version: 0.4.0

## Platform-neutral core suite

Command:

```bash
./tools/core-verification/run.sh
```

Result: **35 passed, 0 failed.**

Coverage includes GBA parsing, SHA-256, duplicates, catalog validation, input/session state, save envelope/atomic backup/path safety, IPS/UPS/BPS patching and corruption, declarative cheats/matching/conflicts/executable rejection, HTTPS catalog policy and restricted JSON, local/private target and unsafe ID rejection, achievement counters/unique progress/integrity, social friend codes/privacy-safe sharing, multiplayer exact compatibility, packet CRC/order, and LAN loopback framing.

## Native reference engine

Command:

```bash
./tools/native-verification/run.sh
```

Result: **PASS.**

Verified load, frame generation, input marker, state creation, restore, reset, and corrupted-state rejection.

## Libretro gameplay adapter

Command:

```bash
./tools/libretro-verification/run.sh
```

Result: **PASS.**

Verified ABI symbol loading, in-memory ROM loading, frame conversion, GBA input, PCM audio, state serialization, battery-save memory, and cheat reset/set through a deterministic fake libretro core.

## Project/static verification

Command:

```bash
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

Result: **PASS.**

Verified thirteen modules and build files; TOML/XML parsing; Material icon imports; non-destructive Room migrations; patch, codes, catalog, achievements, social, multiplayer, requested skill snapshots, DI and emulation contracts; shell syntax; and host C++20 compilation of JNI and libretro adapter with `-Werror`.

## Combined script note

The non-skipping project script reruns all compilation suites and exceeded the command runner’s time limit after the 35 core checks. This is a runner-duration limitation, not a failed assertion. Its constituent suites and static phase were executed separately and passed.

## Not executed

- Android Gradle build, lint, JVM Android tests, or instrumentation tests.
- APK install/launch.
- Real mGBA Android library loading.
- Device rendering, audio, controllers, haptics, lifecycle, Room, SAF, or network downloads.
- Real GBA link multiplayer, OAuth, cloud, or internet relay.
