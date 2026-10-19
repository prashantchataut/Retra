// Retra GBA core - DMA controller.
#include "dma.h"

#include "bus_inl.h"
#include "gba.h"
#include "state.h"

namespace retra {

static constexpr u32 kSrcMask[4] = {0x07FFFFFF, 0x0FFFFFFF, 0x0FFFFFFF, 0x0FFFFFFF};
static constexpr u32 kDstMask[4] = {0x07FFFFFF, 0x07FFFFFF, 0x07FFFFFF, 0x0FFFFFFF};

void Dma::reset() {
    for (auto& ch : c) ch = Channel{};
    running = false;
    pending = 0;
}

void Dma::writeControl(int ch, u16 value) {
    Channel& d = c[ch];
    const bool was = d.cnt & 0x8000;
    d.cnt = value & (ch == 3 ? 0xFFE0 : 0xF7E0);
    g->bus.setIo16(0xBA + ch * 12, d.cnt);
    if (!(d.cnt & 0x8000)) {
        pending &= ~(1u << ch);
        return;
    }
    if (!was) start(ch);
}

void Dma::start(int ch) {
    Channel& d = c[ch];
    Bus& b = g->bus;
    const u32 base = 0xB0 + ch * 12;
    d.src = (b.io16(base) | ((u32)b.io16(base + 2) << 16)) & kSrcMask[ch];
    d.dst = (b.io16(base + 4) | ((u32)b.io16(base + 6) << 16)) & kDstMask[ch];
    u32 count = b.io16(base + 8);
    if (ch < 3) count &= 0x3FFF;
    if (count == 0) count = ch == 3 ? 0x10000 : 0x4000;
    d.count = count;
    const u32 timing = (d.cnt >> 12) & 3;
    d.fifoMode = timing == DMA_SPECIAL && (ch == 1 || ch == 2);
    if (timing == DMA_NOW) {
        pending |= 1u << ch;
        runPending();
    }
}

void Dma::trigger(DmaTiming timing) {
    bool any = false;
    for (int ch = 0; ch < 4; ch++) {
        const Channel& d = c[ch];
        if ((d.cnt & 0x8000) && ((d.cnt >> 12) & 3) == timing) {
            pending |= 1u << ch;
            any = true;
        }
    }
    if (any) runPending();
}

void Dma::requestFifo(int fifo) {
    const u32 addr = 0x040000A0 + fifo * 4;
    for (int ch = 1; ch <= 2; ch++) {
        const Channel& d = c[ch];
        if ((d.cnt & 0x8000) && d.fifoMode && d.dst == addr) {
            pending |= 1u << ch;
            runPending();
            return;
        }
    }
}

void Dma::videoCapture(u32 vcount) {
    Channel& d = c[3];
    if (!(d.cnt & 0x8000) || ((d.cnt >> 12) & 3) != DMA_SPECIAL) return;
    if (vcount == 161) {
        d.cnt &= ~0x8000;
        g->bus.setIo16(0xDE, d.cnt);
        return;
    }
    pending |= 1u << 3;
    runPending();
}

void Dma::runPending() {
    if (running) return;
    running = true;
    while (pending) {
        const int ch = __builtin_ctz(pending);
        pending &= ~(1u << ch);
        if (c[ch].cnt & 0x8000) transfer(ch);
    }
    running = false;
}

void Dma::transfer(int ch) {
    Channel& d = c[ch];
    Bus& b = g->bus;
    const u32 timing = (d.cnt >> 12) & 3;
    const u32 dstCtl = (d.cnt >> 5) & 3;
    u32 srcCtl = (d.cnt >> 7) & 3;
    bool word = d.cnt & 0x400;
    u32 count = d.count;
    if (d.fifoMode) {
        word = true;
        count = 4;
    }
    const u32 unit = word ? 4 : 2;
    if (d.src >= 0x08000000 && d.src < 0x0E000000) srcCtl = 0;  // ROM: always increments
    const s32 srcStep = srcCtl == 0 ? (s32)unit : (srcCtl == 1 ? -(s32)unit : 0);
    s32 dstStep = (dstCtl == 0 || dstCtl == 3) ? (s32)unit : (dstCtl == 1 ? -(s32)unit : 0);
    if (d.fifoMode) dstStep = 0;

    if (ch == 3 && (d.dst >> 24) == 0xD && g->save.isEeprom()) g->save.eepromDmaHint(count);

    u32 src = d.src & ~(unit - 1), dst = d.dst & ~(unit - 1);
    for (u32 i = 0; i < count; i++) {
        const bool seq = i != 0;
        if (word) {
            u32 v;
            if (src >= 0x02000000) {
                v = b.read<u32>(src, seq);
                b.dmaLatch = v;
            } else {
                g->sched.now += 1;
                v = b.dmaLatch;
            }
            b.write<u32>(dst, v, seq);
        } else {
            u16 v;
            if (src >= 0x02000000) {
                v = b.read<u16>(src, seq);
                b.dmaLatch = v | ((u32)v << 16);
            } else {
                g->sched.now += 1;
                v = (u16)(b.dmaLatch >> ((dst & 2) * 8));
            }
            b.write<u16>(dst, v, seq);
        }
        src += (u32)srcStep;
        dst += (u32)dstStep;
    }
    d.src = src & kSrcMask[ch];
    d.dst = dst & kDstMask[ch];

    if (d.cnt & 0x4000) g->raiseIrq((u16)(IRQ_DMA0 << ch));

    if ((d.cnt & 0x200) && timing != DMA_NOW) {
        // repeat: reload count (and destination when "increment/reload")
        const u32 base = 0xB0 + ch * 12;
        u32 cnt = b.io16(base + 8);
        if (ch < 3) cnt &= 0x3FFF;
        if (cnt == 0) cnt = ch == 3 ? 0x10000 : 0x4000;
        d.count = cnt;
        if (dstCtl == 3) d.dst = (b.io16(base + 4) | ((u32)b.io16(base + 6) << 16)) & kDstMask[ch];
    } else {
        d.cnt &= ~0x8000;
        b.setIo16(0xBA + ch * 12, d.cnt);
    }
}

void Dma::serialize(StateWriter& w) const {
    w.tag("DMA0");
    w.pod(c);
    w.pod(pending);
}

void Dma::deserialize(StateReader& rd) {
    rd.tag("DMA0");
    rd.pod(c);
    rd.pod(pending);
    running = false;
}

}  // namespace retra
