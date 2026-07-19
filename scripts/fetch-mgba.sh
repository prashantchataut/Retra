#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST="$ROOT/third_party/mgba/upstream"
TAG="0.10.5"
REPOSITORY="https://github.com/mgba-emu/mgba.git"

if ! command -v git >/dev/null 2>&1; then
  echo "git is required." >&2
  exit 1
fi

if [ -e "$DEST" ]; then
  echo "$DEST already exists; refusing to overwrite it." >&2
  exit 1
fi

git clone --depth 1 --branch "$TAG" --recurse-submodules --shallow-submodules "$REPOSITORY" "$DEST"

if [ ! -f "$DEST/LICENSE" ]; then
  echo "Fetched tree has no LICENSE; removing it." >&2
  rm -rf "$DEST"
  exit 1
fi

if ! grep -qi "Mozilla Public License" "$DEST/LICENSE"; then
  echo "Fetched tree does not contain the expected MPL notice; removing it." >&2
  rm -rf "$DEST"
  exit 1
fi

COMMIT=$(git -C "$DEST" rev-parse HEAD)
printf 'repository=%s\ntag=%s\ncommit=%s\n' "$REPOSITORY" "$TAG" "$COMMIT" > "$ROOT/third_party/mgba/SOURCE_LOCK.txt"
echo "Fetched mGBA $TAG at $COMMIT"
echo "Next: follow docs/MGBA_INTEGRATION_PLAN.md and run the Android compatibility matrix before enabling gameplay." 
