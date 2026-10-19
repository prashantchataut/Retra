"""Original background / UI art for Mochi Hop: font, panels, terrain, parallax, logo."""
import math
from pix import Layer, pal16

# ---------------------------------------------------------------- UI palette (BG bank 15)
PAL_UI = pal16([
    "000000",  # 0
    "5a2a4e",  # 1 plum
    "ffffff",  # 2 white
    "fff6ec",  # 3 cream panel
    "f6dfe6",  # 4 panel shade
    "ffd84d",  # 5 star
    "f2a93b",  # 6 star shade
    "ff8fb2",  # 7 pink
    "8fdc84",  # 8 green
    "3d8a58",  # 9 dark green
    "e8849f",  # 10 border pink
    "b8b0c0",  # 11 grey
    "ffd6e2",  # 12 light pink
    "a0522d",  # 13 brown
    "c9b6ff",  # 14 lavender
    "35203f",  # 15 ink
])

FONT_CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?.,:/-'+()&"
FONT = """
.....|.###.|####.|.###.|####.|#####|#####|.###.|#...#|.###.|..###|#...#|#....|#...#|#...#|.###.|####.|.###.|####.|.####|#####|#...#|#...#|#...#|#...#|#...#|#####|.###.|..#..|.###.|.###.|...#.|#####|.###.|#####|.###.|.###.|..#..|.###.|.....|.....|.....|....#|.....|..#..|.....|..#..|...#.|.#...|.##..
.....|#...#|#...#|#...#|#...#|#....|#....|#....|#...#|..#..|...#.|#..#.|#....|##.##|##..#|#...#|#...#|#...#|#...#|#....|..#..|#...#|#...#|#...#|#...#|#...#|...#.|#..##|.##..|#...#|#...#|..##.|#....|#....|....#|#...#|#...#|..#..|#...#|.....|.....|.....|....#|.....|..#..|.....|..#..|..#..|..#..|#..#.
.....|#...#|####.|#....|#...#|####.|####.|#.###|#####|..#..|...#.|###..|#....|#.#.#|#.#.#|#...#|####.|#...#|####.|.###.|..#..|#...#|#...#|#.#.#|.#.#.|.###.|..#..|#.#.#|..#..|...#.|..##.|.#.#.|####.|####.|...#.|.###.|.####|..#..|...#.|.....|.....|..#..|...#.|.....|.....|..#..|.###.|..#..|..#..|.##..
.....|#####|#...#|#....|#...#|#....|#....|#...#|#...#|..#..|#..#.|#..#.|#....|#...#|#..##|#...#|#....|#.#.#|#..#.|....#|..#..|#...#|.#.#.|#.#.#|..#..|..#..|.#...|##..#|..#..|..#..|....#|#..#.|....#|#...#|..#..|#...#|....#|..#..|..#..|.....|.....|.....|..#..|.###.|.....|#####|..#..|..#..|.#.#.
.....|#...#|#...#|#...#|#...#|#....|#....|#...#|#...#|..#..|#..#.|#...#|#....|#...#|#...#|#...#|#....|#..#.|#...#|....#|..#..|#...#|.#.#.|##.##|.#.#.|..#..|#....|#...#|..#..|.#...|#...#|#####|#...#|#...#|.#...|#...#|...#.|.....|.....|..#..|..#..|..#..|.#...|.....|.....|..#..|..#..|..#..|#..#.
.....|#...#|####.|.###.|####.|#####|#....|.###.|#...#|.###.|.##..|#...#|#####|#...#|#...#|.###.|#....|.##.#|#...#|####.|..#..|.###.|..#..|#...#|#...#|..#..|#####|.###.|.###.|#####|.###.|...#.|.###.|.###.|.#...|.###.|.##..|..#..|..#..|..#..|.#...|.....|#....|.....|.....|.....|...#.|.#...|.##.#
"""


def font_glyphs():
    rows = [r for r in FONT.strip().split("\n")]
    cols = [r.split("|") for r in rows]
    n = len(cols[0])
    assert n == len(FONT_CHARS), (n, len(FONT_CHARS))
    g = {}
    for i, ch in enumerate(FONT_CHARS):
        g[ch] = [cols[r][i] for r in range(6)]
    return g


