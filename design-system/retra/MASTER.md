# Retra Prism — Master Design System

## Design read
A premium retro-gaming operating system for enthusiasts: emotionally nostalgic, technically credible, controller-first, and modern rather than faux-retro.

## Dials
- Variance: 7/10 — asymmetric editorial hero regions and varied shelf composition, never chaotic.
- Motion: 5/10 — short spatial continuity and clear state transitions; gameplay controls stay immediate.
- Density: 6/10 — compact metadata and settings, comfortable touch surfaces.

## Tokens
- 4/8dp spacing grid; page tiers 16/24/32dp.
- Touch targets: 48dp minimum; icon glyphs typically 20–24dp.
- Shape scale: 8/12/18/24/32dp; prominent glass panels max 24dp on phones.
- Elevation: tonal first; shadow only where separation requires it.
- Motion: 150ms press, 220ms local transition, 300ms navigation; reduced-motion removes translation/scale choreography.
- Typography: Material roles, 16sp body baseline, tabular/monospace numerals for hashes, timers, codes, and diagnostics.

## Color
Retra Prism is token-driven. Indigo is brand/action, cyan is technical/live, violet is memory/collection, mint is safe/success, gold is achievement. Status always includes text or icon, never color alone. Dynamic color is opt-in and does not erase semantic success/warning/error roles.

## Composition
- Compact: 5-item labeled bottom navigation.
- Medium: navigation rail.
- Expanded: rail/drawer with list-detail panes.
- One primary CTA per screen.
- Reusable composites: GameCard, StatusPill, MetricCard, CatalogSourceCard, CheatCard, AchievementCard, MultiplayerRoomCard, SettingRow.
- Avoid repetitive equal cards: use hero + shelves + supporting tiles.

## Product-specific rules
- Never imply commercial ROM downloads.
- Online catalog entries expose creator, license, permission, checksum, and source domain before download.
- Cheat activation always surfaces ROM match, conflicts, risk, save-backup state, and achievement eligibility impact.
- Multiplayer status distinguishes local architecture, signaling, transport, core-link support, and verified gameplay.
- Social sharing defaults to privacy-safe metadata; never attach ROM/save contents.

## 0.5 brand/application notes
- Canonical mark: rounded cartridge-like field, abstract R, prism trail, and memory sparkle.
- Onboarding is a narrative sequence, not a permission dump.
- The You destination combines identity with settings so account status never displaces gameplay/library tabs.
- Game cards accept bounded custom covers; deterministic token-based artwork remains the fallback.
- Player chrome yields to the 240×160 frame, hides system bars, and keeps advanced actions inside one scrollable session sheet.
- Google sign-in uses neutral explicit wording and a generic login glyph rather than a counterfeit Google logo.
## 0.6.1 install + brand notes

- Release APKs are signed with the Android debug keystore so sideload installs succeed on phones.
- Launcher mark: prism R with indigo bowl, cyan leg, gold spark on near-black cartridge plate.
- Bottom navigation always shows labels; destinations keep a 48dp minimum touch height.
- Home hero and empty library lead with brand mark + one primary import CTA.

