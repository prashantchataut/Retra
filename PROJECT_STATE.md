# Project State

## Current milestone

**Retra 0.7.0 — managed imports/patching, four-destination shell, Discover security, and CI release automation.**

## Stable foundations

- modular Kotlin/Compose architecture;
- platform-neutral ROM, catalog, patch, cheat, achievement, social, multiplayer, save, and rewind systems;
- JNI reference engine and mGBA/libretro frontend with CI-built ABI packaging;
- Room/DataStore persistence with explicit migrations through schema 5;
- adaptive Home / Library / Discover / Settings shell with contextual Vault, patches, and community;
- offline-first Google-optional identity;
- graphite/indigo brand system and original short sound assets.

## Exact continuation point

1. Confirm the GitHub Actions `Build and release Retra` workflow on `main` produces a debug-signed APK and optional mGBA hashes.
2. Install that APK on arm64 hardware after uninstalling any differently signed Retra build.
3. Exercise managed import, ZIP safety, guided patch apply, Discover curated links, and Room migration from a 0.6 library.
4. Capture screenshots across compact phone, landscape, tablet, dark/light, and reduced motion/transparency.
5. Replace debug signing with a private upload key only if/when preparing a store build.

## Non-negotiable invariants

- save integrity outranks visual effects;
- glass never reduces text contrast or hides focus state;
- reduced transparency and reduced motion are respected;
- frequent haptics stay subtle and can be disabled;
- notification permission is requested in context and notifications remain optional;
- local gameplay never requires Google or network access;
- no commercial ROMs, proprietary BIOS, Heart & Soul binaries, or unreviewed native binaries are bundled;
- library deletion removes only Retra-managed copies.
