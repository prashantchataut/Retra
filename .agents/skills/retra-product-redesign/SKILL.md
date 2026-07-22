---
name: retra-product-redesign
description: Redesign and implement the Retra Android emulator experience in Jetpack Compose. Use when auditing the Retra repository, changing its information architecture, creating Material 3 Archive Glass screens, refining onboarding, home, library, Discover, profile, settings, game-details, or player UX, producing original Retra branding, or reviewing implementation quality, accessibility, performance, content provenance, and legal discovery boundaries.
---

# Retra Product Redesign

## Mission

Turn Retra from an emulator utility into a polished, private memory archive for returning to handheld worlds. Preserve emulator behavior and user data while replacing weak hierarchy, navigation, and presentation.

## Workflow

1. Inspect the repository before editing. Identify modules, state ownership, persistence, navigation, emulator boundaries, supported formats, and tests.
2. Separate functional inventory from visual inheritance. Preserve capabilities, not the old composition.
3. Read `references/product-principles.md` before choosing visual or interaction direction.
4. Read `references/screen-blueprints.md` before implementing primary screens.
5. Build from shared primitives outward: theme, Portal / Save Core mark, atmosphere, navigation, then screens and states.
6. Preserve the legal content boundaries in `references/legal-content.md`.
7. Run `references/review-checklist.md`. Report checks that cannot run instead of claiming success.

## Codebase Rules

- Prefer focused Compose files over further expanding the original UI monolith.
- Reuse repository state and ViewModel methods unless a product requirement genuinely needs new domain state.
- Keep edge-to-edge behavior and consume insets once.
- Respect `reduceMotion`, `reduceTransparency`, font scale, high contrast, and 48 dp minimum interactive targets.
- Use Material 3 semantics and adaptive layouts: bottom navigation on phones, a rail on wider windows, and two-pane settings where useful.
- Keep emulator controls and game rendering quieter than library chrome.
- Avoid blocking I/O in composables and effects that require continuous recomposition.
- Do not add font binaries, copyrighted game artwork, or proprietary game data without an explicit license.

## Archive Glass Direction

- Use mineral black, deep navy, graphite, ice blue, aqua, coral, mint, and restrained warm-gold semantics.
- Do not use purple, neon-violet, rainbow chrome, or visible multicolor gradients.
- Create glass with translucent tonal surfaces, subtle borders, clipped ambient blur on Android 12+, and opaque fallbacks.
- Never blur text or content. Never use transparency as the only state cue.
- Keep game artwork dominant on Home and Library; technical and legal surfaces stay calm and highly legible.
- Use the Portal / Save Core mark: open return loop, inner save core, and three-pixel interruption.

## Content Systems

- Use official creator pages as the default Discover action.
- Permit direct downloads only for HTTPS assets with explicit distribution permission, bounded size, and a published SHA-256.
- Treat patches as patch files. Require the user to supply the compatible base ROM locally.
- Support `.rci` trusted cheat indexes only when each pack is HTTPS, SHA-256 pinned, licensed, and matched to the exact ROM hash, game code, and revision.
- Keep all imported ROMs, saves, screenshots, and account identity private by default.

## Deliverables

For a full redesign task, produce updated source/assets, an original logo, concise UX and security documentation, validation evidence with limitations, and the complete updated project archive.

## Boundaries

Do not package, link to, scrape, index, or automate downloads of unauthorized commercial ROMs. Retra may support user-owned ROM imports, legal homebrew/public releases, official project pages, checksum-pinned authorized files, and patches applied to a compatible base ROM supplied by the user.
