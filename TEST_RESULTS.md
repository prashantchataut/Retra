# Test Results

Date: 2026-07-19 UTC  
Version: 0.5.0

## Platform-neutral suite

Command:

```bash
./tools/core-verification/run.sh
```

Result: **36 passed, 0 failed**.

Coverage includes GBA parsing, SHA-256, duplicate detection, catalog security, strict JSON, network policy, atomic storage, save envelopes, path traversal, bounded rewind, patching, cheat parsing/conflicts, achievements, social privacy, multiplayer compatibility/framing, and loopback transport.

## Native reference engine

Command:

```bash
./tools/native-verification/run.sh
```

Result: **PASS** for load, frame generation, input, state serialize/restore, reset, and corruption rejection.

## libretro frontend

Command:

```bash
./tools/libretro-verification/run.sh
```

Result: **PASS** for ABI loading, ROM load, video, input, audio, save states, battery memory, and cheat activation against the host mock core.

## Static project suite

Command:

```bash
SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh
```

Result: **PASS** for module graph, Kotlin source expectations, Android XML, icons, Room migrations, DI, branding, onboarding/account files, secure catalogs, patches, cheats, achievements, social, multiplayer, rewind, screenshots, artwork, mGBA scripts, shell syntax, and C++20 `-Werror` compilation.

## Not run

- Gradle/AGP compile
- Android unit/instrumentation/UI tests
- lint and R8
- APK install/launch
- physical-device, emulator, accessibility, performance, thermal, battery, and network testing
