"""Original track layouts for Turbo Drift.

A track is a loop of road segments (256 world units each).  Each segment has
a curvature (signed byte, + bends right) and a height (world units).  Sections
are written like (enter, hold, leave, curve, hill) with eased transitions.
Roadside objects are placed procedurally (deterministic LCG) per environment.
"""
import math

from draw import Rng

SEG_LEN = 256
HILL_UNIT = 160      # world units per hill step


def ease_in(a, b, t):
    return a + (b - a) * t * t


def ease_in_out(a, b, t):
    return a + (b - a) * ((-math.cos(t * math.pi) / 2) + 0.5)


class Track:
    def __init__(self, name, env, laps, sections, seed, density=1.0):
        self.name = name
        self.env = env
        self.laps = laps
        self.curve = []
        self.y = []
        self.seed = seed
        self.density = density
        last_y = 0.0
        for (enter, hold, leave, curve, hill) in sections:
            start_y = last_y
            end_y = start_y + hill * HILL_UNIT
            total = enter + hold + leave
            n = 0
            for i in range(enter):
                self._add(ease_in(0, curve, i / max(1, enter)), ease_in_out(start_y, end_y, n / total))
                n += 1
            for i in range(hold):
                self._add(curve, ease_in_out(start_y, end_y, n / total))
                n += 1
            for i in range(leave):
                self._add(ease_in_out(curve, 0, i / max(1, leave)), ease_in_out(start_y, end_y, n / total))
                n += 1
            last_y = end_y
        # return smoothly to height 0 over the final stretch
        if abs(last_y) > 1:
            k = 120
            base = len(self.y) - k
            y0 = self.y[base]
            for i in range(k):
                self.y[base + i] = ease_in_out(y0, 0.0, i / float(k))
        self.n = len(self.curve)
        self.sprites = []
        self.place_sprites()

    def _add(self, c, y):
        self.curve.append(int(round(c)))
        self.y.append(y)

    # ------------------------------------------------------------ decoration
    def place_sprites(self):
        rng = Rng(self.seed)
        n = self.n
        add = self.sprites.append
        dens = self.density
        # start pylons both sides
        add((3, -1.30, 6, 0))
        add((3, 1.30, 6, 0))
        for s in range(8, n - 2):
            c = self.curve[s]
            # chevrons on the outside of sharp bends
            if abs(c) >= 26 and s % 5 == 0:
                if c > 0:
                    add((s, -1.42, 5, 0))
                else:
                    add((s, 1.42, 4, 0))
                continue
            e = self.env
            if e == 0:     # neon city
                if s % 8 == 0:
                    add((s, -1.32, 0, 0))
                    add((s, 1.32, 0, 0))
                elif s % 8 == 4 and rng.rand() < 0.55 * dens:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(1.75, 2.2), 2, 1 if side < 0 else 0))
                if s % 90 == 45:
                    side = -1 if (s // 90) % 2 else 1
                    add((s, side * 2.0, 1, 0))
                if s % 24 == 12 and rng.rand() < 0.6:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(3.3, 4.6), 3, 0))
            elif e == 1:   # sunset coast: land on the right, beach on the left
                if s % 6 == 0 and rng.rand() < 0.85 * dens:
                    add((s, rng.uniform(1.4, 2.3), 0, 0))
                if s % 6 == 3 and rng.rand() < 0.35 * dens:
                    add((s, -rng.uniform(1.5, 2.0), 0, 1))
                if s % 46 == 20:
                    add((s, -rng.uniform(1.8, 2.6), 3, 0))
                if s % 130 == 70:
                    add((s, 2.1, 1, 0))
                if s % 400 == 200:
                    add((s, -3.6, 2, 0))
            elif e == 2:   # desert canyon
                if s % 5 == 0 and rng.rand() < 0.5 * dens:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(1.4, 3.2), 0, 0))
                if s % 17 == 8 and rng.rand() < 0.5:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(1.7, 3.0), 1, 0))
                if s % 29 == 14 and rng.rand() < 0.5:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(1.6, 2.8), 2, 1 if side < 0 else 0))
                if s % 220 == 110:
                    add((s, 2.0 if (s // 220) % 2 else -2.0, 3, 0))
            else:          # snowy pass
                if s % 4 == 0 and rng.rand() < 0.7 * dens:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(1.4, 2.4), 0, 0))
                if s % 11 == 5 and rng.rand() < 0.6 * dens:
                    side = -1 if rng.rand() < 0.5 else 1
                    add((s, side * rng.uniform(2.2, 3.6), 1, 0))
                if s % 70 == 35:
                    add((s, (1 if (s // 70) % 2 else -1) * 1.6, 2, 0))
                if s % 260 == 130:
                    add((s, (-1 if (s // 260) % 2 else 1) * 2.8, 3, 0))
        self.sprites.sort(key=lambda t: (t[0], t[1]))

    # ------------------------------------------------------------ minimap
    def layout(self):
        tot = sum(self.curve)
        k = (2 * math.pi / tot) if abs(tot) > 200 else 0.0006
        hx, hy, h = 0.0, 0.0, -math.pi / 2
        pts = []
        for s in range(self.n):
            pts.append((hx, hy))
            h += self.curve[s] * k
            hx += math.cos(h)
            hy += math.sin(h)
        ex, ey = hx - pts[0][0], hy - pts[0][1]
        pts = [(x - ex * i / self.n, y - ey * i / self.n) for i, (x, y) in enumerate(pts)]
        return pts

    def minimap(self, size=40, margin=4):
        pts = self.layout()
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        sc = (size - 2 * margin) / max(max(xs) - min(xs), max(ys) - min(ys))
        ox = (size - (max(xs) - min(xs)) * sc) / 2
        oy = (size - (max(ys) - min(ys)) * sc) / 2
        return [((x - min(xs)) * sc + ox, (y - min(ys)) * sc + oy) for x, y in pts]


R = 1  # shorthand for readability in layouts below

TRACKS = [
    Track("DOWNTOWN LOOP", 0, 3, [
        (20, 120, 20, 0, 0),
        (12, 40, 12, 38, 0),
        (20, 160, 20, 0, 6),
        (10, 30, 10, -34, -6),
        (16, 90, 16, 0, 0),
        (12, 46, 12, 40, 4),
        (16, 60, 16, 0, -4),
        (14, 50, 14, 22, 0),
        (14, 50, 14, -22, 0),
        (20, 140, 20, 0, 8),
        (12, 40, 12, 38, -8),
        (20, 110, 20, 0, 0),
        (12, 36, 12, 42, 0),
        (16, 80, 16, 0, 3),
        (10, 26, 10, -30, -3),
        (12, 40, 12, 36, 0),
        (20, 160, 20, 0, 0),
    ], 0x1001),
    Track("HARBOR EXPRESS", 0, 3, [
        (20, 160, 20, 0, 0),
        (40, 120, 40, 16, 0),
        (20, 60, 20, 0, 18),
        (20, 100, 20, 0, -18),
        (30, 90, 30, -20, 0),
        (20, 120, 20, 0, 4),
        (30, 100, 30, 26, -4),
        (20, 60, 20, 0, 0),
        (30, 80, 30, 18, 6),
        (20, 80, 20, -12, -6),
        (30, 120, 30, 22, 0),
        (20, 180, 20, 0, 0),
        (30, 80, 30, 20, 0),
        (20, 120, 20, 0, 0),
    ], 0x1002),
    Track("PALM BOULEVARD", 1, 3, [
        (20, 140, 20, 0, 0),
        (40, 100, 40, 14, 4),
        (30, 80, 30, 0, -4),
        (40, 80, 40, -16, 6),
        (30, 100, 30, 0, -6),
        (40, 120, 40, 20, 0),
        (20, 80, 20, 0, 5),
        (30, 60, 30, 18, -5),
        (30, 60, 30, -14, 0),
        (40, 120, 40, 0, 0),
        (40, 90, 40, 24, 3),
        (20, 140, 20, 0, -3),
        (30, 80, 30, 16, 0),
        (20, 100, 20, 0, 0),
    ], 0x2001),
    Track("CLIFFSIDE RUN", 1, 3, [
        (20, 120, 20, 0, 0),
        (20, 40, 20, 30, 10),
        (20, 40, 20, -30, 0),
        (20, 60, 20, 26, -10),
        (30, 100, 30, 0, 14),
        (20, 50, 20, -36, 0),
        (30, 80, 30, 0, -14),
        (20, 40, 20, 32, 0),
        (20, 40, 20, -24, 6),
        (20, 60, 20, 34, -6),
        (40, 120, 40, 0, 0),
        (20, 50, 20, 30, 8),
        (20, 60, 20, 0, -8),
        (20, 60, 20, 28, 0),
        (20, 100, 20, 0, 0),
    ], 0x2002),
    Track("RED MESA", 2, 3, [
        (20, 220, 20, 0, 0),
        (40, 120, 40, 20, 0),
        (20, 120, 20, 0, -10),
        (20, 80, 20, 0, 10),
        (40, 120, 40, -18, 0),
        (20, 200, 20, 0, 6),
        (40, 120, 40, 26, -6),
        (20, 80, 20, 0, -8),
        (20, 80, 20, 0, 8),
        (40, 100, 40, 22, 0),
        (20, 220, 20, 0, 0),
        (40, 80, 40, 16, 0),
    ], 0x3001),
    Track("DUST DEVIL", 2, 3, [
        (20, 100, 20, 0, 0),
        (16, 40, 16, 34, 4),
        (16, 40, 16, -30, -4),
        (20, 80, 20, 0, 9),
        (16, 50, 16, 38, -9),
        (20, 70, 20, 0, 0),
        (16, 40, 16, -36, 6),
        (16, 40, 16, 32, -6),
        (20, 120, 20, 0, -7),
        (20, 60, 20, 30, 7),
        (16, 40, 16, -26, 0),
        (20, 60, 20, 36, 0),
        (20, 140, 20, 0, 0),
        (16, 40, 16, 30, 0),
        (20, 80, 20, 0, 0),
    ], 0x3002),
    Track("GLACIER ROAD", 3, 3, [
        (20, 120, 20, 0, 0),
        (30, 80, 30, 22, 10),
        (20, 60, 20, 0, 6),
        (30, 60, 30, -30, 0),
        (20, 80, 20, 0, -12),
        (30, 60, 30, 34, -4),
        (20, 100, 20, 0, 8),
        (30, 60, 30, -28, 0),
        (20, 60, 20, 30, -8),
        (30, 120, 30, 0, 0),
        (30, 80, 30, 26, 6),
        (20, 80, 20, 0, -6),
        (30, 60, 30, 24, 0),
        (20, 100, 20, 0, 0),
    ], 0x4001),
    Track("SUMMIT RUSH", 3, 3, [
        (20, 100, 20, 0, 0),
        (20, 40, 20, 38, 12),
        (20, 40, 20, -38, 8),
        (20, 60, 20, 0, -14),
        (20, 40, 20, 40, -6),
        (20, 80, 20, 0, 16),
        (20, 40, 20, -34, 0),
        (20, 40, 20, 36, -16),
        (20, 100, 20, 0, 10),
        (20, 40, 20, 40, -10),
        (20, 40, 20, -30, 0),
        (20, 60, 20, 38, 0),
        (20, 120, 20, 0, 0),
        (20, 50, 20, 34, 0),
        (20, 80, 20, 0, 0),
    ], 0x4002),
]
