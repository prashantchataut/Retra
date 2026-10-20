"""Original 5x6 pixel font for Sky Sprout, plus a smooth (Scale2x) banner font."""
from pix import Layer

G = {}


def _g(chars, art):
    rows = [r for r in art.strip("\n").split("\n")]
    blocks = [r.split() for r in rows]
    for i, ch in enumerate(chars):
        G[ch] = [b[i] for b in blocks]


_g("ABCDEFGH", """
.###. ####. .###. ####. ##### ##### .###. #...#
#...# #...# #...# #...# #.... #.... #.... #...#
#...# ####. #.... #...# ####. ####. #.### #####
##### #...# #.... #...# #.... #.... #...# #...#
#...# #...# #...# #...# #.... #.... #...# #...#
#...# ####. .###. ####. ##### #.... .###. #...#
""")
_g("IJKLMNOP", """
.###. ..### #...# #.... #...# #...# .###. ####.
..#.. ...#. #..#. #.... ##.## ##..# #...# #...#
..#.. ...#. ###.. #.... #.#.# #.#.# #...# #...#
..#.. ...#. #..#. #.... #...# #..## #...# ####.
..#.. #..#. #...# #.... #...# #...# #...# #....
.###. .##.. #...# ##### #...# #...# .###. #....
""")
_g("QRSTUVWX", """
.###. ####. .#### ##### #...# #...# #...# #...#
#...# #...# #.... ..#.. #...# #...# #...# .#.#.
#...# #...# .###. ..#.. #...# #...# #...# ..#..
#.#.# ####. ....# ..#.. #...# .#.#. #.#.# ..#..
#..#. #..#. ....# ..#.. #...# .#.#. ##.## .#.#.
.##.# #...# ####. ..#.. .###. ..#.. #...# #...#
""")
_g("YZ012345", """
#...# ##### .###. ..#.. .###. ####. ...#. #####
.#.#. ...#. #..## .##.. #...# ....# ..##. #....
..#.. ..#.. #.#.# ..#.. ...#. .###. .#.#. ####.
..#.. .#... ##..# ..#.. ..#.. ....# #..#. ....#
..#.. #.... #...# ..#.. .#... ....# ##### ....#
..#.. ##### .###. .###. ##### ####. ...#. ####.
""")
_g("6789.,!?", """
.###. ##### .###. .###. ..... ..... ..#.. .###.
#.... ....# #...# #...# ..... ..... ..#.. #...#
####. ...#. .###. #...# ..... ..... ..#.. ...#.
#...# ..#.. #...# .#### ..... ..... ..#.. ..#..
#...# ..#.. #...# ....# ..... ..#.. ..... .....
.###. ..#.. .###. .###. ..#.. .#... ..#.. ..#..
""")
_g("-:/'x%+(", """
..... ..... ....# ..#.. ..... ##..# ..... ...#.
..... ..#.. ...#. ..#.. #...# ##.#. ..#.. ..#..
.###. ..... ..#.. ..... .#.#. ..#.. ..#.. ..#..
..... ..... ..#.. ..... ..#.. ..#.. ##### ..#..
..... ..#.. .#... ..... .#.#. .#.## ..#.. ..#..
..... ..... #.... ..... #...# #..## ..#.. ...#.
""")
_g(")&<>*@ #", """
.#... .##.. ...#. .#... ..#.. ..... ..... .#.#.
..#.. #..#. ..#.. ..#.. #.#.# .#.#. ..... #####
..#.. .##.. .#... ...#. .###. ##### ..... .#.#.
..#.. #.#.# .#... ...#. .###. ##### ..... .#.#.
..#.. #..#. ..#.. ..#.. #.#.# .###. ..... #####
.#... .##.# ...#. .#... ..#.. ..#.. ..... .#.#.
""")

FONT_CHARS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?-:/'x%+()&<>*@#"


def glyph(ch):
    return G.get(ch, G[" "])


