# Changelog

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
