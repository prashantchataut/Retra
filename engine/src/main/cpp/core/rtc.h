// Retra GBA core - cartridge GPIO port + Seiko S-3511 real-time clock.
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

class Rtc {
public:
    explicit Rtc(Gba* gba) : g(gba) {}
    void reset();

    bool enabled = false;
    s64 userOffset = 0;    // seconds, set by host app
    s64 gameOffset = 0;    // seconds, from game writes to the clock

    // GPIO registers (ROM 0x080000C4/C6/C8)
    void write(u32 addr, u16 v);
    u16 read(u32 addr) const;
    bool readable() const { return (control & 1) != 0; }

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

    u8 pinsOut = 0;   // last value written by the GBA
    u8 direction = 0;
    u8 control = 0;
    u8 sioOut = 0;    // bit driven by the RTC on SIO

private:
    void writePins(u8 v);
    void beginCommand();
    void latchTime();
    void applyWrite();

    u8 status = 0x40;  // 24-hour mode
    bool sck = false, cs = false;
    u32 state = 0;  // 0 idle, 1 receiving command, 2 reading, 3 writing
    u32 bitPos = 0, bytePos = 0, byteCount = 0;
    u8 cmd = 0;
    u8 cur = 0;
    u8 buf[8] = {};
};

}  // namespace retra
