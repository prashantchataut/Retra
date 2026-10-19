"""Hand-designed levels for Mochi Hop.

Grid cells (16x16 metatiles):  '.' air  '#' ground  'B' block  '=' one-way cloud
                                '^' spikes  'f' 't' 'm' decorations (non-solid)
Objects: P start, o star, S sprout, r raindrop, b bee, k spiky, C checkpoint, G goal, J spring

Physics reference: a full jump clears ~3 tiles up and ~5 tiles across; springs launch ~8 tiles.
"""

OBJ_TYPES = {"o": 1, "S": 2, "r": 3, "b": 4, "k": 5, "C": 6, "G": 7, "J": 8}


class Lvl:
    def __init__(self, name, theme, w, h):
        self.name = name
        self.theme = theme
        self.w = w
        self.h = h
        self.g = [["."] * w for _ in range(h)]
        self.objs = []
        self.start = (2, h - 3)

    def ground(self, x0, x1, top, bottom=None):
        bottom = self.h - 1 if bottom is None else bottom
        for x in range(x0, x1 + 1):
            for y in range(top, bottom + 1):
                self.g[y][x] = "#"
        return self

    def clear(self, x0, x1, y0, y1):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.g[y][x] = "."
        return self

    def blocks(self, x, y, n=1, vertical=False):
        for i in range(n):
            if vertical:
                self.g[y + i][x] = "B"
            else:
                self.g[y][x + i] = "B"
        return self

    def plat(self, x, y, n):
        for i in range(n):
            self.g[y][x + i] = "="
        return self

    def spikes(self, x, y, n=1):
        for i in range(n):
            self.g[y][x + i] = "^"
        return self

    def deco(self, x, y, ch):
        if self.g[y][x] == ".":
            self.g[y][x] = ch
        return self

    def put(self, x, y, ch):
        if ch == "P":
            self.start = (x, y)
        else:
            self.objs.append((OBJ_TYPES[ch], x, y))
        return self

    def stars(self, x, y, n=1, dx=1, dy=0):
        for i in range(n):
            self.put(x + i * dx, y + i * dy, "o")
        return self

    def arc(self, x, y, n):
        """Arc of stars over a gap starting at (x, y)."""
        hts = {3: [0, -1, 0], 4: [0, -1, -1, 0], 5: [0, -1, -2, -1, 0], 6: [0, -1, -2, -2, -1, 0]}[n]
        for i, h in enumerate(hts):
            self.put(x + i, y + h, "o")
        return self

    def rows(self):
        out = [r[:] for r in self.g]
        for t, x, y in self.objs:
            ch = [k for k, v in OBJ_TYPES.items() if v == t][0]
            out[y][x] = ch
        sx, sy = self.start
        out[sy][sx] = "P"
        return ["".join(r) for r in out]

    def grid(self):
        return ["".join(r) for r in self.g]

    def check(self):
        for t, x, y in self.objs:
            assert 0 <= x < self.w and 0 <= y < self.h, (self.name, t, x, y)
            assert self.g[y][x] in ".ftm", (self.name, "object inside solid", t, x, y, self.g[y][x])
        assert sum(1 for t, _, _ in self.objs if t == 7) == 1, self.name
        assert sum(1 for t, _, _ in self.objs if t == 2) == 3, (self.name, "needs 3 sprouts")


