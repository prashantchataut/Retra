"""Original car designs for Turbo Drift, built as lofted low-poly models and
pre-rendered with art3d into rear-view race sprites, steer/drift/spin frames and
3/4 garage portraits.  No real-world car brand or design is reproduced."""
import math

import art3d as R
from pix import Layer, pal16, hexrgb, mix

# ------------------------------------------------------------------ palettes
# Shared 16-colour layout for every car palette (lets any body wear any paint).
#  1 outline   2 tyre   3 rim/grey   4-7 paint ramp   8-9 glass
# 10 tail lamp 11 hot lamp/brake 12 chrome/plate 13 trim 14 driver 15 leaf/extra
PAINTS = [
    # name, dark, mid, light, highlight
    ("CRIMSON", "6a1028", "b0203c", "e8404e", "ff9a8e"),
    ("COBALT", "142c6c", "2454c0", "3c8cf0", "a8dcff"),
    ("CITRUS", "8a5a08", "d89a10", "ffd23a", "fff3a8"),
    ("MINT", "0e5a4a", "1c9a78", "3cdca0", "b8ffe0"),
    ("SAKURA", "8a2c5a", "d8608e", "f9a0c0", "ffe0ec"),
    ("PEARL", "707890", "a8b0c8", "e0e6f2", "ffffff"),
    ("ONYX", "141420", "2a2a3c", "4a4a62", "8c8cae"),
    ("VIOLET", "3a1470", "6a2cc0", "a060f0", "e0c0ff"),
]
TRAFFIC_PAINTS = [
    ("SAND", "6a5a40", "9a8664", "c8b48e", "efe2c4"),
    ("SLATE", "2e3a46", "4c5c6c", "74889a", "b8c8d6"),
    ("OLIVE", "3e4a26", "62723a", "8a9c56", "c6d49a"),
    ("RUST", "5a2a1a", "8c4426", "b8663a", "e8a47a"),
]


def car_palette(paint, driver=False, night=False):
    _, d, m, l, h = paint
    return pal16([
        "000000", "120e1a", "34323e", "6e7280", d, m, l, h,
        "1a2236", "5a7ca8", "a8142c", "ff4a5e", "cfd4e0", "3a3a48",
        "f9b3c9" if driver else "1a2236", "6fcf7a" if driver else "1a2236",
    ])


MATMAP = {
    "body": [4, 5, 6, 7], "body_d": [4, 4, 5, 6],
    "glass": [8, 8, 8, 9], "glass_hi": [9, 9, 9, 9],
    "tail": [10, 10, 10, 10], "tail_hot": [11, 11, 11, 11],
    "trim": [1, 13, 13, 13], "tire": [1, 2, 2, 2], "rim": [2, 3, 12, 12],
    "chrome": [3, 12, 12, 12], "plate": [12, 12, 12, 12], "dark": [1, 1, 1, 1],
    "shadow": [1, 1, 1, 1], "mochi": [14, 14, 14, 14], "leaf": [15, 15, 15, 15],
    "light": [12, 12, 12, 12], "grey": [3, 3, 3, 12],
}
WEIGHTS = {"tail": 2.6, "tail_hot": 2.6, "plate": 2.0, "chrome": 2.2, "mochi": 2.4, "leaf": 2.8,
           "light": 2.2, "glass_hi": 1.3, "tire": 1.3, "shadow": 0.8}


# ------------------------------------------------------------------ lofting
def section(st):
    """Half cross-section (x >= 0) from bottom centre to top centre."""
    w, yb, ys, yh, yd, wg, yr = st["w"], st["yb"], st["ys"], st["yh"], st["yd"], st["wg"], st["yr"]
    crown = st.get("crown", 0.25)
    return [(0.0, yb), (w * 0.9, yb), (w, ys), (w, yh), (w * 0.95, yd), (wg, yr), (0.0, yr + crown)]


