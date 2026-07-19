# Known Issues

- No Android APK/AAB was built because the sandbox lacks Gradle, Android SDK/NDK, ADB, and dependency access.
- The archive intentionally excludes compiled mGBA libraries and the upstream source tree; fetch/build scripts are provided.
- Google sign-in cannot complete without a real Web OAuth client ID, eligible device account, and device testing.
- Receiving a Google ID token only creates a device identity. Cloud/social account trust requires a production verification backend.
- Rewind serialization cadence and 32 MiB budget are not profiled on real devices; low-end tuning may be required.
- mGBA state compatibility, save memory, RTC, and cheat behavior are host-adapter tested but not Android-hardware tested.
- Custom artwork decoding and MediaStore screenshots require memory/storage testing across OEM devices.
- Pre-Android 10 screenshots use app-specific external storage rather than the public gallery.
- Touch-control placement is adaptive but a drag-to-position editor is not complete.
- Provider OAuth, cloud sync, achievements federation, hosted relay, and actual GBA link multiplayer remain external integrations.
- Room schema 4 could not be exported by AGP in this environment; the migration is statically checked but must be build-tested.
