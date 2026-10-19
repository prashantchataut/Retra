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
        case 0x2: dataBreak = true; return loadLE<T>(&ewram[a & 0x3FFFF]);
        case 0x3: dataBreak = true; return loadLE<T>(&iwram[a & 0x7FFF]);
        case 0x8:
        case 0x9:
        case 0xA:
        case 0xB:
        case 0xC: pfNext = ~0u; return loadLE<T>(&rom[a & 0x1FFFFFF]);
        default: dataBreak = true; return readSlow<T>(addr);
    }
}

template <typename T>
RETRA_INLINE void Bus::write(u32 addr, T value, bool seq) {
    writeCount++;
    u32 region = addr >> 24;
    if (RETRA_UNLIKELY(region > 15)) {
        g->sched.now += 1;
        return;
    }
    g->sched.now += timing[busWidthIndex<T>()][seq][region];
    const u32 a = addr & ~(u32)(sizeof(T) - 1);
    dataBreak = true;
    if (region >= 0x8 && region <= 0xD) pfNext = ~0u;
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

// Cartridge code fetch cost (one halfword), modelling the GamePak prefetch buffer:
// while the CPU is busy elsewhere the prefetcher reads ahead one halfword per S cycle
// (up to 8); buffered halfwords then cost a single cycle.
RETRA_INLINE u32 Bus::romFetchCost(u32 addr, u32 region, bool seq) {
    const u64 now = g->sched.now;
    u32 cost;
    if (prefetchEnabled && seq && addr == pfNext) {
        const u32 s16 = timing[1][1][region];
        const u64 elapsed = now - pfLast;
        u32 count = pfCount;
        u32 rem;
        if (elapsed >= 8u * s16) {
            count = 8;
            rem = 0;
        } else {
            rem = (u32)elapsed + pfAccum;
            while (rem >= s16 && count < 8) {
                rem -= s16;
                count++;
            }
            if (count >= 8) rem = 0;
        }
        pfCount = count;
        if (count) {
            // buffer hit: the prefetcher keeps running during this cycle
            pfCount--;
            pfAccum = rem;
            dataBreak = false;
            pfNext = addr + 2;
            pfLast = now;
            return 1;
        }
        cost = s16 - rem;
        pfAccum = 0;
    } else {
        const bool s = seq && addr == pfNext && (prefetchEnabled || !dataBreak);
        cost = timing[1][s][region];
        pfCount = 0;
        pfAccum = 0;
    }
    dataBreak = false;
    pfNext = addr + 2;
    pfLast = now + cost;
    return cost;
}

RETRA_INLINE u32 Bus::fetch32(u32 addr, bool seq) {
    u32 region = (addr >> 24) & 0xF;
    const u32 a = addr & ~3u;
    switch (region) {
        case 0x0:
            g->sched.now += 1;
            if (a < 0x4000) {
                return biosLatch = loadLE<u32>(&bios[a]);
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
            g->sched.now += romFetchCost(a, region, seq);
            g->sched.now += romFetchCost(a + 2, region, true);
            return loadLE<u32>(&rom[a & 0x1FFFFFF]);
        default: return read<u32>(addr, seq);
    }
}

RETRA_INLINE u16 Bus::fetch16(u32 addr, bool seq) {
    u32 region = (addr >> 24) & 0xF;
    const u32 a = addr & ~1u;
    switch (region) {
        case 0x0:
            g->sched.now += 1;
            if (a < 0x4000) {
                biosLatch = loadLE<u32>(&bios[a & ~3u]);
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
            g->sched.now += romFetchCost(a, region, seq);
            return loadLE<u16>(&rom[a & 0x1FFFFFF]);
        default: return read<u16>(addr, seq);
    }
}

// Pipeline refill after a jump (defined here because it needs the inline fetch paths).
RETRA_INLINE void Cpu::branchTo(u32 addr) {
    Bus& b = g->bus;
    if (thumb) {
        addr &= ~1u;
        pipe[0] = b.fetch16(addr, false);
        pipe[1] = b.fetch16(addr + 2, true);
        r[15] = addr + 2;
    } else {
        addr &= ~3u;
        pipe[0] = b.fetch32(addr, false);
        pipe[1] = b.fetch32(addr + 4, true);
        r[15] = addr + 4;
    }
}

}  // namespace retra
