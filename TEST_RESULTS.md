# Test Results

Date: 2026-07-22 UTC  
Version: 2.0.0

## Platform-neutral suite

`./tools/core-verification/run.sh`

**43 passed, 0 failed.** New coverage includes SHA-1, exact Libretro DAT matching, RetroArch `.cht` conversion, ROM binding, and skipping unsupported placeholder cheats while retaining concrete definitions.

## Native reference engine

`./tools/native-verification/run.sh`

**PASS:** load, frame, input marker, serialize/restore, reset, and corruption rejection.

## libretro frontend

`./tools/libretro-verification/run.sh`

**PASS:** ABI load, in-memory ROM, video, input, audio, state, battery save, and cheat activation against the host mock core.

## Project/static suite

`SKIP_EXECUTION_SUITES=1 ./tools/project-verification/run.sh`

**PASS:** module graph, TOML, Android XML, Material icon imports, public Compose API guard, Room migrations through schema 6, Homebrew Hub, Libretro metadata and cheats, four-stage onboarding, developer/About information, signing-secret guard, shell syntax, and JNI/libretro C++20 `-Werror` host compilation.

## Android build attempt

`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon`

**NOT RUN TO COMPILATION:** the wrapper attempted to download Gradle 9.5.0 but failed with `java.net.UnknownHostException: services.gradle.org`.

No APK, Android unit-test, instrumentation-test, lint, Room schema export, install, or device result is claimed.
