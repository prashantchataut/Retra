// Retra GBA core - audio processing unit (PSG 1-4 + Direct Sound A/B).
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

struct Envelope {
    u32 initVol = 0, dirUp = 0, period = 0;
    u32 vol = 0, timer = 0;
    void load(u16 reg) {
        period = (reg >> 8) & 7;
        dirUp = (reg >> 11) & 1;
        initVol = (reg >> 12) & 15;
    }
    bool dacOn() const { return initVol != 0 || dirUp; }
    void trigger() {
        vol = initVol;
        timer = period ? period : 8;
    }
    void clock() {
        if (!period) return;
        if (--timer == 0) {
            timer = period;
            if (dirUp && vol < 15) vol++;
            else if (!dirUp && vol > 0) vol--;
        }
    }
};

struct SquareCh {
    bool on = false;
    u32 duty = 0, length = 0;
    bool lengthEnable = false;
    u32 freq = 0;
    s32 timer = 0;  // cycles until next duty step
    u32 pos = 0;
    Envelope env;
    // sweep (channel 1 only)
    u32 sweepPeriod = 0, sweepNeg = 0, sweepShift = 0, sweepTimer = 0, shadow = 0;
    bool sweepOn = false;
};

struct WaveCh {
    bool on = false;
    bool dac = false;
    bool dim64 = false;  // two banks played as 64 samples
    u32 bank = 0;        // playing bank
    u32 length = 0;
    bool lengthEnable = false;
    u32 volCode = 0;
    bool force75 = false;
    u32 freq = 0;
    s32 timer = 0;
    u32 pos = 0;  // 0..63
};

struct NoiseCh {
    bool on = false;
    u32 length = 0;
    bool lengthEnable = false;
    Envelope env;
    u32 lfsr = 0x7FFF;
    bool narrow = false;
    u32 period = 8;
    s32 timer = 0;
};

class Apu {
public:
    explicit Apu(Gba* gba) : g(gba) {}
    void reset();

    void write16(u32 addr, u16 value);  // value already stored in io[]
    u16 read16(u32 addr);
    void fifoWrite(int ch, const u8* bytes, int n);
    void onTimerOverflow(int timer);
    void sampleEvent();
    bool timerUsedForFifo(int timer) const;

    // Output ring (stereo interleaved)
    static constexpr u32 kRing = 8192;  // stereo frames
    s16 ring[kRing * 2];
    u32 ringWrite = 0, ringRead = 0;
    u32 available() const { return ringWrite - ringRead; }
    u32 drain(s16* out, u32 maxFrames);

    u8 waveRam[2][16] = {};

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

private:
    SquareCh sq[2];
    WaveCh wave;
    NoiseCh noise;
    u32 seqStep = 0;     // frame sequencer step 0..7
    u32 seqCycles = 0;   // cycles accumulated toward next 512 Hz tick
    bool master = false;

    struct Fifo {
        s8 data[32];
        u32 r = 0, w = 0, count = 0;
        s8 cur = 0;
    } fifo[2];

    void triggerSquare(int i);
    void triggerWave();
    void triggerNoise();
    void clockLength();
    void clockSweep();
    void clockEnvelope();
    u32 sweepCalc();
    void fifoReset(int ch);
    s32 squareOut(int i, u32 cycles);
    s32 waveOut(u32 cycles);
    s32 noiseOut(u32 cycles);
};

}  // namespace retra
