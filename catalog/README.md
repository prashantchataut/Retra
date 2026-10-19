# Retra Game Library catalog

`manifest.json` is the online Game Library. The app fetches it from
`https://raw.githubusercontent.com/prashantchataut/Retra/main/catalog/manifest.json`.

## Content rules (hard)

- No commercial ROMs, ROM sites, pre-patched ROMs or Nintendo BIOS files. Ever.
- Pokémon fan games are listed only as **patches** (IPS/UPS/BPS). The user applies a patch to a
  base ROM they dumped themselves. The app checks the base ROM's CRC32 before patching.
- A direct `download` for a patch is listed only when the creator hosts the file at a stable HTTPS
  URL (for example a GitHub release asset or the creator's own site). If the patch is only on
  MediaFire, Google Drive, Mega, Ko-fi, PokéCommunity or Discord, `download` is `null` and the app
  opens `homepage` so the user can get it from the creator.
- Homebrew ROMs are direct downloads only when their license allows redistribution. The license is
  given in `license`, and `source` links to the listing that hosts the file.

## Top level

```json
{
  "schema": 1,
  "updated": "2026-10-19",
  "collections": [ Collection, ... ],
  "entries": [ Entry, ... ]
}
```

| Field | Type | Notes |
|---|---|---|
| `schema` | int | Format version. Currently `1`. Clients should ignore manifests with a higher major schema they don't understand. |
| `updated` | string | ISO date (`YYYY-MM-DD`) of the last edit. |
| `collections` | array | Shelves shown in the Library UI, in display order. |
| `entries` | array | All catalog items. |

## Collection

| Field | Type | Notes |
|---|---|---|
| `id` | string | `featured`, `pokemon`, `challenge` or `homebrew`. |
| `title` | string | Shelf heading. |
| `subtitle` | string | Short line under the heading. |
| `entryIds` | string[] | Entry `id`s in display order. An entry may appear in several collections. |

## Entry

| Field | Type | Notes |
|---|---|---|
| `id` | string | Stable unique slug (`[a-z0-9-]`). Use it as the key for downloads and saves. |
| `kind` | `"rom"` \| `"patch"` | `rom` is a playable GBA image (or a zip containing one). `patch` must be applied to `base`. |
| `title` | string | Display title. |
| `author` | string | Creator(s). |
| `version` | string \| null | Latest known release. `null` means unknown, so check the homepage. |
| `description` | string | 1–2 sentences written for this catalog. |
| `tags` | string[] | Lower-case, hyphenated. |
| `status` | string | `complete`, `beta`, `in-development` or `archived`. |
| `license` | string \| null | SPDX identifier for redistributable content. `null` for ROM hacks, which are never redistributed by us. |
| `homepage` | string | Creator's official page or thread. Always present. The app opens it when `download` is `null`. |
| `source` | string \| null | Where the file listing or license metadata comes from, such as a Homebrew Hub page. Informational only. |
| `download` | Download \| null | Direct file. `null` means the user has to get it from `homepage`. |
| `patchFormat` | string \| null | Patches only: `ips`, `ups`, `bps`, or `xdelta` (shown for info only, since the app can't apply xdelta). `null` means unknown. Omitted for roms. |
| `base` | Base \| null | Patches only: the clean ROM the patch expects. `null` for roms. |
| `accent` | string | Pastel card colour `#RRGGBB` (upper-case hex). |
| `featured` | bool | Highlighted card. |
| `bundled` | bool | `true` if the APK also ships this file in assets (Mochi Hop), so the app can play it offline without downloading. |

### Download

| Field | Type | Notes |
|---|---|---|
| `url` | string | HTTPS URL. |
| `size` | int | Exact byte size of the file at `url`. |
| `sha256` | string | Lower-case hex SHA-256 of the file at `url`. The client must reject a mismatch. |
| `format` | string | `gba`, `zip`, `ips`, `ups` or `bps`. |
| `archiveEntry` | string | Only when `format` is `zip`: the name of the single file to extract (`.gba`, `.ips`, `.ups` or `.bps`). |
| `archiveEntrySize` | int | Only for zip: byte size of `archiveEntry`. |
| `archiveEntrySha256` | string | Only for zip: SHA-256 of `archiveEntry`. |

### Base

| Field | Type | Notes |
|---|---|---|
| `title` | string | Human-readable name, region and revision. |
| `gameCode` | string | 4-char cartridge code from the ROM header at 0xAC (`BPEE`, `BPRE`, `AXVE`, ...). |
| `crc32` | string | CRC32 of the whole clean ROM, 8 upper-case hex chars. |
| `size` | int | Byte size of the clean ROM. |

Known bases (all 16,777,216 bytes):

| ROM | Code | CRC32 |
|---|---|---|
| Emerald (U) | BPEE | `1F1C08FB` |
| FireRed (U) v1.0 | BPRE | `DD88761C` |
| FireRed (U) v1.1 | BPRE | `84EE4776` |
| LeafGreen (U) v1.0 | BPGE | `D69C96CC` |
| Ruby (U) v1.0 | AXVE | `F0815EE7` |
| Sapphire (U) v1.0 | AXPE | `554DEDC4` |

## Client behaviour

1. If `download` is present, download it, then check `size` and `sha256`. For zips, extract
   `archiveEntry` and check it too.
2. For a `patch`, ask the user for their base ROM and compare its CRC32 with `base.crc32`.
   UPS and BPS patches also carry the source CRC32 in their footer, and it should match.
   If it doesn't match, explain which revision is needed.
3. If `download` is `null`, show a "Get from creator" button that opens `homepage`, then let the
   user import the patch file they downloaded.

## Maintaining

- Every URL must be checked by hand before it is added. PokéCommunity, Ko-fi and romhacking.net
  answer scripted requests with HTTP 403 (bot protection), but they work in a browser.
- Download each new file to a scratch directory outside the repo, record `size` and `sha256`, and
  for UPS/BPS confirm the embedded source CRC32 matches `base.crc32`.
- Bump `updated` on every change. Never add pre-patched ROMs or links to ROM sites.
