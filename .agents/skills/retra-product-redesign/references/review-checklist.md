# Review Checklist

## Architecture

- UI changes do not bypass repository/ViewModel state ownership.
- No network, file, checksum, or database work runs directly in a composable.
- New destinations restore safely after process recreation.
- Existing game, save, patch, and catalog flows remain reachable.

## Compose

- Stable keys are used for lazy lists.
- Large screens use adaptive width/navigation.
- Scroll containers do not conflict or create unbounded measurement.
- Interactive targets are at least 48 dp where practical.
- Icons have meaningful descriptions when they perform an action.
- Reduced motion/transparency and text scaling remain usable.

## Visual System

- Contrast remains readable over artwork and glass.
- Artwork is cropped consistently and never distorted.
- The logo remains legible at launcher and 38–48 dp UI sizes.
- Dark and light themes both have intentional surfaces.
- Decorative gradients carry no semantic meaning.

## Discovery and Privacy

- Commercial ROMs are not bundled or automatically sourced.
- Downloadable entries require explicit distribution permission and checksum validation.
- External links are visibly external.
- ROM and save privacy copy matches actual behavior.

## Validation

Run, when available:

1. XML/resource parsing.
2. Kotlin/Compose compilation.
3. Unit tests.
4. Instrumented onboarding/navigation smoke tests.
5. Project verification scripts.
6. Manual checks on compact phone, tall phone, tablet, light mode, dark mode, large text, reduced motion, and reduced transparency.

Document unavailable tooling and pre-existing failures separately from redesign regressions.
