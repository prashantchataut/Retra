# Changelog

## 2.3.0 — 2026-07-22

### Build and signing repair
- Replaced the false-positive signing check with a tracked-source policy that rejects committed keys and literal passwords while allowing Provider/environment-backed signing.
- Added optional CI keystore materialization under `$RUNNER_TEMP`, all-or-none signing secret validation, Gradle wrapper validation, and wrapper-based build commands.
- Debug builds continue to use Android debug signing; release compilation works unsigned without secrets and signs only when all four variables are supplied.

### Controller Studio
- Added real Android gamepad key, D-pad, joystick, hat, and analog-trigger handling.
- Added per-device and optional per-game mappings, live input testing, remapping capture, dead-zone calibration, trigger calibration, and atomic profile persistence.
- Separated touch and hardware input sources so releasing one input cannot cancel the same button still held by another source.

### Saves and performance
- Added named and automatic immutable Save Timeline checkpoints with bounded retention and reversible restore.
- Added locally measured frame-time, FPS, speed, dropped-frame, audio-underrun, thermal, and battery evidence with recommendations only after meaningful samples.
- Added per-game launch profiles and a compatibility notebook.
- Corrected fast-forward frame pacing and GBA-target speed metrics; legacy Boosted/Extreme settings normalize to Balanced.

### Experience
- Added Controller Studio, Save Timeline, and Measured Performance Advisor to Settings.
- Added compatibility and per-game profile tools to game details and controller customization to the player.
- Improved small-screen chip rows and retained separate Profile/Settings pages with developer attribution to **Prashant Chataut**.

## 2.2.0 — 2026-07-22

### Build repair
- Fixed the reported KAPT/Room failure by replacing the truncated schema 6 JSON with a complete schema.
- Applied the Room Gradle plugin, added a 5→6 migration instrumentation test, and validates schemas before and after Android compilation.
- Disabled parallel schema generation and split CI into strict, diagnosable stages.
- CI now uploads an installable debug APK for private testing while separately compiling the unsigned release variant; it no longer publishes an unsigned production release.

### Player and customization
- Rebuilt the active player for portrait and landscape play with screen-first layout hierarchy.
- Added classic, compact, left-handed, and controller-first presets plus glass, solid, and minimal control styles.
- Added control scale, spacing, opacity, dead zone, shoulder-button visibility, quick actions, immersive mode, fit/fill/integer scaling, save/load, screenshot, rewind, speed, audio, reset, cheats, and autosave controls.

### Library, content, and artwork
- Added legal Homebrew Hub GBA discovery with real provider screenshots, bounded HTTPS loading, one-tap eligible installs, local SHA-256, and retained creator/license/source provenance.
- Kept commercial Pokémon titles as owned-game guides and exact-checksum import workflows rather than ROM downloads.
- Added user artwork and preserves artwork as presentation metadata rather than game identity.
- Bundled the user-supplied Pokémon Heart & Soul v1.2.1 UPS patch with creator credit and SHA-256 validation; no commercial base ROM is included.

### Progress and safety
- Added working local achievements for imports, saves, patches, screenshots, rewind, sessions, playtime, and backup export.
- Added Save Health metrics, previous-backup restore, and bounded ROM-free portable backup import/export.
- Added explicit review before importing files received through Android VIEW/SEND intents.
- Removed legacy duplicate UI files and obsolete project-state scratch documents.

### Product identity
- Rebuilt onboarding, Home, Library, Discover, Profile, Settings, game details, and About around the active Archive Glass system.
- Settings and Profile are separate. About identifies **Prashant Chataut** as developer and credits Homebrew Hub/gbdev for legal homebrew discovery.
- Added a critical 2.2 product review, roadmap status, content policy, build-failure analysis, and 2.3 recommendations.

## 2.0.0 — 2026-07-22

### Real library and identity
- Added live Homebrew Hub search, pagination, source pages, and eligible one-tap GBA homebrew installs.
- Added SHA-1 ROM identity, Libretro No-Intro DAT parsing, exact canonical metadata matching, and Room schema 6 with a non-destructive 5→6 migration.
- Added clear Nintendo DS rejection for HeartGold, SoulSilver, Platinum, and other `.nds` files while the app remains GBA-only.

