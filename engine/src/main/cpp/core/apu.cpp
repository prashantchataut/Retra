// Retra GBA core - APU. PSG channels are integrated over each 512-cycle output period
// (box filter, avoids most aliasing); Direct Sound FIFOs are sample-and-hold like hardware.
#include "apu.h"

#include "gba.h"
#include "state.h"

namespace retra {

static constexpr u8 kDuty[4] = {0x01, 0x81, 0x87, 0x7E};

void Apu::reset() {
    sq[0] = SquareCh{};
    sq[1] = SquareCh{};
    wave = WaveCh{};
    noise = NoiseCh{};
    seqStep = 0;
    seqCycles = 0;
    master = false;
    for (auto& f : fifo) f = Fifo{};
    std::memset(waveRam, 0, sizeof(waveRam));
    std::memset(ring, 0, sizeof(ring));
    ringWrite = ringRead = 0;
}

bool Apu::timerUsedForFifo(int timer) const {
    if (!master || timer > 1) return false;
    const u16 h = g->bus.io16(0x82);
    if ((h & 0x0300) && (u32)((h >> 10) & 1) == (u32)timer) return true;
    if ((h & 0x3000) && (u32)((h >> 14) & 1) == (u32)timer) return true;
    return false;
}

u16 Apu::read16(u32 addr) {
    Bus& b = g->bus;
    switch (addr) {
        case 0x60: return b.io16(addr) & 0x007F;
        case 0x62: return b.io16(addr) & 0xFFC0;
        case 0x64: return b.io16(addr) & 0x4000;
        case 0x68: return b.io16(addr) & 0xFFC0;
        case 0x6C: return b.io16(addr) & 0x4000;
        case 0x70: return b.io16(addr) & 0x00E0;
        case 0x72: return b.io16(addr) & 0xE000;
        case 0x74: return b.io16(addr) & 0x4000;
        case 0x78: return b.io16(addr) & 0xFF00;
        case 0x7C: return b.io16(addr) & 0x40FF;
        case 0x80: return b.io16(addr) & 0xFF77;
        case 0x82: return b.io16(addr) & 0x770F;
        case 0x84:
            return (u16)((master ? 0x80 : 0) | (sq[0].on ? 1 : 0) | (sq[1].on ? 2 : 0) | (wave.on ? 4 : 0) |
                         (noise.on ? 8 : 0));
        case 0x88: return b.io16(addr);
        default:
            if (addr >= 0x90 && addr < 0xA0) {
                const u32 o = addr - 0x90;
                const u8* bank = waveRam[wave.bank ^ 1];
                return (u16)(bank[o] | (bank[o + 1] << 8));
            }
            return 0;
    }
}

void Apu::write16(u32 addr, u16 v) {
    Bus& b = g->bus;
    if (addr >= 0x90 && addr < 0xA0) {
        u8* bank = waveRam[wave.bank ^ 1];
        bank[addr - 0x90] = (u8)v;
        bank[addr - 0x90 + 1] = (u8)(v >> 8);
        return;
    }
    if (addr == 0x84) {
        const bool on = v & 0x80;
        if (master && !on) {
            for (u32 a = 0x60; a < 0x82; a += 2) b.setIo16(a, 0);
            sq[0] = SquareCh{};
            sq[1] = SquareCh{};
            const bool bank = wave.bank;
            wave = WaveCh{};
            wave.bank = bank;
            noise = NoiseCh{};
        }
        master = on;
        b.setIo16(0x84, v & 0x80);
        g->timers.reschedule();
        return;
    }
    if (addr == 0x88) {
        b.setIo16(0x88, v & 0xC3FE);
        return;
    }
    if (addr == 0x82) {
        b.setIo16(0x82, v & 0x770F);
        if (v & 0x0800) fifoReset(0);
        if (v & 0x8000) fifoReset(1);
        g->timers.reschedule();
        return;
    }
    if (!master) return;  // PSG registers are read-only while the APU is off
    const bool trigger = v & 0x8000;
    switch (addr) {
        case 0x60: {
            b.setIo16(addr, v);
            SquareCh& c = sq[0];
            c.sweepShift = v & 7;
            c.sweepNeg = (v >> 3) & 1;
            c.sweepPeriod = (v >> 4) & 7;
            break;
        }
        case 0x62:
        case 0x68: {
            b.setIo16(addr, v);
            SquareCh& c = sq[addr == 0x62 ? 0 : 1];
            c.length = 64 - (v & 63);
            c.duty = (v >> 6) & 3;
            c.env.load(v);
            if (!c.env.dacOn()) c.on = false;
            break;
        }
        case 0x64:
        case 0x6C: {
            b.setIo16(addr, v & 0x7FFF);
            const int i = addr == 0x64 ? 0 : 1;
            SquareCh& c = sq[i];
            c.freq = v & 0x7FF;
            c.lengthEnable = v & 0x4000;
            if (trigger) triggerSquare(i);
            break;
        }
        case 0x70:
            b.setIo16(addr, v);
            wave.dim64 = v & 0x20;
            wave.bank = (v >> 6) & 1;
            wave.dac = v & 0x80;
            if (!wave.dac) wave.on = false;
            break;
        case 0x72:
            b.setIo16(addr, v);
            wave.length = 256 - (v & 0xFF);
            wave.volCode = (v >> 13) & 3;
            wave.force75 = v & 0x8000;
            break;
        case 0x74:
            b.setIo16(addr, v & 0x7FFF);
            wave.freq = v & 0x7FF;
            wave.lengthEnable = v & 0x4000;
            if (trigger) triggerWave();
            break;
        case 0x78:
            b.setIo16(addr, v);
            noise.length = 64 - (v & 63);
            noise.env.load(v);
            if (!noise.env.dacOn()) noise.on = false;
            break;
        case 0x7C: {
            b.setIo16(addr, v & 0x7FFF);
            const u32 r = v & 7, s = (v >> 4) & 15;
            noise.narrow = v & 8;
            noise.period = (r ? (r << 6) : 32u) << s;
            noise.lengthEnable = v & 0x4000;
            if (trigger) triggerNoise();
            break;
        }
        case 0x80: b.setIo16(addr, v & 0xFF77); break;
        default: b.setIo16(addr, v); break;
    }
}

void Apu::triggerSquare(int i) {
    SquareCh& c = sq[i];
    c.on = c.env.dacOn();
    if (c.length == 0) c.length = 64;
    c.timer = (s32)((2048 - c.freq) * 16);
    c.env.trigger();
    if (i == 0) {
        c.shadow = c.freq;
        c.sweepTimer = c.sweepPeriod ? c.sweepPeriod : 8;
        c.sweepOn = c.sweepPeriod || c.sweepShift;
        if (c.sweepShift && sweepCalc() > 2047) c.on = false;
    }
}

void Apu::triggerWave() {
    wave.on = wave.dac;
    if (wave.length == 0) wave.length = 256;
    wave.timer = (s32)((2048 - wave.freq) * 8);
    wave.pos = 0;
}

void Apu::triggerNoise() {
    noise.on = noise.env.dacOn();
    if (noise.length == 0) noise.length = 64;
    noise.env.trigger();
    noise.lfsr = noise.narrow ? 0x7F : 0x7FFF;
    noise.timer = (s32)noise.period;
}

u32 Apu::sweepCalc() {
    SquareCh& c = sq[0];
    const u32 d = c.shadow >> c.sweepShift;
    return c.sweepNeg ? c.shadow - d : c.shadow + d;
}

void Apu::clockLength() {
    for (auto& c : sq)
        if (c.lengthEnable && c.length && --c.length == 0) c.on = false;
    if (wave.lengthEnable && wave.length && --wave.length == 0) wave.on = false;
    if (noise.lengthEnable && noise.length && --noise.length == 0) noise.on = false;
}

void Apu::clockSweep() {
    SquareCh& c = sq[0];
    if (--c.sweepTimer == 0) {
        c.sweepTimer = c.sweepPeriod ? c.sweepPeriod : 8;
        if (c.sweepOn && c.sweepPeriod) {
            const u32 nf = sweepCalc();
            if (nf > 2047) {
                c.on = false;
            } else if (c.sweepShift) {
                c.shadow = nf;
                c.freq = nf;
                if (sweepCalc() > 2047) c.on = false;
            }
        }
    }
}

void Apu::clockEnvelope() {
    sq[0].env.clock();
    sq[1].env.clock();
    noise.env.clock();
}

// Returns the channel output integrated over 'cycles' (amplitude units * cycles).
s32 Apu::squareOut(int i, u32 cycles) {
    SquareCh& c = sq[i];
    const u32 period = (2048 - c.freq) * 16;
    if (!c.on) {
        // keep phase running
        return 0;
    }
    const s32 vol = (s32)c.env.vol;
    const u8 pattern = kDuty[c.duty];
    s32 acc = 0;
    u32 rem = cycles;
    while (rem) {
        const u32 step = (u32)c.timer < rem ? (u32)c.timer : rem;
        acc += ((pattern >> c.pos) & 1) ? vol * (s32)step : -vol * (s32)step;
        c.timer -= (s32)step;
        rem -= step;
        if (c.timer <= 0) {
            c.timer += (s32)period;
            c.pos = (c.pos + 1) & 7;
        }
    }
    return acc;
}

s32 Apu::waveOut(u32 cycles) {
    if (!wave.on) return 0;
    static constexpr s32 kVolMul[4] = {0, 4, 2, 1};
    const s32 mul = wave.force75 ? 3 : kVolMul[wave.volCode];
    const u32 period = (2048 - wave.freq) * 8;
    s32 acc = 0;
    u32 rem = cycles;
    while (rem) {
        const u32 step = (u32)wave.timer < rem ? (u32)wave.timer : rem;
        const u32 bankSel = wave.dim64 ? (wave.bank ^ (wave.pos >> 5)) : wave.bank;
        const u32 idx = wave.pos & 31;
        const u8 byte = waveRam[bankSel][idx >> 1];
        const s32 s = (idx & 1) ? (byte & 15) : (byte >> 4);
        acc += ((2 * s - 15) * mul / 4) * (s32)step;
        wave.timer -= (s32)step;
        rem -= step;
        if (wave.timer <= 0) {
            wave.timer += (s32)period;
            wave.pos = (wave.pos + 1) & (wave.dim64 ? 63 : 31);
        }
    }
    return acc;
}

s32 Apu::noiseOut(u32 cycles) {
    if (!noise.on) return 0;
    const s32 vol = (s32)noise.env.vol;
    s32 acc = 0;
    u32 rem = cycles;
    while (rem) {
        const u32 step = (u32)noise.timer < rem ? (u32)noise.timer : rem;
        acc += (noise.lfsr & 1) ? -vol * (s32)step : vol * (s32)step;
        noise.timer -= (s32)step;
        rem -= step;
        if (noise.timer <= 0) {
            noise.timer += (s32)noise.period;
            const u32 bit = (noise.lfsr ^ (noise.lfsr >> 1)) & 1;
            noise.lfsr = (noise.lfsr >> 1) | (bit << 14);
            if (noise.narrow) noise.lfsr = (noise.lfsr & ~0x40u) | (bit << 6);
        }
    }
    return acc;
}

void Apu::fifoReset(int ch) {
    Fifo& f = fifo[ch];
    f.r = f.w = f.count = 0;
}

void Apu::fifoWrite(int ch, const u8* bytes, int n) {
    Fifo& f = fifo[ch];
    for (int i = 0; i < n; i++) {
        if (f.count >= 32) {
            // overflow: hardware drops the FIFO contents
            f.r = f.w = f.count = 0;
        }
        f.data[f.w] = (s8)bytes[i];
        f.w = (f.w + 1) & 31;
        f.count++;
    }
}

void Apu::onTimerOverflow(int timer) {
    if (!master) return;
    const u16 h = g->bus.io16(0x82);
    for (int ch = 0; ch < 2; ch++) {
        const u32 sel = (h >> (10 + ch * 4)) & 1;
        if (sel != (u32)timer) continue;
        if (!(h & (0x300 << (ch * 4)))) continue;
        Fifo& f = fifo[ch];
        if (f.count) {
            f.cur = f.data[f.r];
            f.r = (f.r + 1) & 31;
            f.count--;
        }
        if (f.count <= 16) g->dma.requestFifo(ch);
    }
}

void Apu::sampleEvent() {
    Bus& b = g->bus;
    s32 left = 0, right = 0;
    if (master) {
        const u32 cyc = kCyclesPerSample;
        s32 o[4];
        o[0] = squareOut(0, cyc);
        o[1] = squareOut(1, cyc);
        o[2] = waveOut(cyc);
        o[3] = noiseOut(cyc);
        const u16 l = b.io16(0x80);
        const u16 h = b.io16(0x82);
        s32 pr = 0, pl = 0;
        for (int i = 0; i < 4; i++) {
            if (l & (0x100 << i)) pr += o[i];
            if (l & (0x1000 << i)) pl += o[i];
        }
        // average over the period, apply master volume (1..8)
        pr = pr * (s32)((l & 7) + 1) / (s32)cyc;
        pl = pl * (s32)(((l >> 4) & 7) + 1) / (s32)cyc;
        const u32 ratio = h & 3;
        const int psgShift = ratio == 0 ? 2 : (ratio == 1 ? 1 : 0);
        right = pr >> psgShift;
        left = pl >> psgShift;
        for (int ch = 0; ch < 2; ch++) {
            const s32 s = (s32)fifo[ch].cur * ((h & (4 << ch)) ? 4 : 2);
            if (h & (0x100 << (ch * 4))) right += s;
            if (h & (0x200 << (ch * 4))) left += s;
        }
    }
    // frame sequencer (512 Hz)
    seqCycles += kCyclesPerSample;
    if (seqCycles >= 32768) {
        seqCycles -= 32768;
        switch (seqStep) {
            case 0:
            case 4: clockLength(); break;
            case 2:
            case 6:
                clockLength();
                clockSweep();
                break;
            case 7: clockEnvelope(); break;
            default: break;
        }
        seqStep = (seqStep + 1) & 7;
    }
    const s32 bias = b.io16(0x88) & 0x3FE;
    auto out = [bias](s32 v) -> s16 {
        s32 t = v + bias;
        if (t < 0) t = 0;
        if (t > 0x3FF) t = 0x3FF;
        s32 o = (t - bias) * 64;
        if (o > 32767) o = 32767;
        if (o < -32768) o = -32768;
        return (s16)o;
    };
    if (ringWrite - ringRead >= kRing) ringRead = ringWrite - kRing + 1;
    const u32 idx = (ringWrite % kRing) * 2;
    ring[idx] = out(left);
    ring[idx + 1] = out(right);
    ringWrite++;
}

u32 Apu::drain(s16* out, u32 maxFrames) {
    u32 n = available();
    if (n > maxFrames) {
        // keep the newest samples
        ringRead = ringWrite - maxFrames;
        n = maxFrames;
    }
    for (u32 i = 0; i < n; i++) {
        const u32 idx = ((ringRead + i) % kRing) * 2;
        out[i * 2] = ring[idx];
        out[i * 2 + 1] = ring[idx + 1];
    }
    ringRead += n;
    return n;
}

void Apu::serialize(StateWriter& w) const {
    w.tag("APU0");
    w.pod(sq);
    w.pod(wave);
    w.pod(noise);
    w.pod(seqStep);
    w.pod(seqCycles);
    w.pod(master);
    w.pod(fifo);
    w.pod(waveRam);
}

void Apu::deserialize(StateReader& rd) {
    rd.tag("APU0");
    rd.pod(sq);
    rd.pod(wave);
    rd.pod(noise);
    rd.pod(seqStep);
    rd.pod(seqCycles);
    rd.pod(master);
    rd.pod(fifo);
    rd.pod(waveRam);
    for (auto& f : fifo) {
        f.r &= 31;
        f.w &= 31;
        if (f.count > 32) f.count = 0;
    }
    seqStep &= 7;
    ringRead = ringWrite;
}

}  // namespace retra
