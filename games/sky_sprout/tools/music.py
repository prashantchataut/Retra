"""Original music for Sky Sprout, written as melody + chord progressions.

Adapted from the Mochi Hop generator: harmony arpeggios, bass and drums are
generated from the chord symbols so every channel stays in sync. Output is a
per-channel byte stream for the PSG sequencer in src/audio.c.

Stream format (pairs):  note(1..127 MIDI) dur | 0 dur (rest) | 0xF0 env | 0xF1 duty
                        0xFE end | 0xFF loop
Drums channel: 1 kick, 2 snare, 3 hat, 4 crash, 0 rest.
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


def chord_notes(sym, octv):
    minor = sym.endswith("m")
    root = sym[:-1] if minor else sym
    r = 12 * (octv + 1) + NOTE[root]
    return [r, r + (3 if minor else 4), r + 7]


def bars_of(prog, bar):
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
        ns = ns + [ns[0] + 12]
        t = i = 0
        while t < ln:
            ev.append((ns[pattern[i % len(pattern)]], min(step, ln - t)))
            t += step
            i += 1
    return ev


def gen_bass(prog, bar, style="drive"):
    ev = []
    for sym, ln in bars_of(prog, bar):
        r = chord_notes(sym, 2)[0]
        if r < 40:
            r += 12
        f = r + 7
        if style == "drive":
            unit = [(r, 2), (r + 12, 2)]
        elif style == "gallop":
            unit = [(r, 2), (r, 1), (r, 1), (r + 12, 2), (r, 1), (r, 1)]
        elif style == "bounce":
            unit = [(r, 3), (0, 1), (r + 12, 2), (f, 2)]
        elif style == "walk":
            unit = [(r, 4), (f, 4)]
        elif style == "pulse":
            unit = [(r, 1), (0, 1), (r, 1), (r + 12, 1)]
        else:
            unit = [(r, ln)]
        seq = []
        while sum(d for _, d in seq) < ln:
            seq += unit
        tot = 0
        out = []
        for n, d in seq:
            if tot + d > ln:
                d = ln - tot
            if d <= 0:
                break
            out.append((n, d))
            tot += d
        ev += out
    return ev


DRUMS = {
    "drive": ([(1, 2), (3, 2), (2, 2), (3, 2), (1, 2), (1, 2), (2, 2), (3, 2)],
              [(1, 2), (3, 2), (2, 2), (3, 2), (2, 1), (2, 1), (2, 1), (2, 1), (2, 2), (4, 2)]),
    "pop": ([(1, 2), (3, 2), (3, 2), (3, 2), (2, 2), (3, 2), (3, 1), (3, 1), (3, 2)],
            [(1, 2), (3, 2), (3, 2), (1, 2), (2, 2), (3, 2), (2, 2), (2, 2)]),
    "tick": ([(1, 2), (3, 1), (3, 1), (2, 2), (3, 2), (1, 2), (3, 1), (3, 1), (2, 2), (3, 2)],
             [(1, 2), (3, 1), (3, 1), (2, 2), (3, 2), (1, 2), (2, 2), (2, 1), (2, 1), (4, 2)]),
    "march": ([(1, 1), (1, 1), (3, 2), (2, 2), (3, 2), (1, 1), (1, 1), (3, 2), (2, 2), (2, 1), (2, 1)],
              [(1, 1), (1, 1), (3, 2), (2, 2), (3, 2), (2, 1), (2, 1), (2, 1), (2, 1), (2, 2), (4, 2)]),
    "light": ([(1, 4), (3, 4), (2, 4), (3, 4)], [(1, 4), (3, 4), (2, 4), (2, 2), (4, 2)]),
    "breeze": ([(1, 3), (3, 3), (2, 2), (3, 2), (1, 2), (2, 2), (3, 2)],
               [(1, 3), (3, 3), (2, 2), (3, 2), (2, 2), (2, 2), (4, 2)]),
}


def gen_drums(nbars, bar, style):
    ev = []
    main, fill = DRUMS[style]
    for b in range(nbars):
        pat = fill if b % 4 == 3 else main
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


def song(name, tempo, lead, prog, bass_style, drum_style, lead_env=0xA3, harm_env=0x52,
         lead_duty=2, harm_duty=1, arp_pattern=(0, 1, 2, 1), arp_oct=4, bar=16, loop=True):
    lead_ev = parse(lead)
    nb = len(prog)
    # check every bar individually for nicer error messages
    t = 0
    for i, (n, d) in enumerate(lead_ev):
        t += d
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
    "title", 7,
    "A5:4 C6:4 F6:6 E6:2 G6:4 E6:4 C6:8 D6:2 E6:2 F6:4 A6:4 F6:4 D6:4 F6:4 Bb6:8 "
    "A6:4 G6:2 F6:2 C6:4 A5:4 G5:2 A5:2 C6:4 E6:4 G6:4 F6:4 D6:4 Bb5:4 D6:4 E6:4 G6:4 C7:8 "
    "F6:3 E6:1 D6:4 A5:4 D6:4 E6:3 D6:1 C6:4 A5:4 E5:4 F5:2 Bb5:2 D6:4 F6:4 D6:4 C6:4 A5:4 F5:8 "
    "G5:2 Bb5:2 D6:4 G6:4 F6:4 E6:2 F6:2 G6:4 C7:4 Bb6:4 A6:6 G6:2 F6:4 C6:4 F6:8 r:8",
    ["F", "C", "Dm", "Bb", "F", "C", "Bb", "C", "Dm", "Am", "Bb", "F", "Gm", "C", "F", "F"],
    "bounce", "pop", lead_env=0xA4, harm_env=0x62, arp_pattern=(0, 1, 2, 3, 2, 1, 0, 2))

STAGE1 = song(
    "stage1", 6,
    "E5:2 G5:2 C6:4 B5:2 G5:2 E5:4 D5:2 G5:2 B5:4 A5:2 G5:2 D5:4 C5:2 E5:2 A5:4 G5:2 E5:2 C6:4 "
    "A5:4 G5:2 F5:2 E5:4 D5:4 E5:2 G5:2 C6:4 D6:2 E6:2 C6:4 B5:2 D6:2 G6:4 F6:2 D6:2 B5:4 "
    "A5:2 C6:2 F6:4 E6:2 C6:2 A5:4 G5:4 A5:2 B5:2 D6:8 E6:3 D6:1 C6:4 A5:4 E5:4 G5:3 F5:1 E5:4 B5:4 G5:4 "
    "A5:3 G5:1 F5:4 C6:4 A5:4 G5:2 E5:2 C5:4 E5:2 G5:2 C6:4 D6:2 C6:2 A5:4 F5:2 A5:2 D6:4 "
    "B5:2 A5:2 G5:4 D5:2 G5:2 B5:4 C6:4 E6:4 G6:4 E6:4 D6:2 E6:2 C6:12",
    ["C", "G", "Am", "F", "C", "G", "F", "G", "Am", "Em", "F", "C", "Dm", "G", "C", "C"],
    "drive", "drive", lead_env=0xA3, harm_env=0x52, arp_pattern=(0, 1, 2, 3))

STAGE2 = song(
    "stage2", 5,
    "D5:2 D5:2 F5:2 A5:2 D6:4 C6:2 A5:2 Bb5:2 A5:2 G5:2 F5:2 E5:4 F5:2 G5:2 "
    "F5:2 F5:2 Bb5:2 D6:2 F6:4 D6:2 Bb5:2 C6:2 Bb5:2 A5:2 G5:2 E5:4 G5:4 "
    "D5:2 D5:2 F5:2 A5:2 D6:4 E6:2 F6:2 E6:2 D6:2 C6:2 A5:2 F5:4 A5:4 "
    "Bb5:2 C6:2 D6:4 C6:2 Bb5:2 A5:4 A5:4 C#6:4 E6:4 A6:4 "
    "G5:3 A5:1 Bb5:4 D6:4 Bb5:4 A5:3 G5:1 F5:4 G5:8 F5:3 G5:1 A5:4 D6:4 A5:4 C6:3 Bb5:1 A5:4 F5:8 "
    "D6:2 D6:2 D6:2 C6:2 Bb5:4 D6:4 E6:2 E6:2 E6:2 D6:2 C6:4 E6:4 F6:4 E6:4 C#6:4 A5:4 E6:4 C#6:2 A5:2 A5:8",
    ["Dm", "Dm", "Bb", "C", "Dm", "Dm", "Bb", "A", "Gm", "Gm", "Dm", "Dm", "Bb", "C", "A", "A"],
    "drive", "drive", lead_env=0xB2, harm_env=0x42, lead_duty=1, arp_pattern=(0, 2, 1, 2))

STAGE3 = song(
    "stage3", 6,
    "G5:3 Bb5:3 Eb6:2 D6:2 Bb5:2 G5:4 G5:3 C6:3 Eb6:2 D6:2 C6:2 G5:4 Ab5:3 C6:3 Eb6:2 F6:2 Eb6:2 C6:4 "
    "D6:3 Bb5:3 F5:2 Bb5:8 Eb6:3 D6:3 Bb5:2 G5:2 Bb5:2 Eb6:4 G6:3 F6:3 Eb6:2 D6:2 C6:2 G5:4 "
    "Ab5:2 Bb5:2 C6:2 Eb6:2 F6:4 Eb6:2 C6:2 D6:4 F6:4 Bb6:8 Bb5:4 D6:2 G6:2 F6:4 D6:4 "
    "Eb6:4 G5:2 C6:2 Eb6:4 D6:4 C6:4 Ab5:2 F5:2 Ab5:4 C6:4 D6:2 C6:2 Bb5:2 F5:2 D5:4 F5:4 "
    "Eb6:3 C6:3 Ab5:2 C6:8 F6:3 D6:3 Bb5:2 D6:8 G6:4 F6:2 Eb6:2 Bb5:4 G5:4 Eb6:8 r:8",
    ["Eb", "Cm", "Ab", "Bb", "Eb", "Cm", "Ab", "Bb", "Gm", "Cm", "Fm", "Bb", "Ab", "Bb", "Eb", "Eb"],
    "bounce", "breeze", lead_env=0x95, harm_env=0x43, lead_duty=2, harm_duty=2, arp_pattern=(0, 1, 2, 3, 2, 1))

STAGE4 = song(
    "stage4", 6,
    "A5:2 r:2 E5:2 r:2 A5:2 B5:2 C6:4 B5:2 r:2 A5:2 r:2 E5:4 r:4 F5:2 r:2 A5:2 r:2 C6:2 D6:2 E6:4 "
    "D6:2 r:2 B5:2 r:2 G5:4 r:4 A5:2 C6:2 E6:2 A6:2 G6:2 E6:2 C6:4 D6:2 C6:2 B5:2 A5:2 G#5:4 E5:4 "
    "F5:2 A5:2 D6:2 F6:2 E6:2 D6:2 A5:4 G#5:4 B5:4 E6:4 r:4 C6:2 C6:2 A5:2 C6:2 F6:4 E6:4 "
    "D6:2 D6:2 B5:2 D6:2 G6:4 F6:4 E6:2 D6:2 B5:2 G5:2 E5:4 G5:4 A5:4 C6:4 E6:4 A5:4 "
    "F6:3 E6:1 D6:4 A5:4 F5:4 E6:3 D6:1 B5:4 G#5:4 E5:4 A5:2 B5:2 C6:2 E6:2 A6:8 A6:4 E6:4 C6:4 A5:4",
    ["Am", "Am", "F", "G", "Am", "Am", "Dm", "E", "F", "G", "Em", "Am", "Dm", "E", "Am", "Am"],
    "pulse", "tick", lead_env=0x92, harm_env=0x41, lead_duty=0, arp_pattern=(0, 2, 1, 2, 3, 2))

STAGE5 = song(
    "stage5", 5,
    "E5:2 E5:2 G5:2 E5:2 B5:4 A5:2 G5:2 F#5:2 G5:2 A5:2 B5:2 E6:8 E6:2 D6:2 C6:2 B5:2 C6:4 G5:4 "
    "F#5:2 A5:2 D6:4 C6:2 A5:2 F#5:4 E5:2 G5:2 B5:2 E6:2 G6:4 F#6:2 E6:2 D6:2 E6:2 F#6:2 G6:2 B6:8 "
    "A6:2 G6:2 E6:2 C6:2 G6:4 E6:4 D#6:4 F#6:4 B6:4 B5:4 C6:3 B5:1 A5:4 E6:4 C6:4 B5:3 A5:1 G5:4 A5:8 "
    "G5:3 F#5:1 E5:4 B5:4 G5:4 B5:3 C6:1 B5:4 E5:8 E6:2 E6:2 G6:2 E6:2 C6:4 E6:4 "
    "F#6:2 F#6:2 A6:2 F#6:2 D6:4 F#6:4 B6:4 A6:4 F#6:4 D#6:4 B5:4 D#6:2 F#6:2 B6:8",
    ["Em", "Em", "C", "D", "Em", "Em", "C", "B", "Am", "Am", "Em", "Em", "C", "D", "B", "B"],
    "drive", "march", lead_env=0xB2, harm_env=0x42, lead_duty=1, arp_pattern=(0, 1, 2, 1))

BOSS = song(
    "boss", 5,
    "G5:1 G5:1 r:2 G5:1 G5:1 r:2 Bb5:2 A5:2 G5:2 D5:2 G5:1 G5:1 r:2 G5:1 G5:1 r:2 D6:4 C6:4 "
    "Bb5:2 G5:2 Eb5:2 G5:2 Bb5:4 Eb6:4 D6:2 C6:2 A5:2 F5:2 C6:4 A5:4 G6:2 F6:2 D6:2 Bb5:2 G5:4 Bb5:4 "
    "A5:2 Bb5:2 C6:2 D6:2 G6:8 G6:2 F6:2 Eb6:2 D6:2 Eb6:4 Bb5:4 A5:4 D6:4 F#6:4 A6:4 "
    "Eb6:3 D6:1 C6:4 G5:4 Eb5:4 G5:3 Ab5:1 G5:4 C6:8 D6:3 C6:1 Bb5:4 G5:4 D5:4 G5:2 A5:2 Bb5:2 C6:2 D6:8 "
    "Eb6:2 Eb6:2 D6:2 Eb6:2 G6:4 Eb6:4 F6:2 F6:2 Eb6:2 F6:2 A6:4 F6:4 F#6:4 A6:4 D6:4 F#6:4 A6:2 G6:2 F#6:2 E6:2 D6:8",
    ["Gm", "Gm", "Eb", "F", "Gm", "Gm", "Eb", "D", "Cm", "Cm", "Gm", "Gm", "Eb", "F", "D", "D"],
    "gallop", "march", lead_env=0xC2, harm_env=0x52, lead_duty=1, arp_pattern=(0, 1, 2, 3, 2, 1, 0, 1))

FINAL = song(
    "final", 5,
    "C5:2 Eb5:2 G5:2 C6:2 Eb6:4 D6:2 C6:2 Ab5:4 C6:2 Eb6:2 Ab6:4 G6:4 F6:2 Eb6:2 D6:2 Bb5:2 F6:4 D6:4 "
    "B5:2 D6:2 G6:4 F6:2 D6:2 B5:4 C6:4 G6:4 Eb6:2 D6:2 C6:4 Eb6:4 Ab6:4 G6:2 F6:2 Eb6:4 "
    "D6:2 Eb6:2 F6:2 G6:2 Ab6:4 F6:4 G6:8 B6:4 D7:4 Ab6:3 G6:1 F6:4 C6:4 Ab5:4 G6:3 F6:1 Eb6:4 C6:8 "
    "Eb6:3 F6:1 G6:4 Ab6:4 C7:4 Bb6:3 Ab6:1 G6:4 F6:8 F6:2 G6:2 Ab6:2 G6:2 F6:4 C6:4 "
    "D6:2 Eb6:2 F6:2 Eb6:2 D6:4 B5:4 C6:4 Eb6:4 G6:4 C7:4 B6:4 G6:4 D6:4 B5:4",
    ["Cm", "Ab", "Bb", "G", "Cm", "Ab", "Bb", "G", "Fm", "Cm", "Ab", "Bb", "Fm", "G", "Cm", "G"],
    "gallop", "drive", lead_env=0xC3, harm_env=0x52, lead_duty=2, arp_pattern=(0, 1, 2, 3))

ENDING = song(
    "ending", 8,
    "D6:6 C6:2 Bb5:4 F5:4 A5:6 Bb5:2 C6:8 D6:4 Bb5:4 G5:4 Bb5:4 G5:6 F5:2 Eb5:8 "
    "F5:4 Bb5:4 D6:4 F6:4 Eb6:4 D6:4 C6:8 Bb5:4 C6:4 Eb6:4 G6:4 F6:8 A5:8 "
    "G6:6 F6:2 D6:4 Bb5:4 A5:6 Bb5:2 F5:8 G5:4 Bb5:4 Eb6:4 G6:4 F6:8 D6:8 "
    "Eb6:4 D6:4 C6:4 G5:4 A5:4 C6:4 F6:4 Eb6:4 D6:6 C6:2 Bb5:8 r:16",
    ["Bb", "F", "Gm", "Eb", "Bb", "F", "Eb", "F", "Gm", "Dm", "Eb", "Bb", "Cm", "F", "Bb", "Bb"],
    "walk", "light", lead_env=0x95, harm_env=0x43, lead_duty=2, harm_duty=2, arp_pattern=(0, 1, 2, 3, 2, 1, 2, 1))

CLEAR = {
    "name": "clear", "tempo": 5,
    "ch": [
        encode(parse("E5:2 G5:2 C6:2 E5:2 G5:2 C6:2 F5:2 A5:2 D6:2 G5:2 B5:2 E6:2 C6:16 r:4"), 0x72, 2, loop=False),
        encode(parse("C5:2 E5:2 G5:2 C6:2 E6:2 G6:2 A5:2 C6:2 F6:2 B5:2 D6:2 G6:2 C7:16 r:4"), 0xB3, 1, loop=False),
        encode(parse("C3:6 C4:6 F3:6 G3:6 C3:16 r:4"), loop=False),
        encode([(1, 2), (3, 2), (2, 2), (1, 2), (3, 2), (2, 2), (1, 2), (2, 2), (2, 2), (2, 2), (2, 1), (2, 1),
                (4, 4), (0, 16)], loop=False),
    ],
}

GAMEOVER = {
    "name": "gameover", "tempo": 8,
    "ch": [
        encode(parse("C5:4 Eb5:4 G4:4 B4:4 C5:12 r:4"), 0x63, 2, loop=False),
        encode(parse("G5:4 F5:4 Eb5:4 D5:4 C5:12 r:4"), 0xA4, 2, loop=False),
        encode(parse("C3:4 Ab2:4 F2:4 G2:4 C3:12 r:4"), loop=False),
        encode([(1, 4), (0, 4), (1, 4), (2, 4), (4, 12), (0, 4)], loop=False),
    ],
}

SONGS = [TITLE, STAGE1, STAGE2, STAGE3, STAGE4, STAGE5, BOSS, FINAL, CLEAR, GAMEOVER, ENDING]


def square_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    return max(0, min(2047, int(round(2048 - 131072.0 / hz))))


def wave_rate(m):
    hz = 440.0 * 2 ** ((m - 69) / 12.0)
    return max(0, min(2047, int(round(2048 - 65536.0 / hz))))
