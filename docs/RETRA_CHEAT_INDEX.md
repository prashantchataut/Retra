# Retra Trusted Cheat Index (`.rci`)

## Purpose

A Retra cheat index is a small, declarative catalog that lets a user install a compatible `.rcc` Retra Codes pack without copying individual codes. It is not executable and does not bypass the normal pack parser, conflict analysis, risk labels, or pre-cheat save-state protection.

## Trust model

Importing an index does not automatically trust arbitrary code. Retra requires every entry to provide:

- exact target ROM SHA-256;
- optional four-character game code and revision;
- HTTPS pack URL ending in `.rcc` or `.txt`;
- expected pack SHA-256;
- provider, license, and explicit distribution permission;
- optional HTTPS source page.

The index itself is bounded to 1 MiB and 256 entries. Input must be strict UTF-8. Unknown fields, duplicate fields, unsafe identifiers, malformed hashes, credentials/fragments in URLs, insecure HTTP, and unclosed/nested blocks are rejected.

A pack is shown as one-tap compatible only when the imported game exactly matches the declared identity. The downloaded bytes are checksum verified and then parsed through `RETRA-CODES-1` before storage.

## Format

```text
RETRA-CHEAT-INDEX-1
catalogId=<safe ASCII id>
name=<display name>
provider=<provider>
sourcePageUrl=https://...

[pack]
id=<safe ASCII id>
title=<display title>
description=<what the pack changes>
provider=<optional override>
gameSha256=<64 hex characters>
gameCode=<optional four letters/digits>
revision=<optional 0..255>
downloadUrl=https://.../pack.rcc
packSha256=<64 hex characters>
license=<license identifier or terms>
distributionPermission=<explicit permission statement>
sourcePageUrl=https://...
[/pack]
```

See `docs/examples/retra-cheat-index.example.rci`. Its URLs and hashes are intentionally fictional and must not be enabled in production.

## Publisher checklist

1. Obtain permission to redistribute the pack and document the license.
2. Bind the entry to the exact clean/patched ROM SHA-256 it targets.
3. Host the pack over HTTPS on a stable domain.
4. Publish the SHA-256 of the exact bytes.
5. Keep code lines declarative and compatible with Retra Codes.
6. Test conflicts and mark risky cheats accurately.
7. Provide a source page that explains authorship, version, supported game build, and changes.

## Prohibited uses

Do not use `.rci` to distribute ROMs, patches without permission, proprietary binaries, executable scripts, credentials, dynamic code, or packs that are not legally redistributable.
