#!/usr/bin/env python3
"""Dev preview of UI art (not used by the build)."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image
import art_ui as U
from pix import to_image
sheet = Image.new("RGB", (480, 300), (40, 50, 90))
x = 0
for i, t in enumerate(U.font_tiles()):
    sheet.paste(to_image(t, U.UI_MAIN, 1, (40, 50, 90)), ((i % 40) * 8, (i // 40) * 8))
for i, d in enumerate("0123456789"):
    sheet.paste(to_image(U.big_digit(d), U.UI_MAIN, 1, (40, 50, 90)), (i * 9, 20))
for i, t in enumerate(U.panel_tiles()):
    sheet.paste(to_image(t, U.UI_MAIN), (100 + (i % 3) * 8, 20 + (i // 3) * 8))
for i, t in enumerate(U.bar_tiles()):
    sheet.paste(to_image(t, U.UI_MAIN, 1, (40, 50, 90)), (140 + i * 8, 20))
logo = U.logo_layer()
sheet.paste(to_image(logo, U.LOGO_PAL, 1, (40, 50, 90)), (0, 50))
fin = U.banner_layer("FINISH")
sheet.paste(to_image(fin, U.LOGO_PAL, 1, (40, 50, 90)), (250, 50))
for i, m in enumerate(("happy", "cheer", "focus", "sad")):
    sheet.paste(to_image(U.mochi_portrait(m), U.MOCHI_PAL, 1, (40, 50, 90)), (250 + i * 34, 90))
fx = U.fx_tiles()
x = 0
for k, v in fx.items():
    for l in v:
        sheet.paste(to_image(l, U.FX_PAL, 1, (40, 50, 90)), (x, 120))
        x += l.w + 2
for i, l in enumerate(U.countdown_layers()):
    sheet.paste(to_image(l, U.HUDSPR_PAL, 1, (40, 50, 90)), (i * 36, 160))
for i, l in enumerate(U.dot_tiles()):
    sheet.paste(to_image(l, U.HUDSPR_PAL, 1, (40, 50, 90)), (200 + i * 10, 160))
sheet = sheet.resize((960, 600), Image.NEAREST)
sheet.save(sys.argv[1] if len(sys.argv) > 1 else "/tmp/verify/td/ui.png")
