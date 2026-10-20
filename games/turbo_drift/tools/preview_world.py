#!/usr/bin/env python3
"""Dev preview: mock road scenes + scenery sheets per environment (not used by the build)."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
import art_world as W
from pix import to_image

out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/verify/td/world.png"
HZ = 64
sheet = Image.new("RGB", (240 * 2 + 300, 160 * 4), (30, 30, 40))
for e, env in enumerate(W.ENVS):
    im = Image.new("RGB", (240, 160))
    sky = W.sky_table(env)
    gnd = W.ground_table(env)
    rows, pal = W.road_texture(env)
    lay, P, band = W.SKYLINES[e]()
    for y in range(HZ):
        for x in range(240):
            im.putpixel((x, y), sky[HZ - 1 - y])
    for y in range(160):
        for x in range(240):
            r = y - (HZ - W.SKY_H)
            if 0 <= r < W.SKY_H:
                c = lay.px[r][(x + 40) % W.SKY_W]
                if c:
                    im.putpixel((x, y), P[c])
    CAMH, F = 860, 128
    for y in range(HZ + 1, 160):
        dz = CAMH * F / (y - HZ)
        seg = int((dz + 300) / 256)
        band_ = (seg // 3) & 1
        fog = min(7, int(max(0, dz - 4000) / 6000))
        im.paste(gnd[band_][fog], (0, y, 240, y + 1))
        w = 1114 * F / dz
        cx = 120 - 0.0 * dz / 40
        variant = band_
        row = rows[fog * 4 + variant]
        for x in range(240):
            t = (x - cx) / w * 128 + 128
            if 0 <= t < 256:
                im.putpixel((x, y), pal[row[int(t)] - 1])
    sheet.paste(im.resize((480, 320), Image.NEAREST), (0, 160 * e if False else 0) if False else (0, 160 * e))
    objs = W.scenery_set(e)
    x = 0
    for name, l, p, ws, solid in objs:
        sheet.paste(to_image(l, p, 1, (60, 60, 80)), (490 + x, 160 * e))
        mx = 490 + x
        for m in W.mips_of(l)[1:]:
            sheet.paste(to_image(m, p, 1, (60, 60, 80)), (mx, 160 * e + 70))
            mx += m.w + 2
        x += l.w + 4
sheet.paste(to_image(W.obj_chevron(False), W.SIGN_PAL, 1), (490, 160 * 3 + 100))
sheet.paste(to_image(W.obj_chevron(True), W.SIGN_PAL, 1), (530, 160 * 3 + 100))
sheet.paste(to_image(W.obj_start_pylon(), W.SIGN_PAL, 1), (570, 160 * 3 + 90))
sheet.save(out)
print("ok")
