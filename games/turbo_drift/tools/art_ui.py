"""Original UI art for Turbo Drift: font, HUD gauges, panels, logo, Mochi
portraits, effects and countdown sprites."""
import math

from pix import Layer, pal16
from draw import poly, ellipse, thick_line, ramp_pick, Rng

# ------------------------------------------------------------------ palettes (BG banks 13-15, OBJ banks 13-15)
# UI bank layout: 1 outline, 2 text top, 3 text bottom, 4 panel border light, 5 panel border,
# 6 panel fill, 7 panel fill dark, 8 accent pink, 9 accent cyan, 10 bar green, 11 bar yellow,
# 12 bar red, 13 bar empty, 14 icon gold, 15 white
UI_MAIN = pal16(["000000", "0e0a1c", "ffffff", "c8c4e8", "8a7cff", "4a3aa0", "1e1840", "15112e",
                 "ff4aa8", "34e0ff", "5cf08a", "ffd23c", "ff4a5a", "2e2a4e", "ffcc4a", "ffffff"])
UI_HILITE = pal16(["000000", "1c0a10", "fff2a0", "ffb43c", "ffb6e0", "ff4aa8", "1e1840", "2a1240",
                   "ff4aa8", "34e0ff", "5cf08a", "ffd23c", "ff4a5a", "2e2a4e", "ffcc4a", "ffffff"])
UI_DIM = pal16(["000000", "0e0a1c", "8a86a8", "6a6688", "5a5280", "3a3270", "1e1840", "141030",
                "a05a88", "4a8aa0", "4a8a60", "a08a40", "a04a50", "24203e", "a08a40", "a8a8c0"])
LOGO_PAL = pal16(["000000", "140a24", "3a1458", "ffffff", "c8f2ff", "6ad0ff", "3a72e0", "2a2a90",
                  "fff2a0", "ffb43c", "ff5a7a", "d02a8a", "ffffff", "28e4ff", "ff3cac", "fff6ff"])

