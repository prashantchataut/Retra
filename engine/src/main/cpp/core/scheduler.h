// Retra GBA core - tiny event scheduler (fixed event slots, linear min scan).
#pragma once

#include "common.h"

namespace retra {

enum EventId : int {
    EV_HBLANK = 0,    // H-draw ends (HBlank flag/IRQ/DMA, render line)
    EV_LINE_END,      // line ends, VCOUNT++
    EV_TIMER0,
    EV_TIMER1,
    EV_TIMER2,
    EV_TIMER3,
    EV_APU_SAMPLE,    // produce one output sample
    EV_RESERVED,      // unused slot (keeps the save-state layout stable)
    EV_COUNT
};

constexpr u64 kNever = ~0ull;

struct Scheduler {
    u64 now = 0;
    u64 next = kNever;
    u64 when[EV_COUNT];

    Scheduler() { clear(); }
    void clear() {
        for (auto& w : when) w = kNever;
        next = kNever;
        now = 0;
    }
    RETRA_INLINE void schedule(int ev, u64 t) {
        when[ev] = t;
        if (t < next) next = t;
    }
    void cancel(int ev) {
        when[ev] = kNever;
        recompute();
    }
    void recompute() {
        u64 m = kNever;
        for (u64 w : when) m = w < m ? w : m;
        next = m;
    }
};

}  // namespace retra
