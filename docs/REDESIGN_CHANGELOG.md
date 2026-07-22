# Retra 0.8 Redesign Change Log

## New

- Added `RetraFinalExperienceUi.kt` for final-form Home, Library, You, and Settings experiences.
- Added the Archive Glass atmosphere and accessibility-aware glass primitives.
- Implemented the Portal / Save Core logo in Compose, SVG, PNG, launcher, round-launcher, splash, and monochrome forms.
- Rebuilt onboarding as a three-stage responsive experience.
- Added a strict trusted-cheat-index (`.rci`) pipeline and game-specific one-tap pack surface.
- Updated the reusable `retra-product-redesign` skill and final UX/security documentation.

## Changed

- Replaced the previous visible screen composition instead of restyling it.
- Reworked Material 3 colors, typography, shapes, navigation, and settings hierarchy around a mineral/ice palette without purple or rainbow gradients.
- Promoted You/Profile to a primary destination and moved Settings to contextual top-bar/rail access.
- Reframed Discover around official creator pages, user-supplied patch bases, and checksum-verified authorized releases.
- Updated launcher branding across all density buckets.

## Preserved

- ViewModel/repository state ownership and Room/DataStore/WorkManager/JNI/emulation boundaries.
- GBA/ZIP import, folder scan, patching, saves, achievements, legal catalog validation, downloads, and controller settings.
- Offline-first play and optional account identity.

## Intentionally not added

- Unauthorized commercial ROM distribution, Poke Harbor indexing, or one-click commercial ROM downloads.
- Continuous full-screen blur, visible multicolor gradients, or purple gaming-dashboard chrome.
- Proprietary game artwork or bundled third-party font files.
