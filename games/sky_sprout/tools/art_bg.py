"""Original background art for Sky Sprout: five parallax stages, sky gradients,
UI tiles, banner phrases and the title logo. Procedural; deterministic."""
import math
import random
from pix import Layer, pal16, hexrgb
import paint as P
import font as F


class WrapLayer(Layer):
    """Layer that wraps horizontally (seamless scrolling)."""

    def get(self, x, y):
        if 0 <= y < self.h:
            return self.px[y][x % self.w]
        return 0

    def set(self, x, y, c):
        if 0 <= y < self.h:
            self.px[y][x % self.w] = c


def grad_table(stops, n=162):
    out = []
    for y in range(n):
        for i in range(len(stops) - 1):
            y0, c0 = stops[i]
            y1, c1 = stops[i + 1]
            if y0 <= y <= y1 or i == len(stops) - 2:
                t = 0 if y1 == y0 else max(0.0, min(1.0, (y - y0) / (y1 - y0)))
                a, b = hexrgb(c0), hexrgb(c1)
                out.append(tuple(int(round(a[j] + (b[j] - a[j]) * t)) for j in range(3)))
                break
    return out


def bands(spec, n=160):
    """spec: list of (y_start, factor) -> per-line 8.8 factors."""
    out = []
    for y in range(n):
        f = spec[0][1]
        for ys, fv in spec:
            if y >= ys:
                f = fv
        out.append(int(round(f * 256)))
    return out


def perspective(horizon, f0, f1, top=0.0, n=160):
    out = []
    for y in range(n):
        if y < horizon:
            out.append(int(round(top * 256)))
        else:
            t = (y - horizon) / max(1, (n - 1 - horizon))
            out.append(int(round((f0 + (f1 - f0) * t * t * 0.6 + (f1 - f0) * t * 0.4) * 256)))
    return out


def cloud_row(L, rnd, x0, x1, y, rmin, rmax, ramp, step=(10, 18), squash=0.8, dy=3, dither=False, amb=0.4):
    circles = []
    x = x0
    while x < x1:
        r = rnd.uniform(rmin, rmax)
        circles.append((x, y + rnd.uniform(-dy, dy) - r * 0.25, r))
        x += rnd.randint(*step)
    # wrap-aware: duplicate circles shifted by width
    allc = circles + [(cx - L.w, cy, r) for cx, cy, r in circles] + [(cx + L.w, cy, r) for cx, cy, r in circles]
    P.blobs(L, allc, ramp, amb=amb, squash=squash, dither=dither)
    return circles


def fill_below(L, ytop_fn, col):
    for x in range(L.w):
        yt = int(ytop_fn(x))
        for y in range(max(0, yt), L.h):
            L.set(x, y, col)


