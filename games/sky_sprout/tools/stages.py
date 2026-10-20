"""Hand-planned enemy wave scripts for Sky Sprout's five stages.

Each stage is a list of formation "phrases"; a phrase adds spawn events and
returns how long to wait before the next phrase starts. Times are frames on
the stage clock, which pauses while a mid-boss is alive.
"""
EV_SPAWN, EV_MID, EV_WAIT, EV_BOSS, EV_SPEED = range(5)
GNAT, SWOOP, TURRET, MINE, SPIN, DIVER, CARRIER, HEAVY = range(8)
LINE, SINE, SWOOPAI, STOP, DIVE, CHASE, ARC, LOOP, DRIFT, HOVER = range(10)
F_NONE, F_AIM, F_AIM3, F_RING, F_BURST, F_SPRAY, F_HEAVY, F_DEATHRING, F_SPIRAL, F_RAIN = range(10)
D_NONE, D_P, D_S, D_L, D_H, D_O, D_B, D_SHIELD, D_1UP, D_CYCLE = range(10)


def fx(v):
    return int(round(v * 256))


class Script:
    def __init__(self, stage):
        self.stage = stage
        self.ev = []
        self.g = 0

    def add(self, t, kind, typ=0, ai=0, x=0, y=0, p0=0, p1=0, p2=0, fire=0, drop=0, group=0, gsize=0):
        self.ev.append((int(t), kind, typ, ai, fire, drop, group, gsize, int(x), int(y), int(p0), int(p1), int(p2)))

    def spawn(self, t, typ, ai, x, y, p0=0, p1=0, p2=0, fire=0, drop=0, group=0, gsize=0):
        self.add(t, EV_SPAWN, typ, ai, x, y, p0, p1, p2, fire, drop, group, gsize)

    def new_group(self):
        self.g = self.g % 63 + 1
        return self.g

    @property
    def sp(self):
        return 1.0 + 0.08 * self.stage


# ------------------------------------------------------------------ phrases
def gnat_line(S, t, y, n=5, gap=14, spd=1.3, fire=F_NONE, drop=D_P, typ=GNAT):
    g = S.new_group() if drop else 0
    for i in range(n):
        S.spawn(t + i * gap, typ, LINE, 252, y, fx(-spd * S.sp), 0, 0, fire, drop, g, n if g else 0)
    return n * gap + 40


def gnat_sine(S, t, y, n=6, gap=12, spd=1.2, amp=22, freq=4, fire=F_NONE, drop=D_NONE, typ=GNAT):
    g = S.new_group() if drop else 0
    for i in range(n):
        S.spawn(t + i * gap, typ, SINE, 252, y, fx(-spd * S.sp), freq, amp, fire, drop, g, n if g else 0)
    return n * gap + 40


def swoop(S, t, y, n=4, gap=16, spd=1.8, turn=150, up=False, fire=F_NONE, drop=D_NONE):
    g = S.new_group() if drop else 0
    for i in range(n):
        S.spawn(t + i * gap, SWOOP, SWOOPAI, 252, y, fx(-spd * S.sp), turn, -18 if up else 18, fire, drop, g,
                n if g else 0)
    return n * gap + 50


def turrets(S, t, ys=(40, 120), stopx=196, hold=150, fire=F_AIM, drop=D_NONE, stagger=20):
    for i, y in enumerate(ys):
        S.spawn(t + i * stagger, TURRET, STOP, 252, y, fx(-1.6), stopx - (i % 2) * 20, hold, fire, drop)
    return 90 + len(ys) * stagger


def dive(S, t, xs, top=True, turn=None, spd=2.0, out=1.8, fire=F_NONE, gap=14, drop=D_NONE):
    g = S.new_group() if drop else 0
    for i, x in enumerate(xs):
        ty = turn if turn is not None else (60 if top else 100)
        S.spawn(t + i * gap, DIVER, DIVE, x, -12 if top else 172, fx(spd), ty, fx(out * S.sp), fire, drop, g,
                len(xs) if g else 0)
    return len(xs) * gap + 60


def carrier(S, t, y, drop=D_CYCLE, fire=F_NONE):
    S.spawn(t, CARRIER, HOVER, 260, y, fx(-0.9), 170, 200, fire, drop)
    return 90


def heavy(S, t, y, fire=F_HEAVY, drop=D_P, hold=320):
    S.spawn(t, HEAVY, HOVER, 268, y, fx(-0.8), 180, hold, fire, drop)
    return 120


