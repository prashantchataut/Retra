# Retra engine

`:engine` is Retra's Game Boy Advance emulator: a C++17 core written from scratch for this
project, a JNI bridge, and the Kotlin API the app codes against (`app.retra.engine`).
No code from other emulators is used; hardware behaviour was implemented from public
documentation (GBATEK, Tonc) and verified with freely licensed test ROMs.

```
engine/
  src/main/cpp/core/          pure C++17 core (no JNI / Android headers)
  src/main/cpp/jni/           retra_jni.cpp  (JNI bridge, no C++ exceptions)
  src/main/cpp/CMakeLists.txt -> libretra_engine.so (always -O3, 16 KiB page aligned)
  src/main/kotlin/app/retra/engine/  GbaCore, GbaKey, SaveType, RomInfo, LoadResult
  host/                       retra-headless runner, test scripts, own test ROM sources
```

## Kotlin API

Exactly the contract in `docs/REBUILD_PLAN.md`. Notes on semantics:

* `GbaCore` is not thread-safe; use one emulation thread. `close()` is idempotent.
* `loadRom(rom, bios)`: ROMs up to 32 MiB. `bios == null` uses Retra's own replacement BIOS
  (see below); a user BIOS must be exactly 16 KiB. Boot always goes straight to the cartridge
  (the BIOS intro would refuse ROM hacks with a bad header checksum).
* `runFrame(video, audio)`: runs until the next VBlank; writes 240x160 `0xFFRRGGBB` pixels and
  returns the number of stereo s16 frames written (548-549 at 32768 Hz). The buffers are
  validated in Kotlin (`video.size >= 38400`, `audio.size >= 4096`).
* `RomInfo.saveType` is detected from the library ID strings (`SRAM_V`, `SRAM_F_V`, `FLASH_V`,
  `FLASH512_V`, `FLASH1M_V`, `EEPROM_V`). `AUTO` is reported for EEPROM games: the 512 B /
  8 KiB size is decided from the first DMA transfer to the EEPROM. Games without a tag that
  write to the SRAM area become 32 KiB SRAM carts on the fly (homebrew convention).
* `batterySave()` returns raw `.sav` contents (SRAM 32 KiB, Flash 64/128 KiB, EEPROM 512 B /
  8 KiB, EEPROM bytes in transmission order). `loadBatterySave()` accepts the same and adapts
  Flash/EEPROM size to the file size (common emulator `.sav` files load directly).
* `saveState()` is a versioned binary blob (`RTAS` magic, version 1, game ID check). It
  contains the save memory, so restoring a state also restores the battery contents (and
  sets the dirty flag). A rejected state leaves the running game untouched.
* `setCheats(list)` returns the number of accepted code lines; each list entry may contain
  several lines. Formats: raw `AAAAAAAA:VV|VVVV|VVVVVVVV`, CodeBreaker `XXXXXXXX YYYY`
  (unencrypted), GameShark/Action Replay v1/v2 and Pro Action Replay v3 `XXXXXXXX YYYYYYYY`
  (encrypted, auto-detected; prefix an entry with `GS:` or `AR:` to force). Supported code
  types: writes, fills/slides, add/or/and, conditionals, AR indirect writes and IO writes, ROM
  patches (GS `6`, AR `18..1E`). Not supported: encrypted CodeBreaker (`9...` seed codes) and the
  GS/AR `DEADFACE` seed change (needs device-internal tables); such lines are rejected.
* `setRtcOffsetSeconds()` shifts the cartridge clock relative to the host clock. If the game
  sets the clock itself, that adjustment is kept (and saved in states).

## Architecture

| File | Responsibility |
| --- | --- |
| `gba.h/.cpp` | facade: load/reset, frame loop, event dispatch, IRQ line, save states |
| `scheduler.h` | fixed-slot event scheduler (HBlank, line end, timers, audio sample) |
| `cpu.h/.cpp`, `cpu_arm.cpp`, `cpu_thumb.cpp`, `cpu_shift.h` | ARM7TDMI interpreter |
| `bus.h/.cpp`, `bus_inl.h` | memory map, wait states, GamePak prefetch, open bus, IO registers |
| `ppu.h/.cpp` | scanline renderer |
| `apu.h/.cpp` | PSG 1-4, Direct Sound A/B, mixer |
| `dma.h/.cpp`, `timer.h/.cpp` | DMA 0-3, timers 0-3 |
| `bios/retra_bios.S`, `bios_image.h`, `bios_hle.cpp` | replacement BIOS + HLE SWIs |
| `cart_save.h/.cpp` | SRAM / Flash / EEPROM |
| `rtc.h/.cpp` | GPIO port + Seiko S-3511 RTC |
| `cheats.h/.cpp` | cheat engine |
| `state.h` | serialization helpers |

