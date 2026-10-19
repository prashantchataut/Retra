// Retra GBA core - top level facade: loading, frame loop, events, save states.
#include "gba.h"

#include "bus_inl.h"
#include "state.h"

namespace retra {

constexpr u32 kHDrawCycles = 1006;

Gba::Gba()
    : bus(this), cpu(this), ppu(this), apu(this), dma(this), timers(this), save(this), rtc(this), cheats(this) {}

static std::string trimAscii(const u8* p, size_t n) {
    std::string s;
    for (size_t i = 0; i < n; i++) {
        char c = (char)p[i];
        if (c == 0) break;
        s.push_back((c >= 32 && c < 127) ? c : '?');
    }
    while (!s.empty() && s.back() == ' ') s.pop_back();
    return s;
}

static bool containsString(const u8* data, size_t size, const char* str) {
    const size_t n = std::strlen(str);
    if (size < n) return false;
    const u8 first = (u8)str[0];
    for (size_t i = 0; i + n <= size; i++) {
        if (data[i] == first && std::memcmp(data + i, str, n) == 0) return true;
    }
    return false;
}

RomHeaderInfo Gba::parseHeader(const u8* rom, size_t size) {
    RomHeaderInfo h;
    if (size >= 0xC0) {
        h.title = trimAscii(rom + 0xA0, 12);
        h.gameCode = trimAscii(rom + 0xAC, 4);
        h.makerCode = trimAscii(rom + 0xB0, 2);
        h.version = rom[0xBC];
    }
    bool unknown = false;
    h.saveKind = CartSave::detect(rom, size, &unknown);
    const std::string& c = h.gameCode;
    bool rtc = false;
    if (c.size() >= 3) {
        const std::string p = c.substr(0, 3);
        // Pokemon Ruby (AXV), Sapphire (AXP), Emerald (BPE), plus a few other RTC carts.
        if (p == "AXV" || p == "AXP" || p == "BPE" || p == "U3I" || p == "U32" || p == "A2Y" || p == "BR4" ||
            p == "BKA" || p == "U33" || p == "BKE")
            rtc = true;
    }
    if (!rtc && containsString(rom, size, "SIIRTC_V")) rtc = true;
    h.hasRtc = rtc;
    return h;
}

bool Gba::loadRom(const u8* rom, size_t romSize, const u8* bios, size_t biosSize, std::string& error) {
    if (!rom || romSize < 0xC0) {
        error = "ROM is too small to be a GBA image";
        return false;
    }
    if (romSize > kRomMax) {
        error = "ROM is larger than 32 MiB";
        return false;
    }
    if (bios && biosSize != 0x4000) {
        error = "BIOS image must be exactly 16 KiB";
        return false;
    }
    header = parseHeader(rom, romSize);
    bus.setRom(rom, romSize);
    bus.loadBios(bios, biosSize);
    bool unknown = false;
    SaveKind k = CartSave::detect(rom, romSize, &unknown);
    save.configure(k, unknown);
    rtc.enabled = header.hasRtc;
    romLoaded = true;
    reset();
    return true;
}

void Gba::reset() {
    sched.clear();
    bus.reset();
    cpu.reset();
    ppu.reset();
    apu.reset();
    dma.reset();
    timers.reset();
    save.resetState();
    rtc.reset();
    bus.updateGpioView();
    keyInput = 0x3FF;
    frameCount = 0;

    sched.schedule(EV_HBLANK, kHDrawCycles);
    sched.schedule(EV_APU_SAMPLE, kCyclesPerSample);

    if (bus.realBios) {
        cpu.setCpsr(0xD3);
        cpu.r[15] = 0 + 8;
    } else {
        bootDirect();
    }
    updateIrqLine();
}

void Gba::bootDirect() {
    // Register state as left by the BIOS boot sequence.
    cpu.setCpsr(0xD3);
    cpu.r[13] = 0x03007FE0;
    cpu.r[14] = 0;
    cpu.spsr = 0;
    cpu.setCpsr(0xD2);
    cpu.r[13] = 0x03007FA0;
    cpu.r[14] = 0;
    cpu.spsr = 0;
    cpu.setCpsr(0x1F);
    cpu.r[13] = 0x03007F00;
    for (int i = 0; i < 13; i++) cpu.r[i] = 0;
    cpu.r[14] = 0;
    cpu.r[15] = 0x08000000 + 8;
    bus.biosLatch = 0xE129F000;
    bus.io[0x300] = 1;  // POSTFLG
    bus.setIo16(0x000, 0x0080);
    // Affine BG identity matrices.
    bus.setIo16(0x020, 0x100);
    bus.setIo16(0x026, 0x100);
    bus.setIo16(0x030, 0x100);
    bus.setIo16(0x036, 0x100);
}

void Gba::setKeys(u16 pressedMask) {
    keyInput = (u16)(~pressedMask & 0x3FF);
    checkKeypadIrq();
}

void Gba::checkKeypadIrq() {
    const u16 cnt = bus.io16(0x132);
    if (!(cnt & 0x4000)) return;
    const u16 pressed = (u16)(~keyInput & 0x3FF);
    const u16 sel = cnt & 0x3FF;
    bool hit = (cnt & 0x8000) ? ((pressed & sel) == sel && sel) : ((pressed & sel) != 0);
    if (hit) raiseIrq(IRQ_KEYPAD);
}

void Gba::raiseIrq(u16 bits) {
    bus.setIo16(0x202, bus.io16(0x202) | bits);
    updateIrqLine();
}

void Gba::updateIrqLine() {
    const u16 pending = bus.io16(0x200) & bus.io16(0x202);
    if (pending) cpu.halted = false;
    cpu.irqLine = (bus.io16(0x208) & 1) && pending;
}

void Gba::processEvents() {
    for (;;) {
        u64 m = kNever;
        int e = -1;
        for (int i = 0; i < EV_COUNT; i++) {
            if (sched.when[i] < m) {
                m = sched.when[i];
                e = i;
            }
        }
        sched.next = m;
        if (e < 0 || m > sched.now) break;
        sched.when[e] = kNever;
        switch (e) {
            case EV_HBLANK:
                sched.when[EV_LINE_END] = m + (kCyclesPerLine - kHDrawCycles);
                ppu.onHBlank();
                break;
            case EV_LINE_END:
                sched.when[EV_HBLANK] = m + kHDrawCycles;
                ppu.onLineEnd();
                break;
            case EV_TIMER0:
            case EV_TIMER1:
            case EV_TIMER2:
            case EV_TIMER3: timers.onOverflowEvent(e - EV_TIMER0); break;
            case EV_APU_SAMPLE:
                sched.when[EV_APU_SAMPLE] = m + kCyclesPerSample;
                apu.sampleEvent();
                break;
            default: break;
        }
    }
}

int Gba::runFrame(u32* video, s16* audio, int maxAudioFrames) {
    if (!romLoaded) return 0;
    if (!cheats.empty()) cheats.apply();
    ppu.frameDone = false;
    // Safety bound: a frame is 280896 cycles; never loop forever.
    const u64 limit = sched.now + kCyclesPerFrame * 2;
    while (!ppu.frameDone && sched.now < limit) {
        cpu.run();
        processEvents();
    }
    frameCount++;
    if (video) std::memcpy(video, ppu.frame, sizeof(ppu.frame));
    int n = 0;
    if (audio && maxAudioFrames > 0) {
        n = (int)apu.drain(audio, (u32)maxAudioFrames);
    } else {
        apu.ringRead = apu.ringWrite;
    }
    return n;
}

// ------------------------------------------------------------------ save states

std::vector<u8> Gba::saveState() {
    StateWriter w;
    w.pod(kStateMagic);
    w.pod(kStateVersion);
    // identify the game
    w.bytes(&bus.rom[0xA0], 16);
    w.pod(sched.now);
    w.pod(sched.when);
    w.pod(keyInput);
    w.pod(frameCount);
    cpu.serialize(w);
    bus.serialize(w);
    ppu.serialize(w);
    apu.serialize(w);
    dma.serialize(w);
    timers.serialize(w);
    save.serialize(w);
    rtc.serialize(w);
    w.tag("END!");
    return w.buf;
}

bool Gba::loadState(const u8* d, size_t n) {
    if (!romLoaded || !d) return false;
    // Validate on a scratch copy first so a bad state cannot corrupt the running game.
    {
        StateReader rd(d, n);
        u32 magic = rd.get<u32>(), ver = rd.get<u32>();
        if (!rd.ok || magic != kStateMagic || ver != kStateVersion) return false;
        u8 id[16];
        rd.bytes(id, 16);
        if (!rd.ok || std::memcmp(id, &bus.rom[0xA0], 16) != 0) return false;
    }
    std::vector<u8> backup = saveState();
    StateReader rd(d, n);
    rd.get<u32>();
    rd.get<u32>();
    u8 id[16];
    rd.bytes(id, 16);
    rd.pod(sched.now);
    rd.pod(sched.when);
    rd.pod(keyInput);
    rd.pod(frameCount);
    cpu.deserialize(rd);
    bus.deserialize(rd);
    ppu.deserialize(rd);
    apu.deserialize(rd);
    dma.deserialize(rd);
    timers.deserialize(rd);
    save.deserialize(rd);
    rtc.deserialize(rd);
    rd.tag("END!");
    if (!rd.ok) {
        StateReader rb(backup.data(), backup.size());
        rb.get<u32>();
        rb.get<u32>();
        rb.bytes(id, 16);
        rb.pod(sched.now);
        rb.pod(sched.when);
        rb.pod(keyInput);
        rb.pod(frameCount);
        cpu.deserialize(rb);
        bus.deserialize(rb);
        ppu.deserialize(rb);
        apu.deserialize(rb);
        dma.deserialize(rb);
        timers.deserialize(rb);
        save.deserialize(rb);
        rtc.deserialize(rb);
        sched.recompute();
        bus.updateGpioView();
        updateIrqLine();
        return false;
    }
    sched.recompute();
    bus.updateGpioView();
    updateIrqLine();
    return true;
}

}  // namespace retra
