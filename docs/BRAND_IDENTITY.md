# Retra Brand Identity — Portal / Save Core

## Mark

The canonical Retra mark comes from the supplied brand board and is rebuilt as an original vector/Compose asset:

- **Portal:** a thick open loop expressing return and replay.
- **Save core:** the rounded inner square representing preserved progress.
- **Memory lights:** two small dark apertures that keep the mark friendly without becoming a mascot.
- **Pixel step:** three rising blocks that interrupt the loop and imply digital re-entry.
- **Coral terminal:** one warm pixel used as the recognizable signature.

It is intentionally not a letter R, console, cartridge, Poké Ball, or game-specific symbol.

## Canonical assets

- `branding/retra-logo.svg` — source mark.
- `branding/retra-logo.png` — raster presentation asset.
- `app/src/main/res/drawable-nodpi/retra_logo.png` — in-app image.
- `app/src/main/res/drawable/ic_retra_foreground.xml` — adaptive launcher foreground.
- `app/src/main/res/drawable/ic_retra_monochrome.xml` — themed icon.
- `app/src/main/res/mipmap-*` — launcher fallbacks.
- `app/src/main/kotlin/app/retra/emulator/BrandUi.kt` — scalable Compose implementation.

## Color roles

- Void Black `#050A0D` — launcher and OLED anchor.
- Night Navy `#091219` — app background.
- Graphite `#101A20` — opaque technical surfaces.
- Retra Ice `#75D7F2` — primary interaction.
- Frost `#B7ECF8` — edge highlight and quiet emphasis.
- Memory Aqua `#54C9C0` — connection/continuity.
- Memory Coral `#FF806F` — terminal pixel, destructive caution, human warmth.
- Save Mint `#63D6A2` — save integrity and success.
- Adventure Gold `#E8BE69` — achievements and rare emphasis.

Purple and rainbow gradients are not part of the brand.

## Application rules

- Preserve the mark’s open portal and three-pixel interruption at all sizes.
- Use the full-color mark on dark surfaces; use the monochrome silhouette for themed icons.
- Keep clear space equal to at least one inner-eye width.
- Do not place the mark over detailed game artwork without an opaque tile.
- Do not recolor it with multicolor gradients.
- Do not bundle or redistribute font files as brand assets.