def mines(S, t, ys, spd=0.55, fire=F_DEATHRING, gap=24):
    for i, y in enumerate(ys):
        S.spawn(t + i * gap, MINE, DRIFT, 252, y, fx(-spd * S.sp), 0, 0, fire)
    return len(ys) * gap + 40


def spinners(S, t, y, n=3, gap=30, spd=0.9, amp=30, fire=F_AIM, drop=D_NONE):
    g = S.new_group() if drop else 0
    for i in range(n):
        S.spawn(t + i * gap, SPIN, SINE, 252, y, fx(-spd * S.sp), 3, amp, fire, drop, g, n if g else 0)
    return n * gap + 40


def loopers(S, t, y, n=5, gap=12, spd=1.6, loopx=140, r=1.6, fire=F_NONE, drop=D_P):
    g = S.new_group() if drop else 0
    for i in range(n):
        S.spawn(t + i * gap, GNAT, LOOP, 252, y, fx(-spd * S.sp), loopx, fx(r), fire, drop, g, n if g else 0)
    return n * gap + 80


def chasers(S, t, ys, spd=0.9, fire=F_AIM, gap=20, typ=SWOOP):
    for i, y in enumerate(ys):
        S.spawn(t + i * gap, typ, CHASE, 252, y, fx(-spd * S.sp), 0, 0, fire)
    return len(ys) * gap + 50


def arcs(S, t, xs, vx=-0.9, vy=-3.3, g=0.055, fire=F_NONE, typ=SWOOP, gap=16):
    for i, x in enumerate(xs):
        S.spawn(t + i * gap, typ, ARC, x, 172, fx(vx), fx(vy), fx(g), fire)
    return len(xs) * gap + 50


def rest(S, t, n):
    return n


def speed(S, t, v):
    S.add(t, EV_SPEED, p0=fx(v))
    return 0


