# Threat Model

## Protected assets

- User-owned ROMs and SAF grants.
- Battery saves, save states, suspend snapshots, backups, and patched derivatives.
- Native process memory, rendering/audio resources, and emulator integrity.
- Catalogs, downloaded legal games, cheat packs, and provenance.
- Profiles, friend codes, achievements, multiplayer sessions, settings, and future tokens/cloud data.

## Trust boundaries

1. Android document provider → bounded local import and hash verification.
2. Explicit HTTPS URL → redirect/target/response/hash policy → strict parser/quarantine.
3. Kotlin application → JNI → dynamic libretro shared library.
4. Native save/state memory → versioned verified envelope → atomic storage.
5. Patch/cheat/catalog bytes → restricted parser/engine.
6. Local multiplayer peer → bounded framed packet → CRC/order/compatibility gate.
7. Local profile/share payload → Android share sheet or future provider adapter.
8. Local data → future cloud/relay/OAuth services.

## Implemented controls

- SAF instead of broad storage permission.
- ROM size/header/checksum and SHA-256 verification before native load, patching, or catalog import.
- Native core receives verified bytes rather than catalog-controlled paths.
- Gameplay capability only when a reviewed shared library loads.
- Save magic/version, ROM/core identity, kind, slot, timestamp, length, and payload SHA-256.
- Canonical safe paths, temporary files, fsync, atomic move where supported, and rotating backups.
- IPS/UPS/BPS format, CRC, integer, source/target-size, patch-size, and output-size checks.
- Retra Codes bounded declarative grammar, exact ROM identity, dependency/conflict checks, and no scripting.
- Battery flush and protected pre-cheat state before cheat activation.
- Restricted catalog JSON with duplicate-key/unknown-field rejection and size/depth/count/string limits.
- HTTPS-only URLs, explicit expected hashes, safe IDs, legal provenance, supported `.gba` type, and maximum sizes.
- Manual same-host redirects, content-length/MIME/encoding checks, streaming caps, final hash/header verification, and atomic persistence.
- Local/private IP literals and common local hostnames blocked for internet imports.
- Android cleartext network traffic disabled.
- Achievements include an integrity policy and are not represented as unbreakable proof.
- Share payloads omit ROM/save contents and use user-controlled public fields.
- Multiplayer exact ROM/core/patch compatibility, bounded packet size, CRC, sequence ordering, and LAN-only bind restrictions.
- No downloaded Android code execution and no invisible ROM-site scraping.

## Residual risks before release

- DNS rebinding/resolved-private-address checks must be enforced at the actual connection.
- Real mGBA requires fuzzing and Android memory-pressure/thermal/crash tests.
- Shared-library supply chain, source lock, notices, ABI hashes, and reproducible builds require review.
- Save durability needs crash injection on real Android filesystems.
- Catalog/cheat publisher signatures, revocation, key rotation, and trust UX are absent.
- Foreground downloads are not process-durable queues.
- Raw memory-write cheat semantics remain gated.
- Social OAuth/token storage, relay authentication, matchmaking abuse controls, and cloud encryption are not implemented.
- LAN transport has no gameplay connection until a link-capable core and synchronization model exist.
- Local achievements can be modified by a sufficiently privileged user and must not be marketed as tamper-proof.

## 0.5 additions

### Google identity

Threats: replayed ID tokens, wrong audience, forged client claims, persisted bearer tokens, account confusion, and treating a device credential as server trust.

Controls: cryptographic nonce, official credential type parsing, Web client ID audience, no raw token persistence, local trust level, credential-state clearing, and an explicit backend-verification gate. The future backend must verify signature, issuer, audience, expiry, nonce, revocation/account state, and issue its own short-lived session.

### Custom artwork

Threats: oversized images, decompression memory pressure, malformed codecs, path injection, and artwork replacing ROM identity.

Controls: ContentResolver-only import, 12 MiB source cap, dimension cap, bounded downsampling, platform decoder, app-private destination, deterministic SHA-based filename, atomic temporary commit, and a separate database path. Artwork never enters ROM hashing or emulation.

### Screenshots

Threats: incomplete MediaStore entries, storage failure, accidental sensitive sharing, and corrupt image output.

Controls: explicit user action, PNG encoding, pending MediaStore transaction on Android 10+, cleanup on failure, app-specific storage on older Android, and no automatic upload/share.

### Rewind

Threats: memory exhaustion, mutated native buffers, incompatible persistent states, and misuse in integrity modes.

Controls: copied snapshots, 32 MiB maximum, thread-safe queue, minimum-history checks, memory-only lifetime, reset/load/stop clearing, and separation from Vault. Future Pure Run rules must disable rewind explicitly.

### Automatic backup

Android automatic backup is disabled to prevent silent cloud transfer of ROMs, saves, artwork, private catalogs, and local identity metadata. Any future backup must be explicit, encrypted, user-scoped, conflict-aware, and ROM-excluding by default.
