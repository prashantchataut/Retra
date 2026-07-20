# Retra — Master Design System

## Design read
A premium, privacy-first Game Boy Advance player. Surfaces stay graphite / off-white with one indigo accent. No decorative glass, aurora fills, oversized radii, or multi-color chrome.

## Dials
- Variance: 4/10 — calm product layout with one clear hero action.
- Motion: 3/10 — short 150–220ms fades; reduced-motion cuts to instant.
- Density: 6/10 — comfortable library browsing with 48dp touch targets.

## Tokens
- Spacing: 4dp base; page padding 16/20/24dp; group gaps 8–12dp; section gaps 24dp.
- Touch targets: 48dp minimum.
- Shape: 6 / 8 / 12 / 16dp. Cards and panels stay at or under 16dp.
- Elevation: flat surfaces + 1dp outlineVariant borders. No decorative shadows.
- Typography: Material roles only. One sans family.

## Color
- Brand/action: Retra Indigo `#675CF5`
- Surfaces: Void black / graphite dark, soft cloud / white light
- Semantic only: save mint, adventure gold, error coral
- Status always includes text or icon, never color alone
- Dynamic color remains opt-in

## Composition
- Compact: Material bottom navigation — Home, Library, Discover, Settings
- Medium+: Material navigation rail from 600dp
- Content width expands to ~1200dp on tablets
- Vault, community, and advanced tools live under Settings or game details
- One primary CTA per screen

## Brand mark
- Canonical mark: interlocking indigo forms with a negative-space handheld / cartridge silhouette
- Delivered as a PNG launcher asset (`retra_logo`) plus a monochrome adaptive silhouette
- Do not use letter-R monograms or geometric glyph experiments as the primary brand

## Product-specific rules
- Never imply commercial ROM downloads or bundle commercial ROMs/patches
- Heart & Soul CRC metadata may guide patch apply; users supply their own clean Emerald base and UPS
- Discover shows official creator pages first; one-tap download requires a published SHA-256
- After browser download, users can Open with Retra via ACTION_VIEW / ACTION_SEND
- Deleting a library entry removes only Retra-managed copies, never the user’s original SAF file
- Releases must package mGBA for arm64-v8a, armeabi-v7a, and x86_64 or fail the workflow
