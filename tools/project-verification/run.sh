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
    if "import androidx.compose.foundation.layout.weight" in source:
        raise SystemExit(f"Internal Compose weight import remains in {kotlin.relative_to(root)}")
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

for migration_token in ["MIGRATION_5_6", "sha1", "canonicalTitle", "metadataSource", "index_games_sha1"]:
    if migration_token not in app_module:
        raise SystemExit(f"Database v6 metadata migration is incomplete: {migration_token}")
if "version = 6" not in (root / "app/src/main/kotlin/app/retra/emulator/data/RetraDatabase.kt").read_text():
    raise SystemExit("Room database version 6 is not active")

v2_capabilities = {
    "core/rom/src/main/kotlin/app/retra/core/rom/Sha1.kt": ["MessageDigest", "SHA-1"],
    "core/rom/src/main/kotlin/app/retra/core/rom/LibretroDat.kt": ["LibretroDatParser", "canonicalTitle", "match"],
    "core/cheats/src/main/kotlin/app/retra/core/cheats/RetroArchCheats.kt": ["RetroArchCheatParser", "mapNotNull", "no supported concrete codes"],
    "app/src/main/kotlin/app/retra/emulator/data/HomebrewHubRepository.kt": ["HomebrewHubRepository", "directInstallEligible", "HttpsURLConnection"],
    "app/src/main/kotlin/app/retra/emulator/data/LibretroMetadataRepository.kt": ["LibretroMetadataRepository", "applyCanonicalMetadata", "HttpsURLConnection"],
    "app/src/main/kotlin/app/retra/emulator/data/LibretroCheatRepository.kt": ["LibretroCheatRepository", "RetroArchCheatParser", "HttpsURLConnection"],
}
for relative, tokens in v2_capabilities.items():
    source = (root / relative).read_text()
    for token in tokens:
        if token not in source:
            raise SystemExit(f"Retra v2 capability missing from {relative}: {token}")

if (root / "app/retra-sideload.jks").exists() or "storePassword" in (root / "app/build.gradle.kts").read_text():
    raise SystemExit("A reusable app signing key or password must not be committed")

patch_engine = (root / "core/patching/src/main/kotlin/app/retra/core/patching/PatchEngine.kt").read_text()
for signature in ["PATCH", "UPS1", "BPS1", "validatePatchCrc", "MAX_OUTPUT_SIZE_BYTES"]:
    if signature not in patch_engine:
        raise SystemExit(f"Patch engine capability missing: {signature}")

codes = (root / "core/cheats/src/main/kotlin/app/retra/core/cheats/RetraCodes.kt").read_text()
for token in ["RETRA-CODES-1", "MAX_PACK_SIZE_BYTES", "gameSha256", "CheatConflictAnalyzer", "does not"]:
    if token not in codes:
        raise SystemExit(f"Retra Codes capability missing: {token}")


cheat_catalog = (root / "core/cheats/src/main/kotlin/app/retra/core/cheats/RetraCheatCatalog.kt").read_text()
for token in ["RETRA-CHEAT-INDEX-1", "MAX_CATALOG_BYTES", "RetraCodesDownloadPolicy", "distributionPermission"]:
    if token not in cheat_catalog:
        raise SystemExit(f"Trusted cheat-index capability missing: {token}")
cheat_catalog_repository = (root / "app/src/main/kotlin/app/retra/emulator/data/CheatCatalogRepository.kt").read_text()
for token in ["writeAtomically", "RetraCheatCatalogParser.parse", "compatibleEntries"]:
    if token not in cheat_catalog_repository:
        raise SystemExit(f"Cheat-index persistence capability missing: {token}")
download_policy = (root / "core/download/src/main/kotlin/app/retra/core/download/CatalogDownloadPolicy.kt").read_text()
for token in ["https", "MAX_DOWNLOAD_BYTES", "MAX_REDIRECTS", "validateRedirect", "validateResponse", "validateCompletedSize"]:
    if token not in download_policy:
        raise SystemExit(f"Secure catalog-download capability missing: {token}")

download_repository = (root / "app/src/main/kotlin/app/retra/emulator/data/CatalogDownloadRepository.kt").read_text()
for token in ["HttpsURLConnection", "instanceFollowRedirects = false", "Accept-Encoding", "fd.sync()", "sha256(temporary)", "importVerifiedCatalogFile"]:
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


manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
for permission in ["android.permission.VIBRATE", "android.permission.POST_NOTIFICATIONS"]:
    if permission not in manifest:
        raise SystemExit(f"Feedback/notification permission missing: {permission}")
