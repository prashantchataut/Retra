#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

python3 - "$ROOT" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

root = Path(sys.argv[1]).resolve()

# Only committed files are part of the source-supply-chain policy. A developer may
# keep an ignored keystore locally, and CI may materialize one in $RUNNER_TEMP.
def tracked_files():
    try:
        result = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
        )
        return [root / value.decode("utf-8") for value in result.stdout.split(b"\0") if value]
    except Exception:
        # Archive verification fallback. Ignore generated and local-only locations.
        ignored_parts = {".git", ".gradle", ".idea", "build", ".cxx", ".externalNativeBuild"}
        return [
            path for path in root.rglob("*")
            if path.is_file() and not any(part in ignored_parts for part in path.relative_to(root).parts)
        ]

files = tracked_files()
private_key_suffixes = {".jks", ".keystore", ".p12", ".pfx", ".pem", ".der", ".key"}
committed_keys = [path.relative_to(root) for path in files if path.suffix.lower() in private_key_suffixes]
if committed_keys:
    raise SystemExit(f"Committed signing/private-key material is forbidden: {committed_keys}")

secret_file_names = {"keystore.properties", "signing.properties", "release-signing.properties"}
for path in files:
    if path.name.lower() in secret_file_names:
        text = path.read_text(errors="ignore")
        if re.search(r"(?im)^\s*(?:storePassword|keyPassword)\s*=\s*\S+", text):
            raise SystemExit(f"Committed signing credentials are forbidden: {path.relative_to(root)}")

# Environment/provider-based signing DSL is allowed. Only literal password values
# embedded in Gradle source are rejected.
literal_password = re.compile(
    r"(?m)\b(?:storePassword|keyPassword)\s*=\s*[\"'](?!\$|\{)[^\"']+[\"']"
)
for path in files:
    if path.name not in {"build.gradle", "build.gradle.kts"}:
        continue
    text = path.read_text(errors="ignore")
    if literal_password.search(text):
        raise SystemExit(f"Literal signing password found in {path.relative_to(root)}")

print("PASS signing source policy: no committed key material or literal credentials")
PY
