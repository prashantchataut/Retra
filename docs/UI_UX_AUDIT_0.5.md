# UI/UX Audit — Retra 0.5

## Skill-informed design dials

- **Variance 7/10:** an asymmetric logo/hero system, distinct game artwork, and editorial onboarding; not a repetitive dashboard grid.
- **Motion 5/10:** short `AnimatedContent` transitions and immediate controls; reduced-motion remains available.
- **Density 6/10:** metadata-rich library/settings with 48dp-class targets and opaque technical surfaces.

The retained `ui-ux-pro-max`, `design-guide`, `design-taste-frontend`, and `mobile-android-design` guidance was applied to hierarchy, adaptive navigation, controller focus, status clarity, action feedback, touch targets, onboarding, and design tokens. The Paperclip agent skill remains intentionally unused because no Paperclip organization or agent objective was supplied.

## Primary journey

1. **Onboarding:** brand promise → legal/private library → save/player safeguards → live personalization → optional Google identity/offline entry.
2. **Home:** one strong continue/import hero, meaningful statistics, favorites, recently added, then status.
3. **Library:** explicit import actions, search, adaptive list/grid, custom covers, favorite cues, and no pirated discovery framing.
4. **Game details:** artwork, play, patch, cover customization, verified identity/provenance, Retra Codes, readiness.
5. **Player:** immersive video anchor, minimal performance/status layer, optional touch controls, speed controls, and a scrollable session menu.
6. **You:** account/profile first, then appearance, home/library, display/audio, controls/tester, privacy, and implementation status.

## Design quality safeguards

- Gradients are limited to brand, artwork fallback, and hero layers.
- Glass is restrained; long settings, hashes, warnings, and code data use opaque surfaces.
- Status is never communicated by color alone.
- Google identity is optional and does not masquerade as verified cloud membership.
- Input capture is disabled outside gameplay/tester so controller navigation remains viable.
- Touch controls can be hidden for handhelds/controllers.
- Android themed icons use a dedicated monochrome resource on API 33+.

## Remaining visual validation

- screenshot comparisons on small phones, tall phones, tablets, foldables, and landscape handhelds;
- TalkBack and switch-access focus order;
- 1.3×–2.0× font scale clipping;
- gamepad-only navigation and dialog focus;
- custom artwork crop/contrast extremes;
- OLED black and dynamic-color contrast;
- system bar/cutout behavior in portrait and landscape;
- motion timing on low-refresh and high-refresh displays.
