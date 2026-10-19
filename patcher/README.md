# :patcher

Pure Kotlin/JVM ROM-hack patch engine (package `app.retra.patcher`), no third-party dependencies.

## Formats

| Format | Notes |
|--------|-------|
| IPS    | Normal + RLE records, `EOF` marker, Lunar IPS truncate extension, growing outputs. A missing `EOF` is a warning. |
| UPS    | Varint/XOR records, source/target/patch CRC32. It can also be applied in reverse (patched ROM -> original). |
| BPS    | All four actions, metadata (`PatchInfo.metadata`), CRC32 checks. `BpsCreator.create()` builds patches. |
| VCDIFF / xdelta3 | RFC 3284 with the default code table, near/same address cache, `VCD_SOURCE`/`VCD_TARGET` windows, xdelta's `VCD_ADLER32` per-window checksum and app header (`metadata`). Supports **LZMA secondary compression**, which Linux/Debian builds of `xdelta3 -e` use by default. DJW/FGK secondary compressors (`-S djw`/`-S fgk`) and custom code tables return `UNSUPPORTED`. |

## API

```kotlin
Patcher.detect(patch)            // PatchFormat? (by magic)
Patcher.inspect(patch)           // PatchInfo? (sizes, CRCs, metadata), no source needed
Patcher.apply(source, patch, strict = true, progress = { f -> })   // PatchResult.Ok / PatchResult.Error
Checksums.crc32 / crc32Hex / sha1Hex / sha256Hex
PatchArchive.extractSingle(zipBytes)   // best entry: .bps > .ups > .xdelta/.vcdiff > .ips > .gba, then any entry that looks like a patch
BpsCreator.create(source, target, metadata)
```

`PatchResult.Error.message` is written for end users and can go straight into the UI. With
`strict = false`, a wrong base ROM (source CRC mismatch, xdelta checksum mismatch, a source
that's too short for xdelta) and the resulting target CRC mismatch become `warnings`. Structural
corruption is always an error. The only exceptions are BPS reads past the end of a source that's
too small, which can't be forced.

Error codes: `UNKNOWN_FORMAT`, `CORRUPT_PATCH` (truncated or damaged), `SOURCE_MISMATCH`
(wrong base ROM), `TARGET_MISMATCH`, `PATCH_CHECKSUM` (UPS/BPS file CRC), `UNSUPPORTED`.

Output size is capped at 512 MiB. A 16 MiB -> 32 MiB patch applies in about 20-120 ms on a desktop JVM.

## Tests

`./gradlew --no-daemon :patcher:test`. The xdelta fixtures in `src/test/resources/vcdiff` were
made with xdelta3 3.0.11. Optional large-file tests (`RealWorldTest`) run when
`RETRA_PATCH_SAMPLES` points at a directory with `pokemonHnS_v1.2.1.ups`, and/or
`big_src.bin` + `big_tgt.bin` + `big_*.xdelta`.
