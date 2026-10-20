"""Original music for Turbo Drift (melody + chord progressions -> PSG streams).

Format (pairs) for the sequencer in src/audio.c (same as Mochi Hop):
  note(1..127 MIDI) dur | 0 dur (rest) | 0xF0 env | 0xF1 duty | 0xFE end | 0xFF loop
Drum channel notes: 1 kick, 2 snare, 3 closed hat, 4 open hat.
During races channel 0 (square 1) is taken over by the engine sound, so race
songs keep their identity in the lead, bass and drums.
"""

NOTE = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6,
        "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def midi(n):
    return 12 * (int(n[-1]) + 1) + NOTE[n[:-1]]


def parse(seq):
    out = []
    for tok in seq.split():
        n, d = tok.split(":")
        out.append((0 if n == "r" else midi(n), int(d)))
    return out


CHORDS = {
    "C": ("C", [0, 4, 7]), "Dm": ("D", [0, 3, 7]), "Em": ("E", [0, 3, 7]), "F": ("F", [0, 4, 7]),
    "G": ("G", [0, 4, 7]), "Am": ("A", [0, 3, 7]), "Bb": ("Bb", [0, 4, 7]), "A": ("A", [0, 4, 7]),
    "Gm": ("G", [0, 3, 7]), "E": ("E", [0, 4, 7]), "B": ("B", [0, 4, 7]), "D": ("D", [0, 4, 7]),
    "Bm": ("B", [0, 3, 7]), "Eb": ("Eb", [0, 4, 7]),
    "Dm7": ("D", [0, 3, 7, 10]), "G7": ("G", [0, 4, 7, 10]), "Cmaj7": ("C", [0, 4, 7, 11]),
    "Am7": ("A", [0, 3, 7, 10]),
}


def chord_notes(sym, octv):
    root, iv = CHORDS[sym]
    r = 12 * (octv + 1) + NOTE[root]
    return [r + i for i in iv]


def bars_of(prog, bar):
    out = []
    for b in prog:
        parts = b.split()
        each = bar // len(parts)
        for p in parts:
            out.append((p, each))
    return out