# ------------------------------------------------------------------ font (5x6 glyphs, outlined)
FONT_CHARS = (" ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:;!?-+/'\"%()#*<>=_$@&[]{}|~^")
GLYPHS = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001"],
    "B": ["11110", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "01111"],
    "D": ["11110", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10011", "10001", "10001", "01111"],
    "H": ["10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00001", "00001", "00001", "10001", "01110"],
    "K": ["10001", "10010", "11100", "10010", "10001", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10010", "10001"],
    "S": ["01111", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "01010", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "11011", "10001"],
    "X": ["10001", "01010", "00100", "00100", "01010", "10001"],
    "Y": ["10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "11110"],
    "6": ["01110", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000"],
    "8": ["01110", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "01110"],
    ".": ["00000", "00000", "00000", "00000", "00000", "00100"],
    ",": ["00000", "00000", "00000", "00000", "00100", "01000"],
    ":": ["00000", "00100", "00000", "00000", "00100", "00000"],
    ";": ["00000", "00100", "00000", "00000", "00100", "01000"],
    "!": ["00100", "00100", "00100", "00100", "00000", "00100"],
    "?": ["01110", "10001", "00010", "00100", "00000", "00100"],
    "-": ["00000", "00000", "01110", "00000", "00000", "00000"],
    "+": ["00000", "00100", "01110", "00100", "00000", "00000"],
    "/": ["00001", "00010", "00100", "00100", "01000", "10000"],
    "'": ["00100", "00100", "00000", "00000", "00000", "00000"],
    "\"": ["01010", "01010", "00000", "00000", "00000", "00000"],
    "%": ["11001", "11010", "00100", "01000", "10011", "10011"],
    "(": ["00010", "00100", "00100", "00100", "00100", "00010"],
    ")": ["01000", "00100", "00100", "00100", "00100", "01000"],
    "#": ["01010", "11111", "01010", "01010", "11111", "01010"],
    "*": ["00000", "10101", "01110", "11111", "01110", "10101"],
    "<": ["00010", "00100", "01000", "01000", "00100", "00010"],
    ">": ["01000", "00100", "00010", "00010", "00100", "01000"],
    "=": ["00000", "11111", "00000", "11111", "00000", "00000"],
    "_": ["00000", "00000", "00000", "00000", "00000", "11111"],
    # icons
    "$": ["00100", "01111", "10100", "01110", "00101", "11110"],   # credits
    "@": ["01010", "11111", "11111", "01110", "00100", "00000"],   # heart
    "&": ["00100", "01110", "11111", "01110", "01010", "00000"],   # star-ish
    "[": ["11111", "11111", "01110", "00100", "01110", "11111"],   # trophy
    "]": ["00000", "00110", "01111", "11110", "01100", "00000"],   # flame
    "{": ["00100", "01100", "11111", "01100", "00100", "00000"],   # arrow left
    "}": ["00100", "00110", "11111", "00110", "00100", "00000"],   # arrow right
    "|": ["11111", "10001", "10001", "10001", "10001", "11111"],   # lock box
    "~": ["00000", "01000", "10101", "00010", "00000", "00000"],
    "^": ["00100", "01110", "11111", "00000", "00000", "00000"],
}


def glyph_tile(ch, bg=0):
    lay = Layer(8, 8)
    g = GLYPHS.get(ch)
    if g:
        for y, row in enumerate(g):
            for x, b in enumerate(row):
                if b == "1":
                    lay.set(x + 1, y + 1, 2 if y < 3 else 3)
        lay.outline(1, diag=False)
        # drop shadow pixels to the lower right
        for y in range(7, 0, -1):
            for x in range(7, 0, -1):
                if lay.get(x, y) == 0 and lay.get(x - 1, y - 1) in (2, 3):
                    lay.set(x, y, 1)
    if bg:
        for y in range(8):
            for x in range(8):
                if lay.px[y][x] == 0:
                    lay.px[y][x] = bg
    return lay


def font_tiles(bg=0):
    return [glyph_tile(c, bg) for c in FONT_CHARS]


# big italic digits 8x16 (2 tiles high) for speed & position
BIG = {
    "0": ["0111110", "1100011", "1100111", "1101011", "1110011", "1100011", "1100011", "0111110"],
    "1": ["0001100", "0011100", "0111100", "0001100", "0001100", "0001100", "0001100", "0111111"],
    "2": ["0111110", "1100011", "0000011", "0001110", "0111000", "1100000", "1100000", "1111111"],
    "3": ["1111110", "0000011", "0000011", "0011110", "0000011", "0000011", "1100011", "0111110"],
    "4": ["0000110", "0001110", "0011110", "0110110", "1100110", "1111111", "0000110", "0000110"],
    "5": ["1111111", "1100000", "1111110", "0000011", "0000011", "0000011", "1100011", "0111110"],
    "6": ["0011110", "0110000", "1100000", "1111110", "1100011", "1100011", "1100011", "0111110"],
    "7": ["1111111", "0000011", "0000110", "0001100", "0011000", "0011000", "0011000", "0011000"],
    "8": ["0111110", "1100011", "1100011", "0111110", "1100011", "1100011", "1100011", "0111110"],
    "9": ["0111110", "1100011", "1100011", "0111111", "0000011", "0000011", "0000110", "0111100"],
}


def big_digit(d):
    lay = Layer(8, 16)
    g = BIG[d]
    for gy, row in enumerate(g):
        for gx, b in enumerate(row):
            if b == "1":
                for k in range(2):
                    y = 1 + gy * 7 // 4 + k if False else 1 + int(gy * 1.75) + k
                    if y >= 15:
                        continue
                    shift = (7 - gy) // 4
                    x = gx + shift - 0
                    if 0 <= x < 7:
                        lay.set(x, y, 2 if y < 8 else 3)
    lay.outline(1)
    return lay


# ------------------------------------------------------------------ panels & HUD pieces
def panel_tiles():
    """9-slice panel (rounded, with a light top edge)."""
    tiles = []
    full = Layer(24, 24)
    for y in range(24):
        for x in range(24):
            full.set(x, y, 6)
    for i in range(24):
        full.set(i, 0, 1)
        full.set(i, 23, 1)
        full.set(0, i, 1)
        full.set(23, i, 1)
        full.set(i, 1, 4)
        full.set(i, 22, 5)
        full.set(1, i, 5 if i > 1 else 4)
        full.set(22, i, 5)
    for (x, y) in ((0, 0), (1, 0), (0, 1), (23, 0), (22, 0), (23, 1), (0, 23), (1, 23), (0, 22), (23, 23),
                   (22, 23), (23, 22)):
        full.set(x, y, 0)
    full.set(1, 1, 1)
    full.set(22, 1, 1)
    full.set(1, 22, 1)
    full.set(22, 22, 1)
    for ty in range(3):
        for tx in range(3):
            tiles.append(full.crop(tx * 8, ty * 8, 8, 8))
    return tiles


def bar_tiles():
    """Segmented gauge tiles: [empty, green, yellow, red, pink(nitro), cyan(nitro)], each a 4px-wide pair."""
    out = []
    for fill in (13, 10, 11, 12, 8, 9):
        lay = Layer(8, 8)
        for x in range(8):
            for y in range(1, 7):
                if x in (3, 7):
                    continue
                lay.set(x, y, fill)
            lay.set(x, 0, 1)
            lay.set(x, 7, 1)
        for x in (0, 4):
            for y in range(2, 5):
                if fill != 13:
                    lay.set(x, y, 15)
        out.append(lay)
    # half-filled (left segment filled, right empty) for nitro pink
    lay = Layer(8, 8)
    for x in range(8):
        for y in range(1, 7):
            if x in (3, 7):
                continue
            lay.set(x, y, 8 if x < 3 else 13)
        lay.set(x, 0, 1)
        lay.set(x, 7, 1)
    out.append(lay)
    # bar caps (left/right)
    for side in (0, 1):
        lay = Layer(8, 8)
        for y in range(0, 8):
            lay.set(7 if side == 0 else 0, y, 1)
        for y in range(1, 7):
            lay.set(6 if side == 0 else 1, y, 4)
        out.append(lay)
    return out


def minimap_layer(points, size=40):
    """Draws the track outline (list of (x,y) in 0..size) into a size x size layer."""
    lay = Layer(size, size)
    n = len(points)
    for i in range(n):
        x0, y0 = points[i]
        x1, y1 = points[(i + 1) % n]
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for s in range(steps + 1):
            t = s / float(steps)
            x = x0 + (x1 - x0) * t
            y = y0 + (y1 - y0) * t
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                lay.set(int(x) + dx, int(y) + dy, 3)
    lay.outline(1)
    return lay


# ------------------------------------------------------------------ logo
LETTERS = {
    "T": ([[(0, 0), (6, 0), (6, 2), (4, 2), (4, 7), (2, 7), (2, 2), (0, 2)]], 6),
    "U": ([[(0, 0), (2, 0), (2, 5), (4, 5), (4, 0), (6, 0), (6, 6), (5, 7), (1, 7), (0, 6)]], 6),
    "R": ([[(0, 0), (5, 0), (6, 1), (6, 3.2), (5.2, 4), (6, 4.8), (6, 7), (4, 7), (4, 5.2), (2, 5.2), (2, 7), (0, 7)],
           [(2, 1.8), (4, 1.8), (4, 3.3), (2, 3.3)]], 6),
    "B": ([[(0, 0), (5, 0), (6, 1), (6, 2.8), (5.3, 3.5), (6, 4.2), (6, 6), (5, 7), (0, 7)],
           [(2, 1.7), (4, 1.7), (4, 2.8), (2, 2.8)], [(2, 4.3), (4, 4.3), (4, 5.3), (2, 5.3)]], 6),
    "O": ([[(1, 0), (5, 0), (6, 1), (6, 6), (5, 7), (1, 7), (0, 6), (0, 1)],
           [(2, 2), (4, 2), (4, 5), (2, 5)]], 6),
    "D": ([[(0, 0), (5, 0), (6, 1), (6, 6), (5, 7), (0, 7)], [(2, 2), (4, 2), (4, 5), (2, 5)]], 6),
    "I": ([[(0, 0), (2, 0), (2, 7), (0, 7)]], 2),
    "F": ([[(0, 0), (6, 0), (6, 2), (2, 2), (2, 2.8), (5, 2.8), (5, 4.6), (2, 4.6), (2, 7), (0, 7)]], 6),
    "N": ([[(0, 0), (2, 0), (4, 3.5), (4, 0), (6, 0), (6, 7), (4, 7), (2, 3.5), (2, 7), (0, 7)]], 6),
    "S": ([[(1, 0), (6, 0), (6, 2), (2, 2), (2, 2.6), (5, 2.6), (6, 3.6), (6, 6), (5, 7), (0, 7), (0, 5), (4, 5),
            (4, 4.4), (1, 4.4), (0, 3.4), (0, 1)]], 6),
    "H": ([[(0, 0), (2, 0), (2, 2.6), (4, 2.6), (4, 0), (6, 0), (6, 7), (4, 7), (4, 4.6), (2, 4.6), (2, 7), (0, 7)]], 6),
    "G": ([[(1, 0), (6, 0), (6, 2), (2, 2), (2, 5), (4, 5), (4, 4.4), (3, 4.4), (3, 3), (6, 3), (6, 6), (5, 7), (1, 7),
            (0, 6), (0, 1)]], 6),
    "!": ([[(0, 0), (2, 0), (2, 4.6), (0, 4.6)], [(0, 5.4), (2, 5.4), (2, 7), (0, 7)]], 2),
    "A": ([[(1, 0), (5, 0), (6, 1), (6, 7), (4, 7), (4, 4.8), (2, 4.8), (2, 7), (0, 7), (0, 1)],
           [(2, 1.8), (4, 1.8), (4, 3.2), (2, 3.2)]], 6),
    "P": ([[(0, 0), (5, 0), (6, 1), (6, 3.6), (5, 4.6), (2, 4.6), (2, 7), (0, 7)], [(2, 1.8), (4, 1.8), (4, 3), (2, 3)]], 6),
    "L": ([[(0, 0), (2, 0), (2, 5), (6, 5), (6, 7), (0, 7)]], 6),
    "E": ([[(0, 0), (6, 0), (6, 2), (2, 2), (2, 2.6), (5, 2.6), (5, 4.4), (2, 4.4), (2, 5), (6, 5), (6, 7), (0, 7)]], 6),
    "W": ([[(0, 0), (2, 0), (2, 4.4), (3, 3.4), (4, 4.4), (4, 0), (6, 0), (6, 7), (4.4, 7), (3, 5.6), (1.6, 7), (0, 7)]], 6),
    "1": ([[(1, 0), (4, 0), (4, 5), (5.5, 5), (5.5, 7), (0.5, 7), (0.5, 5), (2, 5), (2, 2.2), (1, 2.2)]], 6),
    "2": ([[(0, 0), (5, 0), (6, 1), (6, 3.6), (5, 4.4), (2, 4.4), (2, 5), (6, 5), (6, 7), (0, 7), (0, 3.6), (1, 2.6),
            (4, 2.6), (4, 2), (0, 2)]], 6),
    "3": ([[(0, 0), (5, 0), (6, 1), (6, 2.8), (5.3, 3.5), (6, 4.2), (6, 6), (5, 7), (0, 7), (0, 5), (4, 5), (4, 4.4),
            (1.5, 4.4), (1.5, 2.6), (4, 2.6), (4, 2), (0, 2)]], 6),
}


def fill_rings(lay, rings, c_fn):
    ys = [p[1] for r in rings for p in r]
    y0 = max(0, int(math.floor(min(ys))))
    y1 = min(lay.h - 1, int(math.ceil(max(ys))))
    for y in range(y0, y1 + 1):
        py = y + 0.5
        xs = []
        for ring in rings:
            n = len(ring)
            for i in range(n):
                ax, ay = ring[i]
                bx, by = ring[(i + 1) % n]
                if (ay <= py < by) or (by <= py < ay):
                    xs.append(ax + (py - ay) * (bx - ax) / (by - ay))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            for x in range(max(0, int(math.ceil(xs[i] - 0.5))), min(lay.w - 1, int(math.floor(xs[i + 1] - 0.5))) + 1):
                v = c_fn(x, y)
                if v:
                    lay.px[y][x] = v


def word_layer(text, s, skew, ramp, gap=1.0, outline=1, shadow=2, w=None, h=None, hl=12, horizon=None):
    width = sum((LETTERS[c][1] + gap) * s if c != " " else 3 * s for c in text) + 8 * s * skew + 8
    W = w or int(width) + 6
    H = h or int(7 * s + 8)
    lay = Layer(W, H)
    x = 3.0
    top = 3.0

    def cfn(px, py):
        t = (py - top) / (7.0 * s)
        if horizon is not None:
            if t < horizon:
                return ramp_pick(ramp[0], t / horizon, px, py)
            return ramp_pick(ramp[1], (t - horizon) / (1 - horizon), px, py)
        return ramp_pick(ramp, t, px, py)
    for c in text:
        if c == " ":
            x += 3 * s
            continue
        rings, adv = LETTERS[c]
        tr = [[(x + (px + (7 - py) * skew) * s, top + py * s) for px, py in r] for r in rings]
        fill_rings(lay, tr, cfn)
        x += (adv + gap) * s
    # highlight: top-most pixel of each column of the fill
    for xx in range(W):
        for yy in range(H):
            if lay.px[yy][xx]:
                if yy == 0 or lay.px[yy - 1][xx] == 0:
                    lay.px[yy][xx] = hl
    lay.outline(outline)
    if shadow:
        sh = Layer(W, H)
        for yy in range(H):
            for xx in range(W):
                if lay.px[yy][xx] and lay.get(xx + 1, yy + 2) == 0:
                    sh.set(xx + 1, yy + 2, shadow)
        sh.paste(lay)
        lay = sh
    return lay


def logo_layer():
    """'TURBO' (chrome) over 'DRIFT' (hot) with speed streaks: 240 x 64."""
    lay = Layer(240, 64)
    chrome = ([3, 4, 5, 6], [7, 6, 5, 4])
    turbo = word_layer("TURBO", 3.4, 0.32, chrome, gap=1.0, horizon=0.55)
    drift = word_layer("DRIFT", 4.6, 0.32, [8, 9, 10, 11], gap=0.9)
    # speed streaks
    rng = Rng(0x1060)
    for i in range(9):
        y = 30 + i * 3 + rng.range(0, 1)
        x0 = rng.range(4, 26)
        ln = rng.range(18, 44)
        c = 13 if i % 2 else 14
        for x in range(x0, x0 + ln):
            lay.set(x, y, c)
    ox = (240 - drift.w) // 2 + 10
    lay.paste(drift, ox, 64 - drift.h)
    lay.paste(turbo, ox - 14, 0)
    return lay


def banner_layer(text, s=2.6, ramp=(8, 9, 10, 11)):
    return word_layer(text, s, 0.3, list(ramp), gap=0.8)


# ------------------------------------------------------------------ Mochi portraits (OBJ 32x32)
MOCHI_PAL = pal16(["000000", "5a2a4e", "e8849f", "f9b3c9", "ffd6e2", "fff6ec", "ff7b9c", "35203f",
                   "ffffff", "3d8a58", "6fcf7a", "b9f2a2", "b8486a", "2a2a3c", "34e0ff", "c7f0ff"])


def mochi_portrait(mood):
    """mood: 'happy', 'cheer', 'focus', 'sad'.  Mochi wears racing goggles."""
    lay = Layer(32, 32)
    cx, cy = 16.0, 19.5
    sq = 1.0 if mood != "cheer" else 1.08

    def body(x, y, dx, dy, d):
        if dy < -0.55 and dx < -0.1:
            return 4
        if dy > 0.45 or d > 0.82:
            return 2
        return 3
    ellipse(lay, cx, cy, 13.0 * sq, 10.5 / sq, 3, body)
    ellipse(lay, cx - 5, cy - 6, 2.2, 1.4, 5)
    # leaf
    poly(lay, [(16, 9.5), (14.5, 5.5), (16.5, 2.5), (19.5, 3.0), (18.5, 7.0)], 10)
    poly(lay, [(16, 9.5), (16.5, 2.5), (17.4, 3.2), (17.0, 8.0)], 11)
    lay.set(16, 9, 9)
    lay.set(16, 10, 9)
    # goggles strap + lenses on the forehead
    for x in range(4, 29):
        y = 13 if 6 < x < 26 else 14
        lay.set(x, y, 13)
        lay.set(x, y + 1, 13)
    for gx in (11, 21):
        ellipse(lay, gx, 13.5, 3.4, 2.6, 13)
        ellipse(lay, gx, 13.5, 2.4, 1.7, 14, lambda x, y, dx, dy, d: 15 if dx < -0.2 and dy < 0 else 14)
    # face
    ey = 20
    if mood == "happy" or mood == "cheer":
        for ex in (11, 20):
            lay.set(ex, ey, 7)
            lay.set(ex + 1, ey - 1, 7)
            lay.set(ex + 2, ey, 7)
    elif mood == "focus":
        for ex in (11, 20):
            lay.rect(ex, ey - 1, ex + 2, ey + 1, 7)
            lay.set(ex, ey - 1, 8)
        lay.rect(10, ey - 3, 14, ey - 2, 1)
        lay.rect(19, ey - 3, 23, ey - 2, 1)
    else:
        for ex in (11, 20):
            lay.rect(ex, ey - 1, ex + 2, ey + 1, 7)
            lay.set(ex + 1, ey - 1, 8)
        lay.set(10, ey + 2, 15)
        lay.set(10, ey + 3, 15)
    lay.rect(7, 22, 10, 24, 6)
    lay.rect(23, 22, 26, 24, 6)
    if mood in ("happy", "cheer"):
        lay.rect(15, 23, 18, 24, 12)
        lay.set(14, 22, 12)
        lay.set(18, 22, 12)
        lay.rect(15, 24, 18, 25, 6)
    elif mood == "focus":
        lay.rect(15, 23, 18, 24, 12)
    else:
        lay.rect(15, 24, 18, 25, 12)
        lay.set(14, 25, 12)
        lay.set(18, 25, 12)
    if mood == "cheer":
        for (x, y) in ((3, 8), (28, 6), (26, 2)):
            lay.set(x, y, 8)
            lay.set(x - 1, y, 15)
            lay.set(x + 1, y, 15)
            lay.set(x, y - 1, 15)
            lay.set(x, y + 1, 15)
    lay.outline(1)
    return lay


# ------------------------------------------------------------------ effects (OBJ bank 13)
FX_PAL = pal16(["000000", "3a2a2a", "fff8d0", "ffd23c", "ff8a2a", "e8e8f0", "c4c4d4", "9a9aac",
                "6a6a7c", "ffffff", "a8f4ff", "34c8ff", "3a6aff", "c89a6a", "9a6a40", "6a4a2a"])


def fx_tiles():
    """Returns dict name -> list of layers."""
    out = {}
    # sparks 8x8
    sp = []
    for k in range(2):
        lay = Layer(8, 8)
        pts = [(1, 6), (3, 4), (5, 2), (6, 1)] if k == 0 else [(1, 3), (3, 3), (5, 4), (7, 4)]
        for i, (x, y) in enumerate(pts):
            lay.set(x, y, 2 if i > 1 else 3)
            lay.set(x - 1, y + (1 if k == 0 else 0), 4)
        sp.append(lay)
    out["spark"] = sp
    # smoke puffs 16x16 (3 sizes)
    sm = []
    for k, r in enumerate((3.5, 5.5, 7.2)):
        lay = Layer(16, 16)
        rng = Rng(0x5300 + k)
        for j in range(4):
            ox, oy = rng.uniform(-2, 2), rng.uniform(-1.5, 1.5)
            rr = r * rng.uniform(0.6, 0.9)
            ellipse(lay, 8 + ox, 9 + oy, rr, rr * 0.85, 6,
                    lambda x, y, dx, dy, d: 5 if dx + dy < -0.3 else 6 if d < 0.7 else 7)
        sm.append(lay)
    out["smoke"] = sm
    # dust (brown) 16x16 x2
    du = []
    for k, r in enumerate((4.5, 6.5)):
        lay = Layer(16, 16)
        rng = Rng(0xD500 + k)
        for j in range(4):
            ox, oy = rng.uniform(-2, 2), rng.uniform(-1, 1)
            ellipse(lay, 8 + ox, 10 + oy, r * 0.8, r * 0.6, 13,
                    lambda x, y, dx, dy, d: 13 if dy < 0 else 14 if d < 0.8 else 15)
        du.append(lay)
    out["dust"] = du
    # nitro flame 16x16 x3 (pointing down/back towards camera)
    fl = []
    for k in range(3):
        lay = Layer(16, 16)
        ln = 9 + k * 2
        for y in range(ln):
            t = y / float(ln)
            half = 4.5 * (1 - t) ** 0.8 + 0.5
            for x in range(int(8 - half), int(8 + half) + 1):
                d = abs(x - 7.5) / (half + 0.01)
                c = 9 if d < 0.35 and t < 0.6 else 10 if d < 0.65 else 11 if t < 0.7 else 12
                lay.set(x, 2 + y, c)
        fl.append(lay)
    out["flame"] = fl
    # speed streak 8x32
    lay = Layer(8, 32)
    for y in range(32):
        if 4 < y < 28:
            lay.set(3, y, 9 if 10 < y < 22 else 10)
            if 8 < y < 24:
                lay.set(4, y, 10)
    out["streak"] = [lay]
    return out


def countdown_layers():
    """'3','2','1' (32x32) and 'GO!' (64x32) sprites in HUD bank colours."""
    out = []
    ramp = [2, 3, 4, 5]
    for ch in "321":
        w = word_layer(ch, 3.4, 0.25, ramp, gap=0.5, hl=6, outline=1, shadow=7)
        lay = Layer(32, 32)
        lay.paste(w, (32 - w.w) // 2 + 1, (32 - w.h) // 2)
        out.append(lay)
    w = word_layer("GO!", 3.4, 0.25, [8, 9, 10, 11], gap=0.7, hl=6, outline=1, shadow=7)
    lay = Layer(64, 32)
    lay.paste(w, (64 - w.w) // 2, (32 - w.h) // 2)
    out.append(lay)
    return out


HUDSPR_PAL = pal16(["000000", "140a20", "fff8d0", "ffe060", "ffa030", "ff5a3c", "ffffff", "3a1458",
                    "c8f2ff", "6ad0ff", "3a72e0", "2a2a90", "f9b3c9", "ff4aa8", "34e0ff", "5cf08a"])


def dot_tiles():
    """Minimap / position dots: one 8x8 tile per colour (player first)."""
    out = []
    for c in (12, 3, 9, 15, 13, 14, 6):
        lay = Layer(8, 8)
        for (x, y) in ((3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4), (3, 5), (4, 5)):
            lay.set(x, y, c)
        lay.outline(1)
        out.append(lay)
    # cursor arrow (menus)
    lay = Layer(8, 8)
    poly(lay, [(1, 0.5), (6.5, 4), (1, 7.5)], 3)
    poly(lay, [(1, 0.5), (6.5, 4), (1, 4)], 2)
    lay.outline(1)
    out.append(lay)
    return out
