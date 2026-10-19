"""Original sprite art for Mochi Hop (procedural shapes + hand-placed ASCII stamps)."""
import math
from pix import Layer, pal16

# ---------------------------------------------------------------- palettes
PAL_MOCHI = pal16([
    "000000",  # 0 transparent
    "5a2a4e",  # 1 outline plum
    "e8849f",  # 2 pink shade
    "f9b3c9",  # 3 pink base
    "ffd6e2",  # 4 pink light
    "fff6ec",  # 5 cream highlight
    "ff7b9c",  # 6 blush
    "35203f",  # 7 eye
    "ffffff",  # 8 shine
    "3d8a58",  # 9 leaf dark
    "6fcf7a",  # 10 leaf
    "b9f2a2",  # 11 leaf light
    "b8486a",  # 12 mouth
    "d86b8e",  # 13 feet
    "ffe27a",  # 14 sparkle
    "c7f0ff",  # 15 tear / sweat
])
PAL_STAR = pal16([
    "000000", "a0522d", "f2a93b", "ffd84d", "fff3a6", "ffffff", "4a2a20", "ff8fa8",
    "8fe3ff", "ffb6e1", "b9f2a2", "c9b6ff", "ffffff", "ffffff", "ffffff", "ffffff",
])
PAL_DROP = pal16([
    "000000", "26386e", "4a7fd0", "74b6f2", "aee0ff", "ffffff", "1c2440", "ff9ab0",
    "3a5a9a", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff",
])
PAL_BEE = pal16([
    "000000", "4a2c22", "e9a227", "ffd34a", "fff0a0", "ffffff", "7a4a2a", "d8f4ff",
    "a8d8f0", "ff9ab0", "2a1a14", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff",
])
PAL_SPIKY = pal16([
    "000000", "3a1f4f", "7a4fb0", "a77fe0", "d4b8ff", "ffffff", "241433", "ff9ab0",
    "e070b0", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff",
])
PAL_SPROUT = pal16([
    "000000", "2f5a3a", "5fb06a", "8fdc84", "c8f5a8", "fff8e0", "f0e0b8", "35203f",
    "ff8fa8", "ffffff", "d8b890", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff",
])
PAL_PROPS = pal16([
    "000000",  # 0
    "4e2a3e",  # 1 outline
    "a8704a",  # 2 wood dark
    "dca070",  # 3 wood light
    "e8608c",  # 4 flag shade
    "ff8fb2",  # 5 flag
    "ffc6d8",  # 6 flag light
    "9a90a8",  # 7 grey shade
    "cfc8d8",  # 8 grey
    "fff6ec",  # 9 cream
    "ff6f7f",  # 10 cap red
    "ffb0a0",  # 11 cap light
    "ffd84d",  # 12 gold
    "f2a93b",  # 13 gold shade
    "8fdc84",  # 14 green
    "ffffff",  # 15 white
])
PAL_FX = pal16([
    "000000", "ffffff", "fff6ec", "ffe27a", "ffb0c8", "c7f0ff", "e0d0f0", "ff7b9c",
    "a0e8a0", "ffd84d", "f2a93b", "8fb8ff", "ffffff", "ffffff", "ffffff", "ffffff",
])

SPRITE_PALETTES = [PAL_MOCHI, PAL_STAR, PAL_DROP, PAL_BEE, PAL_SPROUT, PAL_SPIKY, PAL_PROPS, PAL_FX]


# ---------------------------------------------------------------- helpers
def blob(w, h, cx, cy, a, bt, bb, p=2.4):
    def inside(x, y):
        dx = (x + 0.5 - cx) / a
        yy = y + 0.5 - cy
        dy = yy / (bt if yy < 0 else bb)
        return abs(dx) ** p + abs(dy) ** p <= 1.0, dx, dy
    return inside


# ---------------------------------------------------------------- Mochi
LEAF = """
....gg.
..ggLLg
.gGLLGg
.gGGGg.
..ggg..
...g...
"""

EYES = {
    "open": """
Wee
eee
eeW
.e.
""",
    "up": """
Wee
eeW
.e.
...
""",
    "wide": """
WWe
Wee
eee
eee
""",
    "blink": """
...
...
eee
...
""",
    "happy": """
...
.e.
e.e
...
""",
    "hurt": """
e..
.e.
e..
...
""",
}


