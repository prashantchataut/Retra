# Skills Integration

## Requested skill commands

The five exact commands requested by the user were executed with complete output redirected to temporary files and copied into `docs/requested-skills/cli-output/`:

- `ui-ux-pro-max`
- `design-guide`
- `paperclip-create-agent`
- `design-taste-frontend`
- `mobile-android-design`

Each CLI invocation failed at the Git clone stage because the sandbox could not resolve `github.com`. Retra does not represent these as successful installations.

Canonical instructions recovered through the available retrieval path were reviewed and stored as offline provenance snapshots under `.agents/skills/`. Relative supporting guidance was resolved into the same project tree where available.

## Applied product decisions

- Premium, nostalgic retro-gaming operating-system feel rather than a generic file manager.
- Material 3 structure with semantic tokens and restrained glass.
- Deliberate asymmetry and hierarchy without sacrificing scanability.
- Design dials: variance 7/10, motion 5/10, density 6/10.
- At least 48dp primary Android touch targets.
- Controller-first and touch-first operation, adaptive phone/tablet navigation, high contrast, reduced motion/transparency, and scalable typography.
- Settings must change real behavior rather than only storing values.
- Explicit implementation-status language instead of polished placeholder buttons.

The Retra design-system summary is in `design-system/retra/MASTER.md`.

## Existing Android guidance

The previously included Android skills were also retained and applied:

- `edge-to-edge`: edge-to-edge setup, inset ownership, and IME behavior.
- `adaptive`: bottom navigation on compact widths and navigation rail on larger windows.
- `testing-setup`: separate platform-neutral, native, static, and device-test evidence.
- `compose-styles`: reviewed but not enabled because the experimental Styles API is not appropriate for the current stable baseline.

## Paperclip agent boundary

`paperclip-create-agent` was reviewed but not executed against a Paperclip API. No organization, credentials, reporting chain, or concrete agent-creation objective was available. Inventing those values would violate the skill’s preconditions.
