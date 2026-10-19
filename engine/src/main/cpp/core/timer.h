// Retra GBA core - timers 0-3.
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

class Timers {
public:
    explicit Timers(Gba* gba) : g(gba) {}
    void reset();

    u16 readCounter(int i);
    void writeReload(int i, u16 v) { t[i].reload = v; }
    void writeControl(int i, u16 v);
    void onOverflowEvent(int i);
    void reschedule();  // re-evaluate which timers need events (e.g. after SOUNDCNT_H change)

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

    struct Timer {
        u16 reload = 0;
        u16 control = 0;
        u16 counter = 0;   // value at 'start' (for running, non-cascade timers)
        u64 start = 0;     // scheduler time at which counter had value 'counter'
        u32 shift = 0;     // prescaler shift (0, 6, 8, 10)
        bool running = false;
        bool cascade = false;
        bool scheduled = false;
    } t[4];

private:
    void overflow(int i, u32 times);
    void schedule(int i);
    void syncCounter(int i);  // fold elapsed time into counter/start
    bool needsEvents(int i) const;
};

}  // namespace retra
