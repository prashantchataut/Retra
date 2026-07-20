# Retra 0.7.1 Build Report

## Scope delivered

- Premium Material shell: top app bar, bottom navigation, navigation rail (≥600dp), opaque surfaces, ≤16dp radii
- PNG negative-space brand mark for launcher, splash, and in-app logos (no letter-R glyph)
- Restrained graphite / indigo theme with quieter interface sounds and haptic cooldowns
- Honest `coreAvailable` gating (gameplay-tier mGBA only; diagnostic cores do not claim play-ready)
- CI release workflow fails unless `arm64-v8a`, `armeabi-v7a`, and `x86_64` mGBA cores are staged
- Discover prioritizes official creator pages with browser download → Open-with-Retra import handoff
- Clearer NDS rejection messaging (e.g. HeartGold/SoulSilver) and more reliable MIME-typed GBA/ZIP/patch imports

## Local verification (this workstation)

- JDK: `C:\Users\MMT\AppData\Local\Programs\Microsoft\jdk-17.0.10.7-hotspot`
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:assembleDebug` — BUILD SUCCESSFUL (~25 MB APK at `app/build/outputs/apk/debug/app-debug.apk`)
- `:core:download:test` / `:core:patching:test` — BUILD SUCCESSFUL
- Local debug APK packages `libretra_native.so` only; it does **not** include CI-staged `libmgba_libretro.so`. Real GBA play still requires a release APK from GitHub Actions after the mGBA packaging gate passes.

## Signing

Release APKs remain **debug-signed** for personal / FOSS sideload testing. They are not Play Store artifacts.

## Host/unit coverage retained

- `PatchEngine` UPS descriptor / CRC inspection tests
- `CatalogDownloadPolicy` tests for GBA/ZIP/patch URLs, EXTERNAL blocking, private hosts, and GitHub asset redirect hops
- Existing core / native / libretro / project verification scripts remain in `tools/`

## Claims that are intentionally not made

- No commercial ROM or Heart & Soul patch binary is bundled
- No Play-ready signing key is present
- Physical device UX matrix still requires installing a build that packages mGBA for the device ABI

## Device checks remaining

1. Install the CI or local APK on arm64 hardware (uninstall any differently signed Retra first)
2. Confirm a GBA ROM (including large hacks ≤64 MiB) plays
3. Confirm `.nds` imports are rejected with the HeartGold/SoulSilver callout
4. Confirm Discover creator links → browser download → Open with Retra import
5. Spot-check Home / Library / Discover padding on phone and tablet widths
