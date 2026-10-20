#!/usr/bin/env python3
"""Original cover art for the Retra Game Library catalog entries (pixel art, 240x160 -> 480x320).

No franchise characters, logos or items: each cover evokes its game through setting, mood and colour.
Deterministic (seeded); regenerate with:

    PYTHONDONTWRITEBYTECODE=1 python3 tools/covers/make_covers.py [--sheet /tmp/covers-sheet.png]
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw  # noqa: E402

import font  # noqa: E402
from px import BAYER, H, W, Canvas, hexc, mix  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "covers")

CREAM = ("#FFF7E8", "#FFD9A8")
ICE = ("#F4FBFF", "#BFE3F5")
MINT = ("#F2FFF6", "#B9EFCF")
ROSE = ("#FFF2F5", "#FFC2D1")
LILAC = ("#FBF4FF", "#D9C6F5")
GOLD = ("#FFFBE0", "#FFD45C")


def title(cv, kicker, lines, fills=CREAM, outline="#1B1420", shadow="#0B0810", y=10, align="left"):
    font.title_block(cv, lines, 10, y, fills, outline, shadow, kicker=kicker, kicker_color="#FFFFFF", align=align)


def sea(cv, y0, deep, mid, light, seed):
    cv.vgrad(y0, H, [mid, deep], steps=5)
    r = random.Random(seed)
    for _ in range(60):
        x, y = r.randrange(W), r.randrange(y0 + 2, H)
        w = r.randint(3, 10)
        for i in range(w):
            cv.put(x + i, y, hexc(light))


# ---- covers --------------------------------------------------------------------------------

def heart_soul(cv):
    cv.vgrad(0, 110, ["#2C1B4D", "#7A3B6E", "#F08A5D", "#FFD08A"], steps=8)
    cv.disc(178, 82, 20, "#FFE7B0")
    cv.glow(178, 82, 46, "#FFC27A", 0.5)
    cv.ridge(3, 112, 26, 0.012, "#5A3A63", rim="#8E5A7A")
    cv.ridge(4, 124, 18, 0.02, "#3E2848")
    # pagoda
    px_, base = 66, 124
    tiers = [(40, 10), (34, 9), (28, 9), (22, 8), (16, 8)]
    y = base
    for i, (w, h) in enumerate(tiers):
        cv.rect(px_ - w // 2 + 4, y - h, px_ + w // 2 - 4, y, "#7A2E2E")
        cv.rect(px_ - w // 2 + 4, y - h, px_ - w // 2 + 7, y, "#A64434")
        cv.poly([(px_ - w // 2 - 4, y - h), (px_ + w // 2 + 4, y - h), (px_ + w // 2 - 2, y - h - 5), (px_ - w // 2 + 2, y - h - 5)], "#2A1A26")
        y -= h + 5
    cv.rect(px_ - 1, y - 12, px_ + 1, y, "#E8B85A")
    for wx in range(px_ - 12, px_ + 13, 6):
        cv.rect(wx, base - 7, wx + 2, base - 3, "#FFD27A")
    cv.ridge(5, 140, 8, 0.05, "#2A1A2E")
    r = random.Random(7)
    for _ in range(14):
        x = r.randrange(120, 240)
        cv.round_tree(x, 150 + r.randrange(6), r.randint(5, 9), "#D9643A", "#9C3A2A", "#F4A35A", "#3A2020")
    for _ in range(40):
        cv.put(r.randrange(W), r.randrange(40, 150), hexc(r.choice(["#F4A35A", "#FFD08A", "#D9643A"])))
    title(cv, "POKEMON", ["HEART & SOUL"], fills=CREAM)


def unbound(cv):
    cv.vgrad(0, 120, ["#050A1F", "#0E1D45", "#163A6B"], steps=6)
    cv.stars(90, ["#FFFFFF", "#BFD8FF", "#8FB5FF"], ymax=100, seed=11)
    for band, (col, off) in enumerate([("#3EF0B5", 0), ("#6BE8FF", 9), ("#B98CFF", 18)]):
        for x in range(W):
            y = 46 + off + 10 * math.sin(x * 0.035 + band) + 6 * math.sin(x * 0.011 + 2 * band)
            for k in range(26):
                if BAYER[(int(y) + k) & 3][x & 3] < (1 - k / 26) * 9:
                    cv.put(x, int(y) + k, hexc(col))
    cv.ridge(21, 120, 48, 0.008, "#1C2B52", rim="#E8F2FF", sharp=True)
    cv.ridge(22, 132, 30, 0.014, "#111A36", rim="#8FA8D9", sharp=True)
    cv.rect(0, 140, W, H, "#0A1026")
    r = random.Random(23)
    for _ in range(26):
        x = r.randrange(W)
        cv.pine(x, 148 + r.randrange(10), r.randint(10, 20), "#16264A", "#0A1226", "#2E4A7A")
    title(cv, "POKEMON", ["UNBOUND"], fills=ICE)


def radical_red(cv):
    cv.vgrad(0, 120, ["#2A0508", "#7A0F12", "#E0402A", "#FF9A4A"], steps=8)
    cv.glow(150, 70, 70, "#FF6A2A", 0.55)
    # volcano
    cv.poly([(80, 140), (140, 58), (162, 58), (232, 140)], "#2A0E10")
    cv.poly([(140, 58), (162, 58), (176, 80), (128, 80)], "#3E1414")
    for i, x in enumerate(range(142, 160, 3)):
        cv.poly([(x, 58), (x + 3, 58), (x + 6 - i, 120), (x - 2 + i, 120)], "#FF5A1E")
    cv.rect(140, 54, 162, 59, "#FFD25A")
    r = random.Random(31)
    for _ in range(70):
        x, y = 151 + r.gauss(0, 18), 54 - abs(r.gauss(0, 26))
        cv.put(int(x), int(y), hexc(r.choice(["#FFD25A", "#FF8A3A", "#FFFFFF"])))
    cv.ridge(32, 138, 24, 0.02, "#1A0608", rim="#5A1A14", sharp=True)
    cv.rect(0, 146, W, H, "#120406")
    for x in range(0, W, 2):
        if r.random() < 0.3:
            cv.put(x, 145, hexc("#FF5A1E"))
    title(cv, "POKEMON", ["RADICAL RED"], fills=ROSE, outline="#1A0406")


def emerald_rogue(cv):
    cv.vgrad(0, H, ["#0D1A16", "#163329", "#0A1410"], steps=5)
    for y in range(0, 150, 10):
        off = 0 if (y // 10) % 2 else 10
        for x in range(-off, W, 20):
            cv.rect(x + 1, y + 1, x + 19, y + 9, "#1E3B31")
            cv.rect(x + 1, y + 8, x + 19, y + 9, "#14291F")
    for i, cx in enumerate((60, 120, 180)):
        glow = ["#6BFFB8", "#5AD8FF", "#FFD25A"][i]
        cv.glow(cx, 110, 34, glow, 0.5)
        cv.rect(cx - 16, 84, cx + 16, 146, "#0A1410")
        cv.disc(cx, 84, 16, "#0A1410")
        cv.rect(cx - 12, 88, cx + 12, 146, mix(hexc(glow), (10, 20, 16), 0.55))
        cv.disc(cx, 88, 12, mix(hexc(glow), (10, 20, 16), 0.55))
        cv.dither_rect(cx - 12, 76, cx + 12, 146, glow, 0.25)
        cv.rect(cx - 2, 74, cx + 2, 78, "#C9F5DC")
    for tx in (30, 90, 150, 210):
        cv.rect(tx - 1, 96, tx + 1, 108, "#5A4030")
        cv.disc(tx, 93, 3, "#FFB23A")
        cv.glow(tx, 93, 14, "#FFB23A", 0.4)
    cv.rect(0, 146, W, H, "#081210")
    title(cv, "POKEMON", ["EMERALD ROGUE"], fills=MINT)


def gaia(cv):
    cv.vgrad(0, 120, ["#7EC8F2", "#BDE7F7", "#FFF2C9"], steps=6)
    cv.disc(196, 34, 13, "#FFF6C9")
    cv.glow(196, 34, 40, "#FFF2B0", 0.5)
    cv.cloud(24, 44, 40, "#FFFFFF", "#D9EEF7")
    cv.cloud(120, 30, 30, "#FFFFFF", "#D9EEF7")
    cv.ridge(41, 118, 22, 0.01, "#8FC9A0", rim="#C9EFC9")
    cv.ridge(42, 132, 16, 0.016, "#5AA86E", rim="#8FD98F")
    # world tree on the hill
    cv.poly([(146, 128), (154, 128), (158, 84), (142, 84)], "#6B4A2E")
    for (dx, dy, r) in ((0, 70, 26), (-22, 80, 18), (22, 80, 18), (-10, 58, 16), (12, 58, 16)):
        cv.disc(150 + dx, dy, r, "#4FA35A", shade="#2E7A3E", light="#9BE08A")
    cv.rect(0, 136, W, H, "#3E8A4E")
    r = random.Random(43)
    for _ in range(90):
        cv.put(r.randrange(W), r.randrange(137, H), hexc(r.choice(["#6FC27A", "#2E6E3A", "#FFE38A"])))
    title(cv, "POKEMON", ["GAIA"], fills=CREAM, outline="#173220")


def light_platinum(cv):
    cv.vgrad(0, 120, ["#2E2A5A", "#6E6AA8", "#D9D2F5", "#F7F2FF"], steps=8)
    cv.glow(120, 60, 80, "#FFFFFF", 0.35)
    for (x, h, w) in ((40, 70, 16), (70, 96, 20), (100, 62, 14), (150, 104, 22), (185, 76, 16), (212, 58, 12)):
        cv.poly([(x - w, 140), (x, 140 - h), (x + w, 140)], "#BFC9F5")
        cv.poly([(x, 140 - h), (x + w, 140), (x + 2, 140)], "#8E98D9")
        cv.poly([(x - 2, 140 - h + 6), (x, 140 - h), (x - w // 3, 140 - h // 2)], "#FFFFFF")
    cv.rect(0, 138, W, H, "#E8E4FF")
    cv.dither_rect(0, 138, W, H, "#C9C2F5", 0.3)
    cv.stars(50, ["#FFFFFF", "#E8E4FF"], ymax=80, seed=51)
    title(cv, "POKEMON", ["LIGHT PLATINUM"], fills=LILAC, outline="#1E1A40")


def clover(cv):
    cv.vgrad(0, H, ["#CFF5C9", "#8FD98A", "#3E9A4E"], steps=6)
    r = random.Random(61)
    for _ in range(26):
        x, y, s = r.randrange(-10, W), r.randrange(30, H), r.randint(6, 14)
        col = r.choice([("#3E9A4E", "#2A6E36", "#8FE08A"), ("#5AB85E", "#3E8A44", "#B9F2A2")])
        for a in range(4):
            ang = a * math.pi / 2 + r.random() * 0.3
            cv.disc(x + math.cos(ang) * s * 0.6, y + math.sin(ang) * s * 0.6, s * 0.55, col[0], shade=col[1], light=col[2])
        cv.rect(x, y, x + 1, y + s, "#2A6E36")
    cv.disc(176, 96, 26, "#FFE36B", shade="#E8B83A", light="#FFF7C9")
    for a in range(4):
        ang = a * math.pi / 2 + 0.785
        cv.disc(176 + math.cos(ang) * 16, 96 + math.sin(ang) * 16, 14, "#7ED96A", shade="#4FA84A", light="#C9F7B0")
    cv.disc(176, 96, 6, "#FFE36B")
    title(cv, "POKEMON", ["CLOVER"], fills=CREAM, outline="#173220")


def seaglass(cv):
    cv.vgrad(0, 84, ["#9BE3F0", "#D9F7F2"], steps=5)
    cv.cloud(30, 30, 46, "#FFFFFF", "#C9EDEA")
    cv.cloud(150, 22, 36, "#FFFFFF", "#C9EDEA")
    sea(cv, 84, "#2A8A9A", "#5AC8C9", "#C9FFF5", 71)
    cv.poly([(0, 126), (W, 112), (W, H), (0, H)], "#F2E2BF")
    cv.dither_rect(0, 118, W, H, "#E3CC9E", 0.3)
    r = random.Random(73)
    for _ in range(26):
        x, y = r.randrange(W), r.randrange(122, H - 3)
        col = r.choice(["#7EE8D2", "#9BD9FF", "#B9F2A2", "#FFFFFF", "#7EC8E3"])
        cv.poly([(x, y), (x + r.randint(3, 6), y + 1), (x + r.randint(2, 5), y + r.randint(3, 5)), (x - 1, y + 3)], col)
        cv.put(x + 1, y + 1, (255, 255, 255))
    title(cv, "POKEMON", ["EMERALD", "SEAGLASS"], fills=MINT, outline="#0E3A40")


def crest(cv):
    cv.vgrad(0, 120, ["#1E6E4E", "#5AC88E", "#C9F2D2"], steps=7)
    cv.ridge(81, 112, 30, 0.012, "#2E8A5E", rim="#7EE0A8", sharp=True)
    cv.ridge(82, 128, 18, 0.02, "#1E6040")
    cx, cy = 172, 82
    cv.poly([(cx - 26, cy - 30), (cx + 26, cy - 30), (cx + 26, cy + 4), (cx, cy + 30), (cx - 26, cy + 4)], "#0E3A26")
    cv.poly([(cx - 22, cy - 26), (cx + 22, cy - 26), (cx + 22, cy + 2), (cx, cy + 25), (cx - 22, cy + 2)], "#E8C85A")
    cv.poly([(cx - 18, cy - 22), (cx + 18, cy - 22), (cx + 18, cy), (cx, cy + 20), (cx - 18, cy)], "#1E8A5A")
    cv.poly([(cx, cy - 16), (cx + 11, cy - 4), (cx, cy + 12), (cx - 11, cy - 4)], "#7EFFC2")
    cv.poly([(cx, cy - 16), (cx - 11, cy - 4), (cx, cy - 4)], "#C9FFE3")
    cv.rect(0, 136, W, H, "#174E34")
    title(cv, "POKEMON", ["EMERALD CREST"], fills=GOLD, outline="#0E2A1E")


def imperium(cv):
    cv.vgrad(0, 120, ["#1A0E3A", "#4A2A7A", "#C98ACF", "#FFD2B0"], steps=8)
    cv.disc(120, 92, 30, "#FFE2C2")
    cv.glow(120, 92, 70, "#FFC2D8", 0.4)
    base = 132
    cv.rect(44, base - 30, 196, base, "#2A1640")
    for x in range(50, 192, 12):
        cv.rect(x, base - 26, x + 5, base, "#4A2E66")
        cv.rect(x, base - 26, x + 1, base, "#6E4A8E")
    cv.poly([(38, base - 30), (202, base - 30), (120, base - 54)], "#2A1640")
    cv.poly([(56, base - 32), (184, base - 32), (120, base - 50)], "#4A2E66")
    cv.rect(116, base - 66, 124, base - 52, "#2A1640")
    cv.disc(120, base - 66, 6, "#E8C85A")
    cv.rect(0, base, W, H, "#1A0E2A")
    for x in range(30, 210, 3):
        cv.put(x, base, hexc("#6E4A8E"))
    title(cv, "POKEMON", ["EMERALD", "IMPERIUM"], fills=LILAC, outline="#140A26")


def inclement(cv):
    cv.vgrad(0, 104, ["#141C26", "#2E3E4E", "#4E6070"], steps=6)
    for (x, y, w) in ((10, 26, 70), (90, 18, 80), (170, 30, 70), (40, 50, 90), (150, 56, 80)):
        cv.cloud(x, y, w, "#3A4A5A", "#1E2A36")
    bolt = [(150, 50), (140, 76), (150, 76), (136, 106)]
    for i in range(len(bolt) - 1):
        (x0, y0), (x1, y1) = bolt[i], bolt[i + 1]
        for t in range(40):
            x = int(x0 + (x1 - x0) * t / 40)
            y = int(y0 + (y1 - y0) * t / 40)
            cv.rect(x - 1, y, x + 1, y + 1, "#FFFFFF")
    cv.glow(144, 80, 50, "#BFE8FF", 0.4)
    sea(cv, 104, "#0E1E2A", "#2A4A5A", "#8FB5C9", 91)
    r = random.Random(93)
    for _ in range(260):
        x, y = r.randrange(W), r.randrange(H)
        for k in range(4):
            cv.put(x - k // 2, y + k, hexc("#9FC2D9"))
    title(cv, "FAN GAME", ["INCLEMENT", "EMERALD"], fills=ICE, outline="#0A1018")


def liquid_crystal(cv):
    cv.vgrad(0, 90, ["#1E3A6E", "#5A8AD9", "#BFE3FF"], steps=6)
    cv.ridge(101, 90, 34, 0.012, "#3A5A9A", rim="#D9EEFF", sharp=True)
    tops = cv.ridge(102, 92, 18, 0.02, "#2A4478")
    # reflection
    for y in range(92, H):
        for x in range(W):
            src = cv.get(x + int(2 * math.sin(y * 0.6)), 184 - y)
            c = mix(src, hexc("#2A5A9A"), 0.45)
            cv.px[x, y] = c
    for y in range(94, H, 5):
        cv.rect(0, y, W, y + 1, mix(hexc("#BFE3FF"), hexc("#2A5A9A"), 0.6))
    for (x, h) in ((176, 40), (190, 28), (164, 24)):
        cv.poly([(x - 6, 92), (x, 92 - h), (x + 6, 92)], "#9BE8FF")
        cv.poly([(x, 92 - h), (x + 6, 92), (x + 1, 92)], "#5AB8E8")
    title(cv, "POKEMON", ["LIQUID CRYSTAL"], fills=ICE, outline="#0E1E3A")


def ash_gray(cv):
    cv.vgrad(0, 110, ["#8E9AA8", "#C9CFD6", "#F2E9D9"], steps=6)
    cv.disc(60, 70, 14, "#FFF2D2")
    cv.ridge(111, 108, 20, 0.012, "#7A8A86", rim="#A8B5AE")
    cv.ridge(112, 124, 14, 0.02, "#5A6E5E")
    cv.rect(0, 126, W, H, "#6E8A5E")
    cv.poly([(108, 126), (132, 126), (200, H), (40, H)], "#C9B590")
    cv.poly([(116, 126), (124, 126), (150, H), (90, H)], "#B59E78")
    cv.rect(170, 118, 172, 136, "#4A3A2A")
    cv.rect(160, 112, 186, 120, "#8E6E4A")
    cv.rect(160, 112, 186, 113, "#B59068")
    r = random.Random(113)
    for _ in range(60):
        cv.put(r.randrange(W), r.randrange(127, H), hexc(r.choice(["#8FAA6E", "#4E6A44"])))
    title(cv, "POKEMON", ["ASH GRAY"], fills=("#FFFFFF", "#D9D9D9"), outline="#1E2226")


def rocket_edition(cv):
    cv.vgrad(0, H, ["#0A0A14", "#1E1A2E", "#3A2030"], steps=6)
    for (x0, ang) in ((60, -0.5), (180, 0.45)):
        for t in range(120):
            for w in range(-6, 7):
                x = int(x0 + math.sin(ang) * -t + w * (1 + t / 60))
                y = 140 - t
                if BAYER[y & 3][x & 3] < 5:
                    cv.put(x, y, hexc("#FF5A5A"))
    r = random.Random(121)
    x = 0
    while x < W:
        w, h = r.randint(14, 28), r.randint(30, 90)
        cv.building(x, 148, w, h, "#16121E", "#0C0A12", "#FFC24A", lit=0.25, seed=x)
        x += w + 1
    cv.rect(0, 148, W, H, "#0A0810")
    title(cv, "POKEMON FIRERED", ["ROCKET", "EDITION"], fills=ROSE, outline="#14080A")


def dark_rising(cv):
    cv.vgrad(0, 120, ["#050308", "#1E0E2E", "#4A1E4A"], steps=6)
    cv.disc(150, 64, 30, "#FFE2F0")
    cv.disc(160, 58, 28, "#140A1E")
    cv.glow(150, 64, 56, "#C94A9A", 0.35)
    cv.stars(40, ["#FFFFFF", "#D9B8FF"], ymax=100, seed=131)
    for (x, h) in ((30, 50), (52, 64), (74, 40), (176, 58), (200, 46), (222, 70)):
        cv.rect(x - 6, 140 - h, x + 6, 140, "#1A0E1E")
        cv.rect(x - 8, 140 - h - 4, x + 8, 140 - h, "#24142A")
        cv.rect(x - 2, 140 - h + 10, x + 2, 140 - h + 18, "#5A1E5A")
    cv.ridge(132, 146, 10, 0.03, "#0A050E")
    title(cv, "POKEMON", ["DARK RISING"], fills=LILAC, outline="#0A050E")


def emerald_kaizo(cv):
    cv.vgrad(0, 120, ["#1E0505", "#5A0E0E", "#B83A1E"], steps=6)
    cv.ridge(141, 150, 110, 0.03, "#0E0606", rim="#7A1E14", sharp=True, octaves=4)
    cv.ridge(142, 160, 40, 0.06, "#050202", sharp=True)
    r = random.Random(143)
    for _ in range(3):
        x = r.randrange(40, 200)
        y = 0
        while y < 90:
            nx = x + r.randint(-6, 6)
            for t in range(10):
                cv.put(int(x + (nx - x) * t / 10), y + t, hexc("#FFE2C9"))
            x, y = nx, y + 10
    title(cv, "POKEMON", ["EMERALD KAIZO"], fills=("#FFFFFF", "#FFB0A0"), outline="#140404")


def theta_ex(cv):
    cv.vgrad(0, 92, ["#3AB8E8", "#9BE3F7", "#E8FBFF"], steps=6)
    cv.cloud(20, 30, 50, "#FFFFFF", "#CFEFF7")
    sea(cv, 92, "#0E6E8A", "#2AB0C9", "#BFFFF5", 151)
    cv.poly([(110, 120), (150, 98), (200, 100), (232, 124)], "#E8D9A0")
    cv.poly([(122, 112), (150, 82), (186, 86), (214, 114)], "#3E9A4E")
    for x in (138, 160, 196):
        cv.rect(x, 74, x + 2, 104, "#6E4A2E")
        for a in range(5):
            ang = -math.pi + a * math.pi / 4
            cv.poly([(x + 1, 74), (x + 1 + math.cos(ang) * 14, 74 + math.sin(ang) * 6 + 6), (x + 1 + math.cos(ang) * 10, 74 + math.sin(ang) * 4 + 8)], "#2E8A3E")
    cx, cy = 176, 50
    cv.glow(cx, cy, 34, "#7EFFC2", 0.6)
    cv.poly([(cx, cy - 18), (cx + 13, cy - 4), (cx, cy + 18), (cx - 13, cy - 4)], "#1EB86E")
    cv.poly([(cx, cy - 18), (cx - 13, cy - 4), (cx, cy - 2)], "#9BFFD2")
    cv.poly([(cx, cy - 2), (cx + 13, cy - 4), (cx, cy + 18)], "#0E7A44")
    title(cv, "POKEMON", ["THETA EMERALD", "EX"], fills=MINT, outline="#0A2A2A")


# ---- homebrew ------------------------------------------------------------------------------

def skyland(cv):
    cv.vgrad(0, H, ["#4A9AE8", "#9BD2F7", "#E8F7FF"], steps=7)
    for (x, y, w) in ((10, 120, 60), (140, 140, 70), (180, 40, 40), (60, 60, 34)):
        cv.cloud(x, y, w, "#FFFFFF", "#CFE8F7")
    for (cx, cy, w) in ((70, 96, 70), (178, 82, 50), (200, 128, 30)):
        cv.poly([(cx - w // 2, cy), (cx + w // 2, cy), (cx + w // 5, cy + w // 2), (cx - w // 6, cy + w // 2 + 6)], "#7A5A3E")
        cv.poly([(cx + w // 5, cy), (cx + w // 2, cy), (cx + w // 5, cy + w // 2)], "#5A3E2A")
        cv.rect(cx - w // 2, cy - 3, cx + w // 2, cy + 2, "#5AB85A")
        cv.rect(cx - w // 2, cy - 3, cx + w // 2, cy - 2, "#9BE08A")
    cv.rect(54, 82, 70, 93, "#E8D9BF")
    cv.poly([(52, 82), (72, 82), (62, 74)], "#C94A3A")
    cv.rect(80, 84, 90, 93, "#D9C9A8")
    cv.poly([(78, 84), (92, 84), (85, 78)], "#3A6EC9")
    cv.round_tree(100, 93, 6, "#4FA35A", "#2E7A3E", "#9BE08A", "#5A3E2A")
    cv.round_tree(176, 79, 7, "#4FA35A", "#2E7A3E", "#9BE08A", "#5A3E2A")
    title(cv, "HOMEBREW", ["SKYLAND"], fills=ICE, outline="#0E2A4A")


def apotris(cv):
    cv.vgrad(0, H, ["#0E0E2A", "#1E2050", "#3A2E6E"], steps=6)
    cv.stars(40, ["#FFFFFF", "#9BB0FF"], seed=161)
    cols = ["#5AD8FF", "#FFD25A", "#B98CFF", "#7EE88A", "#FF7A8A", "#FFA84A"]
    shapes = [[(0, 0), (1, 0), (2, 0), (3, 0)], [(0, 0), (1, 0), (0, 1), (1, 1)], [(0, 0), (1, 0), (2, 0), (1, 1)],
              [(0, 0), (0, 1), (1, 1), (2, 1)], [(1, 0), (2, 0), (0, 1), (1, 1)], [(0, 0), (1, 0), (1, 1), (2, 1)]]
    r = random.Random(163)
    for i in range(16):
        sh = shapes[i % 6]
        col = cols[i % 6]
        bx, by = r.randrange(0, 220), r.randrange(-10, 150)
        for (dx, dy) in sh:
            x, y = bx + dx * 9, by + dy * 9
            cv.rect(x, y, x + 8, y + 8, mix(hexc(col), (14, 14, 42), 0.25))
            cv.rect(x, y, x + 8, y + 1, mix(hexc(col), (255, 255, 255), 0.4))
            cv.rect(x, y, x + 1, y + 8, mix(hexc(col), (255, 255, 255), 0.25))
            cv.rect(x + 7, y, x + 8, y + 8, mix(hexc(col), (0, 0, 0), 0.3))
    cv.rect(0, 150, W, H, "#0A0A20")
    title(cv, "HOMEBREW", ["APOTRIS"], fills=("#FFFFFF", "#BFC9FF"), outline="#08081A")


def minicraft(cv):
    cv.vgrad(0, 70, ["#6EB8F2", "#BFE3FF"], steps=5)
    cv.rect(190, 18, 206, 34, "#FFF2A0")
    r = random.Random(171)
    hts = []
    h = 74
    for x in range(0, W, 8):
        h = max(56, min(90, h + r.choice((-8, 0, 0, 8))))
        hts.append(h)
        cv.rect(x, h, x + 8, h + 4, "#5AB84A")
        cv.rect(x, h + 4, x + 8, h + 20, "#8A5A3A")
        cv.rect(x, h + 20, x + 8, H, "#6E6E7A")
        for yy in range(h + 4, H, 8):
            cv.rect(x, yy, x + 8, yy + 1, mix(cv.get(x, yy + 2), (0, 0, 0), 0.2))
    for _ in range(26):
        x, y = r.randrange(0, W, 8), r.randrange(110, H - 8, 8)
        cv.rect(x + 2, y + 2, x + 6, y + 6, hexc(r.choice(["#5AD8FF", "#FFD25A", "#FF6A4A", "#E8E8E8"])))
    cv.rect(96, 118, 152, 142, "#1A1A22")
    cv.rect(100, 122, 148, 142, "#24242E")
    cv.glow(124, 132, 18, "#FFB23A", 0.5)
    for i, x in enumerate(range(0, W, 8)):
        if r.random() < 0.18:
            cv.rect(x + 3, hts[i] - 10, x + 5, hts[i], "#6E4A2E")
            cv.rect(x, hts[i] - 18, x + 8, hts[i] - 9, "#3E9A3E")
    title(cv, "HOMEBREW", ["MINICRAFT"], fills=CREAM, outline="#1A1408")


def ucity(cv):
    cv.vgrad(0, H, ["#2E2A5A", "#C96E6E", "#FFC28A"], steps=7)
    cv.disc(190, 90, 16, "#FFE2A8")
    r = random.Random(181)
    for row, (base, shade) in enumerate(((118, 0.55), (134, 0.3), (152, 0.0))):
        x = r.randint(-10, 0)
        while x < W:
            w, h = r.randint(10, 22), r.randint(16, 50 - row * 10)
            wall = mix(hexc("#3A2E4A"), hexc("#E8C2A0"), 0.25 + row * 0.2)
            cv.building(x, base, w, h, mix(wall, hexc("#C96E6E"), shade), mix(wall, (0, 0, 0), 0.3), "#FFE28A", lit=0.35, seed=x + row)
            x += w + r.randint(1, 4)
    cv.rect(0, 152, W, H, "#2A2436")
    for x in range(0, W, 12):
        cv.rect(x, 155, x + 6, 156, "#FFE28A")
    title(cv, "HOMEBREW", ["UCITY ADVANCE"], fills=CREAM, outline="#1A1426")


def voxelburg(cv):
    cv.vgrad(0, H, ["#5AA8E8", "#BFE3F7", "#F7E8D2"], steps=6)
    r = random.Random(191)
    for i in range(12):
        x = 10 + i * 19
        h = r.randint(4, 9) * 6
        for yy in range(150 - h, 150, 6):
            for xx in range(x, x + 18, 6):
                top = yy == 150 - h
                cv.rect(xx, yy, xx + 6, yy + 6, "#E8E2D9" if top else "#C9BFB0")
                cv.rect(xx + 5, yy, xx + 6, yy + 6, "#9E9282")
                cv.rect(xx, yy + 5, xx + 6, yy + 6, "#9E9282")
    for (cx, cy) in ((70, 96), (160, 84)):
        cv.disc(cx, cy, 16, "#FF8A3A")
        cv.disc(cx, cy, 10, "#FFD25A")
        cv.disc(cx, cy, 5, "#FFFFFF")
        for _ in range(20):
            a, d = r.random() * 6.28, r.uniform(16, 30)
            cv.rect(int(cx + math.cos(a) * d), int(cy + math.sin(a) * d), int(cx + math.cos(a) * d) + 3, int(cy + math.sin(a) * d) + 3, "#C9BFB0")
    cv.rect(0, 150, W, H, "#6E6A60")
    title(cv, "HOMEBREW", ["ATTACK ON", "VOXELBURG"], fills=ROSE, outline="#2A0E0E")


def hat_wizard(cv):
    cv.vgrad(0, H, ["#140E2A", "#2E1E5A", "#5A3A8A"], steps=6)
    cv.stars(60, ["#FFFFFF", "#FFE28A", "#C9B8FF"], ymax=120, seed=201)
    cv.glow(150, 100, 60, "#FFE2A8", 0.4)
    cv.poly([(118, 128), (182, 128), (176, 120), (124, 120)], "#3A1E6E")
    cv.poly([(124, 120), (176, 120), (160, 56), (170, 40), (150, 52)], "#6E3ABF")
    cv.poly([(150, 52), (160, 56), (170, 40)], "#8E5AE0")
    cv.rect(126, 112, 174, 120, "#FFD25A")
    for (x, y) in ((140, 92), (156, 76), (148, 104), (162, 98)):
        cv.put(x, y, hexc("#FFE28A"))
        for d in (1, 2):
            for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                cv.put(x + dx, y + dy, hexc("#FFE28A"))
    cv.rect(110, 128, 190, 150, "#24164A")
    cv.rect(110, 128, 190, 130, "#4A3A7A")
    cv.rect(0, 150, W, H, "#0E0A1E")
    title(cv, "HOMEBREW", ["THE HAT", "CHOOSES THE WIZARD"], fills=GOLD, outline="#140A26")


def purple_night(cv):
    cv.vgrad(0, H, ["#1E0E3A", "#4A2A7A", "#8E5AB8"], steps=7)
    cv.disc(182, 46, 22, "#F2E2FF")
    cv.disc(176, 42, 4, "#D9C2F5")
    cv.disc(190, 52, 3, "#D9C2F5")
    cv.glow(182, 46, 50, "#E8C9FF", 0.35)
    cv.stars(50, ["#FFFFFF", "#E8C9FF"], ymax=110, seed=211)
    r = random.Random(213)
    x = 0
    while x < W:
        w, h = r.randint(16, 26), r.randint(20, 44)
        cv.rect(x, 140 - h, x + w, 140, "#2A1648")
        cv.poly([(x - 2, 140 - h), (x + w + 2, 140 - h), (x + w // 2, 140 - h - 10)], "#1E0E36")
        if r.random() < 0.7:
            cv.rect(x + w // 2 - 2, 140 - h + 8, x + w // 2 + 2, 140 - h + 13, "#FFC27A")
        x += w + r.randint(4, 10)
    for lx in range(20, W, 50):
        cv.rect(lx, 128, lx + 1, 146, "#140A26")
        cv.disc(lx, 126, 3, "#FFD28A")
        cv.glow(lx, 126, 14, "#FFD28A", 0.4)
    cv.rect(0, 146, W, H, "#140A26")
    title(cv, "HOMEBREW", ["THE PURPLE", "NIGHT"], fills=LILAC, outline="#0E0620")


def beatbeast(cv):
    cv.vgrad(0, H, ["#2A0E2E", "#8A2A6E", "#FF7AAE"], steps=7)
    for i, x in enumerate(range(0, W, 30)):
        cv.poly([(x + 15, 0), (x + 5, 140), (x + 25, 140)], "#FFB0D2" if i % 2 else "#C94A8E")
    cv.dither_rect(0, 0, W, 140, "#2A0E2E", 0.45)
    cv.disc(120, 92, 40, "#1A0814")
    cv.poly([(88, 62), (98, 40), (106, 64)], "#1A0814")
    cv.poly([(134, 64), (142, 40), (152, 62)], "#1A0814")
    cv.disc(106, 88, 6, "#FFE24A")
    cv.disc(134, 88, 6, "#FFE24A")
    cv.rect(104, 86, 108, 90, "#1A0814")
    cv.rect(132, 86, 136, 90, "#1A0814")
    for (x, y) in ((40, 70), (60, 44), (190, 60), (206, 92), (30, 110)):
        cv.disc(x, y + 8, 4, "#FFFFFF")
        cv.rect(x + 3, y - 4, x + 5, y + 8, "#FFFFFF")
        cv.rect(x + 3, y - 4, x + 9, y - 2, "#FFFFFF")
    cv.rect(0, 140, W, H, "#1A0814")
    for x in range(0, W, 16):
        cv.rect(x, 140, x + 14, 142, "#FF7AAE" if (x // 16) % 2 else "#5AD8FF")
    title(cv, "HOMEBREW", ["BEATBEAST"], fills=ROSE, outline="#1A0814")


def meteorain(cv):
    cv.vgrad(0, H, ["#0A1430", "#1E3A6E", "#3E6EA8"], steps=6)
    cv.stars(60, ["#FFFFFF", "#9BD2FF"], seed=221)
    r = random.Random(223)
    for _ in range(9):
        x, y, s = r.randrange(40, 260), r.randrange(-20, 110), r.randint(3, 7)
        for t in range(30):
            cv.glow(x - t * 1.6, y - t * 1.1, max(1, s - t // 6), "#FFB23A", 0.6)
        cv.disc(x, y, s, "#FFE2A8", shade="#C96E3A")
    x = 0
    while x < W:
        w, h = r.randint(10, 20), r.randint(12, 30)
        cv.building(x, 152, w, h, "#0E1A30", "#08101E", "#9BD2FF", lit=0.3, seed=x)
        x += w + 1
    cv.rect(0, 152, W, H, "#060C18")
    title(cv, "HOMEBREW", ["METEORAIN"], fills=ICE, outline="#060C18")


def trogdor(cv):
    cv.vgrad(0, 110, ["#3A1E0E", "#C9603A", "#FFC27A"], steps=7)
    cv.rect(0, 110, W, H, "#7A8A3A")
    cv.dither_rect(0, 110, W, H, "#5A6E2A", 0.35)
    for (x, burning) in ((30, True), (90, False), (200, True)):
        cv.rect(x, 116, x + 26, 136, "#E8D2A0")
        cv.poly([(x - 4, 116), (x + 30, 116), (x + 13, 100)], "#C9A04A")
        cv.rect(x + 10, 124, x + 16, 136, "#5A3A1E")
        if burning:
            for k in range(14):
                fx = x + 2 + k * 2
                fh = 10 + (k * 7) % 12
                cv.poly([(fx - 2, 104), (fx + 2, 104), (fx, 104 - fh)], "#FF7A1E" if k % 2 else "#FFD25A")
            cv.glow(x + 13, 98, 26, "#FFB23A", 0.5)
    # dragon (original design): body, wing, head, fire breath
    cv.poly([(110, 128), (150, 104), (176, 112), (168, 134)], "#3E9A3A")
    cv.poly([(140, 108), (150, 66), (166, 104)], "#2E7A2E")
    cv.poly([(150, 104), (160, 82), (180, 86), (178, 104)], "#3E9A3A")
    cv.disc(180, 84, 9, "#4FB84A", shade="#2E7A2E", light="#8FE08A")
    cv.poly([(172, 76), (176, 66), (180, 76)], "#E8E2C9")
    cv.disc(183, 81, 2, "#FFE24A")
    for t in range(40):
        cv.glow(190 + t * 1.2, 90 + t * 0.6, 3 + t // 8, "#FF8A1E", 0.7)
    cv.rect(116, 132, 122, 140, "#2E7A2E")
    cv.rect(156, 132, 162, 140, "#2E7A2E")
    title(cv, "HOMEBREW", ["TROGDOR"], fills=GOLD, outline="#1E0E06")


def solar_guard(cv):
    cv.vgrad(0, H, ["#05040E", "#120E2A", "#1E1640"], steps=5)
    cv.stars(120, ["#FFFFFF", "#FFE2A8", "#9BB8FF"], seed=231)
    cv.disc(30, 150, 60, "#FF9A2A")
    cv.disc(30, 150, 50, "#FFC24A")
    cv.glow(30, 150, 100, "#FFB23A", 0.45)
    cv.disc(176, 56, 22, "#5A8AD9", shade="#2E4A8A", light="#9BC9FF")
    for x in range(146, 208):
        y = int(56 + (x - 176) * 0.18)
        if not (156 < x < 196 and abs(y - 56) < 10):
            cv.put(x, y, hexc("#E8D2A0"))
    cv.disc(112, 40, 8, "#C96E5A", shade="#8A3A2E")
    sx, sy = 140, 112
    cv.poly([(sx, sy - 10), (sx + 6, sy + 6), (sx, sy + 3), (sx - 6, sy + 6)], "#E8E8F2")
    cv.poly([(sx, sy - 10), (sx + 6, sy + 6), (sx, sy + 3)], "#9E9EB8")
    cv.rect(sx - 1, sy + 4, sx + 1, sy + 10, "#5AD8FF")
    for k in range(3):
        cv.rect(sx - 1, sy - 22 - k * 12, sx + 1, sy - 16 - k * 12, "#FFE24A")
    title(cv, "HOMEBREW", ["SOLAR GUARD"], fills=CREAM, outline="#05040E")


COVERS = {
    "pokemon-heart-soul": heart_soul,
    "pokemon-unbound": unbound,
    "pokemon-radical-red": radical_red,
    "pokemon-emerald-rogue": emerald_rogue,
    "pokemon-gaia": gaia,
    "pokemon-light-platinum": light_platinum,
    "pokemon-clover": clover,
    "pokemon-emerald-seaglass": seaglass,
    "pokemon-emerald-crest": crest,
    "pokemon-emerald-imperium": imperium,
    "inclement-emerald": inclement,
    "pokemon-liquid-crystal": liquid_crystal,
    "pokemon-ash-gray": ash_gray,
    "firered-rocket-edition": rocket_edition,
    "pokemon-dark-rising": dark_rising,
    "pokemon-emerald-kaizo": emerald_kaizo,
    "theta-emerald-ex": theta_ex,
    "skyland": skyland,
    "apotris": apotris,
    "minicraft-gba": minicraft,
    "ucity-advance": ucity,
    "attack-on-voxelburg": voxelburg,
    "the-hat-chooses-the-wizard": hat_wizard,
    "the-purple-night": purple_night,
    "beatbeast": beatbeast,
    "meteorain": meteorain,
    "trogdor-gba": trogdor,
    "solar-guard": solar_guard,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet_path = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    made = []
    for i, (cid, fn) in enumerate(sorted(COVERS.items())):
        cv = Canvas(seed=i)
        fn(cv)
        path = os.path.join(OUT, cid + ".png")
        cv.save(path)
        made.append((cid, path))
        print("%-30s %6d bytes" % (cid, os.path.getsize(path)))
    if sheet_path:
        cols = 4
        rows = (len(made) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * 250, rows * 180), (40, 40, 40))
        d = ImageDraw.Draw(sheet)
        for k, (cid, path) in enumerate(made):
            im = Image.open(path).convert("RGB").resize((240, 160), Image.NEAREST)
            x, y = (k % cols) * 250 + 5, (k // cols) * 180 + 2
            sheet.paste(im, (x, y))
            d.text((x, y + 162), cid, fill=(230, 230, 230))
        sheet.save(sheet_path)


if __name__ == "__main__":
    main()