def loft(model, stations, matfn, front_cap, rear_cap):
    secs = [section(s) for s in stations]
    zs = [s["z"] for s in stations]
    for k in range(len(stations) - 1):
        a, b = secs[k], secs[k + 1]
        za, zb = zs[k], zs[k + 1]
        for j in range(len(a) - 1):
            m = matfn(k, j, stations[k], stations[k + 1])
            if m is None:
                continue
            # right side (x > 0)
            model.quad((a[j][0], a[j][1], za), (a[j + 1][0], a[j + 1][1], za),
                       (b[j + 1][0], b[j + 1][1], zb), (b[j][0], b[j][1], zb), m)
            # left side mirrored
            model.quad((-b[j][0], b[j][1], zb), (-b[j + 1][0], b[j + 1][1], zb),
                       (-a[j + 1][0], a[j + 1][1], za), (-a[j][0], a[j][1], za), m)
    # caps: fan from centre (star-shaped outline)
    for sec, z, mat, front in ((secs[0], zs[0], rear_cap, False), (secs[-1], zs[-1], front_cap, True)):
        ring = [(x, y) for x, y in sec] + [(-x, y) for x, y in reversed(sec)]
        ring = [p for i, p in enumerate(ring) if i == 0 or p != ring[i - 1]]
        cx = 0.0
        cy = sum(p[1] for p in ring) / len(ring)
        for i in range(len(ring)):
            p, q = ring[i], ring[(i + 1) % len(ring)]
            if p == q:
                continue
            if front:
                model.add([(cx, cy, z), (p[0], p[1], z), (q[0], q[1], z)], mat)
            else:
                model.add([(cx, cy, z), (q[0], q[1], z), (p[0], p[1], z)], mat)


def default_matfn(spec):
    cab0, cab1 = spec["cabin"]          # station indices bounding the glass area
    rw0, rw1 = spec["rear_window"]      # station pair for the rear window
    ws0, ws1 = spec["windshield"]

    def fn(k, j, s0, s1):
        if j == 0:
            return "dark"
        if j == 1:
            return "trim"
        if j in (2, 3):
            return "body"
        if j == 4:
            if cab0 <= k < cab1:
                return "glass"
            return "body"
        if j == 5:
            if rw0 <= k < rw1:
                return spec.get("rear_window_mat", "glass")
            if ws0 <= k < ws1:
                return "glass"
            return "body"
        return "body"
    return fn


def glass_fn(driver_x=None, head_y=None):
    """Rear window: glass with reflection streaks and (optionally) Mochi peeking."""
    def fn(p, n):
        x, y, z = p
        if driver_x is not None:
            dx = (x - driver_x) / 1.9
            dy = (y - head_y) / 1.45
            if dx * dx + dy * dy < 1.0:
                return "mochi"
            # little leaf on top of Mochi's head
            lx, ly = x - (driver_x + 0.35), y - (head_y + 1.6)
            if -0.55 < lx < 0.75 and -0.4 < ly < 0.55 and abs(lx - ly * 0.6) < 0.6:
                return "leaf"
        t = (x * 0.55 + y * 1.0 + 40.0) % 5.0
        if t < 0.7:
            return "glass_hi"
        return "glass"
    return fn


