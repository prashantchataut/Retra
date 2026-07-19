# Restricted Catalog Manifest Format

Retra accepts a deliberately small JSON schema for legal, redistributable GBA/homebrew content. It is not a general plugin format.

## Limits and parser behavior

- UTF-8 only; maximum 2 MiB.
- Maximum 512 games.
- Maximum nesting depth 32.
- Duplicate object keys are rejected.
- Unknown top-level or game fields are rejected.
- Catalog/game IDs must match Retra’s safe identifier policy.
- All game downloads must use HTTPS and `.gba` metadata with bounded size and exact SHA-256.
- Creator, license, distribution permission, and source provenance are required.

## Schema example

```json
{
  "catalogVersion": 1,
  "catalogId": "example-homebrew",
  "name": "Example Homebrew",
  "description": "Legally redistributable games from Example Studio.",
  "owner": "Example Studio",
  "sourceUrl": "https://example.org/retra/catalog.json",
  "contentPolicy": "AUTHORIZED_ONLY",
  "games": [
    {
      "id": "example-adventure-1",
      "title": "Example Adventure",
      "description": "A developer-authorized homebrew title.",
      "creator": "Example Studio",
      "version": "1.0.0",
      "downloadUrl": "https://example.org/releases/example-adventure.gba",
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "fileSize": 1048576,
      "license": "Example redistribution license",
      "distributionPermission": "The creator authorizes redistribution through this catalog.",
      "artworkUrl": "https://example.org/art/example-adventure.webp",
      "tags": ["homebrew", "adventure"],
      "compatibility": "UNKNOWN"
    }
  ]
}
```

## Accepted top-level fields

`catalogVersion`, `catalogId`, `name`, `description`, `owner`, `sourceUrl`, `contentPolicy`, `games`.

## Accepted game fields

`id`, `title`, `description`, `creator`, `version`, `downloadUrl`, `sha256`, `fileSize`, `license`, `distributionPermission`, `artworkUrl`, `tags`, `compatibility`.

Commercial games may be represented as metadata elsewhere, but this downloadable manifest must not contain unauthorized commercial ROM links.
