#!/usr/bin/env python3
"""Generates build/gen/assets.c + assets.h for Mochi Hop from the original art,
level and music definitions in this directory. Deterministic output."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pix import Layer, TileBank, rgb555, sprite_tiles, tile_bytes  # noqa: E402
import art_sprites as SP  # noqa: E402
import art_bg as BG  # noqa: E402
import levels as LV  # noqa: E402
import music as MU  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "build", "gen")
os.makedirs(OUT, exist_ok=True)

C = []
H = []


def carr(ctype, name, data, per=16, fmt="0x%02X"):
    C.append("const %s %s[%d] __attribute__((aligned(4))) = {" % (ctype, name, len(data)))
    for i in range(0, len(data), per):
        C.append("  " + ",".join(fmt % v for v in data[i:i + per]) + ",")
    C.append("};")
    H.append("extern const %s %s[%d];" % (ctype, name, len(data)))


def u16s(b):
    return [b[i] | (b[i + 1] << 8) for i in range(0, len(b), 2)]


def pal_u16(p):
    return [rgb555(c) for c in p]


def define(name, val):
    H.append("#define %s %d" % (name, val))


# ------------------------------------------------------------------ sprites
obj = bytearray()


def add_sprites(name, frames):
    start = len(obj) // 32
    for f in frames:
        obj.extend(sprite_tiles(f))
    define("SPR_" + name, start)
    return start


add_sprites("MOCHI", SP.mochi_frames())
add_sprites("STAR", SP.star_frames())
add_sprites("DROP", [SP.drop_frame(0), SP.drop_frame(1), SP.drop_frame(flat=True)])
add_sprites("BEE", [SP.bee_frame(0), SP.bee_frame(1)])
add_sprites("SPIKY", [SP.spiky_frame(0), SP.spiky_frame(1)])
add_sprites("SPROUT", [SP.sprout_frame(0), SP.sprout_frame(1)])
add_sprites("FLAG", [SP.flag_frame(i) for i in range(3)])
add_sprites("GOAL", [SP.goal_frame(0), SP.goal_frame(1)])
add_sprites("SPRING", [SP.spring_frame(0), SP.spring_frame(1)])
add_sprites("NODE", [SP.node_frame(i) for i in range(4)])
add_sprites("FX", SP.fx_frames())
assert len(obj) <= 32768
carr("unsigned short", "obj_tiles", u16s(obj), 16, "0x%04X")
carr("unsigned short", "obj_pals", sum((pal_u16(p) for p in SP.SPRITE_PALETTES), []), 16, "0x%04X")

# ------------------------------------------------------------------ UI / font (CBB2, tiles 0..)
ui, uidx = BG.ui_tiles()
assert len(ui) <= 128, len(ui)
uib = bytearray()
for t in ui:
    uib.extend(tile_bytes(t, 0, 0))
carr("unsigned short", "ui_tiles", u16s(uib), 16, "0x%04X")
carr("unsigned short", "ui_pal", pal_u16(BG.PAL_UI), 16, "0x%04X")
fmap = [0] * 128
for ch in BG.FONT_CHARS:
    fmap[ord(ch)] = BG.FONT_CHARS.index(ch)
for ch in "abcdefghijklmnopqrstuvwxyz":
    fmap[ord(ch)] = BG.FONT_CHARS.index(ch.upper())
carr("unsigned char", "font_map", fmap)
define("UI_HUDFONT", uidx["h "])
define("UI_PANFONT", uidx["p "])
for k, v in uidx.items():
    if k.startswith("panel") or k[0] in "ij" or k == "solid":
        define("UI_" + k.upper(), v)
MID_BASE = 128
define("MID_TILE_BASE", MID_BASE)

# ------------------------------------------------------------------ themes
theme_info = []
for t in range(3):
    far, fpal = BG.far_layer(t)
    fb = TileBank(0)
    fw, fh, fmapents = fb.map_layer(far, 4)
    assert (fw, fh) == (32, 32)
    assert len(fb.tiles) <= 512, ("far", t, len(fb.tiles))
    mid, mpal = BG.mid_layer(t)
    mb = TileBank(MID_BASE)
    mw, mh, mmap = mb.map_layer(mid, 5)
    assert (mw, mh) == (64, 32)
    assert len(mb.tiles) + MID_BASE <= 512, ("mid", t, len(mb.tiles))
    carr("unsigned short", "far_tiles_%d" % t, u16s(fb.data()), 16, "0x%04X")
    carr("unsigned short", "far_map_%d" % t, fmapents, 32, "0x%04X")
    carr("unsigned short", "far_pal_%d" % t, pal_u16(fpal), 16, "0x%04X")
    carr("unsigned short", "mid_tiles_%d" % t, u16s(mb.data()), 16, "0x%04X")
    carr("unsigned short", "mid_map_%d" % t, mmap, 32, "0x%04X")
    carr("unsigned short", "mid_pal_%d" % t, pal_u16(mpal), 16, "0x%04X")
    carr("unsigned short", "terrain_pal_%d" % t, pal_u16(BG.THEME_TERRAIN_PALS[t]), 16, "0x%04X")
    theme_info.append((len(fb.tiles), len(mb.tiles)))
    print("theme", t, "far tiles", len(fb.tiles), "mid tiles", len(mb.tiles))

# ------------------------------------------------------------------ levels
levels = LV.all_levels()
terrain_banks = [TileBank(0) for _ in range(3)]
level_meta = []
COLL = {".": 0, "f": 0, "t": 0, "m": 0, "#": 1, "B": 1, "=": 2, "^": 3}
for i, L in enumerate(levels):
    grid = L.grid()
    lay = BG.render_terrain(grid, L.theme)
    tb = terrain_banks[L.theme]
    tw, th, ents = tb.map_layer(lay, 0)
    # column-major, 32 rows per column (pad with 0)
    col = []
    for x in range(tw):
        for y in range(32):
            col.append(ents[y * tw + x] if y < th else 0)
    carr("unsigned short", "lvl_map_%d" % i, col, 32, "0x%04X")
    coll = [COLL[grid[y][x]] for y in range(L.h) for x in range(L.w)]
    carr("unsigned char", "lvl_coll_%d" % i, coll, 64)
    objs = []
    # objects sorted by x for nicer culling
    for t, x, y in sorted(L.objs, key=lambda o: (o[1], o[2])):
        objs += [t, x, y, 0]
    carr("unsigned char", "lvl_objs_%d" % i, objs, 32)
    nstars = sum(1 for t, _, _ in L.objs if t == 1)
    level_meta.append((L.name, L.theme, L.w, L.h, L.start[0], L.start[1], len(L.objs), nstars))
for t, tb in enumerate(terrain_banks):
    assert len(tb.tiles) <= 512, ("terrain", t, len(tb.tiles))
    carr("unsigned short", "terrain_tiles_%d" % t, u16s(tb.data()), 16, "0x%04X")
    print("theme", t, "terrain tiles", len(tb.tiles))

H.append("typedef struct { const char* name; unsigned char theme, w, h, sx, sy, nobjs, nstars;"
         " const unsigned short* map; const unsigned char* coll; const unsigned char* objs; } LevelDef;")
H.append("extern const LevelDef level_defs[%d];" % len(levels))
define("NUM_LEVELS", len(levels))
C.append("const LevelDef level_defs[%d] = {" % len(levels))
for i, m in enumerate(level_meta):
    C.append('  {"%s", %d, %d, %d, %d, %d, %d, %d, lvl_map_%d, lvl_coll_%d, lvl_objs_%d},' % (m + (i, i, i)))
C.append("};")

H.append("typedef struct { const unsigned short *far_tiles, *far_map, *far_pal, *mid_tiles, *mid_map, *mid_pal,"
         " *terrain_tiles, *terrain_pal; unsigned short far_n, mid_n, terrain_n; } ThemeDef;")
H.append("extern const ThemeDef theme_defs[3];")
C.append("const ThemeDef theme_defs[3] = {")
for t in range(3):
    C.append("  {far_tiles_%d, far_map_%d, far_pal_%d, mid_tiles_%d, mid_map_%d, mid_pal_%d, terrain_tiles_%d,"
             " terrain_pal_%d, %d, %d, %d}," % ((t,) * 8 + (theme_info[t][0], theme_info[t][1],
                                                         len(terrain_banks[t].tiles))))
C.append("};")
H.append("extern const char* const theme_names[3];")
C.append("const char* const theme_names[3] = {%s};" % ",".join('"%s"' % n for n in BG.THEME_NAMES))

# ------------------------------------------------------------------ title screen BG1 (logo + island)
title = TileBank(0)
logo = BG.logo_layer()
logo_pad = Layer(240, 56)
logo_pad.paste(logo, 4, 0)
lw, lh, lmap = title.map_layer(logo_pad, 2)
title_grid = [
    "...............",
    "...............",
    "....=====......",
    "..t.......f.m..",
    "...############",
    "..#############",
]
# island at the bottom: 15 metatiles wide, rendered with the meadow terrain style
isl = BG.render_terrain(title_grid, 0)
iw, ih, imap = title.map_layer(isl, 0)
tmap = [0] * (32 * 32)
for y in range(lh):
    for x in range(lw):
        tmap[(y + 2) * 32 + x] = lmap[y * lw + x]
for y in range(ih):
    for x in range(iw):
        ty = 20 - ih + y
        if 0 <= ty < 32:
            tmap[ty * 32 + x] = imap[y * iw + x]
assert len(title.tiles) <= 512, len(title.tiles)
print("title tiles", len(title.tiles))
carr("unsigned short", "title_tiles", u16s(title.data()), 16, "0x%04X")
carr("unsigned short", "title_map", tmap, 32, "0x%04X")
carr("unsigned short", "logo_pal", pal_u16(BG.LOGO_PAL), 16, "0x%04X")

# ------------------------------------------------------------------ misc tables
sin = [int(round(math.sin(i * 2 * math.pi / 256) * 256)) for i in range(256)]
carr("short", "sin_lut", sin, 16, "%d")

# ------------------------------------------------------------------ music
carr("unsigned short", "sq_rate", [MU.square_rate(m) for m in range(128)], 16, "%d")
carr("unsigned short", "wave_rate", [MU.wave_rate(m) for m in range(128)], 16, "%d")
H.append("typedef struct { const unsigned char* ch[4]; unsigned char tempo; } SongDef;")
for s in MU.SONGS:
    for c in range(4):
        carr("unsigned char", "song_%s_%d" % (s["name"], c), s["ch"][c], 32)
H.append("extern const SongDef song_defs[%d];" % len(MU.SONGS))
C.append("const SongDef song_defs[%d] = {" % len(MU.SONGS))
for s in MU.SONGS:
    n = s["name"]
    C.append("  {{song_%s_0, song_%s_1, song_%s_2, song_%s_3}, %d}," % (n, n, n, n, s["tempo"]))
C.append("};")
for i, s in enumerate(MU.SONGS):
    define("SONG_" + s["name"].upper(), i)

with open(os.path.join(OUT, "assets.c"), "w") as f:
    f.write("/* Generated by tools/gen_assets.py - do not edit. */\n#include \"assets.h\"\n")
    f.write("\n".join(C) + "\n")
with open(os.path.join(OUT, "assets.h"), "w") as f:
    f.write("/* Generated by tools/gen_assets.py - do not edit. */\n#ifndef ASSETS_H\n#define ASSETS_H\n")
    f.write("\n".join(H) + "\n#endif\n")
print("obj tiles", len(obj) // 32)
