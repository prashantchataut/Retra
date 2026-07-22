# Retra 2.2 Critical Product Review

## What was wrong with the prior build

The earlier package looked broader than it was. Several surfaces were written as large one-off Compose files, the player lacked enough tactile hierarchy, catalog cards did not visually prove what they represented, and the CI report failed to expose the real Room schema error early. That combination made the project feel generated rather than deliberately engineered.

## What changed

### Product structure

Retra is now framed as three dependable systems:

1. **Archive** — verified local games, artwork, metadata, patches, collections, and provenance.
2. **Player** — a focused emulator surface with predictable save, speed, rewind, display, audio, and control behavior.
3. **Vault** — health checks, rotating backups, restore, achievements, and ROM-free portable export.

Decorative features do not appear in navigation unless they have working data and a recovery story.

### Player experience

The player now supports portrait and landscape arrangements, fit/fill/integer scaling, immersive mode, classic/compact/left-handed/controller-first presets, glass/solid/minimal controls, control scale, spacing, opacity, dead zone, shoulder visibility, quick actions, save/load, screenshots, rewind, speed, cheats, reset, autosave, and pause-on-background behavior.

The screen receives visual priority. Controls are grouped by function rather than spread evenly for symmetry. Customization is available from the player without forcing the user out of a session.

### Library and discovery

The library is local-first and checksum-addressed. Discover uses legal, creator-published Homebrew Hub releases with real screenshots and retains creator/license/source metadata after installation. Commercial Pokémon titles are represented only as owned-game guides and patch workflows.

### Achievements

Retra 2.2 ships working app-level achievements for real lifecycle events: importing a first game, creating saves, applying a patch, using rewind, taking a screenshot, completing sessions, accumulating playtime, and exporting a backup. It does not pretend to know in-game events without reading verified game memory definitions.

### Reliability

The Room schema failure is fixed structurally, not hidden. Backup import is bounded, path-normalized, ROM-free, and validates save envelopes against the game hash. External intents now stop at a review dialog. The native core is built from a pinned source archive for each supported ABI and CI fails if any ABI is absent.

## What is still not final

- Android compilation and installation must pass on the provisioned CI runner.
- Real devices must complete 30-, 60-, and 120-minute sessions.
- Per-device and per-game controller profiles need a dedicated persistence model and conflict editor.
- Named save timelines need a first-class screen.
- True game-specific achievements need an audited rcheevos/RetroAchievements integration, authentication, and hardcore-mode semantics.
- GPU/frame-pacing telemetry needs measurement before automatic recommendations are shown.

Retra 2.2 is materially more honest and functional, but it should not be called store-ready until those gates pass.
