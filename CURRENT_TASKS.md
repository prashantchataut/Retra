# Current Tasks

## Completed in 2.0.0

- Added live Homebrew Hub discovery and eligible GBA homebrew installation.
- Added exact Libretro metadata synchronization and SHA-1/CRC-32/size matching.
- Added matching Libretro cheats, local RetroArch `.cht` import, strict conversion, and ROM binding.
- Advanced Room to schema 6 with canonical metadata fields.
- Hardened imports with serialization, atomic storage, duplicate control, and failed-persistence cleanup.
- Expanded onboarding to four stages and rebuilt separate Profile and Settings surfaces.
- Added developer information for Prashant Chataut.
- Removed committed release signing material.
- Passed 43 core checks plus native, libretro, static, XML, shell, and host C++ checks.

## Active release task

Compile and test the Android app on provisioned CI, generate schema 6, package reviewed mGBA libraries, and complete the device matrix before publishing any APK.
