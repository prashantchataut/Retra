// Retra GBA core - top level facade.
#pragma once

#include <string>
#include <vector>

#include "apu.h"
#include "bus.h"
#include "cart_save.h"
#include "cheats.h"
#include "common.h"
#include "cpu.h"
#include "dma.h"
#include "ppu.h"
#include "rtc.h"
#include "scheduler.h"
#include "timer.h"

namespace retra {

enum IrqBit : u16 {
    IRQ_VBLANK = 1 << 0,
    IRQ_HBLANK = 1 << 1,
    IRQ_VCOUNT = 1 << 2,
    IRQ_TIMER0 = 1 << 3,
    IRQ_SERIAL = 1 << 7,
    IRQ_DMA0 = 1 << 8,
    IRQ_KEYPAD = 1 << 12,
    IRQ_GAMEPAK = 1 << 13,
};

struct RomHeaderInfo {
    std::string title;
    std::string gameCode;
    std::string makerCode;
    int version = 0;
    SaveKind saveKind = SaveKind::NONE;
    bool hasRtc = false;
};

class Gba {
public:
    Gba();
    ~Gba() = default;
    Gba(const Gba&) = delete;
    Gba& operator=(const Gba&) = delete;

    // Load a ROM (and optional 16 KiB BIOS; null -> built-in replacement BIOS).
    bool loadRom(const u8* rom, size_t romSize, const u8* bios, size_t biosSize, std::string& error);
    void reset();
    bool loaded() const { return romLoaded; }

    void setKeys(u16 pressedMask);  // GbaKey bits, 1 = pressed
    // Run one video frame. video: 240*160 ARGB8888 (may be null). audio: interleaved stereo,
    // capacity maxAudioFrames stereo frames (may be null). Returns stereo frames written.
    int runFrame(u32* video, s16* audio, int maxAudioFrames);

    // Battery saves
    bool hasBattery() const { return save.kind != SaveKind::NONE; }
    std::vector<u8> batterySave() const { return save.data; }
    bool loadBatterySave(const u8* d, size_t n) { return save.load(d, n); }
    bool consumeSaveDirty() {
        bool d = save.dirty;
        save.dirty = false;
        return d;
    }

    std::vector<u8> saveState();
    bool loadState(const u8* d, size_t n);

    int setCheats(const std::vector<std::string>& codes) { return cheats.set(codes); }
    void setColorCorrection(bool on) { ppu.setColorCorrection(on); }
    void setRtcOffsetSeconds(s64 off) { rtc.userOffset = off; }

    const RomHeaderInfo& info() const { return header; }
    static RomHeaderInfo parseHeader(const u8* rom, size_t size);

    // --- internal interface for components
    void raiseIrq(u16 bits);
    void updateIrqLine();
    void processEvents();
    void hleCall(u32 n);  // BIOS trap
    void checkKeypadIrq();
    u16 keyInput = 0x3FF;  // KEYINPUT register (active low)

    Scheduler sched;
    Bus bus;
    Cpu cpu;
    Ppu ppu;
    Apu apu;
    Dma dma;
    Timers timers;
    CartSave save;
    Rtc rtc;
    Cheats cheats;

    u64 frameCount = 0;

private:
    void bootDirect();
    bool romLoaded = false;
    RomHeaderInfo header;
    std::vector<u8> userBios;
};

}  // namespace retra
