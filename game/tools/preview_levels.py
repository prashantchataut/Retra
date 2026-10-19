import sys
from PIL import Image
import art_bg as B, levels as LV, art_sprites as A
from preview import to_img
icons = {1: (A.star_frame(1.0), A.PAL_STAR), 2: (A.sprout_frame(0), A.PAL_SPROUT), 3: (A.drop_frame(0), A.PAL_DROP),
         4: (A.bee_frame(0), A.PAL_BEE), 5: (A.spiky_frame(0), A.PAL_SPIKY), 6: (A.flag_frame(1), A.PAL_PROPS),
         7: (A.goal_frame(0), A.PAL_PROPS), 8: (A.spring_frame(0), A.PAL_PROPS)}
def paste_t(im, l, pal, x, y):
    for yy in range(l.h):
        for xx in range(l.w):
            c = l.px[yy][xx]
            if c and 0 <= x+xx < im.width and 0 <= y+yy < im.height: im.putpixel((x+xx, y+yy), pal[c])
lv = LV.all_levels()
idx = int(sys.argv[2])
L = lv[idx]
terr = B.render_terrain(L.grid(), L.theme)
bgc = {0: (190, 225, 255), 1: (110, 70, 120), 2: (40, 40, 90)}[L.theme]
im = Image.new("RGB", (terr.w, terr.h), bgc)
paste_t(im, terr, B.THEME_TERRAIN_PALS[L.theme], 0, 0)
for t, x, y in L.objs:
    l, p = icons[t]
    bx, by = x*16+8, (y+1)*16
    paste_t(im, l, p, bx - l.w//2, by - l.h)
m = Image.new("RGB", (64, 32), (0, 0, 0))
sx, sy = L.start
paste_t(im, A.mochi_frames()[0], A.PAL_MOCHI, sx*16+8-16, (sy+1)*16-32)
half = (terr.w + 1) // 2
out = Image.new("RGB", (half, terr.h*2 + 4), (0,0,0))
out.paste(im.crop((0,0,half,terr.h)), (0,0)); out.paste(im.crop((half,0,terr.w,terr.h)), (0,terr.h+4))
out.save(sys.argv[1])
