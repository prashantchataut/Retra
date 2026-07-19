# mGBA Android Integration Plan

## Current state

Retra contains a complete libretro-facing adapter and a fake-core ABI test. The real mGBA source and Android shared objects are intentionally excluded from the repository archive. Two pinned fetch paths and a reviewed Android build/staging script are provided; the core is selected only when the expected shared object is bundled for the current ABI.

## Fetch and lock

```bash
./scripts/fetch-mgba-archive.sh
# or
./scripts/fetch-mgba.sh
```

The script fetches tag `0.10.5`, checks for the expected MPL notice, records repository/tag/commit in `third_party/mgba/SOURCE_LOCK.txt`, and refuses to overwrite an existing tree.

Review the commit and submodules before building. Retain upstream license/notice files and any source obligations for modified MPL-covered files.

## Android build and staging

```bash
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
```

The script:

- requires the pinned source and source lock;
- requires a reviewed NDK toolchain;
- disables desktop frontends and optional scripting/network-oriented dependencies;
- builds the libretro target for selected ABIs;
- stages each binary as `emulation/native/src/main/jniLibs/<abi>/libmgba_libretro.so`;
- retains the MPL license;
- records binary SHA-256 values and build-tool details.

Review generated CMake flags against the fetched release because upstream options can change.

## Existing bridge behavior

- `dlopen`/`dlsym` required libretro ABI functions.
- Memory-only ROM load; no catalog-controlled native path.
- 0RGB1555, RGB565, and XRGB8888 conversion to Retra frames.
- GBA joypad mapping.
- Interleaved stereo PCM buffering and bounded drain.
- Serialize/unserialize state.
- Battery-save memory import/export.
- Reset, run, speed accumulator, dimensions, and sample rate.
- Single active instance to match libretro’s global callback model.

## Device acceptance matrix

Test legally redistributable fixtures for:

- ARM/Thumb CPU instruction suites and timing;
- SRAM, EEPROM, Flash64, Flash128, RTC, rumble, solar, and motion where supported;
- cold start, app background/foreground, screen rotation, process recreation, controller reconnect, low storage, low memory, and thermal pressure;
- audio focus, Bluetooth latency, underrun recovery, fast-forward mute/pitch policy;
- save crash injection at temporary write, fsync, backup, move, and directory-sync stages;
- state rejection across ROM revision, core update, patch identity, and active cheat set.

Only after these gates may a distributable build describe gameplay as verified.
