"""Mochi Pop blob art (original): 12x12 mochi of 5 flavours + nuisance.

Art is drawn with *logical* colour indices so the same pixels can be emitted as
8bpp background cells (well) and 4bpp sprites (falling pair):
  1 outline  2 deep  3 shade  4 base  5 light  6 highlight
  7 ink      8 white 9 blush  10 sparkle  11 sparkle-deep
"""
from pix import Layer

# flavour ramps: outline, deep, shade, base, light, highlight
FLAVOURS = [
    ("strawberry", ["7a2242", "b83a5c", "e0607e", "f58ba3", "ffbccb", "ffe6ec"]),
    ("matcha",     ["2c5631", "4a8a47", "6fb862", "93d483", "c2ecb0", "eefbe4"]),
    ("yuzu",       ["7a5216", "c98a22", "eab43a", "f9d55a", "ffeb94", "fffbe0"]),
    ("taro",       ["452866", "7552ab", "9a7ad6", "b99cec", "d9c8fa", "f6efff"]),
    ("soda",       ["1d4874", "3a7fc0", "5aaee6", "84cdf6", "b8e7ff", "eaf9ff"]),
    ("nuisance",   ["4a4150", "7f7486", "a097a6", "bdb4c2", "dad3de", "f3eff5"]),
]
SHARED = ["35203f", "ffffff", "ff7b9c", "fff08a", "f5a742"]  # ink white blush spark spark2
NUM_FLAV = 5
NUIS = 5

BODY_NORMAL = """
...OOOOOO...
..OLLLLBBO..
.OLHHLBBBBO.
OLHHLBBBBBSO
OLLBBBBBBBSO
OLBBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBSSO
OSBBBBBBBSDO
.OSSSSSSSDO.
..OOOOOOOO..
"""

BODY_SQUISH = """
............
............
...OOOOOO...
.OOLLHLBBOO.
OLHHLBBBBBSO
OLLBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBSSO
OSBBBBBBBSDO
OSSSSSSSSDDO
.OOOOOOOOOO.
"""

BODY_STRETCH = """
....OOOO....
...OLLBBO...
..OLHHBBBO..
.OLHHLBBBSO.
.OLLBBBBBSO.
OLBBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBBSO
OBBBBBBBBSSO
OSBBBBBBBSDO
.OSSSSSSSDO.
..OOOOOOOO..
"""

BODY_LEG = {"O": 1, "D": 2, "S": 3, "B": 4, "L": 5, "H": 6}
FACE_LEG = {"K": 7, "W": 8, "P": 9, "B": 4, "S": 3, "L": 5, "D": 2}

# Faces: rows of 12 chars starting at row 4 ('.' keeps the body pixel).
FACES = {
    "strawberry": ["............",
                   "....K..K....",
                   "....K..K....",
                   "..P......P..",
                   ".....KK....."],
    "matcha":     ["............",
                   "...K....K...",
                   "..K.K..K.K..",
                   ".P........P.",
                   ".....KK....."],
    "yuzu":       ["............",
                   "...WK..WK...",
                   "...KK..KK...",
                   ".PP......PP.",
                   ".....KK....."],
    "taro":       ["............",
                   "............",
                   "...KK..KK...",
                   "..P......P..",
                   "....K..K....",
                   ".....KK....."],
    "soda":       ["............",
                   "....K..K....",
                   "....K..K....",
                   "..P..KK..P..",
                   ".....KK....."],
    "nuisance":   ["..D.........",
                   "...D........",
                   "...KK..KK...",
                   "............",
                   "....KKKK...."],
}
BLINK = ["............", "............", "...KK..KK...", "..P......P..", ".....KK....."]
WINCE = ["...K....K...", "....K..K....", "...K....K...", "..P......P..", "....KKKK...."]
SQUISH_FACE = {  # happy squint on landing
    "default": ["............", "............", "...K....K...", "..K.K..K.K..",
                ".P...KK...P.", "............"],
    "nuisance": ["............", "............", "............", "...KK..KK...",
                 "............", "....KKKK...."],
}

# frames emitted per flavour (order matters, mirrored in game.h)
FRAMES = ["normal", "blink", "squish", "wince", "flash", "pop1", "pop2", "stretch"]


def _body(tmpl):
    L = Layer(12, 12)
    L.stamp(tmpl, 0, 0, BODY_LEG)
    return L


def _face(L, rows, y0):
    for j, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in FACE_LEG:
                L.set(x, y0 + j, FACE_LEG[ch])


def _flash(L):
    m = {2: 3, 3: 4, 4: 5, 5: 6, 6: 8}
    for y in range(L.h):
        for x in range(L.w):
            c = L.px[y][x]
            if c in m:
                L.px[y][x] = m[c]


def _pop1():
    # body bursts into four puffs with a sparkle core
    L = Layer(12, 12)
    puff = """
.OO.
OLBO
OBSO
.OO.
"""
    for (x, y) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        L.stamp(puff, x, y, BODY_LEG)
    L.stamp("""
..#..
.#X#.
#XXX#
.#X#.
..#..
""", 4, 4, {"#": 10, "X": 8})
    return L


def _pop2():
    L = Layer(12, 12)
    spark = """
.#.
#X#
.#.
"""
    for (x, y) in ((0, 1), (9, 0), (1, 9), (8, 8), (4, 4)):
        L.stamp(spark, x, y, {"#": 10, "X": 8})
    L.set(5, 0, 11)
    L.set(0, 6, 11)
    L.set(11, 5, 11)
    L.set(6, 11, 11)
    return L


def blob_frames(flav):
    """Returns the list of 12x12 logical-index layers for flavour index flav."""
    name = FLAVOURS[flav][0]
    face = FACES[name]
    out = []
    for fr in FRAMES:
        if fr == "normal":
            L = _body(BODY_NORMAL)
            _face(L, face, 4)
        elif fr == "blink":
            L = _body(BODY_NORMAL)
            _face(L, BLINK if name != "nuisance" else face, 4)
        elif fr == "squish":
            L = _body(BODY_SQUISH)
            _face(L, SQUISH_FACE.get(name, SQUISH_FACE["default"]), 4)
        elif fr == "wince":
            L = _body(BODY_NORMAL)
            _face(L, WINCE, 4)
        elif fr == "flash":
            L = _body(BODY_NORMAL)
            _face(L, WINCE, 4)
            _flash(L)
        elif fr == "pop1":
            L = _pop1()
        elif fr == "pop2":
            L = _pop2()
        elif fr == "stretch":
            L = _body(BODY_STRETCH)
            _face(L, face, 4)
        out.append(L)
    return out


def flavour_rgb(flav):
    """16-entry palette (hex strings) for logical indices of a flavour."""
    ramp = FLAVOURS[flav][1]
    return ["000000"] + ramp + SHARED + ["000000"] * 4


def scaled(L, s):
    out = Layer(L.w * s, L.h * s)
    for y in range(out.h):
        for x in range(out.w):
            out.px[y][x] = L.px[y // s][x // s]
    return out
