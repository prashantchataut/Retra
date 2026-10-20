"""Original sprite art for Sky Sprout: Mochi's cloud-copter, shots, bullets, items,
effects and the Gloom Fleet's common enemies. Everything is drawn procedurally."""
import math
from pix import Layer, pal16
import paint as P
import font as F

# ------------------------------------------------------------------ palettes
PAL_PLAYER = pal16([
    "000000", "5a2a4e", "e8849f", "f9b3c9", "ffd6e2", "fff6ec", "ff7b9c", "35203f",
    "ffffff", "3d8a58", "6fcf7a", "b9f2a2", "b7c2ee", "e6ebff", "7466a6", "ffe27a",
])
PAL_SHOTS = pal16([
    "000000", "2f7a3e", "66d27a", "c8f8b0", "ffffff",      # pea 1-4
    "b8366a", "ff6fa8", "ffc2dc",                          # petal 5-7
    "1f6ab8", "4fc8ff", "b8f0ff",                          # laser 8-10
    "9a5a1a", "f0a83a", "ffe08a", "fff8e0",                # seed 11-14
    "ffffff",
])
PAL_EBUL = pal16([
    "000000", "2a0c2c",
    "d81e6a", "ff5a96", "ffc0da",      # magenta 2-4
    "1e4ed8", "4aa2ff", "bce6ff",      # blue 5-7
    "7a28d8", "b466ff", "e6ccff",      # violet 8-10
    "d85a0c", "ffa030", "ffe680",      # amber 11-13
    "ffffff", "120c30",
])
PAL_FX = pal16([
    "000000", "3a2a44", "6c5a7c", "a898bc", "d23a2a", "ff7a1e", "ffb43a", "ffe65e",
    "ffffff", "ff8fbc", "ffd2e4", "9af0ff", "8fe37a", "ffd84d", "c8b8ff", "8a1a3a",
])
PAL_ITEM = pal16([
    "000000", "34183e", "ffffff", "fff2d8", "e8365a", "ff9aa8", "ff62ac", "ffc4e0",
    "24aef0", "a6ecff", "eea21c", "ffe27a", "44c25c", "b4f2a4", "5a6af0", "c4ccff",
])
PAL_FLASH = pal16(["000000", "ffd8ec"] + ["ffffff"] * 14)
PAL_HUDSPR = pal16([
    "000000", "2a1236", "ffffff", "ffe27a", "ffb43a", "ff7ba6", "c0406e", "9af0ff",
    "6fcf7a", "c8b8ff", "fff2d8", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff",
])

# mech palette layout shared by enemies/bosses:
#  1 outline | 2-5 hull ramp | 6-8 accent ramp | 9-11 glow ramp | 12-13 metal | 14 white | 15 pupil
HULL = [2, 3, 4, 5]
ACC = [6, 7, 8]
GLOW = [9, 10, 11]
METAL = [12, 13]


def mech_pal(hull, acc, glow, metal=("6a6480", "b4aec8"), outline="221428", pupil="1a0c22"):
    return pal16([("000000"), outline] + list(hull) + list(acc) + list(glow) + list(metal) + ["ffffff", pupil])