def rear_cap_fn(spec):
    lamp = spec["lamps"]
    yl0, yl1 = spec["lamp_y"]

    def fn(p, n):
        x, y, z = p
        ax = abs(x)
        w = spec["rear_w"]
        # bumper / diffuser
        if y < spec["bumper_y"]:
            if spec.get("exhaust") and y > spec["bumper_y"] - 1.5:
                for ex in spec["exhaust"]:
                    if (ax - ex) ** 2 + ((y - (spec["bumper_y"] - 0.75)) * 1.3) ** 2 < 0.55:
                        return "chrome" if (ax - ex) ** 2 + (y - (spec["bumper_y"] - 0.75)) ** 2 > 0.12 else "dark"
            if spec.get("diffuser") and ax < w * 0.62 and int((x + 30) * 1.2) % 2 == 0:
                return "dark"
            return "trim"
        # number plate
        if ax < 2.4 and spec["plate_y"][0] < y < spec["plate_y"][1]:
            return "plate"
        if yl0 <= y <= yl1:
            if lamp == "bar":
                if ax < w - 0.5:
                    core = abs(y - (yl0 + yl1) / 2) < (yl1 - yl0) * 0.22
                    return "tail_hot" if core and ax > 1.0 else "tail"
            elif lamp == "twin":
                for cxl in spec["lamp_cx"]:
                    d = ((ax - cxl) / 1.35) ** 2 + ((y - (yl0 + yl1) / 2) / ((yl1 - yl0) / 2 + 0.01)) ** 2
                    if d < 1.0:
                        return "tail_hot" if d < 0.35 else "tail"
                if ax < spec["lamp_cx"][0] - 1.4:
                    return "trim"
            elif lamp == "slim":
                if ax > w * 0.42:
                    edge = y > yl1 - (yl1 - yl0) * 0.45
                    return "tail_hot" if edge else "tail"
                if ax > w * 0.36:
                    return "trim"
                return "trim" if y > (yl0 + yl1) / 2 else "body"
            elif lamp == "round":
                for cxl in spec["lamp_cx"]:
                    d = ((ax - cxl) ** 2 + (y - (yl0 + yl1) / 2) ** 2) / (((yl1 - yl0) / 2) ** 2)
                    if d < 1.0:
                        return "tail_hot" if d < 0.3 else "tail"
            elif lamp == "block":
                if ax > w * 0.55:
                    return "tail"
                return "trim"
        if spec.get("rear_stripe") and spec["rear_stripe"][0] < y < spec["rear_stripe"][1]:
            return "trim"
        return "body"
    return fn


def front_cap_fn(spec):
    def fn(p, n):
        x, y, z = p
        ax = abs(x)
        if y < spec["fbumper_y"]:
            return "trim" if ax > spec["front_w"] * 0.45 else "dark"
        hl0, hl1 = spec["head_y"]
        if hl0 < y < hl1 and spec["head_x"][0] < ax < spec["head_x"][1]:
            return "light"
        if ax < spec["grille_w"] and y < spec["grille_y"]:
            return "dark" if int(y * 2.2) % 2 == 0 else "trim"
        return "body"
    return fn


