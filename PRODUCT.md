# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

People who own Game Boy Advance ROMs and want a private, local Android archive for returning to those games. Primary job: continue a known game quickly. Secondary jobs: import/scan owned files, apply patches to a locally supplied base ROM, manage saves, and optionally browse lawful homebrew/creator releases.

## Product Purpose

Retra is a privacy-first personal memory player for handheld GBA games on Android. Success means one-tap return to play, trustworthy import/patch/save flows, and clear honesty when the emulation core is not playable.

## Positioning

A local memory archive and patch workspace, not a ROM store or social game hub. Differentiator: content-addressed local library, provenance, patch studio that requires user-owned base ROMs, and Archive Glass brand without console trade dress.

## Operating Context

Kotlin / Jetpack Compose / Material 3 app with Room, DataStore, Hilt, JNI/CMake, and a pinned mGBA/libretro core path. Fresh installs include original homebrew **Retra Drift**. Commercial ROMs and unauthorized downloads are never packaged or scraped.

## Capabilities

- Import `.gba`, `.zip`, `.ups`, `.ips`, `.bps`; reject `.nds`
- Content-addressed library with hashes, artwork, tags, favorites, provenance
- Local saves, backups, Save Timeline / Save Health, screenshots, rewind, verified cheats
- Per-game launch profiles and Controller Studio
- Discover: Drift, Patch Studio, creator-first Homebrew Hub, checksum-pinned authorized downloads only
- Core readiness reporting when mGBA binaries are missing

## Constraints

- Do not package, link to, scrape, or automate unauthorized commercial ROMs
- Patches are patch files only; user supplies compatible base ROM
- Accessibility: reduce motion/transparency, high contrast, 48 dp targets, TalkBack-safe labels
- No bundled commercial game artwork or proprietary game data without license
- Dynamic Material color must not silently erase brand palette by default

## Brand Commitments

- Name: Retra
- Tagline direction: return to play / private memory archive
- Mark: Vault Aperture / Memory Prism (non-letterform)
- Visual world: Archive Glass — mineral dark surfaces, ice/aqua/coral/mint/gold semantics; no purple or rainbow gaming gradients
- Voice: quiet, plain, trustworthy; not gamer hype or manifesto-heavy UI copy

## Evidence Sources

- README.md, docs/BRAND_IDENTITY.md, docs/RETRA_3_UX_AUDIT.md
- `.agents/skills/retra-product-redesign/`
- Live shell: `RetraV3Root` via MainActivity; ViewModel/repository domain retained

## Open Decisions

- *[assumed]* Full redesign keeps Archive Glass brand tokens and mark; replaces sloppy Compose execution, dead V23 shell, and Operate-hostile copy density
- *[assumed]* Delivery is phased (foundation → primary shell → secondary shell → player → harden), not a single big-bang PR
- Optional identity/account depth beyond local profile remains product-flexible
