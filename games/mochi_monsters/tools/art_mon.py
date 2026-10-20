"""Original creature ("Mochling") art for Mochi Monsters.

Every creature is sculpted from shaded primitives in a 64x64 design space,
facing left. The same description renders the 64x64 battle frames and the
32x32 party icons, so all sizes stay consistent.
"""
import math
from sculpt import Sculpt, ell, circ, poly, capsule, leaf, flame, palette


def mouth(s, x, y, kind="smile"):
    if s.k < 0.8:
        return
    arts = {
        "smile": "#..#\n.##.",
        "small": "#.#\n.#.",
        "open": ".##.\n#33#\n.##.",
        "w": "#.#.#\n.#.#.",
        "fang": "#..#\n.##.\n.#..",
        "flat": "###",
    }
    s.stamp(x, y, arts[kind], {"#": 1, "3": 3})


# ---------------------------------------------------------------- creatures
def sproutle(s):
    s.part(ell(23, 60, 4.5, 2.6), "A", tone_shift=-0.5)
    s.part(ell(40, 60.5, 4.5, 2.6), "A", tone_shift=-0.5)
    s.part(ell(31, 47, 16, 14, p=2.3, rb=12.5), "A", shine=True)
    s.decal(ell(28, 54.5, 9, 5.5), "C", level=2, edge=1)
    s.part(capsule(32, 35, 31, 29, 1.3), "B", tone_shift=-0.3)
    s.part(leaf(31, 30, 18, 22, 4.4, bend=0.12), "B")
    s.part(leaf(32, 30, 46, 21, 4.8, bend=-0.14), "B", shine=True)
    s.line([(22, 25), (28, 28)], 1, "B")
    s.line([(40, 24), (35, 27)], 1, "B")
    s.eye(20, 46, near=False)
    s.eye(30, 46)
    s.blush(17, 51, 2)
    s.blush(35, 51)
    mouth(s, 25, 50)


def bloomble(s):
    s.part(leaf(16, 44, 3, 52, 4.5, bend=-0.1), "A", tone_shift=-0.2)
    s.part(ell(20, 60, 6.5, 3.2), "A", tone_shift=-0.5)
    s.part(ell(44, 60, 6.5, 3.2), "A", tone_shift=-0.5)
    s.part(ell(32, 43, 22, 19, p=2.2, rb=17), "A", shine=True)
    s.decal(ell(29, 51, 12.5, 8.5), "C", level=2, edge=1)
    s.part(leaf(48, 44, 62, 52, 4.5, bend=0.12), "A")
    s.part(leaf(30, 26, 17, 20, 4, bend=0.15), "A")
    s.part(leaf(34, 26, 47, 19, 4, bend=-0.15), "A")
    for a in (-90, -18, 54, 126, 198):
        r = math.radians(a)
        s.part(ell(32 + math.cos(r) * 8.5, 16 + math.sin(r) * 6, 6.5, 4.5, rot=a), "B", shine=(a == 198))
    s.part(circ(32, 16, 4.5), "C", shine=True)
    s.eye(21, 38, near=False)
    s.eye(32, 38)
    s.blush(17, 44, 2)
    s.blush(37, 44)
    mouth(s, 26, 44, "open")