def glyph_tile(rows, fg, shadow, bg):
    L = Layer(8, 8, bg)
    # 5x6 glyph placed at (1,1); drop shadow down-right
    if shadow is not None:
        for y, r in enumerate(rows):
            for x, ch in enumerate(r):
                if ch == "#":
                    for dx, dy in ((1, 1), (0, 1), (1, 0)):
                        if L.get(1 + x + dx, 1 + y + dy) == bg:
                            L.set(1 + x + dx, 1 + y + dy, shadow)
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == "#":
                L.set(1 + x, 1 + y, fg)
    return L


ICON_STAR = """
...11...
..1551..
111551 1
15555551
.155561.
.156651.
15611651
111..111
"""
ICON_SPROUT = """
.11..11.
1881188
19881881
.119911.
..1331..
.133331.
.133331.
..1111..
"""
ICON_SPROUT_EMPTY = """
.11..11.
1bb11bb1
1bbb1bb1
.11bb11.
..1bb1..
.1bbbb1.
.1bbbb1.
..1111..
"""
ICON_HEART = """
.11.11..
1771771.
1727771.
1777771.
.17771..
..171...
...1....
........
"""
ICON_ARROW = """
.1......
.11.....
.171....
.1771...
.171....
.11.....
.1......
........
"""


def icon_tile(art, bg=0):
    L = Layer(8, 8, bg)
    leg = {"1": 1, "3": 3, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9, "b": 11, "2": 2}
    rows = art.strip("\n").split("\n")
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in leg:
                c = leg[ch]
                if bg and c == 3:
                    c = 2
                L.set(x, y, c)
    return L


def panel_tiles():
    """3x3 nine-slice rounded panel (cream with plum outline + pink inner line)."""
    big = Layer(24, 24)

    def col(x, y):
        # distance from rounded rect edge
        def inside(px, py, inset, r):
            x0, y0, x1, y1 = inset, inset, 23 - inset, 23 - inset
            cx = min(max(px, x0 + r), x1 - r)
            cy = min(max(py, y0 + r), y1 - r)
            return (px - cx) ** 2 + (py - cy) ** 2 <= r * r + 0.5
        if not inside(x, y, 0, 6):
            return None
        if not inside(x, y, 1, 5):
            return 1
        if not inside(x, y, 3, 4):
            return 12 if y < 12 else 10
        return 3
    big.fill_fn(col)
    return [big.crop(tx * 8, ty * 8, 8, 8) for ty in range(3) for tx in range(3)]


def ui_tiles():
    """Returns (list of 8x8 layers, dict name->index). Index 0 = blank."""
    tiles = [Layer(8, 8)]
    idx = {}
    g = font_glyphs()
    for ch in FONT_CHARS:
        idx["h" + ch] = len(tiles)
        tiles.append(glyph_tile(g[ch], 2, 1, 0))
    for ch in FONT_CHARS:
        idx["p" + ch] = len(tiles)
        tiles.append(glyph_tile(g[ch], 1, None, 3))
    for i, t in enumerate(panel_tiles()):
        idx["panel%d" % i] = len(tiles)
        tiles.append(t)
    for name, art in (("star", ICON_STAR), ("sprout", ICON_SPROUT), ("sprout0", ICON_SPROUT_EMPTY),
                      ("heart", ICON_HEART), ("arrow", ICON_ARROW)):
        idx["i" + name] = len(tiles)
        tiles.append(icon_tile(art))
        idx["j" + name] = len(tiles)
        tiles.append(icon_tile(art, bg=3))
    # solid cream (for panel interior) and a dim 50% checker for dialogs
    idx["solid"] = len(tiles)
    tiles.append(Layer(8, 8, 3))
    return tiles, idx


# ---------------------------------------------------------------- themes
THEME_TERRAIN_PALS = [
    pal16(["000000", "4a3048", "d9a982", "f3d6b0", "fff0d8", "4f9e62", "86d47c", "b8ef9a", "e2ffc8",
           "c8d4f4", "ffffff", "9a6ac0", "d6b8f4", "ff9ab8", "ffe066", "ffffff"]),
    pal16(["000000", "4a2238", "7a4a3a", "a86a50", "c98a6a", "d8609c", "ff9cc8", "ffc8e0", "fff0f8",
           "d89a50", "ffd890", "ff6f90", "ffffff", "8fe0ff", "fff07a", "c8a0ff"]),
    pal16(["000000", "1e1a3a", "343672", "4a4d92", "6a6ec0", "3a78a0", "5aa6c8", "8fd0e0", "d0f4ff",
           "8a80c8", "d8d0ff", "9aa4e8", "ffffff", "ffe680", "ff9ad0", "a0ffe0"]),
]

