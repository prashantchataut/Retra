# Retra Patch Formats

Retra Patch applies IPS, UPS, and BPS files locally to a user-selected, re-verified base ROM.

## Safety rules

- Maximum base ROM: 64 MiB.
- Maximum patch: 32 MiB.
- Maximum output: 64 MiB.
- The base ROM SHA-256 is checked again immediately before application.
- UPS/BPS source, target, and patch CRC values are verified.
- Integer overflows, out-of-range reads/writes, malformed variable integers, missing footers, and invalid copy references are rejected.
- The source ROM is never overwritten.
- The output is GBA-header validated, written with fsync/atomic replacement, hashed, and added as a separate library entry.
- Library provenance records the base SHA-256, patch SHA-256, format, and patch display name.

## Supported behavior

- IPS literal records and RLE, including optional truncate size.
- UPS variable-length offsets and XOR records.
- BPS SourceRead, TargetRead, SourceCopy, and TargetCopy actions.

Retra distributes neither copyrighted base ROMs nor pre-patched commercial games. A catalog may provide a legally distributed patch, but the user must supply a compatible base copy.