def build_car(spec, driver=False):
    m = R.Model()
    st = spec["stations"]
    fn = default_matfn(spec)
    rw = spec["rear_window"]
    gfn = glass_fn(spec.get("driver_x", -2.4) if driver else None, spec.get("drv_y", 10.0))

    arch_r = spec["wheel_r"] + 0.7

    def side_fn(p, n):
        for zax in spec["axles"]:
            dy, dz = p[1] - spec["wheel_r"], p[2] - zax
            if dy * dy + dz * dz < arch_r * arch_r:
                return "dark"
        return "body"

    def matfn(k, j, s0, s1):
        r = fn(k, j, s0, s1)
        if j in (1, 2) and r in ("body", "trim"):
            zlo, zhi = s0["z"], s1["z"]
            if any(zlo - arch_r < zax < zhi + arch_r for zax in spec["axles"]):
                return side_fn if j == 2 else (lambda p, n: "dark" if any(
                    (p[2] - zax) ** 2 < arch_r * arch_r for zax in spec["axles"]) else "trim")
        if j == 5 and rw[0] <= k < rw[1] and r == "glass":
            return gfn
        if j == 4 and r == "glass":
            return glass_fn()
        return r
    loft(m, st, matfn, front_cap_fn(spec), rear_cap_fn(spec))
    # wheels
    r_, tw = spec["wheel_r"], spec["wheel_w"]
    for zax in spec["axles"]:
        for side in (-1, 1):
            xo = side * (spec["track"])
            xi = side * (spec["track"] - tw)
            m.cylinder_x(0, r_, zax, r_, xi, xo, 10, "tire", rim_fn(r_, zax), "tire")
    # spoiler
    sp = spec.get("spoiler")
    if sp:
        x0, y0, y1, z0, z1 = sp["x"], sp["y0"], sp["y1"], sp["z0"], sp["z1"]
        m.box(-x0, y0, z0, x0, y1, z1, {"back": "body_d", "front": "body", "top": "body", "bottom": "trim",
                                         "left": "body_d", "right": "body_d"})
        if sp.get("posts"):
            for px in sp["posts"]:
                for s in (-1, 1):
                    m.box(s * px - 0.35, sp["post_y"], z0 + 0.6, s * px + 0.35, y0, z0 + 1.6, "trim")
        if sp.get("plates"):
            for s in (-1, 1):
                m.box(s * x0 - (0.4 if s > 0 else -0.0), y0 - 1.0, z0 - 0.2, s * x0 + (0.0 if s > 0 else 0.4),
                      y1 + 0.5, z1 + 0.4, "trim")
    # mirrors
    if spec.get("mirrors"):
        mz, my, mx = spec["mirrors"]
        for s in (-1, 1):
            m.box(s * mx - 0.9, my, mz, s * mx + 0.9, my + 1.0, mz + 1.2,
                  {"back": "body_d", "front": "body", "top": "body", "bottom": "trim", "left": "body",
                   "right": "body"})
    # mochi leaf antenna on the roof
    if spec.get("leaf_antenna"):
        lz, ly = spec["leaf_antenna"]
        m.add([(0, ly, lz - 0.5), (0.0, ly + 2.4, lz - 1.6), (1.6, ly + 3.2, lz - 0.4), (0.9, ly + 1.0, lz + 0.4)],
              "leaf", twosided=True)
        m.add([(-0.0, ly, lz - 0.5), (-0.9, ly + 1.0, lz + 0.4), (-1.6, ly + 3.0, lz - 0.6), (-0.1, ly + 2.4, lz - 1.6)],
              "leaf", twosided=True)
    for extra in spec.get("boxes", []):
        m.box(*extra)
    # ground shadow
    sw, sl = spec["track"] + 0.6, spec["length"] / 2 + 0.6
    pts = []
    for i in range(12):
        a = 2 * math.pi * i / 12
        pts.append((sw * math.copysign(abs(math.cos(a)) ** 0.5, math.cos(a)), 0.03,
                    sl * math.copysign(abs(math.sin(a)) ** 0.5, math.sin(a))))
    m.add(pts[::-1], "shadow")
    return m


def rim_fn(r, zax):
    def fn(p, n):
        dy, dz = p[1] - r, p[2] - zax
        d = math.sqrt(dy * dy + dz * dz) / r
        if d > 0.74:
            return "tire"
        if d < 0.18:
            return ("chrome", 2)
        if d > 0.62:
            return ("grey", 0)
        ang = math.atan2(dy, dz)
        spoke = (math.cos(ang * 5) > 0.35)
        return ("grey", 1) if spoke else ("dark", 0)
    return fn


def S(z, w, yb, ys, yh, yd, wg, yr, crown=0.25):
    return {"z": z, "w": w, "yb": yb, "ys": ys, "yh": yh, "yd": yd, "wg": wg, "yr": yr, "crown": crown}


# ------------------------------------------------------------------ designs
# KITSUNE: nimble fastback coupe, twin round lamps, duck-tail spoiler.
KITSUNE = {
    "name": "KITSUNE", "length": 42.0, "track": 9.75, "wheel_r": 3.0, "wheel_w": 2.4, "axles": (-13.0, 13.5),
    "stations": [
        S(-21.0, 8.6, 1.7, 2.7, 6.6, 7.5, 8.0, 7.7),
        S(-20.2, 9.3, 1.6, 2.6, 7.0, 8.2, 8.6, 8.5),
        S(-15.5, 9.5, 1.6, 2.6, 7.0, 8.1, 8.3, 8.6),
        S(-11.5, 9.5, 1.6, 2.6, 7.0, 8.0, 7.8, 8.7),
        S(-4.5, 9.5, 1.6, 2.6, 7.0, 7.9, 6.6, 11.6, 0.35),
        S(3.5, 9.4, 1.6, 2.6, 6.9, 7.8, 6.4, 11.8, 0.35),
        S(9.5, 9.3, 1.6, 2.6, 6.6, 7.4, 7.6, 7.9),
        S(18.5, 9.0, 1.6, 2.6, 6.0, 6.7, 8.0, 6.9),
        S(21.0, 8.2, 1.5, 2.4, 5.0, 5.6, 7.2, 5.8),
    ],
    "cabin": (3, 6), "rear_window": (3, 4), "windshield": (5, 6),
    "rear_w": 8.6, "bumper_y": 3.0, "plate_y": (3.3, 4.7), "lamps": "twin", "lamp_y": (5.3, 6.9),
    "lamp_cx": (5.1, 7.3), "exhaust": (4.2,), "diffuser": False, "rear_stripe": None,
    "front_w": 8.2, "fbumper_y": 2.6, "head_y": (3.8, 4.9), "head_x": (4.4, 7.4), "grille_w": 3.6,
    "grille_y": 3.8, "mirrors": (8.6, 7.6, 9.6),
    "spoiler": {"x": 8.4, "y0": 8.5, "y1": 9.0, "z0": -21.0, "z1": -19.6},
    "driver_x": -2.3, "drv_y": 9.7,
}

