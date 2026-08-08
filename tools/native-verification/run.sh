#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
OUT="$ROOT/tools/native-verification/build"
mkdir -p "$OUT"
c++ -std=c++20 -Wall -Wextra -Werror \
  "$ROOT/emulation/native/src/main/cpp/reference_engine.cpp" \
  "$ROOT/tools/native-verification/main.cpp" \
  -I"$ROOT/emulation/native/src/main/cpp" \
  -o "$OUT/native-verification"
"$OUT/native-verification" | tee "$ROOT/tools/native-verification/last-result.txt"
