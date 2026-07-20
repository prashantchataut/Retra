#!/usr/bin/env bash
set -euo pipefail
if [ -n "${GITHUB_ACTIONS:-}" ]; then
  set -x
fi

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT/third_party/mgba/upstream"
OUT_ROOT="$ROOT/third_party/mgba/android-build"
JNI_ROOT="$ROOT/emulation/native/src/main/jniLibs"
ANDROID_PLATFORM=${ANDROID_PLATFORM:-26}
ABIS=${ABIS:-"arm64-v8a armeabi-v7a x86_64"}
CMAKE_VERSION=${CMAKE_VERSION:-3.22.1}
PATCH_FILE="$ROOT/third_party/mgba/patches/android-posix-vfs.patch"
POPCOUNT_PATCH="$ROOT/third_party/mgba/patches/android-cmake-probes.patch"
MGBA_TAG_COMMIT="26b7884bc25a5933960f3cdcd98bac1ae14d42e2"

apply_mgba_patches() {
  apply_single_patch() {
    local patch_path="$1"
    local label="$2"
    if [ ! -f "$patch_path" ]; then
      return 0
    fi
    if command -v patch >/dev/null 2>&1; then
      if patch -d "$SOURCE" -p1 -N --dry-run < "$patch_path" >/dev/null 2>&1; then
        patch -d "$SOURCE" -p1 -N < "$patch_path"
        return 0
      fi
      echo "patch(1) could not apply $label; trying scripted fallback." >&2
    fi
    return 1
  }

  local file="$SOURCE/CMakeLists.txt"
  if ! grep -q 'CMAKE_SYSTEM_NAME STREQUAL "Android"' "$file"; then
    if apply_single_patch "$PATCH_FILE" "Android POSIX VFS"; then
      :
    else
      sed -i 's/^\([[:space:]]*\)elseif(UNIX)$/\1elseif(UNIX OR CMAKE_SYSTEM_NAME STREQUAL "Android")/' "$file"
    fi
    grep -q 'CMAKE_SYSTEM_NAME STREQUAL "Android"' "$file" || {
      echo "Failed to apply Android POSIX VFS patch to mGBA CMakeLists.txt." >&2
      exit 1
    }
  fi

  if ! grep -q 'if(NOT ANDROID)' "$file" || ! grep -A2 'if(NOT ANDROID)' "$file" | grep -q 'find_function(popcount32)'; then
    if apply_single_patch "$POPCOUNT_PATCH" "Android CMake probes"; then
      :
    else
      sed -i '/find_function(popcount32)/i if(NOT ANDROID)' "$file"
      sed -i '/find_function(popcount32)/a endif()' "$file"
      sed -i '/find_function(snprintf_l)/d' "$file"
      sed -i '/if(NOT ANDROID)/a find_function(snprintf_l)' "$file"
    fi
  fi
  grep -q 'if(NOT ANDROID)' "$file" && grep -A2 'if(NOT ANDROID)' "$file" | grep -q 'find_function(popcount32)' || {
    echo "Failed to apply Android CMake probe patch to mGBA CMakeLists.txt." >&2
    exit 1
  }

  local inih_dir="$SOURCE/src/third-party/inih"
  if [ ! -f "$inih_dir/ini.h" ]; then
    echo "Bundled inih is missing from the DFSG archive; fetching pinned upstream copy." >&2
    mkdir -p "$inih_dir"
    for inih_file in ini.h ini.c; do
      if ! curl -fL --retry 3 --connect-timeout 20 \
        -o "$inih_dir/$inih_file" \
        "https://raw.githubusercontent.com/mgba-emu/mgba/${MGBA_TAG_COMMIT}/src/third-party/inih/${inih_file}"; then
        echo "Failed to fetch required inih file: $inih_file" >&2
        exit 1
      fi
    done
  fi
}

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

