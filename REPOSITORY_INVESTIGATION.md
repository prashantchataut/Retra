# Retra repository investigation

Date: 2026-08-08

## What the app is

Retra is a local-first Android Game Boy Advance archive and emulator. Its primary job is **returning to a known game quickly**, not finding commercial ROMs. The product boundary is intentional: the user imports a backup they are allowed to use, while Retra stores provenance, hashes, artwork, saves, patches, and preferences around it.

The app has three distinct responsibilities:

1. **Archive** — import `.gba`, supported archives, and patch files; identify games by SHA-256/SHA-1/CRC-32 plus GBA metadata; de-duplicate; persist content-addressed copies and Room records.
2. **Play** — load a `GameFile` through the `EmulationCore` API, present the native frame surface and touch/hardware controls, and manage pause, save states, battery saves, rewind, audio, screenshots, cheats, speed, and lifecycle suspension.
3. **Stewardship** — keep rotating save backups and timeline checkpoints, expose save health, support portable ROM-free backups, and keep optional discovery limited to creator/homebrew sources.

## Runtime flow

`MainActivity` is the Android boundary. It installs `RetraV3Root`, routes VIEW/SEND intents into a review dialog, and forwards gamepad keys/axes to the ViewModel. `RetraViewModel` is the orchestration layer: it combines repositories, settings, emulation, feedback, notifications, and state flows.

The root chooses one of three states:

- onboarding until `AppSettings.onboardingComplete`;
- `PlayerScreen` while `activeGame` is non-null;
- the adaptive four-destination app shell otherwise.

The shell is Home, Library, Discover, and You/Profile. Phone uses a bottom dock; wider layouts use a navigation rail. Selecting a game opens a sheet rather than navigating away. Import and patch actions are handled by Activity Result contracts, then serialized by repositories.

## Important data and safety boundaries

- `GameRepository` is the import boundary. It validates file type/size/format, calculates hashes, avoids duplicate identity, and persists managed copies atomically.
- `PatchRepository` never replaces the base game. It requires a compatible local base and creates a separate patched library record.
- Catalog and Homebrew repositories enforce HTTPS, bounded reads, origin/redirect rules, and local validation. However, Homebrew Hub's documented objects do not provide a published digest, so its local post-download hash is integrity evidence, not release authenticity.
- Cheats are exact-ROM-bound data and are validated for dependencies/conflicts. A checksum match does not prove that community cheat code is safe or correct.
- `BackupRepository` exports metadata, artwork, saves, and achievements but deliberately excludes ROM bytes.
- The native layer has an `EmulationCore` contract and a diagnostic/reference fallback. A diagnostic core must not be presented as playable; the UI correctly disables Play when gameplay capability is unavailable.

## Current strengths

- Strong privacy/legal posture: no commercial ROM store, no runtime native-core download, no bundled proprietary BIOS, and no reusable signing credential.
- Good identity model: filenames/artwork are not canonical identity; hashes and parsed ROM metadata are.
- Import and patch operations are observable and mostly failure-safe (duplicate/rejected/batch outcomes, cleanup, atomic persistence).
- Controller input sources are merged independently, which prevents releasing touch input from cancelling a still-held hardware input.
- Lifecycle handling records playtime, clears held input on background/disconnect, optionally suspends, and pauses on audio-route loss.
- The UI system has an explicit Archive Glass palette, reduced-transparency mode, high contrast, font scaling, adaptive navigation, and TalkBack-oriented labels.

## Limitations and logical risks

### Release and device validation

The repository documents core/host verification, but the important product path is still not proven in this checkout: Android/Compose compilation, Room migration instrumentation, packaged mGBA libraries, APK installation, physical controller behavior, audio routing, thermal behavior, long sessions, and real Homebrew/Libretro responses. This environment has no `java` executable, so Gradle tests could not be run here.

### Native gameplay availability

The UI can be fully navigable while the packaged gameplay core is absent. This is honest, but it means a successful APK build is not equivalent to a playable release. ABI coverage and the mGBA/libretro shared library must be checked on real arm64-v8a, armeabi-v7a, and x86_64 devices before publishing.

### Online provenance

Homebrew installation performs TLS download, GBA validation, and a locally recorded SHA-256, but the upstream API's missing expected digest means Retra cannot establish that the downloaded bytes are the creator's intended release. The UI correctly distinguishes verified/direct-install eligibility from creator-page-only content, but this distinction must remain prominent.

### Identity is optional but cloud identity is incomplete

Google sign-in is optional and local play does not require it. The repository explicitly does not persist the raw ID token and warns that a backend still needs nonce/token verification. Therefore account connection must not be treated as authorization for cloud saves, social features, or trusted sync until that backend exists.

### User-facing product gaps

- The prior empty Home state told users that the built-in demo existed but required navigating elsewhere to find it. The empty state now exposes both **Import game** and **Install offline demo**.
- Discover previously kept a refresh action active even when online recommendations were disabled. It now removes that action in the disabled state, avoiding a privacy-setting contradiction.
- The Library filter previously said **Continue** even though it meant “has been played at least once.” It now says **Played**, matching the actual predicate.
- Some advanced functionality (Controller Studio, Save Timeline, performance advisor, compatibility notebook, and multiplayer) lives in secondary screens rather than the primary four-destination shell. This keeps the shell calm, but those surfaces need device QA and clear entry points from game details/settings.

## Recommended next verification order

1. Install JDK 17 and run the core, native, schema, and Android unit/instrumentation suites.
2. Build and inspect the debug APK with each target ABI; verify the runtime descriptor and Play gating on a device without the mGBA library and on a device with it.
3. Test import from file picker, folder, VIEW, and SEND intents, including revoked URI permissions and malformed ZIP/patch files.
4. Test save state, battery save, timeline restore, backup restore, app kill, background/foreground, headphone disconnect, and low-storage behavior.
5. Test touch + Bluetooth controller simultaneously, key repeat, controller disconnect, analog dead zones, triggers, TalkBack focus order, 320dp width, large font, landscape cutouts, light/dark/OLED, and reduced transparency.
6. Run Homebrew and Libretro integrations against malformed, slow, changed, and rate-limited responses. Keep direct install disabled unless authenticity is stronger than a post-download local hash.

This report intentionally separates what is implemented in source from what is verified in a packaged Android product.
