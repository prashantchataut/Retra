# Retra app — design direction and tokens

```
Direction: Playful-soft "cute 2D under liquid glass" for GBA players (Pokémon fans first), optimized for getting back into a game in one tap.
Type: Fredoka (display, rounded) / Nunito (body). Palette: tinted neutrals ("cloud cream" light, "night indigo" dark) + Mochi pink accent (+ success / warning / danger).
Signature: Mochi, the squishy rice-cake mascot, living on a pastel sky, with glossy glass surfaces (specular top rim) and a floating glass dock whose indicator stretches like a liquid drop between tabs.
Density: balanced (airy on Home, denser in My Games / Settings).  Motion: subtle springs, one orchestrated moment per screen (staggered entrance), squish on press.
```

## Atmosphere
A calm, pastel sky that drifts very slowly behind everything (three soft light pools, 40 s loop, static
with reduced motion). Content sits on glass: translucent fill, a top-lit sheen, a 1 dp specular rim that
fades from bright at the top to nearly nothing at the bottom, and a soft shadow tinted toward the
background hue. Real backdrop blur (Haze) is only used where content actually scrolls under a surface
(dock, sticky headers, sheets, player menu) — blurring a smooth gradient is invisible, so cards are
cheap translucent glass. Below API 31 blur degrades to a denser frosted tint.

Mochi carries the emotion: happy on Home, sleepy in empty states, surprised on errors, bouncing while
loading. Everything else stays quiet so Mochi and the game art can be the colour.

## Tokens (code: `app/src/main/kotlin/app/retra/ui/theme/Tokens.kt`)

### Colour — semantic layer (light / dark)
| Token | Light "cloud cream" | Dark "night indigo" | Use |
|---|---|---|---|
| `sky` | `#FBF5F1` | `#15132B` | window background base |
| `skyGlowA/B/C` | `#FFD3E1` / `#FFE6C7` / `#D7E4FF` | `#3B2C66` / `#2A3A6E` / `#4A2A55` | ambient light pools |
| `glassFill` | white 62 % | `#2C2950` 62 % | glass surface tint |
| `glassFillStrong` | white 86 % | `#2C2950` 90 % | frosted fallback / sheets |
| `glassRim` | white 95 % → 10 % | white 32 % → 4 % | specular rim gradient |
| `ink` | `#2B2130` | `#F4EFFB` | primary text (14.3 : 1 / 16.0 : 1) |
| `inkMuted` | `#6B5E70` | `#B9B0CE` | secondary text (5.6 : 1 / 8.8 : 1) |
| `accent` | `#C7366B` | `#FF8FB4` | Mochi pink: primary actions, active states |
| `onAccent` | `#FFFFFF` (5.0 : 1) | `#2A0E1D` (8.4 : 1) | text on accent |
| `accentSoft` | `#FFE1EA` | `#4A2846` | selected chips, soft containers |
| `success` | `#23794F` | `#7FE0AE` | "You have it", verified |
| `warning` | `#965C0A` | `#F6C56E` | CRC mismatch, beta |
| `danger` | `#C4364A` | `#FF8F9C` | delete, errors |
| `shadow` | `#7A4A60` @ 14 % | black @ 40 % | tinted elevation |

Catalog entries bring their own pastel `accent`, used only as a card wash, never for text.

### Type (7 sizes)
| Token | Font | Size / line | Weight |
|---|---|---|---|
| `display` | Fredoka | 32 / 38 | SemiBold 600 |
| `title` | Fredoka | 24 / 30 | SemiBold 600 |
| `headline` | Fredoka | 20 / 26 | Medium 500 |
| `bodyStrong` | Nunito | 16 / 22 | ExtraBold 800 |
| `body` | Nunito | 16 / 23 | Regular 400 |
| `label` | Nunito | 14 / 18 | SemiBold 600 |
| `caption` | Nunito | 12 / 16 | SemiBold 600 |

### Space (4 dp grid)
`xxs 4 · xs 8 · s 12 · m 16 · l 24 · xl 32 · xxl 48`. Screen gutter 20 dp (m + xxs).

### Radius (one family, continuous rounding)
`small 12` (chips, inputs, thumbnails) · `medium 20` (cards) · `large 28` (hero, sheets, dock) · `pill` (buttons).
Nested radius = outer − padding.

### Elevation (3 levels)
`raised` 6 dp (cards) · `floating` 16 dp (dock, FAB-like buttons) · `overlay` 28 dp (sheets, dialogs). Shadow colour = `shadow` token; in dark the glass gets lighter instead of darker shadows.

### Motion (one spring family, damping 0.82)
`snappy` stiffness 1400 (press feedback) · `smooth` 500 (indicator, chips, layout) · `gentle` 200 (screen-level, sheets).
Press = scale 0.96 on `snappy`. List entrance = fade + 16 dp rise, 40 ms stagger, max 8 items staggered.
Idle loops (sky drift 40 s, Mochi breathing 2.4 s) stop entirely with reduced motion (system animator
scale 0 or the in-app toggle); springs become snaps.

## Components
* **GlassSurface** — fill + sheen + rim + tinted shadow; `blur = true` only for surfaces over scrolling content.
* **Dock** — floating 64 dp glass pill, 5 tabs, liquid indicator (leading edge `snappy`, trailing edge `smooth`, so it stretches then settles).
* **Buttons** — pill; primary = accent fill; secondary = glass; tertiary = text. ≥ 48 dp tall; squish on press; disabled = 38 % alpha.
* **GameCover** — bundled art → last screenshot → generated cover (pastel stripes/dots seeded by CRC32 + title + game code).
* **Sheets** — in-window glass sheet with drag handle, scrim 32 %, back gesture closes.
* **Empty / error / loading** — always Mochi + one sentence + one action. No spinners except the patch progress ring.

## Accessibility
Text ≥ 4.5 : 1 against glass-on-sky, touch targets ≥ 48 dp, every icon-only button has a TalkBack
label, game controls expose a single "Game controls" node, reduced motion honoured everywhere.

## Never
Emoji as icons; purple→blue gradients; neon glows; gradient text; random blobs; cards in cards;
bounce on everything; fake numbers; lorem ipsum; offering commercial ROM downloads.