def embit(s):
    s.part(flame(49, 50, 6.5, 22, lean=0.3), "B", shine=True)
    s.part(ell(43, 58, 3.2, 4.5), "A", tone_shift=-0.6)
    s.part(ell(47.5, 58, 3.2, 4.5), "A", tone_shift=-0.6)
    s.part(ell(37, 49, 12.5, 9), "A")
    s.decal(ell(30, 52, 5, 5), "C", level=2, edge=1)
    s.part(ell(27, 58, 3.1, 4.5), "A", tone_shift=-0.2)
    s.part(ell(32, 58.5, 3.1, 4.5), "A", tone_shift=-0.2)
    s.decal(ell(27, 61.5, 3, 1.4) | ell(32, 62, 3, 1.4), "C", level=2)
    s.part(poly([(25, 28), (32, 14), (36, 31)]), "A", tone_shift=-0.2)
    s.decal(poly([(28, 27), (32, 19), (34, 29)]), "B", level=2)
    s.part(ell(24, 37, 11.5, 10.5), "A", shine=True)
    s.part(ell(14, 41, 6.5, 4.6), "A")
    s.decal(ell(15, 43, 6, 3.2), "C", level=2, edge=1)
    s.part(poly([(13, 33), (13, 17), (22, 28)]), "A")
    s.decal(poly([(15, 30), (15, 22), (20, 28)]), "B", level=2)
    s.decal(flame(25, 30, 2.2, 5), "B", level=2)
    s.dot(9.5, 40, 1)
    s.dot(10, 40, 1)
    s.eye(17, 36, near=False)
    s.eye(26, 36)
    s.blush(28, 42)
    mouth(s, 14, 46, "small")


def pyrrox(s):
    s.part(flame(53, 46, 9, 32, lean=0.32, tongues=4), "B", shine=True)
    s.part(capsule(45, 48, 46, 61, 3.6), "A", tone_shift=-0.6)
    s.part(capsule(50, 48, 52, 61, 3.4), "A", tone_shift=-0.6)
    s.part(ell(39, 44, 15.5, 10.5, rot=-8), "A")
    s.part(capsule(29, 46, 27, 61, 3.3), "A", tone_shift=-0.15)
    s.part(capsule(34, 48, 34, 61, 3.3), "A", tone_shift=-0.15)
    s.decal(ell(27, 61.5, 3.4, 1.6) | ell(34, 61.5, 3.4, 1.6), "C", level=2)
    s.part(flame(31, 44, 10, 22, lean=0.15, tongues=3) | ell(31, 42, 10.5, 8), "B")
    s.part(poly([(24, 19), (32, 4), (34, 22)]), "A", tone_shift=-0.2)
    s.decal(poly([(27, 18), (31, 9), (32, 20)]), "B", level=2)
    s.part(ell(22, 28, 10.5, 9.5), "A", shine=True)
    s.part(ell(11, 32, 7.5, 4.6, rot=8), "A")
    s.decal(ell(11, 34, 6.5, 2.6, rot=8), "C", level=2, edge=1)
    s.part(poly([(13, 24), (12, 6), (22, 19)]), "A")
    s.decal(poly([(15, 21), (14, 12), (19, 19)]), "B", level=2)
    s.dot(4.5, 31, 1)
    s.dot(5, 31, 1)
    s.eye(15, 26, "angry", near=False)
    s.eye(23, 26, "angry")
    mouth(s, 10, 37, "fang")


def puddlet(s):
    s.part(leaf(44, 56, 56, 47, 4.5, bend=-0.2), "A", tone_shift=-0.3)
    s.part(ell(31, 48, 16, 13, p=2.2, rb=12), "A", shine=True)
    s.decal(ell(27, 55, 10.5, 6), "C", level=2, edge=1)
    s.part(ell(17, 57, 6.5, 3, rot=25), "A", tone_shift=-0.2)
    s.part(ell(41, 59, 5.5, 2.6, rot=-15), "A", tone_shift=-0.4)
    s.part(ell(30, 34, 5.5, 6) | leaf(30, 30, 39, 22, 3.4, bend=-0.35), "B", shine=True)
    s.decal(circ(28, 33, 1.6), "B", level=3)
    s.eye(21, 45, near=False)
    s.eye(31, 45)
    s.blush(18, 50, 2)
    s.blush(36, 50)
    mouth(s, 26, 50, "w")


