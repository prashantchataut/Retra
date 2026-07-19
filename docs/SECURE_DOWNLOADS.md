# Secure Legal Downloads

Retra’s downloader is intended only for public-domain, open-source, developer-authorized, licensed homebrew, demos, and other legally redistributable GBA files.

## Preflight policy

Each entry must provide:

- safe identifier;
- HTTPS URL;
- `.gba` file expectation;
- size within the 64 MiB cap;
- exact 64-character SHA-256;
- creator, license, and explicit distribution permission.

Loopback/private IP literals and common local hostnames are rejected. The built-in preview uses a non-routable example endpoint and cannot be downloaded.

## Network execution

- Android cleartext traffic is disabled.
- Redirects are handled manually and limited to five.
- Redirects may not silently change host authority.
- Compression is disabled with `Accept-Encoding: identity` to keep byte counts deterministic.
- HTTP status, Content-Length (when present), MIME type, and redirect metadata are checked.
- Bytes stream into a temporary file with a hard limit and progress reporting.

## Import gate

Before a download enters the library, Retra verifies:

1. final byte count;
2. SHA-256 against manifest metadata;
3. GBA header/checksum;
4. duplicate identity;
5. destination safety;
6. creator/source/license/distribution provenance.

The file is flushed, fsynced, atomically moved where supported, and only then inserted into Room.

## Remaining production work

- tie DNS resolution/private-address validation to the actual TLS connection;
- add signed provider manifests, key rotation, revocation, and trust UX;
- move jobs into WorkManager for process-safe queue/retry/cancel/resume/history;
- add privacy-sanitized failure diagnostics and certificate/pinning policy where appropriate;
- perform Android integration and hostile-network testing.
