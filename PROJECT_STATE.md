# Project State

## Current milestone

**Retra 0.4.0 — community, verified internet imports, achievements, customization, and multiplayer architecture.**

The source now spans thirteen Gradle modules and reaches the gameplay-core boundary without misrepresenting a diagnostic renderer as a GBA emulator. The mGBA/libretro adapter is host-verified for ROM transfer, frames, input, PCM, state, battery memory, and cheats. Android selects gameplay only when a real reviewed shared library is loadable.

The user-facing platform now includes secure local and online libraries, custom/local/online Retra Codes, protected in-session cheat activation, Vault saves, IPS/UPS/BPS patching, local achievements, a private-first profile and share layer, broad customization, and bounded LAN multiplayer transport.

## Verification snapshot

- 35 platform-neutral checks passed.
- Native reference-engine suite passed.
- Libretro gameplay-adapter suite passed, including cheat reset/set.
- Static project verification passed for modules, XML, TOML, icons, Room migrations, secure-content capabilities, achievements, social, multiplayer, requested skills, DI, and emulation contracts.
- Shell syntax passed.
- JNI bridge and libretro adapter compiled on the host with C++20 warnings treated as errors.

## Environment blockers

This environment has no Android SDK/NDK, Gradle executable/wrapper JAR, resolved Android dependencies, Android emulator/device, or usable outbound DNS. No APK/AAB or Android mGBA shared library was produced. Device-only behavior remains unverified.

## Critical truth boundaries

- Social provider OAuth is not implemented because no provider credentials or redirect URIs exist; Retra stores user-entered public identity labels/links locally and supports the Android share sheet.
- Internet multiplayer relay is an interface, not a hosted service.
- LAN framing is implemented and loopback-tested, but actual GBA link play requires emulator-core link callbacks and synchronized clocks.
- Achievements are local Retra achievements; no third-party achievement provider is claimed.
- Raw memory-write cheats remain disabled pending reviewed width/endianness translation.

## Resume point

On a prepared Android workstation:

1. Generate the Gradle wrapper, resolve dependencies, and repair any Android-only compile errors.
2. Build the pinned mGBA Android library and verify legal notices and binary hashes.
3. Run arm64 device tests for launch, video, audio, touch/gamepad input, saves, cheats, lifecycle, catalog downloads, Room migrations, and accessibility.
4. Add real link-cable callbacks to the selected core before enabling multiplayer gameplay.
5. Only then add credentialed OAuth, a production relay, cloud saves, or external achievement providers.
