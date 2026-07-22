# Retra 2.2 Content, Artwork, and Attribution Policy

## Commercial games

Retra does not bundle, scrape, index, deep-link, or automate downloads of commercial Pokémon ROMs from ROM mirror websites. A credit line does not create redistribution permission. Commercial games enter Retra through a user-selected local backup and are identified by checksum, size, internal game code, and revision.

Retra may display neutral game guides for titles such as FireRed or Emerald so the user understands the supported import, patch, metadata, and cheat workflows. Those guides are not downloadable catalog entries.

## Legal playable catalog

The live playable catalog uses Homebrew Hub's GBA API. An entry is directly installable only when:

- it exposes a playable `.gba` file;
- it is a game, homebrew release, demo, or music release rather than a ROM-hack distribution;
- it declares usable license metadata;
- the request remains on the expected HTTPS host;
- response size is bounded;
- the downloaded bytes parse as a GBA ROM;
- Retra computes and stores the local SHA-256;
- creator, license, and source-page provenance are retained in the library record.

The first Homebrew Hub screenshot is used as the discover preview and, after installation, as local cover artwork. Preview and artwork downloads are bounded and restricted to the provider's HTTPS origin.

## User artwork

A user may attach PNG, JPEG, or WebP artwork to a locally imported game. Artwork is presentation metadata and never participates in game identification. Retra does not silently scrape commercial box art.

## Patches

The supplied Pokémon Heart & Soul v1.2.1 UPS patch is included because it was provided for this project and contains no commercial base ROM. It is SHA-256 checked before use and can only be applied through the review flow to a compatible user-owned Pokémon Emerald backup.

## Cheats

Cheats are matched to exact ROM identity where possible. Imported RetroArch/Libretro `.cht` content is converted to Retra's restricted declarative format. Placeholder and unsupported definitions are rejected or skipped. Retra creates a protected pre-cheat state before activation and labels community cheats as community content rather than guaranteed-safe data.
