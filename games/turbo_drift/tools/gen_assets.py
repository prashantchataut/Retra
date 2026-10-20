#!/usr/bin/env python3
"""Generates build/gen/assets.c + assets.h for Turbo Drift from the original
art, track and music definitions in this directory.  Deterministic output."""
import hashlib
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pix import Layer, TileBank, rgb555, sprite_tiles, tile_bytes, tile_bytes8, hexrgb  # noqa: E402
import art_cars as CARS  # noqa: E402
import art_world as WORLD  # noqa: E402
import art_ui as UI  # noqa: E402
import tracks as TR  # noqa: E402
import music as MU  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "build", "gen")
os.makedirs(OUT, exist_ok=True)
HERE = os.path.dirname(os.path.abspath(__file__))

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


# ------------------------------------------------------------------ constants shared with C
SEG = 256
ROAD_W = 975
TEXHALF = ROAD_W * 128 // 112
FOCAL = 128
CAM_H = 860
CAM_BACK = 1280
HORIZON = 64
DRAW = 176
define("SEG_LEN", SEG)
define("ROAD_W", ROAD_W)
define("TEXHALF", TEXHALF)
define("FOCAL", FOCAL)
define("CAM_H", CAM_H)
define("CAM_BACK", CAM_BACK)
define("HORIZON_Y", HORIZON)
define("DRAW_DIST", DRAW)
define("FOG_LEVELS", WORLD.FOG_LEVELS)
define("TEX_VARIANTS", WORLD.VARIANTS)
define("SKY_H", WORLD.SKY_H)