### Cheats
- Added matching Libretro `.cht` lookup and local RetroArch `.cht` import.
- Convert community files into restricted Retra Codes bound to the exact ROM SHA-256, game code, and revision.
- Skip placeholder definitions such as `????` while preserving supported concrete cheats.

### Reliability and product hardening
- Serialized ROM imports, retained SHA-256 uniqueness, and cleaned up newly created managed files when database persistence fails.
- Preserved established user title edits during later canonical metadata refreshes.
- Removed the committed sideload signing key and credentials; release signing is now a private CI/store responsibility.
- Expanded the platform-neutral suite to 43 passing checks and expanded static gates for v2 capabilities.

### Experience
- Expanded onboarding to four stages, including source provenance and the commercial-ROM boundary.
- Rebuilt separate Profile and Settings experiences; About identifies **Prashant Chataut** as developer.
- Added Library Health and Content Sources sections.

### Honest release state
- Commercial Pokémon ROMs are not bundled or downloaded. FireRed/Emerald use the lawful-backup → exact metadata → matching cheat flow.
- Android Gradle compilation could not start in this sandbox because the Gradle distribution host could not be resolved.

## 0.8.0 — 2026-07-21

### Final product redesign
- Rebuilt the visible app from scratch around Home, Library, Discover, and You, with contextual Settings rather than preserving the previous layout.
- Added the Archive Glass Material 3 system with mineral black/navy foundations, ice/aqua/coral/mint accents, static ambient blur, and opaque accessibility fallbacks.
- Removed purple/violet and visible multicolor-gradient direction from the active theme and launcher resources.
- Rebuilt onboarding, Home, Library, profile, settings, game details, and cheat surfaces with adaptive navigation and two-pane settings.
- Retained system typography instead of bundling unlicensed font binaries.

### Brand
- Replaced the previous letter/negative-space concepts with the supplied Portal / Save Core mark.
- Regenerated SVG, PNG, launcher, monochrome, splash, and Compose assets.

### Legal library and patches
- Added official Heart & Soul project releases as patch-only discovery entries requiring a user-supplied compatible base ROM.
- Added Minicraft GBA official open-source release discovery and corrected Butano licensing metadata.
- Kept direct downloads disabled unless an official release exposes a usable SHA-256 digest.
- Explicitly excluded Poke Harbor and other unauthorized commercial ROM indexes.

### Trusted cheat indexes
- Added strict `RETRA-CHEAT-INDEX-1` / `.rci` parsing with bounded UTF-8 input, safe identifiers, duplicate/unknown-field rejection, HTTPS enforcement, exact ROM matching, license/permission metadata, and SHA-256-pinned `.rcc` packs.
- Added atomic index persistence, game-specific compatible-pack discovery, and one-tap verified pack installation from game details.
- Expanded the platform-neutral suite to 39 passing checks.

### Verification and release state
- Bumped the app to versionCode 10 / versionName 0.8.0.
- Updated static verification for Archive Glass, Portal / Save Core branding, trusted cheat indexes, and the final experience file.
- Android Gradle compilation was not run in this sandbox because the Gradle distribution was unavailable and DNS access to `services.gradle.org` failed; host/static suites are reported separately.

## 0.7.1 — 2026-07-19

### Premium UI phase
- Replaced decorative glass/blur chrome with Material top app bar, bottom navigation, and navigation rail.
- Tightened spacing/shape tokens to a 4dp grid and ≤16dp card radii; quieted multi-accent decoration.
- Replaced the letter-R glyph with a PNG negative-space brand mark for launcher, splash, and in-app logos.
- Simplified Home/Discover/onboarding copy and removed feature-readiness clutter from game details.
- Regenerated quieter interface sounds and added haptic/sound emission cooldowns.

### Gameplay honesty and discovery
- Release workflow now fails if any of `arm64-v8a`, `armeabi-v7a`, or `x86_64` mGBA cores is missing.
- `coreAvailable` means real GBA gameplay, not diagnostic fallback availability.
- Discover now prioritizes official creator pages (GBA Jam, Goodboy Galaxy Demo, Anguna, Aereven Advance) with Open-with-Retra handoff after download.
- Import diagnostics call out Nintendo DS titles such as HeartGold/SoulSilver and accept MIME-typed GBA/ZIP/patch payloads more reliably.

