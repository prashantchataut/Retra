#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/third_party/mgba/upstream"
OUT_ROOT="$ROOT/third_party/mgba/android-build"
JNI_ROOT="$ROOT/emulation/native/src/main/jniLibs"
ANDROID_PLATFORM=${ANDROID_PLATFORM:-26}
ABIS=${ABIS:-"arm64-v8a armeabi-v7a x86_64"}

if [ ! -d "$SOURCE" ] || [ ! -f "$SOURCE/LICENSE" ]; then
  echo "Pinned mGBA source is missing. Run scripts/fetch-mgba.sh first." >&2
  exit 1
fi
if [ ! -f "$ROOT/third_party/mgba/SOURCE_LOCK.txt" ]; then
  echo "SOURCE_LOCK.txt is missing; refusing an unpinned core build." >&2
  exit 1
fi
if [ -z "${ANDROID_NDK_HOME:-}" ] || [ ! -f "$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" ]; then
  echo "Set ANDROID_NDK_HOME to the reviewed Android NDK installation." >&2
  exit 1
fi
for tool in cmake ninja sha256sum; do
  command -v "$tool" >/dev/null 2>&1 || { echo "$tool is required." >&2; exit 1; }
done

grep -qi "Mozilla Public License" "$SOURCE/LICENSE" || {
  echo "The source tree does not contain the expected MPL license." >&2
  exit 1
}

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT" "$JNI_ROOT" "$ROOT/third_party/mgba/notices"
cp "$SOURCE/LICENSE" "$ROOT/third_party/mgba/notices/LICENSE-mGBA-MPL-2.0.txt"

for ABI in $ABIS; do
  BUILD="$OUT_ROOT/$ABI"
  cmake -S "$SOURCE" -B "$BUILD" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-$ANDROID_PLATFORM" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_LIBRETRO=ON \
    -DSKIP_LIBRARY=ON \
    -DBUILD_QT=OFF \
    -DBUILD_SDL=OFF \
    -DBUILD_GL=OFF \
    -DBUILD_GLES2=OFF \
    -DBUILD_GLES3=OFF \
    -DUSE_DEBUGGERS=OFF \
    -DUSE_GDB_STUB=OFF \
    -DUSE_EDITLINE=OFF \
    -DUSE_FFMPEG=OFF \
    -DUSE_LIBZIP=OFF \
    -DUSE_LUA=OFF \
    -DENABLE_SCRIPTING=OFF \
    -DUSE_DISCORD_RPC=OFF \
    -DBUILD_TEST=OFF \
    -DBUILD_SUITE=OFF \
    -DBUILD_EXAMPLE=OFF \
    -DDISABLE_DEPS=ON
  cmake --build "$BUILD" --parallel
  CORE=$(find "$BUILD" -type f \( -name '*libretro*.so' -o -name 'libmgba*.so' \) | head -n 1)
  if [ -z "$CORE" ] || [ ! -f "$CORE" ]; then
    echo "No libretro shared library was produced for $ABI." >&2
    exit 1
  fi
  mkdir -p "$JNI_ROOT/$ABI"
  cp "$CORE" "$JNI_ROOT/$ABI/libmgba_libretro.so"
  sha256sum "$JNI_ROOT/$ABI/libmgba_libretro.so"
done > "$ROOT/third_party/mgba/ANDROID_BINARY_HASHES.txt"

cat > "$ROOT/third_party/mgba/ANDROID_BUILD_LOCK.txt" <<LOCK
android_platform=$ANDROID_PLATFORM
android_ndk_home=$ANDROID_NDK_HOME
abis=$ABIS
cmake=$(cmake --version | head -n 1)
ninja=$(ninja --version)
LOCK

echo "Built and staged the pinned mGBA libretro core for: $ABIS"
echo "Run the compatibility and save-integrity gates before distributing an APK."
