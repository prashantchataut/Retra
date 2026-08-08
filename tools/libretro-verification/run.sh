#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
OUT="$ROOT/tools/libretro-verification/build"
rm -rf "$OUT"
mkdir -p "$OUT"
CXX=${CXX:-c++}
"$CXX" -std=c++20 -Wall -Wextra -Werror -fPIC -shared \
  "$ROOT/tools/libretro-verification/src/fake_core.cpp" \
  -o "$OUT/libmgba_libretro_fake.so"
"$CXX" -std=c++20 -Wall -Wextra -Werror \
  -I"$ROOT/emulation/native/src/main/cpp" \
  "$ROOT/emulation/native/src/main/cpp/libretro_mgba_engine.cpp" \
  "$ROOT/tools/libretro-verification/src/main.cpp" \
  -ldl -o "$OUT/libretro-verification"
"$OUT/libretro-verification" "$OUT/libmgba_libretro_fake.so" | tee "$ROOT/tools/libretro-verification/last-result.txt"
