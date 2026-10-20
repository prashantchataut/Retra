"""Original buildings and interior furniture for Mochi Monsters (RGB painters)."""
from art_world import C, h2

OUT = C("3b2434")
ROOFS = {
    "pink": (C("b64f72"), C("e07a9a"), C("f7a8c0"), C("ffd0de")),
    "teal": (C("2e8576"), C("4fb3a0"), C("86dcc8"), C("c2f2e6")),
    "blue": (C("3e62a8"), C("5f8cd6"), C("91b9f0"), C("c9dfff")),
}
WALL_DK, WALL_MD, WALL_LT = C("d9bc98"), C("f2ddbf"), C("fff3e2")
WOOD_DK, WOOD_MD = C("6e4630"), C("a26b45")
WIN_DK, WIN_LT, WIN_HL = C("4c79b8"), C("a8ddf7"), C("e8f8ff")
STONE, STONE_LT = C("8f8aa0"), C("c9c4d8")
G_SH = C("3a7a44")


def window(cv, x, y):
    cv.stamp(x, y, """
oooooooo
oHLLoLLo
oLLLoLDo
oooooooo
oLLDoLDo
oLDDoDDo
oooooooo
.wwwwww.""", {"o": OUT, "H": WIN_HL, "L": WIN_LT, "D": WIN_DK, "w": WOOD_MD})


def door(cv, x, y, roof):
    dk, md, lt, hl = roof
    cv.stamp(x, y, """
.oooooooooo.
ommmmmmmmmmo
omllllllllmo
omlddddddlmo
omlddddddlmo
omlddddddlmo
omlddddddymo
omlddddddlmo
omlddddddlmo
omlddddddlmo
ommmmmmmmmmo
osssssssssso""", {"o": OUT, "m": WOOD_DK, "l": WOOD_MD, "d": md, "y": C("ffd86a"), "s": STONE})


