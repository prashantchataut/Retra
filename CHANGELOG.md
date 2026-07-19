# Changelog

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
