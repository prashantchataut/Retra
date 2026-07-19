# Retra 0.6.0 Build Report

## Reported failure

The supplied CI run reached `:app:compileReleaseKotlin` after successfully compiling the platform-neutral modules and all three native ABIs. Kotlin then rejected six explicit imports of Compose's internal `weight` implementation.

## Repair

Removed `import androidx.compose.foundation.layout.weight` from:

- `CommunityUi.kt`
- `ControllerUi.kt`
- `OnboardingUi.kt`
- `PlayerUi.kt`
- `ProfileUi.kt`
- `RetraUi.kt`

`Modifier.weight(...)` remains only inside `Row` or `Column` receiver scopes, which resolves to the public Compose API. `tools/project-verification/run.sh` now fails if the forbidden import returns.

## Implemented in 0.6.0

- Prism Glass component system and quieter dark/light palettes.
- Minimal adaptive top bar, bottom bar, navigation rail, content panels, pills, profile, community, catalog, Vault, library, onboarding, and player-status surfaces.
- Categorized settings UX instead of one giant preference form.
- Reduced-transparency fallback and decorative-only blur.
- Semantic haptic engine with API-aware predefined effects and brief API 26-28 fallbacks.
- Original Retra tap, confirmation, save, achievement, error, and invite sound cues.
- Asynchronous `SoundPool` load tracking so unloaded cues are never played.
- Separate preferences for haptics, UI sounds, UI-sound volume, and notification categories.
- Android notification channels and Android 13+ contextual permission flow.
- Achievement, verified-download, multiplayer-room, and suspend-protection notifications.
- Existing ROM playback, saves, rewind, screenshots, catalogs, patches, cheats, identity, and multiplayer foundations retained.

## Verification executed here

```text
core-verification:       36 passed, 0 failed
native-verification:     PASS
libretro-verification:   PASS
project-verification:    PASS
shell syntax:            PASS
JNI C++20 -Werror:       PASS
libretro C++20 -Werror:  PASS
sound asset inspection:  PASS (6 PCM mono cues, all under 0.5 seconds)
```

## Full Android build

**Not executable in this sandbox.** There is no Gradle runtime, Android SDK/NDK installation, ADB, emulator, or dependency cache. The exact CI compilation error has been removed and guarded, but `:app:assembleRelease` must be rerun in the user's Android build environment before calling the release buildable.

Expected command:

```bash
gradle --no-daemon --stacktrace :app:assembleRelease
```

## Required device validation

- API 26 through current Android versions;
- dark, light, OLED, dynamic color, high contrast, reduced motion, and reduced transparency;
- phone, tablet, foldable, gaming handheld, display cutout, and large font sizes;
- haptic quality and fallback behavior across actuator classes;
- notification permission denial/allow/dismiss, channels, sounds, vibration, and system-disabled notifications;
- SoundPool routing, Bluetooth, Do Not Disturb, mute, and audio focus;
- TalkBack, switch access, controller-only navigation, and minimum touch targets;
- gameplay audio/video/input/save behavior with reviewed mGBA ABI libraries.