# ================================================================== Stage 1: Blossom Skies
def stage1():
    rnd = random.Random(101)
    far = WrapLayer(256, 160)
    mid = WrapLayer(512, 160)
    near = WrapLayer(256, 160)
    farpal = pal16(["000000", "c9c2ee", "b0a6e0", "9a8cd2", "e8e4fa", "ffffff", "ffc4dc", "ff9cc4",
                    "f07aa8", "8a7cc4", "7a6cb4", "f4f0ff", "dcd6f6", "6f62a8", "ffe2ee", "a594d8"])
    # high wispy clouds (rows 8-50)
    for i in range(6):
        cx = rnd.randint(0, 255)
        cy = rnd.randint(14, 44)
        w = rnd.randint(22, 46)
        for x in range(-w, w):
            yy = cy + int(1.5 * math.sin((cx + x) * 0.15))
            far.set(cx + x, yy, 4 if abs(x) < w - 6 else 12)
            if abs(x) < w - 12:
                far.set(cx + x, yy + 1, 12)
    # floating islands with blossom trees (rows 66-118)
    for k in range(3):
        cx = 30 + k * 86 + rnd.randint(-8, 8)
        cy = 92 + rnd.randint(-10, 8)
        w = rnd.randint(26, 36)
        isl = WrapLayer(256, 160)
        pts = [(cx - w, cy), (cx - w + 6, cy - 4), (cx + w - 6, cy - 4), (cx + w, cy), (cx + w * 0.5, cy + 8),
               (cx + 6, cy + 18), (cx - 4, cy + 22), (cx - w * 0.5, cy + 9)]
        P.poly(isl, pts, ramp=[13, 10, 9, 3], grad=(0.9, 0.0))
        for t in range(rnd.randint(2, 3)):
            tx = cx - w + 10 + t * (w - 4)
            P.line(isl, tx, cy - 4, tx, cy - 10, 10)
            P.blobs(isl, [(tx - 4, cy - 14, 5), (tx + 3, cy - 15, 5.5), (tx, cy - 19, 5)], [8, 7, 6, 14], amb=0.4)
        P.outline_sel(isl, 13)
        far.paste(isl)
    # distant cloud bank at the island base
    cloud_row(far, rnd, 0, 256, 118, 8, 14, [15, 1, 4, 11], step=(9, 15), squash=0.7)
    for x in range(256):
        for y in range(126, 160):
            if far.get(x, y) == 0:
                far.set(x, y, 4 if y < 140 else 11)

    midpal = pal16(["000000", "8a90d0", "a8b2e6", "c8d2f6", "e8eeff", "ffffff", "ffd8e8", "f0b6d0",
                    "7a7ec0", "6a6aac", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # big puffy clouds: hanging from the top and drifting low
    for k in range(4):
        cx = 40 + k * 128 + rnd.randint(-20, 20)
        circ = [(cx + rnd.randint(-36, 36), rnd.randint(-6, 10), rnd.uniform(10, 18)) for _ in range(7)]
        c = WrapLayer(512, 160)
        P.blobs(c, circ + [(x - 512, y, r) for x, y, r in circ] + [(x + 512, y, r) for x, y, r in circ],
                [1, 2, 3, 4, 5], amb=0.35)
        P.outline_sel(c, 8)
        mid.paste(c)
    for k in range(3):
        cx = 100 + k * 170 + rnd.randint(-20, 20)
        cy = 104 + rnd.randint(-8, 8)
        circ = [(cx + i * 9 - 27 + rnd.randint(-3, 3), cy + rnd.randint(-3, 2) - (3 - abs(i - 3)) * 2,
                 rnd.uniform(6, 9)) for i in range(7)]
        c = WrapLayer(512, 160)
        P.blobs(c, circ + [(x - 512, y, r) for x, y, r in circ] + [(x + 512, y, r) for x, y, r in circ],
                [7, 6, 4, 5], amb=0.4, squash=0.7)
        P.outline_sel(c, 1)
        mid.paste(c)

    nearpal = pal16(["000000", "b6a6e6", "d0c6f4", "eae6ff", "ffffff", "ffd6e8", "f6b8d4", "9888d4",
                     "fff6fb", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # rolling cloud sea (perspective rows 124..159)
    for row, (y, r0, r1) in enumerate(((140, 6, 9), (152, 9, 13))):
        cloud_row(near, rnd, 0, 256, y, r0, r1, [1, 2, 3, 3, 4] if row else [6, 5, 3, 8], step=(12, 18), squash=0.55, dy=1)
    for x in range(256):
        for y in range(150, 160):
            if near.get(x, y) == 0:
                near.set(x, y, 2)
    return dict(
        name="BLOSSOM SKIES", far=far, mid=mid, near=near, pals=[farpal, midpal, nearpal],
        grad=grad_table([(0, "6cbcf0"), (60, "9ed6f6"), (100, "cfeaf8"), (124, "ffe2ee"), (161, "ffd2e2")]),
        far_f=bands([(0, 0.06), (60, 0.16), (120, 0.22)]), near_f=bands([(0, 1.25)]),
        mid_fx=0.42, mid_fy=0.0, rain=False)


# ================================================================== Stage 2: Storm Front
def stage2():
    rnd = random.Random(202)
    far = WrapLayer(256, 160)
    mid = WrapLayer(256, 256)
    near = WrapLayer(256, 160)
    farpal = pal16(["000000", "2e2c4a", "3c3a5c", "4e4c70", "625e86", "7a729c", "8e84b0", "1e1c34",
                    "a89cc6", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # towering thunderheads
    for k in range(4):
        cx = 30 + k * 64 + rnd.randint(-10, 10)
        top = rnd.randint(28, 60)
        circ = []
        y = 150
        while y > top:
            w = 10 + (y - top) * 0.22
            for j in range(2):
                circ.append((cx + rnd.uniform(-w, w), y, rnd.uniform(9, 14)))
            y -= 9
        circ.append((cx, top, 14))
        c = WrapLayer(256, 160)
        P.blobs(c, circ + [(x - 256, yy, r) for x, yy, r in circ] + [(x + 256, yy, r) for x, yy, r in circ],
                [7, 1, 2, 3, 4, 5], amb=0.25, dither=True)
        far.paste(c)
    for x in range(256):
        for y in range(130, 160):
            if far.get(x, y) == 0:
                far.set(x, y, 1)
    # rain layer: sparse diagonal streaks on a 32x32 repeating cell grid
    midpal = pal16(["000000", "8c94c4", "b4bce0", "6a7098", "ffffff", "ffffff", "ffffff", "ffffff",
                    "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    cell = Layer(64, 64)
    r2 = random.Random(7)
    for i in range(9):
        x0, y0 = r2.randint(0, 63), r2.randint(0, 63)
        ln = r2.randint(5, 9)
        for s in range(ln):
            cell.set((x0 - s // 2) % 64, (y0 + s) % 64, 2 if s < 2 else (1 if s < ln - 2 else 3))
    for y in range(256):
        for x in range(256):
            c = cell.px[y % 64][x % 64]
            if c:
                mid.set(x, y, c)
    nearpal = pal16(["000000", "1a1830", "282644", "363458", "48466e", "5c5a84", "706c98", "13111f",
                     "8a86b0", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    for row, (y, r0, r1) in enumerate(((130, 6, 9), (141, 8, 12), (152, 11, 16))):
        cloud_row(near, rnd, 0, 256, y, r0, r1, [7, 1, 2, 3, 4, 5], step=(10, 16), squash=0.6, dy=2, dither=True,
                  amb=0.25)
    for x in range(256):
        for y in range(148, 160):
            if near.get(x, y) == 0:
                near.set(x, y, 2)
    return dict(
        name="STORM FRONT", far=far, mid=mid, near=near, pals=[farpal, midpal, nearpal],
        grad=grad_table([(0, "1c1e36"), (60, "2e3050"), (110, "48466c"), (140, "5e5680"), (161, "6a5c86")]),
        far_f=bands([(0, 0.12), (100, 0.18)]), near_f=bands([(0, 1.5)]),
        mid_fx=2.2, mid_fy=-5.0, rain=True)


# ================================================================== Stage 3: Sunset Reef
def stage3():
    rnd = random.Random(303)
    far = WrapLayer(256, 160)
    mid = WrapLayer(512, 160)
    near = WrapLayer(256, 160)
    farpal = pal16(["000000", "ffd27a", "ffb45e", "ff8e5a", "ff6e6e", "fff2b0", "6a2e6e", "8a3e7a",
                    "a85488", "4a2058", "c86a8e", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # setting sun with retro stripe cut-outs (rows 46..99)
    cx, cy, r = 168, 92, 40
    for y in range(int(cy - r), 100):
        for x in range(int(cx - r), int(cx + r) + 1):
            d = (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2
            if d <= r * r:
                gap = y > cy - 14 and ((y - int(cy - 14)) % 7) < (1 + (y - int(cy - 14)) // 9)
                if gap:
                    continue
                t = (y - (cy - r)) / (2 * r)
                far.set(x, y, 5 if t < 0.25 else (1 if t < 0.45 else (2 if t < 0.62 else 3)))
    midpal = pal16(["000000", "ff9a8a", "ffc0a0", "ffe2c4", "e0708a", "b85a8a", "fff4e4", "4a2058",
                    "6a2e6e", "8a3e7a", "a85488", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    for k in range(5):
        bx = 20 + k * 100 + rnd.randint(-10, 10)
        h = rnd.randint(12, 26)
        w = rnd.randint(14, 26)
        pts = [(bx, 100), (bx + 3, 100 - h), (bx + w * 0.4, 100 - h - 4), (bx + w, 100 - h + 6), (bx + w + 4, 100)]
        P.poly(mid, pts, ramp=[7, 8, 9, 10], grad=(0.0, 0.9))
        if k % 2 == 1:
            for y in range(100 - h // 2, 100):
                for x in range(int(bx + w * 0.3), int(bx + w * 0.7)):
                    mid.set(x, y, 0)
    for k in range(9):
        cx = rnd.randint(0, 511)
        cy = rnd.randint(10, 60)
        w = rnd.randint(30, 70)
        for x in range(-w, w):
            th = 2 if abs(x) < w * 0.6 else 1
            for t in range(th):
                mid.set(cx + x, cy + t, 3 if t == 0 and abs(x) < w * 0.4 else (2 if t == 0 else 1))
            if abs(x) < w * 0.5:
                mid.set(cx + x, cy + th, 4 if cy > 35 else 1)
    nearpal = pal16(["000000", "3a1e5a", "4e2a72", "64388a", "7c4a9e", "9a5aa8", "c26a9a", "ff9a6a",
                     "ffd08a", "fff4c8", "2c1648", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # ocean: banded rows, brighter toward the horizon, with glints
    for y in range(100, 160):
        t = (y - 100) / 60.0
        base = [6, 5, 4, 3, 2, 1, 10][min(6, int(t * 7.2))]
        for x in range(256):
            near.set(x, y, base)
    for i in range(170):
        y = rnd.randint(100, 159)
        x = rnd.randint(0, 255)
        t = (y - 100) / 60.0
        ln = 2 + int(t * 9)
        c = 8 if t < 0.35 else (7 if t < 0.7 else 5)
        for s in range(ln):
            near.set(x + s, y, c if s < ln - 1 else (9 if t < 0.3 else c))
    for x in range(256):
        near.set(x, 100, 8)
    return dict(
        name="SUNSET REEF", far=far, mid=mid, near=near, pals=[farpal, midpal, nearpal],
        grad=grad_table([(0, "3a2464"), (36, "7a3a82"), (66, "d2648e"), (90, "ff9a68"), (100, "ffc282"), (161, "ffc282")]),
        far_f=bands([(0, 0.0)]), near_f=perspective(100, 0.3, 3.2, 0.3),
        mid_fx=0.35, mid_fy=0.0, rain=False)


# ================================================================== Stage 4: Clockwork Moon
def _gear_poly(cx, cy, r, teeth, rot=0.0):
    pts = []
    for i in range(teeth * 4):
        a = rot + i * math.pi * 2 / (teeth * 4)
        rr = r if (i % 4) in (0, 1) else r * 0.8
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return pts


def stage4():
    rnd = random.Random(404)
    far = WrapLayer(256, 160)
    mid = WrapLayer(512, 160)
    near = WrapLayer(256, 160)
    farpal = pal16(["000000", "fff4d0", "f2e0a8", "d8c48a", "b8a472", "ffffff", "8a88c8", "3a3870",
                    "2c2a5a", "4a4884", "f8d870", "6c6aa8", "ffffff", "ffffff", "ffffff", "ffffff"])
    # moon with craters
    mcx, mcy, mr = 180, 40, 28

    def moon(x, y, v, nx, ny):
        c = P.pick(v ** 0.8, [4, 3, 2, 1], x, y, True)
        for (ox, oy, rr) in ((-8, -6, 5), (7, 4, 6), (-3, 12, 4), (12, -10, 3), (-14, 6, 3)):
            d = (x + 0.5 - (mcx + ox)) ** 2 + (y + 0.5 - (mcy + oy)) ** 2
            if d < rr * rr:
                c = 3 if d < (rr - 1.2) ** 2 else 4
        return c
    P.ellipse_cb(far, mcx, mcy, mr, mr, moon, amb=0.45)
    for i in range(60):
        x, y = rnd.randint(0, 255), rnd.randint(0, 90)
        if far.get(x, y) == 0 and far.get(x + 1, y) == 0:
            far.set(x, y, 5 if i % 5 == 0 else 6)
            if i % 9 == 0:
                for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    far.set(x + d[0], y + d[1], 6)
    # clock-tower skyline
    x = 0
    while x < 256:
        w = rnd.randint(10, 22)
        h = rnd.randint(22, 52)
        top = 160 - 40 - h
        P.rect(far, x, top, x + w - 1, 159, 8)
        P.line(far, x, top, x, 159, 9)
        if rnd.random() < 0.5:
            P.poly(far, [(x - 1, top), (x + w / 2, top - 10), (x + w, top)], col=8)
        if w > 14 and rnd.random() < 0.6:
            P.disc(far, x + w / 2, top + 8, 4, 11)
            P.line(far, x + w / 2, top + 8, x + w / 2, top + 5, 7)
            P.line(far, x + w / 2, top + 8, x + w / 2 + 2, top + 8, 7)
        for wy in range(top + 16, 150, 8):
            for wx in range(x + 3, x + w - 3, 5):
                if rnd.random() < 0.3:
                    far.set(wx, wy, 10)
                    far.set(wx, wy + 1, 10)
        x += w + rnd.randint(0, 4)
    midpal = pal16(["000000", "1e1c40", "2c2a58", "403c74", "5a5490", "7a6eaa", "c08c3c", "f2cc7c",
                    "86561e", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    for k in range(5):
        cx = 50 + k * 100 + rnd.randint(-20, 20)
        cy = rnd.choice((rnd.randint(4, 30), rnd.randint(110, 130)))
        r = rnd.randint(14, 24)
        g = WrapLayer(512, 160)
        P.poly(g, _gear_poly(cx, cy, r, rnd.randint(8, 12), rnd.random()), ramp=[1, 2, 3, 4], grad=(0.9, 0.1))
        P.disc(g, cx, cy, r * 0.35, 0)
        P.ring(g, cx, cy, r * 0.35, r * 0.48, 5)
        P.outline_sel(g, 1)
        mid.paste(g)
        # hanging chain
        if cy < 60:
            for y in range(0, int(cy - r)):
                mid.set(cx + (1 if (y // 3) % 2 else 0), y, 3)
    nearpal = pal16(["000000", "2a1a10", "4e3014", "86561e", "c08c3c", "f2cc7c", "1c1a3a", "3a3870",
                     "fff2c8", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # brass rooftops & pipes along the bottom
    x = 0
    while x < 256:
        w = rnd.randint(18, 34)
        top = rnd.randint(132, 146)
        P.rect(near, x, top, x + w - 1, 159, 2)
        P.line(near, x, top, x + w - 1, top, 4)
        P.line(near, x, top + 1, x + w - 1, top + 1, 3)
        for rx in range(x + 3, x + w - 2, 6):
            near.set(rx, top + 3, 5)
        if rnd.random() < 0.6:
            P.rect(near, x + 4, top - 8, x + 7, top - 1, 3)
            P.line(near, x + 4, top - 8, x + 7, top - 8, 5)
        x += w
    for x in range(256):
        y = 152 + int(2 * math.sin(x * 0.1))
        near.set(x, y, 4)
        near.set(x, y + 1, 3)
    return dict(
        name="CLOCKWORK MOON", far=far, mid=mid, near=near, pals=[farpal, midpal, nearpal],
        grad=grad_table([(0, "0c0e2c"), (50, "1a1c48"), (100, "2c2a62"), (140, "4a3c7c"), (161, "5a4a8a")]),
        far_f=bands([(0, 0.02), (70, 0.15)]), near_f=bands([(0, 1.4)]),
        mid_fx=0.55, mid_fy=0.0, rain=False)


# ================================================================== Stage 5: Gloom Fortress
def stage5():
    rnd = random.Random(505)
    far = WrapLayer(256, 160)
    mid = WrapLayer(256, 160)
    near = WrapLayer(256, 160)
    farpal = pal16(["000000", "1a0c24", "26122e", "34183c", "46204c", "ff5a3a", "ffb45a", "5a2a5a",
                    "120818", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    x = 0
    while x < 256:
        w = rnd.randint(12, 26)
        h = rnd.randint(40, 100)
        top = 150 - h
        P.rect(far, x, top, x + w - 1, 159, 2 if (x // 13) % 2 else 3)
        P.line(far, x, top, x, 159, 4)
        P.line(far, x + w - 1, top, x + w - 1, 159, 1)
        for b in range(x, x + w, 4):
            P.rect(far, b, top - 3, b + 1, top - 1, 2)
        if rnd.random() < 0.5:
            P.poly(far, [(x, top - 3), (x + w / 2, top - 18), (x + w, top - 3)], col=1)
        for wy in range(top + 6, 150, 9):
            for wx in range(x + 3, x + w - 3, 6):
                if rnd.random() < 0.35:
                    far.set(wx, wy, 5)
                    far.set(wx, wy + 1, 6 if rnd.random() < 0.5 else 5)
        x += w + rnd.randint(1, 6)
    midpal = pal16(["000000", "120a1a", "241830", "382846", "4e3a5e", "6a5078", "c02848", "ff6a80",
                    "8a7098", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    # girders: a top beam with hanging X-braced trusses, and a lower walkway
    for (y0, y1) in ((0, 9), (124, 131)):
        P.rect(mid, 0, y0, 255, y1, 3)
        P.line(mid, 0, y0 + 1, 255, y0 + 1, 5)
        P.line(mid, 0, y1, 255, y1, 1)
        for xx in range(0, 256, 8):
            mid.set(xx + 3, y0 + 3, 5)
            mid.set(xx + 4, y0 + 4, 1)
    for xx in range(0, 256, 64):
        P.rect(mid, xx, 10, xx + 3, 30, 2)
        P.rect(mid, xx + 28, 10, xx + 31, 30, 2)
        P.line(mid, xx + 1, 10, xx + 1, 30, 4)
        P.line(mid, xx + 29, 10, xx + 29, 30, 4)
        P.line(mid, xx + 3, 11, xx + 28, 29, 2, 2)
        P.line(mid, xx + 28, 11, xx + 3, 29, 2, 2)
        P.line(mid, xx + 4, 11, xx + 28, 28, 4)
        P.rect(mid, xx, 30, xx + 31, 32, 3)
        P.line(mid, xx, 30, xx + 31, 30, 5)
        P.rect(mid, xx + 13, 33, xx + 18, 36, 6)
        mid.set(xx + 14, 34, 7)
    for xx in range(16, 256, 32):
        P.rect(mid, xx + 12, 132, xx + 15, 136, 6)
        mid.set(xx + 13, 133, 7)
    nearpal = pal16(["000000", "0e0814", "1c1226", "2c1c38", "3e2a4c", "5a3e66", "c02848", "ff6a80",
                     "ffd06a", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff", "ffffff"])
    for y in range(136, 160):
        t = (y - 136) / 24.0
        for x in range(256):
            band = (y - 136) % (3 + int(t * 6))
            c = 3 if band else 4
            if x % 32 < 2:
                c = 2
            near.set(x, y, c)
    for x in range(256):
        near.set(x, 136, 5)
        near.set(x, 137, 1)
    for xx in range(4, 256, 32):
        P.rect(near, xx, 140, xx + 5, 141, 6)
        near.set(xx + 1, 140, 7)
    return dict(
        name="GLOOM FORTRESS", far=far, mid=mid, near=near, pals=[farpal, midpal, nearpal],
        grad=grad_table([(0, "0c0614"), (60, "1e0a26"), (110, "3a1030"), (150, "5a1830"), (161, "6a1c30")]),
        far_f=bands([(0, 0.18)]), near_f=perspective(136, 1.0, 3.4, 1.0),
        mid_fx=0.8, mid_fy=0.0, rain=False)


STAGES = [stage1, stage2, stage3, stage4, stage5]
STAGE_NAMES = ["BLOSSOM SKIES", "STORM FRONT", "SUNSET REEF", "CLOCKWORK MOON", "GLOOM FORTRESS"]


# ================================================================== UI tiles (BG palette 15)
PAL_UI = pal16([
    "000000", "241430", "ffffff", "d6cef0", "fff6fa", "5a2a4e", "f6dcea", "4a2848",
    "ffd84d", "c8801c", "ff7ba6", "c0406e", "6fcf7a", "7fe0ff", "9a8ab0", "eab0cc",
])


def ui_tiles():
    tiles = [Layer(8, 8)]
    idx = {}
    idx["hud"] = len(tiles)
    for ch in F.FONT_CHARS:
        tiles.append(F.glyph_tile(ch, 2, 3, 1))
    idx["pan"] = len(tiles)
    for ch in F.FONT_CHARS:
        tiles.append(F.panel_glyph_tile(ch, 7, 15, 4))
    idx["gold"] = len(tiles)
    for ch in "0123456789":
        tiles.append(F.glyph_tile(ch, 8, 9, 1))
    # panel frame (3x3)
    idx["panel"] = len(tiles)
    for r in range(3):
        for c in range(3):
            L = Layer(8, 8, 4)
            for y in range(8):
                for x in range(8):
                    edge_t = r == 0 and y < 2
                    edge_b = r == 2 and y > 5
                    edge_l = c == 0 and x < 2
                    edge_r = c == 2 and x > 5
                    if edge_t or edge_b or edge_l or edge_r:
                        L.set(x, y, 5)
                    elif (r == 0 and y == 2) or (c == 0 and x == 2):
                        L.set(x, y, 2)
                    elif (r == 2 and y == 5) or (c == 2 and x == 5):
                        L.set(x, y, 6)
            # round the outer corners
            if r != 1 and c != 1:
                cx = 0 if c == 0 else 7
                cy = 0 if r == 0 else 7
                for (dx, dy) in ((0, 0), (1, 0), (0, 1)):
                    L.set(abs(cx - dx), abs(cy - dy), 0)
                L.set(abs(cx - 1), abs(cy - 1), 5)
            tiles.append(L)
    # boss bar: left cap, 9 fill levels, right cap
    idx["bar"] = len(tiles)
    capl = Layer(8, 8)
    for y in range(1, 7):
        capl.set(6, y, 1)
        capl.set(7, y, 1)
    capl.set(7, 0, 1)
    capl.set(7, 7, 1)
    tiles.append(capl)
    for lvl in range(9):
        L = Layer(8, 8)
        for x in range(8):
            L.set(x, 0, 1)
            L.set(x, 7, 1)
            for y in range(1, 7):
                if x < lvl:
                    L.set(x, y, 10 if y > 2 else (2 if y == 1 else 15))
                else:
                    L.set(x, y, 7)
            if x < lvl:
                L.set(x, 6, 11)
        tiles.append(L)
    capr = Layer(8, 8)
    for y in range(1, 7):
        capr.set(0, y, 1)
        capr.set(1, y, 1)
    capr.set(0, 0, 1)
    capr.set(0, 7, 1)
    tiles.append(capr)
    # icons: life (mochi head), bomb (seed bloom), speed arrow, power pip on/off
    idx["life"] = len(tiles)
    L = Layer(8, 8)
    P.ellipse(L, 4, 4.6, 3.4, 2.9, None, flat=10)
    P.ellipse(L, 3, 3.5, 1.2, 0.7, None, flat=2)
    L.set(4, 4, 7)
    L.set(6, 4, 7)
    L.set(3, 0, 12)
    L.set(4, 1, 12)
    P.outline_sel(L, 1)
    tiles.append(L)
    idx["bomb"] = len(tiles)
    L = Layer(8, 8)
    for i in range(5):
        a = i * 2 * math.pi / 5 - math.pi / 2
        P.disc(L, 4 + math.cos(a) * 1.9, 4 + math.sin(a) * 1.9, 1.3, 8)
    P.disc(L, 4, 4, 1.0, 10)
    P.outline_sel(L, 1)
    tiles.append(L)
    idx["speed"] = len(tiles)
    L = Layer(8, 8)
    P.poly(L, [(1, 1), (4, 4), (1, 7)], col=13)
    P.poly(L, [(4, 1), (7, 4), (4, 7)], col=2)
    P.outline_sel(L, 1)
    tiles.append(L)
    idx["pip"] = len(tiles)
    for on in (1, 0):
        L = Layer(8, 8)
        P.disc(L, 4, 4, 2.4, 8 if on else 14)
        if on:
            L.set(3, 3, 2)
        P.outline_sel(L, 1)
        tiles.append(L)
    # weapon icons (pea, spread, laser, homing)
    idx["weap"] = len(tiles)
    for w in range(4):
        L = Layer(8, 8)
        if w == 0:
            P.disc(L, 4, 4, 2.6, 12)
            L.set(3, 3, 2)
        elif w == 1:
            for a in (-0.6, 0, 0.6):
                L.set(int(2 + math.cos(a) * 4), int(4 + math.sin(a) * 4), 10)
                L.set(int(2 + math.cos(a) * 2), int(4 + math.sin(a) * 2), 10)
        elif w == 2:
            P.rect(L, 0, 3, 7, 4, 13)
            P.rect(L, 0, 3, 7, 3, 2)
        else:
            P.poly(L, [(1, 4), (4, 2), (7, 4), (4, 6)], col=8)
            L.set(4, 4, 2)
        P.outline_sel(L, 1)
        tiles.append(L)
    # sproutling option + shield icons
    idx["sprout"] = len(tiles)
    L = Layer(8, 8)
    P.ellipse(L, 4, 5, 2.6, 2.3, None, flat=12)
    L.set(3, 4, 2)
    L.set(2, 1, 12)
    L.set(3, 2, 12)
    L.set(5, 1, 12)
    L.set(4, 2, 12)
    P.outline_sel(L, 1)
    tiles.append(L)
    idx["shield"] = len(tiles)
    L = Layer(8, 8)
    P.ring(L, 4, 4, 2.2, 3.4, 13)
    L.set(3, 2, 2)
    P.outline_sel(L, 1)
    tiles.append(L)
    # chain gauge segments (empty, full)
    idx["gauge"] = len(tiles)
    for full in (0, 1):
        L = Layer(8, 8)
        for x in range(8):
            L.set(x, 5, 1)
            L.set(x, 7, 1)
            L.set(x, 6, 8 if full else 14)
        tiles.append(L)
    # solid dark tile (for HUD strip backing) and dotted divider
    idx["solid"] = len(tiles)
    tiles.append(Layer(8, 8, 1))
    # menu cursor (pink heart)
    idx["cursor"] = len(tiles)
    L = Layer(8, 8)
    P.poly(L, [(0.5, 2.5), (2.5, 0.8), (4, 2.2), (5.5, 0.8), (7.5, 2.5), (4, 7)], col=10)
    L.set(2, 2, 2)
    P.outline_sel(L, 1)
    tiles.append(L)
    return tiles, idx


# ================================================================== banners (BG palettes 11/12/14)
PAL_BANNER_GOLD = pal16(["000000", "2a1236", "6a2a5a", "fff6c8", "ffe070", "ffb840", "f0802a", "ffffff"])
PAL_BANNER_PINK = pal16(["000000", "2a1236", "5a2050", "fff0f6", "ffc0d8", "ff8ab8", "e0508e", "ffffff"])
PAL_BANNER_RED = pal16(["000000", "2a0a10", "5a1020", "ffe0d0", "ff9a70", "ff5040", "d02030", "ffffff"])
PAL_BANNER_SKY = pal16(["000000", "1a1a46", "2a3a7a", "eafcff", "a8e4ff", "6cc0f4", "3e8ce0", "ffffff"])

BANNERS = ["STAGE 1", "STAGE 2", "STAGE 3", "STAGE 4", "STAGE 5"] + STAGE_NAMES + [
    "WARNING", "STAGE CLEAR!", "GAME OVER", "PAUSED", "CONTINUE?", "ALL CLEAR!", "NEW RECORD!",
    "RECORDS", "THE END", "PRACTICE"]


def banner_layer(text):
    return F.banner(text, [3, 4, 5, 6], 1, 2, 7, 16)


# ================================================================== title logo (BG palette 13)
PAL_LOGO = pal16(["000000", "3a1838", "8a3a6c", "ff86b4", "ffaacb", "ffcfe0", "fff0f6",
                  "3a8ce0", "66b8f4", "a6dcff", "e6f8ff", "3d8a58", "6fcf7a", "35203f", "ffffff", "c8507e"])


def _big_glyph(ch):
    g = F.glyph(ch)
    cols = [x for x in range(5) if any(g[y][x] == "#" for y in range(6))]
    t = [r[min(cols):max(cols) + 1] for r in g]
    s1 = F._scale2x(t)
    s1 = ["".join("#" if v else "." for v in row) for row in s1]
    s2 = F._scale2x(s1)
    h, w = len(s2), len(s2[0])
    # thicken strokes by one pixel for a chunkier logo
    out = []
    for y in range(h + 1):
        row = ""
        for x in range(w + 1):
            on = False
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                xx, yy = x - dx, y - dy
                if 0 <= xx < w and 0 <= yy < h and s2[yy][xx]:
                    on = True
            row += "#" if on else "."
        out.append(row)
    return out


def logo_layer():
    L = Layer(224, 72)
    body = Layer(224, 72)

    def word(text, x, y, ramp, special_o=False):
        for ch in text:
            if ch == " ":
                x += 8
                continue
            g = _big_glyph(ch)
            gh, gw = len(g), len(g[0])
            if special_o and ch == "O":
                # Mochi's face stands in for the O
                cx, cy = x + gw / 2.0, y + gh / 2.0 + 1
                P.ellipse(body, cx, cy, gw / 2.0 + 1.5, gh / 2.0 - 0.5, [3, 4, 5, 6], amb=0.45)
                P.ellipse(body, cx - 4, cy - 5, 3.2, 1.8, None, flat=14)
                for ex in (cx - 4, cx + 3):
                    P.rect(body, int(ex), int(cy - 2), int(ex) + 1, int(cy + 2), 13)
                    body.set(int(ex), int(cy - 2), 14)
                for bx in (cx - 8, cx - 7, cx + 6, cx + 7):
                    body.set(int(bx), int(cy + 3), 15)
                body.set(int(cx - 1), int(cy + 3), 13)
                body.set(int(cx), int(cy + 4), 13)
                body.set(int(cx + 1), int(cy + 3), 13)
                # leaf sprout on top
                lf = Layer(224, 72)
                P.ellipse(lf, cx - 4, cy - gh / 2.0 - 2, 4.2, 2.2, [11, 12, 12], amb=0.5)
                P.ellipse(lf, cx + 4, cy - gh / 2.0 - 4, 4.6, 2.4, [11, 12, 12], amb=0.5)
                P.line(lf, cx, cy - gh / 2.0 + 1, cx, cy - gh / 2.0 - 3, 11)
                body.paste(lf)
                x += gw + 3
                continue
            for yy in range(gh):
                for xx in range(gw):
                    if g[yy][xx] == "#":
                        t = yy / (gh - 1)
                        c = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
                        if yy == 0 or g[yy - 1][xx] != "#":
                            c = ramp[0] if t < 0.5 else c
                        body.set(x + xx, y + yy, c)
            # inner gloss stripe
            for xx in range(gw):
                for yy in range(2, gh // 3):
                    if g[yy][xx] == "#" and (xx + yy) % 7 == 0:
                        body.set(x + xx, y + yy, 14)
            x += gw + 3
        return x

    word("SKY", 6, 3, [10, 9, 8, 7])
    word("SPROUT", 30, 35, [6, 5, 4, 3, 15], special_o=True)
    # extruded 3D depth (3px down-right) + outline
    for d in (3, 2, 1):
        for y in range(72):
            for x in range(224):
                if body.get(x, y) and not body.get(x + d, y + d):
                    L.set(x + d, y + d, 2)
    L.paste(body)
    P.outline_sel(L, 1, diag=True)
    return L
