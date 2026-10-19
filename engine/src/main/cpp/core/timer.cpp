// Retra GBA core - timers. Counters are evaluated lazily from the scheduler clock;
// overflow events are only scheduled when something observes them (IRQ, cascade, sound FIFO).
#include "timer.h"

#include "gba.h"
#include "state.h"

namespace retra {

static constexpr u32 kShift[4] = {0, 6, 8, 10};

void Timers::reset() {
    for (auto& tm : t) tm = Timer{};
}

void Timers::syncCounter(int i) {
    Timer& tm = t[i];
    if (!tm.running) return;
    const u64 now = g->sched.now;
    if (now <= tm.start) return;
    const u64 ticks = (now - tm.start) >> tm.shift;
    if (!ticks) return;
    u64 v = (u64)tm.counter + ticks;
    if (v >= 0x10000) {
        const u64 period = 0x10000 - tm.reload;
        v = tm.reload + (v - 0x10000) % period;
    }
    tm.counter = (u16)v;
    tm.start += ticks << tm.shift;
}

u16 Timers::readCounter(int i) {
    // Pure evaluation: never folds a pending overflow (its event must still fire).
    const Timer& tm = t[i];
    if (!tm.running) return tm.counter;
    const u64 now = g->sched.now;
    if (now <= tm.start) return tm.counter;
    const u64 v = (u64)tm.counter + ((now - tm.start) >> tm.shift);
    if (v < 0x10000) return (u16)v;
    return (u16)(tm.reload + (v - 0x10000) % (0x10000 - tm.reload));
}

bool Timers::needsEvents(int i) const {
    const Timer& tm = t[i];
    if (!tm.running) return false;
    if (tm.control & 0x40) return true;
    if (i < 3 && (t[i + 1].control & 0x80) && t[i + 1].cascade) return true;
    return g->apu.timerUsedForFifo(i);
}

void Timers::schedule(int i) {
    Timer& tm = t[i];
    if (needsEvents(i)) {
        syncCounter(i);
        const u64 when = tm.start + ((u64)(0x10000 - tm.counter) << tm.shift);
        g->sched.schedule(EV_TIMER0 + i, when);
        tm.scheduled = true;
    } else if (tm.scheduled) {
        tm.scheduled = false;
        g->sched.cancel(EV_TIMER0 + i);
    }
}

void Timers::reschedule() {
    for (int i = 0; i < 4; i++) schedule(i);
}

void Timers::writeControl(int i, u16 v) {
    Timer& tm = t[i];
    syncCounter(i);
    const bool was = tm.control & 0x80;
    tm.control = v & 0xC7;
    tm.shift = kShift[v & 3];
    tm.cascade = i > 0 && (v & 4);
    const bool enabled = v & 0x80;
    if (!was && enabled) {
        tm.counter = tm.reload;
        tm.start = g->sched.now + 2;  // timers start counting two cycles after enable
    } else if (enabled && tm.running && tm.cascade) {
        // switched to cascade: counter freezes at current value
    } else if (enabled && !tm.running && !tm.cascade) {
        tm.start = g->sched.now;  // switched from cascade to free running
    }
    tm.running = enabled && !tm.cascade;
    reschedule();
}

void Timers::onOverflowEvent(int i) {
    Timer& tm = t[i];
    tm.scheduled = false;
    if (!tm.running) return;
    const u64 now = g->sched.now;
    const u64 ticks = now > tm.start ? (now - tm.start) >> tm.shift : 0;
    const u64 total = (u64)tm.counter + ticks;
    if (total >= 0x10000) {
        const u64 period = 0x10000 - tm.reload;
        const u64 over = total - 0x10000;
        const u64 times = 1 + over / period;
        tm.counter = (u16)(tm.reload + over % period);
        tm.start += ticks << tm.shift;
        // Deliver each overflow (bounded: only a few can be pending when the event fires).
        for (u64 k = 0; k < times && k < 8; k++) overflow(i, 1);
    }
    schedule(i);
}

void Timers::overflow(int i, u32 times) {
    (void)times;
    Timer& tm = t[i];
    if (tm.control & 0x40) g->raiseIrq((u16)(IRQ_TIMER0 << i));
    if (i < 2) g->apu.onTimerOverflow(i);
    if (i < 3) {
        Timer& nx = t[i + 1];
        if ((nx.control & 0x80) && nx.cascade) {
            if (nx.counter == 0xFFFF) {
                nx.counter = nx.reload;
                overflow(i + 1, 1);
            } else {
                nx.counter++;
            }
        }
    }
}

void Timers::serialize(StateWriter& w) const {
    w.tag("TMR0");
    w.pod(t);
}

void Timers::deserialize(StateReader& rd) {
    rd.tag("TMR0");
    rd.pod(t);
    for (int i = 0; i < 4; i++) {
        t[i].shift = kShift[t[i].control & 3];
        t[i].cascade = i > 0 && (t[i].control & 4);
        t[i].running = (t[i].control & 0x80) && !t[i].cascade;
    }
}

}  // namespace retra