def building(cv, X, Y, wc, hc, roof="pink", door_col=1, deco=None):
    """wc x hc cells; door in the bottom row at door_col."""
    W, H = wc * 16, hc * 16
    dk, md, lt, hl = ROOFS[roof]
    wall_h = 22
    roof_h = H - wall_h + 4
    # wall
    for j in range(H - wall_h, H):
        for i in range(2, W - 2):
            c = WALL_MD
            if i in (2, W - 3) or j == H - 1:
                c = OUT
            elif i in (3,) or (i - 3) % 16 == 0:
                c = WALL_DK
            elif j == H - 2:
                c = WALL_DK
            elif (i + j) % 7 == 0 and h2(i, j, 41) < 90:
                c = WALL_LT
            cv.set(X + i, Y + j, c)
    # roof: shingle rows, overhang 1px past the walls
    for j in range(roof_h):
        for i in range(W):
            edge = j == 0 or i == 0 or i == W - 1 or j == roof_h - 1
            if edge:
                c = OUT
            elif j == roof_h - 2:
                c = dk
            elif j < 3:
                c = hl if j == 1 else lt
            else:
                row = (j - 3) // 5
                k = (j - 3) % 5
                off = 4 if row % 2 else 0
                if k == 4:
                    c = dk
                elif (i + off) % 8 == 0:
                    c = dk
                elif k == 0:
                    c = lt
                else:
                    c = md
                    if (i + off) % 8 == 1:
                        c = lt
            cv.set(X + i, Y + j, c)
    # eave shadow on the wall
    for i in range(3, W - 3):
        cv.set(X + i, Y + roof_h, WALL_DK)
    # chimney
    if deco != "tea":
        cv.stamp(X + W - 18, Y - 5, ".oooo.\noSSSSo\noLSSSo\noLSSSo\noooooo", {"o": OUT, "S": STONE, "L": STONE_LT})
    door(cv, X + door_col * 16 + 2, Y + H - 12, ROOFS[roof])
    for c in range(wc):
        if c != door_col and c * 16 + 4 < W - 8:
            window(cv, X + c * 16 + 4, Y + H - 18)
    if deco == "tea":
        # hanging tea cup sign on the roof edge
        cv.stamp(X + W // 2 - 8, Y + 3, """
..oooooooooooo..
.oyyyyyyyyyyyyo.
oyWWWWWWWWWWWWyo
oyWWppWWWWWWWWyo
oyWWppppppWoWWyo
oyWWppppppWWoWyo
oyWWWppppWWWoWyo
oyWWWWppWWWoWWyo
oyWWWWWWWWWWWWyo
.oyyyyyyyyyyyyo.
..oooooooooooo..""", {"o": OUT, "y": C("ffd86a"), "W": C("fff3e2"), "p": C("4fb3a0")})
    if deco == "shop":
        cv.stamp(X + W // 2 - 8, Y + 4, """
..oooooooooooo..
.oyyyyyyyyyyyyo.
oyWWWWWoWWWWWWyo
oyWWWWoooWWWWWyo
oyWWWoWoWWWWWWyo
oyWWWWoooWWWWWyo
oyWWWWWoWoWWWWyo
oyWWWWoooWWWWWyo
oyWWWWWoWWWWWWyo
.oyyyyyyyyyyyyo.
..oooooooooooo..""", {"o": OUT, "y": C("ffd86a"), "W": C("fff3e2")})
    # ground shadow
    for i in range(2, W - 1):
        cv.set(X + i, Y + H, G_SH)


# ------------------------------------------------------------------ interiors
F_OUT = C("3b2434")
FL_DK, FL_MD, FL_LT, FL_HL = C("a8704a"), C("c8925e"), C("e0ae78"), C("f2cc98")
IW_DK, IW_MD, IW_LT = C("c9a88a"), C("e2c8ae"), C("f6e6d2")
RUG_DK, RUG_LT = C("e07a9a"), C("f9b3c9")
LEAF_DK, LEAF_LT = C("3c8a4e"), C("69b85e")
TEAL = C("4fb3a0")
CREAM = C("fff8ee")


def floor(cv, X, Y, gx, gy):
    for j in range(16):
        for i in range(16):
            plank = (j // 4)
            c = FL_MD
            if j % 4 == 3:
                c = FL_DK
            elif (i + plank * 5) % 16 == 0:
                c = FL_DK
            elif j % 4 == 0:
                c = FL_LT
            elif h2(X + i, Y + j, 51) < 12:
                c = FL_HL
            cv.set(X + i, Y + j, c)


def wall(cv, X, Y, gx, gy, top):
    for j in range(16):
        for i in range(16):
            c = IW_MD
            if top:
                c = IW_LT if (i // 4 + j // 4) % 2 == 0 else IW_MD
                if j < 3:
                    c = FL_DK if j < 2 else F_OUT
            else:
                c = IW_MD if (i % 8) else IW_DK
                if j >= 13:
                    c = FL_DK if j < 15 else F_OUT
                elif j == 12:
                    c = FL_LT
            cv.set(X + i, Y + j, c)


def furniture(cv, kind, X, Y):
    leg = {"o": F_OUT, "d": FL_DK, "m": FL_MD, "l": FL_LT, "h": FL_HL, "g": LEAF_DK, "G": LEAF_LT,
           "t": TEAL, "c": CREAM, "r": RUG_DK, "R": RUG_LT, "w": IW_LT, "W": C("ffffff"),
           "b": C("4c79b8"), "B": C("a8ddf7"), "y": C("ffd86a"), "p": C("f9b3c9")}
    if kind == "counter":
        cv.stamp(X, Y, "\n".join(["oooooooooooooooo", "hhhhhhhhhhhhhhhh", "llllllllllllllll", "oooooooooooooooo",
                                  "mmmmmmmmmmmmmmmm", "mdmmmmmmmdmmmmmm", "mdmmmmmmmdmmmmmm", "mdmmmmmmmdmmmmmm",
                                  "mdmmmmmmmdmmmmmm", "mdmmmmmmmdmmmmmm", "mdmmmmmmmdmmmmmm", "dddddddddddddddd",
                                  "oooooooooooooooo"]), leg)
    elif kind == "teapot":
        cv.stamp(X + 3, Y + 1, "....oo....\n...oyyo...\n.oooooooo.\noottttttoo\notWttttttoto\notttttttto.o\notttttttoo.\n.otttttto..\n..oooooo...", leg)
    elif kind == "plant":
        cv.stamp(X + 2, Y, """
....oo..oo..
...oGGooGGo.
..oGGgGGgGo.
.oGgGGGGGGo.
.oGGgGgGgGo.
..oGGGgGGo..
...ooGGoo...
....oddo....
...oRRRRo...
...oRrrRo...
...orrrro...
....oooo....""", leg)
    elif kind == "shelf":
        cv.stamp(X, Y, """
oooooooooooooooo
ohhhhhhhhhhhhhho
omoyyooRRoottoMo
omoyyooRRoottomo
omddddddddddddmo
omocooobboooggmo
omocooBbbooogGmo
omddddddddddddmo
ommmmmmmmmmmmmmo
oooooooooooooooo""".replace("M", "m"), leg)
    elif kind == "table":
        cv.stamp(X + 1, Y + 2, """
.oooooooooooo.
ohhhhhhhhhhhho
olllllllllllll
olllcclllllllo
ollcWWclllllpo
olllcclllllppo
olllllllllllll
oddddddddddddo
.oooooooooooo.
.od........do.
.oo........oo.""", leg)
    elif kind == "rug":
        for j in range(16):
            for i in range(16):
                c = RUG_LT if (i + j) % 4 else RUG_DK
                if j in (0, 15) or i in (0, 15):
                    c = RUG_DK
                cv.set(X + i, Y + j, c)
    elif kind == "mat":
        for j in range(4, 14):
            for i in range(1, 15):
                c = TEAL if j not in (4, 13) else F_OUT
                if i in (1, 14):
                    c = F_OUT
                cv.set(X + i, Y + j, c)
    elif kind == "window":
        cv.stamp(X + 2, Y + 3, "oooooooooooo\nobBBBBoBBBBo\nobBWBBoBBBBo\nobBBBBoBBBbo\noooooooooooo\nobBBBBobBBBo\nobbBBBobbBBo\noooooooooooo\n.dddddddddd.", leg)
    elif kind == "bed":
        cv.stamp(X, Y, "oooooooooooooooo\nowwwwwwwwwwwwwwo\nowWWWWWWWWWWWwwo\noooooooooooooooo\norrrrrrrrrrrrrro\norRRRRRRRRRRRRro\norRRRRRRRRRRRRro\norRRRRRRRRRRRRro\norrrrrrrrrrrrrro\noooooooooooooooo", leg)
