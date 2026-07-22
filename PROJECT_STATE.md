# Project State

## Current milestone

**Retra 2.0.0 — provenance-first personal GBA archive.**

Developer: **Prashant Chataut**

## Delivered in this milestone

- Real Homebrew Hub search and eligible GBA install flow.
- Libretro No-Intro metadata synchronization using exact ROM checksums.
- One-click matching Libretro cheat lookup plus local RetroArch `.cht` import.
- Strict conversion to ROM-bound Retra Codes, including safe skipping of placeholder definitions.
- SHA-1 added to SHA-256/CRC-32 identity; Room schema advanced to 6.
- Serialized, atomic, content-addressed imports with duplicate and failed-persistence handling.
- Four-stage onboarding and rebuilt separate Profile and Settings experiences.
- About page naming Prashant Chataut and crediting external open data sources.
- Reusable signing key and credentials removed from the source tree.

## Security and product invariants

- No commercial ROM downloads, pirate indexes, proprietary BIOS files, or pre-patched copyrighted games.
- Local gameplay never requires identity or network access.
- Metadata identity comes from checksums, not filenames or artwork.
- Cheats are data-only, exact-ROM-bound, reversible, and visibly sourced.
- Remote content uses HTTPS, strict origin/redirect rules, bounded reads, and atomic persistence.
- Emulator native code is packaged at build time, never downloaded at runtime.

## Verification completed

- Platform-neutral core suite: **43 passed, 0 failed**.
- Native reference runtime host suite: passed.
- mGBA/libretro mock-core host suite: passed.
- Project/static, XML, shell, JNI, and libretro C++20 checks: passed.

## Unverified in this environment

Android Gradle compilation, Room schema export, APK assembly, Android tests, installation, physical-device UI, controller behavior, and packaged mGBA gameplay. See `BUILD_REPORT.md`.
