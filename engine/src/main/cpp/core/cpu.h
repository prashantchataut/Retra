// Retra GBA core - ARM7TDMI interpreter.
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

enum CpuMode : u32 {
    MODE_USR = 0x10,
    MODE_FIQ = 0x11,
    MODE_IRQ = 0x12,
    MODE_SVC = 0x13,
    MODE_ABT = 0x17,
    MODE_UND = 0x1B,
    MODE_SYS = 0x1F,
};

// Trap opcode used by the replacement BIOS to call into the HLE implementation:
// ARM "permanently undefined" space 0xE7F000F0 | (n & 0xF) | ((n >> 4) << 8).
constexpr u32 kHleTrapMask = 0xFFF000F0;
constexpr u32 kHleTrapBase = 0xE7F000F0;

class Cpu {
public:
    explicit Cpu(Gba* gba) : g(gba) {}

    void reset();
    // Execute until the scheduler's next event (or halted).
    void run();

    // Register file. r[15] = address of current instruction + 8 (ARM) / + 4 (THUMB)
    // while executing; between instructions it is next instruction + 8 / + 4.
    u32 r[16] = {};
    // Flags kept unpacked for speed (each 0 or 1).
    u32 fN = 0, fZ = 0, fC = 0, fV = 0;
    u32 mode = MODE_SYS;
    bool thumb = false;
    bool irqDisable = true;  // CPSR.I
    bool fiqDisable = true;  // CPSR.F
    u32 spsr = 0;            // SPSR of current mode (0 in usr/sys)

    // Banked storage (index via bankIndex()).
    u32 bankR13[6] = {}, bankR14[6] = {}, bankSpsr[6] = {};
    u32 bankFiq[5] = {}, bankUsr[5] = {};  // r8-r12

    // Two-stage prefetch: pipe[0] = opcode to execute next, pipe[1] = the one after.
    u32 pipe[2] = {};
    void flushPipeline();  // refill from r[15] (untimed), after external PC changes

    bool halted = false;
    bool irqLine = false;  // set by Gba when (IE & IF) && IME

    u32 cpsr() const {
        return (fN << 31) | (fZ << 30) | (fC << 29) | (fV << 28) | (irqDisable ? 0x80 : 0) |
               (fiqDisable ? 0x40 : 0) | (thumb ? 0x20 : 0) | mode;
    }
    void setCpsr(u32 v);  // full write including mode switch
    void setFlags(u32 v) {
        fN = v >> 31;
        fZ = (v >> 30) & 1;
        fC = (v >> 29) & 1;
        fV = (v >> 28) & 1;
    }
    void switchMode(u32 newMode);
    bool hasSpsr() const { return mode != MODE_USR && mode != MODE_SYS; }

    RETRA_INLINE void branchTo(u32 addr);  // jump; pipeline refill accounted
    void raiseIrq();
    void raiseSwi();
    void raiseUndefined();

    // Open-bus value (what the pipeline last fetched).
    u32 openBus();

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

    // --- execution helpers used by the instruction handlers
    void stepArm();
    void stepThumb();
    RETRA_INLINE bool condPassed(u32 cond) const {
        return (kCondTable[cond] >> ((fN << 3) | (fZ << 2) | (fC << 1) | fV)) & 1;
    }
    static const u16 kCondTable[16];

    // Barrel shifter helpers
    template <bool SETC>
    RETRA_INLINE u32 shiftImm(u32 type, u32 v, u32 amt, u32& carry) const;
    template <bool SETC>
    RETRA_INLINE u32 shiftReg(u32 type, u32 v, u32 amt, u32& carry) const;

    RETRA_INLINE void setNZ(u32 v) {
        fN = v >> 31;
        fZ = v == 0;
    }
    RETRA_INLINE u32 addFlags(u32 a, u32 b) {
        u32 res = a + b;
        fN = res >> 31;
        fZ = res == 0;
        fC = res < a;
        fV = ((~(a ^ b) & (a ^ res)) >> 31);
        return res;
    }
    RETRA_INLINE u32 adcFlags(u32 a, u32 b, u32 c) {
        u64 res64 = (u64)a + b + c;
        u32 res = (u32)res64;
        fN = res >> 31;
        fZ = res == 0;
        fC = (u32)(res64 >> 32);
        fV = ((~(a ^ b) & (a ^ res)) >> 31);
        return res;
    }
    RETRA_INLINE u32 subFlags(u32 a, u32 b) {
        u32 res = a - b;
        fN = res >> 31;
        fZ = res == 0;
        fC = a >= b;
        fV = (((a ^ b) & (a ^ res)) >> 31);
        return res;
    }
    RETRA_INLINE u32 sbcFlags(u32 a, u32 b, u32 c) {
        // a - b - !c
        u32 nc = c ^ 1;
        u32 res = a - b - nc;
        fN = res >> 31;
        fZ = res == 0;
        fC = (u64)a >= (u64)b + nc;
        fV = (((a ^ b) & (a ^ res)) >> 31);
        return res;
    }

    // Multiply internal cycles from multiplier operand.
    static RETRA_INLINE u32 mulCycles(u32 rs, bool signedMode) {
        if (signedMode) {
            if ((rs & 0xFFFFFF00) == 0 || (rs & 0xFFFFFF00) == 0xFFFFFF00) return 1;
            if ((rs & 0xFFFF0000) == 0 || (rs & 0xFFFF0000) == 0xFFFF0000) return 2;
            if ((rs & 0xFF000000) == 0 || (rs & 0xFF000000) == 0xFF000000) return 3;
            return 4;
        }
        if ((rs & 0xFFFFFF00) == 0) return 1;
        if ((rs & 0xFFFF0000) == 0) return 2;
        if ((rs & 0xFF000000) == 0) return 3;
        return 4;
    }

    RETRA_INLINE void idle(u32 n);  // internal cycles

    // Idle-loop detection: called on short backward branches. If an iteration of the loop
    // leaves registers, flags and memory unchanged, nothing can change until the next
    // scheduled event, so time is fast-forwarded to it.
    void idleCheck();
    u32 idlePc = 0xFFFFFFFF;
    u32 idleFlags = 0;
    u32 idleWrites = 0;
    u32 idleRegs[15] = {};
    u32 idleFails = 0, idleBackoff = 0, idleEpoch = 0;

private:
    static int bankIndex(u32 m);
};

}  // namespace retra
