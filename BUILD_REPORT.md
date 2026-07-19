# Retra 0.7.0 Build Report

## Scope delivered

- Managed multi-format imports and guided CRC patch application
- Room schema 5 migration for CRC, managed paths, collections, and tags
- Discover groups: Retra Curated, official creator releases, SHA-256-pinned manifests
- External `ACTION_VIEW` / `SEND` import intents
- Four-destination Compose shell, three-step onboarding, graphite/indigo theme, refreshed R mark
- CI workflow provisioning SDK 37 / NDK 28.2 / CMake 3.22.1 / Gradle 9.5 and packaging mGBA for three ABIs

## Signing

Release APKs remain **debug-signed** for personal / FOSS sideload testing. They are not Play Store artifacts.

## Local sandbox limitation

This workstation checkout does not currently have a working Android SDK/NDK Gradle toolchain (JDK boot-class-path failure on the local Temurin 17 install). Full `:app:test` / `:app:assembleRelease` verification is delegated to GitHub Actions on push to `main`.

## Host/unit coverage added or retained

- `PatchEngine` UPS descriptor / CRC inspection tests
- `CatalogDownloadPolicy` tests for GBA/ZIP/patch URLs, EXTERNAL blocking, private hosts, and GitHub asset redirect hops
- Existing core / native / libretro / project verification scripts remain in `tools/`

## Claims that are intentionally not made

- No commercial ROM or Heart & Soul patch binary is bundled
- No Play-ready signing key is present
- Device UX matrix and hardware gameplay validation still require a physical install of the CI APK
