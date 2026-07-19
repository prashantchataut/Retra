# Retra 0.5.0 Build Report

## Implemented in source

- Adaptive onboarding, Retra Prism brand assets, launcher icon, themed icon, and splash configuration.
- Google Credential Manager button flow with cryptographic nonce, official Google ID credential parsing, local identity persistence, disconnect, and strict no-token-persistence behavior.
- Real mGBA/libretro Android frontend boundary with ROM hash re-verification, frames, audio, input, battery memory, states, cheats, speed control, and runtime metrics.
- Integer pixel scaling, optional smoothing, immersive player, audio volume/mute/focus, pause on noisy output, touch/gamepad/keyboard input, controller disconnect safety, and tester.
- Five save-state slots, bounded rewind, screenshots, suspend/resume, fast-forward, slow motion, and session controls.
- Searchable/customizable library with favorites, editable title/notes, and bounded app-private cover art.
- Existing legal catalogs, secure downloads, patches, Retra Codes, achievements, social profile, and multiplayer architecture retained.

## Android build command

Expected after installing the toolchain and staging mGBA:

```bash
./gradlew :app:assembleDebug
```

## Android build result

**Not executed.** The sandbox has Java and CMake but no Gradle command, Android SDK manager, Android SDK platforms, Android NDK, ADB, emulator, or cached Android dependencies. Outbound DNS is unavailable to normal build processes.

No APK or AAB is included and no device behavior is claimed as verified.

## Host verification

```text
core-verification:       36 passed, 0 failed
native-verification:     PASS
libretro-verification:   PASS
project-verification:    PASS
shell syntax:            PASS
JNI C++20 -Werror:       PASS
libretro C++20 -Werror:  PASS
```

## Device validation still required

- all target ABIs and Android 8–current;
- common save types, RTC, process death, low storage, damaged states, and ROM revisions;
- audio latency/focus/Bluetooth routes and headphone disconnect;
- SurfaceView scaling, foldables, tablets, cutouts, refresh rates, and thermal pressure;
- touch ergonomics, controller models, D-pad navigation, reconnect, keyboard mappings, and accessibility services;
- Credential Manager on devices with and without eligible Google accounts;
- screenshot MediaStore behavior and custom artwork memory use;
- rewind performance and state determinism;
- release signing, R8, Baseline Profiles, macrobenchmarks, and Play pre-launch testing.
