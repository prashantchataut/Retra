#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/third_party/mgba/upstream"
OUT_ROOT="$ROOT/third_party/mgba/android-build"
JNI_ROOT="$ROOT/emulation/native/src/main/jniLibs"
ANDROID_PLATFORM=${ANDROID_PLATFORM:-26}
ABIS=${ABIS:-"arm64-v8a armeabi-v7a x86_64"}
CMAKE_VERSION=${CMAKE_VERSION:-3.22.1}
PATCH_FILE="$ROOT/third_party/mgba/patches/android-posix-vfs.patch"

if [ ! -d "$SOURCE" ] || [ ! -f "$SOURCE/LICENSE" ]; then
  echo "Pinned mGBA source is missing. Run scripts/fetch-mgba-archive.sh first." >&2
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

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK_ROOT" ]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME must be set." >&2
  exit 1
fi

CMAKE_BIN="${CMAKE:-$SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake}"
if [ ! -x "$CMAKE_BIN" ]; then
  CMAKE_BIN="$(command -v cmake 2>/dev/null || true)"
fi
if [ -z "$CMAKE_BIN" ] || [ ! -x "$CMAKE_BIN" ]; then
  echo "CMake is required (expected at $SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake)." >&2
  exit 1
fi

NINJA_BIN="$(command -v ninja 2>/dev/null || true)"
if [ -z "$NINJA_BIN" ]; then
  echo "ninja is required." >&2
  exit 1
fi

for tool in sha256sum patch; do
  command -v "$tool" >/dev/null 2>&1 || { echo "$tool is required." >&2; exit 1; }
done

grep -qi "Mozilla Public License" "$SOURCE/LICENSE" || {
  echo "The source tree does not contain the expected MPL license." >&2
  exit 1
}

if ! grep -q 'CMAKE_SYSTEM_NAME STREQUAL "Android"' "$SOURCE/CMakeLists.txt"; then
  patch -d "$SOURCE" -p1 -N < "$PATCH_FILE"
fi

echo "Using cmake: $("$CMAKE_BIN" --version | head -n 1)"
echo "Using ninja: $($NINJA_BIN --version)"
echo "Using NDK: $ANDROID_NDK_HOME"

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT" "$JNI_ROOT" "$ROOT/third_party/mgba/notices"
cp "$SOURCE/LICENSE" "$ROOT/third_party/mgba/notices/LICENSE-mGBA-MPL-2.0.txt"

for ABI in $ABIS; do
  BUILD="$OUT_ROOT/$ABI"
  echo "Configuring mGBA libretro for $ABI..."
  "$CMAKE_BIN" -S "$SOURCE" -B "$BUILD" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$NINJA_BIN" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-$ANDROID_PLATFORM" \
    -DANDROID_STL=c++_static \
    -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
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
    -DBUILD_LTO=OFF \
    -DSKIP_GIT=ON \
    -DDISABLE_DEPS=ON

  JOBS=${CMAKE_BUILD_PARALLEL_LEVEL:-$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 2)}
  echo "Building mGBA libretro for $ABI with $JOBS parallel jobs..."
  "$CMAKE_BIN" --build "$BUILD" --parallel "$JOBS" --target mgba_libretro

  CORE=""
  for candidate in "$BUILD/mgba_libretro.so" "$BUILD/libmgba_libretro.so"; do
    if [ -f "$candidate" ]; then
      CORE="$candidate"
      break
    fi
  done
  if [ -z "$CORE" ]; then
    CORE=$(find "$BUILD" -type f \( -name 'mgba_libretro.so' -o -name 'libmgba_libretro.so' -o -name '*libretro*.so' \) | head -n 1)
  fi
  if [ -z "$CORE" ] || [ ! -f "$CORE" ]; then
    echo "No libretro shared library was produced for $ABI." >&2
    find "$BUILD" -maxdepth 3 -type f -name '*.so' 2>/dev/null || true
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
cmake=$("$CMAKE_BIN" --version | head -n 1)
ninja=$($NINJA_BIN --version)
LOCK

echo "Built and staged the pinned mGBA libretro core for: $ABIS"
echo "Run the compatibility and save-integrity gates before distributing an APK."
