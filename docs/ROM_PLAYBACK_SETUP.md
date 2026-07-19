# ROM Playback Setup

Retra's Android player is wired to a gameplay-capable mGBA 0.10.5 libretro adapter. The application never downloads native executable code at runtime. A reviewed mGBA core must be compiled and bundled at build time for each Android ABI.

## 1. Obtain a pinned source tree

Preferred reproducible archive path:

```bash
./scripts/fetch-mgba-archive.sh
```

This downloads Debian's DFSG repack of mGBA 0.10.5 over HTTPS, verifies SHA-256
`8aee6705d2dd0fa1cbfdba2c2c475f630001d855b384849a1a6288e9aa376680`, checks the MPL-2.0 notice, and writes `third_party/mgba/SOURCE_LOCK.txt`.

The Git path remains available:

```bash
./scripts/fetch-mgba.sh
```

Review the resulting source lock before release.

## 2. Build Android libretro binaries

Install a reviewed Android NDK and CMake, then run:

```bash
export ANDROID_NDK_HOME=/path/to/android-ndk
ABIS="arm64-v8a armeabi-v7a x86_64" ./scripts/build-mgba-libretro-android.sh
```

The script stages:

```text
emulation/native/src/main/jniLibs/<abi>/libmgba_libretro.so
```

The APK selects `MgbaLibretroEmulationCore` only when the required ABI is present and the complete libretro symbol set loads. Otherwise it falls back to the explicit diagnostic core and does not claim gameplay.

## 3. What the Android frontend already provides

- memory-only `.gba` loading after SHA-256 re-verification;
- XRGB8888, RGB565, and 0RGB1555 conversion;
- `SurfaceView` presentation with integer scaling or optional smoothing;
- touch, keyboard, Bluetooth, USB, D-pad hat, and analog fallback input;
- low-latency `AudioTrack` streaming, focus handling, mute, and volume;
- battery-save memory import/export;
- versioned, checksummed save states and suspend states;
- fast-forward speed accumulator;
- declarative libretro cheat activation with a pre-cheat backup;
- app background suspend/resume and save flushing.

## 4. Release gates

A distributable gameplay build still needs device testing on each ABI for ROM timing, save types, RTC, audio focus, Bluetooth latency, controller reconnect, process death, low storage, thermal pressure, and state compatibility. Use only homebrew, public-domain fixtures, and personal backups during validation.