**CPU.** Full ARMv4T: all data-processing ops with barrel-shifter edge cases (LSL/LSR/ASR #32,
RRX, register shifts >= 32, PC+12 with register shifts), multiplies (incl. long), single /
halfword / signed transfers with ARM7 misaligned-load rotation, SWP, LDM/STM quirks (empty list,
base in list, user-bank transfer, S-bit CPSR restore), MRS/MSR with field masks, BX, SWI,
undefined-instruction trap, all banked modes. Decoding uses 4096-entry (ARM, bits 27-20/7-4)
and 1024-entry (THUMB, bits 15-6) tables of template-specialised handlers generated at compile
time. A real two-stage prefetch pipeline is modelled (self-modifying code, BIOS open bus).
Cycle counting uses per-region N/S wait states from WAITCNT, internal cycles for
multiplies/loads/register shifts, branch refills, the "first fetch after a data access /
internal cycle is non-sequential" rule, and a GamePak prefetch-buffer model (the prefetcher
reads ahead one halfword per S-cycle while the CPU is busy elsewhere).
Short backward branches run an idle-loop detector: if a loop iteration leaves registers,
flags and memory unchanged (and reads no free-running timer), time skips to the next event.

**Memory.** BIOS read protection (returns the last BIOS opcode fetched), EWRAM/IWRAM/palette/
VRAM/OAM mirroring, VRAM 0x18000-0x1FFFF mirror, byte-write rules (palette/VRAM duplicate,
OAM/OBJ-VRAM ignore), ROM up to 32 MiB with out-of-range open-bus pattern (the ROM buffer
is pre-filled so reads need no bounds check), SRAM-bus rules (8-bit bus, unaligned byte
selection), CPU open bus for unmapped areas and write-only IO registers.

**BIOS.** No Nintendo code is shipped. `bios/retra_bios.S` is an original 836-byte BIOS
(assembled with `bios/build_bios.sh`, output committed as `bios_image.h`): exception vectors,
an IRQ dispatcher that saves r0-r3/r12/lr and calls `[0x03FFFFFC]`, the SWI dispatcher,
Halt/Stop/CustomHalt/IntrWait/VBlankIntrWait as real ARM code (using BIOS IF at 0x03FFFFF8),
and placement of the well-known open-bus values (0xE129F000 after boot, 0xE25EF004 during IRQ,
0xE55EC002 after IRQ, 0xE3A02004 after SWI). Other SWIs trap into C++ (`bios_hle.cpp`):
SoftReset, RegisterRamReset, Div, DivArm, Sqrt, ArcTan, ArcTan2 (bit-exact incl. r1/r3),
CpuSet, CpuFastSet, GetBiosChecksum, BgAffineSet, ObjAffineSet, BitUnPack, LZ77UnComp
(WRAM/VRAM), HuffUnComp, RLUnComp (WRAM/VRAM), Diff8bitUnFilter (WRAM/VRAM),
Diff16bitUnFilter, SoundBias, MidiKey2Freq, MultiBoot (fails: no link), HardReset.
A user-supplied BIOS replaces all of this.

**PPU.** Scanline renderer run at the end of each H-draw: modes 0-5; text BGs (all sizes,
4/8 bpp, flips), affine BGs (wraparound, internal reference points latched at VBlank and on
register writes, advanced by PB/PD per line so HBlank-DMA "mode 7" works), bitmap modes 3/4/5
(page flip, affine transformed), sprites (normal/affine/double size, 1D/2D mapping, OBJ window,
semi-transparency, priority vs. BGs, bitmap-mode tile limit), windows 0/1/OBJ/outside, alpha /
brighten / darken blending, BG and OBJ mosaic, forced blank, DISPSTAT/VCOUNT flags and IRQs
(1232 cycles/line, HBlank at cycle 1006, 228 lines). Composition is a branch-free painter's
algorithm on packed (layer, colour) lanes. Output ARGB8888; optional LCD colour correction
(gamma + channel cross-talk approximation).

**DMA / timers.** DMA 0-3: immediate, VBlank, HBlank, sound FIFO (4 words, fixed destination),
video capture (DMA3, lines 2-161); address control incl. increment/reload, repeat, 16/32 bit,
IRQ, forced address alignment, ROM sources always increment, DMA open bus, EEPROM size hint.
Timers: prescalers, cascade, IRQ, 2-cycle start delay, lazily evaluated counters with events
only scheduled when something observes the overflow.

**APU.** PSG square 1 (sweep) and 2, wave (two banks, 32/64 samples, volume codes, force 75%),
noise (7/15-bit LFSR), envelopes, length counters, 512 Hz frame sequencer; PSG output is
integrated over each 512-cycle output period (band-limits high tones). Direct Sound A/B 32-byte
FIFOs driven by timer 0/1 overflow with DMA refill requests, sample-and-hold like hardware.
SOUNDCNT_L/H/X and SOUNDBIAS mixing into 10 bits then s16 stereo at 32768 Hz.