def mochi_frame(face="open", body_dy=0, feet=None, sx=1.0, sy=1.0, mouth="smile"):
    L = Layer(32, 32)
    cx = 16.0
    bottom = 30 - body_dy
    a = 10.0 * sx
    bt = 9.0 * sy
    bb = 6.0 * sy
    cy = bottom - bb
    inside = blob(32, 32, cx, cy, a, bt, bb)
    body = Layer(32, 32)

    def col(x, y):
        ok, dx, dy = inside(x, y)
        if not ok:
            return None
        r = abs(dx) ** 2.4 + abs(dy) ** 2.4
        # highlight oval top-left
        hx = (x + 0.5 - (cx - 4.5)) / 2.6
        hy = (y + 0.5 - (cy - 5.0)) / 1.6
        if hx * hx + hy * hy <= 1.0:
            return 5
        lx = (x + 0.5 - (cx - 3.5)) / 5.5
        ly = (y + 0.5 - (cy - 4.0)) / 3.2
        if lx * lx + ly * ly <= 1.0:
            return 4
        if dy > 0.45 and r > 0.45:
            return 2
        if dx > 0.72 and dy > -0.2:
            return 2
        return 3
    body.fill_fn(col)
    # feet (drawn before outline so they share it)
    if feet:
        for fx, fy in feet:
            for yy in range(2):
                for xx in range(4):
                    if yy == 1 and xx in (0, 3):
                        continue
                    body.set(int(cx) + fx + xx, bottom + fy + yy, 13)
    body.outline(1)
    L.paste(body)
    # leaf stem sits on top of the head
    top = int(cy - bt)
    L.stamp(LEAF, int(cx) - 2, top - 5, {"g": 9, "G": 10, "L": 11})
    # face
    ey = int(cy) - 2
    if face == "up":
        ey -= 1
    art = EYES[face]
    leg = {"e": 7, "W": 8}
    L.stamp(art, int(cx) - 6, ey, leg)
    L.stamp(art, int(cx) + 3, ey, leg, flip=(face == "hurt"))
    # blush
    by = ey + 4
    for bx in (int(cx) - 8, int(cx) - 7, int(cx) + 6, int(cx) + 7):
        L.set(bx, by, 6)
    # mouth
    my = ey + 4
    mx = int(cx) - 1
    if mouth == "smile":
        L.set(mx - 1, my, 12)
        L.set(mx, my + 1, 12)
        L.set(mx + 1, my + 1, 12)
        L.set(mx + 2, my, 12)
    elif mouth == "open":
        L.set(mx, my, 12)
        L.set(mx + 1, my, 12)
        L.set(mx, my + 1, 12)
        L.set(mx + 1, my + 1, 12)
    elif mouth == "o":
        L.set(mx, my + 1, 12)
        L.set(mx + 1, my + 1, 12)
    return L


def mochi_frames():
    F = []
    F.append(mochi_frame("open"))                                         # 0 idle
    F.append(mochi_frame("blink"))                                        # 1 blink
    F.append(mochi_frame("open", 2, [(-7, -1), (3, 0)]))                  # 2 run a
    F.append(mochi_frame("open", 1, [(-5, 0), (1, 0)]))                   # 3 run b
    F.append(mochi_frame("open", 2, [(-6, 0), (4, -1)]))                  # 4 run c
    F.append(mochi_frame("open", 1, [(-4, 0), (0, 0)]))                   # 5 run d
    F.append(mochi_frame("up", 2, [(-6, -1), (2, -1)], mouth="open"))     # 6 jump
    F.append(mochi_frame("wide", 2, [(-7, 0), (3, 0)], mouth="o"))        # 7 fall
    F.append(mochi_frame("happy", 0, mouth="open"))                       # 8 happy
    F.append(mochi_frame("hurt", 0, mouth="o"))                           # 9 hurt
    return F


# ---------------------------------------------------------------- star
def star_poly(cx, cy, ro, ri, xs=1.0, rot=-math.pi / 2):
    pts = []
    for i in range(10):
        r = ro if i % 2 == 0 else ri
        a = rot + i * math.pi / 5
        pts.append((cx + math.cos(a) * r * xs, cy + math.sin(a) * r))
    return pts


def in_poly(x, y, pts):
    ins = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi):
            ins = not ins
        j = i
    return ins


