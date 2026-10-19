// Retra GBA core - barrel shifter (shared by ARM and THUMB).
#pragma once

#include "cpu.h"

namespace retra {

// Immediate shift amount (0..31) with the ARM encodings for #0 (LSR/ASR #32, RRX).
template <bool SETC>
RETRA_INLINE u32 Cpu::shiftImm(u32 type, u32 v, u32 amt, u32& carry) const {
    switch (type) {
        case 0:  // LSL
            if (amt == 0) return v;
            if (SETC) carry = (v >> (32 - amt)) & 1;
            return v << amt;
        case 1:  // LSR
            if (amt == 0) {
                if (SETC) carry = v >> 31;
                return 0;
            }
            if (SETC) carry = (v >> (amt - 1)) & 1;
            return v >> amt;
        case 2:  // ASR
            if (amt == 0) {
                if (SETC) carry = v >> 31;
                return (u32)((s32)v >> 31);
            }
            if (SETC) carry = ((u32)((s32)v >> (amt - 1))) & 1;
            return (u32)((s32)v >> amt);
        default:  // ROR / RRX
            if (amt == 0) {
                const u32 res = (fC << 31) | (v >> 1);
                if (SETC) carry = v & 1;
                return res;
            }
            if (SETC) carry = (v >> (amt - 1)) & 1;
            return ror32(v, amt);
    }
}

// Register specified shift amount (bottom byte of Rs).
template <bool SETC>
RETRA_INLINE u32 Cpu::shiftReg(u32 type, u32 v, u32 amt, u32& carry) const {
    if (amt == 0) return v;
    switch (type) {
        case 0:  // LSL
            if (amt < 32) {
                if (SETC) carry = (v >> (32 - amt)) & 1;
                return v << amt;
            }
            if (SETC) carry = amt == 32 ? (v & 1) : 0;
            return 0;
        case 1:  // LSR
            if (amt < 32) {
                if (SETC) carry = (v >> (amt - 1)) & 1;
                return v >> amt;
            }
            if (SETC) carry = amt == 32 ? (v >> 31) : 0;
            return 0;
        case 2:  // ASR
            if (amt < 32) {
                if (SETC) carry = ((u32)((s32)v >> (amt - 1))) & 1;
                return (u32)((s32)v >> amt);
            }
            if (SETC) carry = v >> 31;
            return (u32)((s32)v >> 31);
        default: {  // ROR
            const u32 a = amt & 31;
            if (a == 0) {
                if (SETC) carry = v >> 31;
                return v;
            }
            if (SETC) carry = (v >> (a - 1)) & 1;
            return ror32(v, a);
        }
    }
}

}  // namespace retra
