# Retra 2.2 Roadmap Status

## P0 — dependable emulator

| Roadmap item | 2.2 status |
|---|---|
| Reproducible CI | Implemented for private-test debug APKs and unsigned release compilation; final store signing remains private. |
| Pinned mGBA core for three ABIs | Build scripts and CI gates implemented for arm64-v8a, armeabi-v7a, and x86_64. Core binaries are generated, not committed. |
| Room migration tests | Implemented for schema 5→6; schema parser gate added. |
| Save Health center | Implemented: valid-save count, damaged-record count, rotating-backup count, and previous-backup restore. |
| ROM-free portable backup | Implemented for settings, local achievements, valid saves, metadata, and local artwork. Import is bounded and path-safe. |
| Controller calibration and per-game mapping | Global presets, scale, spacing, opacity, dead zone, shoulder controls, and visual styles implemented. Per-device/per-game mapping remains open. |
| Long real-device sessions | Not executed in this container; remains a required release gate. |
| Privacy-safe crash diagnostics | Not implemented. |

## P1 — personal archive

| Roadmap item | 2.2 status |
|---|---|
| Smart shelves | Recent, Favorites, Patched, and Homebrew implemented. Save-at-risk is represented in Save Health, not yet as a shelf. |
| Save Timeline | Protected autosaves and rotating backups exist; named timeline snapshots and retention UI remain open. |
| Patch Workspace | Base selection, patch inspection, compatibility checks, local application, and patch lineage metadata implemented. A visual lineage graph remains open. |
| Artwork provenance | Homebrew artwork retains creator/source/license metadata; user artwork is supported. Commercial artwork providers are intentionally absent. |
| Share/deep-link review | Implemented with an explicit confirmation step before import. |
| Measured performance profiles | Player profiles and frame controls exist; automatic measured frame-pacing recommendations remain open. |
| End-to-end encrypted cloud saves | Not implemented. |
| Accessibility | High contrast, reduced motion/transparency, scalable typography, large configurable controls, labels, and left-handed/controller-first layouts implemented. Switch-access testing remains open. |

## P2 — expansion

GB/GBC, Nintendo DS, netplay, Android TV, and desktop/foldable-specific product work are not presented as complete. Retra 2.2 stays focused on a trustworthy GBA experience.
