# Architecture Decisions

## AD-001 — Truthful core tiers

Retra exposes `DIAGNOSTIC_PIPELINE` and `GBA_GAMEPLAY` as distinct capabilities. The app must never label synthetic frames as emulation. Gameplay is available only when a reviewed libretro-compatible mGBA shared library loads.

## AD-002 — Core independence

Compose, catalogs, profiles, artwork, accounts, and networking do not leak into the native core. `:emulation:api` owns the replaceable contract; `:emulation:native` owns JNI and libretro integration.

## AD-003 — Bytes across the native boundary

The core receives verified ROM bytes and returns bounded frames/audio/state/save payloads. Untrusted catalog paths are not passed into native code.

## AD-004 — Save integrity before convenience

State and battery data use versioned ROM/core-bound envelopes, payload hashes, safe names, temporary files, fsync, atomic replacement, and rotating backups. Cheats trigger a battery flush and protected pre-cheat state.

## AD-005 — Explicit internet imports only

Retra does not scrape ROM sites. Users may add an explicit catalog or cheat-pack URL only with HTTPS and an expected SHA-256. Redirects remain on the same host; private/local targets and oversized responses are rejected.

## AD-006 — Declarative Retra Codes

Cheat packs are bounded UTF-8 data, not scripts. They are matched to exact ROM identity and analyzed for dependency/conflict errors. Libretro-compatible text codes are supported; raw memory writes remain gated until translation semantics are reviewed.

## AD-007 — Legal catalog provenance

Every downloadable game entry requires creator, source, license, distribution permission, size, checksum, and supported file type. Commercial games can be represented as metadata but are never downloaded by Retra.

## AD-008 — Private-first social layer

The app owns a local profile/friend code and creates privacy-safe share payloads. Provider labels and HTTPS public-profile URLs may be stored locally. OAuth is not simulated; it requires real credentials and provider-specific adapters.

## AD-009 — Local achievements first

Achievements are deterministic local rules driven by app events, with integrity policy separated from progress. External achievement providers are future adapters rather than hidden dependencies.

## AD-010 — Multiplayer is layered

Compatibility, session state, packet codec, ordering, LAN transport, and future relay are separate. LAN transport is bounded and checksummed. The UI remains gated until a core exposes real link callbacks; network transport alone is not called multiplayer gameplay.

## AD-011 — Customization uses semantic settings

Themes and behavior use enums/ranged values persisted through DataStore. Settings must change real behavior: recommendations filter built-ins, density affects layout, touch opacity affects controls, and haptics affect presses.

## AD-012 — Restrained visual language

Retra Prism combines Material 3, nostalgic color, asymmetry, controlled glass, and controller-first ergonomics. Readability, 48dp targets, reduced motion/transparency, and high contrast outrank decorative effects.

## AD-013 — Skills provenance is explicit

The five requested `npx skills use` commands were executed but GitHub DNS failed. Command output is retained. Canonical instructions recovered through available retrieval were stored as offline snapshots and applied; they are not represented as successful clones.
