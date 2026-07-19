#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

if [ "${SKIP_EXECUTION_SUITES:-0}" != "1" ]; then
  "$ROOT/tools/core-verification/run.sh"
  "$ROOT/tools/native-verification/run.sh"
  "$ROOT/tools/libretro-verification/run.sh"
fi

python3 - "$ROOT" <<'PY'
from pathlib import Path
import re
import sys
import tomllib
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
settings = (root / "settings.gradle.kts").read_text()
modules = set(re.findall(r'include\("(:[^"]+)"\)', settings))
required = {":app", ":core:model", ":core:rom", ":core:emulation", ":core:patching", ":core:cheats", ":core:download", ":core:catalog", ":core:achievements", ":core:social", ":core:multiplayer", ":emulation:api", ":emulation:native"}
missing = required - modules
if missing:
    raise SystemExit(f"Missing modules: {sorted(missing)}")
for module in modules:
    path = root.joinpath(*module.lstrip(":").split(":"))
    if not (path / "build.gradle.kts").is_file():
        raise SystemExit(f"Missing build file for {module}")

with (root / "gradle/libs.versions.toml").open("rb") as handle:
    catalog = tomllib.load(handle)
for key in ["agp", "kotlin", "composeBom", "coroutines"]:
    if key not in catalog["versions"]:
        raise SystemExit(f"Missing version catalog key: {key}")

for xml in root.glob("**/src/main/**/*.xml"):
    ET.parse(xml)

for kotlin in root.glob("**/src/main/**/*.kt"):
    source = kotlin.read_text()
    used_icons = set(re.findall(r"Icons\.Default\.([A-Za-z0-9_]+)", source))
    imported_icons = set(re.findall(r"import androidx\.compose\.material\.icons\.filled\.([A-Za-z0-9_]+)", source))
    unresolved = used_icons - imported_icons
    if unresolved:
        raise SystemExit(f"Unresolved material icon imports in {kotlin.relative_to(root)}: {sorted(unresolved)}")

app_module = (root / "app/src/main/kotlin/app/retra/emulator/di/AppModule.kt").read_text()
for provider in ["NativeReferenceEmulationCore", "MgbaLibretroEmulationCore"]:
    if provider not in app_module:
        raise SystemExit(f"Native core provider is not wired: {provider}")
if "fallbackToDestructiveMigration" in app_module:
    raise SystemExit("Destructive database migration is forbidden")
for migration_token in [
    "MIGRATION_1_2", "baseSha256", "patchSha256", "patchFormat", "patchDisplayName",
    "MIGRATION_2_3", "creator", "sourceUrl", "license", "distributionPermission"
]:
    if migration_token not in app_module:
        raise SystemExit(f"Database patch provenance migration is incomplete: {migration_token}")

patch_engine = (root / "core/patching/src/main/kotlin/app/retra/core/patching/PatchEngine.kt").read_text()
for signature in ["PATCH", "UPS1", "BPS1", "validatePatchCrc", "MAX_OUTPUT_SIZE_BYTES"]:
    if signature not in patch_engine:
        raise SystemExit(f"Patch engine capability missing: {signature}")

codes = (root / "core/cheats/src/main/kotlin/app/retra/core/cheats/RetraCodes.kt").read_text()
for token in ["RETRA-CODES-1", "MAX_PACK_SIZE_BYTES", "gameSha256", "CheatConflictAnalyzer", "does not"]:
    if token not in codes:
        raise SystemExit(f"Retra Codes capability missing: {token}")

download_policy = (root / "core/download/src/main/kotlin/app/retra/core/download/CatalogDownloadPolicy.kt").read_text()
for token in ["https", "MAX_DOWNLOAD_BYTES", "MAX_REDIRECTS", "validateRedirect", "validateResponse", "validateCompletedSize"]:
    if token not in download_policy:
        raise SystemExit(f"Secure catalog-download capability missing: {token}")

download_repository = (root / "app/src/main/kotlin/app/retra/emulator/data/CatalogDownloadRepository.kt").read_text()
for token in ["HttpsURLConnection", "instanceFollowRedirects = false", "Accept-Encoding", "fd.sync()", "moveAtomically"]:
    if token not in download_repository:
        raise SystemExit(f"Catalog download executor is incomplete: {token}")