# w16 (Q4 road texture half-width) at the player's depth -> car sprite scale 1.0
W16_PLAYER = 16 * TEXHALF * FOCAL // CAM_BACK
define("W16_PLAYER", W16_PLAYER)
define("K_CAR", 4096 * 256 // W16_PLAYER)

# ------------------------------------------------------------------ car renders (cached)
src_hash = hashlib.sha1()
for fn in ("art_cars.py", "art3d.py", "pix.py"):
    src_hash.update(open(os.path.join(HERE, fn), "rb").read())
cache_dir = os.path.join(OUT, "..", "cache")
os.makedirs(cache_dir, exist_ok=True)
cache_path = os.path.join(cache_dir, "cars-%s.json" % src_hash.hexdigest()[:16])


def lay_to(l):
    return [l.w, l.h, ["".join("%x" % c for c in row) for row in l.px]]


def lay_from(d):
    l = Layer(d[0], d[1])
    l.px = [[int(ch, 16) for ch in row] for row in d[2]]
    return l


if os.path.exists(cache_path):
    with open(cache_path) as f:
        cars = json.load(f)
else:
    cars = {"player": [], "rival": [], "traffic": [], "garage": []}
    for spec in CARS.PLAYER_CARS:
        cars["player"].append([lay_to(l) for l in CARS.player_frames(spec)])
        cars["garage"].append(lay_to(CARS.garage_frame(spec)))
    for spec in CARS.PLAYER_CARS[:3]:
        cars["rival"].append([[lay_to(m) for m in mips] for mips in CARS.rival_frames(spec)])
    for spec in CARS.TRAFFIC_CARS:
        cars["traffic"].append([lay_to(m) for m in CARS.traffic_frames(spec)])
    with open(cache_path, "w") as f:
        json.dump(cars, f)

# ------------------------------------------------------------------ OBJ tile layout (race)
T_PLAYER, T_COUNT, T_FX, T_HUD, T_RIVAL = 0, 64, 96, 136, 144
define("T_PLAYER", T_PLAYER)
define("T_COUNT", T_COUNT)
define("T_FX", T_FX)
define("T_HUD", T_HUD)
define("T_RIVAL", T_RIVAL)
define("T_GARAGE", 0)
define("T_MOCHI", 128)
define("MENU_MM_TILE", 400)

# shape/size codes for OBJ attributes
SHAPE = {(8, 8): (0, 0), (16, 16): (0, 1), (32, 32): (0, 2), (64, 64): (0, 3),
         (16, 8): (1, 0), (32, 8): (1, 1), (32, 16): (1, 2), (64, 32): (1, 3),
         (8, 16): (2, 0), (8, 32): (2, 1), (16, 32): (2, 2), (32, 64): (2, 3)}


def mip_tiles(mips):
    out = bytearray()
    offs = []
    for m in mips:
        offs.append(len(out) // 32)
        out += sprite_tiles(m)
    return out, offs


# rivals: 3 bodies x 2 yaws x 4 mips (64x32, 32x16, 16x8, 8x8) = 43 tiles each
rival = bytearray()
for b in range(3):
    for y in range(2):
        mips = [lay_from(d) for d in cars["rival"][b][y]]
        data, offs = mip_tiles(mips)
        assert offs == [0, 32, 40, 42], offs
        rival += data
carr("unsigned short", "rival_tiles", u16s(rival), 16, "0x%04X")
define("RIVAL_FRAME_TILES", 43)
T_TRAFFIC = T_RIVAL + len(rival) // 32
define("T_TRAFFIC", T_TRAFFIC)

traffic = bytearray()
for t in range(2):
    mips = [lay_from(d) for d in cars["traffic"][t]]
    data, offs = mip_tiles(mips)
    assert offs == [0, 64, 80, 84], offs
    traffic += data
carr("unsigned short", "traffic_tiles", u16s(traffic), 16, "0x%04X")
define("TRAFFIC_FRAME_TILES", 85)
T_SIGNS = T_TRAFFIC + len(traffic) // 32
define("T_SIGNS", T_SIGNS)

# object definitions table (scenery + signs)
OBJDEFS = []      # emitted per env as ObjDef arrays
H.append("typedef struct { unsigned short tile[4]; unsigned char shape[4], size[4], w[4], h[4];"
         " unsigned char pal, solid; unsigned short kscale; } ObjDef;")


def objdef(base_tile, mips, offs, pal, world, solid):
    tiles, shapes, sizes, ws, hs = [], [], [], [], []
    for m, o in zip(mips, offs):
        sh, sz = SHAPE[(m.w, m.h)]
        tiles.append(base_tile + o)
        shapes.append(sh)
        sizes.append(sz)
        ws.append(m.w)
        hs.append(m.h)
    k = int(round(4096.0 * 16 * world / (TEXHALF * mips[0].w)))
    solid_q8 = int(round(solid * 256))
    return (tiles, shapes, sizes, ws, hs, pal, min(255, solid_q8), k)


def objdef_c(d):
    t, sh, sz, ws, hs, pal, solid, k = d
    return "{{%s},{%s},{%s},{%s},{%s},%d,%d,%d}" % (",".join(map(str, t)), ",".join(map(str, sh)),
                                                   ",".join(map(str, sz)), ",".join(map(str, ws)),
                                                   ",".join(map(str, hs)), pal, solid, k)


signs = bytearray()
sign_defs = []
for lay, world, solid in ((WORLD.obj_chevron(False), 900, 0.1), (WORLD.obj_chevron(True), 900, 0.1),
                          (WORLD.obj_start_pylon(), 1500, 0.1)):
    mips = WORLD.mips_of(lay)
    data, offs = mip_tiles(mips)
    sign_defs.append(objdef(T_SIGNS + len(signs) // 32, mips, offs, 15, world, solid))
    signs += data
carr("unsigned short", "sign_tiles", u16s(signs), 16, "0x%04X")
carr("unsigned short", "sign_pal", pal_u16(WORLD.SIGN_PAL), 16, "0x%04X")
T_SCENERY = T_SIGNS + len(signs) // 32
define("T_SCENERY", T_SCENERY)

scen_names = []
for e in range(4):
    objs = WORLD.scenery_set(e)
    data_all = bytearray()
    defs = []
    pals = []
    for i, (name, lay, pal, world, solid) in enumerate(objs):
        mips = WORLD.mips_of(lay)
        data, offs = mip_tiles(mips)
        defs.append(objdef(T_SCENERY + len(data_all) // 32, mips, offs, 9 + i, world, solid))
        data_all += data
        pals += pal_u16(pal)
    assert T_SCENERY + len(data_all) // 32 <= 1024, ("obj vram", e, T_SCENERY + len(data_all) // 32)
    carr("unsigned short", "scen_tiles_%d" % e, u16s(data_all), 16, "0x%04X")
    carr("unsigned short", "scen_pal_%d" % e, pals, 16, "0x%04X")
    allds = defs + sign_defs
    H.append("extern const ObjDef objdefs_%d[%d];" % (e, len(allds)))
    C.append("const ObjDef objdefs_%d[%d] = {%s};" % (e, len(allds), ",".join(objdef_c(d) for d in allds)))
    print("env", e, "scenery tiles", len(data_all) // 32, "vram end", T_SCENERY + len(data_all) // 32)
H.append("extern const unsigned short* const scen_tiles[4];")
C.append("const unsigned short* const scen_tiles[4] = {scen_tiles_0,scen_tiles_1,scen_tiles_2,scen_tiles_3};")
H.append("extern const unsigned short scen_tiles_len[4];")
C.append("const unsigned short scen_tiles_len[4] = {%s};" % ",".join(
    "sizeof(scen_tiles_%d)/2" % e for e in range(4)))
H.append("extern const unsigned short* const scen_pals[4];")
C.append("const unsigned short* const scen_pals[4] = {scen_pal_0,scen_pal_1,scen_pal_2,scen_pal_3};")
H.append("extern const ObjDef* const objdefs[4];")
C.append("const ObjDef* const objdefs[4] = {objdefs_0,objdefs_1,objdefs_2,objdefs_3};")
define("OBJ_CHEV_L", 4)
define("OBJ_CHEV_R", 5)
define("OBJ_PYLON", 6)

# FX + HUD sprites
fx = UI.fx_tiles()
fxb = bytearray()
for key in ("spark", "smoke", "dust", "flame", "streak"):
    define("FX_" + key.upper(), T_FX + len(fxb) // 32)
    for l in fx[key]:
        fxb += sprite_tiles(l)
assert len(fxb) // 32 <= T_HUD - T_FX, len(fxb) // 32
carr("unsigned short", "fx_tiles", u16s(fxb), 16, "0x%04X")
carr("unsigned short", "fx_pal", pal_u16(UI.FX_PAL), 16, "0x%04X")
hudb = bytearray()
for l in UI.dot_tiles():
    hudb += sprite_tiles(l)
carr("unsigned short", "hud_tiles", u16s(hudb), 16, "0x%04X")
carr("unsigned short", "hudspr_pal", pal_u16(UI.HUDSPR_PAL), 16, "0x%04X")
define("HUD_CURSOR", T_HUD + 7)
cd = UI.countdown_layers()
for i, l in enumerate(cd):
    carr("unsigned short", "cd_tiles_%d" % i, u16s(sprite_tiles(l)), 16, "0x%04X")
H.append("extern const unsigned short* const cd_tiles[4];")
C.append("const unsigned short* const cd_tiles[4] = {cd_tiles_0,cd_tiles_1,cd_tiles_2,cd_tiles_3};")

# player frames (128x32 composite = left 64x32 + right 64x32), streamed
NPF = len(CARS.STEER_YAWS) + len(CARS.SPIN_YAWS)
define("PLAYER_FRAMES", NPF)
define("STEER_FRAMES", len(CARS.STEER_YAWS))
for b in range(4):
    data = bytearray()
    for d in cars["player"][b]:
        l = lay_from(d)
        data += sprite_tiles(l.crop(0, 0, 64, 32))
        data += sprite_tiles(l.crop(64, 0, 64, 32))
    carr("unsigned short", "car_frames_%d" % b, u16s(data), 16, "0x%04X")
    g = lay_from(cars["garage"][b])
    gd = sprite_tiles(g.crop(0, 0, 64, 64)) + sprite_tiles(g.crop(64, 0, 64, 64))
    carr("unsigned short", "garage_tiles_%d" % b, u16s(gd), 16, "0x%04X")
H.append("extern const unsigned short* const car_frames[4];")
C.append("const unsigned short* const car_frames[4] = {car_frames_0,car_frames_1,car_frames_2,car_frames_3};")
H.append("extern const unsigned short* const garage_tiles[4];")
C.append("const unsigned short* const garage_tiles[4] = {garage_tiles_0,garage_tiles_1,garage_tiles_2,garage_tiles_3};")

mochib = bytearray()
for m in ("happy", "cheer", "focus", "sad"):
    mochib += sprite_tiles(UI.mochi_portrait(m))
carr("unsigned short", "mochi_tiles", u16s(mochib), 16, "0x%04X")
carr("unsigned short", "mochi_pal", pal_u16(UI.MOCHI_PAL), 16, "0x%04X")

# car palettes: base template + paint ramps
base = CARS.car_palette(CARS.PAINTS[0], driver=True)
carr("unsigned short", "car_pal_base", pal_u16(base), 16, "0x%04X")
carr("unsigned short", "paint_ramps", sum((pal_u16([hexrgb(c) for c in p[1:5]]) for p in CARS.PAINTS), []), 16,
     "0x%04X")
carr("unsigned short", "traffic_ramps", sum((pal_u16([hexrgb(c) for c in p[1:5]]) for p in CARS.TRAFFIC_PAINTS), []),
     16, "0x%04X")
H.append("extern const char* const paint_names[%d];" % len(CARS.PAINTS))
C.append("const char* const paint_names[%d] = {%s};" % (len(CARS.PAINTS), ",".join('"%s"' % p[0] for p in CARS.PAINTS)))
define("NUM_PAINTS", len(CARS.PAINTS))
H.append("extern const char* const car_names[4];")
C.append("const char* const car_names[4] = {%s};" % ",".join('"%s"' % s["name"] for s in CARS.PLAYER_CARS))

# ------------------------------------------------------------------ BG: environments
for e, env in enumerate(WORLD.ENVS):
    rows, rpal = WORLD.road_texture(env)
    # 256x256 texture: rows of 8 px; 32 tile rows (fog*4+variant)
    tiles = {}
    tlist = []
    amap = []
    for r in range(32):
        row = rows[r] if r < len(rows) else [0] * 256
        for tx in range(32):
            t = tuple(row[tx * 8 + x] for y in range(8) for x in range(8))
            if t not in tiles:
                tiles[t] = len(tlist)
                tlist.append(t)
            amap.append(tiles[t])
    assert len(tlist) <= 256, len(tlist)
    tb = bytearray()
    for t in tlist:
        tb += bytes(t)
    carr("unsigned short", "road_tiles_%d" % e, u16s(tb), 16, "0x%04X")
    carr("unsigned char", "road_map_%d" % e, amap, 32)
    carr("unsigned short", "road_pal_%d" % e, pal_u16(rpal), 16, "0x%04X")
    carr("unsigned short", "sky_%d" % e, pal_u16(WORLD.sky_table(env)), 16, "0x%04X")
    g = WORLD.ground_table(env)
    carr("unsigned short", "ground_%d" % e, pal_u16(g[0] + g[1]), 16, "0x%04X")
    lay, spal, band = WORLD.SKYLINES[e]()
    sb = TileBank(0)
    sw, sh, smap = sb.map_layer(lay, 10)
    assert len(sb.tiles) <= 512, len(sb.tiles)
    carr("unsigned short", "skyline_tiles_%d" % e, u16s(sb.data()), 16, "0x%04X")
    carr("unsigned short", "skyline_map_%d" % e, smap, 32, "0x%04X")
    carr("unsigned short", "skyline_pal_%d" % e, pal_u16(spal), 16, "0x%04X")
    print("env", e, "road tiles", len(tlist), "colours", len(rpal), "skyline tiles", len(sb.tiles))
    WORLD.ENVS[e]["_band"] = band

H.append("typedef struct { const unsigned short *road_tiles; const unsigned char *road_map; const unsigned short"
         " *road_pal; const unsigned short *sky, *ground, *skyline_tiles, *skyline_map, *skyline_pal;"
         " unsigned short road_tiles_len, road_pal_len, skyline_tiles_len; unsigned char band; unsigned char song;"
         " const char *name, *time; } EnvDef;")
H.append("extern const EnvDef env_defs[4];")
C.append("const EnvDef env_defs[4] = {")
for e, env in enumerate(WORLD.ENVS):
    C.append('  {road_tiles_%d, road_map_%d, road_pal_%d, sky_%d, ground_%d, skyline_tiles_%d, skyline_map_%d,'
             ' skyline_pal_%d, sizeof(road_tiles_%d)/2, sizeof(road_pal_%d)/2, sizeof(skyline_tiles_%d)/2, %d, %d,'
             ' "%s", "%s"},' % ((e,) * 11 + (env["_band"], 2 + e, env["name"], env["time"])))
C.append("};")

# ------------------------------------------------------------------ UI tiles (BG0, charblock 2)
ui = []
uidx = {}


def add_ui(key, lays):
    uidx[key] = len(ui)
    ui.extend(lays)


add_ui("font", UI.font_tiles())
add_ui("pfont", UI.font_tiles(6))
big = []
for d in "0123456789":
    l = UI.big_digit(d)
    big.append(l.crop(0, 0, 8, 8))
    big.append(l.crop(0, 8, 8, 8))
add_ui("big", big)
add_ui("panel", UI.panel_tiles())
add_ui("bar", UI.bar_tiles())
uib = bytearray()
for t in ui:
    uib += tile_bytes(t, 0, 0)
carr("unsigned short", "ui_tiles", u16s(uib), 16, "0x%04X")
define("UI_FONT", uidx["font"])
define("UI_PFONT", uidx["pfont"])
define("UI_NFONT", len(UI.FONT_CHARS))
define("UI_BIG", uidx["big"])
define("UI_PANEL", uidx["panel"])
define("UI_BAR", uidx["bar"])
UI_EXTRA = len(ui)
define("UI_EXTRA", UI_EXTRA)
fmap = [0] * 128
for i, ch in enumerate(UI.FONT_CHARS):
    fmap[ord(ch)] = i
for ch in "abcdefghijklmnopqrstuvwxyz":
    fmap[ord(ch)] = UI.FONT_CHARS.index(ch.upper())
carr("unsigned char", "font_map", fmap)
carr("unsigned short", "ui_pal_main", pal_u16(UI.UI_MAIN), 16, "0x%04X")
carr("unsigned short", "ui_pal_hi", pal_u16(UI.UI_HILITE), 16, "0x%04X")
carr("unsigned short", "ui_pal_dim", pal_u16(UI.UI_DIM), 16, "0x%04X")
carr("unsigned short", "logo_pal", pal_u16(UI.LOGO_PAL), 16, "0x%04X")


def banner_set(name, layers, base):
    """Tiles for one or more banners placed after UI_EXTRA; returns maps."""
    bank = TileBank(base)
    maps = []
    for lay in layers:
        w = (lay.w + 7) // 8 * 8
        h = (lay.h + 7) // 8 * 8
        pad = Layer(w, h)
        pad.paste(lay)
        tw, th, ents = bank.map_layer(pad, 11)
        ents = [0 if (e & 0x3FF) == base else e for e in ents]
        maps.append((tw, th, ents))
    carr("unsigned short", name + "_tiles", u16s(bank.data()[32:]), 16, "0x%04X")
    return bank, maps


# menu set: logo
lbank, lmaps = banner_set("logo", [UI.logo_layer()], UI_EXTRA - 1)
tw, th, ents = lmaps[0]
define("LOGO_W", tw)
define("LOGO_H", th)
carr("unsigned short", "logo_map", ents, 32, "0x%04X")
print("logo tiles", len(lbank.tiles) - 1)
assert UI_EXTRA + len(lbank.tiles) <= 480
# race set: banners
BANNERS = ["FINISH", "FINAL LAP", "NEW BEST"]
rbank, rmaps = banner_set("banner", [UI.banner_layer(t, 2.2 if len(t) > 6 else 2.6) for t in BANNERS], UI_EXTRA - 1)
H.append("typedef struct { const unsigned short *map; unsigned char w, h; } BannerDef;")
for i, (tw, th, ents) in enumerate(rmaps):
    carr("unsigned short", "banner_map_%d" % i, ents, 32, "0x%04X")
H.append("extern const BannerDef banner_defs[%d];" % len(BANNERS))
C.append("const BannerDef banner_defs[%d] = {%s};" % (len(BANNERS), ",".join(
    "{banner_map_%d,%d,%d}" % (i, m[0], m[1]) for i, m in enumerate(rmaps))))
MM_BASE = UI_EXTRA + len(rbank.tiles) - 1
define("UI_MINIMAP", MM_BASE)
assert MM_BASE + 25 <= 512
print("race banner tiles", len(rbank.tiles) - 1, "minimap base", MM_BASE)

# ------------------------------------------------------------------ tracks
H.append("typedef struct { unsigned short seg; signed char x; unsigned char type; } TrackSprite;")
H.append("typedef struct { const char *name; unsigned char env, laps; unsigned short n, nspr;"
         " const signed char *curve; const short *y; const TrackSprite *spr; const unsigned short *sidx;"
         " const unsigned short *mm_tiles; const unsigned char *mm_pts; } TrackDef;")
for i, t in enumerate(TR.TRACKS):
    carr("signed char", "trk_curve_%d" % i, t.curve, 32, "%d")
    carr("short", "trk_y_%d" % i, [int(round(v)) for v in t.y], 16, "%d")
    C.append("const TrackSprite trk_spr_%d[%d] = {" % (i, len(t.sprites)))
    sidx = [0] * (t.n + 1)
    for j, (seg, x, typ, flip) in enumerate(t.sprites):
        C.append("  {%d,%d,%d}," % (seg, max(-127, min(127, int(round(x * 16)))), typ | (flip << 7)))
    C.append("};")
    H.append("extern const TrackSprite trk_spr_%d[%d];" % (i, len(t.sprites)))
    k = 0
    for s in range(t.n + 1):
        while k < len(t.sprites) and t.sprites[k][0] < s:
            k += 1
        sidx[s] = k
    carr("unsigned short", "trk_sidx_%d" % i, sidx, 32, "%d")
    pts = t.minimap(40, 4)
    mm = UI.minimap_layer(pts, 40)
    lay = Layer(40, 40)
    lay.paste(mm)
    mmb = bytearray()
    for ty in range(5):
        for tx in range(5):
            mmb += tile_bytes(lay, tx, ty)
    carr("unsigned short", "trk_mm_%d" % i, u16s(mmb), 16, "0x%04X")
    lay2 = lay.remap({0: 6})
    mmb2 = bytearray()
    for ty in range(5):
        for tx in range(5):
            mmb2 += tile_bytes(lay2, tx, ty)
    carr("unsigned short", "trk_mmpanel_%d" % i, u16s(mmb2), 16, "0x%04X")
    mp = []
    for s in range(0, t.n, 16):
        x, y = pts[s]
        mp += [int(round(x)), int(round(y))]
    carr("unsigned char", "trk_mmp_%d" % i, mp, 32)
H.append("extern const unsigned short* const trk_mmpanel[%d];" % len(TR.TRACKS))
C.append("const unsigned short* const trk_mmpanel[%d] = {%s};" % (len(TR.TRACKS), ",".join(
    "trk_mmpanel_%d" % i for i in range(len(TR.TRACKS)))))
H.append("extern const TrackDef track_defs[%d];" % len(TR.TRACKS))
define("NUM_TRACKS", len(TR.TRACKS))
C.append("const TrackDef track_defs[%d] = {" % len(TR.TRACKS))
for i, t in enumerate(TR.TRACKS):
    C.append('  {"%s", %d, %d, %d, %d, trk_curve_%d, trk_y_%d, trk_spr_%d, trk_sidx_%d, trk_mm_%d, trk_mmp_%d},'
             % (t.name, t.env, t.laps, t.n, len(t.sprites), i, i, i, i, i, i))
C.append("};")

# ------------------------------------------------------------------ tables
carr("short", "sin_lut", [int(round(math.sin(i * 2 * math.pi / 256) * 256)) for i in range(256)], 16, "%d")
# PA for a road line from w16 (Q4 half width): 128 texels over w px -> 8.8
carr("unsigned short", "recip_w", [min(32767, int(round(128 * 256 * 16 / w))) if w > 0 else 32767
                                   for w in range(4096)], 16, "%d")
# 2^24 / dy (dy in Q4 pixels) for per-line slopes
carr("unsigned int", "inv_dy", [(1 << 24) // i if i else (1 << 24) for i in range(2561)], 16, "%d")
# fog level by dz >> 8
fog = []
for i in range(256):
    dz = i * 256
    f = 0 if dz < 9000 else min(WORLD.FOG_LEVELS - 1, int((dz - 9000) / 4300))
    fog.append(f)
carr("unsigned char", "fog_dz", fog, 32)
# affine: inverse scale for Q8 scale s (index s, 0..1023)
carr("unsigned short", "inv_scale", [min(65535, int(round(65536.0 / s))) if s else 65535 for s in range(1024)], 16,
     "%d")
# quantised matrices: scale 0.5*2^(i/16), i = 0..15 and lookup from Q8 scale 128..255
qs = [0.5 * 2 ** (i / 16.0) for i in range(16)]
carr("unsigned short", "quant_pa", [int(round(256 / q)) for q in qs], 16, "%d")
qmap = []
for s in range(128, 256):
    v = s / 256.0
    qmap.append(max(0, min(15, int(round(16 * math.log2(v / 0.5))))))
carr("unsigned char", "quant_idx", qmap, 32)

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
print("assets written")