THEME_NAMES = ["CLOUD MEADOW", "CANDY CAVES", "STARRY NIGHT"]


def _bayer(x, y):
    m = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
    return m[y & 3][x & 3] / 16.0


def gradient_index(y, stops):
    """stops: list of (y, colour index). Ordered dithered transitions."""
    for i in range(len(stops) - 1):
        y0, c0 = stops[i]
        y1, c1 = stops[i + 1]
        if y0 <= y < y1:
            return c0, c1, (y - y0) / float(y1 - y0)
    return stops[-1][1], stops[-1][1], 0.0


def far_layer(theme):
    """256x256 far background (BG3). Returns (layer, palette)."""
    L = Layer(256, 256)
    if theme == 0:
        pal = pal16(["000000", "9ed2ff", "b4dcff", "c9e6ff", "dcefff", "eef4ff", "ffeaf2", "ffffff",
                     "e4e2fa", "c8c4ee", "b9d8f2", "a6c8ea", "fff6fb", "ffffff", "ffffff", "ffffff"])
        stops = [(0, 1), (40, 2), (80, 3), (115, 4), (150, 5), (185, 6), (256, 6)]
    elif theme == 1:
        pal = pal16(["000000", "3a2050", "4a2a60", "5c3470", "70407e", "86508a", "9c6096", "b878a8",
                     "d898c0", "f0b8d8", "ffe0f0", "7a4f9a", "a070c0", "c8a0e8", "ffffff", "ffffff"])
        stops = [(0, 1), (40, 2), (80, 3), (115, 4), (150, 5), (185, 6), (256, 6)]
    else:
        pal = pal16(["000000", "141638", "1c1e48", "262858", "323268", "403c78", "524888", "6a5898",
                     "ffffff", "ffe8a0", "b8c0ff", "fff6d0", "f0e0b0", "d8c890", "8090d8", "ffffff"])
        stops = [(0, 1), (40, 2), (80, 3), (115, 4), (150, 5), (185, 6), (256, 7)]

    def sky(x, y):
        c0, c1, t = gradient_index(y, stops)
        return c1 if _bayer(x, y) < t * 0.999 and t > 0.25 and (t > 0.5 or _bayer(x, y) < (t - 0.25) * 2) else c0
    L.fill_fn(sky)

    if theme == 0:
        # soft cumulus banks: union of circles, two tones, period 256
        clouds = [(30, 112, 26), (60, 100, 30), (95, 116, 22), (150, 92, 20), (176, 104, 28), (210, 112, 22),
                  (238, 120, 18), (120, 128, 16)]
        far = [(20, 50, 10), (36, 46, 12), (52, 52, 9), (150, 34, 8), (162, 30, 10), (176, 36, 8)]

        def puff(x, y, lst):
            for cx, cy, r in lst:
                for ox in (-256, 0, 256):
                    if (x - cx - ox) ** 2 + (y - cy) ** 2 <= r * r:
                        return cy, r
            return None

        def col(x, y):
            p = puff(x, y, clouds)
            if p:
                if y > 128:
                    return 9
                return 7 if y < p[0] - p[1] * 0.35 else 8
            if y >= 128 and y < 256:
                return 8 if y < 140 else (9 if y < 200 else 9)
            p = puff(x, y, far)
            if p:
                return 12 if y < p[0] else 6
            return None
        L.fill_fn(col)
        # distant soft hills below the cloud bank
        def hill(x, y):
            h = 168 + 10 * math.sin(x * 2 * math.pi / 128) + 6 * math.sin(x * 2 * math.pi / 64 + 1)
            if y >= h:
                return 10 if y < h + 3 else 11
            return None
        L.fill_fn(hill)
    elif theme == 1:
        # hanging candy stalactites + crystal sparkles
        def col(x, y):
            for base, w, ln in ((20, 14, 60), (64, 10, 40), (110, 18, 80), (160, 9, 36), (200, 14, 66), (236, 8, 30)):
                dx = abs(((x - base + 128) % 256) - 128)
                if y < ln and dx <= w * (1 - y / float(ln)) ** 0.8:
                    return 11 if dx > w * (1 - y / float(ln)) ** 0.8 - 2 else 12
            for base, w, ht in ((40, 16, 70), (130, 22, 96), (190, 12, 60), (250, 14, 50)):
                dx = abs(((x - base + 128) % 256) - 128)
                yy = 256 - y
                if yy < ht and dx <= w * (1 - yy / float(ht)) ** 0.9:
                    return 7
            return None
        L.fill_fn(col)
        for (sx, sy) in ((30, 90), (90, 60), (140, 130), (180, 100), (225, 150), (70, 170), (200, 40), (10, 140)):
            for d in range(-2, 3):
                L.set(sx + d, sy, 13 if abs(d) == 2 else 14)
                L.set(sx, sy + d, 13 if abs(d) == 2 else 14)
    else:
        # stars and moon
        import random
        rnd = random.Random(7)
        for i in range(70):
            x, y = rnd.randrange(256), rnd.randrange(170)
            L.set(x, y, 8 if i % 3 else 10)
            if i % 9 == 0:
                for d in (-1, 1):
                    L.set(x + d, y, 10)
                    L.set(x, y + d, 10)
        mx, my, mr = 190, 46, 18

        def moon(x, y):
            d = (x - mx) ** 2 + (y - my) ** 2
            if d <= mr * mr:
                if (x - mx + 7) ** 2 + (y - my - 4) ** 2 <= 16:
                    return 12
                if (x - mx - 6) ** 2 + (y - my + 7) ** 2 <= 9:
                    return 12
                return 11 if (x - mx) + (y - my) < 8 else 12
            if d <= (mr + 3) ** 2 and _bayer(x, y) < 0.5:
                return 7
            return None
        L.fill_fn(moon)

        def hill(x, y):
            h = 190 + 12 * math.sin(x * 2 * math.pi / 256 * 2) + 5 * math.sin(x * 2 * math.pi / 64)
            if y >= h:
                return 14 if y < h + 2 else 5
            return None
        L.fill_fn(hill)
    return L, pal


