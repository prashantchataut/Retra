# Retra app — design direction and tokens

```
Direction: Playful-soft "cute 2D under liquid glass" for GBA players (Pokémon fans first), optimized for getting back into a game in one tap.
Type: Fraunces Soft (display serif) / Nunito (body). Palette: tinted neutrals ("mint mist" light, "deep matcha" dark), ink-coloured primary pills in light and soft mint in dark; Mochi pink is reserved for Mochi and brand moments (+ success / warning / danger).
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

Why this palette: the old dark theme ("night indigo" with violet glows) read as generic generated UI.
Dark is now a green-black ground where depth comes from lighter glass steps, not coloured light; light
is a cool mint ground with deep pine ink. One accent per theme carries the actions, so Mochi and the
game art stay the colour on screen.

## Tokens (code: `app/src/main/kotlin/app/retra/ui/theme/Tokens.kt`)

### Colour — semantic layer (light / dark)
| Token | Light "mint mist" | Dark "deep matcha" | Use |
|---|---|---|---|
| `sky` | `#EEF5F2` | `#0C1412` | window background base |
| `skyGlowA/B/C` | `#C6EEDC` / `#CBE6F5` / `#FBE1E8` | `#10291F` / `#0D2329` / `#11201E` | ambient light pools (barely there in dark) |
| `glassFill` | white 66 % | `#1A2824` 70 % | glass surface tint |
| `glassFillStrong` | `#FBFEFC` 92 % | `#141F1C` 96 % | frosted fallback / sheets |
| `glassRim` | white 95 % → 12 % | white 16 % → 3 % | specular rim gradient |
| `ink` | `#14231E` | `#E8F1ED` | primary text (14.7 : 1 / 16.2 : 1) |
| `inkMuted` | `#52645C` | `#9EB1A9` | secondary text (5.7 : 1 / 8.3 : 1) |
| `accent` | `#17302A` | `#9BE8C9` | primary actions, active states |
| `onAccent` | `#FFFFFF` (14.1 : 1) | `#06201A` (12.0 : 1) | text on accent |
| `accentSoft` | `#D2F0E3` | `#173A30` | selected chips, icon wells |
| `brand` | `#E8628F` | `#FF9DBB` | Mochi pink: favourites, the logo; never text |
| `success` | `#1E7A50` | `#7FE0AE` | "You have it", verified |
| `warning` | `#8F5A0A` | `#F2C46E` | CRC mismatch, beta |
| `danger` | `#C0364A` | `#FF8F9C` | delete, errors |
| `shadow` | `#2E5A4C` @ 14 % | black @ 45 % | tinted elevation |

Catalog entries bring their own pastel `accent`, used only as a card wash, never for text.

### Type (7 sizes)
| Token | Font | Size / line | Weight |
|---|---|---|---|
| `display` | Fraunces | 34 / 38 | SemiBold 600 |
| `title` | Fraunces | 24 / 29 | SemiBold 600 |
| `headline` | Fraunces | 20 / 25 | Medium 500 |
| `bodyStrong` | Nunito | 16 / 22 | ExtraBold 800 |
| `body` | Nunito | 16 / 23 | Regular 400 |
| `label` | Nunito | 14 / 18 | SemiBold 600 |
| `caption` | Nunito | 12 / 16 | SemiBold 600 |

Fraunces ships as two static instances cut from the variable font (SOFT 100, WONK 0; opsz 36 for
SemiBold, 24 for Medium) so the serif stays round and friendly.

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
* **Dock** — floating 68 dp glass pill, 5 tabs, a solid accent capsule as the liquid indicator (leading edge `snappy`, trailing edge `smooth`, so it stretches then settles). Its sheen is part of the capsule gradient; no separate highlight bar.
* **Buttons** — pill; primary = accent fill; secondary = glass; tertiary = text. ≥ 48 dp tall; squish on press; disabled = 38 % alpha.
* **GameCover** — bundled art (Retra Originals, `assets/covers/<catalog id>.png`) → box art fetched from libretro-thumbnails for imported cartridges (shown whole over a soft wash) → last screenshot → generated cover (pastel stripes/dots seeded by CRC32 + title + game code).
* **Logo** — glossy "clay" Mochi (form shading, bounce light, softer outline) on a mint glass tile; the launcher icon is generated from the same geometry by `tools/brand/make_launcher_icon.py`.
* **Sheets** — in-window glass sheet with drag handle, scrim 32 %, back gesture closes.
* **Empty / error / loading** — always Mochi + one sentence + one action. No spinners except the patch progress ring.

## Accessibility
Text ≥ 4.5 : 1 against glass-on-sky, touch targets ≥ 48 dp, every icon-only button has a TalkBack
label, game controls expose a single "Game controls" node, reduced motion honoured everywhere.

## Never
Emoji as icons; purple→blue gradients; neon glows; gradient text; random blobs; cards in cards;
bounce on everything; fake numbers; lorem ipsum; offering commercial ROM downloads.
