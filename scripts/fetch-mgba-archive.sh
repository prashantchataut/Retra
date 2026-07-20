#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST="$ROOT/third_party/mgba/upstream"
WORK="$ROOT/third_party/mgba/.fetch"
ARCHIVE="$WORK/mgba_0.10.5+dfsg.orig.tar.xz"
URL="https://deb.debian.org/debian/pool/main/m/mgba/mgba_0.10.5+dfsg.orig.tar.xz"
EXPECTED_SHA256="8aee6705d2dd0fa1cbfdba2c2c475f630001d855b384849a1a6288e9aa376680"

if [ -e "$DEST" ]; then
  echo "$DEST already exists; refusing to overwrite it." >&2
  exit 1
fi
for tool in tar sha256sum; do
  command -v "$tool" >/dev/null 2>&1 || { echo "$tool is required." >&2; exit 1; }
done
if command -v curl >/dev/null 2>&1; then
  FETCH="curl -fL --retry 3 --connect-timeout 20 -o"
elif command -v wget >/dev/null 2>&1; then
  FETCH="wget -O"
else
  echo "curl or wget is required." >&2
  exit 1
fi

rm -rf "$WORK"
mkdir -p "$WORK" "$(dirname "$DEST")"
# shellcheck disable=SC2086
$FETCH "$ARCHIVE" "$URL"
printf '%s  %s\n' "$EXPECTED_SHA256" "$ARCHIVE" | sha256sum -c -
mkdir -p "$WORK/extracted"
tar -xJf "$ARCHIVE" -C "$WORK/extracted"
SOURCE=$(find "$WORK/extracted" -mindepth 1 -maxdepth 1 -type d | head -n 1)
if [ -z "$SOURCE" ] || [ ! -f "$SOURCE/LICENSE" ]; then
  echo "Archive does not contain the expected mGBA source tree." >&2
  rm -rf "$WORK"
  exit 1
fi
grep -qi "Mozilla Public License" "$SOURCE/LICENSE" || {
  echo "Source tree does not contain the expected MPL notice." >&2
  rm -rf "$WORK"
  exit 1
}
mv "$SOURCE" "$DEST"
INIH_DIR="$DEST/src/third-party/inih"
if [ ! -f "$INIH_DIR/ini.h" ]; then
  echo "DFSG archive omitted bundled inih; fetching pinned upstream copy." >&2
  mkdir -p "$INIH_DIR"
  MGBA_TAG_COMMIT="26b7884bc25a5933960f3cdcd98bac1ae14d42e2"
  for inih_file in ini.h ini.c; do
    # shellcheck disable=SC2086
    $FETCH "$INIH_DIR/$inih_file" \
      "https://raw.githubusercontent.com/mgba-emu/mgba/${MGBA_TAG_COMMIT}/src/third-party/inih/${inih_file}"
  done
fi
cat > "$ROOT/third_party/mgba/SOURCE_LOCK.txt" <<LOCK
source=debian-dfsg-archive
url=$URL
version=0.10.5+dfsg
sha256=$EXPECTED_SHA256
fetched_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
LOCK
rm -rf "$WORK"
echo "Fetched and verified mGBA 0.10.5 DFSG source into $DEST"
echo "Next: set ANDROID_NDK_HOME and run scripts/build-mgba-libretro-android.sh"
