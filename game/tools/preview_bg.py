import sys
from PIL import Image
import art_bg as B, levels as LV
def img(layer, pal, im=None, ox=0, oy=0, wrapw=None):
    if im is None: im = Image.new("RGB", (layer.w, layer.h), pal[0])
    W, H = im.size
    for y in range(H):
        for x in range(W):
            sx = (x + ox) % layer.w if wrapw else x + ox
            sy = y + oy
            if 0 <= sx < layer.w and 0 <= sy < layer.h:
                c = layer.px[sy][sx]
                if c: im.putpixel((x, y), pal[c])
    return im
lv = LV.all_levels()
out = Image.new("RGB", (480, 3 * 200))
for t, li in enumerate((0, 2, 4)):
    L = lv[li]
    far, fp = B.far_layer(t); mid, mp = B.mid_layer(t)
    terr = B.render_terrain(L.grid(), t)
    im = Image.new("RGB", (480, L.h*16 - 0))
    camy = L.h*16 - 200
    im = Image.new("RGB", (480, 200))
    img(far, fp, im, 0, camy//4 + 40, True)
    img(mid, mp, im, 0, camy//2 + 30, True)
    img(terr, B.THEME_TERRAIN_PALS[t], im, int(sys.argv[2]) if len(sys.argv)>2 else 0, camy)
    out.paste(im, (0, t*200))
out.resize((960, 1200), Image.NEAREST).save(sys.argv[1])
