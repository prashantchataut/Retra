# Changelog

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