def tidalotl(s):
    s.part(leaf(42, 46, 63, 37, 6.5, bend=0.18), "A", tone_shift=-0.2)
    s.decal(leaf(48, 43, 62, 37, 2.2, bend=0.18), "B", level=2)
    s.part(ell(46, 57, 3.6, 4.6), "A", tone_shift=-0.6)
    s.part(ell(37, 48, 17, 9.5, rot=-5), "A")
    s.decal(ell(35, 53, 12, 4), "C", level=2, edge=1)
    s.part(ell(28, 57.5, 3.6, 4.6), "A", tone_shift=-0.2)
    for (x0, y0, x1, y1) in ((27, 33, 33, 18), (29, 36, 41, 25), (30, 40, 44, 34)):
        s.part(leaf(x0, y0, x1, y1, 3.2, bend=0.15), "B", shine=True)
    s.part(ell(20, 40, 14, 11, p=2.2), "A", shine=True)
    s.decal(ell(15, 46, 8, 3), "C", level=2)
    s.part(leaf(10, 33, 6, 22, 2.4, bend=-0.2), "B", tone_shift=-0.3)
    s.eye(12, 38, near=False)
    s.eye(21, 38)
    s.blush(25, 44)
    mouth(s, 15, 44, "smile")


def fluffin(s):
    s.part(capsule(28, 56, 27, 61, 1.3), "B")
    s.part(capsule(36, 56, 37, 61, 1.3), "B")
    s.decal(ell(26.5, 61.5, 2.5, 1) | ell(37.5, 61.5, 2.5, 1), "B", level=2)
    s.part(ell(46, 45, 5.5, 4.2), "A", tone_shift=-0.3)
    s.part(circ(32, 46, 12) | circ(22, 48.5, 8) | circ(42, 49.5, 8) | circ(27, 38, 8.5) | circ(38, 39, 7.5),
           "A", shine=True, lo=-0.2, hi=0.5)
    s.part(ell(41, 46, 6.5, 4.2, rot=-20), "C")
    s.part(leaf(30, 31, 25, 23, 2.6, bend=0.3), "A")
    s.part(poly([(13, 44), (20, 40.5), (20, 47.5)]), "B")
    s.eye(19, 42, near=False)
    s.eye(27, 42)
    s.blush(21, 47, 2)
    s.blush(32, 47)


def galecrest(s):
    s.part(leaf(45, 42, 63, 33, 4.4, bend=-0.1), "A", tone_shift=-0.3)
    s.part(leaf(46, 46, 63, 49, 4.2, bend=0.1), "A", tone_shift=-0.4)
    s.part(capsule(30, 50, 28, 61, 1.6), "C")
    s.part(capsule(38, 50, 39, 61, 1.6), "C")
    s.decal(ell(27, 61.5, 3, 1.2) | ell(40, 61.5, 3, 1.2), "C", level=2)
    s.part(ell(36, 41, 15.5, 13, rot=-10), "A")
    s.part(circ(26, 46, 7.5) | circ(30, 51.5, 6.5) | circ(23, 39, 6), "B", shine=True)
    s.part(leaf(32, 33, 57, 47, 7, bend=0.12), "A", tone_shift=-0.25)
    s.line([(40, 39), (52, 44)], 1, "A")
    s.part(leaf(25, 19, 41, 9, 3.2, bend=0.25), "B")
    s.part(leaf(23, 18, 33, 3, 3.2, bend=0.15), "A")
    s.part(circ(22, 27, 10.5), "A", shine=True)
    s.decal(ell(19, 31, 6, 3.5), "B", level=2)
    s.part(poly([(4, 29), (14, 24), (14, 32)]), "C", shine=True)
    s.eye(15, 25, "angry", near=False)
    s.eye(23, 25, "angry")


