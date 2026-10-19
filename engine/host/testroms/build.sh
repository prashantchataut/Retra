#!/bin/sh
# Builds Retra's own test ROMs (needs arm-none-eabi-gcc). Usage: build.sh OUTDIR
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT=${1:-/tmp/retra-testroms}
mkdir -p "$OUT"
CC=${CC:-arm-none-eabi-gcc}
OBJCOPY=${OBJCOPY:-arm-none-eabi-objcopy}
CFLAGS="-mcpu=arm7tdmi -mthumb -mthumb-interwork -O2 -Wall -ffreestanding -fno-strict-aliasing -I$HERE"
python3 -I "$HERE/gen_bios_data.py" "$OUT/bios_data.h"
CFLAGS="$CFLAGS -I$OUT"
build() {
    name=$1; shift
    $CC $CFLAGS "$@" -nostartfiles -nostdlib -T "$HERE/link.ld" -Wl,--no-warn-rwx-segments \
        "$HERE/crt0.S" -lgcc -o "$OUT/$name.elf"
    $OBJCOPY -O binary "$OUT/$name.elf" "$OUT/$name.gba"
}
build rtc_test "$HERE/rtc_test.c"
build eeprom8k_test "$HERE/eeprom_test.c" -DEEPROM_BITS=14
build eeprom512_test "$HERE/eeprom_test.c" -DEEPROM_BITS=6
build sound_test "$HERE/sound_test.c"
build bench_test "$HERE/bench_test.c"
build bios_test "$HERE/bios_test.c"
echo "test ROMs in $OUT"
