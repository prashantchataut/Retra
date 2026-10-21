# Catalog cover art

Original pixel-art covers for the Game Library entries, drawn in code at 240x160 and saved 2x
(480x320, nearest-neighbour, palettised PNG) to `app/src/main/assets/covers/<catalog id>.png`.
The app shows them for catalog cards and for games installed from the catalog (`CoverArt` in
`ui/GameUi.kt`); Retra Originals use the covers their own build scripts compose from game art.

```sh
PYTHONDONTWRITEBYTECODE=1 python3 tools/covers/make_covers.py --sheet /tmp/covers-sheet.png
```

- `px.py`: canvas, ordered-dither gradients, terrain ridges, shapes, glows, clouds, trees, buildings.
- `font.py`: a chunky 5x7 display font drawn with outline and drop shadow; long titles wrap.
- `make_covers.py`: one scene per entry. Output is deterministic (seeded randomness only).

Art direction: no franchise characters, logos or items. Each cover evokes its game through setting,
mood and colour (Radical Red's volcano, Unbound's aurora, Emerald Rogue's dungeon doors...). The
covers are original work under the app's Apache-2.0 licence.