def nibbit(s):
    s.part(ell(36, 26, 3.8, 11.5, rot=14), "A", tone_shift=-0.2)
    s.decal(ell(36, 27, 1.6, 8.5, rot=14), "B", level=2)
    s.part(circ(46, 51, 4.8), "C", shine=True)
    s.part(ell(24, 60, 5, 2.5), "A", tone_shift=-0.4)
    s.part(ell(39, 60, 5, 2.5), "A", tone_shift=-0.4)
    s.part(ell(31, 48, 14, 13, p=2.2, rb=11.5), "A", shine=True)
    s.decal(ell(28, 55, 8, 5), "C", level=2, edge=1)
    s.part(ell(25, 27, 3.8, 11, rot=-12), "A")
    s.decal(ell(25, 28, 1.6, 8, rot=-12), "B", level=2)
    s.eye(21, 46, near=False)
    s.eye(30, 46)
    s.stamp(25.5, 49.5, "##", {"#": ("B", 1)})
    s.blush(18, 51, 2)
    s.blush(34, 51)
    mouth(s, 26, 52, "small")


def thumpkin(s):
    s.part(ell(34, 14, 4.6, 14, rot=10), "A", tone_shift=-0.2)
    s.decal(ell(34, 16, 2, 10, rot=10), "B", level=2)
    s.part(circ(50, 46, 5.5), "C", shine=True)
    s.part(ell(45, 59, 9.5, 4), "A", tone_shift=-0.5)
    s.part(ell(34, 43, 15, 16, p=2.1, rb=14), "A")
    s.decal(ell(30, 48, 9, 10.5), "C", level=2, edge=1)
    s.part(ell(24, 60, 8.5, 3.6), "A", tone_shift=-0.2)
    s.part(ell(19, 45, 3.6, 6.5, rot=20), "A")
    s.part(ell(22, 16, 4.6, 13, rot=-18), "A")
    s.decal(ell(22, 18, 2, 9, rot=-18), "B", level=2)
    s.part(ell(26, 28, 11.5, 10), "A", shine=True)
    s.part(ell(28, 37, 11.5, 3.6, rot=-5) | leaf(34, 37, 44, 48, 3.4, bend=0.1), "B", shine=True)
    s.eye(19, 27, near=False)
    s.eye(28, 27)
    s.stamp(23, 31, "##", {"#": ("B", 1)})
    s.blush(16, 31, 2)
    s.blush(32, 31)


def pebblin(s):
    body = ell(31, 48, 15, 13, p=1.9, rb=11)
    s.part(ell(24, 60, 4, 2.2), "A", tone_shift=-0.6)
    s.part(ell(38, 60, 4, 2.2), "A", tone_shift=-0.6)
    s.part(body, "A", shine=True)
    s.part(ell(30, 36, 12, 5.5, p=2.4) & body, "B", inline=False)
    s.part(circ(23, 34, 2.4), "C", shine=True)
    s.part(circ(36, 33, 1.8), "C")
    s.line([(41, 43), (44, 47), (42, 51)], 1, "A")
    s.line([(19, 53), (22, 55)], 1, "A")
    s.eye(22, 47, near=False)
    s.eye(32, 47)
    s.blush(19, 52, 2)
    s.blush(36, 52)
    mouth(s, 27, 52, "small")


def bouldon(s):
    body = ell(32, 40, 22, 20, p=1.8, rb=18)
    for pts in ([(40, 22), (46, 5), (51, 24)], [(48, 28), (59, 15), (57, 33)], [(31, 21), (35, 8), (40, 22)]):
        s.part(poly(pts), "C", shine=True)
    s.part(ell(52, 48, 6.5, 9.5, p=1.8, rot=-15), "A", tone_shift=-0.4)
    s.part(ell(21, 60, 7, 3.5), "A", tone_shift=-0.5)
    s.part(ell(43, 60, 7, 3.5), "A", tone_shift=-0.5)
    s.part(body, "A", shine=True)
    s.part(ell(26, 24, 15, 7, p=2.4) & body, "B", inline=False)
    s.part(circ(18, 22, 2.6), "C", shine=True)
    s.part(ell(10, 46, 6.5, 9.5, p=1.8, rot=15), "A")
    s.line([(44, 40), (48, 45), (46, 51)], 1, "A")
    s.line([(24, 50), (28, 53)], 1, "A")
    s.line([(14, 33), (18, 31)], 1, "A")
    s.eye(21, 38, near=False)
    s.eye(32, 38)
    s.blush(17, 44, 2)
    s.blush(36, 44)
    mouth(s, 27, 44, "flat")


