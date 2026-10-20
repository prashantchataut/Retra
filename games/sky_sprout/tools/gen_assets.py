#!/usr/bin/env python3
"""Generates build/gen/assets.c + assets.h for Sky Sprout from the original art,
stage scripts and music in this directory. Deterministic output."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from pix import Layer, TileBank, rgb555, sprite_tiles, tile_bytes  # noqa: E402
import art_sprites as SP  # noqa: E402
import art_boss as BO  # noqa: E402
import art_bg as BG  # noqa: E402
import stages as ST  # noqa: E402
import music as MU  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "build", "gen")
os.makedirs(OUT, exist_ok=True)

C = []
H = []


def carr(ctype, name, data, per=16, fmt="0x%02X", static=False):
    C.append("const %s %s[%d] __attribute__((aligned(4))) = {" % (ctype, name, len(data)))
    for i in range(0, len(data), per):
        C.append("  " + ",".join(fmt % v for v in data[i:i + per]) + ",")
    C.append("};")
    if not static:
        H.append("extern const %s %s[%d];" % (ctype, name, len(data)))


def u16s(b):
    return [b[i] | (b[i + 1] << 8) for i in range(0, len(b), 2)]


def pal_u16(p):
    return [rgb555(c) for c in p]


def define(name, val):
    H.append("#define %s %d" % (name, val))


H.append("typedef struct { signed char dx, dy; unsigned char ss, pad; unsigned short tile; } Piece;")
H.append("typedef struct { const Piece *p; unsigned char n, pad0, pad1, pad2; } PartFrame;")
H.append("typedef struct { const unsigned short *tiles; unsigned short ntiles; } StageObj;")
H.append("typedef struct { const unsigned short *tiles; unsigned short ntiles; "
         "const unsigned short *far_map, *mid_map, *near_map; unsigned char far_size, mid_size, near_size, pad; "
         "const unsigned short *pals; const unsigned short *grad, *far_f, *near_f; short mid_fx, mid_fy; } StageBG;")
H.append("typedef struct { unsigned short t; unsigned char kind, type, ai, fire, drop, group, gsize, pad; "
         "short x, y, p0, p1, p2; } StageEv;")
H.append("typedef struct { const unsigned char* ch[4]; unsigned char tempo; } SongDef;")

# ================================================================== common sprites
obj = bytearray()


def add_sprites(name, frames):
    start = len(obj) // 32
    for f in frames:
        obj.extend(sprite_tiles(f))
    define("SPR_" + name, start)
    return start


add_sprites("PLAYER", SP.player_frames())
add_sprites("PROP", SP.propeller_frames())
add_sprites("SPROUT", [SP.sproutling_frame(0), SP.sproutling_frame(1)])
add_sprites("HITBOX", [SP.hitbox_frame()])
add_sprites("SHIELD", SP.shield_frames())
add_sprites("PEA", [SP.pea_shot()])
add_sprites("PETAL", [SP.petal_shot(0), SP.petal_shot(1)])
add_sprites("LASER", [SP.laser_seg(0), SP.laser_seg(1)])
add_sprites("SEED", SP.seed_shots())
eb = SP.ebullet_frames()
for c in "mbva":
    add_sprites("EB_S" + c.upper(), [eb["s" + c]])
for c in "mbva":
    add_sprites("EB_M" + c.upper(), [eb["m" + c]])
for c in "mbv":
    add_sprites("EB_R" + c.upper(), [eb["r%s%d" % (c, d)] for d in range(8)])
for c in "mbva":
    add_sprites("EB_B" + c.upper(), [eb["B" + c]])
add_sprites("EB_STARA", [eb["*a0"], eb["*a1"]])
add_sprites("EB_STARM", [eb["*m0"], eb["*m1"]])
add_sprites("PETALPICK", [SP.petal_pickup(0), SP.petal_pickup(1)])
add_sprites("ITEM", [SP.item_frame(k, f) for k in SP.ITEM_KINDS for f in (0, 1)])
add_sprites("EXPL16", [SP.explosion(16, f) for f in range(6)])
add_sprites("EXPL32", [SP.explosion(32, f, 3) for f in range(6)])
add_sprites("SPARK", SP.spark_frames())
add_sprites("PUFF", SP.puff_frames())
add_sprites("BOMBPETAL", [SP.bomb_petal(0), SP.bomb_petal(1)])
add_sprites("POPUP", [SP.popup("x%d" % m) for m in range(2, 9)] + [SP.popup("1UP")])
for name, size, frames in SP.ENEMY_ART:
    add_sprites(name, frames)
common_tiles = len(obj) // 32
STAGE_OBJ_BASE = (common_tiles + 31) // 32 * 32
define("STAGE_OBJ_BASE", STAGE_OBJ_BASE)
carr("unsigned short", "obj_tiles", u16s(obj), 16, "0x%04X")
carr("unsigned short", "obj_pals", sum((pal_u16(p) for p in (SP.PAL_PLAYER, SP.PAL_SHOTS, SP.PAL_EBUL, SP.PAL_FX,
                                                              SP.PAL_ITEM, SP.PAL_FLASH, SP.PAL_HUDSPR)), []),
     16, "0x%04X")
carr("unsigned short", "stage_enemy_pals", sum((pal_u16(a) + pal_u16(b) for a, b in SP.STAGE_ENEMY_PALS), []),
     16, "0x%04X")
print("common obj tiles", common_tiles, "stage base", STAGE_OBJ_BASE)

# ================================================================== bosses / mid-bosses (per-stage OBJ blobs)
SIZES = [(8, 8, 0), (16, 16, 1), (32, 32, 2), (64, 64, 3), (16, 8, 4), (32, 8, 5), (32, 16, 6), (64, 32, 7),
         (8, 16, 8), (8, 32, 9), (16, 32, 10), (32, 64, 11)]


def bbox(L, x0, y0, w, h):
    xs, ys = [], []
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if L.get(x, y):
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return min(xs), min(ys), max(xs), max(ys)


def tile_on(L, tx, ty):
    for y in range(ty * 8, ty * 8 + 8):
        for x in range(tx * 8, tx * 8 + 8):
            if L.get(x, y):
                return True
    return False


def split(L):
    """Covers a layer with OBJ pieces, trading OAM entries against wasted tiles:
    dense 64x64/64x32/32x64 merges, then 32x32 cells shrunk or split into quadrants."""
    tw, th = (L.w + 7) // 8, (L.h + 7) // 8
    on = {(x, y): tile_on(L, x, y) for y in range(th) for x in range(tw)}

    def count(x0, y0, w, h):
        return sum(1 for y in range(y0, y0 + h) for x in range(x0, x0 + w) if on.get((x, y)))
    cw, ch = (tw + 3) // 4, (th + 3) // 4
    used = set()
    pieces = []

    def take(cells, x, y, w, h, ss):
        used.update(cells)
        pieces.append((x * 8, y * 8, w * 8, h * 8, ss, None))
    for cy in range(0, ch - 1, 2):
        for cx in range(0, cw - 1, 2):
            cells = {(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1)}
            if not (cells & used) and count(cx * 4, cy * 4, 8, 8) >= 58:
                take(cells, cx * 4, cy * 4, 8, 8, 3)
    for cy in range(ch):
        for cx in range(cw - 1):
            cells = {(cx, cy), (cx + 1, cy)}
            if not (cells & used) and count(cx * 4, cy * 4, 8, 4) >= 30:
                take(cells, cx * 4, cy * 4, 8, 4, 7)
    for cy in range(ch - 1):
        for cx in range(cw):
            cells = {(cx, cy), (cx, cy + 1)}
            if not (cells & used) and count(cx * 4, cy * 4, 4, 8) >= 30:
                take(cells, cx * 4, cy * 4, 4, 8, 11)

    def best_fit(x0, y0, x1, y1, maxw, maxh):
        bw, bh = x1 - x0 + 1, y1 - y0 + 1
        best = None
        for w, h, ss in SIZES:
            if bw <= w // 8 <= maxw and bh <= h // 8 <= maxh:
                if best is None or w * h < best[0] * best[1]:
                    best = (w, h, ss)
        return best
    for cy in range(ch):
        for cx in range(cw):
            if (cx, cy) in used:
                continue
            tiles = [(x, y) for y in range(cy * 4, cy * 4 + 4) for x in range(cx * 4, cx * 4 + 4) if on.get((x, y))]
            if not tiles:
                continue
            x0 = min(t[0] for t in tiles)
            x1 = max(t[0] for t in tiles)
            y0 = min(t[1] for t in tiles)
            y1 = max(t[1] for t in tiles)
            w, h, ss = best_fit(x0, y0, x1, y1, 4, 4)
            whole = (w // 8) * (h // 8)
            quads = []
            for qy in (0, 2):
                for qx in (0, 2):
                    qt = [t for t in tiles if cx * 4 + qx <= t[0] < cx * 4 + qx + 2 and cy * 4 + qy <= t[1] < cy * 4 + qy + 2]
                    if qt:
                        a0, a1 = min(t[0] for t in qt), max(t[0] for t in qt)
                        b0, b1 = min(t[1] for t in qt), max(t[1] for t in qt)
                        qw, qh, qss = best_fit(a0, b0, a1, b1, 2, 2)
                        quads.append((a0, b0, qw, qh, qss))
            qcost = sum((q[2] // 8) * (q[3] // 8) for q in quads)
            clip = (cx * 32, cy * 32)
            if whole - qcost >= 6 and len(quads) > 1:
                for (a0, b0, qw, qh, qss) in quads:
                    pieces.append((a0 * 8, b0 * 8, qw, qh, qss, clip))
            else:
                pieces.append((x0 * 8, y0 * 8, w, h, ss, clip))
    return pieces


def piece_tiles(L, x0, y0, w, h, clip):
    S = Layer(w, h)
    for y in range(h):
        for x in range(w):
            X, Y = x0 + x, y0 + y
            if clip and not (clip[0] <= X < clip[0] + 32 and clip[1] <= Y < clip[1] + 32):
                continue
            S.px[y][x] = L.get(X, Y)
    return sprite_tiles(S)


piece_stats = []


piece_arrays = []
frame_entries = []     # boss PartFrames (global array)
mid_frame_entries = []
boss_part_first = []
boss_part_pal = []
mid_first = []


def emit_frame(blob, L, label):
    cx, cy = L.w // 2, L.h // 2
    ents = []
    for (x0, y0, w, h, ss, clip) in split(L):
        tile = STAGE_OBJ_BASE + len(blob) // 32
        blob.extend(piece_tiles(L, x0, y0, w, h, clip))
        ents.append((x0 - cx, y0 - cy, ss, tile))
    piece_stats.append((label, len(ents)))
    name = "pc_%s" % label
    C.append("static const Piece %s[%d] = {%s};" % (name, len(ents), ",".join(
        "{%d,%d,%d,0,%d}" % e for e in ents)))
    return name, len(ents)


for s in range(5):
    blob = bytearray()
    name, frames, mpal = BO.MIDBOSSES[s]
    mid_first.append(len(mid_frame_entries))
    for i, f in enumerate(frames):
        mid_frame_entries.append(emit_frame(blob, f, "m%d_%d" % (s, i)))
    B = BO.BOSSES[s]
    firsts, pals = [0, 0, 0, 0], [0, 0, 0, 0]
    for pi, (pname, pframes, slot) in enumerate(B["parts"]):
        firsts[pi] = len(frame_entries)
        pals[pi] = slot
        for i, f in enumerate(pframes):
            frame_entries.append(emit_frame(blob, f, "b%d_%s_%d" % (s, pname, i)))
    boss_part_first.append(firsts)
    boss_part_pal.append(pals)
    nt = len(blob) // 32
    assert STAGE_OBJ_BASE + nt <= 1024, ("stage obj tiles", s, nt)
    carr("unsigned short", "stage_obj_tiles_%d" % s, u16s(blob), 16, "0x%04X", static=True)
    print("stage", s, "boss+mid obj tiles", nt, "-> top", STAGE_OBJ_BASE + nt,
          "pieces", [n for l, n in piece_stats if l.startswith("b%d" % s) or l.startswith("m%d" % s)])
C.append("const StageObj stage_obj[5] = {%s};" % ",".join(
    "{stage_obj_tiles_%d, %d}" % (s, 0) for s in range(5)))
# patch tile counts (computed from array sizes)
C[-1] = "const StageObj stage_obj[5] = {%s};" % ",".join(
    "{stage_obj_tiles_%d, sizeof(stage_obj_tiles_%d) / 32}" % (s, s) for s in range(5))
H.append("extern const StageObj stage_obj[5];")
C.append("const PartFrame boss_frames[%d] = {%s};" % (len(frame_entries), ",".join(
    "{%s,%d,0,0,0}" % e for e in frame_entries)))
H.append("extern const PartFrame boss_frames[%d];" % len(frame_entries))
C.append("const PartFrame mid_frames[%d] = {%s};" % (len(mid_frame_entries), ",".join(
    "{%s,%d,0,0,0}" % e for e in mid_frame_entries)))
H.append("extern const PartFrame mid_frames[%d];" % len(mid_frame_entries))
C.append("const unsigned short boss_part_first[5][4] = {%s};" % ",".join(
    "{%s}" % ",".join(str(v) for v in r) for r in boss_part_first))
H.append("extern const unsigned short boss_part_first[5][4];")
C.append("const unsigned char boss_part_pal[5][4] = {%s};" % ",".join(
    "{%s}" % ",".join(str(v) for v in r) for r in boss_part_pal))
H.append("extern const unsigned char boss_part_pal[5][4];")
carr("unsigned char", "mid_first", mid_first)

bp, bhp, brp, mp, mhp = [], [], [], [], []
for s in range(5):
    B = BO.BOSSES[s]
    for p in B["pals"]:
        bp += pal_u16(p)
        bhp += pal_u16(BO.tint(p, (255, 255, 255), 0.5))
    brp += pal_u16(BO.tint(B["pals"][0], (255, 40, 60), 0.32, skip=(0, 1, 15)))
    mpal = BO.MIDBOSSES[s][2]
    mp += pal_u16(mpal)
    mhp += pal_u16(BO.tint(mpal, (255, 255, 255), 0.5))
carr("unsigned short", "boss_pals", bp, 16, "0x%04X")
carr("unsigned short", "boss_hit_pals", bhp, 16, "0x%04X")
carr("unsigned short", "boss_rage_pals", brp, 16, "0x%04X")
carr("unsigned short", "mid_pals", mp, 16, "0x%04X")
carr("unsigned short", "mid_hit_pals", mhp, 16, "0x%04X")
H.append("extern const char* const boss_names[5];")
C.append("const char* const boss_names[5] = {%s};" % ",".join('"%s"' % b["name"] for b in BO.BOSSES))
H.append("extern const char* const mid_names[5];")
C.append("const char* const mid_names[5] = {%s};" % ",".join('"%s"' % m[0] for m in BO.MIDBOSSES))

# ================================================================== stage backgrounds
def extend(L, h):
    """Copies the last opaque row downward so the vertical camera never shows a gap."""
    N = BG.WrapLayer(L.w, h)
    for y in range(h):
        src = y if y < L.h else L.h - 1
        N.px[y] = L.px[src][:]
    return N


def layer_map(bank, L, palbank):
    """Returns (size_code, entries in SBB order) for a 256 or 512 wide layer."""
    tall = Layer(L.w, 256)
    for y in range(min(256, L.h)):
        tall.px[y] = L.px[y][:]
    tw, th, ents = bank.map_layer(tall, palbank)
    if tw == 32:
        return 0, ents
    assert tw == 64
    out = []
    for half in range(2):
        for y in range(32):
            out += ents[y * 64 + half * 32: y * 64 + half * 32 + 32]
    return 1, out


bg_info = []
for s in range(5):
    st = BG.STAGES[s]()
    far = extend(st["far"], 176)
    near = extend(st["near"], 176)
    mid = st["mid"]
    bank = TileBank(0)
    fs, fmap = layer_map(bank, far, 0)
    ms, mmap = layer_map(bank, mid, 1)
    ns, nmap = layer_map(bank, near, 2)
    assert len(bank.tiles) <= 1024, ("bg tiles", s, len(bank.tiles))
    carr("unsigned short", "bg_tiles_%d" % s, u16s(bank.data()), 16, "0x%04X", static=True)
    carr("unsigned short", "bg_far_%d" % s, fmap, 32, "0x%04X", static=True)
    carr("unsigned short", "bg_mid_%d" % s, mmap, 32, "0x%04X", static=True)
    carr("unsigned short", "bg_near_%d" % s, nmap, 32, "0x%04X", static=True)
    carr("unsigned short", "bg_pals_%d" % s, sum((pal_u16(p) for p in st["pals"]), []), 16, "0x%04X", static=True)
    carr("unsigned short", "bg_grad_%d" % s, [rgb555(c) for c in st["grad"]], 18, "0x%04X", static=True)
    carr("unsigned short", "bg_farf_%d" % s, st["far_f"], 20, "%d", static=True)
    carr("unsigned short", "bg_nearf_%d" % s, st["near_f"], 20, "%d", static=True)
    bg_info.append((len(bank.tiles), fs, ms, ns, int(st["mid_fx"] * 256), int(st["mid_fy"] * 256)))
    print("stage", s, st["name"], "bg tiles", len(bank.tiles))
C.append("const StageBG stage_bgs[5] = {")
for s, (nt, fs, ms, ns, mfx, mfy) in enumerate(bg_info):
    C.append("  {bg_tiles_%d, %d, bg_far_%d, bg_mid_%d, bg_near_%d, %d, %d, %d, 0, bg_pals_%d, bg_grad_%d, "
             "bg_farf_%d, bg_nearf_%d, %d, %d}," % (s, nt, s, s, s, fs, ms, ns, s, s, s, s, mfx, mfy))
C.append("};")
H.append("extern const StageBG stage_bgs[5];")

# ================================================================== UI (CBB2 tiles 0..255)
ui, uidx = BG.ui_tiles()
assert len(ui) <= 256, len(ui)
uib = bytearray()
for t in ui:
    uib.extend(tile_bytes(t, 0, 0))
carr("unsigned short", "ui_tiles", u16s(uib), 16, "0x%04X")
carr("unsigned short", "ui_pal", pal_u16(BG.PAL_UI), 16, "0x%04X")
fmap = [0] * 128
for ch in BG.F.FONT_CHARS:
    fmap[ord(ch)] = BG.F.FONT_CHARS.index(ch)
for ch in "abcdefghijklmnopqrstuvw yz":
    if ch != " ":
        fmap[ord(ch)] = BG.F.FONT_CHARS.index(ch.upper())
carr("unsigned char", "font_map", fmap)
for k, v in uidx.items():
    define("UI_" + k.upper(), v)

# ================================================================== banners (scratch CBB2 tiles 256..511)
BANNER_IDS = {"STAGE 1": "STAGE1", "STAGE 2": "STAGE2", "STAGE 3": "STAGE3", "STAGE 4": "STAGE4",
              "STAGE 5": "STAGE5", "WARNING": "WARNING", "STAGE CLEAR!": "CLEAR", "GAME OVER": "GAMEOVER",
              "PAUSED": "PAUSED", "CONTINUE?": "CONTINUE", "ALL CLEAR!": "ALLCLEAR", "NEW RECORD!": "RECORD",
              "RECORDS": "RECORDS", "THE END": "END", "PRACTICE": "PRACTICE"}
for i, n in enumerate(BG.STAGE_NAMES):
    BANNER_IDS[n] = "NAME%d" % (i + 1)
texts = BG.BANNERS + [str(d) for d in range(10)]
bt = bytearray()
offs, widths = [], []
for i, text in enumerate(texts):
    L = BG.banner_layer(text)
    w = L.w // 8
    assert 2 * w <= 128
    offs.append(len(bt) // 32)
    widths.append(w)
    for ty in range(2):
        for tx in range(w):
            bt.extend(tile_bytes(L, tx, ty))
    ident = BANNER_IDS.get(text, "DIGIT%s" % text if text.isdigit() else None)
    define("BANNER_" + ident, i)
carr("unsigned short", "banner_tiles", u16s(bt), 16, "0x%04X")
carr("unsigned short", "banner_off", offs, 16, "%d")
carr("unsigned char", "banner_w", widths, 16, "%d")
carr("unsigned short", "banner_pals", sum((pal_u16(p) for p in (BG.PAL_BANNER_GOLD, BG.PAL_BANNER_PINK,
                                                                 BG.PAL_BANNER_RED, BG.PAL_BANNER_SKY)), []),
     16, "0x%04X")

# ================================================================== title logo (CBB2 tiles 256.., SBB31)
logo = BG.logo_layer()
pad = Layer(256, 256)
pad.paste(logo, 8, 6)
lb = TileBank(256)
lw, lh, lents = lb.map_layer(pad, 13)
assert len(lb.tiles) <= 256, ("logo tiles", len(lb.tiles))
carr("unsigned short", "logo_tiles", u16s(lb.data()), 16, "0x%04X")
carr("unsigned short", "logo_map", lents, 32, "0x%04X")
carr("unsigned short", "logo_pal", pal_u16(BG.PAL_LOGO), 16, "0x%04X")
print("logo tiles", len(lb.tiles))

# ================================================================== tables
sin = [int(round(math.sin(i * 2 * math.pi / 256) * 256)) for i in range(256)]
carr("short", "sin_lut", sin, 16, "%d")
atan = [int(round(math.atan(i / 64.0) * 128 / math.pi)) for i in range(65)]
carr("unsigned char", "atan_lut", atan, 16, "%d")

# ================================================================== music
carr("unsigned short", "sq_rate", [MU.square_rate(m) for m in range(128)], 16, "%d")
carr("unsigned short", "wave_rate", [MU.wave_rate(m) for m in range(128)], 16, "%d")
for s in MU.SONGS:
    for c in range(4):
        carr("unsigned char", "song_%s_%d" % (s["name"], c), s["ch"][c], 32, static=True)
H.append("extern const SongDef song_defs[%d];" % len(MU.SONGS))
C.append("const SongDef song_defs[%d] = {" % len(MU.SONGS))
for s in MU.SONGS:
    n = s["name"]
    C.append("  {{song_%s_0, song_%s_1, song_%s_2, song_%s_3}, %d}," % (n, n, n, n, s["tempo"]))
C.append("};")
for i, s in enumerate(MU.SONGS):
    define("SONG_" + s["name"].upper(), i)

# ================================================================== stage scripts
for k, n in enumerate(("EV_SPAWN", "EV_MID", "EV_WAIT", "EV_BOSS", "EV_SPEED")):
    define(n, k)
counts = []
for s, evs in enumerate(ST.STAGES):
    C.append("static const StageEv stage_ev_%d[%d] = {" % (s, len(evs)))
    for e in evs:
        t, kind, typ, ai, fire, drop, group, gsize, x, y, p0, p1, p2 = e
        C.append("  {%d,%d,%d,%d,%d,%d,%d,%d,0,%d,%d,%d,%d,%d}," % (t, kind, typ, ai, fire, drop, group, gsize,
                                                                    x, y, p0, p1, p2))
    C.append("};")
    counts.append(len(evs))
    print("stage", s, "events", len(evs), "length", evs[-1][0] // 60, "s (+ mid/boss)")
C.append("const StageEv* const stage_events[5] = {%s};" % ",".join("stage_ev_%d" % s for s in range(5)))
H.append("extern const StageEv* const stage_events[5];")
carr("unsigned short", "stage_event_count", counts, 8, "%d")

with open(os.path.join(OUT, "assets.c"), "w") as f:
    f.write("/* Generated by tools/gen_assets.py - do not edit. */\n#include \"assets.h\"\n")
    f.write("\n".join(C) + "\n")
with open(os.path.join(OUT, "assets.h"), "w") as f:
    f.write("/* Generated by tools/gen_assets.py - do not edit. */\n#ifndef ASSETS_H\n#define ASSETS_H\n")
    f.write("\n".join(H) + "\n#endif\n")
print("assets written")
