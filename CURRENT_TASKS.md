# Current Tasks

## Completed in 0.4.0

- Wire Retra Codes to libretro cheat reset/set.
- Protect saves before cheat activation.
- Add exact-hash HTTPS cheat-pack downloads and custom cheat creation.
- Add exact-hash HTTPS catalog-manifest imports.
- Add local achievement engine, persistence, progress UI, and sharing.
- Add local Retra profile, friend code, social identity configuration, and share payloads.
- Add broad appearance, layout, startup, controls, performance, and privacy settings.
- Add multiplayer compatibility, session model, packet codec/order buffer, LAN transport, and loopback tests.
- Add Community UI and explicit capability/status language.
- Execute and preserve output for all five requested skill commands; add reviewed offline guidance snapshots.
- Expand verification to 35 core checks and libretro cheat coverage.

## Active release gates

- Android compile and lint on a machine with SDK/Gradle/dependencies.
- Build and package pinned mGBA for arm64-v8a and additional intended ABIs.
- Real-device validation of UI, Room/Hilt code generation, SAF, SurfaceView, AudioTrack, controls, haptics, lifecycle, downloads, saves, and cheats.
- Crash-injection and migration tests for Vault and catalog/cheat persistence.
- Emulator-core link callback design before enabling LAN gameplay.

## Explicitly deferred

- Provider OAuth without credentials.
- Hosted internet relay without service infrastructure.
- Cloud synchronization without encryption/conflict/account design.
- Third-party achievements without an authorized API.
- Raw memory-write cheats without reviewed semantics.
- Rewind without deterministic core evidence and memory policy.