def zappup(s):
    s.part(poly([(43, 50), (50, 42), (47, 41), (55, 31), (57, 34), (52, 43), (55, 44), (45, 55)]), "B", shine=True)
    s.part(ell(42, 58, 3, 4.5), "A", tone_shift=-0.6)
    s.part(ell(46.5, 58, 3, 4.5), "A", tone_shift=-0.6)
    s.part(ell(36, 50, 11.5, 8.5), "A")
    s.part(ell(28, 58, 3, 4.5), "A", tone_shift=-0.2)
    s.part(ell(33, 58.5, 3, 4.5), "A", tone_shift=-0.2)
    s.decal(ell(28, 61.8, 3, 1.3) | ell(33, 62, 3, 1.3), "B", level=2)
    s.part(ell(34, 37, 4.2, 8.5, rot=-35), "A", tone_shift=-0.35)
    s.decal(ell(37.5, 42, 3, 3.5, rot=-35) & ell(34, 37, 4.2, 8.5, rot=-35), "B", level=2)
    s.part(ell(24, 38, 11, 10), "A", shine=True)
    s.part(ell(15, 42, 6.5, 4.5), "A")
    s.decal(ell(16, 43.5, 6, 3), "C", level=2, edge=1)
    s.part(ell(16, 33, 3.6, 7, rot=35), "A")
    s.decal(ell(13, 37, 2.6, 2.8, rot=35) & ell(16, 33, 3.6, 7, rot=35), "B", level=2)
    s.decal(poly([(23, 29), (27, 29), (25, 32), (28, 32), (22, 37), (23.5, 33.5), (21, 33.5)]), "B", level=2)
    s.dot(9.5, 41, 1)
    s.dot(10, 41, 1)
    s.eye(18, 37, near=False)
    s.eye(27, 37)
    s.blush(29, 43)
    mouth(s, 14, 46, "small")


def voltaroo(s):
    s.part(poly([(40, 52), (52, 50), (50, 47), (63, 44), (60, 52), (62, 53), (46, 60)]), "B", shine=True)
    s.part(ell(41, 60, 8.5, 3), "A", tone_shift=-0.6)
    s.part(ell(39, 49, 9.5, 9.5), "A", tone_shift=-0.3)
    s.part(ell(33, 37, 11, 15, rot=8), "A")
    s.decal(ell(29, 43, 6.5, 9.5, rot=8), "C", level=2, edge=1)
    s.part(ell(29, 60, 9, 3), "A")
    s.part(capsule(23, 34, 16, 43, 2.4), "A", tone_shift=-0.1)
    s.decal(circ(15.5, 43.5, 2.4), "B", level=2)
    s.part(leaf(32, 15, 39, 1, 3.6, bend=0.1), "A", tone_shift=-0.25)
    s.decal(ell(38, 3, 3, 3.5) & leaf(32, 15, 39, 1, 3.6, bend=0.1), "B", level=2)
    s.part(ell(27, 21, 9.5, 8.5), "A", shine=True)
    s.part(ell(17, 23, 5.5, 4.2), "A")
    s.decal(ell(18, 25, 4.8, 2.4), "C", level=2)
    s.part(leaf(27, 15, 23, 0, 3.6, bend=-0.1), "A")
    s.decal(ell(23.5, 2, 3, 3.5) & leaf(27, 15, 23, 0, 3.6, bend=-0.1), "B", level=2)
    s.dot(12, 22, 1)
    s.eye(21, 20, "angry", near=False)
    s.eye(28, 20, "angry")
    mouth(s, 16, 27, "small")


