# Known Issues and Validation Gaps — Retra 3.0.0

## Release blockers before publishing

1. **Run the Android build.** This environment did not have a provisioned Android SDK/NDK, so Gradle/Compose/AAPT2 compilation remains unverified here.
2. **Stage and verify all mGBA ABIs.** `arm64-v8a`, `armeabi-v7a`, and `x86_64` must each contain a loadable `libmgba_libretro.so`; importing a ROM is not evidence that gameplay works.
3. **Run real-device smoke tests.** Validate boot, audio, controls, save/load, suspend/resume, rewind, fast-forward, screenshots, folder imports, patch application, and process death restoration.
4. **Run accessibility passes.** Test light/dark/system themes, high contrast, reduced transparency, reduced motion, 1.3× text, TalkBack traversal, switch access, and minimum touch targets.
5. **Run responsive passes.** Test a compact phone, tall phone, landscape phone, 7–8 inch tablet, and a large/foldable width.
6. **Confirm patch output in the final Android build.** The uploaded UPS container is valid, but applying it to a commercial base was not executed in this environment because that base is neither included nor requested.

## Product limitations

- Retra currently targets Game Boy Advance. Nintendo DS `.nds` files, including HeartGold and SoulSilver, are intentionally rejected.
- Retra Drift is the only built-in playable title. Commercial games cannot be bundled.
- The Homebrew Hub is only as useful as its maintained creator/licensing/checksum metadata; unverified entries should continue to open creator pages rather than install silently.
- Cloud synchronization and cross-device conflict resolution are not release-ready product systems. Local backup/export remains the trustworthy path.
- Gameplay compatibility claims need a device-backed compatibility matrix rather than user-interface labels alone.
- Cover art for commercial games remains user-supplied or metadata-only unless a lawful licensed provider is integrated.

## Engineering debt

- The app uses one UI shell (`RetraApp` and focused screen files in `docs/UI_MAP.md`). Older historical docs under `docs/` are implementation history, not active specs.
- The current navigation is state-driven rather than Navigation Compose route-backed. It is suitable for the present four-destination shell, but deep links and saved back stacks should move to typed routes before the app grows further.
- Automated screenshot/golden tests should be added once a stable Android build environment exists.
- Real-device play requires staging `libmgba_libretro.so` via `scripts/build-mgba-libretro-android.sh` (jniLibs are gitignored).

## Manual acceptance matrix

- First launch installs Retra Drift once, not repeatedly.
- Home shows readable headings/icons in all themes.
- Retra Drift launches only when the real core reports ready.
- Importing a valid `.gba` creates one library record; reimport reports a duplicate.
- Importing the supplied `.ups` opens Patch Studio and never labels it as a playable game.
- Importing the exact compatible base while Patch Studio is open refreshes the match list.
- Applying a patch never overwrites the base ROM.
- A mismatched base is rejected before output is committed.
- Saves survive process death and have a recoverable timeline/checkpoint.
- Removing a game requires confirmation and does not silently delete unrelated saves.
- External VIEW/SEND imports show review before repository mutation.
