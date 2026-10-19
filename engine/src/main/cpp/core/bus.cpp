// Retra GBA core - system bus: slow paths, IO registers, wait states.
#include "bus.h"

#include "bios_image.h"
#include "bus_inl.h"
#include "gba.h"
#include "state.h"

namespace retra {

void Bus::reset() {
    std::memset(ewram, 0, sizeof(ewram));
    std::memset(iwram, 0, sizeof(iwram));
    std::memset(io, 0, sizeof(io));
    std::memset(pram, 0, sizeof(pram));
    std::memset(vram, 0, sizeof(vram));
    std::memset(oam, 0, sizeof(oam));
    biosLatch = 0;
    dmaLatch = 0;
    haltcnt = 0;
    setIo16(0x134, 0x8000);  // RCNT
    setIo16(0x088, 0x0200);  // SOUNDBIAS
    updateWaitstates();
    if (romSize) {
        // Restore original bytes possibly patched by the GPIO view.
        std::memcpy(&rom[0xC4], gpioRomBackup, 6);
    }
}

void Bus::loadBios(const u8* data, size_t size) {
    if (data && size == sizeof(bios)) {
        std::memcpy(bios, data, sizeof(bios));
        realBios = true;
    } else {
        std::memset(bios, 0, sizeof(bios));
        std::memcpy(bios, kRetraBios, sizeof(kRetraBios) < sizeof(bios) ? sizeof(kRetraBios) : sizeof(bios));
        realBios = false;
    }
}

void Bus::setRom(const u8* data, size_t size) {
    if (size > kRomMax) size = kRomMax;
    rom.assign(kRomMax + 4, 0);
    std::memcpy(rom.data(), data, size);
    romSize = (u32)size;
    // Out-of-range cartridge reads return the low address bits (open bus of the ROM bus).
    u32 start = (romSize + 1) & ~1u;
    for (u32 o = start; o < kRomMax; o += 2) storeLE<u16>(&rom[o], (u16)(o >> 1));
    eepromLarge = romSize > 16 * 1024 * 1024;
    std::memcpy(gpioRomBackup, &rom[0xC4], 6);
}

void Bus::updateGpioView() {
    if (!romSize) return;
    Rtc& rtc = g->rtc;
    if (rtc.enabled && rtc.readable()) {
        storeLE<u16>(&rom[0xC4], rtc.read(0xC4));
        storeLE<u16>(&rom[0xC6], rtc.read(0xC6));
        storeLE<u16>(&rom[0xC8], rtc.read(0xC8));
    } else {
        std::memcpy(&rom[0xC4], gpioRomBackup, 6);
    }
}

void Bus::updateWaitstates() {
    static const u8 nTab[4] = {4, 3, 2, 8};
    const u16 w = io16(0x204);
    const u32 sram = 1 + nTab[w & 3];
    const u32 n[3] = {1u + nTab[(w >> 2) & 3], 1u + nTab[(w >> 5) & 3], 1u + nTab[(w >> 8) & 3]};
    const u32 s[3] = {1u + (((w >> 4) & 1) ? 1u : 2u), 1u + (((w >> 7) & 1) ? 1u : 4u),
                      1u + (((w >> 10) & 1) ? 1u : 8u)};
    prefetchEnabled = (w >> 14) & 1;

    for (int r = 0; r < 16; r++) {
        for (int sq = 0; sq < 2; sq++) {
            timing[0][sq][r] = timing[1][sq][r] = timing[2][sq][r] = 1;
        }
    }
    // EWRAM: 2 wait states (default internal memory control)
    for (int sq = 0; sq < 2; sq++) {
        timing[0][sq][2] = timing[1][sq][2] = 3;
        timing[2][sq][2] = 6;
        // palette / VRAM: 16-bit bus
        timing[2][sq][5] = timing[2][sq][6] = 2;
    }
    for (int ws = 0; ws < 3; ws++) {
        for (int k = 0; k < 2; k++) {
            int r = 8 + ws * 2 + k;
            timing[0][0][r] = timing[1][0][r] = (u8)n[ws];
            timing[0][1][r] = timing[1][1][r] = (u8)s[ws];
            timing[2][0][r] = (u8)(n[ws] + s[ws]);
            timing[2][1][r] = (u8)(2 * s[ws]);
        }
    }
    for (int sq = 0; sq < 2; sq++)
        for (int wd = 0; wd < 3; wd++) timing[wd][sq][0xE] = timing[wd][sq][0xF] = (u8)sram;
}

u32 Bus::openBusRead(u32 addr) {
    (void)addr;
    return g->cpu.openBus();
}

template <typename T>
static RETRA_INLINE T extractOpenBus(u32 v, u32 addr) {
    if (sizeof(T) == 4) return (T)v;
    if (sizeof(T) == 2) return (T)(v >> ((addr & 2) * 8));
    return (T)(v >> ((addr & 3) * 8));
}

template <typename T>
T Bus::readSlow(u32 addr) {
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    switch (addr >> 24) {
        case 0x0:
            if (a < 0x4000) {
                // BIOS is only readable while executing inside it.
                if (g->cpu.r[15] < 0x4000 + 8) return loadLE<T>(&bios[a]);
                return extractOpenBus<T>(biosLatch, addr);
            }
            return extractOpenBus<T>(openBusRead(addr), addr);
        case 0x4: {
            u32 off = a & 0xFFFFFF;
            if (off >= 0x400) {
                if ((off & 0xFFFF) == 0x800 || (off & 0xFFFF) == 0x802) {
                    // Internal memory control (fixed default value).
                    u32 v = 0x0D000020;
                    return extractOpenBus<T>(v, addr);
                }
                return extractOpenBus<T>(openBusRead(addr), addr);
            }
            if (sizeof(T) == 1) return (T)ioRead8(off);
            if (sizeof(T) == 2) return (T)ioRead16(off);
            return (T)(ioRead16(off) | ((u32)ioRead16(off + 2) << 16));
        }
        case 0x5: return loadLE<T>(&pram[a & 0x3FF]);
        case 0x6: {
            u32 o = a & 0x1FFFF;
            if (o >= 0x18000) o -= 0x8000;
            return loadLE<T>(&vram[o]);
        }
        case 0x7: return loadLE<T>(&oam[a & 0x3FF]);
        case 0xD:
            if (g->save.isEeprom() && (!eepromLarge || (addr & 0xFFFFFF) >= 0xFFFF00)) {
                return (T)g->save.eepromRead();
            }
            return loadLE<T>(&rom[a & 0x1FFFFFF]);
        case 0xE:
        case 0xF: {
            u8 b = g->save.read8(addr);
            if (sizeof(T) == 1) return (T)b;
            if (sizeof(T) == 2) return (T)(b * 0x0101u);
            return (T)(b * 0x01010101u);
        }
        default: return extractOpenBus<T>(openBusRead(addr), addr);
    }
}

template <typename T>
void Bus::writeSlow(u32 addr, T value) {
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    switch (addr >> 24) {
        case 0x4: {
            u32 off = a & 0xFFFFFF;
            if (off >= 0x400) return;
            if (sizeof(T) == 1) {
                ioWrite8(off, (u8)value);
            } else if (sizeof(T) == 2) {
                ioWrite16(off, (u16)value);
            } else {
                ioWrite16(off, (u16)value);
                ioWrite16(off + 2, (u16)((u32)value >> 16));
            }
            return;
        }
        case 0x5:
            if (sizeof(T) == 1)
                storeLE<u16>(&pram[a & 0x3FE], (u16)(((u8)value) * 0x0101u));
            else
                storeLE<T>(&pram[a & 0x3FF], value);
            return;
        case 0x6: {
            u32 o = a & 0x1FFFF;
            if (o >= 0x18000) o -= 0x8000;
            if (sizeof(T) == 1) {
                const u32 mode = io16(0) & 7;
                const u32 limit = mode >= 3 ? 0x14000 : 0x10000;
                if (o < limit) storeLE<u16>(&vram[o & ~1u], (u16)(((u8)value) * 0x0101u));
            } else {
                storeLE<T>(&vram[o], value);
            }
            return;
        }
        case 0x7:
            if (sizeof(T) != 1) storeLE<T>(&oam[a & 0x3FF], value);
            return;
        case 0x8:
            if (g->rtc.enabled) {
                u32 off = addr & 0x1FFFFFF;
                if (off >= 0xC4 && off <= 0xC9) {
                    if (sizeof(T) == 4) {
                        g->rtc.write(off & ~1u, (u16)value);
                        g->rtc.write((off & ~1u) + 2, (u16)((u32)value >> 16));
                    } else {
                        g->rtc.write(off & ~1u, (u16)value);
                    }
                    updateGpioView();
                }
            }
            return;
        case 0xD:
            if (g->save.isEeprom() && (!eepromLarge || (addr & 0xFFFFFF) >= 0xFFFF00)) {
                g->save.eepromWrite((u16)value);
            }
            return;
        case 0xE:
        case 0xF: {
            u8 b;
            if (sizeof(T) == 1)
                b = (u8)value;
            else if (sizeof(T) == 2)
                b = (u8)((u32)value >> ((addr & 1) * 8));
            else
                b = (u8)((u32)value >> ((addr & 3) * 8));
            g->save.write8(addr, b);
            return;
        }
        default: return;
    }
}

template u8 Bus::readSlow<u8>(u32);
template u16 Bus::readSlow<u16>(u32);
template u32 Bus::readSlow<u32>(u32);
template void Bus::writeSlow<u8>(u32, u8);
template void Bus::writeSlow<u16>(u32, u16);
template void Bus::writeSlow<u32>(u32, u32);

template <typename T>
void Bus::poke(u32 addr, T value) {
    u64 saved = g->sched.now;
    write<T>(addr, value);
    g->sched.now = saved;
}
template void Bus::poke<u8>(u32, u8);
template void Bus::poke<u16>(u32, u16);
template void Bus::poke<u32>(u32, u32);

// ------------------------------------------------------------------ IO registers

u16 Bus::ioRead16(u32 addr) {
    addr &= 0x3FE;
    switch (addr) {
        case 0x000:
        case 0x002:
        case 0x004:
        case 0x006:
        case 0x008:
        case 0x00A:
        case 0x00C:
        case 0x00E: return io16(addr);
        case 0x048: return io16(addr) & 0x3F3F;
        case 0x04A: return io16(addr) & 0x3F3F;
        case 0x050: return io16(addr) & 0x3FFF;
        case 0x052: return io16(addr) & 0x1F1F;
        case 0x0BA: return g->dma.readControl(0);
        case 0x0C6: return g->dma.readControl(1);
        case 0x0D2: return g->dma.readControl(2);
        case 0x0DE: return g->dma.readControl(3);
        case 0x0B8:
        case 0x0C4:
        case 0x0D0:
        case 0x0DC: return 0;
        case 0x100: return g->timers.readCounter(0);
        case 0x104: return g->timers.readCounter(1);
        case 0x108: return g->timers.readCounter(2);
        case 0x10C: return g->timers.readCounter(3);
        case 0x102:
        case 0x106:
        case 0x10A:
        case 0x10E: return io16(addr) & 0x00C7;
        case 0x120:
        case 0x122:
        case 0x124:
        case 0x126:
        case 0x128:
        case 0x12A:
        case 0x132:
        case 0x134:
        case 0x140:
        case 0x150:
        case 0x152:
        case 0x154:
        case 0x156:
        case 0x158: return io16(addr);
        case 0x130: return g->keyInput;
        case 0x200:
        case 0x202: return io16(addr);
        case 0x204: return io16(addr);
        case 0x208: return io16(addr) & 1;
        case 0x300: return io16(addr) & 0x00FF;
        default:
            if (addr >= 0x060 && addr < 0x0B0) return g->apu.read16(addr);
            return 0;
    }
}

void Bus::ioWrite8(u32 addr, u8 value) {
    addr &= 0x3FF;
    switch (addr) {
        case 0x301:
            if (value & 0x80) {
                // STOP: treated like a halt that only a keypad/gamepak IRQ wakes.
                g->cpu.halted = true;
            } else {
                g->cpu.halted = true;
            }
            return;
        case 0x300: io[0x300] = value & 1; return;
        case 0x202:
        case 0x203:
            ioWrite16(0x202, (u16)(value << ((addr & 1) * 8)));
            return;
        case 0x0A0:
        case 0x0A1:
        case 0x0A2:
        case 0x0A3: g->apu.fifoWrite(0, &value, 1); return;
        case 0x0A4:
        case 0x0A5:
        case 0x0A6:
        case 0x0A7: g->apu.fifoWrite(1, &value, 1); return;
        default: break;
    }
    const u32 a = addr & ~1u;
    const u32 shift = (addr & 1) * 8;
    u16 old = io16(a);
    u16 merged = (u16)((old & ~(0xFF << shift)) | (value << shift));
    ioWrite16(a, merged);
}

void Bus::ioWrite16(u32 addr, u16 v) {
    addr &= 0x3FE;
    switch (addr) {
        case 0x000: setIo16(0, v & 0xFFF7); return;
        case 0x002: setIo16(2, v); return;
        case 0x004:
            setIo16(4, (u16)((io16(4) & 0x0007) | (v & 0xFFB8)));
            g->ppu.onDispstatWrite();
            return;
        case 0x006: return;
        case 0x028:
        case 0x02A:
        case 0x02C:
        case 0x02E:
        case 0x038:
        case 0x03A:
        case 0x03C:
        case 0x03E:
            setIo16(addr, v);
            g->ppu.onAffineWrite(addr);
            return;
        case 0x0A0:
        case 0x0A2: {
            u8 b[2] = {(u8)v, (u8)(v >> 8)};
            g->apu.fifoWrite(0, b, 2);
            return;
        }
        case 0x0A4:
        case 0x0A6: {
            u8 b[2] = {(u8)v, (u8)(v >> 8)};
            g->apu.fifoWrite(1, b, 2);
            return;
        }
        case 0x0BA: setIo16(addr, v); g->dma.writeControl(0, v); return;
        case 0x0C6: setIo16(addr, v); g->dma.writeControl(1, v); return;
        case 0x0D2: setIo16(addr, v); g->dma.writeControl(2, v); return;
        case 0x0DE: setIo16(addr, v); g->dma.writeControl(3, v); return;
        case 0x100:
        case 0x104:
        case 0x108:
        case 0x10C:
            setIo16(addr, v);
            g->timers.writeReload((addr - 0x100) >> 2, v);
            return;
        case 0x102:
        case 0x106:
        case 0x10A:
        case 0x10E:
            setIo16(addr, v);
            g->timers.writeControl((addr - 0x102) >> 2, v);
            return;
        case 0x128: {
            // SIOCNT: no link partner. Complete transfers immediately.
            u16 nv = v;
            if (nv & 0x80) {
                nv &= ~0x80;
                setIo16(0x120, 0xFFFF);
                setIo16(0x122, 0xFFFF);
                setIo16(0x124, 0xFFFF);
                setIo16(0x126, 0xFFFF);
                setIo16(0x12A, 0xFFFF);
                if (nv & 0x4000) g->raiseIrq(IRQ_SERIAL);
            }
            setIo16(addr, nv);
            return;
        }
        case 0x130: return;
        case 0x132:
            setIo16(addr, v & 0xC3FF);
            g->checkKeypadIrq();
            return;
        case 0x200:
            setIo16(addr, v & 0x3FFF);
            g->updateIrqLine();
            return;
        case 0x202:
            setIo16(addr, io16(0x202) & ~v);
            g->updateIrqLine();
            return;
        case 0x204:
            setIo16(addr, v & 0x5FFF);
            updateWaitstates();
            return;
        case 0x208:
            setIo16(addr, v & 1);
            g->updateIrqLine();
            return;
        case 0x300:
            io[0x300] = v & 1;
            if (v & 0xFF00) ioWrite8(0x301, (u8)(v >> 8));
            else g->cpu.halted = true;
            return;
        default:
            if (addr >= 0x060 && addr < 0x0A0) {
                g->apu.write16(addr, v);
                return;
            }
            setIo16(addr, v);
            return;
    }
}

void Bus::serialize(StateWriter& w) const {
    w.tag("BUS0");
    w.bytes(ewram, sizeof(ewram));
    w.bytes(iwram, sizeof(iwram));
    w.bytes(io, sizeof(io));
    w.bytes(pram, sizeof(pram));
    w.bytes(vram, sizeof(vram));
    w.bytes(oam, sizeof(oam));
    w.pod(biosLatch);
    w.pod(dmaLatch);
    w.pod(haltcnt);
}

void Bus::deserialize(StateReader& rd) {
    rd.tag("BUS0");
    rd.bytes(ewram, sizeof(ewram));
    rd.bytes(iwram, sizeof(iwram));
    rd.bytes(io, sizeof(io));
    rd.bytes(pram, sizeof(pram));
    rd.bytes(vram, sizeof(vram));
    rd.bytes(oam, sizeof(oam));
    rd.pod(biosLatch);
    rd.pod(dmaLatch);
    rd.pod(haltcnt);
    updateWaitstates();
}

}  // namespace retra