def dangoo(s):
    s.part(capsule(32, 6, 32, 62, 1.5), "B", tone_shift=-0.6)
    s.part(ell(27, 61.5, 3.6, 2), "C", tone_shift=-0.4)
    s.part(ell(37, 61.5, 3.6, 2), "C", tone_shift=-0.4)
    s.part(circ(32, 51, 9.5), "C", shine=True)
    s.part(circ(32, 17, 9.5), "A", shine=True)
    s.part(ell(19, 37, 3.4, 2.4, rot=-20), "B")
    s.part(ell(45, 37, 3.4, 2.4, rot=20), "B", tone_shift=-0.3)
    s.part(circ(32, 34, 10.5), "B", shine=True)
    s.eye(27, 33, near=False)
    s.eye(35, 33)
    s.blush(24, 38, 2)
    s.blush(39, 38)
    mouth(s, 31, 38, "open")


def kettlet(s):
    s.part(circ(48, 45, 8) - circ(48, 45, 4.4), "A", tone_shift=-0.3)
    s.part(poly([(21, 47), (9, 36), (5, 37.5), (9, 42.5), (18, 55)]), "A", tone_shift=-0.1)
    body = ell(32, 47, 15, 12.5, p=2.2, rb=11)
    s.part(ell(25, 60, 3.6, 2.2), "A", tone_shift=-0.6)
    s.part(ell(39, 60, 3.6, 2.2), "A", tone_shift=-0.6)
    s.part(body, "A", shine=True)
    s.part(ell(32, 54, 16, 2.6) & body, "B", inline=False)
    s.part(ell(32, 35, 10, 3.6), "B", shine=True)
    s.part(flame(32, 32, 3.2, 8), "C", shine=True)
    s.part(circ(6, 30, 2.6) | circ(9.5, 26.5, 2.1) | circ(5, 23, 1.6), "B", tone_shift=0.6)
    s.eye(24, 44, near=False)
    s.eye(34, 44)
    s.blush(21, 49, 2)
    s.blush(38, 49)
    mouth(s, 29, 49, "smile")


def dozymoth(s):
    s.part(ell(43, 23, 15, 10, rot=-55), "A", tone_shift=-0.45)
    s.part(ell(47, 40, 8, 6, rot=25), "A", tone_shift=-0.45)
    s.part(ell(43, 49, 10, 7, rot=20), "B")
    s.line([(39, 46), (41, 52)], 1, "B")
    s.line([(44, 45), (46, 51)], 1, "B")
    s.part(capsule(26, 50, 23, 58, 1), "A", tone_shift=-0.3)
    s.part(capsule(31, 50, 31, 59, 1), "A", tone_shift=-0.3)
    s.part(circ(31, 44, 8), "B")
    s.part(ell(36, 26, 14.5, 11, rot=-42), "A", shine=True)
    s.decal(circ(37, 22, 4.2) - circ(39, 20.5, 3), "C", level=2)
    s.decal(circ(31, 32, 1.8), "C", level=3)
    s.part(ell(43, 40, 8, 6, rot=12), "A")
    s.decal(circ(45, 40, 2.2), "C", level=2)
    s.part(leaf(19, 36, 9, 22, 2.6, bend=0.25), "A", tone_shift=-0.2)
    s.part(leaf(23, 35, 22, 19, 2.6, bend=0.1), "A")
    s.part(circ(21, 42, 8.5), "B", shine=True)
    s.eye(16, 41, "sleepy", near=False)
    s.eye(23, 41, "sleepy")
    s.blush(15, 45, 2)
    s.blush(26, 45)
    mouth(s, 19, 47, "small")


