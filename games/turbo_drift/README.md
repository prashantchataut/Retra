# Turbo Drift

Original pseudo-3D arcade street racer made for Retra. Eight career events across four neon night
tracks, with drifting, nitro, traffic and a garage you upgrade with your winnings. All code, art,
music and track layouts are original; no real car brands or borrowed assets.

### Controls

| Button | Action |
|---|---|
| A | Accelerate (hold) |
| B | Brake |
| D-Pad / L, R | Steer |
| L (while steering at speed) | Drift; hold through the corner to charge nitro |
| R | Fire nitro (while accelerating) |
| START | Pause; confirm on menus |
| B | Back on menus |

### Modes

| Mode | What it is |
|---|---|
| Career | Eight events on four tracks, in four leagues (Rookie, Street, Pro, Legend). Finish on the podium to unlock the next event; win the last one to become champion. |
| Quick Race | Any unlocked track, any car, three laps against a full grid. |
| Time Attack | Empty track, best lap and best race recorded per track. |
| Attract | Title-screen demo lap. |

### Tracks

Downtown Loop, Harbor Express, Palm Boulevard, Cliffside Run, Red Mesa, Dust Devil, Glacier Road,
Summit Rush — city, coast, desert and snow themes with their own palettes and roadside art.

### Garage

Credits are earned from races. Four cars (each with top speed, acceleration and grip figures) and
four upgrade slots per car (engine, nitro, grip, length), plus a paint choice. Progress, credits,
best laps and best times are saved to battery-backed SRAM.

### Build

Requires `arm-none-eabi-gcc`/`objcopy` and Python 3 with Pillow.

```sh
./build.sh             # build/turbo_drift.gba (+ cover)
./build.sh --install   # also copy the ROM and cover into the app and catalog assets
EXTRA_CFLAGS=-DPERF ./build.sh   # debug build that logs frame timing (never ship)
```

Builds are deterministic (same sources -> identical ROM).
