# Implementation Status

| Area | Status | Evidence / limitation |
|---|---|---|
| Retra Prism adaptive UI | Implemented in source | Phone/tablet navigation, Community, player, Vault, settings; not Android-built here |
| Deep appearance customization | Implemented in source | Theme, palette, glass, corners, font scale, contrast, motion, transparency, density |
| Behavior customization | Implemented in source | Startup, stats, recommendations, touch opacity, haptics, speed, performance, privacy |
| SAF ROM import/folder scan | Implemented in source | Persisted grants, bounded reads; device test pending |
| GBA parsing/hash/duplicates | Implemented and host-tested | Core verification |
| Room library/migrations | Implemented in source | Explicit migrations and no destructive fallback; Android build pending |
| Local and HTTPS catalogs | Implemented and host-policy-tested | Strict JSON, exact hash, local-target/cross-host blocking, atomic storage |
| Legal game downloader | Implemented and host-policy-tested | HTTPS, response/length/hash/header/provenance checks; device network test pending |
| Custom cheat creation | Implemented in source | Exact ROM-bound declarative entries |
| Local/online cheat-pack import | Implemented and host-tested | Expected SHA-256, strict parser, size/redirect/private-host controls |
| Cheat conflict/dependency analysis | Implemented and host-tested | Exact ROM matching and raw collision detection |
| Libretro cheat activation | Implemented and adapter-tested | Protected pre-cheat save; real Android mGBA test pending |
| Raw memory-write cheats | Intentionally disabled | Width/endianness translation requires review |
| Native diagnostic engine | Implemented and host-tested | Load/frame/input/state/reset/corruption |
| mGBA/libretro adapter | Implemented and host-tested | Real Android mGBA binary not bundled |
| Automatic gameplay/fallback selection | Implemented in source | Gameplay tier only when shared library loads |
| Touch controls and haptics | Implemented in source | Haptics toggle now affects press feedback; device test pending |
| Physical controller mapping | Implemented in source | Android device/controller validation pending |
| Video and PCM presentation | Implemented in source | SurfaceView and AudioTrack paths; device test pending |
| Save envelopes/atomic backups | Implemented and host-tested | ROM/core identity, checksum, fsync, rotation |
| Quick save/load/suspend/Vault | Implemented in source | Device lifecycle/crash injection pending |
| Battery-save bridge | Implemented and adapter-tested | Real mGBA persistence test pending |
| IPS/UPS/BPS patching | Implemented and host-tested | CRC/bounds/provenance and original preservation |
| Local achievements | Implemented and host-tested | Counters, unique progress, integrity gates, persistence/UI |
| Third-party achievements | Not started | Requires authorized provider/API |
| Retra profile/friend code | Implemented in source | Local persistent identity |
| Android social sharing | Implemented in source | Share-sheet achievement/invite payloads; device test pending |
| Provider public-profile links | Implemented in source | Local labels/HTTPS links, not OAuth |
| Provider OAuth | Interface boundary | Requires credentials, redirect URIs, provider review |
| Multiplayer compatibility/protocol | Implemented and host-tested | Exact identities, CRC packets, ordered buffer |
| LAN transport | Implemented and loopback-tested | Trusted-LAN bounded framing |
| Real GBA link gameplay | Gated | Requires core link callbacks and timing model |
| Internet relay | Interface only | Requires a deployed authenticated service |
| Cloud sync | Not started | Requires provider, encryption, conflicts, account lifecycle |
| Rewind | Not started | Requires deterministic core snapshots and memory budget |
| Android APK/AAB | Blocked | No SDK/NDK/Gradle/dependencies/device in this environment |