for sound in ["retra_tap.wav", "retra_confirm.wav", "retra_save.wav", "retra_achievement.wav", "retra_error.wav", "retra_invite.wav"]:
    if not (root / "app/src/main/res/raw" / sound).is_file():
        raise SystemExit(f"Original Retra sound cue missing: {sound}")
feedback = (root / "app/src/main/kotlin/app/retra/emulator/RetraFeedback.kt").read_text()
for token in ["SoundPool", "VibratorManager", "FeedbackCue", "EFFECT_TICK"]:
    if token not in feedback:
        raise SystemExit(f"Semantic feedback capability missing: {token}")
notifications = (root / "app/src/main/kotlin/app/retra/emulator/RetraNotifications.kt").read_text()
for token in ["NotificationChannel", "CHANNEL_ACHIEVEMENTS", "CHANNEL_DOWNLOADS", "CHANNEL_MULTIPLAYER", "POST_NOTIFICATIONS"]:
    if token not in notifications:
        raise SystemExit(f"Notification capability missing: {token}")
glass = (root / "app/src/main/kotlin/app/retra/emulator/GlassUi.kt").read_text()
for token in ["RetraBackdrop", "GlassPanel", "LocalRetraSettings", "RetraAnimatedContent", "reduceTransparency", "Modifier.blur", "glassIntensity"]:
    if token not in glass:
        raise SystemExit(f"Premium glass design capability missing: {token}")

branding = (root / "branding/retra-logo.svg")
if not branding.is_file() or "Portal and Save Core" not in branding.read_text():
    raise SystemExit("Retra Portal / Save Core brand source is missing or undocumented")
for asset in [
    "app/src/main/res/drawable-nodpi/retra_logo.png",
    "app/src/main/res/drawable/ic_retra_foreground.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/kotlin/app/retra/emulator/auth/GoogleAuthRepository.kt",
]:
    if not (root / asset).is_file():
        raise SystemExit(f"Brand/onboarding/account asset missing: {asset}")
auth = (root / "app/src/main/kotlin/app/retra/emulator/auth/GoogleAuthRepository.kt").read_text()
for token in ["GetSignInWithGoogleOption", "setNonce", "GoogleIdTokenCredential.createFrom", "clearCredentialState", "idToken", "tokenFingerprint"]:
    if token not in auth:
        raise SystemExit(f"Google identity capability missing: {token}")
settings_model = (root / "core/model/src/main/kotlin/app/retra/core/model/GameModels.kt").read_text()
for token in ["screenScalingMode", "displaySmoothing", "showTouchControls", "touchControlScale", "controlLayoutPreset", "autoSaveIntervalMinutes", "audioEnabled", "masterVolume", "autoSuspendOnBackground"]:
    if token not in settings_model:
        raise SystemExit(f"Functional emulator setting missing: {token}")
player = (root / "app/src/main/kotlin/app/retra/emulator/PlayerUi.kt").read_text()
surface = (root / "app/src/main/kotlin/app/retra/emulator/EmulationSurfaceView.kt").read_text()
for token in ["showPerformanceOverlay", "showTouchControls", "screenScalingMode", "touchControlScale", "controlLayoutPreset", "autoSaveIntervalMinutes", "displaySmoothing"]:
    if token not in player:
        raise SystemExit(f"Player does not consume emulator setting: {token}")
for token in ["configure", "scalingMode", "ScreenScalingMode.FILL", "isFilterBitmap", "floor"]:
    if token not in surface:
        raise SystemExit(f"Video presenter capability missing: {token}")

dead_ui = [
    "RetraUi.kt", "RetraFinalExperienceUi.kt", "RetraFinalDiscoverUi.kt",
    "OnboardingUi.kt", "CommunityUi.kt", "ProfileUi.kt", "NotificationSettingsUi.kt"
]
for filename in dead_ui:
    if (root / "app/src/main/kotlin/app/retra/emulator" / filename).exists():
        raise SystemExit(f"Legacy duplicate UI must not remain in the active source tree: {filename}")

v22_ui = (root / "app/src/main/kotlin/app/retra/emulator/RetraV22Ui.kt").read_text()
for token in ["RetraV22Root", "Prashant Chataut", "A library, not a storefront", "Playable homebrew", "Save Health Center", "Pokémon Heart & Soul"]:
    if token not in v22_ui:
        raise SystemExit(f"Retra 2.2 product surface missing: {token}")
main_activity = (root / "app/src/main/kotlin/app/retra/emulator/MainActivity.kt").read_text()
if "RetraV22Root" not in main_activity:
    raise SystemExit("Retra 2.2 root is not active")
