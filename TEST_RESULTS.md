# Test Results

Date: 2026-07-19 UTC  
Version: 0.6.0

## Platform-neutral suite

`./tools/core-verification/run.sh`

**36 passed, 0 failed.** Coverage includes GBA parsing, hashing, catalogs, secure network policy, save envelopes, atomic storage, rewind, patches, Retra Codes, achievements, social privacy, multiplayer compatibility, packet framing, and LAN loopback.

## Native reference engine

`./tools/native-verification/run.sh`

**PASS:** load, frame, input, serialize/restore, reset, and corruption rejection.

## libretro frontend

`./tools/libretro-verification/run.sh`

**PASS:** ABI load, in-memory ROM, video, input, audio, state, battery memory, and cheat activation against the host mock core.

## Project/static suite

`SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh`

**PASS:** module graph, TOML, Android XML, icon references, public Compose weight API guard, glass components, feedback engine, notification channels/permissions, six sound assets, Room migrations, DI, catalogs, patches, cheats, achievements, social, multiplayer, skill snapshots, shell syntax, JNI and libretro C++20 `-Werror` compilation.

## Sound assets

All six cues were parsed as 16-bit mono PCM WAV at 22,050 Hz and are shorter than 0.5 seconds.

## Not run

- Android Gradle/AGP Kotlin compilation after the repair;
- Android unit, instrumentation, screenshot, and Compose UI tests;
- lint, R8, release signing, APK install/launch;
- physical haptic, notification, audio, accessibility, thermal, and performance testing.
