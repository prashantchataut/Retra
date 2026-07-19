# Known Issues

## Build and runtime blockers

- No Android SDK/NDK, Gradle executable/wrapper JAR, dependency cache, emulator, or device was available here.
- No APK/AAB was generated.
- Compose, Room/Hilt code generation, SAF grants, AudioTrack, SurfaceView, haptics, controller handling, and Android networking have not been executed in this environment.
- The archive does not bundle fetched mGBA source or a compiled Android shared library.

## Emulation

- The adapter is host-verified against a deterministic fake libretro core; real mGBA Android behavior remains unverified.
- Frames are presented through a software SurfaceView path; hardware rendering and shader pipelines are future work.
- Audio latency, Bluetooth behavior, thermal fallback, run-ahead, rewind, RTC, sensors, and link-cable behavior require device/core work.
- Compatibility across commercial and homebrew titles is unknown; universal compatibility is not claimed.

## Cheats

- Libretro text-code activation is implemented and host-verified, but requires real mGBA/device testing.
- Raw memory-write entries are rejected until width, alignment, endianness, and conditional semantics are reviewed.
- Provider-specific online cheat databases are not bundled; users add explicit verified packs.

## Catalogs and downloads

- Foreground jobs are not yet a durable WorkManager pause/resume queue.
- URL syntax blocks local/private targets, but DNS rebinding protection must also validate resolved socket addresses.
- Catalog signing, key rotation, revocation, and publisher trust UI are absent.
- Download coverage still requires Android integration and hostile-network tests.

## Achievements and social

- Achievements are local only and not tamper-proof; they are not represented as authoritative competitive proof.
- Provider connections are user-entered labels/HTTPS links, not OAuth sessions.
- Native Discord/Google/other APIs require credentials, redirect URIs, policy review, token storage, revocation, and account deletion.

## Multiplayer

- LAN socket framing is implemented and loopback-tested, but is not connected to GBA serial/link callbacks.
- Internet relay is an interface only; no server, authentication, NAT traversal, matchmaking, moderation, or abuse controls exist.
- Spectator, rollback, deterministic resync, latency compensation, and multi-controller link behavior are not implemented.

## Product and release

- No production privacy policy, terms, crash reporting, analytics, cloud account, store listing, signing configuration, or release pipeline is included.
- Trademark, package-name, domain, and store-name availability still require review.
