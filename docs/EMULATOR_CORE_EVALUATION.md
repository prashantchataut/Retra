# Emulator Core Evaluation Gate

## Selected target

mGBA `0.10.5` under MPL-2.0 is the initial gameplay-core target. Retra owns the Android lifecycle, presentation, storage, input, audio, packaging, and compatibility work around that core.

## Implemented adapters

### Native reference pipeline

A deterministic diagnostic engine verifies JNI, ROM byte transfer, frame delivery, input, serialization, reset, corruption rejection, and teardown. It always reports `DIAGNOSTIC_PIPELINE`.

### mGBA/libretro gameplay adapter

The source dynamically loads `libmgba_libretro.so`, resolves required libretro symbols, requires memory loading, maps GBA input, converts supported pixel formats, drains interleaved PCM, serializes state, and transfers battery-save RAM. A fake core proves the ABI path on the host.

The adapter reports `GBA_GAMEPLAY` only after the real shared library loads successfully. The archive does not include that binary.

## Acceptance gate before distribution

1. Reproducible Android builds for selected ABIs with locked source, notices, flags, and binary hashes.
2. Legal test/homebrew ROM loading from memory.
3. Stable frame/audio timing and bounded queues.
4. Touch/controller mapping, reconnect, sensors, RTC, and lifecycle behavior.
5. SRAM, EEPROM, Flash64/128, RTC, periodic flush, crash recovery, and backup restoration.
6. State compatibility rejection by ROM/core/version/patch/cheat identity.
7. Teardown, low-memory recreation, rotation, thermal pressure, and process death testing.
8. Privacy-sanitized diagnostics and compatibility matrix by ROM hash, device, Android version, ABI, and core commit.

Do not claim universal compatibility. Do not enable rewind, run-ahead, cloud states, or achievement integrity until gameplay and save durability pass.