STAGE_ENEMY_PALS = [
    # stage 1 Blossom Skies: plum hulls, coral fins, magenta eyes
    (mech_pal(("43365e", "665490", "937fba", "c6b6e2"), ("b6404e", "ee7474", "ffb4a4"), ("a01e6a", "ff4a9a", "ffc4e2")),
     mech_pal(("4e2c46", "7e4468", "b06890", "e0a0c0"), ("c08a24", "f0c44a", "fff0a8"), ("a01e6a", "ff4a9a", "ffc4e2"))),
    # stage 2 Storm Front: slate hulls, yellow fins, cyan eyes
    (mech_pal(("2c3656", "48587e", "7286aa", "a8badc"), ("bc8c1c", "f0c840", "fff2a2"), ("1e7cc0", "50d0ff", "d4f8ff")),
     mech_pal(("3a2c56", "584a86", "8676b6", "bcaee0"), ("bc8c1c", "f0c840", "fff2a2"), ("1e7cc0", "50d0ff", "d4f8ff"))),
    # stage 3 Sunset Reef: teal hulls, orange fins, red eyes
    (mech_pal(("1c4650", "2c6e70", "4e9e96", "92d2c2"), ("bc561c", "f08a30", "ffc282"), ("b01e2e", "ff5050", "ffc4b4")),
     mech_pal(("4a2a2a", "7a4038", "b0644c", "e0a07a"), ("1c6e7a", "34aab4", "9ae6e0"), ("b01e2e", "ff5050", "ffc4b4"))),
    # stage 4 Clockwork Moon: brass hulls, teal fins, violet eyes
    (mech_pal(("583816", "885c26", "c08e40", "f0ca7c"), ("1c5e60", "3ea0a0", "92e2d2"), ("5e2ec0", "a472ff", "e2d4ff")),
     mech_pal(("3a3448", "5e5672", "8e86a4", "c6c0d8"), ("a0602a", "e09848", "ffd090"), ("5e2ec0", "a472ff", "e2d4ff"))),
    # stage 5 Gloom Fortress: black-violet hulls, crimson fins, hot eyes
    (mech_pal(("1c1226", "34223e", "523a62", "7e6290"), ("7e1630", "c02848", "ff6a80"), ("c01020", "ff3c30", "ffd6a0")),
     mech_pal(("281828", "463040", "6c4a5e", "9a7088"), ("8a6a1a", "d0aa30", "ffe68a"), ("c01020", "ff3c30", "ffd6a0"))),
]


# ------------------------------------------------------------------ player
def _mochi_body(L, cx, cy, face="open", look=(1, 0)):
    body = Layer(L.w, L.h)
    P.ellipse(body, cx, cy, 8.6, 7.2, [2, 3, 4, 4], amb=0.35)
    # soft cream highlight
    P.ellipse(body, cx - 3.2, cy - 3.6, 2.6, 1.5, None, flat=5)
    P.outline_sel(body, 1)
    L.paste(body)
    ex1, ex2 = int(cx) + 1 + look[0], int(cx) + 6 + look[0]
    ey = int(cy) - 3 + look[1]
    if face == "open":
        for ex in (ex1, ex2):
            P.rect(L, ex, ey, ex + 1, ey + 3, 7)
            L.set(ex, ey, 8)
    elif face == "happy":
        for ex in (ex1, ex2):
            L.set(ex - 1, ey + 2, 7)
            L.set(ex, ey + 1, 7)
            L.set(ex + 1, ey + 1, 7)
            L.set(ex + 2, ey + 2, 7)
    elif face == "hurt":
        for ex in (ex1, ex2):
            for i in range(3):
                L.set(ex - 1 + i, ey + i, 7)
                L.set(ex + 1 - i, ey + i, 7)
    elif face == "focus":
        for ex in (ex1, ex2):
            P.rect(L, ex, ey + 1, ex + 1, ey + 3, 7)
            L.set(ex, ey + 1, 8)
            L.set(ex - 1, ey, 1)
            L.set(ex + 1, ey, 1)
    # blush + mouth
    by = ey + 5
    for bx in (ex1 - 3, ex1 - 2, ex2 + 2, ex2 + 3):
        if L.get(bx, by) in (2, 3, 4):
            L.set(bx, by, 6)
    mx = (ex1 + ex2) // 2 + 1
    if face in ("open", "focus"):
        L.set(mx - 1, by, 1)
        L.set(mx, by + 1, 1)
        L.set(mx + 1, by, 1)
    elif face == "happy":
        P.rect(L, mx - 1, by, mx + 1, by + 1, 1)
        L.set(mx, by + 1, 6)
    else:
        L.set(mx, by, 1)
        L.set(mx, by + 1, 1)