def mid_layer(theme):
    """512x256 mid layer (BG2). Upper part transparent."""
    L = Layer(512, 256)
    if theme == 0:
        pal = pal16(["000000", "5a8a6a", "8ac898", "a8dcae", "c4ecc4", "e0f8d8", "7ab088", "ffd6e2",
                     "ffffff", "6a9a7a", "9ad4a4", "f0c0d0", "ffffff", "ffffff", "ffffff", "ffffff"])

        def h1(x):
            return 150 + 18 * math.sin(x * 2 * math.pi / 512 * 2) + 9 * math.sin(x * 2 * math.pi / 512 * 5 + 1.3)

        def col(x, y):
            h = h1(x)
            if y < h:
                return None
            d = y - h
            if d < 1.5:
                return 5
            if d < 4:
                return 4
            if d < 30:
                return 3
            return 2
        L.fill_fn(col)
        # round trees on the hills
        for tx in (40, 120, 150, 260, 330, 360, 440):
            base = int(h1(tx)) + 4
            for y in range(base - 10, base + 6):
                L.set(tx, y, 6)
                L.set(tx + 1, y, 6)
            for cx, cy, r in ((tx + 0.5, base - 16, 8), (tx - 5, base - 11, 6), (tx + 6, base - 11, 6)):
                for y in range(int(cy - r - 1), int(cy + r + 2)):
                    for x in range(int(cx - r - 1), int(cx + r + 2)):
                        dd = (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2
                        if dd <= r * r:
                            hl = (x + 0.5 - cx + r * 0.35) ** 2 + (y + 0.5 - cy + r * 0.4) ** 2 <= (r * 0.45) ** 2
                            L.set(x % 512, y, 4 if hl else (10 if y < cy + r * 0.3 else 9))
        # tiny flowers dotted on hills
        for fx in range(7, 512, 23):
            hy = int(h1(fx)) + 8 + (fx * 7) % 12
            L.set(fx, hy, 7)
    elif theme == 1:
        pal = pal16(["000000", "4a2a50", "7a4a80", "9a5aa0", "b878b8", "d898d0", "ffd0ec", "ff9ac8",
                     "ffffff", "8fe0ff", "c8f4ff", "6a3a70", "fff07a", "ffffff", "ffffff", "ffffff"])
        # giant candy pillars (striped) and gumdrop mounds
        def col(x, y):
            for base, w in ((60, 14), (230, 10), (390, 16)):
                dx = x - base
                if abs(dx) <= w and y > 40:
                    if abs(dx) == w:
                        return 1
                    stripe = ((y + dx // 2) // 10) % 2
                    return (7 if stripe else 6) if dx < w // 2 else (4 if stripe else 5)
            h = 200 + 16 * math.sin(x * 2 * math.pi / 512 * 3) + 8 * math.sin(x * 2 * math.pi / 512 * 7)
            if y >= h:
                return 2 if y > h + 3 else 3
            # stalactites from top
            for base, w, ln in ((140, 16, 50), (300, 12, 36), (470, 18, 60)):
                dx = abs(x - base)
                if y < ln and dx <= w * (1 - y / float(ln)):
                    return 11
            return None
        L.fill_fn(col)
        # crystals
        for cx, cy in ((150, 196), (330, 205), (20, 210), (450, 200)):
            for y in range(cy - 10, cy + 1):
                w = (y - (cy - 10)) // 3
                for x in range(cx - w, cx + w + 1):
                    L.set(x, y, 10 if x <= cx else 9)
    else:
        pal = pal16(["000000", "1a1840", "26285a", "303470", "3c4284", "4c5498", "ffe680", "fff6c0",
                     "6a72b8", "a0ffe0", "ffb0d8", "202050", "ffffff", "ffffff", "ffffff", "ffffff"])

        def h1(x):
            return 170 + 22 * math.sin(x * 2 * math.pi / 512 * 2 + 0.5) + 10 * math.sin(x * 2 * math.pi / 512 * 6)

        def col(x, y):
            h = h1(x)
            if y < h:
                return None
            if y < h + 2:
                return 8
            return 3 if y < h + 30 else 2
        L.fill_fn(col)
        # little houses with glowing windows
        for hx in (90, 250, 410):
            base = int(h1(hx + 8)) + 6
            for y in range(base - 14, base + 4):
                for x in range(hx, hx + 16):
                    L.set(x, y, 4)
            for i in range(10):
                for x in range(hx - 2 + i, hx + 18 - i):
                    L.set(x, base - 14 - i, 5)
            for wx in (hx + 3, hx + 10):
                for y in range(base - 9, base - 5):
                    for x in range(wx, wx + 3):
                        L.set(x, y, 6 if y > base - 8 else 7)
        # fireflies
        for fx, fy in ((40, 150), (180, 140), (300, 160), (360, 130), (480, 150), (140, 175)):
            L.set(fx, fy, 9)
    return L, pal


# ---------------------------------------------------------------- terrain
DECOR = {
    "f": """
................
................
................
................
................
................
.......11.......
......1dd1......
.....1dccd1.....
......1dd1......
.......15.1.....
.......1511.....
....11.15.......
...1661551......
....1166........
.......5........
""",
    "t": """
................
................
................
................
................
................
................
................
................
................
....1.....1.....
...171...171....
...1761.1761.1..
..17661.17661711
..1766611766176.
.17666611766616.
""",
    "m": """
................
................
................
................
................
................
................
......1111......
....11ddcc11....
...1dddddccd1...
..1dcddddddcd1..
..1111111111111.
......1ff1......
......1ff1......
.....1ffff1.....
.....1ffff1.....
""",
}


def render_terrain(grid, theme):
    """grid: list of strings (metatile chars). Returns Layer of W*16 x H*16."""
    H = len(grid)
    W = len(grid[0])
    Wp, Hp = W * 16, H * 16
    L = Layer(Wp, Hp)

    def cell(cx, cy):
        if cx < 0 or cx >= W:
            return "#" if 0 <= cy < H and grid[cy][0 if cx < 0 else W - 1] == "#" else "."
        if cy >= H:
            return grid[H - 1][cx] if grid[H - 1][cx] in "#" else "."
        if cy < 0:
            return "."
        return grid[cy][cx]

    def is_ground(c):
        return c == "#"

    # solid mask with rounded convex corners
    solid = [[False] * Wp for _ in range(Hp)]
    R = 5
    for cy in range(H):
        for cx in range(W):
            if not is_ground(cell(cx, cy)):
                continue
            up = is_ground(cell(cx, cy - 1))
            dn = is_ground(cell(cx, cy + 1))
            lf = is_ground(cell(cx - 1, cy))
            rt = is_ground(cell(cx + 1, cy))
            for y in range(16):
                for x in range(16):
                    ok = True
                    for (cond, qx, qy) in ((not up and not lf, x < R and y < R, (R, R)),
                                           (not up and not rt, x > 15 - R and y < R, (15 - R, R)),
                                           (not dn and not lf, x < R and y > 15 - R, (R, 15 - R)),
                                           (not dn and not rt, x > 15 - R and y > 15 - R, (15 - R, 15 - R))):
                        if cond and qx:
                            ccx, ccy = qy
                            if (x - ccx) ** 2 + (y - ccy) ** 2 > R * R + 1:
                                ok = False
                    solid[cy * 16 + y][cx * 16 + x] = ok

    def S(x, y):
        if x < 0:
            x = 0
        if x >= Wp:
            x = Wp - 1
        if y >= Hp:
            return S(x, Hp - 1) if grid[H - 1][x // 16] == "#" else False
        if y < 0:
            return False
        return solid[y][x]

    for y in range(Hp):
        for x in range(Wp):
            if not solid[y][x]:
                continue
            if not (S(x - 1, y) and S(x + 1, y) and S(x, y - 1) and S(x, y + 1)):
                L.px[y][x] = 1
                continue
            d = 0
            while d < 12 and S(x, y - d - 1):
                d += 1
            # d = number of solid pixels above (0 = right under the outline)
            lx = x % 16
            scallop = [0, 1, 2, 3, 3, 3, 2, 1][lx % 8]
            if d <= 1:
                c = 8 if lx % 8 in (2, 3) and d == 0 else 7
            elif d <= 4 + scallop:
                c = 6
            elif d == 5 + scallop:
                c = 5
            else:
                side = not (S(x - 2, y) and S(x + 2, y) and S(x, y + 2))
                if side:
                    c = 2
                else:
                    c = 3
                    ly = y % 16
                    var = ((x // 16) * 3 + (y // 16)) % 2
                    pts = [(4, 10), (11, 4)] if var == 0 else [(10, 11), (3, 3), (13, 1)]
                    for px, py in pts:
                        if ly == py and px <= lx <= px + 1:
                            c = 4
                        elif ly == py + 1 and px <= lx <= px + 1:
                            c = 2
            L.px[y][x] = c

    # blocks
    for cy in range(H):
        for cx in range(W):
            ch = grid[cy][cx]
            ox, oy = cx * 16, cy * 16
            if ch == "B":
                for y in range(16):
                    for x in range(16):
                        corner = (x in (0, 15)) and (y in (0, 15))
                        if corner:
                            continue
                        if x in (0, 15) or y in (0, 15):
                            c = 1
                        elif y == 1 or x == 1:
                            c = 4 if not (x == 1 and y > 13) else 2
                        elif y == 14 or x == 14:
                            c = 2
                        else:
                            c = 3
                            if (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
                                c = 2
                            if 6 <= x <= 9 and 6 <= y <= 9 and not ((x in (6, 9)) and (y in (6, 9))):
                                c = 7 if (x + y) < 15 else 6
                        L.px[oy + y][ox + x] = c
            elif ch == "^":
                for y in range(16):
                    for x in range(16):
                        lx = x % 8
                        top = 6 + abs(lx * 2 - 7) * 1.25
                        if y >= top:
                            c = 12 if lx < 3 else 11
                            if y >= top and y < top + 1.2 or lx in (0, 7) or y == 15:
                                c = 1
                            L.px[oy + y][ox + x] = c
            elif ch in DECOR:
                leg = {"1": 1, "d": 13, "c": 14, "5": 5, "6": 6, "7": 7, "f": 15}
                rows = DECOR[ch].strip("\n").split("\n")
                for y, r in enumerate(rows):
                    for x, c in enumerate(r):
                        if c in leg:
                            L.px[oy + y][ox + x] = leg[c]
    # one-way cloud platforms
    for cy in range(H):
        cx = 0
        while cx < W:
            if grid[cy][cx] != "=":
                cx += 1
                continue
            s = cx
            while cx < W and grid[cy][cx] == "=":
                cx += 1
            x0, x1 = s * 16, cx * 16 - 1
            oy = cy * 16
            pl = Layer(x1 - x0 + 3, 16)

            def inside(px, py):
                if py < 1 or py > 11:
                    return False
                if x0 + 4 <= px <= x1 - 4 and 4 <= py <= 9:
                    return True
                n = (x1 - x0 + 1) // 8
                for i in range(n):
                    bx = x0 + 4 + i * 8
                    if (px - bx) ** 2 + (py - 5) ** 2 <= 16:
                        return True
                    if (px - bx - 4) ** 2 + (py - 7.5) ** 2 <= 14:
                        return True
                return False
            for y in range(16):
                for x in range(x0, x1 + 1):
                    if inside(x, y):
                        edge = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
                        if edge:
                            L.px[oy + y][x] = 1
                        else:
                            L.px[oy + y][x] = 10 if y < 7 else 9
    return L


# ---------------------------------------------------------------- logo
LOGO_PAL = pal16(["000000", "5a2a4e", "e8608c", "ff8fb2", "ffc0d4", "ffffff", "fff6ec", "3d8a58",
                  "6fcf7a", "b9f2a2", "f2a93b", "ffd84d", "fff3a6", "c86488", "ffe0ea", "7a3a64"])


def logo_layer():
    g = font_glyphs()
    word1, word2 = "MOCHI", "HOP"
    S = 3
    W, H = 232, 56
    L = Layer(W, H)
    letters = []
    # layout: MOCHI then HOP, wobbly baseline
    x = 4
    for i, ch in enumerate(word1 + " " + word2):
        if ch == " ":
            x += 8
            continue
        letters.append((ch, x, 10 + (3 if i % 2 else 0), i >= 6))
        x += 6 * S + 4
    shape = Layer(W, H)
    for ch, ox, oy, hop in letters:
        rows = g[ch]
        # scale 3x with rounded smoothing
        for y in range(6 * S):
            for x in range(5 * S):
                gx, gy = x // S, y // S
                on = rows[gy][gx] == "#"
                if not on:
                    # fill inner concave corners to round shapes
                    fx, fy = x % S, y % S
                    nb = 0
                    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                        xx, yy = gx + dx, gy + dy
                        if 0 <= yy < 6 and 0 <= xx < 5 and rows[yy][xx] == "#":
                            nb += 1
                    if nb >= 2:
                        lft = gx > 0 and rows[gy][gx - 1] == "#"
                        rgt = gx < 4 and rows[gy][gx + 1] == "#"
                        up = gy > 0 and rows[gy - 1][gx] == "#"
                        dn = gy < 5 and rows[gy + 1][gx] == "#"
                        if (lft and up and fx == 0 and fy == 0) or (rgt and up and fx == S - 1 and fy == 0) or \
                           (lft and dn and fx == 0 and fy == S - 1) or (rgt and dn and fx == S - 1 and fy == S - 1):
                            on = True
                if on:
                    shape.set(ox + x, oy + y, 20 if hop else 10)
    # thicken by 1px (bubbly)
    th = shape.copy()
    for y in range(H):
        for x in range(W):
            if not shape.get(x, y):
                for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    v = shape.get(x + dx, y + dy)
                    if v:
                        th.set(x, y, v)
                        break
    # colour: gradient + highlight; HOP in mint green
    for y in range(H):
        for x in range(W):
            v = th.get(x, y)
            if not v:
                continue
            hop = v == 20
            above = th.get(x, y - 1)
            above2 = th.get(x, y - 2)
            below = th.get(x, y + 1)
            if not above:
                c = 5
            elif not above2:
                c = 4 if not hop else 9
            elif not below or not th.get(x, y + 2):
                c = 2 if not hop else 7
            else:
                c = 3 if not hop else 8
            L.set(x, y, c)
    # thick outline (2px) + drop shadow
    L.outline(1)
    L.outline(15, diag=True)
    sh = Layer(W, H)
    for y in range(H):
        for x in range(W):
            if L.get(x, y) and not L.get(x, y + 2):
                sh.set(x, y + 2, 13)
    for y in range(H):
        for x in range(W):
            if not L.get(x, y) and sh.get(x, y):
                L.set(x, y, 13)
    # sparkle stars around
    for sx, sy in ((2, 4), (226, 48), (150, 2)):
        for d in range(-2, 3):
            L.set(sx + d, sy + 2, 12 if abs(d) < 2 else 11)
            L.set(sx + 2, sy + 2 + d, 12 if abs(d) < 2 else 11)
    return L
