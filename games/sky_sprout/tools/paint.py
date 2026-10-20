"""Procedural pixel-art painting helpers for Sky Sprout (original code).

Shapes are filled with palette *ramps* (lists of indices, dark -> light) using a
simple top-left light model, optional ordered dithering, and selective outlines.
"""
import math
from pix import Layer

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
_L = (-0.52, -0.62, 0.58)
_n = math.sqrt(sum(c * c for c in _L))
LIGHT = tuple(c / _n for c in _L)


def clamp(v, a=0.0, b=1.0):
    return a if v < a else (b if v > b else v)


def pick(v, ramp, x, y, dither=False):
    """Maps v in [0,1] onto a ramp (dark->light), optionally Bayer-dithered."""
    v = clamp(v)
    f = v * (len(ramp) - 1)
    i = int(f)
    fr = f - i
    if dither:
        if fr > (BAYER[y & 3][x & 3] + 0.5) / 16.0:
            i += 1
    elif fr >= 0.5:
        i += 1
    return ramp[min(i, len(ramp) - 1)]


def lambert(nx, ny, nz, amb=0.2, light=LIGHT):
    d = nx * light[0] + ny * light[1] + nz * light[2]
    return amb + (1.0 - amb) * max(0.0, d)


def ellipse(L, cx, cy, rx, ry, ramp, dither=False, amb=0.22, gamma=1.0, only=None, flat=None):
    """Shaded ellipsoid. `only(x,y)` can clip; `flat` paints a single colour."""
    x0, x1 = int(math.floor(cx - rx - 1)), int(math.ceil(cx + rx + 1))
    y0, y1 = int(math.floor(cy - ry - 1)), int(math.ceil(cy + ry + 1))
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            nx = (x + 0.5 - cx) / rx
            ny = (y + 0.5 - cy) / ry
            r2 = nx * nx + ny * ny
            if r2 > 1.0:
                continue
            if only and not only(x, y):
                continue
            if flat is not None:
                L.set(x, y, flat)
                continue
            nz = math.sqrt(max(0.0, 1.0 - r2))
            v = lambert(nx, ny, nz, amb) ** gamma
            L.set(x, y, pick(v, ramp, x, y, dither))
    return L


def blobs(L, circles, ramp, dither=False, amb=0.25, flat=None, squash=1.0):
    """Union of shaded circles (cx, cy, r); each pixel shaded by its most-interior circle."""
    if not circles:
        return L
    x0 = int(min(c[0] - c[2] for c in circles)) - 1
    x1 = int(max(c[0] + c[2] for c in circles)) + 1
    y0 = int(min(c[1] - c[2] * squash for c in circles)) - 1
    y1 = int(max(c[1] + c[2] * squash for c in circles)) + 1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            best = None
            for cx, cy, r in circles:
                nx = (x + 0.5 - cx) / r
                ny = (y + 0.5 - cy) / (r * squash)
                r2 = nx * nx + ny * ny
                if r2 <= 1.0 and (best is None or r2 < best[0] or ny < best[2] - 0.6):
                    best = (r2, nx, ny)
            if best is None:
                continue
            if flat is not None:
                L.set(x, y, flat)
                continue
            r2, nx, ny = best
            nz = math.sqrt(max(0.0, 1.0 - r2))
            L.set(x, y, pick(lambert(nx, ny, nz, amb), ramp, x, y, dither))
    return L


def in_poly(x, y, pts):
    ins = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            ins = not ins
        j = i
    return ins


def poly(L, pts, col=None, grad=None, ramp=None, dither=False):
    """Filled polygon: flat `col`, or vertical gradient over `ramp` (grad=(top_v, bottom_v))."""
    ys = [p[1] for p in pts]
    xs = [p[0] for p in pts]
    y0, y1 = int(min(ys)), int(max(ys)) + 1
    x0, x1 = int(min(xs)), int(max(xs)) + 1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if in_poly(x + 0.5, y + 0.5, pts):
                if ramp:
                    t = (y - y0) / max(1, (y1 - y0))
                    v = grad[0] + (grad[1] - grad[0]) * t
                    L.set(x, y, pick(v, ramp, x, y, dither))
                else:
                    L.set(x, y, col)
    return L