build_file = (root / "app/build.gradle.kts").read_text()
for token in ['versionName = "2.2.0"', "alias(libs.plugins.room)", "schemaDirectory"]:
    if token not in build_file:
        raise SystemExit(f"Retra 2.2 build hardening missing: {token}")
bundled_patch = root / "app/src/main/assets/patches/pokemon_hns_v1_2_1.ups"
if not bundled_patch.is_file() or bundled_patch.stat().st_size != 32558217:
    raise SystemExit("Reviewed Heart & Soul v1.2.1 patch asset is missing or changed")

if not (root / "scripts/fetch-mgba-archive.sh").is_file():
    raise SystemExit("Pinned mGBA archive fetch script is missing")

backup = (root / "app/src/main/kotlin/app/retra/emulator/data/BackupRepository.kt").read_text()
for token in ["RETRA-BACKUP", "romsIncluded", "AtomicSaveStore", "MAX_TOTAL_IMPORT_BYTES", "settingsRepository.replace", "achievementRepository.importProgress"]:
    if token not in backup:
        raise SystemExit(f"Portable ROM-free backup capability missing: {token}")
vault = (root / "app/src/main/kotlin/app/retra/emulator/data/VaultRepository.kt").read_text()
for token in ["VaultHealthSummary", "corruptedRecords", "backupCount", "restorePrevious"]:
    if token not in vault:
        raise SystemExit(f"Save Health capability missing: {token}")
main_activity = (root / "app/src/main/kotlin/app/retra/emulator/MainActivity.kt").read_text()
for token in ["routeExternalIntent", "ACTION_SEND", "queueExternalImport", "onNewIntent"]:
    if token not in main_activity:
        raise SystemExit(f"External import review path missing: {token}")
for token in ["Review external file", "confirmExternalImport", "dismissExternalImport"]:
    if token not in v22_ui:
        raise SystemExit(f"External import confirmation UI missing: {token}")
rewind = (root / "core/emulation/src/main/kotlin/app/retra/core/emulation/RewindBuffer.kt").read_text()
for token in ["maximumBytes", "snapshotCount", "copyOf", "Not enough rewind history"]:
    if token not in rewind:
        raise SystemExit(f"Rewind buffer capability missing: {token}")
for core in [
    root / "emulation/native/src/main/kotlin/app/retra/emulation/nativecore/MgbaLibretroEmulationCore.kt",
    root / "emulation/native/src/main/kotlin/app/retra/emulation/nativecore/NativeReferenceEmulationCore.kt",
]:
    text = core.read_text()
    for token in ["supportsRewind = true", "RewindBuffer", "captureRewindSnapshot", "override fun rewind"]:
        if token not in text:
            raise SystemExit(f"Rewind integration missing from {core.name}: {token}")
screenshot = (root / "app/src/main/kotlin/app/retra/emulator/data/ScreenshotRepository.kt").read_text()
for token in ["MediaStore", "IS_PENDING", "Bitmap.CompressFormat.PNG", "fd.sync()"]:
    if token not in screenshot:
        raise SystemExit(f"Screenshot capability missing: {token}")
artwork = (root / "app/src/main/kotlin/app/retra/emulator/data/ArtworkRepository.kt").read_text()
for token in ["MAX_SOURCE_BYTES", "BitmapFactory", "compress", "Files.move", "setCoverArt"]:
    if token not in artwork:
        raise SystemExit(f"Artwork capability missing: {token}")
for token in ["favorite", "notes", "coverArtPath"]:
    if token not in (root / "core/model/src/main/kotlin/app/retra/core/model/GameModels.kt").read_text():
        raise SystemExit(f"Custom library metadata missing: {token}")

api = (root / "emulation/api/src/main/kotlin/app/retra/emulation/api/EmulationCore.kt").read_text()
for contract in ["CoreTier", "latestFrame", "suspendSession", "saveState", "loadState"]:
    if contract not in api:
        raise SystemExit(f"Missing emulation contract: {contract}")

mgba_adapter = (root / "emulation/native/src/main/cpp/libretro_mgba_engine.cpp").read_text()
for token in ["retro_load_game", "retro_serialize", "retro_get_memory_data", "RTLD_NOW", "onInputState", "onAudioBatch"]:
    if token not in mgba_adapter:
        raise SystemExit(f"mGBA/libretro adapter capability missing: {token}")

print("PASS Retra 2.2 structure, UI, player customization, Room schema hardening, patching, achievements, catalogs, DI, and emulation checks")
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
