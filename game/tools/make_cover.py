#!/usr/bin/env python3
"""Composes the 480x320 store cover for Mochi Hop from the real game layers/sprites
(240x160 scene, upscaled 2x with nearest neighbour)."""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image  # noqa: E402
import art_bg as B  # noqa: E402
import art_sprites as A  # noqa: E402


def blit(im, layer, pal, ox, oy, wrap=False):
    W, H = im.size
    for y in range(layer.h):
        for x in range(layer.w):
            c = layer.px[y][x]
            if not c:
                continue
            X, Y = ox + x, oy + y
            if wrap:
                X %= W
            if 0 <= X < W and 0 <= Y < H:
                im.putpixel((X, Y), pal[c])


out = sys.argv[1]
im = Image.new("RGB", (240, 160))
far, fp = B.far_layer(0)
mid, mp = B.mid_layer(0)
blit(im, far.crop(40, 62, 240, 160), fp, 0, 0)
blit(im, mid.crop(60, 52, 240, 160), mp, 0, 0)
grid = [
    "...............",
    "...............",
    "...........=== ".replace(" ", "."),
    "...............",
    "..f..t....m..t.",
    "###############",
    "###############",
]
terr = B.render_terrain(grid, 0)
blit(im, terr, B.THEME_TERRAIN_PALS[0], 0, 160 - terr.h + 0)
logo = B.logo_layer()
blit(im, logo, B.LOGO_PAL, 4, 6)
ground = 160 - terr.h + 5 * 16
# Mochi mid-hop (happy), sprouts following, stars to collect, a grumpy raindrop
m = A.mochi_frames()
blit(im, m[8], A.PAL_MOCHI, 60, ground - 32 - 14)
spr = [A.sprout_frame(0), A.sprout_frame(1)]
blit(im, spr[0].hflip(), A.PAL_SPROUT, 30, ground - 16)
blit(im, spr[1].hflip(), A.PAL_SPROUT, 14, ground - 17)
stars = A.star_frames()
for i, (x, y) in enumerate(((104, 84), (124, 76), (144, 72), (164, 76))):
    blit(im, stars[[0, 1, 0, 3][i]], A.PAL_STAR, x, y)
blit(im, A.drop_frame(0), A.PAL_DROP, 196, ground - 16)
blit(im, A.bee_frame(0), A.PAL_BEE, 206, 70)
fx = A.fx_frames()
for x, y in ((92, 104), (54, 90), (100, 70)):
    blit(im, fx[3], A.PAL_FX, x, y)
im.resize((480, 320), Image.NEAREST).save(out, optimize=True)
print("cover written", out)
