"""Original environment art for Turbo Drift: road textures, sky gradients,
parallax skylines and roadside scenery for the four locations."""
import math

from pix import Layer, pal16, hexrgb, mix, resample
from draw import poly, ellipse, thick_line, dither, ramp_pick, Rng

FOG_LEVELS = 8
FOG_AMT = [0.0, 0.07, 0.15, 0.24, 0.34, 0.46, 0.58, 0.70]

# texture cross-section (256 texels): rumble | road (3 lanes) | rumble
TEX_W = 256
RUMBLE_W = 14
EDGE = (18, 21)
LANES = ((89, 92), (164, 167))
VARIANTS = 4          # 0 light band, 1 dark band, 2/3 start-line checker

ENVS = [
    {
        "key": "city", "name": "NEON CITY", "time": "MIDNIGHT",
        "sky": [(0.0, "ff6a9e"), (0.10, "e0488e"), (0.28, "7a2a8a"), (0.55, "2c1460"), (1.0, "070820")],
        "fog": "b0488a",
        "ground": ("2e2848", "272140"),
        "road": ("403e5c", "383652"), "lane": "e6dcff", "edge": "f4eeff",
        "rumble": ("ff3cac", "28e4ff"), "check": ("15121f", "f4f0ff"),
    },
    {
        "key": "coast", "name": "SUNSET COAST", "time": "DUSK",
        "sky": [(0.0, "ffe08a"), (0.10, "ffb060"), (0.26, "ff7a6a"), (0.5, "c04a8e"), (1.0, "3a2a8a")],
        "fog": "ffb87a",
        "ground": ("f0c890", "e6b880"),
        "road": ("5e5470", "564c68"), "lane": "fff4dc", "edge": "fff8ec",
        "rumble": ("ee3a46", "fff4ec"), "check": ("1a1622", "fffaf0"),
    },
    {
        "key": "desert", "name": "DESERT CANYON", "time": "AFTERNOON",
        "sky": [(0.0, "fbe6bc"), (0.14, "c8dcec"), (0.45, "6aa4e8"), (1.0, "2a5cc0")],
        "fog": "f2dcc0",
        "ground": ("e4a464", "d89656"),
        "road": ("6e5e5c", "665654"), "lane": "fff6e4", "edge": "fff6e4",
        "rumble": ("ffd02c", "3a302e"), "check": ("1c1818", "fff8f0"),
    },
    {
        "key": "snow", "name": "SNOWY PASS", "time": "MORNING",
        "sky": [(0.0, "eef4ff"), (0.18, "c4dcfa"), (0.5, "7aa8ec"), (1.0, "3a6ad0")],
        "fog": "e8f0ff",
        "ground": ("f4f8ff", "e2eaf8"),
        "road": ("727a92", "6a728a"), "lane": "ffffff", "edge": "ffffff",
        "rumble": ("e02c40", "f8f8ff"), "check": ("1a1c26", "ffffff"),
    },
]


def fogged(c, f, env):
    return mix(hexrgb(c) if isinstance(c, str) else c, hexrgb(env["fog"]), FOG_AMT[f])


def sky_table(env, n=128):
    stops = [(t, hexrgb(c)) for t, c in env["sky"]]
    out = []
    for i in range(n):
        t = min(1.0, i / 96.0)
        for k in range(len(stops) - 1):
            if stops[k][0] <= t <= stops[k + 1][0]:
                u = (t - stops[k][0]) / (stops[k + 1][0] - stops[k][0])
                out.append(mix(stops[k][1], stops[k + 1][1], u))
                break
        else:
            out.append(stops[-1][1])
    return out


def ground_table(env):
    """[band][fog] backdrop colours."""
    return [[fogged(env["ground"][b], f, env) for f in range(FOG_LEVELS)] for b in range(2)]


