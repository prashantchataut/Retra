# Retra Product Design Specification

## 1. Absolute Product Principle
"A beloved handheld memory from childhood rebuilt as a premium modern object."

The emotional experience balances nostalgia, playfulness, charm, tactility, high polish, and a calm, clean aesthetic without copying hardware trade dress or third-party assets.

## 2. Brand Identity: Retra Sprite
The mascot and symbol of Retra is the **Retra Sprite**, a liquid-glass memory creature that embodies the spirit of classic handheld gaming.
- **Asymmetric organic silhouette**: Distinctive top-right lobe/crest recognizable in solid monochrome.
- **Glossy liquid depth**: Internal refractive lighting core and subtle specular highlights.
- **Dynamic expressions**: Interactive spring physics responding to touch and drag gestures; dedicated states for loading, importing, empty archive, errors, and success.

## 3. Color & Material System
- **Foundational Surfaces**:
  - `VoidBlack` (`#09070D`): Deep near-black aubergine.
  - `NightPlum` (`#13101A`): Rich plum-charcoal surface base.
  - `SurfaceMidnight` (`#1B1624`): Elevated glass container.
- **Accent Family**:
  - `ElectricLilac` (`#C7ACFC`): Primary brand highlight.
  - `RaspberryPink` (`#FF6B8B`): Vibrant secondary accent.
  - `SaveMint` (`#5EEAD4`): Verification, healthy saves, and demo homebrew.
  - `MemoryCoral` (`#FFFF9376`): Patches, favorites, and actions.
  - `AdventureGold` (`#FFD166`): Diagnostics and milestones.
- **Liquid Glass Materials**:
  - `GlassTier.THIN`: Light translucent chips.
  - `GlassTier.REGULAR`: Standard panels with specular rim lighting.
  - `GlassTier.ELEVATED`: Feature cards and interactive previews.
  - `GlassTier.STRONG`: Floating navigation dock and bottom modal sheets.
  - `GlassTier.OPAQUE`: 100% solid accessibility fallback.

## 4. Navigation System
- **Floating Bottom Dock**: Suspended above system gesture insets with animated selection and tactile haptics.
- **Primary Destinations**:
  - `Home`: Continue game hero card, recently played shelf, quick feature shortcuts.
  - `Library`: Poster grid, search input, filter chips, list view toggle.
  - `Add (+)`: Bottom sheet for file import, folder scanning, Patch Studio, and Retra Drift demo.
  - `Profile`: Local-first profile with optional Google identity connection.
- **Adaptive Rail**: Automatically docked glass rail on tablets, foldables, and landscape mode.

## 5. Developer & Attribution
- Developer: **Prashant Chataut**
- Website: [knowprashant.vercel.app](https://knowprashant.vercel.app)
- GitHub: [github.com/prashantchataut](https://github.com/prashantchataut)
- Retra Repo: [github.com/prashantchataut/Retra](https://github.com/prashantchataut/Retra)
- Emulator Engine: mGBA Libretro Core (GPL v3)
