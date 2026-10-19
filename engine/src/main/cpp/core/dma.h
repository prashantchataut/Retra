// Retra GBA core - DMA controller (4 channels).
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

enum DmaTiming : u32 { DMA_NOW = 0, DMA_VBLANK = 1, DMA_HBLANK = 2, DMA_SPECIAL = 3 };

class Dma {
public:
    explicit Dma(Gba* gba) : g(gba) {}
    void reset();

    void writeControl(int ch, u16 value);  // DMAxCNT_H written
    void trigger(DmaTiming timing);         // VBlank / HBlank event
    void requestFifo(int fifo);             // sound FIFO A/B wants data
    void videoCapture(u32 vcount);          // DMA3 special timing
    u16 readControl(int ch) const { return c[ch].cnt; }

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

    struct Channel {
        u32 src = 0, dst = 0;      // internal running addresses
        u32 count = 0;             // internal count
        u16 cnt = 0;               // control register
        bool fifoMode = false;
    } c[4];

    bool running = false;
    u32 pending = 0;  // bitmask of channels to run

private:
    void start(int ch);
    void runPending();
    void transfer(int ch);
};

}  // namespace retra
