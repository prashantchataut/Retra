"""Indexed-colour pixel canvas helpers for the Turbo Drift asset generator.

Original code (layout follows the Mochi Hop tools). All art is procedural.
"""


class Layer:
    """An indexed-colour canvas. 0 means transparent."""

    def __init__(self, w, h, fill=0):
        self.w = w
        self.h = h
        self.px = [[fill] * w for _ in range(h)]

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return 0

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(max(0, y0), min(self.h, y1)):
            row = self.px[y]
            for x in range(max(0, x0), min(self.w, x1)):
                row[x] = c
        return self

    def fill_fn(self, fn):
        for y in range(self.h):
            for x in range(self.w):
                c = fn(x, y)
                if c is not None:
                    self.px[y][x] = c

    def outline(self, c, diag=False, skip=None):
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x]:
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
                if diag:
                    nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
                for dx, dy in nb:
                    v = self.get(x + dx, y + dy)
                    if v not in (0, c) and (skip is None or v not in skip):
                        add.append((x, y))
                        break
        for x, y in add:
            self.px[y][x] = c
        return self

    def paste(self, other, ox=0, oy=0):
        for y in range(other.h):
            for x in range(other.w):
                c = other.px[y][x]
                if c:
                    self.set(ox + x, oy + y, c)
        return self

    def copy(self):
        l = Layer(self.w, self.h)
        l.px = [row[:] for row in self.px]
        return l

    def hflip(self):
        l = Layer(self.w, self.h)
        l.px = [row[::-1] for row in self.px]
        return l

    def crop(self, x0, y0, w, h):
        l = Layer(w, h)
        for y in range(h):
            for x in range(w):
                l.px[y][x] = self.get(x0 + x, y0 + y)
        return l

    def remap(self, table):
        l = Layer(self.w, self.h)
        l.px = [[table.get(c, c) for c in row] for row in self.px]
        return l

    def bbox(self):
        xs = [x for y in range(self.h) for x in range(self.w) if self.px[y][x]]
        ys = [y for y in range(self.h) for x in range(self.w) if self.px[y][x]]
        if not xs:
            return (0, 0, 0, 0)
        return (min(xs), min(ys), max(xs) + 1, max(ys) + 1)


def rgb555(c):
    r, g, b = c
    return (r >> 3) | ((g >> 3) << 5) | ((b >> 3) << 10)


def hexrgb(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def pal16(hexes):
    cols = [hexrgb(h) if isinstance(h, str) else h for h in hexes]
    while len(cols) < 16:
        cols.append((255, 0, 255))
    return cols[:16]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def tile_bytes(layer, tx, ty):
    """4bpp tile (32 bytes) at tile coords tx,ty."""
    out = bytearray()
    for y in range(8):
        for x in range(0, 8, 2):
            a = layer.get(tx * 8 + x, ty * 8 + y) & 15
            b = layer.get(tx * 8 + x + 1, ty * 8 + y) & 15
            out.append(a | (b << 4))
    return bytes(out)


def tile_bytes8(layer, tx, ty):
    """8bpp tile (64 bytes)."""
    out = bytearray()
    for y in range(8):
        for x in range(8):
            out.append(layer.get(tx * 8 + x, ty * 8 + y) & 255)
    return bytes(out)


def sprite_tiles(layer):
    """1D-mapped OBJ tiles in row-major tile order for a w x h sprite."""
    out = bytearray()
    for ty in range(layer.h // 8):
        for tx in range(layer.w // 8):
            out += tile_bytes(layer, tx, ty)
    return bytes(out)


def _flip_tile(t, h, v):
    rows = [t[i * 8:(i + 1) * 8] for i in range(8)]
    if h:
        rows = [r[::-1] for r in rows]
    if v:
        rows = rows[::-1]
    return tuple(sum(rows, []))


class TileBank:
    """Deduplicating 4bpp BG tile bank (with h/v flip matching)."""

    def __init__(self, base=0, reserve_blank=True, flips=True):
        self.tiles = []
        self.lookup = {}
        self.base = base
        self.flips = flips
        if reserve_blank:
            self.add(tuple([0] * 64))

    def add(self, t):
        if t in self.lookup:
            return self.lookup[t]
        idx = len(self.tiles)
        self.tiles.append(t)
        for h in ((0, 1) if self.flips else (0,)):
            for v in ((0, 1) if self.flips else (0,)):
                ft = _flip_tile(list(t), h, v)
                if ft not in self.lookup:
                    self.lookup[ft] = (idx, h, v)
        return (idx, 0, 0)

    def map_layer(self, layer, palbank):
        tw, th = layer.w // 8, layer.h // 8
        ents = []
        for ty in range(th):
            for tx in range(tw):
                t = tuple(layer.get(tx * 8 + x, ty * 8 + y) & 15
                          for y in range(8) for x in range(8))
                idx, h, v = self.add(t)
                ents.append(((idx + self.base) & 0x3FF) | (h << 10) | (v << 11) | (palbank << 12))
        return tw, th, ents

    def data(self):
        out = bytearray()
        for t in self.tiles:
            for i in range(0, 64, 2):
                out.append((t[i] & 15) | ((t[i + 1] & 15) << 4))
        return bytes(out)


def downscale(layer, f, weights=None, cover=0.42):
    """Palette-aware box downscale by integer factor f (mode filter)."""
    w, h = layer.w // f, layer.h // f
    out = Layer(w, h)
    for y in range(h):
        for x in range(w):
            cnt = {}
            n = 0
            for yy in range(f):
                row = layer.px[y * f + yy]
                for xx in range(f):
                    c = row[x * f + xx]
                    if c:
                        n += 1
                        cnt[c] = cnt.get(c, 0) + (weights.get(c, 1) if weights else 1)
            if n >= f * f * cover and cnt:
                out.px[y][x] = max(sorted(cnt), key=lambda k: cnt[k])
    return out


def resample(layer, w, h, cover=0.42, weights=None):
    """Nearest/mode resample of a layer to an arbitrary size (used for mips)."""
    out = Layer(w, h)
    sx = layer.w / w
    sy = layer.h / h
    for y in range(h):
        for x in range(w):
            x0, x1 = int(x * sx), max(int(x * sx) + 1, int((x + 1) * sx))
            y0, y1 = int(y * sy), max(int(y * sy) + 1, int((y + 1) * sy))
            cnt = {}
            n = 0
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    c = layer.get(xx, yy)
                    if c:
                        n += 1
                        cnt[c] = cnt.get(c, 0) + (weights.get(c, 1) if weights else 1)
            tot = (x1 - x0) * (y1 - y0)
            if cnt and n >= tot * cover:
                out.px[y][x] = max(sorted(cnt), key=lambda k: cnt[k])
    return out


def to_image(layer, pal, scale=1, bg=None):
    from PIL import Image
    im = Image.new("RGB", (layer.w, layer.h), bg or (40, 40, 48))
    for y in range(layer.h):
        for x in range(layer.w):
            c = layer.px[y][x]
            if c:
                im.putpixel((x, y), tuple(pal[c]))
    if scale != 1:
        im = im.resize((layer.w * scale, layer.h * scale), Image.NEAREST)
    return im
