// Retra GBA core - THUMB (16-bit) instruction set.
#include <array>
#include <utility>

#include "bus_inl.h"
#include "cpu.h"
#include "cpu_shift.h"
#include "gba.h"

namespace retra {

using ThumbHandler = void (*)(Cpu&, u16);

// Format 1: move shifted register
template <u32 TYPE>
static void thShiftImm(Cpu& c, u16 op) {
    const u32 rd = op & 7, rs = (op >> 3) & 7, amt = (op >> 6) & 31;
    u32 carry = c.fC;
    const u32 res = c.shiftImm<true>(TYPE, c.r[rs], amt, carry);
    c.fC = carry;
    c.setNZ(res);
    c.r[rd] = res;
}

// Format 2: add/subtract
template <bool IMM, bool SUB>
static void thAddSub(Cpu& c, u16 op) {
    const u32 rd = op & 7, rs = (op >> 3) & 7, rn = (op >> 6) & 7;
    const u32 v = IMM ? rn : c.r[rn];
    c.r[rd] = SUB ? c.subFlags(c.r[rs], v) : c.addFlags(c.r[rs], v);
}

// Format 3: move/compare/add/subtract immediate
template <u32 OP>
static void thImm(Cpu& c, u16 op) {
    const u32 rd = (op >> 8) & 7, imm = op & 0xFF;
    switch (OP) {
        case 0: c.r[rd] = imm; c.setNZ(imm); break;
        case 1: c.subFlags(c.r[rd], imm); break;
        case 2: c.r[rd] = c.addFlags(c.r[rd], imm); break;
        default: c.r[rd] = c.subFlags(c.r[rd], imm); break;
    }
}

// Format 4: ALU operations
template <u32 OP>
static void thAlu(Cpu& c, u16 op) {
    const u32 rd = op & 7, rs = (op >> 3) & 7;
    const u32 a = c.r[rd], b = c.r[rs];
    u32 carry = c.fC;
    switch (OP) {
        case 0x0: c.r[rd] = a & b; c.setNZ(c.r[rd]); break;
        case 0x1: c.r[rd] = a ^ b; c.setNZ(c.r[rd]); break;
        case 0x2: c.r[rd] = c.shiftReg<true>(0, a, b & 0xFF, carry); c.fC = carry; c.setNZ(c.r[rd]); c.idle(1); break;
        case 0x3: c.r[rd] = c.shiftReg<true>(1, a, b & 0xFF, carry); c.fC = carry; c.setNZ(c.r[rd]); c.idle(1); break;
        case 0x4: c.r[rd] = c.shiftReg<true>(2, a, b & 0xFF, carry); c.fC = carry; c.setNZ(c.r[rd]); c.idle(1); break;
        case 0x5: c.r[rd] = c.adcFlags(a, b, c.fC); break;
        case 0x6: c.r[rd] = c.sbcFlags(a, b, c.fC); break;
        case 0x7: c.r[rd] = c.shiftReg<true>(3, a, b & 0xFF, carry); c.fC = carry; c.setNZ(c.r[rd]); c.idle(1); break;
        case 0x8: c.setNZ(a & b); break;
        case 0x9: c.r[rd] = c.subFlags(0, b); break;
        case 0xA: c.subFlags(a, b); break;
        case 0xB: c.addFlags(a, b); break;
        case 0xC: c.r[rd] = a | b; c.setNZ(c.r[rd]); break;
        case 0xD:
            c.idle(Cpu::mulCycles(a, true));
            c.r[rd] = a * b;
            c.setNZ(c.r[rd]);
            break;
        case 0xE: c.r[rd] = a & ~b; c.setNZ(c.r[rd]); break;
        default: c.r[rd] = ~b; c.setNZ(c.r[rd]); break;
    }
}

// Format 5: hi register operations / BX
template <u32 OP>
static void thHiReg(Cpu& c, u16 op) {
    const u32 rd = (op & 7) | ((op >> 4) & 8);
    const u32 rs = (op >> 3) & 15;
    const u32 v = c.r[rs];
    switch (OP) {
        case 0: {
            const u32 res = c.r[rd] + v;
            if (rd == 15)
                c.branchTo(res);
            else
                c.r[rd] = res;
            break;
        }
        case 1: c.subFlags(c.r[rd], v); break;
        case 2:
            if (rd == 15)
                c.branchTo(v);
            else
                c.r[rd] = v;
            break;
        default:
            c.thumb = v & 1;
            c.branchTo(v);
            break;
    }
}

// Format 6: PC-relative load
static void thLdrPc(Cpu& c, u16 op) {
    const u32 rd = (op >> 8) & 7;
    const u32 addr = (c.r[15] & ~2u) + ((op & 0xFF) << 2);
    c.r[rd] = c.g->bus.read<u32>(addr);
    c.idle(1);
}

// Format 7/8: load/store with register offset
template <u32 OP>
static void thLdStReg(Cpu& c, u16 op) {
    const u32 rd = op & 7, rb = (op >> 3) & 7, ro = (op >> 6) & 7;
    const u32 addr = c.r[rb] + c.r[ro];
    Bus& b = c.g->bus;
    switch (OP) {
        case 0: b.write<u32>(addr, c.r[rd]); break;                              // STR
        case 1: b.write<u16>(addr, (u16)c.r[rd]); break;                         // STRH
        case 2: b.write<u8>(addr, (u8)c.r[rd]); break;                           // STRB
        case 3: c.r[rd] = (u32)(s32)(s8)b.read<u8>(addr); c.idle(1); break;      // LDSB
        case 4: c.r[rd] = ror32(b.read<u32>(addr), (addr & 3) * 8); c.idle(1); break;  // LDR
        case 5: c.r[rd] = ror32(b.read<u16>(addr), (addr & 1) * 8); c.idle(1); break;  // LDRH
        case 6: c.r[rd] = b.read<u8>(addr); c.idle(1); break;                    // LDRB
        default:                                                                 // LDSH
            if (addr & 1)
                c.r[rd] = (u32)(s32)(s8)b.read<u8>(addr);
            else
                c.r[rd] = (u32)(s32)(s16)b.read<u16>(addr);
            c.idle(1);
            break;
    }
}

// Format 9: load/store with immediate offset
template <bool BYTE, bool LOAD>
static void thLdStImm(Cpu& c, u16 op) {
    const u32 rd = op & 7, rb = (op >> 3) & 7, off = (op >> 6) & 31;
    Bus& b = c.g->bus;
    if (BYTE) {
        const u32 addr = c.r[rb] + off;
        if (LOAD) {
            c.r[rd] = b.read<u8>(addr);
            c.idle(1);
        } else {
            b.write<u8>(addr, (u8)c.r[rd]);
        }
    } else {
        const u32 addr = c.r[rb] + (off << 2);
        if (LOAD) {
            c.r[rd] = ror32(b.read<u32>(addr), (addr & 3) * 8);
            c.idle(1);
        } else {
            b.write<u32>(addr, c.r[rd]);
        }
    }
}

// Format 10: load/store halfword
template <bool LOAD>
static void thLdStH(Cpu& c, u16 op) {
    const u32 rd = op & 7, rb = (op >> 3) & 7, off = ((op >> 6) & 31) << 1;
    const u32 addr = c.r[rb] + off;
    Bus& b = c.g->bus;
    if (LOAD) {
        c.r[rd] = ror32(b.read<u16>(addr), (addr & 1) * 8);
        c.idle(1);
    } else {
        b.write<u16>(addr, (u16)c.r[rd]);
    }
}

// Format 11: SP-relative load/store
template <bool LOAD>
static void thLdStSp(Cpu& c, u16 op) {
    const u32 rd = (op >> 8) & 7;
    const u32 addr = c.r[13] + ((op & 0xFF) << 2);
    Bus& b = c.g->bus;
    if (LOAD) {
        c.r[rd] = ror32(b.read<u32>(addr), (addr & 3) * 8);
        c.idle(1);
    } else {
        b.write<u32>(addr, c.r[rd]);
    }
}

// Format 12: load address
template <bool SP>
static void thAddr(Cpu& c, u16 op) {
    const u32 rd = (op >> 8) & 7;
    const u32 imm = (op & 0xFF) << 2;
    c.r[rd] = (SP ? c.r[13] : (c.r[15] & ~2u)) + imm;
}

// Format 13: add offset to SP
static void thAddSp(Cpu& c, u16 op) {
    const u32 imm = (op & 0x7F) << 2;
    if (op & 0x80)
        c.r[13] -= imm;
    else
        c.r[13] += imm;
}

// Format 14: push/pop
template <bool POP, bool R>
static void thPushPop(Cpu& c, u16 op) {
    u32 list = op & 0xFF;
    Bus& b = c.g->bus;
    bool seq = false;
    if (POP) {
        u32 addr = c.r[13];
        if (list == 0 && !R) {
            // Empty list quirk: PC is loaded, SP += 0x40.
            const u32 v = b.read<u32>(addr);
            c.r[13] = addr + 0x40;
            c.branchTo(v);
            return;
        }
        for (u32 i = 0; i < 8; i++) {
            if (list & (1u << i)) {
                c.r[i] = b.read<u32>(addr, seq);
                seq = true;
                addr += 4;
            }
        }
        c.idle(1);
        if (R) {
            const u32 v = b.read<u32>(addr, seq);
            addr += 4;
            c.r[13] = addr;
            c.branchTo(v);
        } else {
            c.r[13] = addr;
        }
    } else {
        if (list == 0 && !R) {
            c.r[13] -= 0x40;
            b.write<u32>(c.r[13], c.r[15] + 2);
            return;
        }
        const u32 n = (u32)__builtin_popcount(list) + (R ? 1 : 0);
        u32 addr = c.r[13] - n * 4;
        c.r[13] = addr;
        for (u32 i = 0; i < 8; i++) {
            if (list & (1u << i)) {
                b.write<u32>(addr, c.r[i], seq);
                seq = true;
                addr += 4;
            }
        }
        if (R) b.write<u32>(addr, c.r[14], seq);
    }
}

// Format 15: multiple load/store
template <bool LOAD>
static void thLdmStm(Cpu& c, u16 op) {
    const u32 rb = (op >> 8) & 7;
    const u32 list = op & 0xFF;
    Bus& b = c.g->bus;
    u32 addr = c.r[rb];
    if (list == 0) {
        if (LOAD) {
            const u32 v = b.read<u32>(addr);
            c.r[rb] = addr + 0x40;
            c.branchTo(v);
        } else {
            b.write<u32>(addr, c.r[15] + 2);
            c.r[rb] = addr + 0x40;
        }
        return;
    }
    const u32 final = addr + (u32)__builtin_popcount(list) * 4;
    bool seq = false;
    if (LOAD) {
        c.r[rb] = final;
        for (u32 i = 0; i < 8; i++) {
            if (list & (1u << i)) {
                c.r[i] = b.read<u32>(addr, seq);
                seq = true;
                addr += 4;
            }
        }
        c.idle(1);
    } else {
        bool first = true;
        for (u32 i = 0; i < 8; i++) {
            if (list & (1u << i)) {
                b.write<u32>(addr, c.r[i], seq);
                seq = true;
                if (first) c.r[rb] = final;
                first = false;
                addr += 4;
            }
        }
    }
}

// Format 16: conditional branch
template <u32 COND>
static void thBcond(Cpu& c, u16 op) {
    if (!c.condPassed(COND)) return;
    const u32 off = (u32)(s32)(s8)(op & 0xFF) << 1;
    if ((s32)off < 0 && (s32)off >= -40) c.idleCheck();
    c.branchTo(c.r[15] + off);
}

static void thSwi(Cpu& c, u16) { c.raiseSwi(); }
static void thUndef(Cpu& c, u16) { c.raiseUndefined(); }

// Format 18: unconditional branch
static void thB(Cpu& c, u16 op) {
    const u32 off = (u32)((s32)((u32)(op & 0x7FF) << 21) >> 20);
    if ((s32)off < 0 && (s32)off >= -40) c.idleCheck();
    c.branchTo(c.r[15] + off);
}

// Format 19: long branch with link
static void thBlHi(Cpu& c, u16 op) {
    const u32 off = (u32)((s32)((u32)(op & 0x7FF) << 21) >> 9);
    c.r[14] = c.r[15] + off;
}

static void thBlLo(Cpu& c, u16 op) {
    const u32 next = (c.r[15] - 2) | 1;
    const u32 target = c.r[14] + ((op & 0x7FF) << 1);
    c.r[14] = next;
    c.branchTo(target);
}

// ---------------------------------------------------------------- decoding (idx = op >> 6)
enum ThumbKind : u32 {
    TK_SHIFT,
    TK_ADDSUB,
    TK_IMM,
    TK_ALU,
    TK_HIREG,
    TK_LDRPC,
    TK_LDST_REG,
    TK_LDST_IMM,
    TK_LDST_H,
    TK_LDST_SP,
    TK_ADDR,
    TK_ADDSP,
    TK_PUSHPOP,
    TK_LDMSTM,
    TK_BCOND,
    TK_SWI,
    TK_B,
    TK_BLHI,
    TK_BLLO,
    TK_UNDEF,
};

static constexpr u32 thumbKind(u32 idx) {
    const u32 top5 = idx >> 5;
    if (top5 < 3) return TK_SHIFT;
    if (top5 == 3) return TK_ADDSUB;
    if ((top5 >> 2) == 1) return TK_IMM;
    if ((idx >> 4) == 0x10) return TK_ALU;  // 010000
    if ((idx >> 4) == 0x11) return TK_HIREG;
    if (top5 == 0x09) return TK_LDRPC;
    if ((top5 >> 1) == 0x5) return TK_LDST_REG;
    if ((top5 >> 2) == 0x3) return TK_LDST_IMM;
    if ((top5 >> 1) == 0x8) return TK_LDST_H;
    if ((top5 >> 1) == 0x9) return TK_LDST_SP;
    if ((top5 >> 1) == 0xA) return TK_ADDR;
    if ((idx >> 2) == 0xB0) return TK_ADDSP;
    if ((top5 >> 1) == 0xB) {
        if (((idx >> 2) & 0x6) == 0x4) return TK_PUSHPOP;  // 1011x10x
        return TK_UNDEF;
    }
    if ((top5 >> 1) == 0xC) return TK_LDMSTM;
    if ((top5 >> 1) == 0xD) {
        const u32 cond = (idx >> 2) & 15;
        if (cond == 15) return TK_SWI;
        if (cond == 14) return TK_UNDEF;
        return TK_BCOND;
    }
    if (top5 == 0x1C) return TK_B;
    if (top5 == 0x1E) return TK_BLHI;
    if (top5 == 0x1F) return TK_BLLO;
    return TK_UNDEF;
}

static constexpr u32 thumbCanon(u32 idx) {
    switch (thumbKind(idx)) {
        case TK_SHIFT: return idx & ~31u;
        case TK_ADDSUB: return idx & ~7u;
        case TK_IMM: return idx & ~31u;
        case TK_ALU: return idx;
        case TK_HIREG: return idx & ~3u;
        case TK_LDRPC: return idx & ~31u;
        case TK_LDST_REG: return idx & ~7u;
        case TK_LDST_IMM: return idx & ~31u;
        case TK_LDST_H: return idx & ~31u;
        case TK_LDST_SP: return idx & ~31u;
        case TK_ADDR: return idx & ~31u;
        case TK_ADDSP: return idx & ~3u;
        case TK_PUSHPOP: return idx & ~3u;
        case TK_LDMSTM: return idx & ~31u;
        case TK_BCOND: return idx & ~3u;
        case TK_SWI: return idx & ~3u;
        case TK_B: return idx & ~31u;
        case TK_BLHI: return idx & ~31u;
        case TK_BLLO: return idx & ~31u;
        default: return 0x3A0;  // 11101 -> undefined
    }
}

template <u32 IDX>
static void thumbExec(Cpu& c, u16 op) {
    constexpr u32 kind = thumbKind(IDX);
    if constexpr (kind == TK_SHIFT) {
        thShiftImm<(IDX >> 5) & 3>(c, op);
    } else if constexpr (kind == TK_ADDSUB) {
        thAddSub<(IDX & 0x10) != 0, (IDX & 0x8) != 0>(c, op);
    } else if constexpr (kind == TK_IMM) {
        thImm<(IDX >> 5) & 3>(c, op);
    } else if constexpr (kind == TK_ALU) {
        thAlu<IDX & 15>(c, op);
    } else if constexpr (kind == TK_HIREG) {
        thHiReg<(IDX >> 2) & 3>(c, op);
    } else if constexpr (kind == TK_LDRPC) {
        thLdrPc(c, op);
    } else if constexpr (kind == TK_LDST_REG) {
        thLdStReg<(IDX >> 3) & 7>(c, op);
    } else if constexpr (kind == TK_LDST_IMM) {
        thLdStImm<(IDX & 0x40) != 0, (IDX & 0x20) != 0>(c, op);
    } else if constexpr (kind == TK_LDST_H) {
        thLdStH<(IDX & 0x20) != 0>(c, op);
    } else if constexpr (kind == TK_LDST_SP) {
        thLdStSp<(IDX & 0x20) != 0>(c, op);
    } else if constexpr (kind == TK_ADDR) {
        thAddr<(IDX & 0x20) != 0>(c, op);
    } else if constexpr (kind == TK_ADDSP) {
        thAddSp(c, op);
    } else if constexpr (kind == TK_PUSHPOP) {
        thPushPop<(IDX & 0x20) != 0, (IDX & 0x4) != 0>(c, op);
    } else if constexpr (kind == TK_LDMSTM) {
        thLdmStm<(IDX & 0x20) != 0>(c, op);
    } else if constexpr (kind == TK_BCOND) {
        thBcond<(IDX >> 2) & 15>(c, op);
    } else if constexpr (kind == TK_SWI) {
        thSwi(c, op);
    } else if constexpr (kind == TK_B) {
        thB(c, op);
    } else if constexpr (kind == TK_BLHI) {
        thBlHi(c, op);
    } else if constexpr (kind == TK_BLLO) {
        thBlLo(c, op);
    } else {
        thUndef(c, op);
    }
}

template <u32... I>
static constexpr std::array<ThumbHandler, 1024> makeThumbTable(std::integer_sequence<u32, I...>) {
    return {{&thumbExec<thumbCanon(I)>...}};
}

static const std::array<ThumbHandler, 1024> kThumbTable =
    makeThumbTable(std::make_integer_sequence<u32, 1024>{});

void Cpu::stepThumb() {
    const u16 op = (u16)pipe[0];
    pipe[0] = pipe[1];
    pipe[1] = g->bus.fetch16(r[15], true);
    kThumbTable[op >> 6](*this, op);
    r[15] += thumb ? 2 : 4;
}

}  // namespace retra
