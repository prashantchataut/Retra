# Project State

## Current milestone

**Retra 0.7.1 — premium UI rebuild, honest gameplay packaging, and legal discovery handoff.**

## Stable foundations

- modular Kotlin/Compose architecture;
- platform-neutral ROM, catalog, patch, cheat, achievement, social, multiplayer, save, and rewind systems;
- JNI reference engine and mGBA/libretro frontend with CI-required ABI packaging;
- Room/DataStore persistence with explicit migrations through schema 5;
- Material Home / Library / Discover / Settings shell;
- offline-first Google-optional identity;
- restrained graphite/indigo brand system, PNG negative-space mark, and quieter sound/haptic cues.

## Exact continuation point

1. Push/commit 0.7.1 locally when ready, then confirm GitHub Actions `Build and release Retra` stages mGBA for all three ABIs and refuses to publish without them.
2. Install either the CI APK or the local debug APK at `app/build/outputs/apk/debug/app-debug.apk` on arm64 hardware after uninstalling any differently signed Retra build.
3. Verify:
   - a GBA ROM (including large ROM hacks ≤64 MiB) plays with the packaged mGBA core;
   - `.nds` HeartGold/SoulSilver imports are rejected with a clear message;
   - Discover creator links open in the browser and downloaded `.gba` files import via Open with Retra;
   - library padding/navigation feel correct on compact phone and ≥600dp tablet.
4. Capture screenshots across dark/light and reduced-motion settings.
5. Replace debug signing with a private upload key only if/when preparing a store build.

## Local verification already done (2026-07-19)

- Microsoft JDK 17.0.10 hotspot configured for Gradle
- `:app:assembleDebug` BUILD SUCCESSFUL (~25 MB APK)
- `:core:download:test`, `:core:patching:test`, `:core:catalog:test` BUILD SUCCESSFUL

## Non-negotiable invariants

- save integrity outranks visual effects;
- surfaces stay opaque and high-contrast; glass is no longer a default language;
- reduced transparency and reduced motion are respected;
- frequent haptics stay subtle and can be disabled;
- notification permission is requested in context and notifications remain optional;
- local gameplay never requires Google or network access;
- no commercial ROMs, proprietary BIOS, Heart & Soul binaries, or unreviewed native binaries are bundled;
- library deletion removes only Retra-managed copies;
- an APK without packaged mGBA must not claim gameplay readiness.
