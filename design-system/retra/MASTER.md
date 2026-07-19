# Retra — Master Design System

## Design read
A premium, privacy-first Game Boy Advance player: emotionally nostalgic, technically credible, controller-first, and modern rather than faux-retro. Surfaces stay graphite/off-white with a single indigo accent.

## Dials
- Variance: 6/10 — shelf composition with restrained hero regions; never chaotic.
- Motion: 4/10 — short continuity; gameplay controls stay immediate; reduced-motion removes choreography.
- Density: 6/10 — compact metadata and settings, comfortable 48dp touch surfaces.

## Tokens
- 4/8dp spacing grid; page tiers 16/24/32dp.
- Touch targets: 48dp minimum; icon glyphs typically 20–24dp.
- Shape scale: 8/12/18/24/32dp; panels max ~24–28dp on phones.
- Elevation: tonal first; shadow only where separation requires it.
- Motion: 150ms press, 220ms local transition, 300ms navigation.
- Typography: Material roles, 16sp body baseline, tabular/monospace numerals for hashes, timers, codes, and diagnostics.

## Color
- Brand/action: Retra Indigo `#5B54E8`
- Surfaces: Void black / graphite dark, soft cloud / white light
- Support: soft violet, save mint, adventure gold, error coral
- Status always includes text or icon, never color alone
- Dynamic color is opt-in and does not erase semantic success/warning/error roles

## Composition
- Compact: 4-item labeled bottom navigation — Home, Library, Discover, Settings
- Medium: navigation rail
- Expanded: rail with list-detail panes and ~900dp content width cap
- Vault, community, patches, cheats, and account live in Settings or game details — not as permanent destinations
- One primary CTA per screen

## Product-specific rules
- Never imply commercial ROM downloads or bundle commercial ROMs/patches
- Heart & Soul CRC metadata may guide patch apply; users supply their own clean Emerald base and UPS
- Online catalog entries expose creator, license, permission, checksum, and source domain before download
- EXTERNAL catalog entries open the official creator page; one-tap download requires a valid SHA-256
- Cheat activation always surfaces ROM match, conflicts, risk, save-backup state, and achievement eligibility impact
- Deleting a library entry removes only Retra-managed copies, never the user’s original SAF file

## 0.7 brand notes
- Canonical mark: forward-leaning white **R** with one indigo bowl highlight on a near-black cartridge plate
- Monochrome adaptive icon uses the solid white R silhouette
- Splash and launcher reuse the same mark family
- Onboarding is three focused screens; Google identity remains optional and never gates offline play
- Glass intensity defaults quieter; reduce-transparency falls back to opaque surfaces