# RONIN: long-hood muscle GT, full-width light bar, fat tyres, lip spoiler.
RONIN = {
    "name": "RONIN", "length": 46.0, "track": 10.25, "wheel_r": 3.2, "wheel_w": 3.0, "axles": (-14.0, 15.0),
    "stations": [
        S(-23.0, 9.4, 1.8, 2.8, 7.2, 7.9, 9.0, 8.1),
        S(-22.2, 9.9, 1.7, 2.7, 7.4, 8.4, 9.3, 8.7),
        S(-17.0, 10.0, 1.7, 2.7, 7.4, 8.4, 8.8, 8.8),
        S(-12.5, 10.0, 1.7, 2.7, 7.4, 8.3, 8.0, 8.9),
        S(-7.5, 10.0, 1.7, 2.7, 7.4, 8.2, 7.0, 11.9, 0.3),
        S(1.0, 10.0, 1.7, 2.7, 7.3, 8.1, 6.9, 12.0, 0.3),
        S(6.5, 9.9, 1.7, 2.7, 7.1, 7.9, 8.2, 8.3),
        S(20.0, 9.7, 1.7, 2.7, 6.7, 7.4, 8.8, 7.7),
        S(23.0, 9.2, 1.6, 2.6, 6.2, 6.8, 8.4, 7.0),
    ],
    "cabin": (3, 6), "rear_window": (3, 4), "windshield": (5, 6),
    "rear_w": 9.4, "bumper_y": 3.1, "plate_y": (3.4, 4.9), "lamps": "bar", "lamp_y": (5.5, 6.9),
    "exhaust": (5.6, 7.0), "diffuser": False, "rear_stripe": (5.0, 5.4),
    "front_w": 9.2, "fbumper_y": 2.7, "head_y": (4.4, 5.6), "head_x": (5.2, 8.6), "grille_w": 4.8,
    "grille_y": 5.6, "mirrors": (5.4, 7.9, 10.1),
    "spoiler": {"x": 9.2, "y0": 8.6, "y1": 9.1, "z0": -23.0, "z1": -21.8},
    "boxes": [(-2.4, 7.6, 9.0, 2.4, 8.4, 15.0, {"back": "dark", "front": "body", "top": "body_d", "bottom": "trim",
                                                  "left": "body_d", "right": "body_d"})],
    "driver_x": -2.6, "drv_y": 9.9,
}