def glowcap(s):
    s.part(ell(25, 60, 4, 2.2), "B", tone_shift=-0.5)
    s.part(ell(39, 60, 4, 2.2), "B", tone_shift=-0.5)
    s.part(ell(32, 50, 11.5, 11, p=2.3, rb=9), "B", shine=True)
    cap = ell(32, 33, 21, 14, p=2.3, rb=6)
    s.part(cap, "A", shine=True)
    s.part(ell(32, 38.5, 17, 2.2) & cap, "A", flat=1)
    for (x, y, r) in ((23, 27, 3.4), (36, 23, 2.6), (45, 31, 2.8), (29, 34, 1.8), (14, 33, 1.8)):
        s.part(circ(x, y, r) & cap, "C", shine=True, inline=False)
    s.eye(27, 48, near=False)
    s.eye(35, 48)
    s.blush(24, 53, 2)
    s.blush(39, 53)
    mouth(s, 31, 53, "small")


def mochi(s):
    """The app mascot: a round squishy rice cake with a leaf on top."""
    s.part(ell(22, 61, 5, 2.4), "A", tone_shift=-0.6)
    s.part(ell(42, 61, 5, 2.4), "A", tone_shift=-0.6)
    s.part(ell(32, 47, 22, 15.5, p=2.6, rb=13), "A", shine=True)
    s.part(capsule(33, 32, 34, 27, 1.2), "B", tone_shift=-0.4)
    s.part(leaf(34, 29, 48, 21, 5.0, bend=-0.18), "B", shine=True)
    s.line([(37, 27), (44, 23)], 1, "B")
    s.eye(18, 42, near=False)
    s.eye(30, 42)
    s.blush(14, 49, 3)
    s.blush(35, 49)
    mouth(s, 24, 49, "w")