## 0.7.0 — 2026-07-19

### Product shell and branding
- Rebuilt permanent navigation to Home, Library, Discover, and Settings; Vault and community live under Settings, patches/cheats under game details.
- Replaced Prism Glass rainbow accents with graphite/off-white surfaces and a single indigo accent; retained dark/light, reduced motion/transparency, 48dp targets, safe areas, and adaptive bottom-nav/rail.
- Simplified onboarding to three focused screens with optional Google identity.
- Added searchable library filters for favorites, recent, unplayed, patched, and homebrew plus collections/tags editing.
- Refreshed the launcher/monochrome mark to a forward-leaning white R with one indigo highlight.

### Managed imports and guided patching
- Imports accept `.gba`, `.zip`, `.ups`, `.ips`, and `.bps`, explicitly reject `.nds`, and copy valid games into content-addressed app-private storage with bounded ZIP inspection.
- Guided pending-patch flow inspects UPS/BPS descriptors, matches base CRCs, lets the user choose a clean base, applies locally, validates output, and preserves the original.
- Heart & Soul values are metadata hints only; Retra does not bundle the patch or any commercial ROM.

### Library migration
- Room schema 5 adds CRC, managed path, collections, and tags with an explicit non-destructive 4→5 migration.
- Deletion removes only Retra-managed copies; external SAF originals are left untouched.

### Discover and external import security
- Discover groups pinned Retra Curated links, official creator release discovery, and SHA-256-pinned custom manifests.
- One-tap downloads require a valid SHA-256; otherwise the official creator page opens.
- HTTPS, redirect, private-network, size, traversal, and expansion-bomb protections remain enforced for GBA/ZIP/patch assets.
- Android `ACTION_VIEW` / `SEND` intents import compatible files into the guided pipeline.

### Release automation
- Bumped the app to versionCode 8 / versionName 0.7.0 while keeping the release build debug-signed for sideload testing.
- Rewrote the GitHub Actions release workflow to provision Java 17, Gradle 9.5.0, Android SDK platform 37, build-tools, NDK 28.2.13676358, and CMake 3.22.1.
- Added CI steps that fetch the pinned, SHA-256-verified mGBA 0.10.5 source and build the libretro core for `arm64-v8a`, `armeabi-v7a`, and `x86_64` before assembling the APK.
- Ran unit tests and release assembly in CI, with best-effort Android lint and host verification suites so an installable APK is produced end to end.
- Packaged the APK with a SHA-256 sum, uploaded it as an artifact, and published a GitHub release whose notes state the APK is debug-signed for sideload and not for the Play Store; per-ABI `ANDROID_BINARY_HASHES.txt` is attached when the native core is staged.

### Repository hygiene
- Ignored fetched/generated mGBA artifacts (`third_party/mgba/upstream`, staged `emulation/native/src/main/jniLibs`, build/source locks, and binary hash outputs) so reproduced binaries are never committed.

### Honest build status
- A full Android APK is now built in CI; local checkouts without the Android SDK, NDK, and CMake still cannot reproduce the APK.

## 0.6.0 — 2026-07-19

### Build repair
- Removed invalid explicit imports of Compose's internal `weight` property from six UI files.
- Added a static regression gate that rejects the forbidden import.

### Prism Glass redesign
- Unified onboarding, navigation, home, library, Discover, Vault, profile, community, settings, and player status under one restrained glass component system.
- Reduced saturation, simplified typography, limited blur to decorative atmosphere, and added opaque reduced-transparency fallbacks.
- Reorganized settings into focused categories rather than one long preference page.

### Feedback and notifications
- Added six original short Retra interface sounds with asynchronous SoundPool load tracking.
- Added semantic haptics for taps, game controls, confirmation, saves, achievements, invitations, and errors.
- Added separate user settings for haptics, interface sounds, interface volume, and notification categories.
- Added Android notification channels, contextual Android 13+ permission UX, and notifications for achievements, verified downloads, multiplayer rooms, and protected suspend states.

