# Retra Final UI/UX Product Specification

## 1. Product Vision
Retra is a beloved handheld memory from childhood rebuilt as a premium, tactile modern object. It balances nostalgia, playfulness, charm, tactile responsiveness, and sleek, calm design without generic AI clichés, neon gradients, or card soup.

## 2. Brand Identity: Retra Sprite
- **Concept**: A living memory-creature crafted from liquid glass that floats from nostalgic game worlds.
- **Silhouette**: An asymmetrical, rounded organic silhouette with an iconic top-right crown lobe and expressive glossy eyes.
- **Materiality**: Translucent internal core gradient, subtle refractive rim light, specular top highlight, and warm cheek glows.
- **States & Expressions**:
  - `IDLE`: Gentle breathing and responsive spring physics on touch/drag.
  - `IMPORTING` / `ANALYZING`: Inquisitive scanning eye motion.
  - `EMPTY`: Curious wide-eyed stance inviting game discovery.
  - `SUCCESS`: Joyful squint and vibrant cheek glow.
  - `ERROR`: Empathetic, calm expression.
  - `PATCH_READY`: Dual-color aura illustrating base ROM + patch pairing.

## 3. Color & Material System
- **Base Surfaces**:
  - `VoidBlack` (`#09070D`): Near-black aubergine foundational canvas.
  - `NightPlum` (`#13101A`): Deep plum-charcoal surface layer.
  - `SurfaceMidnight` (`#1B1624`): Elevated glass container base.
- **Accent Family**:
  - `ElectricLilac` (`#C7ACFC`): Primary brand highlight.
  - `RaspberryPink` (`#FF6B8B`): Warm expressive accents.
  - `SaveMint` (`#5EEAD4`): Verification, healthy states, and demo homebrew.
  - `MemoryCoral` (`#FFFF9376`): Patches, favorites, and actions.
  - `AdventureGold` (`#FFD166`): Milestones and diagnostics.
  - `WarmCream` (`#FFF4E0`): Nostalgic moments.
- **Liquid Glass Tiers**:
  - `GlassTier.THIN`: Subtle translucent layer for filter chips and secondary panels.
  - `GlassTier.REGULAR`: Standard surface with rim lighting and ambient blur.
  - `GlassTier.ELEVATED`: Hero feature cards and prominent sheets.
  - `GlassTier.STRONG`: Floating navigation dock and modal action sheets.
  - `GlassTier.OPAQUE`: 100% solid high-contrast accessibility fallback.

## 4. Navigation Architecture
- **Primary Destinations**:
  - `HOME`: Clean top identity bar, prominent asymmetric "Continue" hero card, recently played shelf, quick feature banners.
  - `LIBRARY`: Visual-first 2-column poster grid, floating glass search field, responsive filter chips (All, Played, Favorites, Patched, Homebrew, Unplayed), grid/list toggle.
  - `ADD (+)`: Elevated action in the floating dock opening the liquid-glass Add Action Sheet (Import Game, Scan Folder, Apply Patch Studio, Play Retra Drift Demo, Restore Backup).
  - `PROFILE`: Local-first player profile, Google identity linking, save health summary, milestone tracker.
- **Form Factors**:
  - Phone: Floating liquid-glass dock centered above system navigation insets.
  - Tablet / Foldable / Landscape: Intelligently docked adaptive glass rail.

## 5. 5-Screen Interactive Onboarding
1. **Emotional Introduction**: Large lowercase wordmark, Retra Sprite with spring drag physics, clear mission message.
2. **Your Games, Your Library**: Floating interactive game cards with parallax drag, local privacy ownership message.
3. **ROM Hacks & Patches Made Simple**: Animated visual metaphor of Base ROM + Patch pairing producing a new game copy.
4. **Make The Handheld Yours**: Live interactive controller preview reacting to touch, instant theme palette switcher.
5. **Identity & Sign-In**: Retra Sprite culmination, prominent "Continue with Google", unmistakable "Use Retra offline" option.

## 6. Player Chrome & Emulation
- Edge-to-edge game canvas with immersive mode support.
- Liquid-glass touch controls with live opacity, scale, spacing, and layout customization.
- 1-tap screen rotation toggle button between portrait and landscape.
- Prioritized session pause overlay: Resume, Save State, Load State, Fast-Forward, Rewind, Screenshot, Settings, Exit.
