#!/usr/bin/env python3
"""Small PNG pixel statistics for test screenshots (8-bit RGB, any filter).
usage: pngcheck.py file.png colors        -> number of distinct colours
       pngcheck.py file.png count R G B T -> pixels within distance T of (R,G,B)
       pngcheck.py file.png histogram     -> top colours"""
import struct
import sys
import zlib


def load(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos = 8
    idat = b''
    w = h = 0
    while pos < len(data):
        n, typ = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if typ == b'IHDR':
            w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
            assert depth == 8 and ctype in (2, 6)
            bpp = 3 if ctype == 2 else 4
        elif typ == b'IDAT':
            idat += body
        pos += 12 + n
    raw = zlib.decompress(idat)
    stride = w * bpp
    rows = []
    prev = bytearray(stride)
    i = 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        rows.append(line)
        prev = line
    px = []
    for line in rows:
        for x in range(w):
            px.append(tuple(line[x * bpp:x * bpp + 3]))
    return w, h, px


def main():
    w, h, px = load(sys.argv[1])
    cmd = sys.argv[2]
    if cmd == 'colors':
        print(len(set(px)))
    elif cmd == 'count':
        r, g, b, t = (int(v) for v in sys.argv[3:7])
        print(sum(1 for p in px if abs(p[0] - r) + abs(p[1] - g) + abs(p[2] - b) <= t))
    elif cmd == 'histogram':
        hist = {}
        for p in px:
            hist[p] = hist.get(p, 0) + 1
        for c, n in sorted(hist.items(), key=lambda kv: -kv[1])[:12]:
            print('%02X%02X%02X %d' % (c + (n,)))


main()
