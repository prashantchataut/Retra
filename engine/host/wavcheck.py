#!/usr/bin/env python3
"""Estimate the dominant frequency and level of each channel of a 16-bit stereo WAV.
usage: wavcheck.py file.wav [skip_seconds]"""
import struct
import sys
import wave

w = wave.open(sys.argv[1], 'rb')
skip = float(sys.argv[2]) if len(sys.argv) > 2 else 0.5
rate = w.getframerate()
data = w.readframes(w.getnframes())
n = len(data) // 4
frames = struct.unpack('<%dh' % (n * 2), data)
start = int(skip * rate)
for ch, name in ((0, 'left'), (1, 'right')):
    s = frames[start * 2 + ch::2]
    if not s:
        print(name, 'no data')
        continue
    mean = sum(s) / len(s)
    crossings = 0
    prev = s[0] - mean
    for v in s[1:]:
        cur = v - mean
        if prev < 0 <= cur:
            crossings += 1
        prev = cur
    secs = len(s) / rate
    peak = max(abs(v - mean) for v in s)
    print('%s: freq=%.1f Hz peak=%d' % (name, crossings / secs, peak))
