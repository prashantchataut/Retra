// Retra GBA core - GPIO + Seiko S-3511 RTC (3-wire serial: SCK=bit0, SIO=bit1, CS=bit2).
// Commands are sent MSB first as 0110 CCC R; data bytes travel LSB first.
#include "rtc.h"

#include <ctime>

#include "gba.h"
#include "state.h"

namespace retra {

static u8 toBcd(int v) { return (u8)(((v / 10) % 10) << 4 | (v % 10)); }
static int fromBcd(u8 v) { return ((v >> 4) & 15) * 10 + (v & 15); }

void Rtc::reset() {
    pinsOut = 0;
    direction = 0;
    control = 0;
    sioOut = 0;
    status = 0x40;
    sck = cs = false;
    state = 0;
    bitPos = bytePos = byteCount = 0;
    cmd = cur = 0;
    std::memset(buf, 0, sizeof(buf));
}

u16 Rtc::read(u32 addr) const {
    switch (addr & 0xF) {
        case 0x4: {
            // Input pins (direction bit 0) read the device; outputs read back the latch.
            u8 v = pinsOut & direction;
            if (!(direction & 2)) v |= (u8)(sioOut << 1);
            return v & 0xF;
        }
        case 0x6: return direction & 0xF;
        case 0x8: return control & 1;
        default: return 0;
    }
}

void Rtc::write(u32 addr, u16 v) {
    switch (addr & 0xF) {
        case 0x4: writePins((u8)(v & 0xF)); break;
        case 0x6: direction = (u8)(v & 0xF); break;
        case 0x8: control = (u8)(v & 1); break;
        default: break;
    }
}

void Rtc::latchTime() {
    std::time_t t = std::time(nullptr) + (std::time_t)(userOffset + gameOffset);
    std::tm tmv{};
#if defined(_WIN32)
    localtime_s(&tmv, &t);
#else
    localtime_r(&t, &tmv);
#endif
    buf[0] = toBcd(tmv.tm_year % 100);
    buf[1] = toBcd(tmv.tm_mon + 1);
    buf[2] = toBcd(tmv.tm_mday);
    buf[3] = toBcd(tmv.tm_wday);
    int hour = tmv.tm_hour;
    if (status & 0x40) {
        buf[4] = toBcd(hour);
    } else {
        buf[4] = toBcd(hour % 12);
        if (hour >= 12) buf[4] |= 0x40;
    }
    if (hour >= 12) buf[4] |= 0x80;  // AM/PM flag (games mask it out)
    buf[5] = toBcd(tmv.tm_min);
    buf[6] = toBcd(tmv.tm_sec);
}

void Rtc::beginCommand() {
    // cmd: 0110 CCC R (as received MSB first). Some software sends it reversed.
    u8 c = cmd;
    if ((c & 0xF0) != 0x60) {
        u8 r = 0;
        for (int i = 0; i < 8; i++)
            if (c & (1 << i)) r |= (u8)(0x80 >> i);
        c = r;
    }
    const u32 code = (c >> 1) & 7;
    const bool rd = c & 1;
    bytePos = 0;
    bitPos = 0;
    switch (code) {
        case 0:  // reset
            status = 0;
            gameOffset = 0;
            state = 0;
            byteCount = 0;
            return;
        case 1: byteCount = 1; if (rd) buf[0] = status; break;   // status register
        case 2: byteCount = 7; if (rd) latchTime(); break;       // date + time
        case 3:                                                  // time only
            byteCount = 3;
            if (rd) {
                latchTime();
                buf[0] = buf[4];
                buf[1] = buf[5];
                buf[2] = buf[6];
            }
            break;
        case 6: byteCount = 1; if (rd) buf[0] = 0; break;  // IRQ/alarm (unused)
        default: byteCount = 0; break;
    }
    cmd = c;
    state = byteCount == 0 ? 0 : (rd ? 2 : 3);
}

void Rtc::applyWrite() {
    const u32 code = (cmd >> 1) & 7;
    if (code == 1) {
        status = buf[0] & 0x6A;
    } else if (code == 2 || code == 3) {
        // Game set the clock: remember the difference to the host clock.
        std::time_t now = std::time(nullptr) + (std::time_t)userOffset;
        std::tm tmv{};
#if defined(_WIN32)
        localtime_s(&tmv, &now);
#else
        localtime_r(&now, &tmv);
#endif
        std::tm want = tmv;
        if (code == 2) {
            want.tm_year = 100 + fromBcd(buf[0]);
            want.tm_mon = fromBcd(buf[1] & 0x1F) - 1;
            want.tm_mday = fromBcd(buf[2] & 0x3F);
            want.tm_hour = fromBcd(buf[4] & 0x3F);
            want.tm_min = fromBcd(buf[5] & 0x7F);
            want.tm_sec = fromBcd(buf[6] & 0x7F);
        } else {
            want.tm_hour = fromBcd(buf[0] & 0x3F);
            want.tm_min = fromBcd(buf[1] & 0x7F);
            want.tm_sec = fromBcd(buf[2] & 0x7F);
        }
        want.tm_isdst = -1;
        std::time_t wt = std::mktime(&want);
        if (wt != (std::time_t)-1) gameOffset = (s64)(wt - now);
    }
}

void Rtc::writePins(u8 v) {
    // Only pins configured as outputs are driven by the GBA.
    pinsOut = (u8)((pinsOut & ~direction) | (v & direction));
    const bool nsck = pinsOut & 1;
    const bool nsio = (pinsOut >> 1) & 1;
    const bool ncs = (pinsOut >> 2) & 1;

    if (!ncs) {
        state = 0;
        bitPos = 0;
        cmd = 0;
        cs = false;
        sck = nsck;
        return;
    }
    if (!cs) {
        // chip select rising edge: start receiving a command
        cs = true;
        state = 1;
        bitPos = 0;
        cmd = 0;
        sck = nsck;
        return;
    }
    const bool rising = !sck && nsck;
    sck = nsck;
    if (!rising) return;

    switch (state) {
        case 1:  // command byte, MSB first
            cmd = (u8)((cmd << 1) | (nsio ? 1 : 0));
            if (++bitPos == 8) beginCommand();
            break;
        case 2: {  // reading: present next bit (LSB first)
            if (bytePos < byteCount) {
                sioOut = (buf[bytePos] >> bitPos) & 1;
                if (++bitPos == 8) {
                    bitPos = 0;
                    bytePos++;
                }
            } else {
                sioOut = 0;
            }
            break;
        }
        case 3:  // writing: receive bits LSB first
            if (bytePos < byteCount) {
                if (bitPos == 0) cur = 0;
                cur |= (u8)((nsio ? 1 : 0) << bitPos);
                if (++bitPos == 8) {
                    buf[bytePos++] = cur;
                    bitPos = 0;
                    if (bytePos == byteCount) {
                        applyWrite();
                        state = 0;
                    }
                }
            }
            break;
        default: break;
    }
}

void Rtc::serialize(StateWriter& w) const {
    w.tag("RTC0");
    w.pod(enabled);
    w.pod(gameOffset);
    w.pod(pinsOut);
    w.pod(direction);
    w.pod(control);
    w.pod(sioOut);
    w.pod(status);
    w.pod(sck);
    w.pod(cs);
    w.pod(state);
    w.pod(bitPos);
    w.pod(bytePos);
    w.pod(byteCount);
    w.pod(cmd);
    w.pod(cur);
    w.pod(buf);
}

void Rtc::deserialize(StateReader& rd) {
    rd.tag("RTC0");
    rd.pod(enabled);
    rd.pod(gameOffset);
    rd.pod(pinsOut);
    rd.pod(direction);
    rd.pod(control);
    rd.pod(sioOut);
    rd.pod(status);
    rd.pod(sck);
    rd.pod(cs);
    rd.pod(state);
    rd.pod(bitPos);
    rd.pod(bytePos);
    rd.pod(byteCount);
    rd.pod(cmd);
    rd.pod(cur);
    rd.pod(buf);
    if (byteCount > 8) byteCount = 0;
    if (bytePos > 8) bytePos = 0;
    bitPos &= 7;
}

}  // namespace retra
