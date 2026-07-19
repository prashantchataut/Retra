# Next Actions

1. **Create the Android build environment.** Install SDK 37, build tools, platform tools, NDK, CMake, and Gradle wrapper dependencies; run `:app:assembleDebug`, unit tests, and lint.
2. **Compile and bundle mGBA.** Build arm64-v8a first, then armeabi-v7a and x86_64; verify the exact libretro symbols and license notices inside the APK.
3. **Run gameplay validation.** Use legal homebrew and personal backups across save types, RTC games, patches, cheats, states, rewind, fast-forward, audio routes, process death, and low storage.
4. **Run UI/device validation.** Phones, tablets, foldables, gaming handhelds, Android TV exploration, controller-only navigation, TalkBack, font scaling, high contrast, reduced motion, and themed icons.
5. **Implement the identity backend.** Verify Google ID token signature/issuer/audience/expiry/nonce, issue revocable Retra sessions, support account deletion/export, and keep ROM upload disabled.
6. **Add cloud save providers.** Opt-in encrypted save/settings backup with conflict history; never default-upload ROMs.
7. **Expose mGBA link callbacks.** Build deterministic local link synchronization before enabling multiplayer gameplay claims.
8. **Finish release engineering.** R8, Baseline Profiles, macrobenchmarks, crash reporting consent, privacy policy, accessibility audit, signing, SBOM, dependency scanning, and Play pre-launch reports.
