"""Shared procedural pixel-art helpers for Mochi Pop (original code).

Shapes are described by inside(x, y) predicates in pixel space; shading uses a
light source from the top-left, ordered dithering on band edges and coloured
("sel-out") outlines so the art reads like hand-placed pixels.
"""
import math
from pix import Layer

BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def bayer(x, y):
    return (BAYER4[y & 3][x & 3] + 0.5) / 16.0


def superellipse(cx, cy, a, bt, bb=None, p=2.4):
    """inside predicate for a rounded blob; bt/bb = top/bottom radius."""
    if bb is None:
        bb = bt

    def f(x, y):
        dx = (x + 0.5 - cx) / a
        yy = y + 0.5 - cy
        dy = yy / (bt if yy < 0 else bb)
        return abs(dx) ** p + abs(dy) ** p <= 1.0
    return f


def ellipse(cx, cy, rx, ry):
    def f(x, y):
        dx = (x + 0.5 - cx) / rx
        dy = (y + 0.5 - cy) / ry
        return dx * dx + dy * dy <= 1.0
    return f


def rot_ellipse(cx, cy, rx, ry, ang):
    ca, sa = math.cos(ang), math.sin(ang)

    def f(x, y):
        px, py = x + 0.5 - cx, y + 0.5 - cy
        u = (px * ca + py * sa) / rx
        v = (-px * sa + py * ca) / ry
        return u * u + v * v <= 1.0
    return f


def capsule(x0, y0, x1, y1, r):
    def f(x, y):
        px, py = x + 0.5, y + 0.5
        vx, vy = x1 - x0, y1 - y0
        ll = vx * vx + vy * vy
        t = 0 if ll == 0 else max(0.0, min(1.0, ((px - x0) * vx + (py - y0) * vy) / ll))
        qx, qy = x0 + vx * t - px, y0 + vy * t - py
        return qx * qx + qy * qy <= r * r
    return f


def poly(pts):
    def f(x, y):
        px, py = x + 0.5, y + 0.5
        ins = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi + 1e-9) + xi:
                ins = not ins
            j = i
        return ins
    return f


def union(*fs):
    return lambda x, y: any(f(x, y) for f in fs)


def minus(a, b):
    return lambda x, y: a(x, y) and not b(x, y)


def intersect(a, b):
    return lambda x, y: a(x, y) and b(x, y)


def mask_of(w, h, f):
    return [[1 if f(x, y) else 0 for x in range(w)] for y in range(h)]


def shade_fill(L, f, ramp, cx, cy, r, lx=-0.42, ly=-0.48, bands=(0.32, 0.62, 0.95), dither=True,
               rim=True, ox=0, oy=0, w=None, h=None):
    """Fills shape f on layer L with a radial light ramp.

    ramp = (deep, shade, base, light, highlight) palette indices (any may repeat).
    The light centre sits at (cx + lx*r, cy + ly*r); distance bands pick the ramp
    entry, Bayer dithering softens band edges. rim=True darkens the bottom-right edge.
    """
    W = w or L.w
    H = h or L.h
    deep, shade, base, light, hi = ramp
    lcx, lcy = cx + lx * r, cy + ly * r
    m = mask_of(W, H, f)

    def ins(x, y):
        return 0 <= x < W and 0 <= y < H and m[y][x]
    for y in range(H):
        for x in range(W):
            if not m[y][x]:
                continue
            d = math.hypot(x + 0.5 - lcx, y + 0.5 - lcy) / (r * 1.45)
            t = d + ((bayer(x, y) - 0.5) * 0.16 if dither else 0)
            if t < bands[0] * 0.55:
                c = hi
            elif t < bands[0]:
                c = light
            elif t < bands[1]:
                c = base
            elif t < bands[2]:
                c = shade
            else:
                c = deep
            if rim and (not ins(x + 1, y + 1) or not ins(x, y + 1)) and c not in (hi,):
                c = deep if c in (shade, deep) else shade
            L.set(ox + x, oy + y, c)
    return m


def outline_sel(L, mapping, default, diag=False):
    """Coloured outline: each transparent pixel touching art takes mapping[neighbour]."""
    add = []
    nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diag:
        nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
    for y in range(L.h):
        for x in range(L.w):
            if L.px[y][x]:
                continue
            for dx, dy in nb:
                v = L.get(x + dx, y + dy)
                if v and v not in mapping.values() or (v and v in mapping and mapping[v] != v):
                    add.append((x, y, mapping.get(v, default)))
                    break
    for x, y, c in add:
        L.px[y][x] = c
    return L


def outline(L, c, diag=False, only_inside=None):
    return L.outline(c, diag)


def stamp(L, art, ox, oy, legend, flip=False):
    rows = art.strip("\n").split("\n")
    for y, row in enumerate(rows):
        if flip:
            row = row[::-1]
        for x, ch in enumerate(row):
            v = legend.get(ch)
            if v is not None:
                L.set(ox + x, oy + y, v)
    return L


def fill(L, f, c, ox=0, oy=0, w=None, h=None):
    W = w or L.w
    H = h or L.h
    for y in range(H):
        for x in range(W):
            if f(x, y):
                L.set(ox + x, oy + y, c)


def to_image(L, pal, scale=1, bg=None):
    from PIL import Image
    im = Image.new("RGB", (L.w, L.h), bg if bg else (40, 30, 50))
    for y in range(L.h):
        for x in range(L.w):
            c = L.px[y][x]
            if c:
                im.putpixel((x, y), pal[c & 255] if isinstance(pal, (list, tuple)) else pal(c))
    if scale != 1:
        im = im.resize((L.w * scale, L.h * scale), Image.NEAREST)
    return im
