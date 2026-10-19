# Mochi Hop

An original Game Boy Advance homebrew platformer made for **Retra**. You play Mochi, a squishy
pink rice-cake with a leaf on top. Hop across pastel cloud islands, candy caves and a starry
night, collect stars and rescue the little sprout friends, who then follow you around.

All code, art, music and level designs are original and live in this directory. No third-party
assets are used and no devkitPro is needed.

## Controls

| Button | Action |
| --- | --- |
| D-Pad | Move (on the map: pick a level) |
| A | Jump. Hold for a higher jump. Hold while stomping to bounce higher. |
| B | Run (on the map: back to the title) |
| START | Pause menu (Resume / Retry / World Map); confirm on menus |

## Levels

| # | World | Name | Stars |
| --- | --- | --- | --- |
| 1-1 | Cloud Meadow | Fluffy Fields | 40 |
| 1-2 | Cloud Meadow | Breezy Bridges | 38 |
| 2-1 | Candy Caves | Gumdrop Grotto | 43 |
| 2-2 | Candy Caves | Sugar Shafts | 38 |
| 3-1 | Starry Night | Twinkle Path | 34 |
| 3-2 | Starry Night | Moonlit Summit | 43 |

Every level has 3 sprouts, at least one checkpoint flag, and a goal star. Clearing a level
unlocks the next one. Clearing 3-2 for the first time plays the credits ("Made for Retra").

Hazards: grumpy raindrops (walk, stompable), bees (fly, stompable), spiky urchins (cannot be
stomped), thorn spikes, and pits. Mochi has 3 hearts. Running out of hearts or falling into a pit
sends you back to the last checkpoint with full hearts, and you keep the stars you have collected.
Touching a checkpoint also refills your hearts. There is no game over.

## Building

Requirements: `arm-none-eabi-gcc`/`objcopy` (tested with 14.2) and Python 3 with Pillow.

```sh
./build.sh             # -> build/mochi_hop.gba + build/mochi_hop_cover.png
./build.sh --install   # also copies ROM + cover to app/src/main/assets/games and catalog/games
```

The build is deterministic: the same sources always give a byte-identical ROM. Debug variants:
`EXTRA_CFLAGS="-DDEBUG_UNLOCK_ALL -DDEBUG_NEAR_GOAL -DDEBUG_SPROUTS" ./build.sh`.

## Layout

```
build.sh              full rebuild (assets -> C -> ROM -> header fix -> cover)
gba.ld                linker script (ROM at 0x08000000, data/bss in IWRAM)
src/crt0.S            ROM header + startup (copies .data, clears .bss, calls main)
src/gba.h             minimal hardware register definitions
src/main.c            scenes: title, world map, credits; save/unlock flow
src/play.c            platforming: physics, collision, actors, camera, HUD, pause, results
src/gfx.c             VRAM loading, shadow OAM, affine Mochi sprite, text/panels
src/audio.c           PSG music sequencer (4 channels) + sound effects
src/save.c            SRAM save (magic + version + checksum); contains the "SRAM_V113" tag
tools/gen_assets.py   generates build/gen/assets.{c,h} from everything below
tools/art_sprites.py  sprite art (procedural shapes + hand-placed ASCII stamps)
tools/art_bg.py       font, UI panels, terrain auto-tiler, parallax layers, logo
tools/levels.py       hand-designed levels (small placement DSL; `python3 levels.py` prints ASCII)
tools/music.py        the original songs (melodies + chord progressions)
tools/make_cover.py   480x320 cover composed from real game layers, 2x nearest-neighbour
tools/fix_header.py   header complement checksum + padding
tools/preview*.py     dev helpers: render sprite sheets / backgrounds / whole levels to PNG
```

## Technical notes

- **Video:** mode 0 with four backgrounds. BG0 is the HUD/text layer. BG1 is the level
  (64x32 map, with tile columns streamed in from ROM as the camera moves). BG2 is the mid
  parallax layer (scrolls at about 1/3 speed) and BG3 is the far sky (1/8 speed, and the clouds
  drift). Fades and the pause dimming use the hardware brightness registers (BLDCNT/BLDY).
  The credits use window 0 to clip the scrolling text.
- **Sprites:** 1D-mapped 4bpp OBJs with 8 palettes. Mochi is a 32x32 affine double-size
  sprite, so squash and stretch is real hardware scaling driven by a damped spring: stretch on
  jump, squash on landing, bonk on ceilings, plus idle breathing. Animation frames cover idle,
  blink, a 4-frame run, jump, fall, happy and hurt.
- **Physics:** 24.8 fixed point with separate ground and air acceleration, a turn boost,
  friction, and a run speed. Jumps get 6 frames of coyote time and a 7-frame jump buffer.
  Jump height is variable: gravity is lighter while A is held on the way up, and there is a
  little hang time at the apex. Stomps bounce Mochi up, and springs launch about 9 tiles.
  One-way cloud platforms can be jumped through from below.
- **Particles:** a pool of 40 for landing and jump dust, sparkles, hearts, splashes and
  confetti.
- **Audio:** four original songs (Title, Cloud Meadow, Candy Caves and a 3/4 Starry Night
  lullaby) plus a clear jingle. Only the melodies and chord symbols are written by hand. The
  arpeggios (square 1), bass (wave channel, soft triangle) and drums (noise) are generated from
  the chords so all channels stay in sync. Sound effects borrow square 1 and the noise channel,
  using hardware frequency sweep for jumps, springs, stomps and hurts.
- **Save:** 32 KiB SRAM at 0x0E000000, written with byte access only. It stores the "MHOP"
  magic, the version, a rotating-xor checksum, the unlocked count, and per level the best stars,
  rescued-sprout mask and cleared flag. The ROM contains `SRAM_V113` so emulators pick the SRAM
  save type.
- **Header:** title `MOCHIHOP`, game code `RMHE`, maker `00`, fixed byte 0x96, and a correct
  complement checksum. The Nintendo logo area is left as zeros (fine for emulators; real
  hardware would refuse to boot it).
- **Robustness:** no BIOS calls and no interrupts. Frame sync polls VCOUNT, so the game does not
  depend on BIOS/HLE details. The code builds for ARMv4T Thumb, and the libgcc division helpers
  it links contain no ARMv5 instructions.
- **Art direction:** soft pastel palettes with 1px plum outlines and light from the top-left.
  Each world has its own terrain palette, but all worlds share the same auto-tiled shapes
  (rounded corners, scalloped grass/frosting, pebble specks).