def _cloud_pod(L, tilt=0):
    pod = Layer(L.w, L.h)
    t = tilt
    circles = [(6, 22 - t * 0.0, 4.6), (10.5, 24.5, 5.2), (16.5, 25, 5.4), (22, 24 - t, 5.0), (26.5, 22 - t * 1.4, 3.6)]
    P.blobs(pod, circles, [12, 13, 8, 8], amb=0.42, squash=0.85)
    # flat-ish top rim where Mochi sits
    P.outline_sel(pod, 14)
    L.paste(pod)
    # pea-pod blaster at the front
    gun = Layer(L.w, L.h)
    gy = 21 - t
    P.ellipse(gun, 27.5, gy, 3.6, 2.0, [9, 10, 11], amb=0.4)
    P.outline_sel(gun, 9)
    L.paste(gun)
    L.set(31, gy, 3)
    L.set(26, gy, 11)
    L.set(28, gy, 11)


def player_frame(face="open", tilt=0):
    L = Layer(32, 32)
    cy = 15 - tilt
    look = (1, -1) if tilt > 0 else ((1, 1) if tilt < 0 else (1, 0))
    _mochi_body(L, 15.0, cy, face, look)
    # leaf stem on the head (propeller hub)
    L.set(14, int(cy) - 8, 9)
    L.set(14, int(cy) - 9, 9)
    _cloud_pod(L, tilt)
    # little wisps behind the pod
    return L


def player_frames():
    return [player_frame("open", 0), player_frame("open", 1), player_frame("open", -1),
            player_frame("hurt", 0), player_frame("happy", 0)]


def propeller_frames():
    out = []
    for rx in (6.5, 3.6, 1.2):
        L = Layer(32, 8)
        cx = 14.5
        P.ellipse(L, cx - rx - 0.3, 4.0, rx + 0.4, 1.6, [9, 10, 11], amb=0.45)
        P.ellipse(L, cx + rx + 0.3, 4.0, rx + 0.4, 1.6, [9, 10, 11], amb=0.45)
        P.outline_sel(L, 9)
        L.set(14, 4, 9)
        L.set(14, 5, 9)
        L.set(14, 6, 9)
        L.set(14, 3, 11)
        out.append(L)
    return out


def sproutling_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    P.ellipse(b, 8, 10.5, 4.6, 4.0, [9, 10, 11, 11], amb=0.35)
    P.outline_sel(b, 1)
    L.paste(b)
    lv = Layer(16, 16)
    a = 0.55 if f == 0 else 0.25
    for s in (-1, 1):
        cx = 8 + s * 3.4
        cy = 4.2 - (a * 2 if s > 0 else 0) + (1 if f else 0)
        P.ellipse(lv, cx, cy, 2.8, 1.5, [9, 10, 11], amb=0.5)
    P.outline_sel(lv, 9)
    L.paste(lv)
    L.set(8, 6, 9)
    L.set(8, 5, 9)
    L.set(9, 10, 7)
    L.set(9, 9, 7)
    L.set(11, 10, 7)
    L.set(11, 9, 7)
    L.set(7, 12, 6)
    L.set(12, 12, 6)
    return L


def hitbox_frame():
    L = Layer(8, 8)
    P.disc(L, 4, 4, 2.2, 8)
    L.set(4, 4, 6)
    L.set(3, 4, 6)
    L.set(4, 3, 6)
    L.set(3, 3, 6)
    P.outline_sel(L, 1)
    return L


