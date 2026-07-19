# Next Actions

1. **Rerun the failed CI build.** Execute `gradle --no-daemon --stacktrace :app:assembleRelease`; archive the complete new log.
2. **Run Android quality gates.** `test`, Compose instrumentation, lint, R8, dependency analysis, and release resource shrinking.
3. **Stage reviewed mGBA binaries.** Start with arm64-v8a, verify libretro symbols and notices, then add armeabi-v7a and x86_64.
4. **Perform visual QA.** Capture onboarding, home, library, player, Vault, Discover, profile, all settings categories, light/dark/OLED, large fonts, reduced transparency, and tablet layouts.
5. **Perform feedback QA.** Test every sound/haptic cue, mute and volume controls, headset/Bluetooth paths, DND, and weak/strong actuators.
6. **Perform notification QA.** Fresh install permission flow, denial, dismissal, system disable, per-channel controls, channel sounds, and deep return to the app.
7. **Run gameplay validation.** Legal homebrew/personal backups, save types, RTC, states, rewind, speed controls, patches, cheats, process death, low storage, and corrupted files.
8. **Finish production infrastructure.** Google token-verification backend, opt-in cloud saves with conflicts, privacy policy, SBOM, signing, crash consent, Baseline Profiles, macrobenchmarks, and Play pre-launch reports.
