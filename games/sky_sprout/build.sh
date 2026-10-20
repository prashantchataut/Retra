#!/bin/sh
# Rebuilds Sky Sprout deterministically: assets -> C -> ROM.
#   ./build.sh            build build/sky_sprout.gba (+ cover)
#   ./build.sh --install  also copy the ROM and cover into the app/catalog assets
# Debug builds: EXTRA_CFLAGS="-DDEBUG_STAGE=3 -DDEBUG_BOSS -DDEBUG_INVULN -DDEBUG_PERF" ./build.sh
set -e
cd "$(dirname "$0")"
export PYTHONDONTWRITEBYTECODE=1
CC=${CC:-arm-none-eabi-gcc}
OBJCOPY=${OBJCOPY:-arm-none-eabi-objcopy}
PY=${PYTHON:-python3}
OUT=${OUT:-build}
mkdir -p $OUT/gen $OUT/obj

echo "[1/5] generating assets"
$PY tools/gen_assets.py $OUT/gen

CFLAGS="-mcpu=arm7tdmi -mtune=arm7tdmi -mthumb-interwork -O2 -Wall -Wno-char-subscripts \
 -ffreestanding -fno-strict-aliasing -fomit-frame-pointer -ffunction-sections -fdata-sections \
 -Isrc -I$OUT/gen $EXTRA_CFLAGS -ffile-prefix-map=$(pwd)=."

echo "[2/5] compiling"
$CC $CFLAGS -mthumb -c src/crt0.S -o $OUT/obj/crt0.o
for f in main play enemy boss gfx audio save; do
  $CC $CFLAGS -mthumb -c src/$f.c -o $OUT/obj/$f.o
done
# hot loops run from IWRAM as ARM code
$CC $CFLAGS -marm -c src/hot.c -o $OUT/obj/hot.o
$CC $CFLAGS -mthumb -O1 -c $OUT/gen/assets.c -o $OUT/obj/assets.o

echo "[3/5] linking"
$CC -mcpu=arm7tdmi -mthumb -mthumb-interwork -nostartfiles -nostdlib -T gba.ld \
  -Wl,--no-warn-rwx-segments -Wl,-Map,$OUT/sky_sprout.map \
  $OUT/obj/crt0.o $OUT/obj/main.o $OUT/obj/play.o $OUT/obj/enemy.o $OUT/obj/boss.o $OUT/obj/gfx.o \
  $OUT/obj/audio.o $OUT/obj/save.o $OUT/obj/hot.o $OUT/obj/assets.o -lgcc -o $OUT/sky_sprout.elf
$OBJCOPY -O binary $OUT/sky_sprout.elf $OUT/sky_sprout.gba

echo "[4/5] fixing header"
$PY tools/fix_header.py $OUT/sky_sprout.gba

echo "[5/5] cover"
$PY tools/make_cover.py $OUT/sky_sprout_cover.png
ls -l $OUT/sky_sprout.gba
sha256sum $OUT/sky_sprout.gba

if [ "$1" = "--install" ]; then
  mkdir -p ../../app/src/main/assets/games ../../catalog/games
  cp $OUT/sky_sprout.gba ../../app/src/main/assets/games/sky_sprout.gba
  cp $OUT/sky_sprout.gba ../../catalog/games/sky_sprout.gba
  cp $OUT/sky_sprout_cover.png ../../app/src/main/assets/games/sky_sprout_cover.png
  echo "installed ROM + cover"
fi
