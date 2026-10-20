#!/bin/sh
# Rebuilds Turbo Drift deterministically: assets -> C -> ROM.
#   ./build.sh            build build/turbo_drift.gba + cover
#   ./build.sh --install  also copy ROM + cover into the Retra app and catalog
#   EXTRA_CFLAGS=-DPERF ./build.sh   debug build that logs frame timing (never ship)
set -e
cd "$(dirname "$0")"
export PYTHONDONTWRITEBYTECODE=1
CC=${CC:-arm-none-eabi-gcc}
OBJCOPY=${OBJCOPY:-arm-none-eabi-objcopy}
PY=${PYTHON:-python3}
OUT=build
mkdir -p $OUT/gen $OUT/obj

echo "[1/5] generating assets"
$PY tools/gen_assets.py $OUT/gen

BASE="-mcpu=arm7tdmi -mtune=arm7tdmi -mthumb-interwork -O2 -Wall -Wno-char-subscripts \
 -ffreestanding -fno-strict-aliasing -fomit-frame-pointer -ffunction-sections -fdata-sections \
 -fno-builtin -Isrc -I$OUT/gen $EXTRA_CFLAGS -ffile-prefix-map=$(pwd)=."
CFLAGS="$BASE -mthumb"

echo "[2/5] compiling"
$CC $CFLAGS -c src/crt0.S -o $OUT/obj/crt0.o
for f in main race render gfx audio save; do
  $CC $CFLAGS -c src/$f.c -o $OUT/obj/$f.o
done
$CC $BASE -marm -O2 -c src/iwram.c -o $OUT/obj/iwram.o
$CC $CFLAGS -O1 -c $OUT/gen/assets.c -o $OUT/obj/assets.o

echo "[3/5] linking"
$CC -mcpu=arm7tdmi -mthumb -mthumb-interwork -nostartfiles -nostdlib -T gba.ld \
  -Wl,--no-warn-rwx-segments -Wl,--gc-sections -Wl,-Map,$OUT/turbo_drift.map \
  $OUT/obj/crt0.o $OUT/obj/main.o $OUT/obj/race.o $OUT/obj/render.o $OUT/obj/gfx.o $OUT/obj/audio.o \
  $OUT/obj/save.o $OUT/obj/iwram.o $OUT/obj/assets.o -lgcc -o $OUT/turbo_drift.elf
$OBJCOPY -O binary $OUT/turbo_drift.elf $OUT/turbo_drift.gba

echo "[4/5] fixing header"
$PY tools/fix_header.py $OUT/turbo_drift.gba

echo "[5/5] cover"
$PY tools/make_cover.py $OUT/turbo_drift_cover.png
ls -l $OUT/turbo_drift.gba
sha256sum $OUT/turbo_drift.gba

if [ "$1" = "--install" ]; then
  cp $OUT/turbo_drift.gba ../../app/src/main/assets/games/turbo_drift.gba
  cp $OUT/turbo_drift.gba ../../catalog/games/turbo_drift.gba
  cp $OUT/turbo_drift_cover.png ../../app/src/main/assets/games/turbo_drift_cover.png
  echo "installed ROM + cover"
fi
