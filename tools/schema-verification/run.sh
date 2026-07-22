#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
SCHEMA_ROOT="$ROOT/app/schemas"
DATABASE_SOURCE="$ROOT/app/src/main/kotlin/app/retra/emulator/data/RetraDatabase.kt"

python3 - "$SCHEMA_ROOT" "$DATABASE_SOURCE" <<'PY'
import json
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
source = pathlib.Path(sys.argv[2]).read_text(encoding='utf-8')
match = re.search(r'@Database\([^)]*version\s*=\s*(\d+)', source, re.S)
if not match:
    raise SystemExit('Could not determine current Room database version.')
current_version = int(match.group(1))

files = sorted(root.rglob('*.json'))
if not files:
    raise SystemExit('No Room schema JSON files found.')
seen_versions = {}
for path in files:
    with path.open('r', encoding='utf-8') as handle:
        payload = json.load(handle)
    assert payload.get('formatVersion') == 1, f'{path}: unsupported formatVersion'
    database = payload.get('database')
    assert isinstance(database, dict), f'{path}: database object missing'
    version = database.get('version')
    assert isinstance(version, int), f'{path}: database version missing'
    assert version not in seen_versions, f'duplicate schema version {version}: {path} and {seen_versions[version]}'
    seen_versions[version] = path
    assert isinstance(database.get('entities'), list), f'{path}: entities array missing'
    identity = database.get('identityHash')
    assert isinstance(identity, str) and re.fullmatch(r'[0-9a-f]{32}', identity), f'{path}: identityHash missing or malformed'
    setup = database.get('setupQueries')
    assert isinstance(setup, list) and any(identity in query for query in setup), f'{path}: setupQueries do not use identityHash'
    for entity in database['entities']:
        assert isinstance(entity.get('tableName'), str) and entity['tableName'], f'{path}: tableName missing'
        assert isinstance(entity.get('createSql'), str) and entity['createSql'].endswith(')'), f'{path}: CREATE TABLE SQL is incomplete'
        assert isinstance(entity.get('fields'), list), f'{path}: fields array missing'
        assert isinstance(entity.get('indices'), list), f'{path}: indices array missing'
    print(f'validated {path}')

assert current_version in seen_versions, (
    f'Current Room schema {current_version}.json is missing. '
    'Compile the app with the Room Gradle plugin and commit the complete generated schema.'
)
latest = max(seen_versions)
assert latest == current_version, f'Latest schema is {latest}, but @Database declares {current_version}'
print(f'Room schema verification passed for {len(files)} file(s); current version is {current_version}.')
PY
