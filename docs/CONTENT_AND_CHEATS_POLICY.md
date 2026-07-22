# Content and Cheats Policy

## Commercial games

Retra does not include, mirror, index, or download commercial ROMs. Pokémon FireRed, Emerald, and other commercial GBA games must be imported from a backup the player is legally entitled to use. Patches are applied locally to a compatible user-supplied base ROM.

## Legal one-tap content

Retra may install playable GBA homebrew from a known HTTPS origin when the entry is not classified as a hack ROM and declares usable license metadata. The imported binary is validated as a GBA ROM and its SHA-256 is recorded locally. If a provider publishes an expected digest, Retra should verify it before import; otherwise the UI must not claim the download was checksum-pinned.

## Metadata

Metadata is applied only after exact checksum matching. Filenames, cover art, and title similarity are never trusted as game identity. Canonical metadata and user-edited display titles are kept conceptually separate; future synchronization must not silently erase a player's edits.

## Cheats

- Cheat packs are bound to an exact ROM SHA-256 and may additionally bind to game code and revision.
- Imported RetroArch `.cht` files are converted into Retra Codes; executable scripting is not accepted.
- Placeholder or runtime-selected definitions such as `????` are skipped while concrete supported codes remain available.
- Codes are size-bounded, UTF-8 validated, conflict-checked, and stored atomically.
- Retra creates or requires a protected pre-cheat state before activation.
- A matching checksum does not prove a cheat is correct or safe. The provider, license, source, risk, and reversibility must remain visible.

## Native code

Emulator cores are pinned, reviewed, and packaged at build time. Retra must never download and execute a native core at runtime.