def level_1_1():
    L = Lvl("FLUFFY FIELDS", 0, 124, 13)
    L.put(3, 10, "P")
    L.ground(0, 14, 11)
    L.deco(1, 10, "t").deco(5, 10, "f").deco(9, 10, "t").deco(12, 10, "f")
    L.stars(6, 8, 4)
    # first little gap
    L.arc(14, 8, 4)
    L.ground(17, 31, 11).ground(22, 25, 9)
    L.deco(18, 10, "f").deco(23, 8, "t").deco(29, 10, "t")
    L.stars(22, 7, 4)
    L.put(28, 10, "r")
    # cloud bridge
    L.plat(32, 8, 3).stars(32, 7, 3)
    L.ground(35, 53, 11)
    L.deco(36, 10, "f").deco(46, 10, "m").deco(51, 10, "t")
    L.plat(38, 8, 4)
    L.plat(43, 5, 3).put(44, 4, "S")
    L.stars(38, 7, 4)
    L.put(48, 10, "r")
    L.put(52, 10, "C")
    # hills
    L.ground(54, 61, 11).ground(57, 60, 10)
    L.ground(62, 65, 8).deco(63, 7, "f")
    L.ground(66, 71, 11)
    L.stars(62, 6, 4)
    L.put(68, 7, "b")
    # four-wide gap
    L.arc(71, 8, 5)
    L.ground(76, 92, 11)
    L.put(79, 10, "J")
    L.plat(76, 3, 7).stars(76, 2, 3).put(81, 2, "S")
    L.blocks(86, 7, 3).stars(86, 6, 3)
    L.put(84, 10, "r").put(90, 10, "r")
    L.deco(77, 10, "t").deco(88, 10, "f")
    # last steps
    L.ground(95, 123, 11)
    L.ground(99, 101, 10).ground(102, 104, 9).ground(105, 107, 8)
    L.put(106, 7, "S")
    L.stars(99, 8, 3).stars(102, 7, 3)
    L.deco(110, 10, "f").deco(114, 10, "t").deco(119, 10, "f").deco(97, 10, "m")
    L.put(115, 9, "G")
    return L


def level_1_2():
    L = Lvl("BREEZY BRIDGES", 0, 136, 14)
    L.put(2, 11, "P")
    L.ground(0, 10, 12)
    L.deco(5, 11, "f").deco(8, 11, "t")
    L.stars(5, 9, 3)
    # long cloud bridge with a bee
    L.plat(11, 10, 4).plat(17, 9, 4).plat(23, 10, 4)
    L.stars(12, 8, 3).stars(18, 7, 3).stars(24, 8, 3)
    L.put(19, 6, "b")
    L.ground(28, 40, 12)
    L.spikes(33, 11, 2)
    L.stars(32, 8, 4)
    L.put(38, 11, "r")
    L.deco(29, 11, "t").deco(36, 11, "f")
    # stair of clouds up to a sprout
    L.plat(41, 9, 3).plat(45, 6, 3).plat(49, 3, 3).put(50, 2, "S")
    L.stars(45, 5, 3)
    L.plat(53, 8, 4)
    L.ground(58, 70, 12).ground(64, 70, 10)
    L.put(60, 11, "C")
    L.put(67, 9, "r")
    L.deco(59, 11, "f").deco(66, 9, "t").deco(69, 9, "f")
    # thorn valley crossed by clouds
    L.ground(71, 92, 13)
    L.spikes(71, 12, 22)
    L.plat(72, 9, 3).plat(77, 8, 3).plat(82, 9, 3).plat(87, 8, 3)
    L.put(80, 5, "b").put(88, 5, "b")
    L.stars(72, 8, 3).stars(77, 7, 3).stars(82, 8, 3).stars(87, 7, 3)
    L.put(85, 4, "S")
    L.ground(93, 104, 12)
    L.put(96, 11, "J")
    L.blocks(99, 5, 4).stars(99, 4, 4)
    L.put(102, 11, "r")
    L.deco(94, 11, "t").deco(104, 11, "f")
    L.plat(105, 10, 3).plat(110, 8, 3).plat(115, 10, 3)
    L.stars(110, 7, 3)
    L.put(112, 5, "b")
    L.ground(119, 135, 12)
    L.put(121, 11, "S")
    L.deco(123, 11, "f").deco(126, 11, "t").deco(131, 11, "f").deco(134, 11, "t")
    L.put(128, 10, "G")
    return L