# KAZE ZR: mid-engine wedge supercar, slim LED lamps, diffuser, tall wing.
KAZE = {
    "name": "KAZE ZR", "length": 44.0, "track": 10.5, "wheel_r": 3.1, "wheel_w": 3.0, "axles": (-13.5, 13.0),
    "stations": [
        S(-22.0, 9.6, 1.4, 2.4, 6.4, 7.0, 9.0, 7.2),
        S(-21.2, 10.2, 1.3, 2.3, 6.8, 7.6, 9.6, 7.9),
        S(-14.0, 10.3, 1.3, 2.3, 6.9, 7.6, 9.2, 8.2),
        S(-8.0, 10.3, 1.3, 2.3, 6.8, 7.4, 7.0, 8.6),
        S(-3.5, 10.2, 1.3, 2.3, 6.6, 7.2, 6.2, 10.4, 0.3),
        S(2.0, 10.0, 1.3, 2.3, 6.2, 6.8, 6.0, 10.5, 0.3),
        S(9.5, 9.8, 1.3, 2.3, 5.4, 5.9, 7.4, 6.1),
        S(19.5, 9.4, 1.3, 2.2, 4.4, 4.8, 8.0, 5.0),
        S(22.0, 8.6, 1.2, 2.0, 3.6, 4.0, 7.4, 4.1),
    ],
    "cabin": (3, 6), "rear_window": (3, 4), "windshield": (5, 6), "rear_window_mat": "glass",
    "rear_w": 9.6, "bumper_y": 2.8, "plate_y": (3.1, 4.4), "lamps": "slim", "lamp_y": (5.4, 6.4),
    "exhaust": (1.2,), "diffuser": True, "rear_stripe": None,
    "front_w": 8.6, "fbumper_y": 2.2, "head_y": (3.0, 3.7), "head_x": (4.6, 7.8), "grille_w": 3.0,
    "grille_y": 2.9, "mirrors": (3.8, 6.8, 10.0),
    "spoiler": {"x": 9.6, "y0": 9.9, "y1": 10.5, "z0": -21.8, "z1": -19.4, "posts": (5.0,), "post_y": 7.4,
                "plates": True},
    "driver_x": -2.2, "drv_y": 8.9,
}

# MOCHI GT: bubbly prize car with round lamps and a leaf antenna.
MOCHI_GT = {
    "name": "MOCHI GT", "length": 40.0, "track": 10.0, "wheel_r": 3.0, "wheel_w": 2.8, "axles": (-12.5, 12.5),
    "stations": [
        S(-20.0, 8.4, 1.8, 3.0, 6.4, 7.6, 7.8, 7.9, 0.4),
        S(-19.0, 9.4, 1.6, 2.7, 7.0, 8.6, 8.6, 9.0, 0.4),
        S(-14.0, 9.8, 1.6, 2.6, 7.2, 8.6, 8.4, 9.3, 0.4),
        S(-10.0, 9.8, 1.6, 2.6, 7.2, 8.5, 7.8, 9.5, 0.4),
        S(-4.0, 9.8, 1.6, 2.6, 7.1, 8.3, 6.8, 12.0, 0.5),
        S(3.0, 9.7, 1.6, 2.6, 7.0, 8.2, 6.6, 12.1, 0.5),
        S(8.5, 9.6, 1.6, 2.6, 6.8, 7.8, 7.8, 8.4, 0.4),
        S(17.0, 9.2, 1.6, 2.6, 6.2, 7.0, 8.0, 7.3, 0.4),
        S(20.0, 8.2, 1.7, 2.8, 5.2, 6.0, 7.0, 6.2, 0.4),
    ],
    "cabin": (3, 6), "rear_window": (3, 4), "windshield": (5, 6),
    "rear_w": 8.4, "bumper_y": 3.0, "plate_y": (3.3, 4.6), "lamps": "round", "lamp_y": (5.3, 7.1),
    "lamp_cx": (6.2,), "exhaust": (3.4, -0.0), "diffuser": False, "rear_stripe": None,
    "front_w": 8.2, "fbumper_y": 2.8, "head_y": (4.0, 5.4), "head_x": (4.6, 7.2), "grille_w": 2.6,
    "grille_y": 3.8, "mirrors": (8.0, 8.0, 9.8),
    "spoiler": {"x": 8.0, "y0": 9.0, "y1": 9.6, "z0": -19.6, "z1": -18.0},
    "leaf_antenna": (0.0, 12.3),
    "driver_x": -2.2, "drv_y": 9.9,
}

