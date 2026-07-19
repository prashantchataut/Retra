# Project State

## Current milestone

**Retra 0.5.0 — branded onboarding, optional Google identity, gameplay-ready player UX, rewind/screenshots, and customizable library.**

## Stable architecture

- `:core:model` — durable product models.
- `:core:rom` — GBA headers, hashing, catalog rules.
- `:core:emulation` — state machine, envelopes, atomic storage, input, bounded rewind.
- `:core:patching`, `:core:cheats`, `:core:achievements`, `:core:social`, `:core:multiplayer` — platform-neutral systems.
- `:emulation:api` — UI-independent emulator contract.
- `:emulation:native` — reference JNI pipeline and mGBA/libretro adapter.
- `:app` — Compose UI, Room/DataStore, Credential Manager, downloads, artwork, audio, screenshots, lifecycle, and DI.

## Exact continuation point

1. Install a reviewed Android SDK/NDK/Gradle environment.
2. Run `scripts/fetch-mgba-archive.sh` and `scripts/build-mgba-libretro-android.sh`.
3. Build `:app:assembleDebug`; resolve real compiler/API differences before adding features.
4. Run the device matrix in `NEXT_ACTIONS.md` and update this file with evidence.
5. Configure a Google OAuth Web client ID and implement a server token-verification/session endpoint before enabling cloud privileges.

## Non-negotiable invariants

- save integrity outranks visual features;
- ROMs and native code are never downloaded silently;
- legal remote content is explicit, licensed, bounded, and hash verified;
- Google ID tokens are ephemeral and never treated as server verification;
- diagnostic rendering is never called GBA emulation;
- working local play remains available without an account or network.
