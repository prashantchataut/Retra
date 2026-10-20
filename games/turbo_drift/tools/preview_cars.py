#!/usr/bin/env python3
"""Dev preview: renders car sprite sheets to a PNG (not used by the build)."""
import os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
import art_cars as C
from pix import to_image

out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/td_prev/cars.png"
os.makedirs(os.path.dirname(out), exist_ok=True)
which = sys.argv[2] if len(sys.argv) > 2 else "all"
t0 = time.time()
rows = []
for i, spec in enumerate(C.PLAYER_CARS):
    if which not in ("all", spec["name"]) and which != "player":
        continue
    pal = C.car_palette(C.PAINTS[i], driver=True)
    fr = C.player_frames(spec)
    row = Image.new("RGB", (128 * 7, 64 + 64), (60, 64, 80))
    for k, f in enumerate(fr[:7]):
        row.paste(to_image(f, pal, 1, (70, 74, 92)), (k * 128, 0))
    for k, f in enumerate(fr[7:]):
        row.paste(to_image(f, pal, 1, (70, 74, 92)), (k * 128, 32))
    g = C.garage_frame(spec)
    row.paste(to_image(g, pal, 1, (70, 74, 92)), (0, 64))
    rv = C.rival_frames(spec)
    x = 140
    for yawm in rv:
        for m in yawm:
            row.paste(to_image(m, C.car_palette(C.PAINTS[(i + 3) % 8]), 1, (70, 74, 92)), (x, 64))
            x += m.w + 4
    rows.append(row)
    print(spec["name"], "%.1fs" % (time.time() - t0))
if which in ("all", "traffic"):
    row = Image.new("RGB", (128 * 7, 64), (60, 64, 80))
    x = 0
    for j, spec in enumerate(C.TRAFFIC_CARS):
        for m in C.traffic_frames(spec):
            row.paste(to_image(m, C.car_palette(C.TRAFFIC_PAINTS[j]), 1, (70, 74, 92)), (x, 0))
            x += m.w + 4
    rows.append(row)
H = sum(r.height for r in rows)
sheet = Image.new("RGB", (128 * 7, H))
y = 0
for r in rows:
    sheet.paste(r, (0, y)); y += r.height
sheet = sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST)
sheet.save(out)
print("done %.1fs" % (time.time() - t0), out)
