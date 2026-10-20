"""Procedural pixel-art "sculpting" for Mochi Monsters creatures (original code).

Shapes are defined in a 64x64 design space (feet at y~62, facing left). Each
part is rasterised into a target canvas through a transform (scale/squash),
"inflated" into a dome with a distance field, shaded by a top-left light into
a 3-tone ramp with light dithering, then outlined (inner lines in the part's
darkest ramp colour, a coloured outer outline around the silhouette).
"""
import math

LIGHT = (-0.48, -0.66, 0.58)
_ln = math.sqrt(sum(v * v for v in LIGHT))
LIGHT = tuple(v / _ln for v in LIGHT)


class Shape:
    def __init__(self, fn, bb):
        self.fn = fn
        self.bb = bb  # (x0, y0, x1, y1) in design space

    def __or__(self, o):
        a, b = self, o
        return Shape(lambda x, y: a.fn(x, y) or b.fn(x, y),
                     (min(a.bb[0], b.bb[0]), min(a.bb[1], b.bb[1]), max(a.bb[2], b.bb[2]), max(a.bb[3], b.bb[3])))

    def __sub__(self, o):
        a, b = self, o
        return Shape(lambda x, y: a.fn(x, y) and not b.fn(x, y), a.bb)

    def __and__(self, o):
        a, b = self, o
        return Shape(lambda x, y: a.fn(x, y) and b.fn(x, y), a.bb)


def ell(cx, cy, rx, ry, rot=0.0, p=2.0, rb=None):
    """Ellipse / superellipse; rb = different radius for the bottom half."""
    c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    rr = max(rx, ry, rb or 0) + 1

    def f(x, y):
        dx, dy = x - cx, y - cy
        u = (dx * c + dy * s) / rx
        vv = (-dx * s + dy * c)
        v = vv / ((rb if (rb and vv > 0) else ry))
        return abs(u) ** p + abs(v) ** p <= 1.0
    return Shape(f, (cx - rr, cy - rr, cx + rr, cy + rr))


def circ(cx, cy, r):
    return ell(cx, cy, r, r)


def poly(pts):
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]

    def f(x, y):
        ins = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
                ins = not ins
            j = i
        return ins
    return Shape(f, (min(xs), min(ys), max(xs), max(ys)))


def capsule(x0, y0, x1, y1, r0, r1=None):
    r1 = r0 if r1 is None else r1
    vx, vy = x1 - x0, y1 - y0
    L2 = vx * vx + vy * vy + 1e-9

    def f(x, y):
        t = max(0.0, min(1.0, ((x - x0) * vx + (y - y0) * vy) / L2))
        px, py = x0 + vx * t, y0 + vy * t
        r = r0 + (r1 - r0) * t
        return (x - px) ** 2 + (y - py) ** 2 <= r * r
    m = max(r0, r1) + 1
    return Shape(f, (min(x0, x1) - m, min(y0, y1) - m, max(x0, x1) + m, max(y0, y1) + m))


def leaf(x0, y0, x1, y1, w, bend=0.0):
    """Pointed almond from (x0,y0) to (x1,y1), max half-width w, optional bend."""
    vx, vy = x1 - x0, y1 - y0
    L = math.sqrt(vx * vx + vy * vy) + 1e-9
    ux, uy = vx / L, vy / L

    def f(x, y):
        dx, dy = x - x0, y - y0
        t = (dx * ux + dy * uy) / L
        if t < 0 or t > 1:
            return False
        d = -dx * uy + dy * ux - bend * math.sin(math.pi * t) * L
        return abs(d) <= w * (4 * t * (1 - t)) ** 0.75
    m = w + abs(bend) * L + 1
    return Shape(f, (min(x0, x1) - m, min(y0, y1) - m, max(x0, x1) + m, max(y0, y1) + m))


def flame(cx, by, w, h, lean=0.0, tongues=3):
    """Flame silhouette: rounded base at (cx,by) rising h px with wavy tongues."""
    def f(x, y):
        t = (by - y) / h  # 0 at base, 1 at tip
        if t < -0.32 or t > 1.0:
            return False
        if t < 0:
            return (x - cx) ** 2 / (w * w) + (t / 0.32) ** 2 <= 1.0
        xc = cx + lean * t * t * h
        half = w * (1 - t) ** 0.9 * (1.0 + 0.28 * math.sin(t * math.pi * tongues + 0.7))
        return abs(x - xc) <= half
    return Shape(f, (cx - w * 1.4 - abs(lean) * h, by - h - 1, cx + w * 1.4 + abs(lean) * h, by + w))


