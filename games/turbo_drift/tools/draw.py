"""2D drawing helpers (polygons, ellipses, dithered gradients) on indexed Layers."""
import math

BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def poly(lay, pts, c):
    """Even-odd scanline fill of a polygon (float points)."""
    if not pts:
        return
    ys = [p[1] for p in pts]
    y0 = max(0, int(math.floor(min(ys))))
    y1 = min(lay.h - 1, int(math.ceil(max(ys))))
    n = len(pts)
    for y in range(y0, y1 + 1):
        py = y + 0.5
        xs = []
        for i in range(n):
            ax, ay = pts[i]
            bx, by = pts[(i + 1) % n]
            if (ay <= py < by) or (by <= py < ay):
                xs.append(ax + (py - ay) * (bx - ax) / (by - ay))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            xa = int(math.ceil(xs[i] - 0.5))
            xb = int(math.floor(xs[i + 1] - 0.5))
            for x in range(max(0, xa), min(lay.w - 1, xb) + 1):
                lay.px[y][x] = c


def ellipse(lay, cx, cy, rx, ry, c, fn=None):
    for y in range(int(cy - ry - 1), int(cy + ry + 2)):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            dx = (x + 0.5 - cx) / rx
            dy = (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1.0:
                if fn:
                    v = fn(x, y, dx, dy, d)
                    if v is not None:
                        lay.set(x, y, v)
                else:
                    lay.set(x, y, c)


def thick_line(lay, x0, y0, x1, y1, w0, w1, c):
    """Tapered line as a quad."""
    dx, dy = x1 - x0, y1 - y0
    l = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / l, dx / l
    poly(lay, [(x0 + nx * w0 / 2, y0 + ny * w0 / 2), (x1 + nx * w1 / 2, y1 + ny * w1 / 2),
               (x1 - nx * w1 / 2, y1 - ny * w1 / 2), (x0 - nx * w0 / 2, y0 - ny * w0 / 2)], c)


def dither(x, y, t):
    """Ordered dither: True if t (0..1) exceeds the Bayer threshold at x,y."""
    return t * 16.0 > BAYER4[y & 3][x & 3] + 0.5


def ramp_pick(ramp, t, x, y):
    """Pick from a list of colour indices using t in 0..1 with ordered dithering."""
    t = max(0.0, min(0.9999, t))
    f = t * (len(ramp) - 1)
    i = int(f)
    frac = f - i
    if i + 1 < len(ramp) and dither(x, y, frac):
        return ramp[i + 1]
    return ramp[i]


class Rng:
    """Small deterministic LCG (independent of Python's random implementation)."""

    def __init__(self, seed):
        self.s = seed & 0xFFFFFFFF

    def next(self):
        self.s = (self.s * 1664525 + 1013904223) & 0xFFFFFFFF
        return self.s >> 8

    def rand(self):
        return self.next() / float(1 << 24)

    def range(self, a, b):
        return a + self.next() % (b - a + 1)

    def uniform(self, a, b):
        return a + (b - a) * self.rand()