def road_texture(env):
    """Returns (rows, palette) where rows[r] is a list of 256 palette indices
    (row r = fog * VARIANTS + variant) and palette maps index -> rgb (1..)."""
    pal = {}
    order = []

    def idx(rgb):
        if rgb not in pal:
            pal[rgb] = len(order) + 1
            order.append(rgb)
        return pal[rgb]

    rows = []
    for f in range(FOG_LEVELS):
        for v in range(VARIANTS):
            band = 0 if v in (0, 2) else 1
            asph = fogged(env["road"][band], f, env)
            lane = fogged(env["lane"], f, env)
            edge = fogged(env["edge"], f, env)
            rum = fogged(env["rumble"][band], f, env)
            ck = [fogged(env["check"][0], f, env), fogged(env["check"][1], f, env)]
            row = []
            for x in range(TEX_W):
                if x < RUMBLE_W or x >= TEX_W - RUMBLE_W:
                    c = rum
                elif EDGE[0] <= x < EDGE[1] or TEX_W - EDGE[1] <= x < TEX_W - EDGE[0]:
                    c = edge
                elif v >= 2:
                    c = ck[((x // 16) + v) & 1]
                elif v == 0 and any(a <= x < b for a, b in LANES):
                    c = lane
                else:
                    c = asph
                row.append(idx(c))
            rows.append(row)
    assert len(order) <= 159, len(order)
    return rows, order


# ------------------------------------------------------------------ skylines
SKY_W, SKY_H = 512, 96


def periodic(rng, x, terms):
    v = 0.0
    for k, a, ph in terms:
        v += a * math.sin(2 * math.pi * k * x / SKY_W + ph)
    return v


def make_terms(rng, spec):
    return [(k, a, rng.uniform(0, 6.283)) for k, a in spec]


def skyline_city():
    rng = Rng(0xC17)
    P = pal16(["000000", "8a7ab8", "fff4ff", "3c2866", "6c4c98", "22163e", "3e2c66", "150d29",
               "ffc860", "60e0ff", "ff6ab0", "ff3ca0", "2ae8ff", "fff0f6", "e8b8d8", "ff4040"])
    lay = Layer(SKY_W, SKY_H)
    # stars
    for i in range(70):
        x, y = rng.range(0, SKY_W - 1), rng.range(0, 44)
        lay.set(x, y, 2 if rng.rand() < 0.25 else 1)
    # moon
    ellipse(lay, 92, 18, 11, 11, 13, lambda x, y, dx, dy, d: 14 if (dx + 0.35) ** 2 + (dy - 0.1) ** 2 > 0.85 else 13)
    for (cx, cy, r) in ((88, 15, 2), (97, 21, 1.5), (91, 23, 1.2)):
        ellipse(lay, cx, cy, r, r, 14)
    # far skyline: continuous blocks
    x = 0
    while x < SKY_W:
        w = rng.range(3, 7) * 4
        h = rng.range(26, 50)
        top = SKY_H - h
        for yy in range(top, SKY_H):
            for xx in range(x, min(SKY_W, x + w)):
                lay.set(xx, yy, 3)
        for yy in range(top + 3, SKY_H - 4, 4):
            for xx in range(x + 2, min(SKY_W, x + w) - 1, 3):
                if rng.rand() < 0.28:
                    lay.set(xx, yy, 4)
        if rng.rand() < 0.2:
            ax = x + w // 2
            for yy in range(top - 8, top):
                lay.set(ax, yy, 3)
            lay.set(ax, top - 9, 15)
        x += w
    # mid towers with lit windows (tile-aligned facades for good tile reuse)
    x = 0
    while x < SKY_W:
        gap = rng.range(0, 2) * 4
        x += gap
        w = rng.range(2, 4) * 8
        h = rng.range(3, 6) * 8 - 4
        top = SKY_H - h
        wc = [8, 9, 10][rng.range(0, 2)]
        style = rng.range(0, 2)
        for yy in range(top, SKY_H):
            for xx in range(x, min(SKY_W, x + w)):
                c = 5
                if xx == x:
                    c = 6
                lx, ly = (xx - x) % 8, (yy - top) % 8
                if yy > top + 2:
                    if style == 0 and lx in (2, 5) and ly in (2, 3, 6):
                        c = wc if ((xx // 8 + yy // 8) % 3) else 7
                    elif style == 1 and 1 <= lx <= 6 and ly == 4:
                        c = wc
                    elif style == 2 and lx in (2, 3) and ly in (2, 3):
                        c = wc if ((xx // 8 * 7 + yy // 8) % 4) else 5
                lay.set(xx, yy, c)
        if rng.rand() < 0.45:
            sw = min(w - 4, 20)
            sx = x + (w - sw) // 2
            nc = 11 if rng.rand() < 0.5 else 12
            for xx in range(sx, sx + sw):
                lay.set(xx, top - 6, nc)
                lay.set(xx, top - 2, nc)
            for yy in range(top - 6, top - 1):
                lay.set(sx, yy, nc)
                lay.set(sx + sw - 1, yy, nc)
            for xx in range(sx + 2, sx + sw - 2, 3):
                lay.set(xx, top - 4, nc)
        x += w
    # near low silhouettes (bridges / trees) at the base
    for xx in range(SKY_W):
        hh = 6 + int(3 + 2.5 * math.sin(xx * 0.09) + 2 * math.sin(xx * 0.23 + 1.0))
        for yy in range(SKY_H - hh, SKY_H):
            lay.set(xx, yy, 7)
    return lay, P, 40


def skyline_coast():
    rng = Rng(0xC0A5)
    P = pal16(["000000", "ffd6b0", "f49aa8", "b45a98", "fff6b0", "ffc65a", "ff7a5a", "3c2a78",
               "6a3a92", "ffd080", "8a3a88", "5a2470", "2c1444", "ff9a7a", "e0607e", "ffffff"])
    lay = Layer(SKY_W, SKY_H)
    sea = 84
    # sun with synthwave slits
    cx, cy, r = 300, 70, 30

    def sunfn(x, y, dx, dy, d):
        if y >= sea:
            return None
        rel = (y - (cy - r)) / (2.0 * r)
        if rel > 0.5:
            k = (y - cy) / r
            period = 7 - int(k * 3)
            if (y - cy) % max(3, period) < 1 + int(k * 3):
                return None
        t = (y - (cy - r)) / (2.0 * r)
        return ramp_pick([4, 5, 6], t * 1.15, x, y)
    ellipse(lay, cx, cy, r, r, 4, sunfn)
    # clouds: long soft streaks
    for i in range(18):
        cxl = rng.range(0, SKY_W - 1)
        cyl = rng.range(8, 62)
        wl = rng.range(30, 90)
        hl = rng.range(2, 5)
        for yy in range(cyl - hl, cyl + hl + 1):
            half = wl * (1.0 - ((yy - cyl) / (hl + 0.5)) ** 2) ** 0.5 / 2
            for xx in range(int(cxl - half), int(cxl + half)):
                X = xx % SKY_W
                if abs(X - cx) < r + 2 and cy - r < yy < sea:
                    continue
                t = (yy - (cyl - hl)) / (2.0 * hl + 1)
                c = 1 if t < 0.35 else 2 if t < 0.75 else 3
                if lay.get(X, yy) == 0:
                    lay.set(X, yy, c)
    # headlands / islands on the horizon
    terms = make_terms(rng, [(2, 5.0), (5, 3.0), (11, 1.6), (23, 0.8)])
    for xx in range(SKY_W):
        h = periodic(rng, xx, terms)
        hgt = int(max(0.0, h + 2.0))
        far = abs(((xx - 120) % SKY_W) - 256) > 150
        for yy in range(sea - hgt, sea):
            lay.set(xx, yy, 10 if far else 11)
    # sea with glints
    for yy in range(sea, SKY_H):
        for xx in range(SKY_W):
            c = 7 if (yy - sea) > 3 else 8
            if abs(xx - cx) < 26 - (yy - sea) * 0.5 and (xx * 3 + yy * 7) % 9 < 4:
                c = 9
            elif (xx * 5 + yy * 11) % 37 == 0:
                c = 13
            lay.set(xx, yy, c)
    return lay, P, sea


def mountains(lay, rng, base, amp, terms_spec, ramp, snow=None, snow_line=None, shade_c=None, lit_c=None):
    terms = make_terms(rng, terms_spec)
    hs = [base + periodic(rng, x, terms) * amp for x in range(SKY_W)]
    for x in range(SKY_W):
        top = int(SKY_H - hs[x])
        slope = hs[(x + 1) % SKY_W] - hs[x - 1]
        for y in range(max(0, top), SKY_H):
            depth = (y - top) / max(1.0, hs[x])
            c = ramp_pick(ramp, depth * 1.4, x, y)
            if shade_c is not None and slope > 0.6 and depth < 0.7:
                c = shade_c
            elif lit_c is not None and slope < -0.6 and depth < 0.5:
                c = lit_c
            if snow is not None and SKY_H - y > snow_line + (x * 7 % 5):
                c = snow if slope <= 0.3 else (snow_line and shade_c and c)
            lay.set(x, y, c)
    return hs


def skyline_desert():
    rng = Rng(0xDE5)
    P = pal16(["000000", "ffffff", "eef0fa", "c8cce6", "c4a6d0", "ad8ec0", "f8a070", "dc7448",
               "b24e32", "7a2e22", "ffc890", "e8b48a", "8a8a4a", "5a2a1e", "d89a70", "9a6a9a"])
    lay = Layer(SKY_W, SKY_H)
    # clouds (puffy)
    for i in range(9):
        cx, cy = rng.range(0, SKY_W - 1), rng.range(10, 32)
        for k in range(5):
            ox, oy = rng.range(-16, 16), rng.range(-3, 3)
            rr = rng.range(5, 9)
            for yy in range(cy + oy - rr, cy + oy + rr):
                for xx in range(cx + ox - rr * 2, cx + ox + rr * 2):
                    d = ((xx - cx - ox) / (rr * 2.0)) ** 2 + ((yy - cy - oy) / float(rr)) ** 2
                    if d < 1.0 and yy < cy + 4:
                        X = xx % SKY_W
                        c = 1 if yy < cy + oy - rr * 0.2 else 2 if yy < cy + 1 else 3
                        if lay.get(X, yy) in (0, 3, 2):
                            lay.set(X, yy, min(c, lay.get(X, yy) or 9))
    # far violet ranges
    mountains(lay, rng, 30, 1.0, [(3, 8), (7, 4), (13, 2), (29, 1)], [4, 5], shade_c=5, lit_c=4)
    # mesas: flat tops with vertical walls
    x = 0
    while x < SKY_W:
        w = rng.range(30, 90)
        h = rng.range(26, 50)
        top = SKY_H - h
        lean = rng.range(4, 10)
        for xx in range(x, x + w):
            X = xx % SKY_W
            e = min(xx - x, x + w - xx)
            t = top + max(0, lean - e) * 2
            for yy in range(t, SKY_H):
                strata = ((yy - top) // 5) % 3
                if xx - x < lean + 2:
                    c = 6
                elif x + w - xx < lean + 3:
                    c = 9 if strata == 0 else 8
                else:
                    c = [7, 7, 8][strata] if (yy - top) > 3 else 6
                lay.set(X, yy, c)
        x += w + rng.range(6, 40)
    # near desert floor + scrub
    terms = make_terms(rng, [(4, 2.0), (9, 1.4), (19, 0.7)])
    for xx in range(SKY_W):
        h = 6 + int(periodic(rng, xx, terms))
        for yy in range(SKY_H - h, SKY_H):
            lay.set(xx, yy, 14 if yy < SKY_H - h + 2 else 11)
        if (xx * 13) % 29 == 0:
            for yy in range(SKY_H - h - 3, SKY_H - h):
                lay.set(xx, yy, 12)
                lay.set(xx + 1, yy + 1, 12)
    return lay, P, 36


def skyline_snow():
    rng = Rng(0x5A0)
    P = pal16(["000000", "ffffff", "e6eefc", "c4d6f4", "a8bce8", "8aa2d8", "6a86c4", "4e68a8",
               "f4f8ff", "cfdcf2", "2c4a58", "3c6272", "56808e", "1e3440", "b0c4ec", "ffffff"])
    lay = Layer(SKY_W, SKY_H)
    # wispy clouds
    for i in range(10):
        cx, cy = rng.range(0, SKY_W - 1), rng.range(6, 30)
        wl, hl = rng.range(40, 100), rng.range(2, 4)
        for yy in range(cy - hl, cy + hl + 1):
            half = wl * (1.0 - ((yy - cy) / (hl + 0.5)) ** 2) ** 0.5 / 2
            for xx in range(int(cx - half), int(cx + half)):
                if dither(xx, yy, 0.85 - abs(yy - cy) / (hl * 2.0)):
                    lay.set(xx % SKY_W, yy, 2 if yy <= cy else 3)
    # far range with big snowy peaks
    terms = make_terms(rng, [(3, 14), (6, 7), (13, 3.5), (27, 1.6), (53, 0.7)])
    for x in range(SKY_W):
        h = 46 + periodic(rng, x, terms)
        top = int(SKY_H - h)
        sl = periodic(rng, x + 1, terms) - periodic(rng, x - 1, terms)
        for y in range(max(0, top), SKY_H):
            d = y - top
            if d < 10 + (x * 5 % 4):
                c = 1 if sl < -0.2 else 9 if sl < 0.6 else 14
            else:
                c = 4 if sl < 0 else 5
            lay.set(x, y, c)
    # nearer darker range
    terms = make_terms(rng, [(4, 8), (9, 4), (17, 2), (35, 1)])
    for x in range(SKY_W):
        h = 28 + periodic(rng, x, terms)
        top = int(SKY_H - h)
        sl = periodic(rng, x + 1, terms) - periodic(rng, x - 1, terms)
        for y in range(max(0, top), SKY_H):
            d = y - top
            if d < 4 + (x * 3 % 3):
                c = 8 if sl < 0.3 else 3
            else:
                c = 6 if sl < 0 else 7
            lay.set(x, y, c)
    # pine forest strip
    for x in range(SKY_W):
        tree = 4 + ((x * 7919) % 11) // 2 if (x % 5) in (1, 2, 3) else 3
        peak = 2 - abs((x % 5) - 2)
        h = 7 + peak * 2 + ((x // 5 * 31) % 4)
        for y in range(SKY_H - h, SKY_H):
            lay.set(x, y, 10 if (x % 5) < 2 else 11)
        if (x % 5) == 2:
            lay.set(x, SKY_H - h, 2)
    return lay, P, 30


SKYLINES = [skyline_city, skyline_coast, skyline_desert, skyline_snow]


# ------------------------------------------------------------------ scenery sprites
# Each object: draw(fn) at its mip0 size; mips are resampled.
SIGN_PAL = pal16(["000000", "1a1420", "d82838", "ff5a60", "f8f4ec", "c8c0c0", "5a5a6a", "8a8a9e",
                  "ffd02c", "2a2a38", "ff8a3c", "2ae8ff", "ff3cac", "40c060", "ffffff", "9a9aae"])


def obj_chevron(right):
    lay = Layer(32, 32)
    # post
    lay.rect(15, 18, 17, 32, 6)
    lay.rect(15, 18, 16, 32, 7)
    # board
    lay.rect(3, 4, 29, 19, 2)
    lay.rect(3, 4, 29, 5, 3)
    for k in range(3):
        ox = 6 + k * 7
        pts = [(ox, 6), (ox + 3, 6), (ox + 7, 11.5), (ox + 3, 17), (ox, 17), (ox + 4, 11.5)]
        if not right:
            pts = [(32 - px, py) for px, py in pts]
        poly(lay, pts, 4)
    lay.outline(1)
    return lay


def obj_start_pylon():
    lay = Layer(32, 64)
    lay.rect(14, 10, 18, 64, 6)
    lay.rect(14, 10, 15, 64, 7)
    # checkered flag panel
    for y in range(4, 22):
        for x in range(4, 28):
            lay.set(x, y, 4 if ((x // 4) + (y // 4)) % 2 else 9)
    lay.rect(4, 22, 28, 26, 2)
    lay.outline(1)
    return lay


def obj_lamp(night=True):
    """Symmetric double-arm street light (32x64)."""
    P = pal16(["000000", "120e1c", "2c2a40", "4c4a66", "7a78a0", "fff6d0", "ffd27a", "ff9a3c",
               "3a2a50", "ffffff", "e8e0ff", "ff3cac", "28e4ff", "000000", "000000", "000000"])
    lay = Layer(32, 64)
    lay.rect(15, 10, 18, 63, 2)
    lay.rect(15, 10, 16, 63, 3)
    lay.rect(13, 58, 20, 64, 2)
    lay.rect(13, 58, 15, 64, 3)
    for s in (-1, 1):
        for i in range(12):
            x = 16 + s * (i + 1)
            y = 10 - int(3.5 * math.sin(i / 11.0 * math.pi * 0.5))
            lay.set(x, y, 3)
            lay.set(x, y + 1, 2)
        hx = 16 + s * 12
        lay.rect(hx - 3, 6, hx + 3, 9, 3)
        lay.rect(hx - 2, 9, hx + 2, 10, 5)
        lay.set(hx - 3, 9, 6)
        lay.set(hx + 2, 9, 6)
    # neon ring band on the pole
    lay.rect(15, 40, 18, 42, 11 if night else 4)
    lay.outline(1)
    return lay, P


def palm(lay, base_x, base_y, height, lean, rng, trunk=(2, 3, 4), leaves=(5, 6, 7), coco=8):
    pts = []
    for i in range(height):
        t = i / float(height)
        x = base_x + lean * t * t
        pts.append((x, base_y - i))
    for i, (x, y) in enumerate(pts):
        w = 3.2 - 1.2 * i / height
        for dx in range(int(-w), int(w) + 1):
            c = trunk[0] if dx > w * 0.3 else trunk[1] if dx > -w * 0.4 else trunk[2]
            if (i % 4) == 0:
                c = trunk[0]
            lay.set(int(x + dx), int(y), c)
    tx, ty = pts[-1]
    angles = [-170, -140, -110, -70, -40, -10, 200, 160]
    for k, a in enumerate(angles):
        ang = math.radians(a + rng.uniform(-8, 8))
        length = 20 + rng.uniform(-3, 4)
        prev = (tx, ty)
        for j in range(1, 14):
            t = j / 13.0
            px = tx + math.cos(ang) * length * t
            py = ty + math.sin(ang) * length * t * 0.55 + (length * 0.55) * t * t
            wdt = 4.2 * math.sin(t * math.pi) + 0.6
            c = leaves[0] if t < 0.25 else leaves[1] if (j + k) % 3 else leaves[2]
            thick_line(lay, prev[0], prev[1], px, py, wdt, wdt * 0.9, c)
            prev = (px, py)
    for (ox, oy) in ((-2, 2), (2, 2), (0, 3)):
        lay.set(int(tx + ox), int(ty + oy), coco)
        lay.set(int(tx + ox) + 1, int(ty + oy), coco)


def obj_palm(night):
    if night:
        P = pal16(["000000", "100a1a", "4a2e4a", "6a4260", "8a5a78", "1e5a5a", "2a8a7a", "3ab89a",
                   "5a3048", "ff3cac", "000000", "000000", "000000", "000000", "000000", "000000"])
    else:
        P = pal16(["000000", "2a1a14", "7a5236", "a06e46", "c89260", "2a7a3a", "3eae4a", "7ad85a",
                   "6a4026", "000000", "000000", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    rng = Rng(0x9A1 if night else 0x9A2)
    palm(lay, 30, 63, 46, 8, rng)
    if night:
        lay.rect(26, 61, 34, 64, 9)
    lay.outline(1)
    return lay, P


def obj_billboard(kind):
    P = pal16(["000000", "120e1a", "2a2838", "4a4860", "6e6c88", "fff4ff", "ff3cac", "ff9ad2",
               "2ae8ff", "a8f4ff", "f9b3c9", "e8849f", "5a2a4e", "6fcf7a", "ffd02c", "ff7a3c"])
    lay = Layer(64, 64)
    # posts
    for px in (14, 48):
        lay.rect(px, 30, px + 3, 64, 2)
        lay.rect(px, 30, px + 1, 64, 3)
    # panel
    lay.rect(2, 4, 62, 34, 2)
    lay.rect(3, 5, 61, 33, 1)
    if kind == "mochi":
        frame, glow = 6, 7
        # neon frame
        for x in range(4, 60):
            lay.set(x, 6, frame)
            lay.set(x, 31, frame)
        for y in range(6, 32):
            lay.set(4, y, frame)
            lay.set(59, y, frame)
        # Mochi face icon
        ellipse(lay, 16, 20, 8.5, 7.5, 10, lambda x, y, dx, dy, d: 11 if dy > 0.45 or d > 0.8 else 10)
        lay.set(13, 19, 12)
        lay.set(19, 19, 12)
        lay.rect(15, 22, 18, 23, 12)
        lay.rect(15, 10, 18, 13, 13)
        lay.set(18, 10, 13)
        # "MOCHI" in neon block letters
        letters = {
            "M": ["10001", "11011", "10101", "10001", "10001"],
            "O": ["01110", "10001", "10001", "10001", "01110"],
            "C": ["01111", "10000", "10000", "10000", "01111"],
            "H": ["10001", "10001", "11111", "10001", "10001"],
            "I": ["111", "010", "010", "010", "111"],
        }
        x = 28
        for ch in "MOCHI":
            g = letters[ch]
            for gy, row in enumerate(g):
                for gx, b in enumerate(row):
                    if b == "1":
                        lay.set(x + gx, 15 + gy * 2, 8)
                        lay.set(x + gx, 16 + gy * 2, 9)
            x += len(g[0]) + 1
    elif kind == "retra":
        # sunset ad: gradient panel with a wave
        for y in range(5, 33):
            for x in range(3, 61):
                t = (y - 5) / 28.0
                lay.set(x, y, ramp_pick([14, 15, 6], t, x, y))
        ellipse(lay, 32, 24, 9, 9, 5, lambda x, y, dx, dy, d: None if y > 26 else 5)
        for x in range(3, 61):
            yy = 27 + int(1.5 * math.sin(x * 0.4))
            for y in range(yy, 33):
                lay.set(x, y, 8 if y < yy + 2 else 9 if (x + y) % 3 else 8)
        word = {"R": ["110", "101", "110", "101"], "E": ["111", "110", "100", "111"],
                "T": ["111", "010", "010", "010"], "A": ["010", "101", "111", "101"]}
        x = 19
        for ch in "RETRA":
            for gy, row in enumerate(word[ch]):
                for gx, b in enumerate(row):
                    if b == "1":
                        lay.set(x + gx, 8 + gy, 1)
            x += 6
    elif kind == "motel":
        for y in range(5, 33):
            for x in range(3, 61):
                lay.set(x, y, 12 if (x + y) % 2 else 1)
        word = {"M": ["10001", "11011", "10101", "10001"], "O": ["0110", "1001", "1001", "0110"],
                "T": ["111", "010", "010", "010"], "E": ["111", "110", "100", "111"],
                "L": ["100", "100", "100", "111"]}
        x = 10
        for ch in "MOTEL":
            g = word[ch]
            for gy, row in enumerate(g):
                for gx, b in enumerate(row):
                    if b == "1":
                        lay.rect(x + gx * 2, 10 + gy * 2, x + gx * 2 + 2, 12 + gy * 2, 15)
            x += len(g[0]) * 2 + 2
        # arrow
        poly(lay, [(10, 24), (44, 24), (44, 21), (54, 26.5), (44, 32), (44, 29), (10, 29)], 14)
    lay.outline(1)
    return lay, P


def obj_tower():
    P = pal16(["000000", "0c0818", "1e1638", "2e2450", "443670", "ffc860", "60e0ff", "ff6ab0",
               "ff3cac", "2ae8ff", "ff4040", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    rng = Rng(0x70E)
    lay.rect(10, 6, 54, 64, 2)
    lay.rect(10, 6, 14, 64, 3)
    lay.rect(50, 6, 54, 64, 1)
    for y in range(10, 62, 5):
        for x in range(16, 49, 4):
            r = rng.rand()
            c = 5 if r < 0.3 else 6 if r < 0.42 else 7 if r < 0.5 else 1
            lay.rect(x, y, x + 2, y + 3, c)
    lay.rect(18, 1, 46, 6, 1)
    for x in range(19, 45):
        lay.set(x, 2, 8)
        lay.set(x, 4, 8)
    lay.rect(31, 0, 33, 1, 10)
    lay.outline(1)
    return lay, P


def obj_lighthouse():
    P = pal16(["000000", "201828", "f8f0f0", "d8c8d0", "e83a46", "a82030", "3a3048", "fff4b0",
               "ffd060", "6a5a70", "8a7a90", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(32, 64)
    for y in range(14, 60):
        t = (y - 14) / 46.0
        half = 5 + t * 4
        band = ((y - 14) // 8) % 2
        for x in range(int(16 - half), int(16 + half) + 1):
            lit = x < 16 - half * 0.2
            c = (2 if lit else 3) if band == 0 else (4 if lit else 5)
            lay.set(x, y, c)
    lay.rect(9, 59, 24, 64, 9)
    lay.rect(9, 59, 13, 64, 10)
    lay.rect(10, 12, 23, 14, 6)
    lay.rect(12, 5, 21, 12, 7)
    lay.rect(12, 5, 14, 12, 8)
    lay.rect(11, 3, 22, 5, 6)
    lay.rect(15, 1, 18, 3, 6)
    lay.outline(1)
    return lay, P


def obj_umbrellas():
    P = pal16(["000000", "2a1a20", "ff5a6a", "fff4ec", "3ab8d8", "ffd84a", "c89260", "8a6a4a",
               "e8c890", "d0b078", "ffffff", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    for (cx, top, cols) in ((22, 26, (2, 3)), (44, 32, (4, 3))):
        lay.rect(cx, top, cx + 1, 63, 7)
        for y in range(top - 10, top + 1):
            t = (y - (top - 10)) / 10.0
            half = 3 + t * 13
            for x in range(int(cx - half), int(cx + half) + 1):
                seg = int((x - cx + 16) / 4) % 2
                lay.set(x, y, cols[seg])
        lay.set(cx, top - 11, 7)
    # deck chair
    poly(lay, [(26, 56), (40, 56), (44, 49), (42, 48), (38, 54), (28, 54)], 5)
    lay.rect(27, 56, 28, 62, 7)
    lay.rect(38, 56, 39, 62, 7)
    lay.outline(1)
    return lay, P


def obj_cactus():
    P = pal16(["000000", "142a18", "2a6a32", "3e9a44", "6ac85a", "9ae47a", "f0a0c0", "000000",
               "000000", "000000", "000000", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(32, 64)

    def col(x0, y0, y1, w):
        for y in range(y0, y1):
            for x in range(x0 - w, x0 + w + 1):
                d = (x - x0) / float(w)
                c = 4 if d < -0.4 else 3 if d < 0.3 else 2
                if (x - x0) in (-w // 2, w // 2) and y % 3 == 0:
                    c = 5 if d < 0 else 2
                lay.set(x, y, c)
        ellipse(lay, x0 + 0.5, y0, w + 0.5, w * 0.8, 3, lambda x, y, dx, dy, d: 4 if dx < -0.2 else 3)
    col(16, 8, 64, 4)
    col(8, 24, 40, 3)
    for y in range(38, 42):
        for x in range(8, 16):
            lay.set(x, y, 3 if y < 40 else 2)
    col(25, 18, 34, 3)
    for y in range(32, 36):
        for x in range(17, 26):
            lay.set(x, y, 3 if y < 34 else 2)
    lay.set(16, 5, 6)
    lay.set(17, 5, 6)
    lay.set(16, 4, 6)
    lay.outline(1)
    return lay, P


def obj_boulder():
    P = pal16(["000000", "3a1a14", "6a2e20", "9a4a30", "c86a40", "e8925a", "f8b880", "8a8a4a",
               "5a6a32", "000000", "000000", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    for (cx, cy, rx, ry) in ((24, 50, 20, 14), (42, 54, 16, 10), (30, 38, 13, 11)):
        ellipse(lay, cx, cy, rx, ry, 3, lambda x, y, dx, dy, d: ramp_pick([6, 5, 4, 3, 2], (dx + dy + 1.4) / 2.6 + d * 0.2, x, y))
    for x in range(4, 60):
        if (x * 7) % 11 < 3:
            lay.set(x, 63, 8)
            lay.set(x, 62, 7)
    lay.outline(1)
    return lay, P


def obj_deadtree():
    P = pal16(["000000", "2a1810", "5a3a24", "7a5234", "9a6e48", "c8a070", "000000", "000000",
               "000000", "000000", "000000", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    rng = Rng(0xDEAD)

    def branch(x, y, ang, length, w, depth):
        x2 = x + math.cos(ang) * length
        y2 = y + math.sin(ang) * length
        thick_line(lay, x, y, x2, y2, w, max(1.0, w * 0.6), 3 if math.cos(ang) < 0 else 2)
        thick_line(lay, x - 0.5, y, x2 - 0.5, y2, max(1.0, w * 0.4), 1.0, 4)
        if depth > 0:
            for d in (-0.55, 0.5):
                branch(x2, y2, ang + d + rng.uniform(-0.2, 0.2), length * 0.68, w * 0.62, depth - 1)
    branch(32, 63, -math.pi / 2, 22, 6, 4)
    lay.outline(1)
    return lay, P


def obj_pine(big):
    P = pal16(["000000", "0e2420", "1e4a3a", "2a6a4a", "3e8a5a", "f8fcff", "dce8f6", "b8c8e4",
               "5a3a2a", "7a5236", "000000", "000000", "000000", "000000", "000000", "000000"])
    W = 64 if big else 32
    lay = Layer(W, 64)
    cx = W / 2.0
    lay.rect(int(cx) - 2, 54, int(cx) + 2, 64, 8)
    lay.rect(int(cx) - 2, 54, int(cx), 64, 9)
    tiers = 4
    halfmax = (W / 2.0 - 2) if big else 13
    for k in range(tiers):
        top = 4 + k * 12
        bot = top + 18
        hw = halfmax * (0.45 + 0.55 * (k + 1) / tiers)
        for y in range(top, min(bot, 58)):
            t = (y - top) / float(bot - top)
            half = hw * t
            for x in range(int(cx - half), int(cx + half) + 1):
                d = (x - cx) / (half + 0.01)
                c = 4 if d < -0.3 else 3 if d < 0.4 else 2
                if y - top < 3 + abs(d) * 4 * t and t < 0.95:
                    c = 5 if d < 0.2 else 6
                elif t > 0.82 and (x + y) % 3 == 0:
                    c = 6 if d < 0 else 7
                lay.set(x, y, c)
    lay.outline(1)
    return lay, P


def obj_snowman():
    P = pal16(["000000", "1a1c2a", "ffffff", "dce6f6", "b0c0de", "ff7a2a", "f9b3c9", "e8849f",
               "2a2a38", "5a3a2a", "000000", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(32, 64)
    for (cy, r) in ((52, 11), (34, 8.5), (21, 6.5)):
        ellipse(lay, 16, cy, r, r, 2, lambda x, y, dx, dy, d: 2 if dx + dy < 0.2 else 3 if d < 0.85 else 4)
    lay.set(14, 20, 1)
    lay.set(18, 20, 1)
    lay.rect(16, 22, 21, 23, 5)
    for y in (31, 35, 39):
        lay.set(16, y, 8)
    lay.rect(10, 26, 23, 29, 6)
    lay.rect(19, 29, 22, 34, 7)
    lay.rect(11, 12, 22, 14, 8)
    lay.rect(13, 6, 20, 12, 8)
    for (x0, y0, x1, y1) in ((8, 32, 2, 26), (24, 32, 30, 25)):
        thick_line(lay, x0, y0, x1, y1, 1.2, 1.0, 9)
    lay.outline(1)
    return lay, P


def obj_cabin():
    P = pal16(["000000", "1e140e", "5a3a24", "7a5234", "a07048", "f8fcff", "dce8f6", "b8c8e4",
               "ffd27a", "ff9a3c", "3a3040", "000000", "000000", "000000", "000000", "000000"])
    lay = Layer(64, 64)
    lay.rect(10, 34, 54, 63, 3)
    for y in range(34, 63, 4):
        lay.rect(10, y, 54, y + 1, 2)
    lay.rect(10, 34, 13, 63, 4)
    lay.rect(22, 42, 32, 52, 8)
    lay.rect(22, 47, 32, 48, 2)
    lay.rect(26, 42, 27, 52, 2)
    lay.rect(40, 44, 48, 63, 2)
    lay.rect(41, 45, 47, 63, 1)
    poly(lay, [(4, 36), (32, 14), (60, 36)], 6)
    poly(lay, [(4, 36), (32, 14), (32, 18), (8, 36)], 5)
    poly(lay, [(32, 14), (60, 36), (56, 36), (32, 18)], 7)
    lay.rect(44, 14, 49, 26, 10)
    lay.rect(44, 12, 49, 14, 5)
    lay.rect(4, 36, 60, 38, 5)
    lay.outline(1)
    return lay, P


# Per-environment scenery sets: list of (name, layer, palette, world_size, solid_halfwidth)
def scenery_set(env_index):
    if env_index == 0:
        lamp, lp = obj_lamp(True)
        bb, bp = obj_billboard("mochi")
        pl, pp = obj_palm(True)
        tw, tp = obj_tower()
        return [("lamp", lamp, lp, 1500, 0.12), ("billboard", bb, bp, 2600, 0.5),
                ("palm", pl, pp, 2200, 0.15), ("tower", tw, tp, 6400, 1.2)]
    if env_index == 1:
        pl, pp = obj_palm(False)
        bb, bp = obj_billboard("retra")
        lh, lhp = obj_lighthouse()
        um, up = obj_umbrellas()
        return [("palm", pl, pp, 2400, 0.15), ("billboard", bb, bp, 2600, 0.5),
                ("lighthouse", lh, lhp, 3600, 0.4), ("umbrellas", um, up, 2000, 0.5)]
    if env_index == 2:
        ca, cp = obj_cactus()
        bo, bop = obj_boulder()
        dt, dp = obj_deadtree()
        bb, bp = obj_billboard("motel")
        return [("cactus", ca, cp, 1300, 0.15), ("boulder", bo, bop, 2600, 0.6),
                ("deadtree", dt, dp, 2400, 0.15), ("billboard", bb, bp, 2600, 0.5)]
    pi, pp = obj_pine(False)
    bp_, bpp = obj_pine(True)
    sm, smp = obj_snowman()
    cb, cbp = obj_cabin()
    return [("pine", pi, pp, 1500, 0.15), ("bigpine", bp_, bpp, 3000, 0.3),
            ("snowman", sm, smp, 1000, 0.2), ("cabin", cb, cbp, 3400, 0.8)]


def mips_of(lay):
    """mip0..mip3 layers (full, 1/2, 1/4, 1/8 size; min 8 px per axis)."""
    out = [lay]
    for k in (2, 4, 8):
        w, h = max(8, lay.w // k), max(8, lay.h // k)
        if lay.w // k < 8 or lay.h // k < 8:
            # keep aspect inside an 8-px box, bottom-centred
            ww, hh = max(1, lay.w // k), max(1, lay.h // k)
            small = resample(lay, ww, hh, cover=0.3)
            box = Layer(w, h)
            box.paste(small, (w - ww) // 2, h - hh)
            out.append(box)
        else:
            out.append(resample(lay, w, h, cover=0.35))
    return out
