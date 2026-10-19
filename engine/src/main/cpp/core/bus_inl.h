// Retra GBA core - inline fast paths of the system bus. Include after gba.h.
#pragma once

#include "gba.h"

namespace retra {

template <typename T>
constexpr int busWidthIndex() {
    return sizeof(T) == 1 ? 0 : (sizeof(T) == 2 ? 1 : 2);
}

template <typename T>
RETRA_INLINE T Bus::read(u32 addr, bool seq) {
    u32 region = addr >> 24;
    if (RETRA_UNLIKELY(region > 15)) {
        g->sched.now += 1;
        return readSlow<T>(addr);
    }
    g->sched.now += timing[busWidthIndex<T>()][seq][region];
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    switch (region) {
        case 0x2: return loadLE<T>(&ewram[a & 0x3FFFF]);
        case 0x3: return loadLE<T>(&iwram[a & 0x7FFF]);
        case 0x8:
        case 0x9:
        case 0xA:
        case 0xB:
        case 0xC: return loadLE<T>(&rom[a & 0x1FFFFFF]);
        default: return readSlow<T>(addr);
    }
}

template <typename T>
RETRA_INLINE void Bus::write(u32 addr, T value, bool seq) {
    u32 region = addr >> 24;
    if (RETRA_UNLIKELY(region > 15)) {
        g->sched.now += 1;
        return;
    }
    g->sched.now += timing[busWidthIndex<T>()][seq][region];
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    switch (region) {
        case 0x2: storeLE<T>(&ewram[a & 0x3FFFF], value); return;
        case 0x3: storeLE<T>(&iwram[a & 0x7FFF], value); return;
        default: writeSlow<T>(addr, value); return;
    }
}

template <typename T>
RETRA_INLINE T Bus::peek(u32 addr) {
    u32 region = addr >> 24;
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    switch (region) {
        case 0x0: return a < 0x4000 ? loadLE<T>(&bios[a]) : 0;
        case 0x2: return loadLE<T>(&ewram[a & 0x3FFFF]);
        case 0x3: return loadLE<T>(&iwram[a & 0x7FFF]);
        case 0x5: return loadLE<T>(&pram[a & 0x3FF]);
        case 0x6: {
            u32 o = a & 0x1FFFF;
            if (o >= 0x18000) o -= 0x8000;
            return loadLE<T>(&vram[o]);
        }
        case 0x7: return loadLE<T>(&oam[a & 0x3FF]);
        case 0x8:
        case 0x9:
        case 0xA:
        case 0xB:
        case 0xC:
        case 0xD: return loadLE<T>(&rom[a & 0x1FFFFFF]);
        default: return readSlow<T>(addr);
    }
}

inline u32 Bus::fetch32(u32 addr, bool seq) {
    u32 region = (addr >> 24) & 0xF;
    const u32 a = addr & ~3u;
    switch (region) {
        case 0x0:
            g->sched.now += 1;
            if (a < 0x4000) {
                if (a + 8 < 0x4000) biosLatch = loadLE<u32>(&bios[a + 8]);
                return loadLE<u32>(&bios[a]);
            }
            return 0;
        case 0x2: g->sched.now += timing[2][seq][2]; return loadLE<u32>(&ewram[a & 0x3FFFF]);
        case 0x3: g->sched.now += 1; return loadLE<u32>(&iwram[a & 0x7FFF]);
        case 0x8:
        case 0x9:
        case 0xA:
        case 0xB:
        case 0xC:
        case 0xD:
            g->sched.now += (seq && prefetchEnabled) ? 2 : timing[2][seq][region];
            return loadLE<u32>(&rom[a & 0x1FFFFFF]);
        default: return read<u32>(addr, seq);
    }
}

inline u16 Bus::fetch16(u32 addr, bool seq) {
    u32 region = (addr >> 24) & 0xF;
    const u32 a = addr & ~1u;
    switch (region) {
        case 0x0:
            g->sched.now += 1;
            if (a < 0x4000) {
                if (a + 4 < 0x4000) biosLatch = loadLE<u32>(&bios[(a + 4) & ~3u]);
                return loadLE<u16>(&bios[a]);
            }
            return 0;
        case 0x2: g->sched.now += timing[1][seq][2]; return loadLE<u16>(&ewram[a & 0x3FFFF]);
        case 0x3: g->sched.now += 1; return loadLE<u16>(&iwram[a & 0x7FFF]);
        case 0x8:
        case 0x9:
        case 0xA:
        case 0xB:
        case 0xC:
        case 0xD:
            g->sched.now += (seq && prefetchEnabled) ? 1 : timing[1][seq][region];
            return loadLE<u16>(&rom[a & 0x1FFFFFF]);
        default: return read<u16>(addr, seq);
    }
}

}  // namespace retra