def gen_arp(prog, bar, step, octv, pattern):
    ev = []
    for sym, ln in bars_of(prog, bar):
        ns = chord_notes(sym, octv)
        t = 0
        i = 0
        while t < ln:
            p = pattern[i % len(pattern)]
            ev.append((ns[p % len(ns)] + 12 * (p // len(ns)), min(step, ln - t)))
            t += step
            i += 1
    return ev


def gen_bass(prog, bar, style):
    ev = []
    for sym, ln in bars_of(prog, bar):
        root = chord_notes(sym, 2)[0]
        if root < 40:
            root += 12
        fifth = root + 7
        if style == "octave":
            k = ln // 2
            for i in range(k):
                ev.append((root if i % 2 == 0 else root + 12, 2))
        elif style == "drive":
            for i in range(ln // 2):
                ev.append((root if i % 4 != 3 else fifth, 1))
                ev.append((0, 1))
        elif style == "funk":
            if ln == 16:
                ev += [(root, 3), (0, 1), (root + 12, 1), (0, 1), (root, 2), (fifth, 2), (0, 2), (root + 10, 2),
                       (root + 12, 2)]
            else:
                ev += [(root, ln)]
    return ev


def gen_drums(nbars, bar, style):
    ev = []
    for b in range(nbars):
        fill = b % 8 == 7
        if style == "race":
            pat = [(1, 2), (3, 2), (2, 2), (3, 2), (1, 2), (1, 2), (2, 2), (4, 2)]
            if fill:
                pat = [(1, 2), (3, 2), (2, 2), (3, 2), (2, 1), (2, 1), (2, 2), (2, 1), (2, 1), (4, 2)]
        elif style == "four":
            pat = [(1, 2), (3, 2), (1, 2), (3, 1), (3, 1), (1, 2), (3, 2), (1, 2), (4, 2)]
            if fill:
                pat = [(1, 2), (3, 2), (1, 2), (3, 2), (2, 2), (2, 2), (2, 1), (2, 1), (2, 1), (2, 1)]
        elif style == "funk":
            pat = [(1, 3), (3, 1), (2, 2), (3, 2), (0, 2), (1, 2), (2, 2), (3, 2)]
        elif style == "beat":
            pat = [(1, 2), (3, 2), (2, 2), (3, 2), (1, 2), (3, 2), (2, 2), (3, 2)]
        else:
            pat = [(0, bar)]
        assert sum(d for _, d in pat) == bar, (style, bar)
        ev += pat
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
            n = 0
    out.append(0xFF if loop else 0xFE)
    return out


def song(name, tempo, lead, prog, bass_style, drum_style, lead_env=0xA3, harm_env=0x52, lead_duty=2,
         harm_duty=1, arp_pattern=(0, 1, 2, 1), arp_oct=4, arp_step=2, bar=16, loop=True, bass_env=None):
    lead_ev = parse(lead)
    nb = len(prog)
    assert total(lead_ev) == nb * bar, (name, total(lead_ev), nb * bar)
    harm = gen_arp(prog, bar, arp_step, arp_oct, arp_pattern)
    bass = gen_bass(prog, bar, bass_style)
    drums = gen_drums(nb, bar, drum_style)
    for ev in (harm, bass, drums):
        assert total(ev) == nb * bar, (name, total(ev))
    return {"name": name, "tempo": tempo,
            "ch": [encode(harm, harm_env, harm_duty, loop), encode(lead_ev, lead_env, lead_duty, loop),
                   encode(bass, loop=loop), encode(drums, loop=loop)]}


TITLE = song(
    "title", 5,
    "A5:2 C6:2 E6:2 A5:2 G5:2 A5:2 C6:4 "
    "A5:2 F5:2 A5:2 C6:2 D6:2 C6:2 A5:4 "
    "G5:2 E5:2 G5:2 C6:2 E6:4 D6:2 C6:2 "
    "B5:4 D6:4 G5:4 B5:4 "
    "A5:2 C6:2 E6:2 A6:2 G6:2 E6:2 C6:4 "
    "F6:2 E6:2 C6:2 A5:2 C6:4 F5:4 "
    "E6:3 D6:1 C6:2 G5:2 E6:4 D6:4 "
    "D6:4 B5:4 G5:4 B5:2 D6:2 "
    "C6:4 A5:2 C6:2 F6:4 E6:4 "
    "D6:4 B5:2 D6:2 G6:4 F6:4 "
    "E6:2 D6:2 B5:2 G5:2 B5:4 E6:4 "
    "C6:2 B5:2 A5:2 E5:2 A5:8 "
    "A5:2 C6:2 F6:2 C6:2 A6:4 G6:4 "
    "G6:2 F6:2 D6:2 B5:2 D6:4 G6:4 "
    "G#6:4 E6:4 B5:4 G#5:4 "
    "E6:8 r:4 E5:2 G#5:2",
    ["Am", "F", "C", "G", "Am", "F", "C", "G", "F", "G", "Em", "Am", "F", "G", "E", "E"],
    "octave", "four", lead_env=0xB4, harm_env=0x62, lead_duty=2, harm_duty=1, arp_pattern=(0, 1, 2, 3, 2, 1),
    arp_step=1, arp_oct=4)

MENU = song(
    "menu", 7,
    "F5:3 A5:3 C6:2 r:2 D6:2 C6:2 A5:2 "
    "B5:4 r:2 G5:2 F5:2 G5:2 B5:4 "
    "E5:3 G5:3 B5:2 r:2 C6:2 B5:2 G5:2 "
    "A5:6 r:2 E5:2 G5:2 A5:4 "
    "F5:2 A5:2 C6:2 E6:2 D6:4 C6:4 "
    "B5:2 D6:2 F6:4 E6:2 D6:2 B5:4 "
    "C6:4 B5:2 G5:2 E5:4 G5:4 "
    "A5:8 r:8",
    ["Dm7", "G7", "Cmaj7", "Am7", "Dm7", "G7", "Cmaj7", "Am7"],
    "funk", "funk", lead_env=0x93, harm_env=0x42, lead_duty=2, harm_duty=0, arp_pattern=(0, 1, 2, 3),
    arp_step=2, arp_oct=4)

RACE_CITY = song(
    "city", 6,
    "E5:2 G5:2 B5:2 E6:2 D6:2 B5:2 G5:2 B5:2 "
    "C6:4 B5:2 G5:2 E5:4 G5:4 "
    "F#5:2 A5:2 D6:2 F#6:2 E6:2 D6:2 A5:4 "
    "D#6:4 B5:4 F#5:4 D#6:4 "
    "E6:2 B5:2 G5:2 E5:2 G5:2 B5:2 E6:2 G6:2 "
    "F#6:2 E6:2 C6:2 G5:2 E6:4 C6:4 "
    "D6:2 E6:2 F#6:2 A6:2 G6:2 F#6:2 D6:4 "
    "F#6:6 D#6:2 B5:8 "
    "G5:2 C6:2 E6:2 G6:2 E6:4 C6:4 "
    "A5:2 D6:2 F#6:2 A6:2 F#6:4 D6:4 "
    "B5:2 D6:2 F#6:2 B6:2 A6:2 F#6:2 D6:4 "
    "G6:4 E6:4 B5:4 G5:4 "
    "E6:2 G6:2 C7:4 B6:2 G6:2 E6:4 "
    "F#6:2 A6:2 D7:4 C7:2 A6:2 F#6:4 "
    "D#7:4 B6:4 F#6:4 D#6:4 "
    "B6:8 F#6:4 D#6:4",
    ["Em", "C", "D", "B", "Em", "C", "D", "B", "C", "D", "Bm", "Em", "C", "D", "B", "B"],
    "octave", "race", lead_env=0xB3, lead_duty=2)

RACE_COAST = song(
    "coast", 6,
    "A5:2 C6:2 F6:4 E6:2 C6:2 A5:4 "
    "B5:2 D6:2 G6:4 F6:2 D6:2 B5:4 "
    "G5:2 B5:2 E6:2 G6:2 E6:2 D6:2 B5:4 "
    "C6:4 A5:4 E5:4 A5:4 "
    "D6:2 F6:2 A6:4 G6:2 F6:2 D6:4 "
    "D6:2 G6:2 B6:4 A6:2 G6:2 D6:4 "
    "E6:2 G6:2 C7:4 B6:2 G6:2 E6:4 "
    "C7:8 G6:4 E6:4 "
    "F6:3 E6:1 C6:2 A5:2 C6:4 F6:4 "
    "G6:3 F6:1 D6:2 B5:2 D6:4 G6:4 "
    "A6:4 G6:2 E6:2 C6:4 E6:4 "
    "A5:2 B5:2 C6:2 E6:2 A6:8 "
    "F6:2 E6:2 D6:2 A5:2 D6:4 F6:4 "
    "G6:2 F6:2 E6:2 B5:2 E6:4 G6:4 "
    "A6:4 F6:4 C6:4 A5:4 "
    "B5:4 D6:4 G6:4 B6:4",
    ["F", "G", "Em", "Am", "Dm", "G", "C", "C", "F", "G", "Am", "Am", "Dm", "Em", "F", "G"],
    "octave", "beat", lead_env=0xA4, lead_duty=1)

RACE_DESERT = song(
    "desert", 6,
    "D6:2 r:1 D6:1 A5:2 F5:2 D6:2 E6:2 F6:4 "
    "D6:2 r:1 D6:1 Bb5:2 F5:2 D6:2 F6:2 Bb6:4 "
    "C6:2 r:1 C6:1 G5:2 E5:2 C6:2 E6:2 G6:4 "
    "C#6:4 E6:4 A6:4 G6:2 E6:2 "
    "F6:2 E6:2 D6:2 A5:2 F6:2 E6:2 D6:4 "
    "D6:2 C6:2 Bb5:2 F5:2 D6:2 C6:2 Bb5:4 "
    "Bb5:2 D6:2 G6:2 Bb6:2 A6:2 G6:2 D6:4 "
    "C#6:2 E6:2 A6:4 G6:2 E6:2 C#6:4 "
    "F6:4 D6:2 F6:2 Bb6:4 A6:4 "
    "G6:4 E6:2 G6:2 C7:4 Bb6:4 "
    "A6:2 F6:2 D6:2 F6:2 A6:2 D7:2 A6:4 "
    "D7:8 r:4 A6:2 F6:2 "
    "G6:2 Bb6:2 D7:4 C7:2 Bb6:2 G6:4 "
    "A6:2 C#7:2 E7:4 D7:2 C#7:2 A6:4 "
    "F6:2 A6:2 D7:4 A6:2 F6:2 D6:4 "
    "E6:4 C#6:4 A5:4 C#6:4",
    ["Dm", "Bb", "C", "A", "Dm", "Bb", "Gm", "A", "Bb", "C", "Dm", "Dm", "Gm", "A", "Dm", "A"],
    "drive", "race", lead_env=0xB2, lead_duty=2)

RACE_SNOW = song(
    "snow", 6,
    "B5:2 D6:2 F#6:2 B6:2 A6:2 F#6:2 D6:2 F#6:2 "
    "G6:4 D6:2 B5:2 G5:4 B5:4 "
    "A5:2 D6:2 F#6:2 A6:2 G6:2 F#6:2 D6:4 "
    "E6:4 C#6:4 A5:4 C#6:4 "
    "D6:2 F#6:2 B6:4 A6:2 F#6:2 D6:4 "
    "B5:2 D6:2 G6:4 F#6:2 D6:2 B5:4 "
    "F#6:3 E6:1 D6:2 A5:2 D6:4 F#6:4 "
    "E6:8 A6:4 C#7:4 "
    "B6:4 G6:2 D6:2 B5:4 D6:4 "
    "C#7:4 A6:2 E6:2 C#6:4 E6:4 "
    "D7:2 C#7:2 B6:2 F#6:2 D6:2 F#6:2 B6:4 "
    "B6:8 r:4 F#6:2 A6:2 "
    "G6:2 B6:2 E7:4 D7:2 B6:2 G6:4 "
    "D7:2 B6:2 G6:2 D6:2 G6:4 B6:4 "
    "C#7:4 E7:4 A6:4 E6:4 "
    "A6:4 E6:4 C#6:4 E6:4",
    ["Bm", "G", "D", "A", "Bm", "G", "D", "A", "G", "A", "Bm", "Bm", "Em", "G", "A", "A"],
    "octave", "four", lead_env=0xA3, lead_duty=2)

WIN = {
    "name": "win", "tempo": 5,
    "ch": [
        encode(parse("E5:2 G5:2 C6:2 E6:6 G5:2 C6:2 E6:2 G6:10 r:4"), 0x72, 2, loop=False),
        encode(parse("C6:2 E6:2 G6:2 C7:6 E6:2 G6:2 C7:2 E7:10 r:4"), 0xB3, 2, loop=False),
        encode(parse("C3:4 G3:4 C4:4 E3:4 G3:4 C4:8 r:4"), loop=False),
        encode([(1, 2), (3, 2), (2, 2), (3, 2), (1, 2), (2, 2), (2, 2), (2, 2), (1, 4), (4, 8), (0, 4)], loop=False),
    ],
}
LOSE = {
    "name": "lose", "tempo": 7,
    "ch": [
        encode(parse("C5:4 B4:4 A4:4 E4:8 r:4"), 0x62, 1, loop=False),
        encode(parse("E5:4 D5:4 C5:4 B4:8 r:4"), 0x93, 2, loop=False),
        encode(parse("A2:8 F2:8 E2:8"), loop=False),
        encode([(1, 4), (3, 4), (2, 4), (0, 12)], loop=False),
    ],
}

SONGS = [TITLE, MENU, RACE_CITY, RACE_COAST, RACE_DESERT, RACE_SNOW, WIN, LOSE]


def square_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    return max(0, min(2047, int(round(2048 - 131072.0 / hz))))


def wave_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    return max(0, min(2047, int(round(2048 - 65536.0 / hz))))