class Sculpt:
    """Target canvas of palette indices. Palette layout:
    0 transparent, 1 eye dark, 2 white, 3 blush, ramps at 4.., 8.., 12.. (o, d, b, l)."""

    def __init__(self, W=64, H=64, k=1.0, ox=32.0, oy=64.0, sx=1.0, sy=1.0, ramps=("A", "B", "C")):
        self.W, self.H = W, H
        self.k, self.ox, self.oy, self.sx, self.sy = k, ox, oy, sx, sy
        self.px = [[0] * W for _ in range(H)]
        self.ramp = {name: 4 + 4 * i for i, name in enumerate(ramps)}

    # design <-> target
    def tx(self, x, y):
        return ((x - 32) * self.k * self.sx + self.ox, (y - 64) * self.k * self.sy + self.oy)

    def dz(self, X, Y):
        return ((X + 0.5 - self.ox) / (self.k * self.sx) + 32, (Y + 0.5 - self.oy) / (self.k * self.sy) + 64)

    def _mask(self, shape):
        x0, y0 = self.tx(shape.bb[0], shape.bb[1])
        x1, y1 = self.tx(shape.bb[2], shape.bb[3])
        X0, Y0 = max(0, int(min(x0, x1)) - 1), max(0, int(min(y0, y1)) - 1)
        X1, Y1 = min(self.W, int(max(x0, x1)) + 2), min(self.H, int(max(y0, y1)) + 2)
        m = set()
        for Y in range(Y0, Y1):
            for X in range(X0, X1):
                if shape.fn(*self.dz(X, Y)):
                    m.add((X, Y))
        return m

    @staticmethod
    def _dist(m):
        """Chamfer distance (3-4) to the nearest outside pixel, in pixels."""
        INF = 10 ** 6
        d = {p: INF for p in m}
        for p in m:
            x, y = p
            for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if q not in m:
                    d[p] = 3
                    break
        pts = sorted(m, key=lambda p: (p[1], p[0]))
        fw = ((-1, 0, 3), (0, -1, 3), (-1, -1, 4), (1, -1, 4))
        bw = ((1, 0, 3), (0, 1, 3), (1, 1, 4), (-1, 1, 4))
        for _ in range(2):
            for p in pts:
                for dx, dy, c in fw:
                    q = (p[0] + dx, p[1] + dy)
                    if q in d and d[q] + c < d[p]:
                        d[p] = d[q] + c
            for p in reversed(pts):
                for dx, dy, c in bw:
                    q = (p[0] + dx, p[1] + dy)
                    if q in d and d[q] + c < d[p]:
                        d[p] = d[q] + c
        return {p: v / 3.0 for p, v in d.items()}

    def part(self, shape, ramp, outline=True, puff=None, lo=0.02, hi=0.6, dither=0.06,
             shine=False, flat=None, tone_shift=0.0, inline=True):
        m = self._mask(shape)
        if not m:
            return m
        base = self.ramp[ramp]
        if flat is not None:
            for (X, Y) in m:
                self.px[Y][X] = base + flat
            return m
        d = self._dist(m)
        R = puff if puff is not None else max(1.5, max(d.values()) * 0.95)
        R = R * self.k if puff is not None else R

        def h(p):
            v = d.get(p, 0.0)
            v = min(v, R) / R
            return R * math.sqrt(max(0.0, 1 - (1 - v) ** 2))
        old = {p: self.px[p[1]][p[0]] for p in m}
        for p in m:
            X, Y = p
            nx = -(h((X + 1, Y)) - h((X - 1, Y))) * 0.5
            ny = -(h((X, Y + 1)) - h((X, Y - 1))) * 0.5
            nz = 1.0
            ln = math.sqrt(nx * nx + ny * ny + nz * nz)
            I = (nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2]) / ln
            I = (I - 0.45) * 2.2 + tone_shift
            chk = (X + Y) & 1
            if I < lo - dither:
                lv = 1
            elif I < lo + dither:
                lv = 1 if chk else 2
            elif I < hi - dither:
                lv = 2
            elif I < hi + dither:
                lv = 3 if chk else 2
            else:
                lv = 3
            if shine and I > hi + 0.55:
                self.px[Y][X] = 2
            else:
                self.px[Y][X] = base + lv
        if outline and inline:
            # inner lines where this part overlaps earlier opaque pixels
            for p in m:
                X, Y = p
                for q in ((X + 1, Y), (X - 1, Y), (X, Y + 1), (X, Y - 1)):
                    if q not in m and 0 <= q[0] < self.W and 0 <= q[1] < self.H and self.px[q[1]][q[0]]:
                        if self.px[q[1]][q[0]] in (1, 2):
                            continue
                        self.px[Y][X] = base
                        break
        return m

    def decal(self, shape, ramp, level=3, edge=None):
        """Flat patch (e.g. belly) with an optional softer edge level."""
        m = self._mask(shape)
        base = self.ramp[ramp]
        for (X, Y) in m:
            if self.px[Y][X]:
                self.px[Y][X] = base + level
        if edge is not None:
            for (X, Y) in m:
                for q in ((X + 1, Y), (X - 1, Y), (X, Y + 1), (X, Y - 1)):
                    if q not in m:
                        if self.px[Y][X]:
                            self.px[Y][X] = base + edge
                        break
        return m

    def shade_region(self, m, ramp):
        """Re-shade an existing mask region (used for decals that should look rounded)."""
        pass

    def line(self, pts, col, ramp=None):
        """Pixel polyline in design coords; col = absolute index or ramp level."""
        c = col if ramp is None else self.ramp[ramp] + col
        for i in range(len(pts) - 1):
            x0, y0 = self.tx(*pts[i])
            x1, y1 = self.tx(*pts[i + 1])
            n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
            for j in range(n + 1):
                t = j / max(1, n)
                X = int(x0 + (x1 - x0) * t)
                Y = int(y0 + (y1 - y0) * t)
                if 0 <= X < self.W and 0 <= Y < self.H:
                    self.px[Y][X] = c

    def dot(self, x, y, col, ramp=None):
        c = col if ramp is None else self.ramp[ramp] + col
        X, Y = self.tx(x, y)
        X, Y = int(X), int(Y)
        if 0 <= X < self.W and 0 <= Y < self.H:
            self.px[Y][X] = c

    def stamp(self, x, y, art, legend):
        """Stamp a pixel template centred at design point (x,y), unscaled."""
        rows = art.strip("\n").split("\n")
        X0, Y0 = self.tx(x, y)
        X0 = int(round(X0 - len(rows[0]) / 2.0))
        Y0 = int(round(Y0 - len(rows) / 2.0))
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch in legend:
                    X, Y = X0 + i, Y0 + j
                    if 0 <= X < self.W and 0 <= Y < self.H:
                        v = legend[ch]
                        if isinstance(v, tuple):
                            v = self.ramp[v[0]] + v[1]
                        self.px[Y][X] = v

    def eye(self, x, y, style="open", near=True):
        big = self.k >= 0.8
        if not big:
            arts = {
                "open": "#W\n##", "far": "#\n#", "sleepy": "##", "happy": "#.#\n.#.", "angry": "##\n##",
            }
            a = arts.get(style if near else ("far" if style in ("open", "angry") else style), "#W\n##")
            if style == "happy":
                a = ".#.\n#.#"
            self.stamp(x, y, a, {"#": 1, "W": 2})
            return
        if style == "open":
            a = ".##.\n#WW#\n#W##\n####\n##W#\n.##." if near else ".#.\n#W#\n###\n###\n.#."
        elif style == "angry":
            a = "##..\n#WW#\n#W##\n####\n.##." if near else "#..\n#W#\n###\n.#."
        elif style == "sleepy":
            a = "#..#\n.##." if near else "#.#\n.#."
        elif style == "happy":
            a = ".##.\n#..#" if near else ".#.\n#.#"
        elif style == "tiny":
            a = "##\n#W\n##" if near else "#\n#"
        else:
            a = ".##.\n####\n####\n.##."
        self.stamp(x, y, a, {"#": 1, "W": 2})

    def blush(self, x, y, w=3):
        a = "#" * w if self.k >= 0.8 else "#"
        self.stamp(x, y, a, {"#": 3})

    def outline(self, weights=None):
        """Coloured outer outline: transparent pixels next to the silhouette take the
        darkest tone of the neighbouring ramp."""
        W, H = self.W, self.H
        add = []
        for Y in range(H):
            for X in range(W):
                if self.px[Y][X]:
                    continue
                best = None
                for dx, dy in ((0, 1), (1, 0), (-1, 0), (0, -1)):
                    X2, Y2 = X + dx, Y + dy
                    if 0 <= X2 < W and 0 <= Y2 < H:
                        c = self.px[Y2][X2]
                        if c:
                            best = c
                            break
                if best is not None:
                    if best < 4:
                        o = 1
                    else:
                        o = ((best - 4) // 4) * 4 + 4
                    add.append((X, Y, o))
        for X, Y, o in add:
            self.px[Y][X] = o
        # dark outline is darker on the shadow (bottom/right) side: use eye dark there
        for Y in range(H):
            for X in range(W):
                c = self.px[Y][X]
                if c >= 4 and (c - 4) % 4 == 0:
                    below = self.px[Y - 1][X] if Y > 0 else 0
                    left = self.px[Y][X - 1] if X > 0 else 0
                    if Y > 0 and X > 0 and below and left and below != c and left != c:
                        pass
        return self

    def layer(self):
        return self.px


def palette(ramps, eye="35203f", white="ffffff", blush="ff8fab"):
    out = ["000000", eye, white, blush]
    for r in ramps:
        assert len(r) == 4, r
        out += list(r)
    while len(out) < 16:
        out.append("ff00ff")
    return out
