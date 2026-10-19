import sys
from PIL import Image
import art_sprites as A

def to_img(layer, pal, bg=(200,230,255)):
    im = Image.new("RGB", (layer.w, layer.h), bg)
    for y in range(layer.h):
        for x in range(layer.w):
            c = layer.px[y][x]
            if c: im.putpixel((x, y), pal[c])
    return im

def sheet(rows, scale=6):
    W = max(sum(l.w + 2 for l, p in r) for r in rows)
    H = sum(max(l.h for l, p in r) + 2 for r in rows)
    im = Image.new("RGB", (W, H), (200, 230, 255))
    y = 0
    for r in rows:
        x = 0
        for l, p in r:
            im.paste(to_img(l, p), (x, y)); x += l.w + 2
        y += max(l.h for l, p in r) + 2
    return im.resize((W*scale, H*scale), Image.NEAREST)

def main():
  rows = [
   [(f, A.PAL_MOCHI) for f in A.mochi_frames()],
   [(f, A.PAL_STAR) for f in A.star_frames()] + [(A.drop_frame(0), A.PAL_DROP), (A.drop_frame(1), A.PAL_DROP), (A.drop_frame(flat=True), A.PAL_DROP),
    (A.bee_frame(0), A.PAL_BEE), (A.bee_frame(1), A.PAL_BEE), (A.spiky_frame(0), A.PAL_SPIKY), (A.spiky_frame(1), A.PAL_SPIKY),
    (A.sprout_frame(0), A.PAL_SPROUT), (A.sprout_frame(1), A.PAL_SPROUT)],
   [(A.flag_frame(i), A.PAL_PROPS) for i in range(3)] + [(A.goal_frame(0), A.PAL_PROPS), (A.goal_frame(1), A.PAL_PROPS), (A.spring_frame(0), A.PAL_PROPS), (A.spring_frame(1), A.PAL_PROPS)] + [(A.node_frame(i), A.PAL_PROPS) for i in range(4)],
   [(f, A.PAL_FX) for f in A.fx_frames()],
  ]
  sheet(rows, 5).save(sys.argv[1])


if __name__ == '__main__':
    main()
