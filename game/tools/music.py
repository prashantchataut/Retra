"""Original music for Mochi Hop, written as melody + chord progressions.

The harmony (arpeggios), bass line and drums are generated from the chord
symbols so that every channel stays in sync. Output: per-channel byte streams
for the tiny PSG sequencer in src/audio.c.

Stream format (pairs):  note(1..127 MIDI) dur | 0 dur (rest) | 0xF0 env | 0xF1 duty
                        0xFE end | 0xFF loop
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
        "name": name, "tempo": tempo,
        "ch": [encode(harm, harm_env, harm_duty, loop), encode(lead_ev, lead_env, lead_duty, loop),
               encode(bass, loop=loop), encode(drums, loop=loop)],
    }


TITLE = song(
    "title", 6, 16,
    # A
    "C5:2 F5:2 A5:2 G5:2 F5:4 C5:4 "
    "D5:2 F5:2 A5:2 C6:2 A5:4 F5:4 "
    "D5:2 F5:2 Bb5:2 A5:2 G5:4 F5:2 G5:2 "
    "E5:4 G5:4 C5:4 r:4 "
    "C5:2 F5:2 A5:2 G5:2 F5:4 A5:4 "
    "D6:3 C6:1 A5:2 F5:2 A5:4 D5:4 "
    "Bb5:2 A5:2 G5:2 F5:2 E5:2 G5:2 C6:4 "
    "F5:6 A5:2 F5:8 "
    # B
    "F5:4 D5:2 F5:2 Bb5:4 A5:4 "
    "G5:4 E5:2 G5:2 C6:4 Bb5:4 "
    "A5:2 G5:2 E5:2 C5:2 E5:4 A5:4 "
    "F5:2 E5:2 D5:4 A4:4 D5:4 "
    "D5:2 F5:2 Bb5:2 D6:2 C6:4 Bb5:4 "
    "A5:2 G5:2 E5:2 G5:2 C6:8 "
    "A5:4 F5:2 C6:2 A5:4 F5:4 "
    "G5:2 A5:2 G5:2 E5:2 C5:8",
    ["F", "Dm", "Bb", "C", "F", "Dm", "Bb C", "F",
     "Bb", "C", "Am", "Dm", "Bb", "C", "F", "C"],
    "bounce", "pop")

MEADOW = song(
    "meadow", 6, 16,
    "E5:2 G5:2 C6:2 G5:2 E5:2 G5:2 A5:4 "
    "A5:2 C6:2 E6:4 D6:2 C6:2 A5:4 "
    "F5:2 A5:2 C6:2 A5:2 G5:2 F5:2 E5:4 "
    "D5:2 E5:2 G5:4 B5:4 G5:4 "
    "E5:2 G5:2 C6:2 G5:2 E6:4 D6:4 "
    "C6:2 B5:2 A5:2 G5:2 E5:4 A5:4 "
    "F5:2 G5:2 A5:2 C6:2 B5:2 G5:2 D6:4 "
    "C6:6 G5:2 C6:8 "
    "A5:4 G5:2 F5:2 A5:4 C6:4 "
    "B5:4 A5:2 G5:2 D6:8 "
    "E5:2 G5:2 B5:2 G5:2 E6:4 B5:4 "
    "C6:2 B5:2 A5:2 E5:2 A5:8 "
    "F5:2 A5:2 C6:2 F6:2 E6:4 C6:4 "
    "D6:2 C6:2 B5:2 A5:2 G5:4 B5:4 "
    "C6:4 E6:4 D6:4 B5:4 "
    "C6:4 G5:2 E5:2 C5:8",
    ["C", "Am", "F", "G", "C", "Am", "F G", "C",
     "F", "G", "Em", "Am", "F", "G", "Am G", "C"],
    "bounce", "pop", lead_env=0xA4, arp_pattern=(0, 2, 1, 2))

CAVES = song(
    "caves", 7, 16,
    "D5:2 r:1 D5:1 F5:2 A5:2 G5:2 F5:2 E5:2 F5:2 "
    "E5:2 r:1 E5:1 G5:2 C6:2 Bb5:2 A5:2 G5:4 "
    "F5:2 r:1 F5:1 Bb5:2 D6:2 C6:2 Bb5:2 A5:2 Bb5:2 "
    "A5:4 C#6:4 E6:4 r:4 "
    "D6:2 C6:2 A5:2 F5:2 D5:2 F5:2 A5:4 "
    "G5:2 A5:2 G5:2 E5:2 C5:4 E5:4 "
    "F5:2 G5:2 Bb5:2 A5:2 G5:2 E5:2 C#5:4 "
    "D5:8 r:8 "
    "A5:2 C6:2 F6:4 E6:2 C6:2 A5:4 "
    "G5:2 C6:2 E6:4 D6:2 C6:2 G5:4 "
    "Bb5:2 D6:2 G6:4 F6:2 D6:2 Bb5:4 "
    "A5:2 C#6:2 E6:2 A6:2 G6:4 E6:4 "
    "F6:4 E6:2 D6:2 C6:4 A5:4 "
    "E6:4 D6:2 C6:2 G5:8 "
    "Bb5:4 A5:4 G5:4 C#6:4 "
    "D6:4 A5:4 D5:8",
    ["Dm", "C", "Bb", "A", "Dm", "C", "Bb A", "Dm",
     "F", "C", "Gm", "A", "F", "C", "Gm A", "Dm"],
    "walk", "bouncy", lead_env=0x92, lead_duty=2, harm_env=0x41, harm_duty=1, arp_pattern=(0, 1, 2, 0))

NIGHT = song(
    "night", 9, 12,
    "C5:4 F5:4 A5:4 "
    "C6:6 A5:2 G5:4 "
    "F5:4 D5:4 F5:4 "
    "C5:12 "
    "D5:4 F5:4 A5:4 "
    "Bb5:6 A5:2 G5:4 "
    "E5:4 G5:4 Bb5:4 "
    "A5:12 "
    "D6:4 C6:4 Bb5:4 "
    "C6:6 Bb5:2 A5:4 "
    "A5:4 E5:4 A5:4 "
    "F5:8 E5:2 D5:2 "
    "G5:4 Bb5:4 D6:4 "
    "C6:4 E5:4 G5:4 "
    "F5:4 A5:4 C6:4 "
    "F5:12",
    ["F", "Am", "Bb", "F", "Dm", "Gm", "C", "F",
     "Bb", "C", "Am", "Dm", "Gm", "C", "F", "F"],
    "waltz", "light", lead_env=0x95, lead_duty=2, harm_env=0x43, harm_duty=0,
    arp_pattern=(0, 1, 2, 1, 2, 1), arp_oct=4)

CLEAR = {
    "name": "clear", "tempo": 5,
    "ch": [
        encode(parse("E5:2 G5:2 C6:2 E5:2 G5:4 C6:12 r:4"), 0x72, 2, loop=False),
        encode(parse("C5:2 E5:2 G5:2 C6:2 E6:4 G6:12 r:4"), 0xB2, 1, loop=False),
        encode(parse("C3:4 G3:4 C4:4 C3:12 r:4"), loop=False),
        encode([(1, 2), (3, 2), (2, 2), (3, 2), (1, 4), (2, 4), (0, 12)], loop=False),
    ],
}

SONGS = [TITLE, MEADOW, CAVES, NIGHT, CLEAR]

# Square-wave rate register values for MIDI notes 0..127 (only 36..108 used).


def square_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    v = int(round(2048 - 131072.0 / hz))
    return max(0, min(2047, v))


def wave_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    v = int(round(2048 - 65536.0 / hz))
    return max(0, min(2047, v))
