# Retra 2.x Roadmap

## P0 — Make the emulator dependable

- Establish reproducible CI for debug and unsigned release builds; inject signing only from private CI secrets.
- Build and pin reviewed mGBA cores for `arm64-v8a`, `armeabi-v7a`, and `x86_64`; fail releases when an ABI is missing.
- Add Room migration tests for every schema, especially 5→6, plus process-death and low-storage tests.
- Add a Save Health center: battery-save presence, last successful write, backup count, corruption detection, and restore preview.
- Add export/import of a Retra backup bundle that excludes ROMs by default and includes saves, states, artwork references, metadata, controller profiles, and settings.
- Add controller calibration and per-device/per-game mappings with a conflict detector.
- Test 30-, 60-, and 120-minute sessions on representative low-, mid-, and high-end Android hardware.
- Add opt-in crash diagnostics that strip ROM names, file paths, hashes, save contents, and account identifiers.

## P1 — Build the best personal archive

- Smart shelves: Recently resumed, Unfinished, Favorites, Patched variants, Homebrew, and Save at risk.
- Save Timeline: named snapshots, automatic milestones, diffable metadata, retention controls, and one-tap rollback.
- Patch Workspace: base fingerprint, patch fingerprint, expected output fingerprint, patch notes, and a visible lineage graph.
- Cover/artwork providers with explicit source and license caching; never infer identity from artwork.
- Deep-link and Android share-sheet imports with a review step before storage.
- Performance profiles per game and device, with measured frame pacing rather than decorative "boost" toggles.
- Optional end-to-end encrypted cloud saves with conflict resolution and local export keys. Do not make accounts mandatory.
- Accessibility: full remapping, switch-access-friendly controls, haptic alternatives, large-control mode, high contrast, and screen-reader labels.

## P2 — Expand carefully

- GB/GBC support as a separate pinned core with separate compatibility and save namespaces.
- Nintendo DS support only as a separate architecture milestone. It requires a DS core, dual-screen layouts, touch/stylus input, microphone considerations, performance testing, and its own compatibility matrix.
- Netplay only after deterministic synchronization, rollback policy, desync detection, and privacy design are proven.
- Android TV, tablet, foldable, and desktop layouts after the phone player is stable.
- Privacy-safe compatibility reports containing core version, device class, settings, and ROM checksum only—never ROM bytes or save data.

## Features to avoid

- Commercial ROM search or download.
- "AI game enhancement" labels without measurable behavior.
- Fake cloud sync backed only by local account state.
- Unbounded rewind or state storage.
- Downloading native emulator cores at runtime.
- Social feeds before saves, controller handling, and core stability are trustworthy.