resolve_tool() {
  base="$1"
  shift
  for candidate in "$@"; do
    if [ -n "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

CMAKE_BIN="${CMAKE:-}"
if [ -z "$CMAKE_BIN" ]; then
  CMAKE_BIN="$(resolve_tool cmake \
    "$SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake" \
    "$SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake.exe" \
    "$(command -v cmake 2>/dev/null || true)")" || true
fi
if [ -z "$CMAKE_BIN" ] || [ ! -f "$CMAKE_BIN" ]; then
  echo "CMake is required (expected at $SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake)." >&2
  exit 1
fi

NINJA_BIN="$(resolve_tool ninja \
  "$SDK_ROOT/cmake/$CMAKE_VERSION/bin/ninja" \
  "$SDK_ROOT/cmake/$CMAKE_VERSION/bin/ninja.exe" \
  "$(command -v ninja 2>/dev/null || true)")" || true
if [ -z "$NINJA_BIN" ]; then
  echo "ninja is required." >&2
  exit 1
fi

for tool in sha256sum; do
  command -v "$tool" >/dev/null 2>&1 || { echo "$tool is required." >&2; exit 1; }
done

grep -qi "Mozilla Public License" "$SOURCE/LICENSE" || {
  echo "The source tree does not contain the expected MPL license." >&2
  exit 1
}

apply_mgba_patches

echo "ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-unset}"
echo "ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-unset}"
echo "CMAKE_VERSION=${CMAKE_VERSION}"

echo "Using cmake: $("$CMAKE_BIN" --version | head -n 1)"
echo "Using ninja: $($NINJA_BIN --version)"
echo "Using NDK: $ANDROID_NDK_HOME"

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT" "$JNI_ROOT" "$ROOT/third_party/mgba/notices"
: > "$ROOT/third_party/mgba/ANDROID_BINARY_HASHES.txt"
cp "$SOURCE/LICENSE" "$ROOT/third_party/mgba/notices/LICENSE-mGBA-MPL-2.0.txt"

for ABI in $ABIS; do
  BUILD="$OUT_ROOT/$ABI"
  mkdir -p "$BUILD"
  echo "Configuring mGBA libretro for $ABI..."
  if ! "$CMAKE_BIN" -S "$SOURCE" -B "$BUILD" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$NINJA_BIN" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-$ANDROID_PLATFORM" \
    -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_LIBRETRO=ON \
    -DSKIP_LIBRARY=ON \
    -DDISABLE_DEPS=ON \
    -DBUILD_GL=OFF \
    -DBUILD_GLES2=OFF \
    -DBUILD_GLES3=OFF \
    -DUSE_DEBUGGERS=OFF \
    -DENABLE_SCRIPTING=OFF; then
    echo "::error::CMake configure failed for $ABI" >&2
    for log in "$BUILD/CMakeFiles/CMakeError.log" "$BUILD/CMakeFiles/CMakeOutput.log"; do
      if [ -f "$log" ]; then
        echo "---- $log ----" >&2
        tail -n 80 "$log" >&2 || true
      fi
    done
    exit 1
  fi

  JOBS=${CMAKE_BUILD_PARALLEL_LEVEL:-$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 2)}
  echo "Building mGBA libretro for $ABI with $JOBS parallel jobs..."
  if ! "$CMAKE_BIN" --build "$BUILD" --parallel "$JOBS" --target mgba_libretro; then
    echo "::error::CMake build failed for $ABI" >&2
    exit 1
  fi

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
  sha256sum "$JNI_ROOT/$ABI/libmgba_libretro.so" >> "$ROOT/third_party/mgba/ANDROID_BINARY_HASHES.txt"
done

cat > "$ROOT/third_party/mgba/ANDROID_BUILD_LOCK.txt" <<LOCK
android_platform=$ANDROID_PLATFORM
android_ndk_home=$ANDROID_NDK_HOME
abis=$ABIS
cmake=$("$CMAKE_BIN" --version | head -n 1)
ninja=$($NINJA_BIN --version)
LOCK

echo "Built and staged the pinned mGBA libretro core for: $ABIS"
echo "Run the compatibility and save-integrity gates before distributing an APK."
