#!/usr/bin/env python3
"""Patches the GBA header complement byte (0xBD) and pads the ROM."""
import sys

path = sys.argv[1]
rom = bytearray(open(path, "rb").read())
chk = 0
for b in rom[0xA0:0xBD]:
    chk = (chk - b) & 0xFF
rom[0xBD] = (chk - 0x19) & 0xFF
# pad to a multiple of 4 KiB with 0xFF (like a real mask ROM / flash cart)
while len(rom) % 4096:
    rom.append(0xFF)
open(path, "wb").write(rom)
print("header checksum 0x%02X, size %d bytes" % (rom[0xBD], len(rom)))