catalog_parser = (root / "core/catalog/src/main/kotlin/app/retra/core/catalog/CatalogManifestJson.kt").read_text()
for token in ["MAX_MANIFEST_BYTES", "MAX_GAMES", "Duplicate object key", "rejectUnknown", "CatalogDownloadPolicy.validateEntry"]:
    if token not in catalog_parser:
        raise SystemExit(f"Restricted catalog parser is incomplete: {token}")

catalog_repository = (root / "app/src/main/kotlin/app/retra/emulator/data/CatalogRepository.kt").read_text()
for token in ["importManifest", "fd.sync()", "moveAtomically", "deleteCatalog", "CatalogManifestJson.parse"]:
    if token not in catalog_repository:
        raise SystemExit(f"Catalog persistence is incomplete: {token}")


achievements = (root / "core/achievements/src/main/kotlin/app/retra/core/achievements/Achievements.kt").read_text()
for token in ["AchievementIntegrityPolicy", "PLAY_SECONDS", "RetraAchievements", "completionRatio"]:
    if token not in achievements:
        raise SystemExit(f"Achievement capability missing: {token}")

social = (root / "core/social/src/main/kotlin/app/retra/core/social/Social.kt").read_text()
for token in ["FriendCode", "SharePrivacy", "SocialShareFactory", "multiplayerInvite"]:
    if token not in social:
        raise SystemExit(f"Social capability missing: {token}")

multiplayer = (root / "core/multiplayer/src/main/kotlin/app/retra/core/multiplayer/Multiplayer.kt").read_text()
transport = (root / "core/multiplayer/src/main/kotlin/app/retra/core/multiplayer/LanTransport.kt").read_text()
for token in ["MultiplayerCompatibilityGate", "MultiplayerPacketCodec", "OrderedPacketBuffer", "RoomCode"]:
    if token not in multiplayer:
        raise SystemExit(f"Multiplayer protocol capability missing: {token}")
for token in ["MultiplayerLanHost", "MAX_FRAME_BYTES", "InternetRelayTransport", "site-local"]:
    if token not in transport:
        raise SystemExit(f"Multiplayer transport capability missing: {token}")

community_ui = (root / "app/src/main/kotlin/app/retra/emulator/CommunityUi.kt").read_text()
for token in ["OnlineCatalogImportCard", "CommunityHub", "Retra achievements", "Multiplayer link architecture"]:
    if token not in community_ui:
        raise SystemExit(f"Community UI capability missing: {token}")

skills = [
    "ui-ux-pro-max", "design-guide", "paperclip-create-agent", "design-taste-frontend", "mobile-android-design"
]
for skill in skills:
    if not (root / ".agents/skills" / skill / "SKILL.md").is_file():
        raise SystemExit(f"Requested skill snapshot missing: {skill}")

api = (root / "emulation/api/src/main/kotlin/app/retra/emulation/api/EmulationCore.kt").read_text()
for contract in ["CoreTier", "latestFrame", "suspendSession", "saveState", "loadState"]:
    if contract not in api:
        raise SystemExit(f"Missing emulation contract: {contract}")

mgba_adapter = (root / "emulation/native/src/main/cpp/libretro_mgba_engine.cpp").read_text()
for token in ["retro_load_game", "retro_serialize", "retro_get_memory_data", "RTLD_NOW", "onInputState", "onAudioBatch"]:
    if token not in mgba_adapter:
        raise SystemExit(f"mGBA/libretro adapter capability missing: {token}")

print("PASS project structure, TOML, XML, icons, migrations, patching, codes, catalogs, achievements, social, multiplayer, requested skill snapshots, DI, and emulation checks")
PY

for script in "$ROOT"/scripts/*.sh "$ROOT"/tools/*/run.sh; do
  sh -n "$script"
done

echo "PASS shell syntax checks"

JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
c++ -std=c++20 -Wall -Wextra -Werror \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -I"$ROOT/emulation/native/src/main/cpp" \
  -c "$ROOT/emulation/native/src/main/cpp/jni_bridge.cpp" \
  -o "$OUT/jni_bridge.o"
echo "PASS JNI bridge host syntax compilation"

c++ -std=c++20 -Wall -Wextra -Werror \
  -I"$ROOT/emulation/native/src/main/cpp" \
  -c "$ROOT/emulation/native/src/main/cpp/libretro_mgba_engine.cpp" \
  -o "$OUT/libretro_mgba_engine.o"
echo "PASS mGBA/libretro adapter host syntax compilation"
