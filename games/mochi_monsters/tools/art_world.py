"""Original overworld art for Mochi Monsters.

Maps are painted at full resolution in RGB (ground + overlay canvases), then
cut into 8x8 tiles. Each tile picks the best-fitting 16-colour BG bank, so the
painting code can use any colour from the world palette freely.
"""
from pix import hexrgb

# ------------------------------------------------------------------ palette banks (BG 1..12)
BANK_HEX = [
    # 1 meadow
    ["24402f", "4a9150", "69b85e", "8ed273", "b9ea92", "285e3e", "3c8a4e", "5bb060",
     "6b4430", "9a6a44", "ff92b6", "fff6ea", "ffd86a", "3a7a44", "e2f7c0"],
    # 2 path
    ["5b3f2a", "c79c66", "ddba85", "eed3a6", "f8e8c8", "4a9150", "69b85e", "8ed273",
     "b9ea92", "a88a6a", "24402f", "8f6a48", "ffffff", "3a7a44", "e9c895"],
    # 3 water
    ["23406e", "3f7fcc", "62a6e6", "9ad3f7", "e6f8ff", "4a9150", "69b85e", "8ed273",
     "ddba85", "eed3a6", "8e8aa2", "24402f", "c4c0d6", "3a7a44", "2f62a8"],
    # 4 house (pink roof)
    ["3b2434", "b64f72", "e07a9a", "f7a8c0", "ffd0de", "d9bc98", "f2ddbf", "fff3e2",
     "6e4630", "a26b45", "4c79b8", "a8ddf7", "e8f8ff", "8f8aa0", "c9c4d8"],
    # 5 tea house (teal roof)
    ["3b2434", "2e8576", "4fb3a0", "86dcc8", "c2f2e6", "d9bc98", "f2ddbf", "fff3e2",
     "6e4630", "a26b45", "4c79b8", "a8ddf7", "e8f8ff", "e07a9a", "ffd86a"],
    # 6 shop (blue roof)
    ["3b2434", "3e62a8", "5f8cd6", "91b9f0", "c9dfff", "d9bc98", "f2ddbf", "fff3e2",
     "6e4630", "a26b45", "4c79b8", "a8ddf7", "e8f8ff", "ffd86a", "8f8aa0"],
    # 7 wood / stone props
    ["3b2a2a", "7a4e34", "b07a4c", "d9a570", "f0cc98", "6a6680", "9692ac", "c4c0d6",
     "e6e4f0", "4a9150", "69b85e", "8ed273", "b9ea92", "ffffff", "24402f"],
    # 8 interior
    ["3b2434", "a8704a", "c8925e", "e0ae78", "f2cc98", "c9a88a", "e2c8ae", "f6e6d2",
     "e07a9a", "f9b3c9", "3c8a4e", "69b85e", "6e4630", "4fb3a0", "fff8ee"],
    # 9 forest
    ["152e22", "1f4a33", "2e6b45", "46905a", "6fb872", "9fd88a", "3a5e2a", "6b4430",
     "9a6a44", "c06aa0", "f0a0d0", "fff0a0", "2a5a3a", "0f2218", "d8f4b0"],
    # 10 cave
    ["241c2e", "4a3e5a", "685a7e", "8e80a6", "b4a8c8", "6e5a50", "8a7466", "a89080",
     "c6ae9a", "e0ccb6", "5fc8e0", "bff4ff", "3a3048", "ffffff", "4a4058"],
    # 11 grove / shrine
    ["3b2434", "c8b8d8", "e8e0f0", "fffaff", "c9962e", "f2c14e", "ffe58a", "ff9ec0",
     "ffd0e0", "4a9150", "69b85e", "8ed273", "b9ea92", "7a5aa0", "a88ad0"],
    # 12 dark / void + night
    ["0c0a14", "1a1626", "2a2438", "3e3650", "5a4e72", "7a6a96", "fff6ea", "ffd86a",
     "9ad3f7", "e6f8ff", "c9a88a", "24402f", "4a9150", "69b85e", "ff92b6"],
]
BANKS = [[hexrgb(h) for h in b] for b in BANK_HEX]
FIRST_BANK = 1          # BG palette bank of BANKS[0]


def C(h):
    return hexrgb(h)


