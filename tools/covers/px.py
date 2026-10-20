"""Tiny pixel-art toolkit for Retra's catalog covers (240x160 canvas, upscaled 2x)."""
import math
import random

from PIL import Image

W, H = 240, 160
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp(colors, n):
    """n colours evenly through the given stops."""
    out = []
    for k in range(n):
        t = k / max(1, n - 1) * (len(colors) - 1)
        i = min(int(t), len(colors) - 2)
        out.append(mix(colors[i], colors[i + 1], t - i))
    return out


class Canvas:
    def __init__(self, seed):
        self.img = Image.new("RGB", (W, H))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def put(self, x, y, c):
        if 0 <= x < W and 0 <= y < H:
            self.px[x, y] = c

    def get(self, x, y):
        return self.px[max(0, min(W - 1, x)), max(0, min(H - 1, y))]

    # -- fills -------------------------------------------------------------------------------
    def vgrad(self, y0, y1, stops, steps=6, x0=0, x1=W):
        """Ordered-dithered vertical gradient through a small ramp (keeps the palette tight)."""
        pal = ramp([hexc(s) if isinstance(s, str) else s for s in stops], steps)
        for y in range(max(0, y0), min(H, y1)):
            t = (y - y0) / max(1, y1 - y0 - 1) * (steps - 1)
            for x in range(x0, x1):
                i = int(t)
                f = t - i
                if f * 16 > BAYER[y & 3][x & 3] + 0.5 and i + 1 < steps:
                    i += 1
                self.px[x, y] = pal[min(i, steps - 1)]

    def rect(self, x0, y0, x1, y1, c):
        c = hexc(c) if isinstance(c, str) else c
        for y in range(max(0, y0), min(H, y1)):
            for x in range(max(0, x0), min(W, x1)):
                self.px[x, y] = c

    def dither_rect(self, x0, y0, x1, y1, c, density):
        """Sprinkles c over a rect with Bayer density 0..1 (soft light, haze, shadows)."""
        c = hexc(c) if isinstance(c, str) else c
        for y in range(max(0, y0), min(H, y1)):
            for x in range(max(0, x0), min(W, x1)):
                if BAYER[y & 3][x & 3] < density * 16:
                    self.px[x, y] = c

    def disc(self, cx, cy, r, c, shade=None, light=None):
        c = hexc(c) if isinstance(c, str) else c
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                dx, dy = x - cx + 0.5, y - cy + 0.5
                d = dx * dx + dy * dy
                if d <= r * r:
                    col = c
                    if shade is not None and dx + dy > r * 0.55 and BAYER[y & 3][x & 3] < 10:
                        col = hexc(shade) if isinstance(shade, str) else shade
                    if light is not None and dx + dy < -r * 0.75:
                        col = hexc(light) if isinstance(light, str) else light
                    self.put(x, y, col)

    def glow(self, cx, cy, r, c, strength=0.6):
        """Dithered radial light pool."""
        c = hexc(c) if isinstance(c, str) else c
        for y in range(int(cy - r), int(cy + r) + 1):
            for x in range(int(cx - r), int(cx + r) + 1):
                d = math.hypot(x - cx, y - cy) / r
                if d < 1 and BAYER[y & 3][x & 3] < (1 - d) ** 1.6 * strength * 16:
                    self.put(x, y, c)

    def poly(self, pts, c):
        c = hexc(c) if isinstance(c, str) else c
        ys = [p[1] for p in pts]
        for y in range(max(0, int(min(ys))), min(H, int(max(ys)) + 1)):
            xs = []
            n = len(pts)
            for i in range(n):
                (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
                if (y0 <= y + 0.5 < y1) or (y1 <= y + 0.5 < y0):
                    xs.append(x0 + (y + 0.5 - y0) * (x1 - x0) / (y1 - y0))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(int(round(xs[i])), int(round(xs[i + 1]))):
                    self.put(x, y, c)

    # -- terrain -----------------------------------------------------------------------------
    def noise(self, seed, octaves=3):
        r = random.Random(seed)
        tables = [[r.random() for _ in range(64)] for _ in range(octaves)]

        def f(x):
            v, amp, tot = 0.0, 1.0, 0.0
            for o in range(octaves):
                fx = x * (2 ** o)
                i = int(math.floor(fx))
                t = fx - i
                t = t * t * (3 - 2 * t)
                a, b = tables[o][i % 64], tables[o][(i + 1) % 64]
                v += (a + (b - a) * t) * amp
                tot += amp
                amp *= 0.5
            return v / tot
        return f

    def ridge(self, seed, base, amp, freq, c, rim=None, octaves=3, sharp=False):
        """Fills everything below a noisy skyline; optional lit rim on top."""
        n = self.noise(seed, octaves)
        c = hexc(c) if isinstance(c, str) else c
        rimc = hexc(rim) if isinstance(rim, str) else rim
        tops = []
        for x in range(W):
            v = n(x * freq)
            if sharp:
                v = 1 - abs(v * 2 - 1)
            top = int(base - v * amp)
            tops.append(top)
            for y in range(max(0, top), H):
                self.px[x, y] = c
            if rimc is not None and 0 <= top < H:
                self.px[x, top] = rimc
        return tops

    def stars(self, n, colors, ymax=H, seed=0):
        r = random.Random(seed)
        for _ in range(n):
            x, y = r.randrange(W), r.randrange(max(1, ymax))
            c = hexc(r.choice(colors))
            self.put(x, y, c)
            if r.random() < 0.12:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    self.put(x + dx, y + dy, mix(c, self.get(x + dx, y + dy), 0.5))

    def cloud(self, x, y, w, c, shade):
        c, s = hexc(c), hexc(shade)
        r = random.Random(x * 31 + y)
        bumps = max(2, w // 10)
        for i in range(bumps):
            bx = x + i * w / bumps + r.uniform(-2, 2)
            br = r.uniform(5, 9) if 0 < i < bumps - 1 else r.uniform(3, 6)
            self.disc(bx, y - br * 0.4, br, c)
        self.rect(int(x - 3), int(y), int(x + w), int(y + 4), c)
        for xx in range(int(x - 3), int(x + w)):
            self.put(xx, int(y + 4), s)

    def pine(self, x, y, h, c, dark, light=None):
        c, d = hexc(c), hexc(dark)
        for i in range(h):
            half = int((i / h) * h * 0.38) + 1
            for dx in range(-half, half + 1):
                self.put(x + dx, y - h + i, d if dx > half // 3 else c)
        if light:
            for i in range(2, h, 3):
                self.put(x - int((i / h) * h * 0.3), y - h + i, hexc(light))
        self.rect(x, y, x + 1, y + 2, d)

    def round_tree(self, x, y, r, c, dark, light, trunk):
        self.rect(x - 1, y - r // 2, x + 1, y + 3, trunk)
        self.disc(x, y - r, r, c, shade=dark, light=light)

    def building(self, x, y, w, h, wall, dark, window, lit=0.4, seed=0):
        r = random.Random(seed)
        self.rect(x, y - h, x + w, y, wall)
        self.rect(x + w - max(1, w // 4), y - h, x + w, y, dark)
        for wy in range(y - h + 3, y - 2, 4):
            for wx in range(x + 2, x + w - 2, 3):
                if r.random() < lit:
                    self.put(wx, wy, hexc(window))

    def save(self, path, scale=2):
        out = self.img.resize((W * scale, H * scale), Image.NEAREST)
        out = out.quantize(colors=64, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        out.save(path, optimize=True)