# Traffic: tall compact hatchback and a delivery van (muted colours).
HATCH = {
    "name": "HATCH", "length": 36.0, "track": 8.7, "wheel_r": 2.6, "wheel_w": 2.0, "axles": (-11.0, 11.5),
    "stations": [
        S(-18.0, 8.0, 1.8, 2.8, 7.6, 8.6, 7.2, 9.0, 0.3),
        S(-17.4, 8.4, 1.8, 2.8, 7.8, 8.8, 7.4, 13.4, 0.4),
        S(-12.0, 8.5, 1.8, 2.8, 7.8, 8.8, 7.2, 14.0, 0.4),
        S(2.0, 8.5, 1.8, 2.8, 7.6, 8.6, 7.0, 14.0, 0.4),
        S(8.0, 8.4, 1.8, 2.8, 7.2, 8.0, 7.6, 8.8),
        S(16.0, 8.2, 1.8, 2.8, 6.4, 7.2, 7.6, 7.4),
        S(18.0, 7.6, 1.8, 2.8, 5.6, 6.2, 7.0, 6.4),
    ],
    "cabin": (1, 4), "rear_window": (0, 1), "windshield": (3, 4),
    "rear_w": 8.0, "bumper_y": 3.2, "plate_y": (3.6, 5.0), "lamps": "block", "lamp_y": (6.0, 8.4),
    "exhaust": None, "diffuser": False, "rear_stripe": None,
    "front_w": 7.6, "fbumper_y": 2.8, "head_y": (4.4, 5.6), "head_x": (4.2, 6.8), "grille_w": 3.4,
    "grille_y": 4.2, "mirrors": None, "spoiler": None,
}
# hatch rear glass sits on the tailgate: paint it via an extra box
HATCH["boxes"] = [(-6.4, 9.2, -18.05, 6.4, 13.0, -17.9, {"back": "glass", "front": "glass", "top": "body",
                                                           "bottom": "body", "left": "body", "right": "body"})]

VAN = {
    "name": "VAN", "length": 48.0, "track": 9.7, "wheel_r": 2.9, "wheel_w": 2.4, "axles": (-15.0, 15.0),
    "stations": [
        S(-24.0, 9.2, 2.0, 3.0, 19.0, 19.8, 8.8, 20.2, 0.2),
        S(-23.4, 9.5, 2.0, 3.0, 19.4, 20.4, 9.2, 20.8, 0.2),
        S(8.0, 9.5, 2.0, 3.0, 19.4, 20.4, 9.2, 20.8, 0.2),
        S(12.0, 9.4, 2.0, 3.0, 9.0, 9.6, 8.4, 18.0, 0.3),
        S(18.0, 9.2, 2.0, 3.0, 8.6, 9.2, 8.6, 9.6),
        S(24.0, 8.8, 2.0, 3.0, 7.4, 8.0, 8.2, 8.2),
    ],
    "cabin": (3, 4), "rear_window": (-1, -1), "windshield": (2, 3),
    "rear_w": 9.2, "bumper_y": 3.4, "plate_y": (3.8, 5.2), "lamps": "block", "lamp_y": (6.0, 10.0),
    "exhaust": None, "diffuser": False, "rear_stripe": (12.0, 12.6),
    "front_w": 8.8, "fbumper_y": 3.0, "head_y": (5.0, 6.4), "head_x": (5.0, 7.8), "grille_w": 4.0,
    "grille_y": 5.0, "mirrors": (11.0, 13.0, 10.2), "spoiler": None,
}
VAN["boxes"] = [(-0.15, 3.6, -24.06, 0.15, 18.8, -23.95, "trim"),
                (-8.4, 14.6, -24.08, -0.6, 18.4, -23.98, {"back": "glass", "front": "glass", "top": "body",
                                                           "bottom": "body", "left": "body", "right": "body"}),
                (0.6, 14.6, -24.08, 8.4, 18.4, -23.98, {"back": "glass", "front": "glass", "top": "body",
                                                         "bottom": "body", "left": "body", "right": "body"})]

PLAYER_CARS = [KITSUNE, RONIN, KAZE, MOCHI_GT]
TRAFFIC_CARS = [HATCH, VAN]

