"""Original music for Mochi Monsters (melody + chord progressions).

Harmony, bass and drums are generated from chord symbols. Output: per-channel
byte streams for the PSG sequencer in src/audio.c.
Stream format (pairs): note(1..127 MIDI) dur | 0 dur (rest) | 0xF0 env | 0xF1 duty | 0xFE end | 0xFF loop
Drums channel: 1 kick, 2 snare, 3 hat, 0 rest.
"""
NOTE = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6,
        "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def midi(n):
    name = n[:-1]
    octv = int(n[-1])
    return 12 * (octv + 1) + NOTE[name]


def parse(seq):
    out = []
    for tok in seq.split():
        n, d = tok.split(":")
        d = int(d)
        out.append((0 if n == "r" else midi(n), d))
    return out


CHORDS = {
    "C": ("C", [0, 4, 7]), "Dm": ("D", [0, 3, 7]), "Em": ("E", [0, 3, 7]), "F": ("F", [0, 4, 7]),
    "G": ("G", [0, 4, 7]), "Am": ("A", [0, 3, 7]), "Bb": ("Bb", [0, 4, 7]), "A": ("A", [0, 4, 7]),
    "Gm": ("G", [0, 3, 7]),
}


def chord_notes(sym, octv):
    root, iv = CHORDS[sym]
    r = 12 * (octv + 1) + NOTE[root]
    return [r + i for i in iv]


def bars_of(prog, bar):
    """prog: list of bars; each bar is 'F' or 'Bb C' (two halves)."""
    out = []
    for b in prog:
        parts = b.split()
        each = bar // len(parts)
        for p in parts:
            out.append((p, each))
    return out


def gen_arp(prog, bar, step=2, octv=4, pattern=(0, 1, 2, 1)):
    ev = []
    for sym, ln in bars_of(prog, bar):
        ns = chord_notes(sym, octv)
        if ns[0] < midi("F4") - 0:
            pass
        t = 0
        i = 0
        while t < ln:
            ev.append((ns[pattern[i % len(pattern)]], min(step, ln - t)))
            t += step
            i += 1
    return ev


def gen_bass(prog, bar, style="bounce"):
    ev = []
    for sym, ln in bars_of(prog, bar):
        root = chord_notes(sym, 2)[0]
        fifth = root + 7
        if style == "bounce":
            if ln == 16:
                ev += [(root, 3), (0, 1), (root + 12, 2), (root, 2), (fifth, 3), (0, 1), (root + 12, 2), (fifth, 2)]
            else:
                ev += [(root, 3), (0, 1), (root + 12, 2), (fifth, 2)][:4] if ln == 8 else [(root, ln)]
        elif style == "waltz":
            ev += [(root, 4), (fifth, 4), (root + 12, 4)] if ln == 12 else [(root, ln)]
        elif style == "walk":
            if ln == 16:
                ev += [(root, 2), (0, 2), (root, 2), (root + 12, 2), (fifth, 2), (0, 2), (fifth, 2), (root + 10, 2)]
            else:
                ev += [(root, 2), (0, 2), (fifth, 2), (root + 12, 2)]
    return ev


def gen_drums(nbars, bar, style="pop"):
    ev = []
    for b in range(nbars):
        if style == "pop":
            pat = [(1, 2), (3, 2), (3, 2), (3, 2), (2, 2), (3, 2), (3, 1), (3, 1), (3, 2)] if b % 4 != 3 else \
                  [(1, 2), (3, 2), (3, 2), (1, 2), (2, 2), (3, 2), (2, 2), (2, 2)]
        elif style == "light":
            pat = [(1, 4), (3, 4), (3, 4)] if bar == 12 else [(1, 4), (3, 4), (2, 4), (3, 4)]
        elif style == "bouncy":
            pat = [(1, 2), (0, 2), (3, 2), (1, 2), (2, 2), (0, 2), (3, 2), (3, 2)]
        ev += pat
        assert sum(d for _, d in pat) == bar, (style, bar)
    return ev


def total(ev):
    return sum(d for _, d in ev)


def encode(ev, env=None, duty=None, loop=True):
    out = []
    if env is not None:
        out += [0xF0, env]
    if duty is not None:
        out += [0xF1, duty]
    for n, d in ev:
        while d > 0:
            dd = min(d, 255)
            out += [n, dd]
            d -= dd
            n = 0 if n else 0  # long notes are split into note + rest tail (rare)
    out.append(0xFF if loop else 0xFE)
    return out