**Saves / RTC.** SRAM 32 KiB; Flash 64 KiB (Panasonic 0x32/0x1B) and 128 KiB (Macronix
0xC2/0x09) with ID mode, chip/sector erase, byte program and bank switching; EEPROM 512 B /
8 KiB serial protocol via DMA with automatic size detection. GPIO RTC (S-3511) at
0x080000C4/C6/C8: status, date/time and time commands, BCD from the host clock + offsets,
writes supported; enabled for game codes AXV*/AXP*/BPE* (and a few other RTC carts) or when the
ROM contains `SIIRTC_V`.

## Testing

`engine/host/run_tests.sh` builds `retra-headless`, downloads freely licensed test ROMs into
`/tmp/retra-test-roms` (never into the repo) and checks them. Last run: **76 passed, 0 failed**.

| Suite | Result |
| --- | --- |
| jsmolka gba-tests: arm, thumb, memory, bios, nes, unsafe, save/none, sram, flash64, flash128 | all pass ("All tests passed" screen) |
| jsmolka ppu: hello, shades, stripes | correct (verified visually, hash-locked) |
| armwrestler-gba-fixed: ARM ALU/LDR/STR/LDM/STM, THUMB ALU/LDR/STR/LDM/STM | all OK |
| mGBA test suite | memory 1552/1552, I/O read 130/130, DMA 1244/1244, BIOS math 615/615, shifter 140/140, carry 93/93, timing 1392/2020, multiply long 52/72, timer count-up 421/936, timer IRQ 44/90, SIO R/W 25/90, SIO timing 0/4, misc edge 3/12 |
| Tonc demos (31 ROMs: bitmap modes, page flip, affine BG/OBJ, mode 7, windows, blending, mosaic, IRQ, DMA, timers, sound, text) | verified visually, hash-locked |
| Own test ROMs (`host/testroms`, built with arm-none-eabi-gcc) | RTC protocol pass, EEPROM 8 KiB + 512 B pass, BIOS LZ77/RL/Huffman(4+8 bit)/Diff8/Diff16/BitUnPack/CpuSet/CpuFastSet match data from independent encoders (`gen_bios_data.py`), Direct Sound 1024 Hz and PSG 440 Hz measured in WAV output |
| Unit tests (`host/unit_tests.cpp`) | raw / CodeBreaker / encrypted GameShark + Action Replay v3 cheats, battery import/export and size adaptation, state rejection (other game, truncated, junk), header parsing |
| Mochi Hop (`app/src/main/assets/games/mochi_hop.gba`) | title, START, map, level gameplay, pause menu render correctly; PSG music audible; save-state round trip identical |
| Save states | `--state-test` (save mid-run, finish, reload, re-run, compare frames) passes on Mochi Hop, Tonc, mGBA suite |
| Sanitizers | ASan + UBSan clean on all test ROMs and on random-data ROMs |
| Kotlin API + JNI | `host/jni_smoke.sh` compiles the Kotlin sources with kotlinc, builds the JNI library for the host JVM and drives `GbaCore` (load, frames, audio, states, saves, cheats, reset, close) |

Useful commands:

```sh
cmake -S engine/host -B /tmp/b -G Ninja && cmake --build /tmp/b
/tmp/b/retra-headless game.gba --frames 600 --press "200:START,300-400:RIGHT" --out shot.png --wav out.wav
/tmp/b/retra-headless game.gba --frames 3000 --bench
/tmp/b/retra-headless game.gba --frames 800 --state-test
/tmp/b/retra-headless test.gba --debug-log     # prints the debug-port log used by test ROMs
```

## Performance

Host: Intel Xeon @ 2.60 GHz (shared VM), g++ 14 `-O2`, single thread, headless:

| ROM | fps | x realtime |
| --- | --- | --- |
| Mochi Hop gameplay | ~1400 | ~23x |
| Tonc `m7_ex` (mode 7, HBlank IRQ, sprites) | ~1050 | ~18x |
| Tonc `obj_aff` / `bld_demo` | ~1700 / ~1500 | ~28x / 25x |
| armwrestler | ~1800 | ~30x |
| Synthetic worst case (`host/testroms/bench_test.c`: CPU never idles, 4 text BGs, 128 sprites, alpha blending on every pixel) | ~560 | ~9x |

The Android build uses `-O3`; it compiles warning-free with NDK r27c clang for arm64-v8a,
armeabi-v7a and x86_64 (only the 16 JNI symbols are exported, 16 KiB segment alignment).
A mid-range arm64 phone is expected to stay several times above full speed.

## Known gaps

* Cycle accuracy is approximate (mGBA timing suite 1392/2020): no exact DMA/IRQ latencies,
  no per-scanline sprite cycle budget, the ARM7 multiply carry flag is left unchanged.
* Rendering is per scanline: register changes in the middle of a line take effect on the
  next line (HBlank/VCount raster effects work).
* Serial port: no link cable; transfers complete immediately with 0xFFFF data.
* BIOS sound-driver SWIs (0x1A-0x1E, 0x28-0x2A) are no-ops in the replacement BIOS (games
  using the BIOS music player need a user BIOS); Stop mode behaves like Halt.
* Colour correction is an approximation, not a measured LCD profile.
