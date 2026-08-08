#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/app/src/main/assets/demo/retra_drift.gba"
TMP="${TMPDIR:-/tmp}/retra-demo-rom"
mkdir -p "$TMP" "$(dirname "$OUT")"
CLANG="${CLANG:-/usr/local/swift/usr/bin/clang}"
LLD="${LLD:-/usr/local/swift/usr/bin/ld.lld}"
OBJCOPY="${OBJCOPY:-/usr/local/swift/usr/bin/llvm-objcopy}"
"$CLANG" --target=armv4t-none-eabi -mcpu=arm7tdmi -marm -ffreestanding -fno-builtin -fno-stack-protector -Oz -c "$ROOT/tools/demo-rom/startup.S" -o "$TMP/startup.o"
"$CLANG" --target=armv4t-none-eabi -mcpu=arm7tdmi -marm -ffreestanding -fno-builtin -fno-stack-protector -Oz -c "$ROOT/tools/demo-rom/main.c" -o "$TMP/main.o"
"$LLD" -flavor gnu -T "$ROOT/tools/demo-rom/link.ld" -nostdlib "$TMP/startup.o" "$TMP/main.o" -o "$TMP/retra_drift.elf"
"$OBJCOPY" -O binary "$TMP/retra_drift.elf" "$OUT"
python3 - "$OUT" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
data = bytearray(path.read_bytes())
# Pad to a conventional, deterministic 64 KiB homebrew ROM image.
if len(data) < 65536:
    data.extend(b'\xff' * (65536 - len(data)))
# Header complement checksum across 0xA0..0xBC.
data[0xBD] = (-(sum(data[0xA0:0xBD]) + 0x19)) & 0xFF
path.write_bytes(data)
PY
sha256sum "$OUT"