def song(name, tempo, bar, lead, prog, bass_style, drum_style, lead_env=0xA3, harm_env=0x52,
         lead_duty=1, harm_duty=2, arp_pattern=(0, 1, 2, 1), arp_oct=4, loop=True):
    lead_ev = parse(lead)
    nb = len(prog)
    assert total(lead_ev) == nb * bar, (name, total(lead_ev), nb * bar)
    harm = gen_arp(prog, bar, 2, arp_oct, arp_pattern)
    bass = gen_bass(prog, bar, bass_style)
    drums = gen_drums(nb, bar, drum_style)
    for ev in (harm, bass, drums):
        assert total(ev) == nb * bar, (name, total(ev))
    return {


CHORDS.update({
    "D": ("D", [0, 4, 7]), "E": ("E", [0, 4, 7]), "Bm": ("B", [0, 3, 7]), "Cm": ("C", [0, 3, 7]),
    "Eb": ("Eb", [0, 4, 7]), "Ab": ("Ab", [0, 4, 7]), "Fm": ("F", [0, 3, 7]),
})

TITLE = song(
    "title", 7, 16,
    "C5:4 F5:4 A5:4 C6:4 D6:4 C6:2 A5:2 F5:8 D5:4 F5:4 Bb5:4 D6:4 C6:4 Bb5:2 A5:2 G5:8 "
    "A5:2 Bb5:2 C6:4 F6:4 C6:4 D6:2 C6:2 A5:4 F5:4 D5:4 Bb5:4 D6:4 C6:4 E5:4 F5:12 r:4",
    ["F", "Dm", "Bb", "C", "F", "Dm", "Bb C", "F"],
    "bounce", "light", lead_env=0x95, lead_duty=2, harm_env=0x43, harm_duty=0,
    arp_pattern=(0, 1, 2, 1), arp_oct=4)

TOWN = song(
    "town", 7, 16,
    "G5:4 B5:2 D6:2 B5:4 G5:4 E5:4 G5:2 B5:2 G5:8 C5:2 E5:2 G5:2 C6:2 B5:4 A5:4 F#5:4 A5:4 D6:4 r:4 "
    "G5:2 A5:2 B5:4 D6:4 B5:4 C6:2 B5:2 A5:2 G5:2 E5:8 E5:2 G5:2 C6:4 A5:4 F#5:4 G5:8 r:8",
    ["G", "Em", "C", "D", "G", "Em", "C D", "G"],
    "bounce", "light", lead_env=0x94, lead_duty=2, harm_env=0x42, harm_duty=1,
    arp_pattern=(0, 1, 2, 1), arp_oct=4)

ROUTE = song(
    "route", 6, 16,
    "E5:2 G5:2 C6:4 G5:2 E5:2 G5:4 D5:2 G5:2 B5:4 A5:2 G5:2 D5:4 C5:2 E5:2 A5:4 G5:2 E5:2 C5:4 "
    "F5:4 A5:4 C6:4 A5:4 G5:2 E5:2 C6:4 D6:2 E6:2 C6:4 B5:2 G5:2 D6:4 B5:4 G5:4 "
    "A5:4 F5:4 G5:2 A5:2 B5:4 C6:8 G5:4 r:4",
    ["C", "G", "Am", "F", "C", "G", "F G", "C"],
    "walk", "pop", lead_env=0xA3, lead_duty=2, harm_env=0x52, harm_duty=2)

BATTLE = song(
    "battle", 4, 16,
    "A4:2 C5:2 E5:2 A5:2 G5:2 E5:2 C5:2 E5:2 A5:4 B5:2 C6:2 B5:4 G5:4 "
    "F5:2 A5:2 C6:2 A5:2 F5:2 A5:2 C6:2 D6:2 B5:4 G5:4 D5:4 G5:4 "
    "A5:2 A5:2 E6:4 D6:2 C6:2 B5:4 C6:2 B5:2 A5:4 E5:4 A5:4 "
    "F5:2 A5:2 C6:4 D6:2 B5:2 G5:4 G#5:4 B5:4 E6:4 r:4",
    ["Am", "Am", "F", "G", "Am", "Am", "F G", "E"],
    "bounce", "pop", lead_env=0xB2, lead_duty=2, harm_env=0x62, harm_duty=1,
    arp_pattern=(0, 1, 2, 1), arp_oct=4)

BOSS = song(
    "boss", 4, 16,
    "D5:2 F5:2 A5:2 D6:2 C6:2 A5:2 F5:2 A5:2 Bb5:4 A5:2 G5:2 F5:4 D5:4 "
    "E5:2 G5:2 C6:2 G5:2 E6:4 C6:4 C#6:4 A5:4 E5:4 A5:4 "
    "D6:2 D6:2 A5:4 F5:2 G5:2 A5:4 Bb5:2 A5:2 G5:2 F5:2 D5:8 "
    "G5:2 Bb5:2 D6:4 E6:2 C#6:2 A5:4 D6:8 r:8",
    ["Dm", "Bb", "C", "A", "Dm", "Bb", "Gm A", "Dm"],
    "walk", "pop", lead_env=0xB2, lead_duty=1, harm_env=0x62, harm_duty=2,
    arp_pattern=(0, 2, 1, 2), arp_oct=4)

ENDING = song(
    "ending", 9, 16,
    "C5:4 F5:4 A5:4 C6:4 D6:4 C6:2 A5:2 F5:8 D5:4 F5:4 Bb5:4 D6:4 C6:4 Bb5:2 A5:2 G5:8 "
    "A5:2 Bb5:2 C6:4 F6:4 C6:4 D6:2 C6:2 A5:4 F5:4 D5:4 Bb5:4 D6:4 C6:4 E5:4 F5:12 r:4",
    ["F", "Dm", "Bb", "C", "F", "Dm", "Bb C", "F"],
    "walk", "light", lead_env=0x86, lead_duty=2, harm_env=0x43, harm_duty=2,
    arp_pattern=(0, 1, 2, 1, 2, 1, 0, 2), arp_oct=4)

CAVE = song(
    "cave", 8, 16,
    "A4:4 C5:4 E5:8 D5:4 F5:4 A5:8 E5:4 G#5:4 B5:8 A5:8 r:8 "
    "C6:4 B5:4 A5:8 F5:4 E5:4 D5:8 E5:4 B4:4 G#4:4 E5:4 A4:12 r:4",
    ["Am", "Dm", "E", "Am", "Am", "Dm", "E", "Am"],
    "walk", "light", lead_env=0x84, lead_duty=0, harm_env=0x32, harm_duty=2,
    arp_pattern=(0, 2, 1, 2), arp_oct=4)


def jingle(name, tempo, lead, harm, bass, drums):
    return {"name": name, "tempo": tempo,
            "ch": [encode(parse(harm), 0x72, 2, loop=False), encode(parse(lead), 0xB2, 1, loop=False),
                   encode(parse(bass), loop=False), encode(drums, loop=False)]}


VICTORY = jingle("victory", 5,
                 "G5:2 G5:2 G5:2 C6:6 D6:2 E6:2 D6:2 E6:2 G6:10 r:4",
                 "E5:2 E5:2 E5:2 G5:6 B5:2 C6:2 B5:2 C6:2 E6:10 r:4",
                 "C3:4 C3:2 G3:6 G3:4 C4:4 C3:10 r:4",
                 [(1, 2), (3, 2), (2, 2), (3, 6), (1, 4), (2, 4), (1, 10), (0, 4)])
HEAL = jingle("heal", 5,
              "C6:2 E6:2 G6:2 E6:2 C7:8 r:4",
              "G5:2 C6:2 E6:2 C6:2 G6:8 r:4",
              "C4:4 G3:4 C4:8 r:4",
              [(0, 20)])
CATCH = jingle("catch", 5,
               "C6:2 D6:2 E6:2 G6:2 E6:2 G6:2 C7:8 r:4",
               "E5:2 F5:2 G5:2 B5:2 G5:2 B5:2 E6:8 r:4",
               "C3:4 G3:4 E3:4 C3:8 r:4",
               [(1, 4), (2, 4), (1, 4), (2, 4), (1, 4), (0, 4)])

SONGS = [TITLE, TOWN, ROUTE, BATTLE, BOSS, ENDING, CAVE, VICTORY, HEAL, CATCH]


def square_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    v = int(round(2048 - 131072.0 / hz))
    return max(0, min(2047, v))


def wave_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    v = int(round(2048 - 65536.0 / hz))
    return max(0, min(2047, v))
