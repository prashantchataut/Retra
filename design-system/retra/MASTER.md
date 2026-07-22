# Retra — Archive Glass Design System

## Product read

Retra is a private memory archive for Game Boy Advance play, not a storefront or file manager. The UI should feel final, quiet, and tactile: artwork first, reliable controls second, technical detail on demand.

## Design dials

- Variance: 6/10 — expressive heroes and artwork shelves, calm settings and diagnostics.
- Motion: 3/10 — 150–220 ms fades and shared emphasis; reduced-motion becomes immediate.
- Density: 6/10 — browsable library with 48 dp minimum targets and no compressed microcopy.

## Core tokens

- Spacing: 4 dp base; page padding 16 / 20 / 24 dp; section gaps 24–32 dp.
- Shape: 10 / 14 / 18 / 24 dp, scaled by the user corner preference.
- Elevation: tonal separation plus a 1 dp inner/outer edge; restrained shadow only where needed.
- Typography: system sans-serif Material roles, strong weight contrast, no bundled font files.
- Motion: 210 ms content crossfade; never animate ambient decoration continuously.

## Color

- Void Black `#050A0D`
- Night Navy `#091219`
- Graphite `#101A20`
- Ink Blue `#16262F`
- Retra Ice `#75D7F2`
- Frost `#B7ECF8`
- Memory Aqua `#54C9C0`
- Memory Coral `#FF806F`
- Save Mint `#63D6A2`
- Adventure Gold `#E8BE69`

Purple, neon-violet, rainbow gradients, and generic gaming-dashboard chrome are excluded. Status always includes text or iconography, not color alone.

## Archive Glass

- Translucent tonal panels with a quiet edge highlight.
- Ambient light pools are solid blurred shapes, not visible gradients.
- Blur is decorative and Android 12+ only; content is never blurred.
- Reduced transparency makes panels fully opaque and removes ambient effects.
- High contrast increases panel opacity and edge definition.

## Composition

- Compact: Home, Library, Discover, You in bottom navigation; Settings opens from the top bar.
- Medium+: navigation rail with Settings anchored at the bottom.
- Settings becomes two-pane at about 760 dp.
- Library uses adaptive columns and supports grid/list choice.
- Home gives one dominant “continue” action; details and utilities remain secondary.

## Brand mark

The canonical mark is the **Portal / Save Core**: an open rounded return loop, an inner save core with two memory lights, and three ascending pixel steps ending in coral. It is not a letter monogram, console silhouette, cartridge, Poké Ball, or copyrighted game symbol.

## Product rules

- No commercial ROMs, proprietary BIOS files, or unauthorized indexes.
- Official project pages are preferred over direct downloads.
- Direct download requires HTTPS, explicit redistribution permission, bounded size, and SHA-256.
- Patch projects require a user-supplied compatible base ROM.
- One-tap cheat packs require a trusted `.rci` index, exact ROM identity, HTTPS, checksum, license, and explicit permission.
- Deleting a library entry removes only Retra-managed copies, never the original SAF document.
- A release without packaged mGBA for each declared ABI must not claim gameplay readiness.
