---
name: Retra
description: Privacy-first GBA memory player — Archive Glass on Android
colors:
  void-black: "#050A0D"
  night-navy: "#091219"
  graphite: "#101A20"
  ink-blue: "#16262F"
  retra-ice: "#75D7F2"
  frost: "#B7ECF8"
  memory-aqua: "#66E1D1"
  memory-coral: "#FF806F"
  save-mint: "#62D99E"
  adventure-gold: "#FFC65C"
  error-coral: "#FF6F78"
  cloud-white: "#F7FBFC"
  soft-cloud: "#F1F5F6"
  deep-ink: "#10191D"
typography:
  display:
    fontFamily: "sans-serif"
    fontWeight: 900
    fontSize: "40sp"
  headline:
    fontFamily: "sans-serif"
    fontWeight: 800
    fontSize: "31sp"
  title:
    fontFamily: "sans-serif"
    fontWeight: 700
    fontSize: "20sp"
  body:
    fontFamily: "sans-serif"
    fontWeight: 400
    fontSize: "16sp"
  label:
    fontFamily: "sans-serif"
    fontWeight: 700
    fontSize: "14sp"
rounded:
  extraSmall: "8dp"
  small: "12dp"
  medium: "18dp"
  large: "24dp"
  extraLarge: "30dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "20dp"
  xl: "22dp"
components:
  panel:
    backgroundColor: "{colors.night-navy}"
    textColor: "{colors.cloud-white}"
    rounded: "{rounded.large}"
  badge:
    rounded: "999dp"
  button-primary:
    backgroundColor: "{colors.retra-ice}"
    textColor: "#002832"
    rounded: "{rounded.medium}"
---

# Design

## Overview

Retra uses **Archive Glass**: mineral-dark surfaces, restrained ice/aqua/coral semantics, and the Vault Aperture / Memory Prism mark. The product is an Operate surface — return to play — not a marketing manifesto or ROM storefront. Artwork and the game canvas lead; chrome recedes. Purple, neon glow, and rainbow gaming gradients are out of brand.

Platform: Android / Jetpack Compose / Material 3. Tokens live in `app/.../ui/theme/RetraTheme.kt`. Shared primitives live in `app/.../ui/components/`.

## Colors

Dark mode is the primary cinematic scene (OLED evenings, handheld in hand). Light mode is soft cloud, not sterile gray.

| Role | Token | Use |
|------|-------|-----|
| Background | void-black / night-navy | App canvas / surfaces |
| Primary | retra-ice | Primary actions, active nav |
| Continuity | memory-aqua | Provenance, homebrew, calm success cues |
| Caution / spark | memory-coral | Destructive caution, patch emphasis |
| Integrity | save-mint | Saves healthy, verified install |
| Milestone | adventure-gold | Achievements, rare emphasis |
| Error | error-coral | Errors only |

Dynamic Material color must stay **off by default** so brand ice is not replaced by system purple/teal noise.

## Typography

System sans-serif scaled by `fontScale`. Hierarchy uses weight + size, not uppercase eyebrows on every screen. Prefer `headlineMedium` / `titleLarge` for screen titles; reserve `FontWeight.Black` for hero game titles, not every section.

Body copy stays plain and short. Technical hashes (CRC32, SHA-256) use body/label styles only inside details panels.

## Layout

- Phone: bottom navigation (Home, Library, Discover, You).
- ≥760 dp width: navigation rail with logo.
- Page padding: 20 horizontal / 22 vertical.
- Home order: Continue hero → recent row → optional compact stats → one muted privacy line.
- Library: artwork grid first; search and filters progressive; import as header action.
- Player: game canvas primary; chrome on demand.

## Elevation & Depth

Tonal layering over heavy shadows. Panels use translucent surface fill (opacity from glass intensity) plus a 1 dp outline. Ambient backdrop blobs are decoration only — never under text. `reduceTransparency` forces opaque panels and removes ambient light.

## Shapes

Only MaterialTheme shapes: extraSmall 8, small 12, medium 18, large 24, extraLarge 30 (scaled by `cornerScale`). Product UI must not invent additional radii. Pills use full round for badges/chips only.

## Components

- **RetraBackdrop** — mineral background + optional ambient pools
- **RetraPanel** — standard glass panel (`shapes.large` default)
- **RetraPageTitle** — title + optional one-line subtitle + optional action; no essay body, no mandatory eyebrow
- **RetraPosterCard** — artwork-first game tile
- **RetraBadge** — single status pill recipe
- **RetraEmptyState** — mark + title + short help + primary action
- Material buttons, fields, chips for controls; brand colors via theme

## Do's and Don'ts

**Do**

- Lead with Continue / Play when a game exists
- Show core readiness only when it affects launch
- Keep Discover honest (no fake “requirement met” checkmarks)
- Use artwork as the emotional foreground
- Honor reduce motion / transparency / high contrast / 48 dp targets

**Don't**

- Ship parallel product shells or versioned UI dialects (no V1/V2/V3 naming)
- Put manifesto copy above the primary action
- Hardcode `RoundedCornerShape(N.dp)` outside the component kit
- Center one fan-patch project as product identity
- Use purple, neon outer glow, or equal-weight quick-action card strips on Home
- Lie about system status