class Canvas:
    def __init__(self, w, h, fill=None):
        self.w, self.h = w, h
        self.px = [[fill] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def rect(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.set(x + i, y + j, c)

    def stamp(self, x, y, art, legend, flip=False):
        for j, row in enumerate(art.strip("\n").split("\n")):
            if flip:
                row = row[::-1]
            for i, ch in enumerate(row):
                c = legend.get(ch)
                if c is not None:
                    self.set(x + i, y + j, c)


def h2(x, y, s=0):
    """Deterministic hash -> 0..255."""
    v = (x * 374761393 + y * 668265263 + s * 2246822519) & 0xFFFFFFFF
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return (v ^ (v >> 16)) & 0xFF


# ------------------------------------------------------------------ colours
G_DK, G_MD, G_LT, G_HL = C("4a9150"), C("69b85e"), C("8ed273"), C("b9ea92")
G_OUT, G_SH = C("24402f"), C("3a7a44")
T_DK, T_MD, T_LT = C("285e3e"), C("3c8a4e"), C("5bb060")
TR_DK, TR_MD = C("6b4430"), C("9a6a44")
P_OUT, P_DK, P_MD, P_LT, P_HL = C("5b3f2a"), C("c79c66"), C("ddba85"), C("eed3a6"), C("f8e8c8")
W_OUT, W_DK, W_MD, W_LT, W_HL, W_DEEP = C("23406e"), C("3f7fcc"), C("62a6e6"), C("9ad3f7"), C("e6f8ff"), C("2f62a8")
FL_P, FL_W, FL_Y = C("ff92b6"), C("fff6ea"), C("ffd86a")
WD_OUT, WD_DK, WD_MD, WD_LT, WD_HL = C("3b2a2a"), C("7a4e34"), C("b07a4c"), C("d9a570"), C("f0cc98")
RK_DK, RK_MD, RK_LT, RK_HL = C("6a6680"), C("9692ac"), C("c4c0d6"), C("e6e4f0")
WHITE = C("ffffff")


# ------------------------------------------------------------------ terrain painters (16x16 cells)
def grass(cv, X, Y, gx, gy, pal=None):
    dk, md, lt, hl = pal or (G_DK, G_MD, G_LT, G_HL)
    cv.rect(X, Y, 16, 16, md)
    v = h2(gx, gy, 1)
    # soft dither speckles + a few blade tufts
    for j in range(16):
        for i in range(16):
            r = h2(X + i, Y + j, 7)
            if r < 10:
                cv.set(X + i, Y + j, lt)
            elif r > 248:
                cv.set(X + i, Y + j, dk)
    if v % 3 == 0:
        tx, ty = 2 + v % 9, 3 + (v >> 3) % 8
        cv.stamp(X + tx, Y + ty, "#.#\n#.#\n.#.", {"#": dk})
        cv.set(X + tx + 1, Y + ty, lt)
    if v % 5 == 1:
        tx, ty = 3 + (v >> 2) % 9, 2 + (v >> 4) % 10
        cv.stamp(X + tx, Y + ty, "h.\n#h", {"#": dk, "h": hl})


def tall_grass(cv, ov, X, Y, gx, gy, pal=None):
    dk, md, lt, hl = pal or (G_DK, G_MD, G_LT, G_HL)
    sh = G_SH if pal is None else pal[0]
    cv.rect(X, Y, 16, 16, dk)
    blade = ("..#....#..\n.#h#..#h#.\n.#h#..#h#.\n#hlh##hlh#\n#hlh##hlh#\n#llh##llh#\n")
    for k, (bx, by) in enumerate(((0, 0), (8, 0), (4, 6), (-4, 6), (12, 6))):
        for j, row in enumerate(blade.strip().split("\n")):
            for i, ch in enumerate(row[:8]):
                c = {"#": sh, "h": md, "l": lt}.get(ch)
                if c:
                    x, y = bx + i, by + j
                    if 0 <= x < 16 and 0 <= y < 16:
                        cv.set(X + x, Y + y, c)
    for i in range(16):
        cv.set(X + i, Y + 15, sh if i % 4 else dk)
    # front blades on the overlay: hide the walker's feet
    front = "#.#..#.#..#.#..#\nh#h##h#h##h#h##h\nlhl##lhl##lhl##l\n#hh##hh###hh##h#"
    for j, row in enumerate(front.split("\n")):
        for i, ch in enumerate(row):
            c = {"#": sh, "h": md, "l": lt}.get(ch)
            if c:
                ov.set(X + i, Y + 11 + j, c)
    cv.set(X + 3, Y + 2, hl)
    cv.set(X + 11, Y + 2, hl)


def flowers(cv, X, Y, gx, gy):
    grass(cv, X, Y, gx, gy)
    v = h2(gx, gy, 3)
    cols = [FL_P, FL_W, FL_Y]
    for k, (fx, fy) in enumerate(((3, 3), (10, 8), (4, 11))):
        c = cols[(v + k) % 3]
        cv.stamp(X + fx, Y + fy, ".c.\ncyc\n.c.\n.g.", {"c": c, "y": FL_Y if c != FL_Y else FL_W, "g": G_DK})


def tree(cv, X, Y, gx, gy, pal=None):
    dk, md, lt = pal or (T_DK, T_MD, T_LT)
    out = G_OUT if pal is None else C("0f2218")
    art = """
.....oooooo.....
...ooLLLLLMoo...
..oLLHLLLMMMMo..
.oLLHHLLLMMMMMo.
.oLLLLLMMMMMMMo.
oLLLLMMMMMMMDMMo
oMLLMMMMMMMDMMDo
oMMMMMMMMMDMMDDo
oDMMMMMMMMMMDDDo
.oDMMMMMMDMDDDo.
.oDDDMMMDDDDDDo.
..ooDDDDDDDDoo..
....ooottooo....
......oTto......
.....ssoTts.....
......ssss......"""
    leg = {"o": out, "L": lt, "M": md, "D": dk, "H": C("9fe08a") if pal is None else pal[2],
           "t": TR_MD, "T": TR_DK, "s": G_SH}
    cv.stamp(X, Y, art, leg)


def path(cv, X, Y, gx, gy, same):
    """Dirt path auto-tiled against grass. same(dx,dy) -> neighbour is path."""
    cv.rect(X, Y, 16, 16, P_MD)
    for j in range(16):
        for i in range(16):
            r = h2(X + i, Y + j, 11)
            if r < 14:
                cv.set(X + i, Y + j, P_LT)
            elif r > 244:
                cv.set(X + i, Y + j, P_DK)
    v = h2(gx, gy, 5)
    if v % 4 == 0:
        cv.stamp(X + 3 + v % 8, Y + 4 + (v >> 4) % 8, ".k.\nkpk", {"k": C("a88a6a"), "p": P_HL})
    # grass edges with a soft dark rim
    n, s, w, e = same(0, -1), same(0, 1), same(-1, 0), same(1, 0)
    for j in range(16):
        for i in range(16):
            d = 99
            if not n: d = min(d, j)
            if not s: d = min(d, 15 - j)
            if not w: d = min(d, i)
            if not e: d = min(d, 15 - i)
            # rounded inner corners
            if not n and not w: d = min(d, int(((i - 4) ** 2 + (j - 4) ** 2) ** 0.5) if i < 4 and j < 4 else 99, max(i, j) if (i < 4 and j < 4) else 99)
            wav = (h2(X + i, Y + j, 13) & 1)
            if d < 2 + wav:
                cv.set(X + i, Y + j, G_MD if d < 1 + wav else G_DK)
            elif d < 3 + wav:
                cv.set(X + i, Y + j, P_DK)
    # outer corners (diagonal neighbours of grass)
    for (dx, dy, cx, cy) in ((-1, -1, 0, 0), (1, -1, 15, 0), (-1, 1, 0, 15), (1, 1, 15, 15)):
        if same(dx, 0) and same(0, dy) and not same(dx, dy):
            for k in range(3):
                for l in range(3 - k):
                    xx = cx - k if dx > 0 else cx + k
                    yy = cy - l if dy > 0 else cy + l
                    cv.set(X + xx, Y + yy, G_MD if k + l < 2 else P_DK)


def water(cv, X, Y, gx, gy, same, t=0):
    cv.rect(X, Y, 16, 16, W_MD)
    for j in range(16):
        for i in range(16):
            if (j + (gx * 16 + i) // 6 + t) % 8 == 0 and h2(X + i, Y + j, 17) < 120:
                cv.set(X + i, Y + j, W_LT)
            elif (j * 3 + i + gy) % 11 == 0 and h2(X + i, Y + j, 19) < 60:
                cv.set(X + i, Y + j, W_DK)
    v = h2(gx, gy, 23)
    if v % 3 == 0:
        cv.stamp(X + 3 + v % 7, Y + 5 + (v >> 3) % 6, ".hh.\nh..h", {"h": W_HL})
    n, s, w, e = same(0, -1), same(0, 1), same(-1, 0), same(1, 0)
    for j in range(16):
        for i in range(16):
            d = 99
            if not n: d = min(d, j)
            if not s: d = min(d, 15 - j)
            if not w: d = min(d, i)
            if not e: d = min(d, 15 - i)
            if d == 0:
                cv.set(X + i, Y + j, G_MD)
            elif d == 1:
                cv.set(X + i, Y + j, W_OUT if (not n and j == 1) else W_DEEP)
            elif d == 2:
                cv.set(X + i, Y + j, W_HL if (h2(X + i, Y + j, 29) & 3) else W_LT)
    for (dx, dy, cx, cy) in ((-1, -1, 0, 0), (1, -1, 15, 0), (-1, 1, 0, 15), (1, 1, 15, 15)):
        if same(dx, 0) and same(0, dy) and not same(dx, dy):
            for k in range(3):
                for l in range(3 - k):
                    xx = cx - k if dx > 0 else cx + k
                    yy = cy - l if dy > 0 else cy + l
                    cv.set(X + xx, Y + yy, G_MD if k + l < 1 else W_DEEP)


def fence(cv, X, Y, gx, gy, same):
    grass(cv, X, Y, gx, gy)
    leg = {"o": WD_OUT, "d": WD_DK, "m": WD_MD, "l": WD_LT, "h": WD_HL, "s": G_SH}
    l, r = same(-1, 0), same(1, 0)
    # rails
    for yy in (5, 10):
        x0 = 0 if l else 5
        x1 = 16 if r else 11
        for i in range(x0, x1):
            cv.set(X + i, Y + yy - 1, WD_OUT)
            cv.set(X + i, Y + yy, WD_LT)
            cv.set(X + i, Y + yy + 1, WD_DK)
            cv.set(X + i, Y + yy + 2, WD_OUT)
    cv.stamp(X + 5, Y + 1, ".oooo.\nohhllo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\nolmmdo\noddddo\n.oooo.\n.ssss.", leg)


def sign(cv, X, Y, gx, gy):
    grass(cv, X, Y, gx, gy)
    leg = {"o": WD_OUT, "d": WD_DK, "m": WD_MD, "l": WD_LT, "h": WD_HL, "s": G_SH}
    cv.stamp(X + 1, Y + 2, """
oooooooooooooo
ohhhhhhhhhhhlo
ohllllllllllmo
olmdddmdddmmdo
olmmmmmmmmmmdo
olmddmddmdmmdo
olmmmmmmmmmmdo
oddddddddddddo
oooooommoooooo
.....odmo.....
.....odmo.....
....ssoossss..""", leg)


def rock(cv, X, Y, gx, gy):
    grass(cv, X, Y, gx, gy)
    cv.stamp(X, Y + 1, """
....oooooo......
..ooLLHHLMoo....
.oLLHHLLMMMMo...
.oLHLLLMMMMMDo..
oLLLLMMMMMMDDDo.
oMLLMMMMMMDMDDo.
oMMMMMMMMDDDDDo.
oDMMMMMMDDDDDDo.
.oDDMMDDDDDDDo..
..ooDDDDDDDoo...
....ooooooo.....
....sssssss.....""", {"o": WD_OUT, "L": RK_LT, "H": RK_HL, "M": RK_MD, "D": RK_DK, "s": G_SH})


def ledge(cv, X, Y, gx, gy, same):
    grass(cv, X, Y, gx, gy)
    for i in range(16):
        cv.set(X + i, Y + 9, G_HL)
        cv.set(X + i, Y + 10, G_LT)
        cv.set(X + i, Y + 11, G_DK)
        cv.set(X + i, Y + 12, G_OUT)
        cv.set(X + i, Y + 13, G_SH)
    if not same(-1, 0):
        cv.set(X, Y + 10, G_DK); cv.set(X, Y + 11, G_OUT)
    if not same(1, 0):
        cv.set(X + 15, Y + 10, G_DK); cv.set(X + 15, Y + 11, G_OUT)


def bridge(cv, X, Y, gx, gy, same):
    water(cv, X, Y, gx, gy, lambda dx, dy: True)
    for j in range(16):
        for i in range(1, 15):
            c = WD_MD if (j % 4) else WD_DK
            if i in (1, 14):
                c = WD_OUT
            elif j % 4 == 1:
                c = WD_LT
            cv.set(X + i, Y + j, c)
