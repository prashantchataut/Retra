# Sky Sprout

Original side-scrolling shoot-'em-up made for Retra. Mochi pilots a leaf-copter against the Gloom
Fleet across five sky-garden stages, each with a multi-phase boss. All code, art, music and bullet
patterns are original.

### Controls

| Button | Action |
|---|---|
| D-Pad | Move (8-way) |
| A | Fire (hold for rapid fire) |
| B | Bomb (spends one from your stock; clears bullets and damages everything) |
| L / R | Cycle speed (slow / normal / fast) |
| START | Pause; confirm |
| A / START | Continue on the game-over screen |

### Stages

1. Blossom Skies
2. Storm Front
3. Sunset Reef
4. Clockwork Moon
5. Gloom Fortress

Each stage opens with a title card, runs scripted enemy waves and a mid-stage set piece, then a boss
with a health bar and readable patterns; the last boss is King Gloom, whose armour breaks in phases.
Bosses include the Storm Lantern and the Gloom Sentinel.

### Systems

- Difficulty: Easy, Normal, Hard (lives, bullet density and enemy health).
- Weapons: pea shot as standard, with petal spread, sky laser and homing seed power-ups; bombs.
- Scoring: chains for quick kills, no-miss bonus, per-stage results with enemies, max chain, boss
  bonus and totals, plus a high-score table and stage bests in battery-backed SRAM.
- Extends at score thresholds; three credits to continue.

### Build

Requires `arm-none-eabi-gcc`/`objcopy` and Python 3 with Pillow.

```sh
./build.sh             # build/sky_sprout.gba (+ cover)
./build.sh --install   # also copy the ROM and cover into the app and catalog assets
EXTRA_CFLAGS="-DDEBUG_STAGE=3 -DDEBUG_BOSS -DDEBUG_INVULN -DDEBUG_PERF" ./build.sh   # testing only
```

Builds are deterministic (same sources -> identical ROM).
