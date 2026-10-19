#!/usr/bin/env python3
"""Generates bios_data.h for bios_test.c: inputs compressed with independent encoders for the
GBA BIOS formats (LZ77, RL, Huffman, Diff8/16, BitUnPack) plus the expected outputs.
usage: gen_bios_data.py OUT.h"""
import random
import sys


def lz77(data):
    out = bytearray([0x10, len(data) & 0xFF, (len(data) >> 8) & 0xFF, (len(data) >> 16) & 0xFF])
    i = 0
    while i < len(data):
        flag_pos = len(out)
        out.append(0)
        flags = 0
        for bit in range(8):
            if i >= len(data):
                break
            best_len, best_disp = 0, 0
            for disp in range(2, min(i, 4096) + 1):  # disp >= 2: safe for the VRAM variant
                n = 0
                while n < 18 and i + n < len(data) and data[i + n] == data[i - disp + n]:
                    n += 1
                if n > best_len:
                    best_len, best_disp = n, disp
            if best_len >= 3:
                flags |= 0x80 >> bit
                d = best_disp - 1
                out.append(((best_len - 3) << 4) | (d >> 8))
                out.append(d & 0xFF)
                i += best_len
            else:
                out.append(data[i])
                i += 1
        out[flag_pos] = flags
    while len(out) % 4:
        out.append(0)
    return bytes(out)


def rle(data):
    out = bytearray([0x30, len(data) & 0xFF, (len(data) >> 8) & 0xFF, (len(data) >> 16) & 0xFF])
    i = 0
    while i < len(data):
        run = 1
        while i + run < len(data) and run < 130 and data[i + run] == data[i]:
            run += 1
        if run >= 3:
            out += bytes([0x80 | (run - 3), data[i]])
            i += run
        else:
            start = i
            while i < len(data) and i - start < 128:
                if i + 2 < len(data) and data[i] == data[i + 1] == data[i + 2]:
                    break
                i += 1
            out.append(i - start - 1)
            out += data[start:i]
    while len(out) % 4:
        out.append(0)
    return bytes(out)


def huffman(data, bits):
    symbols = []
    for b in data:
        if bits == 8:
            symbols.append(b)
        else:
            symbols += [b & 15, b >> 4]
    freq = {}
    for s in symbols:
        freq[s] = freq.get(s, 0) + 1
    if len(freq) == 1:
        freq[(symbols[0] + 1) % (1 << bits)] = 0
    # build tree: nodes are ('leaf', sym) or ('node', left, right)
    nodes = [(f, i, ('leaf', s)) for i, (s, f) in enumerate(sorted(freq.items()))]
    counter = len(nodes)
    while len(nodes) > 1:
        nodes.sort(key=lambda t: (t[0], t[1]))
        a, b = nodes[0], nodes[1]
        nodes = nodes[2:] + [(a[0] + b[0], counter, ('node', a[2], b[2]))]
        counter += 1
    root = nodes[0][2]
    codes = {}

    def walk(n, prefix):
        if n[0] == 'leaf':
            codes[n[1]] = prefix
        else:
            walk(n[1], prefix + '0')
            walk(n[2], prefix + '1')
    walk(root, '')
    # lay out the table breadth-first; table[k] is the byte at offset 5 + k (root at 5)
    table = [None]
    queue = [(root, 0)]  # (internal node, its table index)
    while queue:
        node, idx = queue.pop(0)
        pair = len(table)
        table += [None, None]
        addr = 5 + idx
        child_addr = 5 + pair
        off = (child_addr - (addr & ~1) - 2) // 2
        assert 0 <= off < 64, 'tree too wide for the 6-bit offsets'
        val = off
        for j, child in enumerate((node[1], node[2])):
            if child[0] == 'leaf':
                val |= 0x80 >> j
                table[pair + j] = child[1]
            else:
                queue.append((child, pair + j))
        table[idx] = val
    table = [t if t is not None else 0 for t in table]
    while (len(table) + 1) % 4:
        table.append(0)
    out = bytearray([0x20 | bits, len(data) & 0xFF, (len(data) >> 8) & 0xFF, (len(data) >> 16) & 0xFF])
    out.append((len(table) + 1) // 2 - 1)
    out += bytes(table)
    assert len(out) % 4 == 0
    bitstr = ''.join(codes[s] for s in symbols)
    while len(bitstr) % 32:
        bitstr += '0'
    for k in range(0, len(bitstr), 32):
        w = int(bitstr[k:k + 32], 2)
        out += w.to_bytes(4, 'little')
    return bytes(out)


def diff8(data):
    out = bytearray([0x81, len(data) & 0xFF, (len(data) >> 8) & 0xFF, (len(data) >> 16) & 0xFF])
    prev = 0
    for b in data:
        out.append((b - prev) & 0xFF)
        prev = b
    while len(out) % 4:
        out.append(0)
    return bytes(out)


def diff16(data):
    out = bytearray([0x82, len(data) & 0xFF, (len(data) >> 8) & 0xFF, (len(data) >> 16) & 0xFF])
    prev = 0
    for k in range(0, len(data), 2):
        v = data[k] | (data[k + 1] << 8)
        out += ((v - prev) & 0xFFFF).to_bytes(2, 'little')
        prev = v
    while len(out) % 4:
        out.append(0)
    return bytes(out)


def c_array(name, data):
    lines = ['static const u8 %s[%d] __attribute__((aligned(4))) = {' % (name, len(data))]
    for k in range(0, len(data), 16):
        lines.append('    ' + ', '.join('0x%02X' % b for b in data[k:k + 16]) + ',')
    lines.append('};')
    return '\n'.join(lines)


def main():
    rnd = random.Random(1234)
    # Tile-like data: repetitive with some noise, even length, multiple of 4
    plain = bytearray()
    for k in range(1024):
        plain.append((k // 7) % 5 + (rnd.randrange(3) if k % 13 == 0 else 0))
    words = bytes(plain)
    text = (b'Retra BIOS test - the quick brown fox jumps over the lazy dog. ' * 6)[:256]
    small = bytes((k * 3) & 15 | (((k * 5) & 15) << 4) for k in range(96))
    unpack_src = bytes(rnd.randrange(256) for _ in range(16))
    out = ['// Generated by gen_bios_data.py - do not edit.', '#pragma once']
    out.append(c_array('lz_src', lz77(words)))
    out.append(c_array('lz_text_src', lz77(text)))
    out.append(c_array('rl_src', rle(words)))
    out.append(c_array('huff8_src', huffman(text, 8)))
    out.append(c_array('huff4_src', huffman(small, 4)))
    out.append(c_array('diff8_src', diff8(words[:256])))
    out.append(c_array('diff16_src', diff16(words[:256])))
    out.append(c_array('plain_words', words))
    out.append(c_array('plain_text', text))
    out.append(c_array('plain_small', small))
    out.append(c_array('unpack_src', unpack_src))
    # BitUnPack 2bpp -> 8bpp with offset 1, zero flag clear
    unpacked = bytearray()
    for b in unpack_src:
        for s in range(0, 8, 2):
            v = (b >> s) & 3
            unpacked.append(v + 1 if v else 0)
    out.append(c_array('unpack_expected', bytes(unpacked)))
    open(sys.argv[1], 'w').write('\n'.join(out) + '\n')


main()