def shield_frames():
    out = []
    for f in range(2):
        L = Layer(32, 32)
        P.ring(L, 16, 16, 12.5, 14.5, 15)
        P.ring(L, 16, 16, 13.2, 14.0, 9)
        for a in range(0, 360, 6):
            r = math.radians(a + f * 25)
            x = 16 + math.cos(r) * 13.6
            y = 16 + math.sin(r) * 13.6
            if (a // 6) % 5 == 0:
                L.set(int(x), int(y), 2)
        # glints top-left
        for i in range(4):
            r = math.radians(200 + i * 8 + f * 10)
            L.set(int(16 + math.cos(r) * 11), int(16 + math.sin(r) * 11), 2)
        out.append(L)
    return out


# ------------------------------------------------------------------ player shots
def pea_shot():
    L = Layer(8, 8)
    P.ellipse(L, 4.5, 4, 3.3, 2.3, [2, 3, 4], amb=0.5)
    L.set(5, 3, 4)
    P.outline_sel(L, 1)
    return L


def petal_shot(f):
    L = Layer(8, 8)
    pts = [(1, 4), (3, 2), (6, 2.5), (8, 4), (6, 5.5), (3, 6)] if f == 0 else \
          [(1, 4), (3, 1.5), (6, 2), (8, 4), (6, 6), (3, 6.5)]
    P.poly(L, pts, ramp=[5, 6, 7], grad=(0.9, 0.2))
    L.set(5, 4, 15)
    L.set(6, 4, 7)
    return L


def laser_seg(f):
    L = Layer(32, 8)
    for x in range(32):
        L.set(x, 2, 8)
        L.set(x, 5, 8)
        L.set(x, 3, 9 if (x + f * 2) % 8 < 6 else 10)
        L.set(x, 4, 10)
    for x in range(0, 32, 4):
        L.set(x + (f * 2) % 4, 3, 15)
    if f:
        for x in range(1, 32, 6):
            L.set(x, 1, 9)
            L.set(x + 3, 6, 9)
    return L


def seed_shots():
    base = Layer(8, 8)
    P.poly(base, [(0.5, 4), (3, 2), (6.5, 2.5), (7.8, 4), (6.5, 5.5), (3, 6)], col=12)
    P.ellipse(base, 5.2, 3.6, 1.8, 0.9, None, flat=13)
    base.set(6, 3, 14)
    P.outline_sel(base, 11)
    out = []
    for d in range(8):
        out.append(P.rotate(base, d * math.pi / 4, 4, 4))
    return out


# ------------------------------------------------------------------ enemy bullets
EB_COL = {"m": (2, 3, 4), "b": (5, 6, 7), "v": (8, 9, 10), "a": (11, 12, 13)}


def orb(col, r, size=8):
    L = Layer(size, size)
    d, m, li = EB_COL[col]
    c = size / 2.0
    P.disc(L, c, c, r, d)
    P.disc(L, c, c, r - 0.9, m)
    P.disc(L, c - 0.3, c - 0.3, max(0.8, r - 2.0), li)
    P.disc(L, c - 0.5, c - 0.5, max(0.6, r - 3.0), 14)
    P.outline_sel(L, 1)
    return L


def rice(col, ang):
    L = Layer(8, 8)
    d, m, li = EB_COL[col]
    for y in range(8):
        for x in range(8):
            dx, dy = x + 0.5 - 4, y + 0.5 - 4
            u = dx * math.cos(ang) + dy * math.sin(ang)
            v = -dx * math.sin(ang) + dy * math.cos(ang)
            e = (u / 3.4) ** 2 + (v / 1.7) ** 2
            if e <= 1.0:
                L.set(x, y, 14 if e < 0.18 else (li if e < 0.45 else (m if e < 0.75 else d)))
    P.outline_sel(L, 1)
    return L


def star_bullet(col, f):
    L = Layer(8, 8)
    d, m, li = EB_COL[col]
    rot = -math.pi / 2 + f * math.pi / 5
    pts = []
    for i in range(10):
        r = 3.7 if i % 2 == 0 else 1.7
        a = rot + i * math.pi / 5
        pts.append((4 + math.cos(a) * r, 4 + math.sin(a) * r))
    P.poly(L, pts, col=m)
    L.set(4, 4, 14)
    L.set(3, 4, li)
    L.set(4, 3, li)
    P.outline_sel(L, 1)
    return L


def petal_pickup(f):
    """Cancelled bullets turn into these little score blossoms (FX palette)."""
    L = Layer(8, 8)
    for i in range(5):
        a = i * 2 * math.pi / 5 + f * 0.6
        P.disc(L, 4 + math.cos(a) * 1.9, 4 + math.sin(a) * 1.9, 1.3, 9)
    P.disc(L, 4, 4, 1.0, 13)
    L.set(4, 4, 8)
    P.outline_sel(L, 15)
    return L


def ebullet_frames():
    out = {}
    for c in "mbva":
        out["s" + c] = orb(c, 2.6)
        out["m" + c] = orb(c, 3.5)
        out["B" + c] = orb(c, 6.4, 16)
    for c in "mbv":
        for d in range(8):
            out["r%s%d" % (c, d)] = rice(c, d * math.pi / 8)
    for f in range(2):
        out["*a%d" % f] = star_bullet("a", f)
        out["*m%d" % f] = star_bullet("m", f)
    return out


# ------------------------------------------------------------------ items
ITEM_KINDS = ["P", "S", "L", "H", "O", "B", "D", "1"]  # D = shield, 1 = 1UP
ITEM_COL = {"P": (4, 5), "S": (6, 7), "L": (8, 9), "H": (10, 11), "O": (12, 13), "B": (4, 11), "D": (14, 15), "1": (6, 7)}


def item_frame(kind, f):
    L = Layer(16, 16)
    d, li = ITEM_COL[kind]
    body = Layer(16, 16)
    if kind == "D":
        P.disc(body, 8, 8, 6.4, 14)
        P.disc(body, 8, 8, 5.2, 15)
        P.disc(body, 7.5, 7.5, 3.8, 3)
        P.disc(body, 6.0, 6.0, 1.4, 2)
    elif kind == "1":
        P.ellipse(body, 8, 9, 6.6, 5.6, [6, 7, 3], amb=0.4)
        P.ellipse(body, 6, 6.5, 1.8, 1.0, None, flat=2)
    else:
        # seed-pod capsule: rounded body with a lighter belly
        for y in range(16):
            for x in range(16):
                dx, dy = (x + 0.5 - 8) / 6.6, (y + 0.5 - 8) / 5.8
                if abs(dx) ** 2.6 + abs(dy) ** 2.6 <= 1.0:
                    body.set(x, y, li if dy < -0.25 else d)
        P.ellipse(body, 5.5, 4.6, 2.2, 1.0, None, flat=2)
    P.outline_sel(body, 1)
    L.paste(body)
    if kind == "1":
        for ex in (8, 11):
            L.set(ex, 8, 1)
            L.set(ex, 9, 1)
        L.set(9, 11, 1)
        L.set(10, 11, 1)
        L.set(6, 3, 12)
        L.set(7, 2, 12)
        L.set(8, 2, 13)
        L.set(7, 3, 13)
    elif kind != "D":
        ch = {"O": "@"}.get(kind, kind)
        g = F.glyph(ch if ch != "@" else "O")
        gl = Layer(16, 16)
        for y in range(6):
            for x in range(5):
                if g[y][x] == "#":
                    gl.set(6 + x, 5 + y, 2)
        P.outline_sel(gl, 1)
        for y in range(16):
            for x in range(16):
                c = gl.px[y][x]
                if c == 2 or (c == 1 and L.get(x, y)):
                    L.set(x, y, c)
    # travelling glint
    if f:
        for i in range(3):
            x, y = 3 + i, 10 - i
            if L.get(x, y) not in (0, 1):
                L.set(x, y, 2)
    return L


# ------------------------------------------------------------------ effects
def explosion(size, f, seed=1):
    """6-frame explosion: flash -> fireball -> rolling smoke."""
    import random
    rnd = random.Random(seed * 31 + size)
    L = Layer(size, size)
    c = size / 2.0
    s = size / 16.0
    if f == 0:
        P.disc(L, c, c, 3.2 * s, 7)
        P.disc(L, c, c, 2.2 * s, 8)
        for i in range(8):
            a = i * math.pi / 4
            P.line(L, c + math.cos(a) * 3 * s, c + math.sin(a) * 3 * s,
                   c + math.cos(a) * 5.5 * s, c + math.sin(a) * 5.5 * s, 7 if i % 2 else 8)
        return L
    if f in (1, 2):
        R = (5.2 if f == 1 else 6.6) * s
        puffs = [(c, c, R)]
        for i in range(6):
            a = i * math.pi / 3 + rnd.random() * 0.5
            puffs.append((c + math.cos(a) * R * 0.55, c + math.sin(a) * R * 0.55, R * 0.55))
        P.blobs(L, puffs, [4, 5, 6, 7] if f == 1 else [15, 4, 5, 6], amb=0.3)
        P.disc(L, c - 0.6 * s, c - 0.6 * s, (2.6 if f == 1 else 2.0) * s, 7)
        P.disc(L, c - 0.8 * s, c - 0.8 * s, (1.4 if f == 1 else 0.9) * s, 8)
        return L
    # smoke stages
    R = (6.0 + (f - 3) * 0.6) * s
    puffs = []
    for i in range(7):
        a = i * 2 * math.pi / 7 + rnd.random() * 0.6
        rr = R * (0.45 + 0.1 * rnd.random())
        dist = R * (0.45 + 0.15 * (f - 3))
        puffs.append((c + math.cos(a) * dist, c + math.sin(a) * dist - (f - 3) * s, rr * (1.0 - 0.18 * (f - 3))))
    ramp = [1, 2, 3] if f < 5 else [2, 3, 3]
    if f == 3:
        ramp = [15, 4, 5, 6]
    P.blobs(L, puffs, ramp, amb=0.3, dither=(f == 5))
    if f == 3:
        P.disc(L, c, c, 1.6 * s, 6)
    return L


def spark_frames():
    out = []
    for f in range(3):
        L = Layer(8, 8)
        r = [1.5, 3.0, 3.6][f]
        cc = 8 if f < 2 else 7
        P.line(L, 4 - r, 4, 4 + r, 4, cc)
        P.line(L, 4, 4 - r, 4, 4 + r, cc)
        if f == 1:
            L.set(3, 3, 7)
            L.set(5, 5, 7)
            L.set(5, 3, 7)
            L.set(3, 5, 7)
        L.set(4, 4, 8)
        out.append(L)
    return out


def puff_frames():
    out = []
    for f in range(3):
        L = Layer(8, 8)
        r = [2.4, 2.0, 1.3][f]
        P.disc(L, 4, 4, r, 3 if f < 2 else 2)
        P.disc(L, 3.6, 3.6, max(0.5, r - 1.1), 8 if f == 0 else 3)
        out.append(L)
    return out


def bomb_petal(f):
    L = Layer(16, 16)
    a0 = f * math.pi / 5
    for i in range(5):
        a = a0 + i * 2 * math.pi / 5
        P.ellipse(L, 8 + math.cos(a) * 3.6, 8 + math.sin(a) * 3.6, 2.8, 2.8, [9, 10, 8], amb=0.4)
    P.disc(L, 8, 8, 1.8, 13)
    L.set(7, 7, 8)
    P.outline_sel(L, 15)
    return L


def popup(text):
    L = Layer(16, 8)
    x = 0
    for ch in text:
        g = F.glyph(ch)
        cols = [i for i in range(5) if any(g[r][i] == "#" for r in range(6))]
        if not cols:
            x += 3
            continue
        for y in range(6):
            for xx in cols:
                if g[y][xx] == "#":
                    L.set(1 + x + xx - cols[0], 1 + y, 3 if y < 3 else 4)
        x += len(cols) + 1
    P.outline_sel(L, 1, diag=True)
    return L


def warning_chevron(f):
    L = Layer(16, 16)
    P.poly(L, [(2, 2), (8, 2), (14, 8), (8, 14), (2, 14), (8, 8)], col=5 if f else 6)
    P.outline_sel(L, 1)
    return L


# ------------------------------------------------------------------ common enemies (mech palette)
def _fin(L, pts, ramp=ACC):
    P.poly(L, pts, ramp=ramp, grad=(0.9, 0.1))


def gnat_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    _fin(b, [(10, 7), (15, 4.5), (14.5, 9.5), (10, 11)])
    P.ellipse(b, 8, 9, 5.6, 5.2, HULL, amb=0.25)
    P.outline_sel(b, 1)
    L.paste(b)
    # big glowing eye
    e = Layer(16, 16)
    P.ellipse(e, 6.2, 9.0, 2.6, 2.6, GLOW, amb=0.35)
    P.outline_sel(e, 1)
    L.paste(e)
    L.set(5, 9, 15)
    L.set(6, 9, 15)
    L.set(5, 8, 14)
    # rotor
    L.set(8, 3, 12)
    L.set(8, 2, 12)
    if f == 0:
        P.line(L, 2, 1, 14, 1, 13)
        L.set(2, 1, 12)
        L.set(14, 1, 12)
    else:
        P.line(L, 5, 1, 11, 1, 13)
        L.set(4, 0, 12)
        L.set(12, 2, 12)
    return L


def swooper_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    _fin(b, [(7, 9), (12, 9), (15, 14.5), (11, 14.5)])
    P.poly(b, [(0.5, 8.5), (5, 5), (11, 3.5), (15.5, 5), (15.5, 11), (11, 12), (5, 11.5)], ramp=HULL, grad=(1.0, 0.1))
    _fin(b, [(9, 4.5), (11, 1), (14, 1), (13.5, 5)])
    P.outline_sel(b, 1)
    L.paste(b)
    P.ellipse(L, 6, 7.4, 2.4, 1.5, GLOW, amb=0.4)
    L.set(5, 7, 14)
    P.line(L, 2, 9, 9, 9, 2)
    if f:
        L.set(15, 7, 10)
        L.set(15, 8, 11)
        L.set(15, 9, 10)
    else:
        L.set(15, 8, 9)
    return L


def turret_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    P.rect(b, 0, 9, 4, 10, 12)
    b.set(0, 9, 13)
    P.ellipse(b, 9.5, 7.5, 4.0, 3.8, GLOW, amb=0.35)
    P.ellipse(b, 8.5, 11, 7.2, 3.2, HULL, amb=0.3)
    P.outline_sel(b, 1)
    L.paste(b)
    L.set(8, 6, 14)
    L.set(9, 5, 14)
    P.line(L, 3, 12, 14, 12, 2)
    for i, x in enumerate((4, 8, 12)):
        L.set(x, 11, 8 if (i + f) % 2 == 0 else 6)
    return L


def mine_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        P.line(b, 8 + math.cos(a) * 4, 8 + math.sin(a) * 4, 8 + math.cos(a) * 7.2, 8 + math.sin(a) * 7.2, 13)
    P.ellipse(b, 8, 8, 5.0, 5.0, HULL, amb=0.25)
    P.outline_sel(b, 1)
    L.paste(b)
    P.disc(L, 8, 8, 1.8, 10 if f else 6)
    L.set(7, 7, 11 if f else 7)
    return L


def spinner_frame(f):
    L = Layer(16, 16)
    b = Layer(16, 16)
    rot = f * math.pi / 12
    for i in range(6):
        a = rot + i * math.pi / 3
        P.disc(b, 8 + math.cos(a) * 6, 8 + math.sin(a) * 6, 1.7, 7)
    P.ellipse(b, 8, 8, 6.0, 6.0, ACC, amb=0.3)
    P.ellipse(b, 8, 8, 3.6, 3.6, HULL, amb=0.3)
    P.outline_sel(b, 1)
    L.paste(b)
    P.disc(L, 8, 8, 2.0, 10)
    L.set(7, 7, 11)
    L.set(8, 8, 9)
    return L


def diver_frame(f):
    L = Layer(16, 16)
    w = Layer(16, 16)
    if f == 0:
        P.ellipse(w, 9, 3.5, 3.6, 2.4, METAL + [14], amb=0.5)
    else:
        P.ellipse(w, 10, 5.0, 3.6, 1.6, METAL + [14], amb=0.5)
    P.outline_sel(w, 1)
    L.paste(w)
    b = Layer(16, 16)
    P.ellipse(b, 8.5, 9.5, 6.4, 4.0, HULL, amb=0.3)
    P.poly(b, [(14, 8.5), (16, 10), (14, 11)], col=12)
    P.outline_sel(b, 1)
    L.paste(b)
    for x in (8, 11):
        for y in range(6, 14):
            if L.get(x, y) in HULL:
                L.set(x, y, 7)
    P.ellipse(L, 4.4, 9.0, 1.8, 1.8, GLOW, amb=0.4)
    L.set(4, 8, 14)
    return L


def carrier_frame(f):
    L = Layer(32, 32)
    b = Layer(32, 32)
    P.ellipse(b, 16, 11, 13.0, 8.0, ACC, amb=0.28)
    for x in (10, 16, 22):
        for y in range(2, 20):
            if b.get(x, y):
                b.set(x, y, 6)
    P.poly(b, [(27, 8), (31.5, 4), (31.5, 18), (27, 14)], ramp=HULL, grad=(1, 0.2))
    P.outline_sel(b, 1)
    L.paste(b)
    P.ellipse(L, 11, 6.5, 4.0, 1.8, None, flat=8)
    # gondola + seed crate
    g = Layer(32, 32)
    P.line(g, 11, 18, 11, 22, 12)
    P.line(g, 21, 18, 21, 22, 12)
    P.bevel(g, 9, 22, 23, 28, 2, 3, 4)
    P.outline_sel(g, 1)
    L.paste(g)
    P.ellipse(L, 16, 25, 3.2, 2.2, GLOW, amb=0.5)
    L.set(15, 24, 14)
    # rear propeller
    if f == 0:
        P.line(L, 30, 8, 30, 15, 13)
    else:
        L.set(30, 11, 13)
        L.set(30, 12, 13)
        L.set(29, 10, 12)
        L.set(29, 13, 12)
    return L


def heavy_frame(f):
    L = Layer(32, 32)
    b = Layer(32, 32)
    # cannons (top & bottom) pointing left
    P.rect(b, 1, 7, 10, 9, 12)
    P.rect(b, 1, 22, 10, 24, 12)
    P.line(b, 1, 7, 10, 7, 13)
    P.line(b, 1, 22, 10, 22, 13)
    _fin(b, [(18, 4), (26, 0.5), (30, 2), (26, 8)])
    _fin(b, [(18, 27), (26, 31.5), (30, 30), (26, 24)])
    P.poly(b, [(4, 16), (9, 9), (20, 5), (29, 8), (31.5, 13), (31.5, 19), (29, 24), (20, 27), (9, 23)],
           ramp=HULL, grad=(1.0, 0.05))
    P.outline_sel(b, 1)
    L.paste(b)
    # armour plate seams
    P.line(L, 14, 8, 12, 24, 2)
    P.line(L, 23, 7, 23, 25, 2)
    P.line(L, 15, 8, 13, 23, 5)
    # big eye
    e = Layer(32, 32)
    P.ellipse(e, 10, 16, 4.6, 4.2, GLOW, amb=0.3)
    P.outline_sel(e, 1)
    L.paste(e)
    P.rect(L, 8, 15, 9, 17, 15)
    L.set(9, 14, 14)
    L.set(8, 13, 14)
    for i, y in enumerate((11, 16, 21)):
        L.set(27, y, 8 if (i + f) % 2 else 6)
    if f:
        L.set(31, 15, 10)
        L.set(31, 16, 11)
        L.set(31, 17, 10)
    return L


ENEMY_ART = [
    # name, size, frames
    ("GNAT", 16, [gnat_frame(0), gnat_frame(1)]),
    ("SWOOP", 16, [swooper_frame(0), swooper_frame(1)]),
    ("TURRET", 16, [turret_frame(0), turret_frame(1)]),
    ("MINE", 16, [mine_frame(0), mine_frame(1)]),
    ("SPIN", 16, [spinner_frame(i) for i in range(4)]),
    ("DIVER", 16, [diver_frame(0), diver_frame(1)]),
    ("CARRIER", 32, [carrier_frame(0), carrier_frame(1)]),
    ("HEAVY", 32, [heavy_frame(0), heavy_frame(1)]),
]
