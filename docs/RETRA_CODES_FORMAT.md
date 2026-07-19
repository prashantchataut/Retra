# Retra Codes Format

Retra Codes is a UTF-8, declarative cheat-pack format. It contains no scripts, expressions, imports, or executable callbacks.

## Limits

- Maximum file size: 512 KiB.
- Maximum line length: 512 characters.
- Maximum cheats per pack: 512.
- Maximum code lines per cheat: 128.
- Blank lines and lines beginning with `#` are ignored.

## Example

```text
RETRA-CODES-1
provider=Example Provider
gameSha256=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
gameCode=ABCD
revision=0
region=USA

[cheat]
id=max-money
name=Max Money
description=Writes a fixed value to the verified address.
category=CURRENCY
format=RAW
risk=CAUTION
code=02000000:4:000F423F
conflicts=story-safe-money
[/cheat]
```

## Pack fields

Required:

- `provider`
- `gameSha256` — exactly 64 hexadecimal characters

Optional:

- `gameCode` — four letters/digits
- `revision` — integer 0–255
- `region`

Unknown or duplicate fields are rejected.

## Cheat fields

Required:

- `id` — lowercase-safe identifier after normalization
- `name`
- `category` — one of the enum values in `CheatCategory`
- `format` — `GAMESHARK`, `CODEBREAKER`, `ACTION_REPLAY`, or `RAW`
- one or more `code` lines

Optional:

- `description`
- `risk` — `SAFE`, `CAUTION`, or `EXPERIMENTAL`
- `depends` — comma-separated cheat IDs
- `conflicts` — comma-separated cheat IDs

For `RAW`, each line is `AAAAAAAA:W:V`, where `AAAAAAAA` is an eight-digit address, `W` is byte width `1`, `2`, or `4`, and `V` fits that width. Other formats accept bounded hexadecimal code tokens.

## Safety behavior

- Pack SHA-256/game code/revision must match the imported ROM before compatibility is reported.
- Unknown dependencies and conflicts are rejected.
- Profiles detect explicit conflicts and different RAW values targeting the same address/width.
- Import stores data internally but does not activate it in the native core.
- Native activation must create a protected pre-cheat save and record the active pack/profile identity in state metadata.
