"""Tiny indexed-colour pixel canvas helpers used by the Mochi Hop asset generator.

Everything here is original code; all art is drawn procedurally or from ASCII
stamps defined in gen_assets.py.
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

    def fill_fn(self, fn):
        """fn(x, y) -> colour or None (keeps existing)."""
        for y in range(self.h):
            for x in range(self.w):
                c = fn(x, y)
                if c is not None:
                    self.px[y][x] = c

    def outline(self, c, diag=False):
        """Adds a 1px outline of colour c around all opaque pixels."""
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x]:
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
                if diag:
                    nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
                for dx, dy in nb:
                    if self.get(x + dx, y + dy) not in (0, c):
                        add.append((x, y))
                        break
        for x, y in add:
            self.px[y][x] = c
        return self

    def stamp(self, art, ox, oy, legend, flip=False):
        rows = [r for r in art.strip("\n").split("\n")]
        for y, row in enumerate(rows):
            if flip:
                row = row[::-1]
            for x, ch in enumerate(row):
                if ch in legend and legend[ch] is not None:
                    self.set(ox + x, oy + y, legend[ch])
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


def rgb555(c):
    r, g, b = c
    return (r >> 3) | ((g >> 3) << 5) | ((b >> 3) << 10)


def hexrgb(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def pal16(hexes):
    cols = [hexrgb(h) for h in hexes]
    while len(cols) < 16:
        cols.append((255, 0, 255))
    return cols[:16]


def tile_bytes(layer, tx, ty):
    """4bpp tile (32 bytes) at tile coords tx,ty."""
    out = bytearray()
    for y in range(8):
        for x in range(0, 8, 2):
            a = layer.get(tx * 8 + x, ty * 8 + y) & 15
            b = layer.get(tx * 8 + x + 1, ty * 8 + y) & 15
            out.append(a | (b << 4))
    return bytes(out)


def sprite_tiles(layer):
    """1D-mapped OBJ tiles in row-major tile order for a w x h sprite."""
    out = bytearray()
    for ty in range(layer.h // 8):
        for tx in range(layer.w // 8):
            out += tile_bytes(layer, tx, ty)
    return bytes(out)


def _flip_tile(t, h, v):
    # t is a list of 64 indices
    rows = [t[i * 8:(i + 1) * 8] for i in range(8)]
    if h:
        rows = [r[::-1] for r in rows]
    if v:
        rows = rows[::-1]
    return tuple(sum(rows, []))


class TileBank:
    """Deduplicating BG tile bank (with h/v flip matching)."""

    def __init__(self, base=0, reserve_blank=True):
        self.tiles = []        # list of tuples of 64 indices
        self.lookup = {}       # tuple -> (index, hflip, vflip)
        self.base = base
        if reserve_blank:
            self.add(tuple([0] * 64))

    def add(self, t):
        if t in self.lookup:
            return self.lookup[t]
        idx = len(self.tiles)
        self.tiles.append(t)
        for h in (0, 1):
            for v in (0, 1):
                ft = _flip_tile(list(t), h, v)
                if ft not in self.lookup:
                    self.lookup[ft] = (idx, h, v)
        return (idx, 0, 0)

    def map_layer(self, layer, palbank):
        """Converts a layer (multiple of 8 in size) into map entries (row-major)."""
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
