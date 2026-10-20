#!/usr/bin/env python3
"""Dev preview: renders all creature frames/icons into a scaled sheet."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
import art_mon as M
from pix import hexrgb

out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/verify/mm/mons.png"
n = len(M.ROSTER)
cols = 6
cw, ch = 64 * 2 + 32 * 2 + 8, 72
im = Image.new("RGB", (cols * cw, ((n + cols - 1) // cols) * ch), (236, 246, 236))
for i in range(n):
    pal = [hexrgb(h) for h in M.pal(i)]
    ox, oy = (i % cols) * cw, (i // cols) * ch
    for f in range(2):
        px = M.render(i, "battle", f)
        for y in range(64):
            for x in range(64):
                if px[y][x]:
                    im.putpixel((ox + f * 64 + x, oy + y), pal[px[y][x]])
        ic = M.render(i, "icon", f)
        for y in range(32):
            for x in range(32):
                if ic[y][x]:
                    im.putpixel((ox + 128 + f * 32 + x, oy + y), pal[ic[y][x]])
scale = int(sys.argv[2]) if len(sys.argv) > 2 else 2
im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
im.save(out)
print(out, im.size)
