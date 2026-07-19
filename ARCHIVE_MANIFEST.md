# Archive Manifest

- Product: Retra Android GBA emulator source
- Version: 0.4.0
- Session date: 2026-07-19
- Pre-package inventory: 143 files; 8,790 Kotlin lines; 1,089 C/C++ header/source lines
- Gradle modules: 13
- Source: Kotlin/Compose, C++/JNI/libretro adapter, Gradle configuration, scripts, host verification, design system, skill snapshots, and engineering documents
- Verification: 35/35 platform-neutral checks; native reference suite; fake-libretro adapter suite including cheats; project/XML/TOML/migration/security/achievements/social/multiplayer/skills/DI checks; shell syntax; JNI and adapter host compilation
- Included systems: local and legal online game libraries, custom/online Retra Codes, player/Vault, IPS/UPS/BPS patching, achievements, private-first social sharing, customization, multiplayer protocol and LAN transport
- Android APK/AAB: not produced because SDK/NDK/Gradle/dependency/device access was unavailable
- Gameplay binary: not bundled; scripts build a pinned reviewed mGBA source and record hashes/notices
- Requested skills: exact CLI failure logs included; reviewed offline instruction snapshots included with provenance
- Excluded: commercial ROMs, proprietary BIOS, copyrighted game assets, pre-patched commercial games, provider credentials/tokens, fetched mGBA source, generated build directories, compiled host objects/shared libraries, APK/AAB