# ------------------------------------------------------------------ cameras / frames
# Rear chase view used by every in-race sprite.  f is chosen so a 19.5-unit
# wide car renders ~50 px wide in the 64x32 mip0 frame.
EYE = (0.0, 12.0, -72.0)
TARGET = (0.0, 5.0, 0.0)
F_MIP0 = 142.0
REAR_ANCHOR_Y = 30.5          # where the model origin lands inside a 32-px-tall frame

STEER_YAWS = [0, 4, 8, 13, 19, 26, 33]          # player steer/drift frames (mirrored in game)
SPIN_YAWS = [45, 67.5, 90, 112.5, 135, 157.5, 180]
RIVAL_YAWS = [0, 5]


def cam_for(scale=1.0):
    return R.Camera(EYE, TARGET, F_MIP0 * scale)


def rear_frame(spec, yaw, W, H, scale=1.0, driver=False, pivot_z=None, anchor_y=None):
    model = build_car(spec, driver)
    if pivot_z is None:
        pivot_z = spec["axles"][0] if abs(yaw) < 30 else 0.0
    cam = cam_for(scale)
    if anchor_y is None:
        bb = R.measure(model, yaw, cam, W, H * 4, (W / 2.0, 0.0), ss=2) if False else \
            R.measure_pivot(model, yaw, cam, W, H, pivot_z)
        ay = (H - 1.6) - bb
    else:
        ay = anchor_y
    lay = R.render_layer(model, yaw, cam, W, H, (W / 2.0, ay), MATMAP,
                         ss=4 if scale >= 0.5 else 6, pivot_z=pivot_z, weights=WEIGHTS,
                         cover=0.38 if scale >= 0.5 else 0.30)
    return lay


def finish(lay, outline=True):
    if outline:
        lay.outline(1, skip=(1,))
    return lay


def player_frames(spec):
    """128x32 frames: steer/drift yaws then spin yaws (all with Mochi at the wheel)."""
    out = []
    for yaw in STEER_YAWS:
        lay = rear_frame(spec, yaw, 128, 32, 1.0, True, pivot_z=spec["axles"][0] if yaw < 16 else -2.0)
        out.append(finish(lay))
    for yaw in SPIN_YAWS:
        lay = rear_frame(spec, yaw, 128, 32, 1.0, True, pivot_z=0.0)
        out.append(finish(lay))
    return out


MIP_SIZES = [(64, 32, 1.0), (32, 16, 0.5), (16, 8, 0.25), (8, 8, 0.125)]


def rival_frames(spec):
    """For each yaw: mip0..mip3 layers (64x32, 32x16, 16x8, 8x8)."""
    res = []
    for yaw in RIVAL_YAWS:
        mips = []
        for (w, h, s) in MIP_SIZES:
            lay = rear_frame(spec, yaw, w, h, s, False)
            mips.append(finish(lay, outline=s >= 0.5))
        res.append(mips)
    return res


VAN_MIPS = [(64, 64, 1.0), (32, 32, 0.5), (16, 16, 0.25), (8, 8, 0.125)]


def traffic_frames(spec):
    mips = []
    sizes = VAN_MIPS
    for (w, h, s) in sizes:
        lay = rear_frame(spec, 0, w, h, s, False)
        mips.append(finish(lay, outline=s >= 0.5))
    return mips


def garage_frame(spec, W=128, H=64, yaw=148.0):
    """3/4 front portrait used in the garage (rendered larger, from a higher eye)."""
    model = build_car(spec, True)
    cam = R.Camera((0.0, 26.0, -80.0), (0.0, 5.0, 0.0), 138.0)
    lay = R.render_layer(model, yaw, cam, W, H, (W / 2.0, H - 16.0), MATMAP, ss=3, pivot_z=0.0,
                         weights=WEIGHTS, cover=0.4)
    return finish(lay)


def side_frame(spec, W=128, H=48):
    model = build_car(spec, True)
    cam = R.Camera((0.0, 10.0, -90.0), (0.0, 5.0, 0.0), 150.0)
    lay = R.render_layer(model, 90.0, cam, W, H, (W / 2.0, H - 8.0), MATMAP, ss=3, pivot_z=0.0,
                         weights=WEIGHTS, cover=0.4)
    return finish(lay)