def star_frame(xs, face=True, size=16, ro=7.6, ri=3.6):
    L = Layer(size, size)
    c = size / 2
    pts = star_poly(c, c + 0.6, ro, ri, xs)
    inner = star_poly(c, c + 0.6, ro - 2.2, ri - 1.0, xs)
    body = Layer(size, size)

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        if not in_poly(px, py, pts):
            return None
        if in_poly(px, py, inner):
            # light from top-left
            if (px - c) * xs + (py - c) < -2.5:
                return 4
            return 3
        if (px - c) + (py - c) > 0:
            return 2
        return 3
    body.fill_fn(col)
    body.outline(1)
    L.paste(body)
    if face and xs > 0.6:
        ex = int(c - 2 * xs)
        L.set(ex, int(c) + 1, 6)
        L.set(ex, int(c) + 2, 6)
        L.set(int(c + 1.6 * xs), int(c) + 1, 6)
        L.set(int(c + 1.6 * xs), int(c) + 2, 6)
        L.set(ex - 1, int(c) + 3, 7)
        L.set(int(c + 1.6 * xs) + 1, int(c) + 3, 7)
    return L


def star_frames():
    return [star_frame(1.0), star_frame(0.8), star_frame(0.55, face=False), star_frame(0.8).hflip()]


# ---------------------------------------------------------------- raindrop
def drop_frame(sq=0, flat=False):
    L = Layer(16, 16)
    body = Layer(16, 16)
    if flat:
        def col(x, y):
            dx = (x + 0.5 - 8) / 7.0
            dy = (y + 0.5 - 13.5) / 2.2
            if dx * dx + dy * dy <= 1:
                return 4 if dy < -0.3 and dx < 0 else (2 if dy > 0.3 else 3)
            return None
        body.fill_fn(col)
        body.outline(1)
        L.paste(body)
        L.set(5, 13, 6); L.set(6, 13, 6); L.set(9, 13, 6); L.set(10, 13, 6)
        return L
    r = 5.6 + sq * 0.5
    cy = 9.5 + sq * 0.5
    top = 2.0 + sq

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx = (px - 8) / (r)
        dy = (py - cy) / (r - sq * 0.8)
        ins = dx * dx + dy * dy <= 1
        if not ins and py < cy and py >= top:
            # cone to the tip
            t = (cy - py) / (cy - top)
            half = r * (1 - t) ** 1.3
            ins = abs(px - 8) <= half
        if not ins:
            return None
        hx = (px - 6) / 1.8
        hy = (py - (cy - 2)) / 2.4
        if hx * hx + hy * hy <= 1:
            return 4
        if dx > 0.3 and dy > 0.1 or dy > 0.7:
            return 2
        return 3
    body.fill_fn(col)
    body.outline(1)
    L.paste(body)
    ey = int(cy) + 0
    # grumpy brows + eyes
    L.set(5, ey - 1, 6); L.set(6, ey, 6)
    L.set(10, ey - 1, 6); L.set(9, ey, 6)
    L.set(6, ey + 1, 6); L.set(6, ey + 2, 6)
    L.set(9, ey + 1, 6); L.set(9, ey + 2, 6)
    L.set(7, ey + 3 + sq // 2, 6); L.set(8, ey + 3 + sq // 2, 6)
    L.set(4, ey + 3, 7); L.set(11, ey + 3, 7)
    return L


# ---------------------------------------------------------------- bee
def bee_frame(wing):
    L = Layer(16, 16)
    # wings (behind)
    wings = Layer(16, 16)
    wy = 2 if wing == 0 else 4

    def wcol(x, y):
        for wx in (6.0, 10.5):
            dx = (x + 0.5 - wx) / 2.4
            dy = (y + 0.5 - (wy + 2)) / 2.6
            if dx * dx + dy * dy <= 1:
                return 7 if dy < 0.2 else 8
        return None
    wings.fill_fn(wcol)
    wings.outline(1)
    L.paste(wings)
    body = Layer(16, 16)

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx = (px - 8) / 6.4
        dy = (py - 10.5) / 4.6
        if dx * dx + dy * dy > 1:
            return None
        if 9.0 <= px < 10.6 or 12.4 <= px < 14.0:
            return 10
        if dx < -0.1 and dy < -0.2:
            return 4
        if dy > 0.45:
            return 2
        return 3
    body.fill_fn(col)
    # stinger
    body.set(15, 11, 10)
    body.outline(1)
    L.paste(body)
    # face on the left (facing left by default)
    L.set(4, 9, 10); L.set(4, 10, 10)
    L.set(6, 9, 10); L.set(6, 10, 10)
    L.set(3, 8, 10); L.set(7, 8, 10)
    L.set(3, 12, 9)
    L.set(5, 12, 1)
    return L


# ---------------------------------------------------------------- spiky
def spiky_frame(ph):
    L = Layer(16, 16)
    body = Layer(16, 16)
    cx, cy = 8.0, 9.0

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx, dy = px - cx, py - cy
        d = math.hypot(dx, dy)
        ang = math.atan2(dy, dx) + ph * math.pi / 8
        spike = 1.9 * max(0.0, math.cos(ang * 4)) ** 3
        if d > 4.9 + spike:
            return None
        if d > 4.9:
            return 4 if dy < 0 else 3
        if dx + dy < -3:
            return 4
        if dx + dy > 2.5:
            return 2
        return 3
    body.fill_fn(col)
    body.outline(1)
    L.paste(body)
    L.set(6, 8, 6); L.set(6, 9, 6); L.set(10, 8, 6); L.set(10, 9, 6)
    L.set(5, 7, 6); L.set(11, 7, 6)
    L.set(7, 11, 6); L.set(8, 11, 6); L.set(9, 11, 6)
    L.set(4, 10, 7); L.set(12, 10, 7)
    return L


# ---------------------------------------------------------------- sprout
SPROUT_LEAVES = [
"""
..LL....LL..
.LgGL..LGgL.
.LgGGLLGGgL.
..LLGGGGLL..
.....LL.....
.....LL.....
""",
"""
............
..LL....LL..
.LgGLLLLGgL.
.LgGGGGGGgL.
..LL.LL.LL..
.....LL.....
""",
]


def sprout_frame(i):
    L = Layer(16, 16)
    body = Layer(16, 16)

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx = (px - 8) / 5.0
        dy = (py - 12.0) / (3.6 if py < 12 else 3.0)
        if dx * dx + dy * dy > 1:
            return None
        if dx < -0.2 and dy < -0.2:
            return 5
        if dy > 0.4:
            return 10
        return 6
    body.fill_fn(col)
    body.outline(1)
    L.paste(body)
    lv = Layer(16, 16)
    lv.stamp(SPROUT_LEAVES[i], 2, 2, {"L": 1, "g": 2, "G": 3})
    L.paste(lv)
    L.set(6, 11, 7); L.set(6, 12, 7); L.set(9, 11, 7); L.set(9, 12, 7)
    L.set(5, 13, 8); L.set(10, 13, 8)
    L.set(7, 13, 1); L.set(8, 13, 1)
    return L


# ---------------------------------------------------------------- props
def flag_frame(state):
    """16x32 checkpoint flag. state 0 = inactive, 1/2 = waving."""
    L = Layer(16, 32)
    pole = Layer(16, 32)
    for y in range(4, 31):
        pole.set(3, y, 3)
        pole.set(4, y, 2)
    # knob
    for (x, y) in [(3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3)]:
        pole.set(x, y, 12)
    pole.set(3, 2, 15) if state else None
    # base
    for y in (29, 30):
        for x in range(0, 9):
            pole.set(x, y, 3 if y == 29 else 2)
    pole.outline(1)
    L.paste(pole)
    fl = Layer(16, 32)
    if state == 0:
        def col(x, y):
            if 5 <= x <= 8 and 5 <= y <= 4 + (x - 3) * 3 and y <= 16:
                return 8 if x < 7 else 7
            return None
    else:
        ph = 0 if state == 1 else 1

        def col(x, y):
            if x < 5 or x > 14:
                return None
            off = int(round(math.sin((x - 5) * 0.7 + ph * 2.4) * 1.2))
            t0 = 5 + off
            t1 = 13 - (x - 5) * 0.35 + off
            if t0 <= y <= t1:
                if y == t0:
                    return 6
                return 5 if (x + y) % 5 else 4
            return None
    fl.fill_fn(col)
    fl.outline(1)
    L.paste(fl)
    if state:
        # tiny heart emblem
        for x, y in [(8, 8), (10, 8), (8, 9), (9, 9), (10, 9), (9, 10)]:
            L.set(x, y + (0 if state == 1 else 1), 15)
    return L


def goal_frame(ph):
    """32x32 goal: a big glowing star on a little flower stand."""
    L = Layer(32, 32)
    stand = Layer(32, 32)
    for y in range(18, 31):
        stand.set(15, y, 14)
        stand.set(16, y, 14)
    for (x, y) in [(12, 24), (13, 24), (13, 23), (14, 25), (19, 22), (18, 22), (18, 21), (17, 23)]:
        stand.set(x, y, 14)
    for x in range(10, 22):
        stand.set(x, 30, 3)
        stand.set(x, 29, 3)
    stand.outline(1)
    L.paste(stand)
    st = star_frame(1.0 if ph == 0 else 0.92, face=False, size=24, ro=11.0, ri=5.2)
    # recolour star into props palette
    remap = {1: 1, 2: 13, 3: 12, 4: 15}
    s2 = Layer(24, 24)
    for y in range(24):
        for x in range(24):
            c = st.px[y][x]
            if c:
                s2.px[y][x] = remap.get(c, c)
    L.paste(s2, 4, -1 + ph)
    # happy face
    fy = 11 + ph
    L.set(13, fy, 1); L.set(13, fy + 1, 1); L.set(18, fy, 1); L.set(18, fy + 1, 1)
    L.set(15, fy + 3, 1); L.set(16, fy + 3, 1)
    L.set(11, fy + 2, 5); L.set(20, fy + 2, 5)
    return L


def spring_frame(compressed):
    L = Layer(16, 16)
    b = Layer(16, 16)
    capy = 5 if not compressed else 9

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx = (px - 8) / 7.0
        dy = (py - (capy + 3.5)) / 3.6
        if dy <= 0.35 and dx * dx + dy * dy <= 1:
            if (x, y - capy) in [(4, 1), (5, 1), (10, 2), (11, 2), (7, 0), (8, 0)]:
                return 15
            return 11 if dy < -0.5 else 10
        if 5 <= x <= 10 and capy + 4 <= y <= 14:
            return 9 if x < 8 else 8
        return None
    b.fill_fn(col)
    b.outline(1)
    L.paste(b)
    L.set(6, capy + 5, 1); L.set(9, capy + 5, 1)
    return L


def node_frame(kind):
    """16x16 level-select node. 0 locked, 1 open, 2 cleared, 3 perfect."""
    L = Layer(16, 16)
    b = Layer(16, 16)
    base = {0: (7, 8), 1: (4, 5), 2: (13, 12), 3: (13, 12)}[kind]

    def col(x, y):
        px, py = x + 0.5, y + 0.5
        dx = (px - 8) / 6.5
        dy = (py - 8.5) / 5.0
        if dx * dx + dy * dy > 1:
            return None
        if dy < -0.35 and dx < 0.2:
            return 15 if kind else 8
        return base[1] if dy < 0.3 else base[0]
    b.fill_fn(col)
    b.outline(1)
    L.paste(b)
    if kind == 0:
        # keyhole
        for x, y in [(7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)]:
            L.set(x, y, 1)
    elif kind == 1:
        for x, y in [(7, 7), (8, 7), (7, 8), (8, 8), (6, 8), (9, 8), (7, 9), (8, 9)]:
            L.set(x, y, 15)
    else:
        st = star_frame(1.0, face=False, size=16, ro=4.5, ri=2.1)
        for y in range(16):
            for x in range(16):
                if st.px[y][x] in (3, 4, 2):
                    L.set(x, y + 1, 15 if kind == 3 else 9)
                elif st.px[y][x] == 1:
                    L.set(x, y + 1, 4 if kind == 3 else 13)
    return L


# ---------------------------------------------------------------- particles (8x8)
def fx_frames():
    F = []
    # 0-2 dust puff big->small
    for r in (3.4, 2.6, 1.6):
        L = Layer(8, 8)
        L.fill_fn(lambda x, y, r=r: (2 if (x + 0.5 - 4) ** 2 + (y + 0.5 - 4) ** 2 <= r * r * 0.5 else 1)
                  if (x + 0.5 - 4) ** 2 + (y + 0.5 - 4) ** 2 <= r * r else None)
        F.append(L)
    # 3-4 sparkle
    for big in (1, 0):
        L = Layer(8, 8)
        n = 3 if big else 2
        for i in range(-n, n + 1):
            L.set(4 + i, 4, 3 if abs(i) > 1 else 1)
            L.set(4, 4 + i, 3 if abs(i) > 1 else 1)
        if big:
            for d in (-1, 1):
                L.set(4 + d, 4 + d, 3); L.set(4 + d, 4 - d, 3)
        F.append(L)
    # 5 heart
    L = Layer(8, 8)
    L.stamp("""
.pp.pp.
pwppppp
ppppppp
.ppppp.
..ppp..
...p...
""", 0, 1, {"p": 7, "w": 1})
    F.append(L)
    # 6 small star
    L = Layer(8, 8)
    L.stamp("""
...a...
..aya..
aayyyaa
.ayyya.
.ayaya.
.aa.aa.
""", 0, 1, {"a": 10, "y": 9})
    L.set(3, 3, 3)
    F.append(L)
    # 7 droplet splash
    L = Layer(8, 8)
    L.stamp("""
..b.
.bbb
.bwb
..b.
""", 2, 2, {"b": 11, "w": 5})
    F.append(L)
    return F
