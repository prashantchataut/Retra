# Retra 3.0 Redesign Change Log

## Rebuilt

- Active app shell and responsive navigation.
- Onboarding, Home, Library, Discover, Profile, Settings, game details, external import review, and Patch Studio.
- Root content-color handling and dark/light surface hierarchy.
- Launcher/in-app logo system.

## Added

- Original Retra Drift GBA homebrew ROM and deterministic source build.
- Idempotent first-run demo import.
- Exact uploaded UPS diagnostics and compatible-base recovery.
- Core-readiness status and honest disabled Play state.
- Source-only Homebrew Hub behavior unless explicit redistribution permission, published SHA-256, and bounded size exist.
- Retra 3.0 audit, implementation, patch, recommendation, test, and deletion documentation.

## Preserved

- Repository/ViewModel state ownership.
- Room/DataStore persistence.
- Imports, patching, saves, timeline, health, achievements, cheats, screenshots, rewind, controller profiles, performance advice, and ROM-free backups.
- Existing mGBA/libretro build and CI staging path.

## Removed

- `RetraV23Ui.kt`, the stale active product monolith.
- Any implication that a patch file is a standalone game.
- Any implication that a diagnostic fallback is a playable GBA core.

See the root `CHANGELOG.md` for release history.