def rect(L, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            L.set(x, y, c)
    return L


def bevel(L, x0, y0, x1, y1, dark, mid, light, hi=None):
    rect(L, x0, y0, x1, y1, mid)
    for x in range(x0, x1 + 1):
        L.set(x, y0, light)
        L.set(x, y1, dark)
    for y in range(y0, y1 + 1):
        L.set(x0, y, light)
        L.set(x1, y, dark)
    if hi is not None:
        L.set(x0, y0, hi)
    return L


def line(L, x0, y0, x1, y1, c, w=1):
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        for oy in range(w):
            for ox in range(w):
                L.set(x0 + ox, y0 + oy, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
    return L


def disc(L, cx, cy, r, c):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                L.set(x, y, c)
    return L


def ring(L, cx, cy, r0, r1, c):
    for y in range(int(cy - r1 - 1), int(cy + r1 + 2)):
        for x in range(int(cx - r1 - 1), int(cx + r1 + 2)):
            d = (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2
            if r0 * r0 <= d <= r1 * r1:
                L.set(x, y, c)
    return L


def outline_sel(L, default, mapping=None, diag=False, inner=None):
    """Adds a 1px outline; colour = mapping[neighbour] if given, else default."""
    mapping = mapping or {}
    add = []
    nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diag:
        nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
    for y in range(L.h):
        for x in range(L.w):
            if L.px[y][x]:
                continue
            col = None
            for dx, dy in nb:
                n = L.get(x + dx, y + dy)
                if n and n != default and n not in mapping.values():
                    col = mapping.get(n, default)
                    break
            if col is not None:
                add.append((x, y, col))
    for x, y, c in add:
        L.px[y][x] = c
    return L


def rim(L, cols, newc, dx, dy):
    """Recolours pixels in `cols` whose neighbour at (dx,dy) is empty or outline-ish."""
    ch = []
    for y in range(L.h):
        for x in range(L.w):
            if L.px[y][x] in cols and L.get(x + dx, y + dy) == 0:
                ch.append((x, y))
    for x, y in ch:
        L.px[y][x] = newc
    return L


def recolor(L, mapping, only=None):
    for y in range(L.h):
        for x in range(L.w):
            c = L.px[y][x]
            if c in mapping and (only is None or only(x, y)):
                L.px[y][x] = mapping[c]
    return L


def mirror_x(L, cx2):
    """Mirrors the left half onto the right around x = cx2/2 (cx2 = 2*centre)."""
    for y in range(L.h):
        for x in range(L.w):
            mx = cx2 - 1 - x
            if 0 <= mx < L.w and L.px[y][x] and not L.px[y][mx]:
                L.px[y][mx] = L.px[y][x]
    return L


def vflip(L):
    n = Layer(L.w, L.h)
    n.px = [row[:] for row in L.px[::-1]]
    return n


def rotate(L, ang, cx=None, cy=None):
    """Nearest-neighbour rotation (radians) about the centre."""
    cx = L.w / 2.0 if cx is None else cx
    cy = L.h / 2.0 if cy is None else cy
    n = Layer(L.w, L.h)
    ca, sa = math.cos(-ang), math.sin(-ang)
    for y in range(L.h):
        for x in range(L.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            sx = ca * dx - sa * dy + cx
            sy = sa * dx + ca * dy + cy
            n.px[y][x] = L.get(int(math.floor(sx)), int(math.floor(sy)))
    return n


def to_image(layer, pal, scale=1, bg=None):
    from PIL import Image
    im = Image.new("RGB", (layer.w, layer.h), bg or (40, 40, 60))
    for y in range(layer.h):
        for x in range(layer.w):
            c = layer.px[y][x]
            if c:
                im.putpixel((x, y), pal[c])
    if scale != 1:
        im = im.resize((layer.w * scale, layer.h * scale), Image.NEAREST)
    return im
