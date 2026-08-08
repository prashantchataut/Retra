# Retra 3.0 Final UI/UX Specification

## Product thesis

Retra is a private memory player for Game Boy Advance worlds. The first question is “What do I return to?”, not “Which file-management tool do I open?”

## Visual system

- Archive Glass with mineral black/navy foundations.
- Ice, aqua, coral, mint, and restrained warm-gold semantic accents.
- Translucency only for grouping/navigation/high-value actions.
- No blurred text, purple gaming chrome, rainbow gradients, letter logo, copied console trade dress, or copyrighted character branding.
- Opaque reduced-transparency mode, reduced motion, high contrast, scalable system typography.
- Vault Aperture / Memory Prism brand across launcher and in-app surfaces.

## Primary information architecture

### Home

Continue hero, core readiness, recent/favorite worlds, compact saves/milestones, and local privacy.

### Library

Search; file/folder import; All, Continue, Favorites, Patched, Homebrew, Unplayed filters; adaptive artwork grid; detailed list; demo recovery.

### Discover

Original built-in homebrew, Patch Studio, creator-first gallery, and explicit source-only behavior when redistribution/checksum requirements are absent.

### Profile

Local identity, archive progress, save health, achievements, and recent play without invented social metrics.

### Settings

Appearance, Player, Controls, Saves, Privacy, and About. Settings exposed in UI must change real persisted behavior.

### Game details

Artwork, provenance, core readiness, Play, favorite, artwork, cheats, technical identity, and confirmed removal.

### Player

Game canvas first; compact controls; saves, load, pause, fast-forward, screenshots, rewind, per-game profiles, controller-first mode, and honest performance telemetry.

## Legal/trust behavior

- Commercial ROMs are user-supplied only.
- UPS/IPS/BPS remain patches and require a compatible local base.
- Direct downloads require HTTPS, explicit redistribution permission, a published SHA-256, a bounded size, and creator/license/source metadata.
- External imports are reviewed before repository mutation.
- ROMs and saves remain local unless the user explicitly exports or shares them.

## Validation standard

The source implementation is not release-complete until Android compilation, resource linking, mGBA ABI loading, instrumented tests, responsive screenshots, accessibility passes, and physical-device gameplay are successful.
