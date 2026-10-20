# GBA metadata (game index + cheat books)

`build_gba_metadata.py` regenerates the two bundled data files from
[libretro-database](https://github.com/libretro/libretro-database) at a pinned commit
(CC BY-SA 4.0; attribution in `app/src/main/assets/licenses/CC-BY-SA-4.0-libretro-database.txt`):

| Output | What | Used by |
|---|---|---|
| `app/src/main/assets/metadata/gba_games.tsv.gz` | `crc32 \t serial \t name` for every No-Intro GBA ROM | box art lookup (`BoxArtRepository`) |
| `app/src/main/assets/metadata/gba_cheats.json.gz` | cheat books keyed by CRC32, serial, or Retra catalog id | `CheatDatabase`, the Cheat book in game details and the player menu |

```sh
PYTHONDONTWRITEBYTECODE=1 python3 tools/metadata/build_gba_metadata.py --cache /tmp/lrdb-cache
```

- Output is deterministic (sorted, gzip mtime 0). Bump `SHA` in the script to update.
- Codes are regrouped to one per line and validated with the app's `CheatParser` rules; when
  `/tmp/retra-host-build/retra-headless` exists, every code is also parsed by the real engine and
  anything it doesn't fully accept is dropped.
- Fan-game cheat files map to catalog ids through the `HACKS` table, never by serial (a hack
  shares its base game's serial but not its RAM layout).

## Box art

Covers for imported cartridges are fetched at runtime (never bundled) from libretro-thumbnails:
`https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/<name>.png`, falling
back to `https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy_Advance/master/Named_Boxarts/<name>.png`.
`<name>` is the No-Intro name with ``&*/:`<>?\|"`` replaced by `_`, then URL-encoded.