# ------------------------------------------------------------------ stage plans
def plan(stage):
    P = []
    a = P.append
    if stage == 0:
        a((speed, dict(v=1.0)))
        a((rest, dict(n=60)))
        a((gnat_line, dict(y=50)))
        a((gnat_line, dict(y=110, drop=D_NONE)))
        a((gnat_sine, dict(y=80)))
        a((swoop, dict(y=30, turn=160)))
        a((carrier, dict(y=70, drop=D_CYCLE)))
        a((gnat_line, dict(y=40, n=4, drop=D_NONE)))
        a((gnat_line, dict(y=120, n=4, drop=D_P)))
        a((turrets, dict(ys=(44, 116), fire=F_AIM)))
        a((gnat_sine, dict(y=60, n=5, amp=18)))
        a((swoop, dict(y=130, up=True, n=5, fire=F_AIM, turn=170)))
        a((dive, dict(xs=(190, 160, 130), top=True)))
        a((loopers, dict(y=70, drop=D_B)))
        a((spinners, dict(y=100, n=3, fire=F_AIM)))
        a((turrets, dict(ys=(30, 80, 130), fire=F_AIM, hold=120)))
        a((gnat_line, dict(y=90, n=6, gap=10, fire=F_NONE, drop=D_P)))
        a("MID")
        a((mines, dict(ys=(40, 100, 70, 130), fire=F_NONE)))
        a((gnat_sine, dict(y=50, n=6)))
        a((carrier, dict(y=110, drop=D_O)))
        a((heavy, dict(y=80, fire=F_HEAVY, drop=D_P)))
        a((rest, dict(n=120)))
        a((swoop, dict(y=24, n=5, turn=150, fire=F_AIM)))
        a((swoop, dict(y=136, n=5, up=True, turn=150)))
        a((dive, dict(xs=(200, 170, 140, 110), top=False)))
        a((gnat_line, dict(y=40, n=5, fire=F_AIM, drop=D_NONE)))
        a((carrier, dict(y=60, drop=D_SHIELD)))
        a((turrets, dict(ys=(40, 120), fire=F_AIM3)))
        a((loopers, dict(y=100, drop=D_P)))
        a((gnat_sine, dict(y=70, n=8, gap=10, amp=30)))
        a("BOSS")
    elif stage == 1:
        a((speed, dict(v=1.6)))
        a((rest, dict(n=60)))
        a((gnat_sine, dict(y=50, n=6, fire=F_NONE, drop=D_P)))
        a((chasers, dict(ys=(30, 130, 80), fire=F_AIM)))
        a((carrier, dict(y=90, drop=D_CYCLE)))
        a((swoop, dict(y=20, n=5, turn=170, fire=F_AIM)))
        a((turrets, dict(ys=(36, 124), fire=F_RAIN, hold=160)))
        a((gnat_line, dict(y=70, n=6, gap=10, drop=D_P)))
        a((dive, dict(xs=(200, 176, 152, 128), top=True, fire=F_AIM)))
        a((spinners, dict(y=60, n=4, fire=F_AIM)))
        a((mines, dict(ys=(30, 130, 80))))
        a((heavy, dict(y=80, drop=D_B)))
        a((rest, dict(n=90)))
        a((swoop, dict(y=140, n=6, up=True, turn=170, fire=F_AIM)))
        a("MID")
        a((chasers, dict(ys=(40, 120, 60, 100), fire=F_AIM3)))
        a((carrier, dict(y=50, drop=D_O)))
        a((gnat_sine, dict(y=110, n=7, amp=26, fire=F_AIM)))
        a((turrets, dict(ys=(28, 80, 132), fire=F_AIM3, hold=140)))
        a((loopers, dict(y=60, drop=D_P)))
        a((heavy, dict(y=50, drop=D_SHIELD)))
        a((heavy, dict(y=120, drop=D_NONE)))
        a((rest, dict(n=200)))
        a((dive, dict(xs=(210, 180, 150, 120, 90), top=False, fire=F_AIM)))
        a((spinners, dict(y=90, n=4, fire=F_SPIRAL, gap=40)))
        a((gnat_line, dict(y=30, n=6, fire=F_AIM, drop=D_P)))
        a((gnat_line, dict(y=130, n=6, fire=F_AIM, drop=D_NONE)))
        a((carrier, dict(y=80, drop=D_CYCLE)))
        a("BOSS")
    elif stage == 2:
        a((speed, dict(v=1.2)))
        a((rest, dict(n=60)))
        a((arcs, dict(xs=(220, 196, 172, 148, 124))))
        a((gnat_line, dict(y=40, n=6, drop=D_P)))
        a((arcs, dict(xs=(230, 200, 170), fire=F_AIM, vx=-1.2)))
        a((carrier, dict(y=60, drop=D_CYCLE)))
        a((swoop, dict(y=30, n=6, turn=150, fire=F_AIM)))
        a((turrets, dict(ys=(40, 90), fire=F_SPRAY, hold=150)))
        a((arcs, dict(xs=(236, 212, 188, 164, 140, 116), typ=DIVER, vy=-3.6, fire=F_AIM)))
        a((loopers, dict(y=50, n=6, drop=D_P)))
        a((mines, dict(ys=(30, 60, 90), fire=F_DEATHRING)))
        a((heavy, dict(y=70, drop=D_B)))
        a((rest, dict(n=100)))
        a("MID")
        a((gnat_sine, dict(y=40, n=7, amp=24, fire=F_AIM, drop=D_P)))
        a((arcs, dict(xs=(230, 210, 190, 170, 150, 130, 110), fire=F_AIM, gap=12)))
        a((carrier, dict(y=50, drop=D_O, fire=F_AIM)))
        a((chasers, dict(ys=(30, 70, 110), fire=F_AIM3)))
        a((turrets, dict(ys=(30, 70, 110), fire=F_AIM3, hold=150)))
        a((spinners, dict(y=60, n=4, fire=F_AIM)))
        a((heavy, dict(y=40, drop=D_SHIELD)))
        a((arcs, dict(xs=(220, 190, 160), typ=DIVER, vy=-3.8, fire=F_AIM)))
        a((heavy, dict(y=100, drop=D_P)))
        a((rest, dict(n=200)))
        a((swoop, dict(y=130, n=6, up=True, fire=F_AIM)))
        a((gnat_line, dict(y=60, n=8, gap=9, fire=F_AIM, drop=D_P)))
        a("BOSS")
    elif stage == 3:
        a((speed, dict(v=1.3)))
        a((rest, dict(n=60)))
        a((spinners, dict(y=50, n=4, fire=F_AIM, drop=D_P)))
        a((loopers, dict(y=110, n=6, fire=F_AIM, drop=D_NONE)))
        a((carrier, dict(y=80, drop=D_CYCLE, fire=F_AIM)))
        a((dive, dict(xs=(200, 170, 140, 110), top=True, fire=F_AIM3)))
        a((turrets, dict(ys=(30, 80, 130), fire=F_RING, hold=150)))
        a((gnat_sine, dict(y=40, n=8, gap=10, amp=20, fire=F_AIM)))
        a((gnat_sine, dict(y=120, n=8, gap=10, amp=20, fire=F_NONE, drop=D_P)))
        a((heavy, dict(y=50)))
        a((heavy, dict(y=110, drop=D_B)))
        a((rest, dict(n=180)))
        a((spinners, dict(y=80, n=5, fire=F_SPIRAL, gap=36)))
        a("MID")
        a((mines, dict(ys=(30, 130, 60, 100, 80), fire=F_DEATHRING, gap=20)))
        a((loopers, dict(y=40, n=6, fire=F_AIM, drop=D_P)))
        a((carrier, dict(y=100, drop=D_O, fire=F_AIM)))
        a((chasers, dict(ys=(30, 130, 50, 110), fire=F_AIM3, typ=DIVER)))
        a((turrets, dict(ys=(40, 120), fire=F_BURST, hold=170)))
        a((dive, dict(xs=(220, 190, 160, 130, 100), top=False, fire=F_AIM3)))
        a((heavy, dict(y=80, drop=D_SHIELD)))
        a((rest, dict(n=150)))
        a((spinners, dict(y=40, n=3, fire=F_SPIRAL, gap=40)))
        a((spinners, dict(y=120, n=3, fire=F_AIM, gap=40, drop=D_P)))
        a((swoop, dict(y=20, n=6, turn=160, fire=F_AIM3)))
        a("BOSS")
    else:
        a((speed, dict(v=1.6)))
        a((rest, dict(n=60)))
        a((turrets, dict(ys=(30, 130), fire=F_AIM3, hold=150)))
        a((gnat_line, dict(y=80, n=8, gap=9, fire=F_AIM, drop=D_P)))
        a((carrier, dict(y=60, drop=D_CYCLE, fire=F_AIM)))
        a((heavy, dict(y=100)))
        a((rest, dict(n=60)))
        a((chasers, dict(ys=(30, 130, 60, 100), fire=F_AIM3)))
        a((spinners, dict(y=50, n=4, fire=F_SPIRAL, gap=36)))
        a((dive, dict(xs=(210, 180, 150, 120, 90), top=True, fire=F_AIM3, drop=D_P)))
        a((mines, dict(ys=(40, 120, 80), fire=F_DEATHRING)))
        a((heavy, dict(y=40, drop=D_B)))
        a((heavy, dict(y=120, drop=D_NONE)))
        a((rest, dict(n=180)))
        a("MID")
        a((loopers, dict(y=60, n=7, fire=F_AIM, drop=D_P)))
        a((turrets, dict(ys=(30, 80, 130), fire=F_RING, hold=160)))
        a((carrier, dict(y=110, drop=D_O, fire=F_AIM)))
        a((gnat_sine, dict(y=40, n=8, gap=10, fire=F_AIM)))
        a((gnat_sine, dict(y=120, n=8, gap=10, fire=F_AIM, drop=D_SHIELD)))
        a((heavy, dict(y=80, fire=F_HEAVY, drop=D_1UP, hold=360)))
        a((rest, dict(n=200)))
        a((swoop, dict(y=20, n=6, turn=170, fire=F_AIM3)))
        a((swoop, dict(y=140, n=6, up=True, turn=170, fire=F_AIM3)))
        a((spinners, dict(y=80, n=5, fire=F_SPIRAL, gap=34)))
        a((carrier, dict(y=70, drop=D_CYCLE, fire=F_AIM)))
        a("BOSS")
    return P


def build(stage):
    S = Script(stage)
    t = 0
    for item in plan(stage):
        if item == "MID":
            S.add(t, EV_WAIT, p0=600)
            t += 60
            S.add(t, EV_MID)
            t += 90
        elif item == "BOSS":
            S.add(t, EV_WAIT, p0=900)
            t += 90
            S.add(t, EV_BOSS)
        else:
            fn, kw = item
            t += fn(S, t, **kw)
    ev = sorted(S.ev, key=lambda e: e[0])
    # the stable sort keeps WAIT/MID/BOSS after spawns scheduled for the same frame
    return ev


STAGES = [build(i) for i in range(5)]
