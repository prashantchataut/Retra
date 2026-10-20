#!/usr/bin/env python3
"""Cover art for Retra's own games (Retra Originals), in the same pixel-art style as the catalog
covers. Retra Originals ship inside the APK, so their covers live next to the ROM in
`app/src/main/assets/games/<id>_cover.png` (Mochi Hop composes its own from real game layers).

    PYTHONDONTWRITEBYTECODE=1 python3 tools/covers/make_original_covers.py [--sheet /tmp/x.png]
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw  # noqa: E402

import font  # noqa: E402
from px import H, W, Canvas, hexc, mix  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "games")


def logo(cv, kicker, lines, fills, outline="#12060E", y=12):
    font.title_block(cv, lines, 12, y, fills, outline, "#000000" if outline == "#12060E" else outline,
                     kicker=kicker, kicker_color="#FFFFFF")


def turbo_drift(cv):
    # Neon night city, seen from the road: horizon, skyline, palms, perspective asphalt, the car.
    cv.vgrad(0, 96, ["#2A0E3A", "#5A1E6E", "#C94A8E"], steps=7)
    cv.stars(40, ["#FFFFFF", "#FFD2F0"], ymax=70, seed=301)
    cv.disc(196, 30, 14, "#FFF0C9")
    cv.glow(196, 30, 44, "#FFC2E0", 0.45)
    r = __import__("random").Random(303)
    x = 0
    while x < W:
        w, h = r.randint(10, 22), r.randint(18, 62)
        cv.building(x, 96, w, h, "#2A1240", "#1A0A28", "#5AD8FF" if r.random() < 0.5 else "#FF7AAE", lit=0.4, seed=x)
        x += w + 1
    cv.rect(0, 96, W, 116, "#160A22")
    # Road: perspective trapezoid with cyan/magenta edges and a dashed centre line.
    cv.poly([(98, 96), (142, 96), (232, H), (8, H)], "#241436")
    cv.poly([(96, 96), (100, 96), (24, H), (0, H)], "#1A0E28")
    cv.poly([(140, 96), (144, 96), (W, H), (216, H)], "#1A0E28")
    for (x0, x1, col) in ((98, 102, "#5AD8FF"), (138, 144, "#FF7AAE")):
        cv.poly([(x0, 96), (x1, 96), (x1 + 84, H), (x0 + 66, H)], col)
    for i in range(7):
        t = (i / 7) ** 1.9
        y = int(100 + t * 58)
        w = int(2 + t * 10)
        h = int(1 + t * 5)
        cv.rect(120 - w // 2, y, 120 + w // 2, y + h, "#FFE2A0")
    for (lx, col) in ((58, "#5AD8FF"), (182, "#FF7AAE")):
        cv.rect(lx, 96 - 34, lx + 2, 112, "#3A1E4E")
        for dy in range(0, 34, 7):
            cv.rect(lx - 2, 96 - 34 + dy, lx + 4, 96 - 31 + dy, col)
    # Car (original design, seen from behind).
    cx, cy = 120, 128
    cv.rect(cx - 22, cy - 8, cx + 22, cy + 14, "#E23A4A")
    cv.rect(cx - 22, cy - 8, cx + 22, cy - 6, "#FF7A84")
    cv.rect(cx - 20, cy + 10, cx + 20, cy + 16, "#B01E30")
    cv.poly([(cx - 18, cy - 8), (cx - 12, cy - 20), (cx + 12, cy - 20), (cx + 18, cy - 8)], "#3A2A4A")
    cv.poly([(cx - 14, cy - 10), (cx - 10, cy - 18), (cx + 10, cy - 18), (cx + 14, cy - 10)], "#7AD2F0")
    cv.rect(cx - 20, cy + 2, cx - 12, cy + 7, "#FFE24A")
    cv.rect(cx + 12, cy + 2, cx + 20, cy + 7, "#FFE24A")
    cv.rect(cx - 30, cy + 12, cx - 20, cy + 20, "#1A0E22")
    cv.rect(cx + 20, cy + 12, cx + 30, cy + 20, "#1A0E22")
    for k in range(10):
        cv.glow(cx - 34 - k * 2, cy + 30 + k, 3, "#FF7AAE", 0.4)
        cv.glow(cx + 34 + k * 2, cy + 30 + k, 3, "#5AD8FF", 0.4)
    # Nitro flame behind the car.
    for k in range(9):
        cv.glow(120, cy + 22 + k * 3, max(1, 5 - k // 2), "#5AD8FF" if k % 2 else "#FFFFFF", 0.6)
    logo(cv, "RETRA ORIGINAL", ["TURBO DRIFT"], ("#FFF0F6", "#FF9EC2"))


def sky_sprout(cv):
    # Blossom sky: parchment cloud sea, blossom trees, the leaf-copter and a wave of raiders.
    cv.vgrad(0, 110, ["#7EC8F2", "#BFE7FA", "#F7E2EE"], steps=7)
    cv.cloud(16, 34, 54, "#FFFFFF", "#D9EDF7")
    cv.cloud(140, 24, 44, "#FFFFFF", "#D9EDF7")
    cv.cloud(96, 58, 36, "#FFFFFF", "#D9EDF7")
    # Ragged parchment cloud sea, three parallax bands.
    for band, (base, amp, freq, col, shade) in enumerate(
        ((96, 9, 0.05, "#EFE2F2", "#C9B8D9"), (110, 8, 0.08, "#F7EFFA", "#D9C9E8"), (126, 7, 0.12, "#FFFFFF", "#E8DCF2"))
    ):
        n = cv.noise(310 + band, 2)
        for x in range(W):
            top = int(base - n(x * freq) * amp)
            for y in range(top, H):
                cv.px[x, y] = hexc(col)
            cv.px[x, top] = hexc(shade)
    r = __import__("random").Random(313)
    for (cx, cy) in ((34, 104), (206, 100), (150, 118)):
        cv.rect(cx - 1, cy - 16, cx + 1, cy + 6, "#6E4A3A")
        for (dx, dy, rad) in ((0, -20, 11), (-11, -14, 9), (11, -14, 9), (-6, -26, 8), (7, -27, 8)):
            cv.disc(cx + dx, cy + dy, rad, "#FFAECB", shade="#E87AA6", light="#FFDCE8")
            cv.dither_rect(int(cx + dx - rad), int(cy + dy - rad), int(cx + dx + rad), int(cy + dy + rad), "#FFFFFF", 0.12)
    # Leaf-copter (Mochi): round pink body, leaf rotor, little goggles.
    px_, py = 92, 92
    cv.rect(px_ - 1, py - 22, px_ + 1, py - 12, "#3D8A58")
    for dx in (-9, 9):
        cv.disc(px_ + dx, py - 24, 7, "#6FCF7A", shade="#3D8A58", light="#B9F2A2")
    cv.disc(px_, py, 12, "#F9B3C9", shade="#E8849F", light="#FFD6E2")
    cv.disc(px_, py, 12, "#F9B3C9")
    cv.disc(px_ - 3, py + 1, 2, "#35203F")
    cv.disc(px_ + 3, py + 1, 2, "#35203F")
    cv.rect(px_ - 6, py - 3, px_ + 6, py, "#7AD2F0")
    cv.rect(px_ - 7, py + 8, px_ + 7, py + 11, "#E8849F")
    for k in range(6):
        cv.glow(px_ - 16 - k * 3, py + 12 + k, 3, "#FFE24A", 0.5)
    for (ex, ey) in ((180, 60), (204, 72), (166, 44), (216, 48)):
        cv.disc(ex, ey, 7, "#8E4AC9", shade="#5A2A8E", light="#C9A0F5")
        cv.disc(ex + 1, ey - 1, 2, "#FFE24A")
        cv.rect(ex - 9, ey - 1, ex - 4, ey + 1, "#C9A0F5")
        cv.rect(ex + 4, ey - 1, ex + 9, ey + 1, "#C9A0F5")
    for (px2, py2) in ((150, 84), (208, 106)):
        cv.rect(px2 - 1, py2 - 3, px2 + 1, py2 + 3, "#5AD8FF")
        cv.disc(px2, py2 - 4, 3, "#FFE24A")
    logo(cv, "RETRA ORIGINAL", ["SKY SPROUT"], ("#FFFFFF", "#FFC2DE"), outline="#3A1042")


COVERS = {"turbo_drift": turbo_drift, "sky_sprout": sky_sprout}


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet_path = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    made = []
    for i, (name, fn) in enumerate(sorted(COVERS.items())):
        cv = Canvas(seed=500 + i)
        fn(cv)
        path = os.path.join(OUT, name + "_cover.png")
        cv.save(path)
        made.append((name, path))
        print("%-16s %6d bytes" % (name, os.path.getsize(path)))
    if sheet_path:
        sheet = Image.new("RGB", (2 * 490, 330), (30, 30, 30))
        for k, (name, path) in enumerate(made):
            sheet.paste(Image.open(path).convert("RGB"), (k * 490 + 5, 5))
            ImageDraw.Draw(sheet).text((k * 490 + 8, 322), name, fill=(230, 230, 230))
        sheet.save(sheet_path)


if __name__ == "__main__":
    main()
