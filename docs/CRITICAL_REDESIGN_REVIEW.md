# Retra 0.8 Critical Redesign Review

## What was wrong with the earlier product

The prior implementation had capable emulator infrastructure but presented it as a collection of feature panels. Navigation mixed destinations with utilities, settings exposed implementation details, identity was buried, visual accents competed with game artwork, and several screens resembled concept-shot dashboards more than a durable Android product. Decorative glass and gradients risked becoming the product instead of supporting it.

The deeper UX problem was not color. Retra did not clearly answer:

1. What should I play now?
2. Where are my games and saves?
3. Can I trust this import/download/cheat action?
4. What remains local and private?

## Decisions made

- Rebuilt the visible composition rather than reskinning it.
- Made Home about returning, Library about organizing, Discover about provenance, and You about identity/save history.
- Removed Settings from permanent phone navigation because it is a utility, not a daily destination.
- Made artwork carry emotion and kept diagnostics/settings visually quiet.
- Replaced decorative multicolor gradients with tonal glass and solid ambient light pools.
- Replaced the old mark with the supplied Portal / Save Core identity.
- Preserved user data and repository state ownership instead of duplicating state in composables.
- Refused to turn Discover into an unauthorized ROM storefront.
- Added exact-ROM trusted cheat indexes instead of making copy/paste cheats easier but still unsafe.

## What is genuinely complete

- The product shell and active primary-screen routing are rebuilt.
- The new theme, brand assets, onboarding, Home, Library, You/Profile, Settings, game-details cheat experience, and legal discovery direction are implemented in source.
- Host-level ROM/patch/save/cheat/catalog/native/libretro verification passes.
- Static project checks pass.

## What is not yet proven

- Android/Compose compilation in this sandbox.
- Real-device layout, text truncation, gesture ergonomics, accessibility services, and OEM rendering.
- Real mGBA ABI packaging in the final APK.
- Long-session save, rewind, battery, audio, controller, and thermal behavior.
- Production-quality metadata/artwork licensing at scale.

## Recommended next features

### P0 — release confidence

- Screenshot tests for every primary screen at compact and expanded widths.
- Instrumented save-state recovery and process-death tests.
- Per-ABI mGBA smoke tests in CI.
- A visible “archive health” screen for save backups, corrupted states, and storage pressure.

### P1 — stronger library ownership

- Per-game controller and performance profiles.
- Patch provenance timeline showing base hash, patch hash, output hash, and version history.
- User-controlled local artwork/metadata packs with explicit licenses and no automatic copyrighted scraping.
- Duplicate/variant grouping so clean bases, patched variants, and regional revisions appear as one family.

### P1 — safer convenience

- Signed catalog/index metadata in addition to SHA-256 payload checks.
- Cheat-pack versioning, changelogs, rollback, and per-cheat compatibility notes.
- Automatic local pre-session backup with retention controls.
- Import queue with pause/retry and a clear audit trail.

### P2 — optional ecosystem

- End-to-end encrypted, opt-in save sync with conflict resolution.
- LAN multiplayer UX after real link-cable callbacks are complete.
- Shareable, privacy-filtered play cards generated locally.
- Baseline Profiles and macrobenchmarks for cold start, library scrolling, and player frame pacing.

## Product rule going forward

A feature belongs in Retra only when it improves return-to-game speed, archive confidence, or player control. It should not be added merely because it looks impressive in a mockup.
