#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
OUT="$ROOT/tools/core-verification/build"
rm -rf "$OUT"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/core/model/src/main/kotlin/app/retra/core/model/GameModels.kt" \
  "$ROOT/core/rom/src/main/kotlin/app/retra/core/rom/GbaRomParser.kt" \
  "$ROOT/core/rom/src/main/kotlin/app/retra/core/rom/Sha256.kt" \
  "$ROOT/core/rom/src/main/kotlin/app/retra/core/rom/DuplicateDetector.kt" \
  "$ROOT/core/rom/src/main/kotlin/app/retra/core/rom/CatalogValidator.kt" \
  "$ROOT/core/emulation/src/main/kotlin/app/retra/core/emulation/EmulationModels.kt" \
  "$ROOT/core/emulation/src/main/kotlin/app/retra/core/emulation/AtomicSaveStore.kt" \
  "$ROOT/core/patching/src/main/kotlin/app/retra/core/patching/PatchEngine.kt" \
  "$ROOT/core/cheats/src/main/kotlin/app/retra/core/cheats/RetraCodes.kt" \
  "$ROOT/core/download/src/main/kotlin/app/retra/core/download/CatalogDownloadPolicy.kt" \
  "$ROOT/core/catalog/src/main/kotlin/app/retra/core/catalog/CatalogManifestJson.kt" \
  "$ROOT/core/achievements/src/main/kotlin/app/retra/core/achievements/Achievements.kt" \
  "$ROOT/core/social/src/main/kotlin/app/retra/core/social/Social.kt" \
  "$ROOT/core/multiplayer/src/main/kotlin/app/retra/core/multiplayer/Multiplayer.kt" \
  "$ROOT/core/multiplayer/src/main/kotlin/app/retra/core/multiplayer/LanTransport.kt" \
  "$ROOT/tools/core-verification/src/Main.kt" \
  -include-runtime -d "$OUT/core-verification.jar"
java -jar "$OUT/core-verification.jar" | tee "$ROOT/tools/core-verification/last-result.txt"