### Verification
- Retained 36/36 platform-neutral checks, native and libretro host passes, and expanded project verification for public Compose API usage, glass UI, permissions, channels, feedback, and sound assets.

## 0.5.0 — 2026-07-19

### Brand and onboarding
- Created and applied the original Retra Prism logo, wordmark, adaptive launcher icon, Android 13 monochrome icon, and Android 12+ splash treatment.
- Rebuilt onboarding as a five-step adaptive journey covering privacy, library, player safety, personalization, and optional Google identity.
- Added a polished You/profile surface with offline identity, friend code, achievement status, Google connect/disconnect, and server-verification disclosure.

### Emulator experience
- Added functional integer scaling, optional display smoothing, performance overlay, audio mute/volume, touch-control visibility, background suspend selection, and headphone-disconnect pause.
- Added immersive player system bars, five save-state slots, bounded 32 MiB rewind, screenshots, slow motion, fast-forward hold, and expanded session controls.
- Added controller disconnect clearing, live controller testing, contextual keyboard/gamepad capture, extra face/trigger mappings, and L3/R3 rewind/fast-forward shortcuts.
- Added a reproducible SHA-256-verified mGBA archive fetch path and Gradle core tasks.

### Library
- Added search, favorites, favorite shelf/count, editable titles and notes, custom cover artwork, and deterministic fallback artwork.
- Added Room schema 4 and explicit migration for favorites, notes, and cover-art paths.

### Identity and privacy
- Added Credential Manager Sign in with Google using explicit button flow, secure nonce, official credential parsing, ephemeral ID tokens, local account metadata, and credential-state clearing.
- Disabled Android automatic backup so private ROM/save/account data is not silently uploaded.

### Verification
- Expanded the pure host suite to 36 checks with bounded rewind coverage.
- Expanded static gates for branding, identity, functional settings, screenshots, artwork, Room schema 4, and mGBA acquisition.
- Kept native and libretro host suites passing.

## 0.4.0 — 2026-07-19

### Added

- Custom ROM-bound Retra Codes creation.
- Explicit HTTPS cheat-pack downloads with expected SHA-256, same-host redirects, local/private target blocking, bounded reads, strict parsing, and atomic storage.
- Libretro cheat reset/set bridge with protected pre-cheat state and battery flush.
- Explicit HTTPS custom catalog-manifest import with expected SHA-256 and hardened policy.
- Local achievements with counters, unique progress, integrity gates, persistence, progress UI, and share cards.
- Persistent private-first profile, friend code, public social identity labels/links, and Android share payloads.
- Accent palettes, glass intensity, corner/font scale, high contrast, content density, startup destination, statistics/recommendation visibility, touch opacity, haptics, and privacy/performance settings.
- Multiplayer compatibility gate, session reducer, room codes, checksummed packet codec, ordered buffer, trusted-LAN transport, internet-relay interface, and loopback verification.
- Community and achievements experience in Discover.
- Offline snapshots and provenance for all five requested design/agent skills, plus exact CLI output logs.

### Changed

- Expanded from ten to thirteen modules.
- Built-in legal recommendations now respect the visibility setting while user-imported catalogs remain available.
- Touch haptics setting now affects player controls.
- Status language now distinguishes local/host-tested systems from credentialed or core-dependent boundaries.
- Core verification expanded from 29 to 35 checks.
- Application version increased to 0.4.0.

### Fixed

- Completed the interrupted cheat adapter path instead of leaving import disconnected from gameplay.
- Removed misleading wording that treated achievements and cheat activation as universally incomplete.
- Preserved explicit fallback behavior when a gameplay-capable core is unavailable.

## 0.3.0 — 2026-07-19

- Added the libretro gameplay adapter, save/Vault infrastructure, patching, Retra Codes import/conflict analysis, restricted catalogs, and secure legal downloads.

## 0.2.0 — 2026-07-19

- Added native diagnostic pipeline, player/input/audio contracts, save envelopes, Vault, and mGBA integration gate.

## 0.1.0 — 2026-07-19

- Initial Android source project with local library, settings, catalog validation, and unavailable-core boundary.
