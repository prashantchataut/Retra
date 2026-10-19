#!/bin/sh
# Rebuilds Mochi Hop deterministically: assets -> C -> ROM.
set -e
cd "$(dirname "$0")"
CC=${CC:-arm-none-eabi-gcc}
OBJCOPY=${OBJCOPY:-arm-none-eabi-objcopy}
PY=${PYTHON:-python3}
OUT=build
mkdir -p $OUT/gen $OUT/obj

echo "[1/4] generating assets"
$PY tools/gen_assets.py $OUT/gen

CFLAGS="-mcpu=arm7tdmi -mtune=arm7tdmi -mthumb -mthumb-interwork -O2 -Wall -Wno-char-subscripts \
 -ffreestanding -fno-strict-aliasing -fomit-frame-pointer -ffunction-sections -fdata-sections \
 -Isrc -I$OUT/gen -ffile-prefix-map=$(pwd)=."

echo "[2/4] compiling"
$CC $CFLAGS -c src/crt0.S -o $OUT/obj/crt0.o
for f in main play gfx audio save; do
  $CC $CFLAGS -c src/$f.c -o $OUT/obj/$f.o
done
$CC $CFLAGS -O1 -c $OUT/gen/assets.c -o $OUT/obj/assets.o

echo "[3/4] linking"
$CC -mcpu=arm7tdmi -mthumb -mthumb-interwork -nostartfiles -nostdlib -T gba.ld \
  -Wl,--no-warn-rwx-segments -Wl,-Map,$OUT/mochi_hop.map \
  $OUT/obj/crt0.o $OUT/obj/main.o $OUT/obj/play.o $OUT/obj/gfx.o $OUT/obj/audio.o $OUT/obj/save.o \
  $OUT/obj/assets.o -lgcc -o $OUT/mochi_hop.elf
$OBJCOPY -O binary $OUT/mochi_hop.elf $OUT/mochi_hop.gba

echo "[4/4] fixing header"
$PY tools/fix_header.py $OUT/mochi_hop.gba
ls -l $OUT/mochi_hop.gba
sha256sum $OUT/mochi_hop.gba