# ---------------------------------------------------------------- roster
# (name, draw, ramps A/B/C)
ROSTER = [
    ("Sproutle", sproutle, [("2f6b47", "5fb370", "8edb84", "c6f2ad"), ("1f5537", "3d8a58", "6fcf7a", "a9eda0"),
                            ("7d6b4a", "e6d6a8", "fff4d6", "ffffff")]),
    ("Bloomble", bloomble, [("285e3e", "52a665", "84d47d", "bdf0a6"), ("8a2f5a", "e0699a", "ffa3c6", "ffd8e8"),
                            ("8a6a2e", "f2c95a", "ffe9a0", "fffbe0")]),
    ("Embit", embit, [("7a2f1f", "e06a3a", "ff9b57", "ffc98e"), ("b2441a", "ff8a2b", "ffc53f", "fff2a3"),
                      ("8a5a44", "f0d2b4", "fff1e0", "ffffff")]),
    ("Pyrrox", pyrrox, [("6e2418", "cf4e2e", "f57f45", "ffb57a"), ("a8381a", "ff7a24", "ffbe3a", "fff09a"),
                        ("845444", "eccbb0", "fff0de", "ffffff")]),
    ("Puddlet", puddlet, [("234d88", "4a86d6", "7fbaf3", "c2e5ff"), ("2a7a92", "5fc6da", "a6ecf3", "e8fdff"),
                          ("5a6f9a", "d8e4f6", "f4f9ff", "ffffff")]),
    ("Tidalotl", tidalotl, [("1f5a6e", "3f9fb5", "74d2df", "bff3f2"), ("8a2f55", "e0668f", "ff9ab8", "ffd0de"),
                            ("6a6a8a", "e2def0", "f8f6ff", "ffffff")]),
    ("Fluffin", fluffin, [("5f73a8", "bccdef", "e9f1ff", "ffffff"), ("9a5a1a", "f2a33a", "ffd36a", "fff0b0"),
                          ("3e6ea8", "7fb2f0", "b7dcff", "e6f4ff")]),
    ("Galecrest", galecrest, [("26508c", "4f8ad6", "86bdf5", "cbe7ff"), ("6a7fb0", "c9d8f0", "f2f7ff", "ffffff"),
                              ("925818", "eba135", "ffd162", "fff1b5")]),
    ("Nibbit", nibbit, [("7a4e38", "d6a47c", "f2cfa6", "fff0da"), ("8a3a55", "e57d9c", "ffadc4", "ffd9e5"),
                        ("8a8090", "e2dce8", "f8f6fb", "ffffff")]),
    ("Thumpkin", thumpkin, [("6a3e2c", "c58a5e", "e8b98a", "fadbb5"), ("7a2238", "d6475f", "f57b8a", "ffb8bf"),
                            ("8a7a70", "e8ddd2", "fbf5ee", "ffffff")]),
    ("Pebblin", pebblin, [("47405a", "8b84a2", "b9b3cb", "e3e0ec"), ("2f5a3a", "5aa865", "8fd68a", "c6f2b0"),
                          ("6a3a5a", "c06a98", "f0a0c8", "ffd8ea")]),
    ("Bouldon", bouldon, [("3e384e", "7a7392", "a9a3bd", "d8d4e4"), ("2f5a3a", "5aa865", "8fd68a", "c6f2b0"),
                          ("1f5a66", "3fa3b0", "74d8de", "c0f6f6")]),
    ("Zappup", zappup, [("3a3678", "7672d4", "a5a2f2", "d6d4ff"), ("9a6a10", "f0c030", "ffe36a", "fff8c0"),
                        ("7a6a80", "e8e2f0", "fbf9ff", "ffffff")]),
    ("Voltaroo", voltaroo, [("2f2b6a", "625ec4", "928ee8", "c8c6ff"), ("9a6a10", "f0c030", "ffe36a", "fff8c0"),
                            ("7a6a80", "e6e0ee", "fbf9ff", "ffffff")]),
    ("Dangoo", dangoo, [("8a3a5a", "e3799d", "ffadc6", "ffdbe7"), ("7a7088", "dcd5e6", "f8f5fb", "ffffff"),
                        ("2f6a3e", "5fae6c", "94d98e", "cdf5bf")]),
    ("Kettlet", kettlet, [("6a2828", "b9503e", "e27b5b", "f8a987"), ("8a6a4a", "ecd3ae", "fff0d6", "ffffff"),
                          ("a83e1a", "ff8a2b", "ffc53f", "fff2a3")]),
    ("Dozymoth", dozymoth, [("4a3a7a", "8e7fd0", "bdb1f0", "e6e1ff"), ("8a6a60", "ead8c8", "fff3e6", "ffffff"),
                            ("8a6a20", "e8c650", "fff0a0", "fffbe0")]),
    ("Glowcap", glowcap, [("1d4d68", "3e9cb6", "70d4de", "baf4f0"), ("7a6a6a", "e9ddd0", "fff6ea", "ffffff"),
                          ("6a7020", "d0dc58", "f4fca0", "ffffe0")]),
    ("Mochi", mochi, [("5a2a4e", "e8849f", "f9b3c9", "ffd6e2"), ("24563a", "3d8a58", "6fcf7a", "b4f2a8"),
                      ("9a6a7e", "f6dbe4", "fff1f5", "ffffff")]),
]


def render(idx, size="battle", frame=0):
    name, fn, ramps = ROSTER[idx]
    if size == "battle":
        sq = (1.0, 1.0) if frame == 0 else (1.035, 0.955)
        s = Sculpt(64, 64, k=1.0, ox=32, oy=63, sx=sq[0], sy=sq[1])
    else:
        sq = (1.0, 1.0) if frame == 0 else (1.08, 0.88)
        s = Sculpt(32, 32, k=0.44, ox=16, oy=31, sx=sq[0], sy=sq[1])
    fn(s)
    s.outline()
    return s.px


def pal(idx):
    if ROSTER[idx][0] == "Mochi":
        return palette(ROSTER[idx][2], blush="ff7b9c")
    return palette(ROSTER[idx][2])
