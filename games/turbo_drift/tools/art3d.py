"""Tiny z-buffered polygon renderer used to pre-render Turbo Drift's car sprites.

Models are lists of planar polygons in model space (x right, y up, z forward,
origin = centre of the car on the ground).  A face's material is either a name
or a callable(point, normal) -> name, which lets caps carry painted details
(tail lights, plates, grilles).  Rendering is supersampled and then reduced
with a material-aware mode filter so the result is crisp pixel art.
"""
import math

from pix import Layer


class Model:
    def __init__(self):
        self.faces = []

    def add(self, verts, mat, twosided=False, emissive=False):
        self.faces.append((list(verts), mat, twosided, emissive))

    def quad(self, a, b, c, d, mat, **kw):
        self.add([a, b, c, d], mat, **kw)

    def box(self, x0, y0, z0, x1, y1, z1, mat, skip=()):
        p = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
             (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
        faces = {
            "back": (0, 3, 2, 1), "front": (4, 5, 6, 7), "left": (0, 4, 7, 3),
            "right": (1, 2, 6, 5), "bottom": (0, 1, 5, 4), "top": (3, 7, 6, 2),
        }
        for k, f in faces.items():
            if k in skip:
                continue
            m = mat[k] if isinstance(mat, dict) else mat
            self.add([p[i] for i in f], m)

    def cylinder_x(self, cx, cy, cz, r, x0, x1, sides, tread, cap_out, cap_in="tire"):
        """Wheel-like cylinder with its axis along x (from x0 to x1)."""
        ring = [(cy + r * math.sin(2 * math.pi * (i + 0.5) / sides),
                 cz + r * math.cos(2 * math.pi * (i + 0.5) / sides)) for i in range(sides)]
        for i in range(sides):
            y0_, z0_ = ring[i]
            y1_, z1_ = ring[(i + 1) % sides]
            if x1 > x0:
                self.add([(x0, y0_, z0_), (x0, y1_, z1_), (x1, y1_, z1_), (x1, y0_, z0_)], tread)
            else:
                self.add([(x0, y0_, z0_), (x1, y0_, z0_), (x1, y1_, z1_), (x0, y1_, z1_)], tread)
        outer = [(x1, y, z) for y, z in ring]
        inner = [(x0, y, z) for y, z in ring]
        if x1 > x0:
            self.add(outer[::-1], cap_out)
            self.add(inner, cap_in)
        else:
            self.add(outer, cap_out)
            self.add(inner[::-1], cap_in)


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _norm(a):
    l = math.sqrt(_dot(a, a)) or 1.0
    return (a[0] / l, a[1] / l, a[2] / l)


def face_normal(vs):
    # Newell's method (robust for slightly non-planar quads)
    nx = ny = nz = 0.0
    for i in range(len(vs)):
        a = vs[i]
        b = vs[(i + 1) % len(vs)]
        nx += (a[1] - b[1]) * (a[2] + b[2])
        ny += (a[2] - b[2]) * (a[0] + b[0])
        nz += (a[0] - b[0]) * (a[1] + b[1])
    return _norm((nx, ny, nz))


LIGHT = _norm((-0.45, 0.8, -0.62))


class Camera:
    """Perspective camera looking from eye towards target (world space)."""

    def __init__(self, eye, target, f):
        self.eye = eye
        self.f = f
        fwd = _norm(_sub(target, eye))
        right = _norm(_cross((0, 1, 0), fwd))
        up = _cross(fwd, right)
        self.fwd, self.right, self.up = fwd, right, up

    def to_cam(self, p):
        d = _sub(p, self.eye)
        return (_dot(d, self.right), _dot(d, self.up), _dot(d, self.fwd))


def rot_yaw(p, yaw, pivot_z=0.0):
    c, s = math.cos(yaw), math.sin(yaw)
    x, y, z = p[0], p[1], p[2] - pivot_z
    return (x * c + z * s, y, -x * s + z * c + pivot_z)


def render(model, yaw_deg, cam, W, H, anchor, ss=4, pivot_z=0.0, shade_fn=None, light=LIGHT):
    """Returns a W x H grid of (material, shade) keys or None.
    anchor: screen position where the model origin projects."""
    yaw = math.radians(yaw_deg)
    SW, SH = W * ss, H * ss
    depth = [0.0] * (SW * SH)
    keys = [None] * (SW * SH)
    o = cam.to_cam((0.0, 0.0, 0.0))
    ox = anchor[0] * ss - o[0] * cam.f * ss / o[2]
    oy = anchor[1] * ss + o[1] * cam.f * ss / o[2]
    fs = cam.f * ss
    for verts, mat, twosided, emissive in model.faces:
        wv = [rot_yaw(v, yaw, pivot_z) for v in verts]
        n = face_normal(wv)
        cv = [cam.to_cam(v) for v in wv]
        if any(c[2] <= 0.1 for c in cv):
            continue
        # back-face cull in camera space
        view = _norm(_sub(wv[0], cam.eye))
        facing = _dot(n, view)
        if facing > 0:
            if not twosided:
                continue
            n = (-n[0], -n[1], -n[2])
        if emissive:
            sh = 3
        elif shade_fn:
            sh = shade_fn(n)
        else:
            d = _dot(n, light)
            inten = 0.22 + 0.78 * max(0.0, d)
            sh = 0 if inten < 0.42 else 1 if inten < 0.62 else 2 if inten < 0.86 else 3
        scr = [(ox + c[0] * fs / c[2], oy - c[1] * fs / c[2], 1.0 / c[2]) for c in cv]
        for i in range(1, len(verts) - 1):
            _tri(depth, keys, SW, SH, scr[0], scr[i], scr[i + 1], verts[0], verts[i], verts[i + 1],
                 mat, sh, n)
    out = [[None] * W for _ in range(H)]
    return keys, SW, SH, out


def _tri(depth, keys, SW, SH, a, b, c, pa, pb, pc, mat, sh, n):
    minx = max(0, int(math.floor(min(a[0], b[0], c[0]))))
    maxx = min(SW - 1, int(math.ceil(max(a[0], b[0], c[0]))))
    miny = max(0, int(math.floor(min(a[1], b[1], c[1]))))
    maxy = min(SH - 1, int(math.ceil(max(a[1], b[1], c[1]))))
    if minx > maxx or miny > maxy:
        return
    area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
    if abs(area) < 1e-9:
        return
    inv = 1.0 / area
    callable_mat = callable(mat)
    key = (mat, sh) if not callable_mat else None
    for y in range(miny, maxy + 1):
        py = y + 0.5
        row = y * SW
        for x in range(minx, maxx + 1):
            px = x + 0.5
            w0 = ((c[0] - b[0]) * (py - b[1]) - (c[1] - b[1]) * (px - b[0])) * inv
            if w0 < 0:
                continue
            w1 = ((a[0] - c[0]) * (py - c[1]) - (a[1] - c[1]) * (px - c[0])) * inv
            if w1 < 0:
                continue
            w2 = 1.0 - w0 - w1
            if w2 < 0:
                continue
            iz = w0 * a[2] + w1 * b[2] + w2 * c[2]
            idx = row + x
            if iz <= depth[idx]:
                continue
            depth[idx] = iz
            if callable_mat:
                q0, q1, q2 = w0 * a[2] / iz, w1 * b[2] / iz, w2 * c[2] / iz
                p = (q0 * pa[0] + q1 * pb[0] + q2 * pc[0],
                     q0 * pa[1] + q1 * pb[1] + q2 * pc[1],
                     q0 * pa[2] + q1 * pb[2] + q2 * pc[2])
                m = mat(p, n)
                if isinstance(m, tuple):
                    keys[idx] = m
                else:
                    keys[idx] = (m, sh)
            else:
                keys[idx] = key


def resolve(keys, SW, SH, ss, W, H, matmap, weights=None, cover=0.4):
    """Mode-filter the supersampled keys into palette indices via matmap[(mat)][shade]."""
    lay = Layer(W, H)
    thr = ss * ss * cover
    for y in range(H):
        for x in range(W):
            cnt = {}
            nfill = 0
            for yy in range(ss):
                base = (y * ss + yy) * SW + x * ss
                for xx in range(ss):
                    k = keys[base + xx]
                    if k is None:
                        continue
                    nfill += 1
                    m, s = k
                    if m is None:
                        continue
                    idx = matmap[m][s]
                    if not idx:
                        continue
                    wgt = weights.get(m, 1.0) if weights else 1.0
                    cnt[idx] = cnt.get(idx, 0.0) + wgt
            if nfill >= thr and cnt:
                lay.px[y][x] = max(sorted(cnt), key=lambda k: cnt[k])
    return lay


def render_layer(model, yaw, cam, W, H, anchor, matmap, ss=4, pivot_z=0.0, weights=None, cover=0.4):
    keys, SW, SH, _ = render(model, yaw, cam, W, H, anchor, ss, pivot_z)
    return resolve(keys, SW, SH, ss, W, H, matmap, weights, cover)


def measure(model, yaw, cam, W, H, anchor, ss=2):
    keys, SW, SH, _ = render(model, yaw, cam, W, H, anchor, ss)
    xs = [i % SW for i, k in enumerate(keys) if k is not None]
    ys = [i // SW for i, k in enumerate(keys) if k is not None]
    if not xs:
        return None
    return (min(xs) / ss, min(ys) / ss, (max(xs) + 1) / ss, (max(ys) + 1) / ss)


def measure_pivot(model, yaw, cam, W, H, pivot_z=0.0):
    """Lowest screen y (relative to the origin anchor) covered by the model."""
    keys, SW, SH, _ = render(model, yaw, cam, W, H * 4, (W / 2.0, H * 2.0), 2, pivot_z)
    ys = [i // SW for i, k in enumerate(keys) if k is not None]
    return (max(ys) + 1) / 2.0 - H * 2.0