def glyph_tile(ch, fg, shade, outline):
    """8x8 tile: glyph at (1,1) with bottom-row shade and a full 8-way outline."""
    L = Layer(8, 8)
    g = glyph(ch)
    for y in range(6):
        for x in range(5):
            if g[y][x] == "#":
                L.set(1 + x, 1 + y, shade if y >= 4 else fg)
    if outline:
        add = []
        for y in range(8):
            for x in range(8):
                if L.get(x, y):
                    continue
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        if L.get(x + dx, y + dy) not in (0, outline):
                            add.append((x, y))
                            break
                    else:
                        continue
                    break
        for x, y in add:
            L.set(x, y, outline)
    return L


def panel_glyph_tile(ch, fg, shadow, bgc):
    L = Layer(8, 8, bgc)
    g = glyph(ch)
    for y in range(6):
        for x in range(5):
            if g[y][x] == "#":
                if L.get(2 + x, 2 + y) == bgc:
                    L.set(2 + x, 2 + y, shadow)
    for y in range(6):
        for x in range(5):
            if g[y][x] == "#":
                L.set(1 + x, 1 + y, fg)
    return L


# ------------------------------------------------------------------ banner font
def _scale2x(rows):
    h, w = len(rows), len(rows[0])

    def P(x, y):
        if 0 <= x < w and 0 <= y < h:
            return rows[y][x] == "#"
        return False
    out = [[False] * (2 * w) for _ in range(2 * h)]
    for y in range(h):
        for x in range(w):
            E = P(x, y)
            A, B, C, D = P(x, y - 1), P(x + 1, y), P(x - 1, y), P(x, y + 1)
            e0 = e1 = e2 = e3 = E
            if B != C and A != D:  # (B=up-ish naming kept local)
                pass
            # EPX / Scale2x
            up, rt, lf, dn = A, B, C, D
            if lf == up and lf != dn and up != rt:
                e0 = up
            if up == rt and up != lf and rt != dn:
                e1 = rt
            if dn == lf and dn != rt and lf != up:
                e2 = lf
            if rt == dn and rt != up and dn != lf:
                e3 = dn
            out[2 * y][2 * x] = e0
            out[2 * y][2 * x + 1] = e1
            out[2 * y + 1][2 * x] = e2
            out[2 * y + 1][2 * x + 1] = e3
    return out


def banner(text, ramp, outline, shadow, hi, height=16, space=4):
    """Renders `text` with the smooth big font. Returns a Layer (width multiple of 8)."""
    glyphs = []
    for ch in text:
        if ch == " ":
            glyphs.append(None)
            continue
        g = glyph(ch)
        # trim empty columns for proportional spacing
        cols = [x for x in range(5) if any(g[y][x] == "#" for y in range(6))]
        x0, x1 = (min(cols), max(cols)) if cols else (0, 0)
        trimmed = [r[x0:x1 + 1] for r in g]
        glyphs.append(_scale2x(trimmed))
    width = 2
    for g in glyphs:
        width += space + 1 if g is None else len(g[0]) + 2
    width += 2
    W = (width + 7) // 8 * 8
    L = Layer(W, height)
    x = (W - width) // 2 + 2
    body = Layer(W, height)
    oy = (height - 12) // 2 - 1
    for g in glyphs:
        if g is None:
            x += space + 1
            continue
        gh, gw = len(g), len(g[0])
        for yy in range(gh):
            for xx in range(gw):
                if g[yy][xx]:
                    t = yy / (gh - 1)
                    idx = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
                    body.set(x + xx, oy + yy, idx)
        # glossy highlight on the top-left edge pixels
        for yy in range(gh):
            for xx in range(gw):
                if g[yy][xx] and (yy == 0 or not g[yy - 1][xx]) and yy < gh // 2:
                    body.set(x + xx, oy + yy, hi)
        x += gw + 2
    # drop shadow then outline
    for yy in range(height):
        for xx in range(W):
            if body.get(xx, yy) and not body.get(xx + 1, yy + 1):
                L.set(xx + 1, yy + 1, shadow)
                L.set(xx + 1, yy + 2, shadow)
    L.paste(body)
    add = []
    for yy in range(height):
        for xx in range(W):
            if body.get(xx, yy):
                continue
            for dx in (-1, 0, 1):
                hit = False
                for dy in (-1, 0, 1):
                    if body.get(xx + dx, yy + dy):
                        hit = True
                        break
                if hit:
                    add.append((xx, yy))
                    break
    for xx, yy in add:
        L.set(xx, yy, outline)
    return L