def level_2_1():
    L = Lvl("GUMDROP GROTTO", 1, 128, 13)
    L.put(2, 10, "P")
    L.ground(0, 127, 0, 1)          # cave ceiling
    L.ground(0, 16, 11)
    L.ground(12, 16, 2, 3)
    L.deco(5, 10, "m").deco(9, 10, "f")
    L.stars(6, 8, 4)
    L.put(14, 10, "r")
    L.arc(17, 8, 4)
    L.ground(21, 34, 11)
    L.spikes(26, 10, 3)
    L.blocks(26, 7, 3).stars(26, 6, 3)
    L.put(32, 10, "k")
    L.deco(22, 10, "f").deco(30, 10, "m")
    L.ground(28, 40, 2, 4)
    L.ground(35, 46, 11)
    L.put(38, 10, "J")
    L.plat(36, 5, 3).plat(41, 6, 4)
    L.put(42, 5, "S")
    L.stars(36, 4, 3)
    L.put(45, 10, "r")
    L.put(47, 10, "C")
    L.ground(47, 58, 11)
    L.ground(50, 52, 9).ground(55, 58, 7)
    L.put(56, 6, "k")
    L.stars(50, 7, 3)
    L.ground(59, 70, 11)
    L.ground(60, 70, 2, 6)
    L.stars(60, 9, 10)
    L.put(66, 10, "r")
    L.ground(71, 74, 11).spikes(71, 10, 4)
    L.plat(71, 7, 4).stars(71, 6, 4)
    L.ground(75, 92, 11)
    L.blocks(80, 7, 2).blocks(84, 5, 2).blocks(88, 3, 1)
    L.put(88, 2, "S")
    L.stars(80, 6, 2).stars(84, 4, 2)
    L.put(86, 10, "k").put(90, 10, "r")
    L.deco(76, 10, "m").deco(82, 10, "f")
    L.arc(93, 8, 4)
    L.ground(97, 127, 11)
    L.ground(100, 110, 2, 5)
    L.put(103, 10, "r").put(108, 10, "r")
    L.stars(101, 9, 4)
    L.ground(112, 114, 10)
    L.put(113, 9, "S")
    L.deco(117, 10, "m").deco(121, 10, "f").deco(125, 10, "m")
    L.put(120, 9, "G")
    return L


def level_2_2():
    L = Lvl("SUGAR SHAFTS", 1, 116, 16)
    L.put(2, 13, "P")
    L.ground(0, 115, 0, 1)
    L.ground(0, 12, 14)
    L.deco(6, 13, "m").deco(10, 13, "f")
    L.stars(5, 11, 3)
    L.put(11, 13, "J")
    L.ground(13, 16, 6, 15)
    L.stars(11, 9, 1, 0, -1).stars(11, 6, 3, 0, -1)
    L.ground(17, 30, 6, 7)
    L.put(22, 5, "r")
    L.put(29, 5, "S")
    L.stars(19, 4, 4)
    # drop down shaft
    L.ground(17, 34, 14)
    L.spikes(23, 13, 4)
    L.plat(22, 11, 6)
    L.stars(31, 12, 1).stars(32, 12, 1).stars(33, 11, 1)
    L.ground(35, 46, 14)
    L.ground(35, 38, 11)
    L.blocks(41, 10, 3).stars(41, 9, 3)
    L.put(44, 13, "k")
    L.put(46, 13, "C")
    L.ground(47, 50, 14)
    L.put(49, 13, "J")
    L.ground(51, 60, 4, 6)
    L.stars(51, 3, 4).put(58, 3, "S")
    L.plat(52, 10, 3).plat(57, 11, 3)
    L.ground(51, 66, 14).spikes(52, 13, 9)
    L.put(56, 9, "b")
    L.ground(61, 66, 12)
    L.stars(62, 10, 4)
    L.put(64, 11, "k")
    L.plat(67, 10, 3).plat(71, 8, 3).plat(75, 6, 3)
    L.ground(67, 82, 15).spikes(67, 14, 16)
    L.stars(67, 9, 3).stars(71, 7, 3).stars(75, 5, 3)
    L.put(73, 4, "b")
    L.ground(79, 92, 7, 8)
    L.put(86, 6, "r")
    L.put(90, 6, "S")
    L.deco(82, 6, "m")
    L.ground(83, 115, 14)
    L.plat(93, 11, 3)
    L.stars(95, 12, 4)
    L.put(100, 13, "k").put(104, 13, "r")
    L.deco(98, 13, "f").deco(107, 13, "m").deco(113, 13, "f")
    L.put(110, 12, "G")
    return L


