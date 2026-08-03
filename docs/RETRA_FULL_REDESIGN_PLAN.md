# Retra Full Redesign Plan

**Locked assumptions (user said continue without answering):** keep Archive Glass brand; phased delivery, not big-bang.

**Mode:** Operate (task UI). Brand lives in disciplined details; return-to-play outranks expression.

**Craft stack:** impeccable Operate · retra-product-redesign · design-guide · mobile-android-design · compose-styles · android-jetpack-compose (skills.sh)

---

## Condition (critical)

| Area | Score | Finding |
|------|-------|---------|
| Aesthetic / minimal | 1/4 | Manifesto headers, equal cards, badge/chip noise |
| Consistency | 1/4 | V3 + dead V23 + Player + Tools dialects |
| Recognition | 2/4 | Artwork helps; CRC/jargon and filters overload |
| System status | 2/4 | Core readiness good; fake Discover checkmarks bad |

**Root cause:** stacked rewrite debt sold as a finished redesign. Docs claim V23 removed; [`RetraV23Ui.kt`](app/src/main/kotlin/app/retra/emulator/RetraV23Ui.kt) remains. Glass is alpha Surfaces. Theme shapes unused. Copy argues with Play.

---

## Direction contract

- **THESIS:** Continue is the product. Refuse admin-dashboard and manifesto-first shells.
- **OWN-WORLD:** Archive Glass — mineral dark, ice primary, aqua/coral/mint/gold roles, Vault Aperture mark, 5 shape tokens only.
- **STORY:** User opens app, sees their game, taps Continue, plays. Import/patch/settings are secondary and honest.
- **FIRST VIEWPORT (Home):** Artwork hero + Continue. One short status line if core missing. No essay header. No equal quick-action strip.
- **FORM:** Operate Material 3 grammar inside Archive Glass materials; chrome quieter than artwork and game canvas.

---

## Anti-goals

- Do not invent a new brand palette or mark
- Do not rewrite ViewModel / repositories / emulation for visuals
- Do not add social/feed features
- Do not center Heart & Soul or any one fan patch as product identity
- Do not ship purple, neon glow, or radius lottery

---

## Phase 0 — Authority (docs)

1. `PRODUCT.md` written (done).
2. Write `DESIGN.md` from Archive Glass + brand identity + theme tokens (document, do not invent).
3. Align docs that falsely claim V23 deletion.

**Exit:** designers and agents share one visual law.

---

## Phase 1 — Demolition + kit

**Delete / quarantine**

- Delete [`RetraV23Ui.kt`](app/src/main/kotlin/app/retra/emulator/RetraV23Ui.kt) (dead shell).
- Keep [`RetraV23ToolsUi.kt`](app/src/main/kotlin/app/retra/emulator/RetraV23ToolsUi.kt) temporarily; restyle in Phase 3 or rename to `AdvancedToolsUi.kt`.
- Remove unused `GlassPill` or promote it as the only badge recipe.

**One component kit** (new package `ui/components/` preferred over growing monoliths)

| Primitive | Rule |
|-----------|------|
| `RetraBackdrop` | Ambient blobs only when transparency allowed |
| `RetraPanel` | Replaces ad-hoc GlassPanel clones; uses `MaterialTheme.shapes` |
| `RetraNav` | Phone bottom bar / wide rail; M3 patterns, brand colors |
| `RetraPageTitle` | Title only; optional one-line subtitle; **no eyebrow essays** |
| `RetraPosterCard` | Artwork-first game tile |
| `RetraBadge` | Single recipe for status chips |
| `RetraEmptyState` | Actionable empty (import / scan / restore Drift) |

**Theme law**

- Ban hardcoded `RoundedCornerShape(N.dp)` outside the kit; use `extraSmall…extraLarge`.
- Dynamic color **off by default** or brand-locked.
- Clean accent enum naming (`RETRA_INDIGO` → ice aliases documented as legacy only).

**Exit:** one shell, one panel, one nav, theme shapes actually used.

---

## Phase 2 — Primary Operate shell

Split [`RetraV3Ui.kt`](app/src/main/kotlin/app/retra/emulator/RetraV3Ui.kt) (~1341 lines) into focused screens:

```
ui/
  shell/RetraShell.kt
  home/HomeScreen.kt
  library/LibraryScreen.kt
  components/...
```

### Home

1. Continue hero (or empty import CTA)
2. Core readiness only when not playable
3. Recent / favorites row
4. Compact local snapshot
5. Privacy as one muted line max, not a sermon card

Cut: greeting essays, equal Import/Patch/Library quick-action cards, redundant stats walls.

