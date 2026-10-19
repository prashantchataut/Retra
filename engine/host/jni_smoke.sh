#!/bin/sh
# Builds libretra_engine.so for the host JVM and runs the Kotlin API smoke test against it.
#   KOTLINC=/path/to/kotlinc/bin/kotlinc JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
#     engine/host/jni_smoke.sh path/to/game.gba
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../src/main"
OUT=${OUT:-/tmp/retra-jni-smoke}
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
KOTLINC=${KOTLINC:-kotlinc}
ROM=${1:-$HERE/../../app/src/main/assets/games/mochi_hop.gba}
mkdir -p "$OUT/classes"
g++ -std=c++17 -O2 -shared -fPIC -fno-exceptions -fno-rtti -Wall \
    -I"$SRC/cpp/core" -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    "$SRC"/cpp/core/*.cpp "$SRC/cpp/jni/retra_jni.cpp" -o "$OUT/libretra_engine.so"
"$KOTLINC" "$SRC"/kotlin/app/retra/engine/*.kt "$HERE/jni/Smoke.kt" -d "$OUT/classes" 2>&1 | grep -v '^warning' || true
STDLIB="$(dirname "$(command -v "$KOTLINC" || echo "$KOTLINC")")/../lib/kotlin-stdlib.jar"
"$JAVA_HOME/bin/java" -Djava.library.path="$OUT" -cp "$OUT/classes:$STDLIB" SmokeKt "$ROM"
