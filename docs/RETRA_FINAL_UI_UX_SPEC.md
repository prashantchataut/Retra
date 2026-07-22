# Retra 0.8 Final UI/UX Specification

## Product thesis

Retra is a **memory archive**: a private, artwork-led place to return to handheld worlds. It must not feel like a file manager, piracy storefront, template dashboard, or collection of unrelated Dribbble screens.

The previous composition is not preserved. Existing emulator, storage, save, import, patch, cheat, identity, achievement, and multiplayer capabilities remain, but the visible hierarchy is rebuilt around three user intents: **return**, **organize**, and **protect**.

## Critical interpretation of the references

The supplied references communicate bold typography, strong device framing, dark gaming surfaces, expressive art, and translucent depth. Taken literally, they would create a generic neon concept. Retra instead uses one coherent system:

- Material 3 interaction and accessibility behavior.
- Mineral black/navy foundations rather than purple.
- Ice blue, aqua, coral, mint, and warm gold as controlled semantic accents.
- Game artwork as the emotional material.
- Archive Glass only for navigation, grouping, and high-value actions.
- Opaque alternatives for reduced transparency and high contrast.

There are no visible rainbow or purple gradients. Ambient depth comes from solid blurred light pools on Android 12+ and static tonal shapes on older versions. Text and artwork are never blurred.

## Brand: Portal / Save Core

The supplied brand board’s mark is implemented as the canonical identity:

- an open return portal;
- an inner save core with two memory lights;
- three pixel steps representing re-entry into a world;
- a coral terminal pixel as the single warm signature.

Assets live in `branding/`, launcher resources, and `BrandUi.kt`. The mark avoids protected console shapes, Poké Balls, game characters, and letter-monogram clichés.

## Information architecture

### Home — return

- Dominant continue-playing hero with real local artwork when available.
- Recent and favorite worlds as an artwork shelf.
- Small archive statistics, never decorative analytics.
- Clear offline/private status.
- One primary action: resume the most relevant game.

### Library — organize

- Search by title, display name, code, creator, tag, or collection.
- Filters for all, continue, favorites, patched/homebrew, and unplayed.
- Adaptive artwork grid and detailed list modes.
- Import file and scan-folder actions remain immediately available.
- Empty state teaches `.gba`, `.zip`, `.ips`, `.ups`, and `.bps` workflows without implying bundled games.

### Discover — obtain legal content

- Official creator/project pages are primary.
- Patch projects are labeled as patches and require a user-owned compatible base ROM.
- Direct import appears only for HTTPS assets with explicit distribution permission and a published SHA-256 digest.
- Downloads are bounded, redirect-restricted, checksum verified, and imported through the existing secure pipeline.
- Commercial Pokémon/Nintendo ROMs and unauthorized sites are not indexed or deep-linked.

### You — identity without surveillance

- Local profile is always available and works offline.
- Optional Google identity never gates play.
- Played games, achievement progress, and save-vault summary use real local state.
- Privacy copy distinguishes public identity metadata from private ROM/save content.

### Settings — intention first

1. Appearance
2. Library
3. Gameplay
4. Controls
5. Feedback
6. Privacy

Settings use existing repository state, not duplicate UI-only values. On wide layouts, categories and controls appear in a two-pane arrangement.

### Game details — contextual tools

- Resume, favorite, title/notes, artwork, save states, patches, and cheats stay with the selected game.
- Cheat packs are matched to exact ROM SHA-256 and, when specified, game code and revision.
- A trusted `.rci` index can offer a one-tap pack only after license, permission, HTTPS, and pack checksum validation.
- Manual local `.rcc` import remains available.

### Onboarding — three decisions

1. **Return to worlds that raised you** — establish the product promise.
2. **Bring what you own** — explain file, folder, and patch import.
3. **Offline first** — make optional identity and privacy boundaries explicit.

The user can enter Retra without an account or network connection.

## Responsive behavior

- Phone: four-destination bottom navigation; Settings in the top bar.
- Medium/expanded: navigation rail with Settings anchored at the bottom.
- Settings becomes two-pane at approximately 760 dp.
- Library uses adaptive columns rather than model-specific breakpoints.
- Insets are consumed once and all primary targets remain at least 48 dp.

## Accessibility and performance

- Material typography respects stored font scale.
- High contrast, reduced motion, and reduced transparency are functional modes.
- Motion is short and non-essential; reduced motion removes it.
- Glass is decorative only and never carries the sole state signal.
- Ambient blur is clipped, static, and unavailable on pre-Android 12 devices by design.
- Lazy content uses stable keys; blocking storage/network work remains outside composables.

## Typography decision

No font binaries are bundled. Retra uses Android’s system sans-serif stack with deliberate weight, tracking, and hierarchy. This avoids licensing/size risk and keeps rendering native across devices. A separately licensed variable font can be introduced later through the normal Android resource pipeline after legal review and device testing.

## Content and legal boundary

Retra supports user-owned ROM imports, legal homebrew/public releases, official creator pages, checksum-pinned authorized files, and local patching with a compatible base ROM supplied by the user.

Retra does **not** bundle, scrape, index, deep-link, or automate unauthorized commercial ROM downloads from Poke Harbor or similar sites. That requested behavior was intentionally replaced with a secure, provenance-first library and patch workflow.
