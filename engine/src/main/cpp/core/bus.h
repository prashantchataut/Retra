// Retra GBA core - system bus / memory map.
#pragma once

#include <vector>

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

constexpr u32 kRomMax = 32 * 1024 * 1024;

class Bus {
public:
    explicit Bus(Gba* gba) : g(gba) {}

    void reset();
    void loadBios(const u8* data, size_t size);  // size must be 16 KiB
    void setRom(const u8* data, size_t size);

    // Timed accesses (add wait-state cycles to the scheduler clock).
    template <typename T>
    T read(u32 addr, bool seq = false);
    template <typename T>
    void write(u32 addr, T value, bool seq = false);
    // Untimed, side-effect-free (as far as possible) access for HLE/cheats/debug.
    template <typename T>
    T peek(u32 addr);
    template <typename T>
    void poke(u32 addr, T value);  // untimed write, normal side effects

    // Code fetches (timed). seq = sequential to previous fetch.
    u32 fetch32(u32 addr, bool seq);
    u16 fetch16(u32 addr, bool seq);

    // IO register access (bus.cpp / io.cpp)
    u16 ioRead16(u32 addr);
    void ioWrite16(u32 addr, u16 value);
    void ioWrite8(u32 addr, u8 value);
    u8 ioRead8(u32 addr) {
        u16 v = ioRead16(addr & ~1u);
        return (addr & 1) ? (v >> 8) : (v & 0xFF);
    }
    RETRA_INLINE u16 io16(u32 off) const { return loadLE<u16>(&io[off]); }
    RETRA_INLINE void setIo16(u32 off, u16 v) { storeLE<u16>(&io[off], v); }

    void updateWaitstates();

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    // Patch ROM bytes at 0xC4..0xC9 (GPIO registers visible when readable).
    void updateGpioView();

    Gba* g;

    alignas(16) u8 bios[0x4000] = {};
    alignas(16) u8 ewram[0x40000] = {};
    alignas(16) u8 iwram[0x8000] = {};
    alignas(16) u8 io[0x400] = {};
    alignas(16) u8 pram[0x400] = {};
    alignas(16) u8 vram[0x18000] = {};
    alignas(16) u8 oam[0x400] = {};
    std::vector<u8> rom;  // always kRomMax + 4 bytes, tail filled with open-bus pattern
    u32 romSize = 0;
    u8 gpioRomBackup[6] = {};

    u32 biosLatch = 0;      // last opcode fetched from BIOS
    bool realBios = false;  // user supplied BIOS image
    bool eepromLarge = false;  // ROM > 16 MiB: EEPROM only at 0x0DFFFF00..
    u32 dmaLatch = 0;          // last value moved by DMA (DMA open bus)

    // timing[width: 0=8,1=16,2=32][seq][region]
    u8 timing[3][2][16] = {};
    bool prefetchEnabled = false;

    u16 haltcnt = 0;
    // GamePak prefetch / sequential-access tracking
    u32 pfNext = ~0u;    // address the prefetcher (or sequential stream) expects next
    u32 pfCount = 0;     // halfwords buffered
    u32 pfAccum = 0;     // partial progress of the next halfword (cycles)
    u64 pfLast = 0;      // time of last code fetch completion
    bool dataBreak = false;  // a data access happened since the last code fetch
    u32 romFetchCost(u32 addr, u32 region, bool seq);
    u32 writeCount = 0;  // bumped on every timed write (idle-loop detection)
    u32 volatileReads = 0;  // reads of free-running registers (timer counters)

    // Optional debug-print port (mGBA-compatible register layout at 0x04FFF600..0x04FFF781),
    // only active when a host callback is installed (test harness).
    void (*debugCallback)(void* user, int level, const char* msg) = nullptr;
    void* debugUser = nullptr;
    bool debugUnlocked = false;
    char debugString[0x101] = {};

private:
    template <typename T>
    T readSlow(u32 addr);
    template <typename T>
    void writeSlow(u32 addr, T value);
    u32 openBusRead(u32 addr);
    static bool ioIsOpenBus(u32 off);
    void debugWrite(u32 off, u32 value, u32 size);
};

}  // namespace retra