### Library

1. Artwork grid first
2. Search collapsed or secondary until list non-empty
3. Filters progressive (All + overflow), not six equal chips always visible
4. Import / scan as FAB or sticky secondary, not above empty content forever

**Exit:** Home/Library pass Operate cognitive-load checklist (≤1 failure).

---

## Phase 3 — Secondary shell

### Discover

- Drift + short Patch Studio entry + creator hub
- Remove always-`true` Heart & Soul requirement theater
- Honest empty/error for validation failures

### Profile

- Local identity, progress, save health, achievements
- Cut hollow manifesto (“Quiet progress, not a feed”)

### Settings

- Intent groups: Appearance, Player, Controls, Saves, Privacy, About
- Daily vs Advanced; embed Controller/Save Timeline/Perf under Advanced
- Restyle tools to `RetraPanel` (kill `surfaceContainerLow` dialect)
- Real back stack / shared chrome (no full-app early `return` orphan)

**Exit:** secondary destinations scannable; no foreign visual dialect.

---

## Phase 4 — Player quieting

[`PlayerUi.kt`](app/src/main/kotlin/app/retra/emulator/PlayerUi.kt):

- Game canvas primary; chrome on demand (tap / pause)
- Immersive means chrome actually gone during play
- Align overlays with Archive Glass, not pure black + stock TopAppBar as the product face

**Exit:** play session feels quieter than library chrome.

---

## Phase 5 — Harden + prove

- Real onboarding UI test (replace fake `Text` assert)
- TalkBack, large font, reduce transparency/motion, landscape/tablet
- `$impeccable audit` (native) + critique on Home/Library
- Device screenshots for empty, populated, core-missing, patch mismatch

**Exit:** critique Aesthetic ≥3, Consistency ≥3; no dead parallel shells.

---

## File map (implementation order)

1. `PRODUCT.md` / `DESIGN.md`
2. Delete `RetraV23Ui.kt`
3. `RetraTheme.kt` + `GlassUi.kt` → kit
4. Shell + Home + Library
5. Discover + Profile + Settings + tools restyle
6. `PlayerUi.kt`
7. Tests + docs honesty (`REDESIGN_CHANGELOG`, `FILES_TO_DELETE`)

**Preserve:** ViewModel, repositories, legal Discover boundaries, BrandUi mark, Drift demo ROM path.

---

## Suggested Impeccable command sequence (per phase)

1. `$impeccable document` → DESIGN.md  
2. `$impeccable distill` Home/Library  
3. `$impeccable clarify` copy pass  
4. `$impeccable layout` Settings/Discover  
5. `$impeccable quieter` Player  
6. `$impeccable harden` + `$impeccable polish`  
7. `$impeccable critique` to score

---

## 2026-08-02 — Operate redesign pass

- Deleted dead `RetraV23Ui.kt` shell.
- Added Archive Glass component kit (`RetraPanel`, `RetraPageTitle`, `RetraBadge`, `RetraEmptyState`).
- Operate-first Home/Library/Discover/Profile; immersive player defaults quieter.
- Split V3 monolith into focused UI files (`RetraHomeUi`, `RetraLibraryUi`, `RetraDiscoverUi`, `RetraProfileUi`, `RetraGameDetailsUi`, `RetraOnboardingUi`, `RetraSharedUi`).
- Settings/tools restyled onto theme shapes.
- Added `RetraChromeScreenTest` Compose smoke coverage.


Phases 0–5 core work landed and compiling (`:app:compileDebugKotlin` green).

### File map after split

| File | Role |
|------|------|
| `RetraV3Ui.kt` | Root shell, nav dock/rail, import orchestration |
| `RetraHomeUi.kt` | Home / Continue hero |
| `RetraLibraryUi.kt` | Library grid/list + filters |
| `RetraDiscoverUi.kt` | Discover / Drift / Patch / Homebrew |
| `RetraProfileUi.kt` | Profile |
| `RetraGameDetailsUi.kt` | Game sheet + Patch Studio dialog |
| `RetraOnboardingUi.kt` | First-run |
| `RetraSharedUi.kt` | Poster cards, badges, format helpers |
| `ui/components/RetraComponents.kt` | Design-system primitives |
| `RetraV3SettingsUi.kt` | Settings |
| `RetraV23ToolsUi.kt` | Advanced tools (restyled) |

### Remaining

- Device / TalkBack / large-font pass
- `$impeccable critique` on Home and Library
- Optional rename of `RetraV23ToolsUi.kt` → `AdvancedToolsUi.kt`
