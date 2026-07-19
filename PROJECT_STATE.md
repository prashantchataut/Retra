# Project State

## Current milestone

**Retra 0.6.0 — Prism Glass UI, semantic feedback, notification channels, and repair for the reported Compose compiler failure.**

## Stable foundations

- modular Kotlin/Compose architecture;
- platform-neutral ROM, catalog, patch, cheat, achievement, social, multiplayer, save, and rewind systems;
- JNI reference engine and mGBA/libretro frontend;
- Room/DataStore persistence and explicit migrations;
- adaptive onboarding, library, player, Vault, Discover, community, profile, and categorized settings;
- offline-first Google-optional identity;
- original brand and original short sound assets.

## Exact continuation point

1. Rerun `gradle --no-daemon --stacktrace :app:assembleRelease` in the same CI that produced the supplied failure.
2. Resolve only newly surfaced compiler/lint errors; do not weaken the public Compose API guard.
3. Build reviewed mGBA ABI libraries and install the APK on arm64 hardware.
4. Run the device matrix in `NEXT_ACTIONS.md`.
5. Capture screenshots and perform a visual QA pass before further feature growth.

## Non-negotiable invariants

- save integrity outranks visual effects;
- glass never reduces text contrast or hides focus state;
- reduced transparency and reduced motion are respected;
- frequent haptics stay subtle and can be disabled;
- notification permission is requested in context and notifications remain optional;
- local gameplay never requires Google or network access;
- no commercial ROMs or unreviewed native binaries are bundled.
