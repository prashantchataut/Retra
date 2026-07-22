# mGBA source and binary location

The upstream source and compiled Android libraries are intentionally excluded from this archive.

1. Run `scripts/fetch-mgba-archive.sh` on a network-enabled workstation.
2. Review `SOURCE_LOCK.txt`, upstream source/submodules, and MPL-2.0 obligations.
3. Set `ANDROID_NDK_HOME` and run `scripts/build-mgba-libretro-android.sh`.
4. Review `ANDROID_BUILD_LOCK.txt`, `ANDROID_BINARY_HASHES.txt`, retained notices, and staged `jniLibs`.
5. Complete `docs/MGBA_INTEGRATION_PLAN.md` before distributing an APK.

The Retra source contains the gameplay adapter and host fake-core verification, but it will use the diagnostic fallback until a real `libmgba_libretro.so` loads.