def level_3_1():
    L = Lvl("TWINKLE PATH", 2, 128, 14)
    L.put(2, 11, "P")
    L.ground(0, 12, 12)
    L.deco(4, 11, "m").deco(8, 11, "t")
    L.stars(6, 9, 4)
    L.put(11, 11, "r")
    L.plat(13, 10, 3).plat(18, 8, 3).plat(23, 10, 3)
    L.stars(13, 9, 3).stars(18, 7, 3).stars(23, 9, 3)
    L.put(18, 5, "b")
    L.ground(27, 42, 12)
    L.ground(32, 35, 9).ground(33, 34, 7)
    L.put(33, 6, "S")
    L.put(30, 11, "r").put(39, 11, "k")
    L.deco(28, 11, "m").deco(37, 11, "t")
    L.arc(43, 9, 5)
    L.ground(48, 60, 12)
    L.put(50, 11, "C")
    L.put(55, 11, "J")
    L.plat(53, 4, 5).stars(53, 3, 5)
    L.plat(59, 4, 3).put(60, 3, "S")
    L.put(58, 11, "r")
    L.ground(61, 64, 13).spikes(61, 12, 4)
    L.plat(61, 9, 4)
    L.put(63, 6, "b")
    L.ground(65, 80, 12)
    L.blocks(68, 9, 1).blocks(70, 7, 1).blocks(72, 5, 1)
    L.stars(68, 8, 1).stars(70, 6, 1).stars(72, 4, 1)
    L.put(76, 11, "k").put(79, 11, "r")
    L.deco(66, 11, "t").deco(74, 11, "m")
    L.plat(81, 10, 2).plat(85, 9, 2).plat(89, 8, 2).plat(93, 9, 2)
    L.stars(81, 9, 2).stars(85, 8, 2).stars(89, 7, 2).stars(93, 8, 2)
    L.put(87, 5, "b").put(95, 6, "b")
    L.ground(97, 127, 12)
    L.ground(101, 104, 10)
    L.put(103, 9, "S")
    L.put(107, 11, "r").put(110, 11, "k")
    L.deco(99, 11, "m").deco(113, 11, "t").deco(117, 11, "m").deco(124, 11, "t")
    L.put(120, 10, "G")
    return L


def level_3_2():
    L = Lvl("MOONLIT SUMMIT", 2, 140, 16)
    L.put(2, 13, "P")
    L.ground(0, 14, 14)
    L.deco(5, 13, "m").deco(9, 13, "t")
    L.stars(6, 11, 3)
    L.ground(15, 20, 12).ground(21, 26, 10)
    L.put(18, 11, "r").put(24, 9, "k")
    L.stars(21, 7, 4)
    L.arc(27, 8, 5)
    L.ground(32, 44, 10)
    L.blocks(35, 6, 4).stars(35, 5, 4)
    L.put(41, 9, "r")
    L.put(38, 3, "b")
    L.ground(45, 48, 15).spikes(45, 14, 4)
    L.plat(45, 9, 4)
    L.ground(49, 60, 10)
    L.put(51, 9, "C")
    L.put(57, 9, "J")
    L.plat(53, 2, 3).put(54, 1, "S")
    L.stars(55, 2, 1).stars(56, 1, 1).stars(57, 0, 1)
    L.put(59, 9, "k")
    L.ground(61, 82, 15).spikes(61, 14, 22)
    L.plat(61, 9, 2).plat(65, 7, 2).plat(69, 9, 2).plat(73, 7, 2).plat(77, 9, 2)
    L.stars(65, 6, 2).stars(73, 6, 2).stars(69, 8, 2)
    L.put(67, 4, "b").put(75, 4, "b")
    L.ground(81, 94, 10)
    L.blocks(85, 6, 1).blocks(88, 6, 1).put(86, 9, "r").put(91, 9, "k")
    L.stars(85, 5, 1).stars(88, 5, 1)
    L.put(93, 9, "S")
    L.ground(95, 100, 12).ground(101, 104, 14)
    L.put(102, 13, "J")
    L.plat(98, 3, 4).stars(98, 2, 4)
    L.ground(105, 116, 8)
    L.put(108, 7, "r").put(113, 7, "k")
    L.stars(108, 5, 5)
    L.put(116, 7, "C")
    L.ground(117, 139, 12)
    L.ground(120, 123, 11).ground(124, 127, 10).ground(128, 139, 9)
    L.stars(120, 9, 4).stars(124, 8, 4)
    L.put(126, 9, "r")
    L.deco(130, 8, "m").deco(137, 8, "t")
    L.put(133, 7, "G")
    return L


def all_levels():
    lv = [level_1_1(), level_1_2(), level_2_1(), level_2_2(), level_3_1(), level_3_2()]
    for l in lv:
        l.check()
    return lv


if __name__ == "__main__":
    for l in all_levels():
        print(l.name, l.w, l.h, "stars", sum(1 for t, _, _ in l.objs if t == 1))
        for r in l.rows():
            print(r)
