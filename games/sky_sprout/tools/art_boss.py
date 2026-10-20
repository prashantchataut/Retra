"""Original boss and mid-boss art for Sky Sprout (procedural, mech palette layout).

Each boss is a set of parts; each part is a list of frames drawn on its own
canvas and split into OBJ pieces by gen_assets.py.
"""
import math
from pix import Layer
import paint as P
from art_sprites import HULL, ACC, GLOW, METAL, mech_pal

W14 = 14
PUP = 15


def put(L, S, outline=1, ox=0, oy=0):
    P.outline_sel(S, outline)
    L.paste(S, ox, oy)
    return L


def tint(pal, toward=(255, 255, 255), k=0.45, skip=(0,)):
    out = []
    for i, c in enumerate(pal):
        if i in skip:
            out.append(c)
        else:
            out.append(tuple(int(c[j] + (toward[j] - c[j]) * k) for j in range(3)))
    return out


# ================================================================== Boss 1: Baron Buzzsaw
def buzzsaw_body():
    L = Layer(128, 96)
    # legs (behind everything)
    lg = Layer(128, 96)
    for x0 in (46, 58, 70):
        P.line(lg, x0, 62, x0 - 7, 75, 12, 2)
        P.line(lg, x0 - 7, 75, x0 - 3, 85, 12, 2)
        P.line(lg, x0 - 6, 75, x0 - 2, 84, 13)
        lg.set(x0 - 7, 75, 13)
    put(L, lg)
    # stinger cannon
    st = Layer(128, 96)
    P.poly(st, [(104, 47), (127, 56), (104, 65)], ramp=[12, 13, W14], grad=(0.9, 0.0))
    P.rect(st, 100, 50, 108, 62, 12)
    put(L, st)
    # striped abdomen
    ab = Layer(128, 96)

    def abd(x, y, v, nx, ny):
        band = int((nx + 1.25) * 3.2) % 2
        return P.pick(v, HULL if band else [6, 7, 8, 8], x, y, True)
    P.ellipse_cb(ab, 80, 54, 29, 23, abd, amb=0.25)
    put(L, ab)
    # thorax
    th = Layer(128, 96)
    P.ellipse(th, 52, 47, 21, 20, HULL, amb=0.25, dither=True)
    put(L, th)
    for a in range(-60, 61, 30):
        r = math.radians(a)
        L.set(int(52 + math.cos(r) * 15), int(47 + math.sin(r) * 15), 5)
        L.set(int(52 + math.cos(r) * 15) + 1, int(47 + math.sin(r) * 15) + 1, 2)
    # fuzzy collar
    co = Layer(128, 96)
    for i in range(7):
        a = math.radians(-70 + i * 23)
        P.ellipse(co, 38 + math.cos(a) * 4, 52 + math.sin(a) * 18, 4.2, 4.2, [6, 7, 8], amb=0.3)
    put(L, co)
    # head
    hd = Layer(128, 96)
    P.ellipse(hd, 28, 54, 17, 16, HULL, amb=0.25, dither=True)
    put(L, hd)
    # compound eye with facets
    ey = Layer(128, 96)

    def eye(x, y, v, nx, ny):
        c = P.pick(v * 1.1, GLOW, x, y)
        if (x % 4 == 0) or ((y + (x // 4) * 2) % 4 == 0):
            c = 9 if c != 9 else 1
        return c
    P.ellipse_cb(ey, 22, 50, 10.5, 11.5, eye, amb=0.35)
    put(L, ey)
    P.rect(L, 17, 44, 19, 46, W14)
    L.set(16, 47, W14)
    # mouth plate + mandibles
    mp = Layer(128, 96)
    P.poly(mp, [(12, 62), (26, 60), (30, 70), (18, 72)], ramp=[12, 13], grad=(1, 0))
    put(L, mp)
    # antennae
    for (x0, y0, x1, y1) in ((30, 39, 22, 18), (36, 40, 34, 16)):
        P.line(L, x0, y0, (x0 + x1) // 2 - 3, (y0 + y1) // 2, 1, 2)
        P.line(L, (x0 + x1) // 2 - 3, (y0 + y1) // 2, x1, y1, 1, 2)
        P.line(L, x0, y0, (x0 + x1) // 2 - 3, (y0 + y1) // 2, 12)
        P.line(L, (x0 + x1) // 2 - 3, (y0 + y1) // 2, x1, y1, 12)
        b = Layer(128, 96)
        P.ellipse(b, x1, y1 - 1, 3.2, 3.2, GLOW, amb=0.4)
        put(L, b)
    # rivet line on the abdomen bands
    for x in range(62, 104, 9):
        L.set(x, 40, 5)
    return L


def buzzsaw_wings(f):
    L = Layer(64, 48)
    for k, (cx, cy, rx, ry, a) in enumerate(((30, 22, 25, 9.5, -0.55 if f == 0 else -0.15),
                                              (40, 26, 20, 7.5, -0.95 if f == 0 else -0.35))):
        w = Layer(64, 48)
        ca, sa = math.cos(a), math.sin(a)
        for y in range(48):
            for x in range(64):
                dx, dy = x + 0.5 - (cx + 14), y + 0.5 - (cy + 8)
                u = dx * ca + dy * sa + rx * 0.9
                v = -dx * sa + dy * ca
                if (u / rx - 0.9) ** 2 + (v / ry) ** 2 <= 1.0:
                    t = (u / (2 * rx))
                    w.set(x, y, 8 if t > 0.55 else (7 if t > 0.2 else 6))
        # veins
        for i in range(3):
            for s in range(int(rx * 1.6)):
                x = cx + 14 + (s - rx * 0.9) * ca - (i - 1) * 2.5 * sa
                y = cy + 8 + (s - rx * 0.9) * sa + (i - 1) * 2.5 * ca
                if w.get(int(x), int(y)):
                    w.set(int(x), int(y), 4)
        put(L, w, 1)
    return L


def saw_frame(f):
    L = Layer(32, 32)
    rot = f * math.pi / 18
    pts = []
    n = 12
    for i in range(n * 2):
        a = rot + i * math.pi / n
        r = 15.2 if i % 2 == 0 else 11.5
        pts.append((16 + math.cos(a) * r, 16 + math.sin(a) * r))
    s = Layer(32, 32)
    P.poly(s, pts, col=4)
    P.ellipse(s, 16, 16, 11.5, 11.5, HULL, amb=0.3)
    put(L, s)
    P.ring(L, 16, 16, 6.5, 7.5, 2)
    for i in range(4):
        a = rot * 2 + i * math.pi / 2
        P.line(L, 16 + math.cos(a) * 3, 16 + math.sin(a) * 3, 16 + math.cos(a) * 9, 16 + math.sin(a) * 9, 5)
    P.disc(L, 16, 16, 3.3, 10)
    P.disc(L, 15.5, 15.5, 1.6, 11)
    return L


BUZZSAW = {
    "name": "BARON BUZZSAW",
    "pals": [mech_pal(("3a2a4e", "5e4878", "8a74a8", "bcaad6"), ("c08a1c", "f2c63c", "fff0a0"),
                      ("a8186a", "ff4aa0", "ffcce6"), ("5c5470", "a8a0bc")),
             mech_pal(("44445a", "707088", "a2a2ba", "dcdcea"), ("5e9ec4", "a6dcf2", "eafcff"),
                      ("c08020", "f0c040", "fff2a8"), ("5c5470", "a8a0bc"))],
    "parts": [("body", [buzzsaw_body()], 0), ("wings", [buzzsaw_wings(0), buzzsaw_wings(1)], 1),
              ("saw", [saw_frame(i) for i in range(3)], 1)],
}


# ================================================================== Boss 2: Thunderhull
def thunder_body():
    L = Layer(128, 80)
    # keel fins
    k = Layer(128, 80)
    P.poly(k, [(96, 40), (124, 22), (126, 30), (110, 44)], ramp=ACC, grad=(1, 0.2))
    P.poly(k, [(96, 50), (124, 66), (126, 58), (110, 46)], ramp=ACC, grad=(0.6, 0.0))
    put(L, k)
    # main hull
    h = Layer(128, 80)

    def hull(x, y, v, nx, ny):
        c = P.pick(v, HULL, x, y, True)
        if abs(ny - 0.32) < 0.035 or abs(ny + 0.3) < 0.03:
            c = 2
        if (x % 12 == 0) and -0.6 < ny < 0.6:
            c = 2 if c != 2 else 1
        return c
    P.ellipse_cb(h, 62, 45, 56, 18, hull, amb=0.22)
    # ram prow
    P.poly(h, [(2, 47), (16, 37), (18, 56)], ramp=[12, 13, W14], grad=(1, 0.1))
    put(L, h)
    # hazard band
    for x in range(20, 104):
        y = 54 + int(2 * math.sin(x * 0.05))
        if L.get(x, y) not in (0, 1):
            L.set(x, y, 7 if (x // 4) % 2 else 1)
            L.set(x, y + 1, 6 if (x // 4) % 2 else 1)
    # bridge tower
    b = Layer(128, 80)
    P.poly(b, [(46, 30), (52, 16), (76, 16), (82, 30)], ramp=HULL, grad=(1.0, 0.3))
    P.rect(b, 56, 10, 72, 16, 3)
    put(L, b)
    for x in range(56, 74, 4):
        P.rect(L, x, 20, x + 2, 22, 10)
        L.set(x, 20, 11)
    # tesla coil masts (sparks are a separate part)
    for cx in (34, 92):
        m = Layer(128, 80)
        P.rect(m, cx - 2, 8, cx + 2, 32, 12)
        P.line(m, cx - 2, 8, cx - 2, 32, 13)
        for y in range(10, 30, 4):
            P.rect(m, cx - 4, y, cx + 4, y + 1, 7)
        P.ellipse(m, cx, 6, 4.5, 4.5, GLOW, amb=0.4)
        put(L, m)
        L.set(cx - 1, 4, W14)
    # central core window
    c = Layer(128, 80)
    P.ellipse(c, 60, 44, 10, 8, GLOW, amb=0.3)
    put(L, c)
    P.ring(L, 60, 44, 9, 11, 13)
    P.rect(L, 56, 41, 58, 42, W14)
    # portholes
    for x in (28, 40, 82, 94):
        P.disc(L, x, 44, 2.2, 1)
        P.disc(L, x, 44, 1.4, 10)
    # underside turrets
    for x in (36, 70, 98):
        t = Layer(128, 80)
        P.ellipse(t, x, 63, 5, 3.6, HULL, amb=0.3)
        P.rect(t, x - 10, 63, x - 4, 64, 12)
        put(L, t)
    return L


def thunder_clouds(f):
    L = Layer(128, 32)
    import random
    rnd = random.Random(7 + f)
    circles = []
    for i in range(12):
        x = 8 + i * 10 + rnd.randint(-2, 2)
        circles.append((x, 12 + rnd.randint(-2, 3), 7 + rnd.randint(0, 3)))
    P.blobs(L, circles, HULL, amb=0.3, dither=True)
    P.outline_sel(L, 1)
    return L


def spark_frame(f):
    L = Layer(32, 32)
    import random
    rnd = random.Random(11 + f * 5)
    for b in range(3):
        x, y = 16.0, 16.0
        a = rnd.random() * math.pi * 2
        for s in range(6):
            a += (rnd.random() - 0.5) * 1.6
            nx, ny = x + math.cos(a) * 2.6, y + math.sin(a) * 2.6
            P.line(L, x, y, nx, ny, 8 if s < 3 else 7)
            x, y = nx, ny
    P.disc(L, 16, 16, 3.0 + f, 7)
    P.disc(L, 16, 16, 1.8 + f * 0.6, 8)
    return L


THUNDER = {
    "name": "THUNDERHULL",
    "pals": [mech_pal(("232c46", "3a486a", "5e7098", "96acd0"), ("b08418", "e8c03c", "fff0a0"),
                      ("1a76c0", "4ad0ff", "dcfcff"), ("525a74", "a0a8c0")),
             mech_pal(("34324e", "504e70", "7a789c", "acaac8"), ("8080ff", "c0c4ff", "ffffff"),
                      ("2080d0", "60d0ff", "e4ffff"), ("525a74", "a0a8c0"))],
    "parts": [("body", [thunder_body()], 0), ("clouds", [thunder_clouds(0), thunder_clouds(1)], 1),
              ("spark", [spark_frame(i) for i in range(3)], 1)],
}


# ================================================================== Boss 3: Admiral Clamp
def clamp_body():
    L = Layer(112, 80)
    # legs
    lg = Layer(112, 80)
    for i, x0 in enumerate((40, 56, 72, 86)):
        P.line(lg, x0, 56, x0 + 6, 68, 12, 3)
        P.line(lg, x0 + 6, 68, x0 + 2, 78, 12, 2)
        lg.set(x0 + 6, 68, 13)
    put(L, lg)
    # shell dome
    sh = Layer(112, 80)

    def shell(x, y, v, nx, ny):
        c = P.pick(v, HULL, x, y, True)
        rr = math.sqrt(nx * nx + ny * ny)
        if abs(rr - 0.55) < 0.03 or abs(rr - 0.85) < 0.025:
            c = 2
        if ny > 0.55:
            c = 6 if ny < 0.7 else 7
        return c
    P.ellipse_cb(sh, 62, 46, 42, 28, shell, amb=0.22)
    put(L, sh)
    # rivets along rings
    for a in range(200, 345, 12):
        r = math.radians(a)
        L.set(int(62 + math.cos(r) * 42 * 0.7), int(46 + math.sin(r) * 28 * 0.7), 5)
    # portholes
    for (x, y) in ((50, 40), (66, 34), (82, 40)):
        p = Layer(112, 80)
        P.ellipse(p, x, y, 4.6, 4.6, GLOW, amb=0.4)
        put(L, p)
        P.ring(L, x, y, 4.0, 5.2, 13)
        L.set(x - 2, y - 2, W14)
    # periscope
    pe = Layer(112, 80)
    P.rect(pe, 70, 6, 73, 19, 12)
    P.rect(pe, 64, 4, 73, 8, 13)
    put(L, pe)
    L.set(64, 5, 10)
    # eye stalks
    for (sx, sy) in ((26, 22), (38, 16)):
        P.line(L, sx + 6, sy + 16, sx, sy + 2, 1, 3)
        P.line(L, sx + 6, sy + 16, sx, sy + 2, 12)
        e = Layer(112, 80)
        P.ellipse(e, sx, sy, 5.5, 5.5, [12, 13, W14, W14], amb=0.4)
        put(L, e)
        P.disc(L, sx - 1.5, sy, 2.4, 9)
        P.disc(L, sx - 2, sy, 1.2, PUP)
        L.set(sx - 1, sy - 2, W14)
    # mouth vents
    for i in range(4):
        P.line(L, 22, 46 + i * 3, 30, 46 + i * 3, 1)
    return L


def clamp_claw(f):
    L = Layer(48, 40)
    arm = Layer(48, 40)
    P.line(arm, 46, 30, 34, 24, 12, 4)
    P.ellipse(arm, 33, 24, 5, 5, HULL, amb=0.3)
    put(L, arm)
    c = Layer(48, 40)
    # upper pincer
    gap = 8 if f == 0 else 1
    P.poly(c, [(30, 18), (12, 8 - gap * 0.5), (2, 14 - gap * 0.6), (10, 16), (28, 24)], ramp=HULL, grad=(1, 0.15))
    # lower pincer (heavier)
    P.poly(c, [(30, 26), (14, 34 + gap * 0.4), (3, 30 + gap * 0.3), (12, 25), (28, 22)], ramp=HULL, grad=(0.85, 0.05))
    P.ellipse(c, 27, 22, 8, 8, HULL, amb=0.3)
    put(L, c)
    for x, y in ((8, 14), (14, 12), (9, 29), (15, 31)):
        L.set(x, y, 8)
    L.set(25, 18, 5)
    L.set(24, 19, 5)
    return L


CLAMP = {
    "name": "ADMIRAL CLAMP",
    "pals": [mech_pal(("561c20", "903830", "cc6248", "f6a07a"), ("1c6a70", "34a6a8", "9ae6dc"),
                      ("c08018", "f8d040", "fff8c0"), ("4e5a66", "9eaab8")),
             mech_pal(("6a1e18", "a83c28", "e26c40", "ffb07a"), ("1c6a70", "34a6a8", "9ae6dc"),
                      ("c08018", "f8d040", "fff8c0"), ("4e5a66", "9eaab8"))],
    "parts": [("body", [clamp_body()], 0), ("claw", [clamp_claw(0), clamp_claw(1)], 1)],
}


# ================================================================== Boss 4: Ticktock Titan
def titan_body():
    L = Layer(96, 96)
    # ear tufts
    t = Layer(96, 96)
    P.poly(t, [(18, 26), (12, 2), (34, 18)], ramp=ACC, grad=(1, 0.2))
    P.poly(t, [(78, 26), (84, 2), (62, 18)], ramp=ACC, grad=(1, 0.2))
    put(L, t)
    # body egg with feather scallops
    b = Layer(96, 96)

    def body(x, y, v, nx, ny):
        c = P.pick(v, HULL, x, y, True)
        if ny > -0.2:
            row = int((ny + 0.2) * 9)
            off = (row % 2) * 3
            if (y - 40) % 6 == 0 and abs(nx) < 0.85 and ((x + off) % 6 != 0):
                c = 2
        return c
    P.ellipse_cb(b, 48, 54, 36, 40, body, amb=0.22)
    put(L, b)
    # facial disc
    fd = Layer(96, 96)
    P.ellipse(fd, 36, 36, 16, 15, [4, 5, 5], amb=0.5)
    P.ellipse(fd, 60, 36, 16, 15, [4, 5, 5], amb=0.5)
    put(L, fd, 2)
    # big clockwork eyes
    for ex in (35, 61):
        e = Layer(96, 96)
        P.ellipse(e, ex, 36, 11, 11, [12, 13, 13], amb=0.4)
        put(L, e)
        P.ellipse(L, ex, 36, 8, 8, GLOW, amb=0.35)
        P.ring(L, ex, 36, 4, 5, 9)
        P.disc(L, ex + 1, 37, 3.2, PUP)
        P.rect(L, ex - 4, 31, ex - 3, 32, W14)
        for i in range(12):
            a = i * math.pi / 6
            L.set(int(ex + 0.5 + math.cos(a) * 9.6), int(36.5 + math.sin(a) * 9.6), 1)
    # beak
    bk = Layer(96, 96)
    P.poly(bk, [(42, 44), (54, 44), (48, 58)], ramp=[6, 7, 8], grad=(1, 0.2))
    put(L, bk)
    # belly clock face (hands are separate affine sprites)
    cf = Layer(96, 96)
    P.disc(cf, 48, 70, 15.5, 13)
    P.disc(cf, 48, 70, 13.5, W14)
    put(L, cf)
    P.ring(L, 48, 70, 13.5, 14.6, 7)
    for i in range(12):
        a = i * math.pi / 6
        r0 = 10.0 if i % 3 else 8.5
        P.line(L, 48 + math.cos(a) * r0, 70 + math.sin(a) * r0, 48 + math.cos(a) * 12, 70 + math.sin(a) * 12, 1)
    P.disc(L, 48, 70, 2.0, 7)
    return L


def titan_hand(length):
    L = Layer(32, 32)
    s = Layer(32, 32)
    P.poly(s, [(15, 16), (16.5, 16 - length), (18, 16)], col=1)
    P.poly(s, [(15, 15), (16.5, 18), (18, 15)], col=1)
    P.disc(s, 16.5, 16 - length + 2, 1.6, 10)
    L.paste(s)
    return L


def gear_frame():
    L = Layer(32, 32)
    pts = []
    n = 10
    for i in range(n * 4):
        a = i * math.pi * 2 / (n * 4)
        r = 14.8 if (i % 4) in (0, 1) else 11.6
        pts.append((16 + math.cos(a) * r, 16 + math.sin(a) * r))
    g = Layer(32, 32)
    P.poly(g, pts, col=4)
    P.ellipse(g, 16, 16, 12, 12, HULL, amb=0.3)
    put(L, g)
    P.ring(L, 16, 16, 7, 8.2, 2)
    for i in range(5):
        a = i * math.pi * 2 / 5
        P.disc(L, 16 + math.cos(a) * 9.6, 16 + math.sin(a) * 9.6, 1.4, 2)
    P.disc(L, 16, 16, 4.2, 7)
    P.disc(L, 16, 16, 2.0, 10)
    return L


TITAN = {
    "name": "TICKTOCK TITAN",
    "pals": [mech_pal(("4e3014", "86561e", "c08c3c", "f2cc7c"), ("1c5e60", "3ea0a0", "92e2d2"),
                      ("5a2cc0", "a070ff", "e4d6ff"), ("6a5a50", "fff2d8")),
             mech_pal(("40384c", "665c78", "958aa8", "cec6dc"), ("8a4a1c", "c87a34", "f8b878"),
                      ("5a2cc0", "a070ff", "e4d6ff"), ("6a5a50", "b8aab0"))],
    "parts": [("body", [titan_body()], 0), ("hour", [titan_hand(10)], 0), ("minute", [titan_hand(14)], 0),
              ("gear", [gear_frame()], 1)],
}


# ================================================================== Boss 5: King Gloom
def king_body(damaged):
    L = Layer(112, 96)
    # side cannons
    for sy in (20, 70):
        c = Layer(112, 96)
        P.rect(c, 6, sy, 28, sy + 6, 12)
        P.line(c, 6, sy, 28, sy, 13)
        P.ellipse(c, 30, sy + 3, 7, 7, HULL, amb=0.3)
        put(L, c)
        P.disc(L, 30, sy + 3, 2.2, 10)
    # armour shell (crown)
    a = Layer(112, 96)
    pts = [(28, 48), (34, 22), (44, 14), (50, 2), (58, 12), (66, 0), (74, 12), (82, 4), (86, 18),
           (100, 26), (108, 48), (100, 70), (86, 78), (82, 92), (74, 84), (66, 96), (58, 84), (50, 94), (44, 82),
           (34, 74)]
    P.poly(a, pts, ramp=HULL, grad=(1.0, 0.1), dither=True)
    put(L, a)
    # gold trim along crown edge
    for i in range(len(pts)):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % len(pts)]
        cx, cy = 66, 48
        ix0, iy0 = x0 + (cx - x0) * 0.12, y0 + (cy - y0) * 0.12
        ix1, iy1 = x1 + (cx - x1) * 0.12, y1 + (cy - y1) * 0.12
        P.line(L, ix0, iy0, ix1, iy1, 7)
    for (x, y) in ((50, 8), (66, 6), (82, 10), (50, 88), (66, 90)):
        P.disc(L, x, y + 2, 1.6, 10)
    # socket
    s = Layer(112, 96)
    P.ellipse(s, 66, 48, 27, 25, [1, 2, 2], amb=0.2)
    L.paste(s)
    # great eye
    e = Layer(112, 96)

    def eye(x, y, v, nx, ny):
        rr = math.sqrt(nx * nx + ny * ny)
        if rr > 0.92:
            return 9
        return P.pick(v * 1.15, [12, 13, W14, W14], x, y)
    P.ellipse_cb(e, 66, 48, 23, 21, eye, amb=0.35)
    put(L, e)
    # veins
    for i in range(6):
        ang = i * math.pi / 3 + 0.3
        P.line(L, 66 + math.cos(ang) * 14, 48 + math.sin(ang) * 13, 66 + math.cos(ang) * 21, 48 + math.sin(ang) * 19, 9)
    if damaged:
        import random
        rnd = random.Random(5)
        for i in range(10):
            x = rnd.randint(32, 104)
            y = rnd.randint(10, 86)
            if L.get(x, y) in HULL:
                for j in range(6):
                    L.set(x + j // 2, y + j, 1)
                    L.set(x + j // 2 + 1, y + j, 9)
        for (cx, cy) in ((40, 30), (92, 64), (46, 70)):
            P.disc(L, cx, cy, 3.4, 10)
            P.disc(L, cx, cy, 1.8, 11)
    return L


def king_cracks():
    """Overlay holding only the pixels that change when King Gloom's armour breaks."""
    a, b = king_body(False), king_body(True)
    L = Layer(a.w, a.h)
    for y in range(a.h):
        for x in range(a.w):
            if b.px[y][x] != a.px[y][x]:
                L.px[y][x] = b.px[y][x]
    return L


def king_iris():
    L = Layer(32, 32)
    P.ellipse(L, 16, 16, 10.5, 10.5, GLOW, amb=0.3)
    P.ring(L, 16, 16, 7, 8, 9)
    P.ellipse(L, 16, 16, 5.0, 7.0, None, flat=PUP)
    P.rect(L, 11, 10, 13, 12, W14)
    P.outline_sel(L, 1)
    return L


def king_pod(f):
    L = Layer(16, 16)
    s = Layer(16, 16)
    P.ellipse(s, 8, 8, 6.4, 6.4, HULL, amb=0.3)
    put(L, s)
    P.disc(L, 8, 8, 2.8, 10 if f else 9)
    P.disc(L, 7.5, 7.5, 1.2, 11)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        L.set(int(8 + math.cos(a) * 5), int(8 + math.sin(a) * 5), 7)
    return L


KING = {
    "name": "KING GLOOM",
    "pals": [mech_pal(("1a1024", "30203e", "4e3860", "7a5e8e"), ("8a6a18", "d0aa30", "ffe68a"),
                      ("c01020", "ff3c30", "ffd6a0"), ("a0909c", "f4ecf0")),
             mech_pal(("2c1838", "4c2c5c", "764a88", "a87cba"), ("8a6a18", "d0aa30", "ffe68a"),
                      ("c0106a", "ff3ca0", "ffd0ec"), ("5a4c64", "a898b0"))],
    "parts": [("body", [king_body(False)], 0), ("iris", [king_iris()], 0),
              ("pod", [king_pod(0), king_pod(1)], 1), ("cracks", [king_cracks()], 0)],
}

BOSSES = [BUZZSAW, THUNDER, CLAMP, TITAN, KING]


# ================================================================== mid-bosses (64x48, one palette)
def thresher(f):
    L = Layer(64, 48)
    b = Layer(64, 48)
    P.poly(b, [(46, 14), (60, 8), (63, 22), (60, 36), (46, 32)], ramp=ACC, grad=(1, 0.1))
    P.ellipse(b, 36, 24, 22, 15, HULL, amb=0.25, dither=True)
    put(L, b)
    c = Layer(64, 48)
    P.ellipse(c, 30, 18, 8, 6, GLOW, amb=0.35)
    put(L, c)
    L.set(27, 15, W14)
    L.set(26, 16, W14)
    # reel
    r = Layer(64, 48)
    P.ellipse(r, 11, 28, 9, 13, [12, 13], amb=0.3)
    put(L, r)
    for i in range(5):
        y = 16 + ((i * 5 + f * 2) % 24)
        P.line(L, 3, y, 19, y, 7)
    P.line(L, 11, 14, 11, 42, 1)
    for x in range(26, 52, 6):
        L.set(x, 34, 5)
    return L


def lantern(f):
    L = Layer(64, 48)
    b = Layer(64, 48)
    P.rect(b, 28, 0, 35, 5, 12)
    P.poly(b, [(18, 8), (46, 8), (40, 4), (24, 4)], ramp=HULL, grad=(1, 0.4))
    P.poly(b, [(18, 40), (46, 40), (40, 46), (24, 46)], ramp=HULL, grad=(0.6, 0.0))
    put(L, b)
    o = Layer(64, 48)
    P.ellipse(o, 32, 24, 13, 15, GLOW, amb=0.3 + 0.15 * f)
    put(L, o)
    for x in (20, 32, 44):
        P.line(L, x, 8, x, 40, 1)
        P.line(L, x + 1, 8, x + 1, 40, 13)
    P.disc(L, 32, 24, 4 + f, W14)
    for sx in (10, 54):
        P.line(L, sx, 20, sx, 30, 7)
        P.disc(L, sx, 18, 3, 8)
        P.line(L, sx, 24, 18 if sx < 32 else 46, 24, 12)
    return L


def ray(f):
    L = Layer(64, 48)
    b = Layer(64, 48)
    up = 0 if f == 0 else 4
    P.poly(b, [(4, 24), (22, 14), (36, up + 2), (44, 16), (52, 22), (63, 24), (52, 26), (44, 32), (36, 46 - up), (22, 34)],
           ramp=HULL, grad=(1, 0.15))
    put(L, b)
    for (x, y) in ((24, 20), (30, 16), (30, 30), (36, 24)):
        P.disc(L, x, y, 1.6, 10)
    e = Layer(64, 48)
    P.ellipse(e, 12, 24, 4, 3, GLOW, amb=0.4)
    put(L, e)
    P.line(L, 44, 24, 63, 24, 6)
    return L


def cog_knight(f):
    L = Layer(64, 48)
    b = Layer(64, 48)
    P.ellipse(b, 40, 24, 16, 19, HULL, amb=0.25)
    P.rect(b, 34, 4, 46, 8, 6)
    put(L, b)
    P.rect(L, 28, 20, 46, 23, 1)
    P.rect(L, 30, 21, 44, 22, 10 if f else 9)
    # gear shield
    g = gear_frame()
    sh = Layer(64, 48)
    for y in range(32):
        for x in range(32):
            c = g.px[y][x]
            if c:
                sh.set(4 + x, 8 + y, {4: 7, 2: 6, 3: 7, 5: 8, 7: 6, 10: 10}.get(c, c))
    L.paste(sh)
    return L


def sentinel(f):
    L = Layer(64, 48)
    for (y0, y1) in ((2, 10), (38, 46)):
        c = Layer(64, 48)
        P.rect(c, 2, y0 + 2, 20, y1 - 2, 12)
        P.ellipse(c, 22, (y0 + y1) / 2, 6, 5, HULL, amb=0.3)
        put(L, c)
    b = Layer(64, 48)
    P.ellipse(b, 38, 24, 22, 20, HULL, amb=0.22, dither=True)
    put(L, b)
    e = Layer(64, 48)
    P.ellipse(e, 32, 24, 10, 9, [12, 13, W14], amb=0.4)
    put(L, e)
    P.disc(L, 30 - f, 24, 4.5, 9)
    P.disc(L, 30 - f, 24, 2.4, PUP)
    L.set(28, 21, W14)
    for i in range(5):
        a = -1.1 + i * 0.55
        L.set(int(38 + math.cos(a) * 18), int(24 + math.sin(a) * 16), 7)
    return L


MIDBOSSES = [
    ("THRESHER", [thresher(0), thresher(1)],
     mech_pal(("3a3a52", "585a7a", "8486a8", "b8bad6"), ("c06024", "f09040", "ffcc8a"), ("2a8a4a", "5ad87a", "d0ffd8"))),
    ("STORM LANTERN", [lantern(0), lantern(1)],
     mech_pal(("2a2440", "463e66", "706894", "a49cc4"), ("b08418", "e8c03c", "fff0a0"), ("3a5ad8", "7ab0ff", "e8f6ff"))),
    ("TORPEDO RAY", [ray(0), ray(1)],
     mech_pal(("1a3a56", "2a5e80", "4a8cb0", "8cc6e0"), ("c05820", "f08a30", "ffc282"), ("c0204a", "ff5a80", "ffd0dc"))),
    ("COG KNIGHT", [cog_knight(0), cog_knight(1)],
     mech_pal(("3a3448", "5e5672", "8e86a4", "c6c0d8"), ("8a5a1c", "c88e34", "f8d07a"), ("1c8a8a", "40d8c8", "d0fff4"))),
    ("GLOOM SENTINEL", [sentinel(0), sentinel(1)],
     mech_pal(("1c1226", "34223e", "523a62", "7e6290"), ("7e1630", "c02848", "ff6a80"), ("c01020", "ff3c30", "ffd6a0"))),
]
