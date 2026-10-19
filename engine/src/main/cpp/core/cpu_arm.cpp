// Retra GBA core - ARM (32-bit) instruction set.
#include <array>
#include <utility>

#include "bus_inl.h"
#include "cpu.h"
#include "cpu_shift.h"
#include "gba.h"

namespace retra {

using ArmHandler = void (*)(Cpu&, u32);

// ---------------------------------------------------------------- data processing
template <u32 OPC, bool S, bool IMM, u32 SHT, bool REGSHIFT>
static void armDataProc(Cpu& c, u32 op) {
    const u32 rn = (op >> 16) & 15;
    const u32 rd = (op >> 12) & 15;
    u32 op2;
    u32 carry = c.fC;
    u32 rnv;
    if constexpr (IMM) {
        const u32 rot = ((op >> 8) & 15) * 2;
        op2 = ror32(op & 0xFF, rot);
        if (rot) carry = op2 >> 31;
        rnv = c.r[rn];
    } else if constexpr (!REGSHIFT) {
        op2 = c.shiftImm<S>(SHT, c.r[op & 15], (op >> 7) & 31, carry);
        rnv = c.r[rn];
    } else {
        // Register specified shift: PC operands read as +12.
        c.r[15] += 4;
        const u32 amt = c.r[(op >> 8) & 15] & 0xFF;
        op2 = c.shiftReg<S>(SHT, c.r[op & 15], amt, carry);
        rnv = c.r[rn];
        c.r[15] -= 4;
        c.idle(1);
    }

    u32 res = 0;
    if constexpr (OPC == 0x0) {  // AND
        res = rnv & op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    } else if constexpr (OPC == 0x1) {  // EOR
        res = rnv ^ op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    } else if constexpr (OPC == 0x2) {  // SUB
        res = S ? c.subFlags(rnv, op2) : rnv - op2;
    } else if constexpr (OPC == 0x3) {  // RSB
        res = S ? c.subFlags(op2, rnv) : op2 - rnv;
    } else if constexpr (OPC == 0x4) {  // ADD
        res = S ? c.addFlags(rnv, op2) : rnv + op2;
    } else if constexpr (OPC == 0x5) {  // ADC
        res = S ? c.adcFlags(rnv, op2, c.fC) : rnv + op2 + c.fC;
    } else if constexpr (OPC == 0x6) {  // SBC
        res = S ? c.sbcFlags(rnv, op2, c.fC) : rnv - op2 - (c.fC ^ 1);
    } else if constexpr (OPC == 0x7) {  // RSC
        res = S ? c.sbcFlags(op2, rnv, c.fC) : op2 - rnv - (c.fC ^ 1);
    } else if constexpr (OPC == 0x8) {  // TST
        c.setNZ(rnv & op2);
        c.fC = carry;
    } else if constexpr (OPC == 0x9) {  // TEQ
        c.setNZ(rnv ^ op2);
        c.fC = carry;
    } else if constexpr (OPC == 0xA) {  // CMP
        c.subFlags(rnv, op2);
    } else if constexpr (OPC == 0xB) {  // CMN
        c.addFlags(rnv, op2);
    } else if constexpr (OPC == 0xC) {  // ORR
        res = rnv | op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    } else if constexpr (OPC == 0xD) {  // MOV
        res = op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    } else if constexpr (OPC == 0xE) {  // BIC
        res = rnv & ~op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    } else {  // MVN
        res = ~op2;
        if (S) { c.setNZ(res); c.fC = carry; }
    }

    if constexpr (OPC >= 0x8 && OPC <= 0xB) {
        if (rd == 15 && c.hasSpsr()) {
            // Legacy "P" form: restore CPSR from SPSR.
            c.setCpsr(c.spsr);
        }
        return;
    } else {
        if (rd == 15) {
            if (S && c.hasSpsr()) c.setCpsr(c.spsr);
            c.branchTo(res);
        } else {
            c.r[rd] = res;
        }
    }
}

// ---------------------------------------------------------------- multiply
template <bool ACC, bool S>
static void armMul(Cpu& c, u32 op) {
    const u32 rd = (op >> 16) & 15, rn = (op >> 12) & 15, rs = (op >> 8) & 15, rm = op & 15;
    u32 res = c.r[rm] * c.r[rs];
    if (ACC) res += c.r[rn];
    c.idle(Cpu::mulCycles(c.r[rs], true) + (ACC ? 1 : 0));
    if (S) c.setNZ(res);
    c.r[rd] = res;
}

template <bool SIGNED, bool ACC, bool S>
static void armMulLong(Cpu& c, u32 op) {
    const u32 rdhi = (op >> 16) & 15, rdlo = (op >> 12) & 15, rs = (op >> 8) & 15, rm = op & 15;
    u64 res;
    if (SIGNED)
        res = (u64)((s64)(s32)c.r[rm] * (s64)(s32)c.r[rs]);
    else
        res = (u64)c.r[rm] * (u64)c.r[rs];
    if (ACC) res += ((u64)c.r[rdhi] << 32) | c.r[rdlo];
    c.idle(Cpu::mulCycles(c.r[rs], SIGNED) + 1 + (ACC ? 1 : 0));
    c.r[rdlo] = (u32)res;
    c.r[rdhi] = (u32)(res >> 32);
    if (S) {
        c.fN = (u32)(res >> 63);
        c.fZ = res == 0;
    }
}

// ---------------------------------------------------------------- single data transfer
template <bool REGOFF, bool PRE, bool UP, bool BYTE, bool WB, bool LOAD, u32 SHT>
static void armSdt(Cpu& c, u32 op) {
    const u32 rn = (op >> 16) & 15, rd = (op >> 12) & 15;
    u32 off;
    if constexpr (REGOFF) {
        u32 dummy = 0;
        off = c.shiftImm<false>(SHT, c.r[op & 15], (op >> 7) & 31, dummy);
    } else {
        off = op & 0xFFF;
    }
    const u32 base = c.r[rn];
    const u32 offAddr = UP ? base + off : base - off;
    const u32 addr = PRE ? offAddr : base;
    Bus& b = c.g->bus;
    if constexpr (LOAD) {
        u32 v;
        if (BYTE)
            v = b.read<u8>(addr);
        else
            v = ror32(b.read<u32>(addr), (addr & 3) * 8);
        c.idle(1);
        if (!PRE || WB) c.r[rn] = offAddr;
        if (rd == 15)
            c.branchTo(v);
        else
            c.r[rd] = v;
    } else {
        const u32 v = c.r[rd] + (rd == 15 ? 4 : 0);
        if (BYTE)
            b.write<u8>(addr, (u8)v);
        else
            b.write<u32>(addr, v);
        if (!PRE || WB) c.r[rn] = offAddr;
    }
}

// ---------------------------------------------------------------- halfword / signed transfer
template <bool PRE, bool UP, bool IMMOFF, bool WB, bool LOAD, u32 SH>
static void armHdt(Cpu& c, u32 op) {
    const u32 rn = (op >> 16) & 15, rd = (op >> 12) & 15;
    const u32 off = IMMOFF ? (((op >> 4) & 0xF0) | (op & 0xF)) : c.r[op & 15];
    const u32 base = c.r[rn];
    const u32 offAddr = UP ? base + off : base - off;
    const u32 addr = PRE ? offAddr : base;
    Bus& b = c.g->bus;
    if constexpr (LOAD) {
        u32 v;
        if constexpr (SH == 1) {
            v = ror32(b.read<u16>(addr), (addr & 1) * 8);
        } else if constexpr (SH == 2) {
            v = (u32)(s32)(s8)b.read<u8>(addr);
        } else {
            if (addr & 1)
                v = (u32)(s32)(s8)b.read<u8>(addr);
            else
                v = (u32)(s32)(s16)b.read<u16>(addr);
        }
        c.idle(1);
        if (!PRE || WB) c.r[rn] = offAddr;
        if (rd == 15)
            c.branchTo(v);
        else
            c.r[rd] = v;
    } else {
        if constexpr (SH == 1) {
            const u32 v = c.r[rd] + (rd == 15 ? 4 : 0);
            b.write<u16>(addr, (u16)v);
        }
        // SH 2/3 stores (LDRD/STRD) do not exist on ARMv4: no transfer.
        if (!PRE || WB) c.r[rn] = offAddr;
    }
}

template <bool BYTE>
static void armSwap(Cpu& c, u32 op) {
    const u32 rn = (op >> 16) & 15, rd = (op >> 12) & 15, rm = op & 15;
    const u32 addr = c.r[rn];
    const u32 src = c.r[rm];
    Bus& b = c.g->bus;
    u32 tmp;
    if (BYTE) {
        tmp = b.read<u8>(addr);
        b.write<u8>(addr, (u8)src);
    } else {
        tmp = ror32(b.read<u32>(addr), (addr & 3) * 8);
        b.write<u32>(addr, src);
    }
    c.idle(1);
    c.r[rd] = tmp;
}

// ---------------------------------------------------------------- block transfer
template <bool PRE, bool UP, bool S, bool WB, bool LOAD>
static void armBlock(Cpu& c, u32 op) {
    const u32 rn = (op >> 16) & 15;
    u32 list = op & 0xFFFF;
    u32 n = (u32)__builtin_popcount(list);
    if (list == 0) {
        list = 0x8000;
        n = 16;
    }
    const u32 base = c.r[rn];
    u32 start, final;
    if (UP) {
        final = base + n * 4;
        start = PRE ? base + 4 : base;
    } else {
        final = base - n * 4;
        start = PRE ? final : final + 4;
    }
    const bool userBank = S && !(LOAD && (list & 0x8000));
    const u32 oldMode = c.mode;
    if (userBank) c.switchMode(MODE_USR);

    Bus& b = c.g->bus;
    u32 addr = start;
    bool seq = false;
    if constexpr (LOAD) {
        if (WB) c.r[rn] = final;
        u32 pcVal = 0;
        for (u32 i = 0; i < 16; i++) {
            if (!(list & (1u << i))) continue;
            u32 v = b.read<u32>(addr, seq);
            seq = true;
            if (i == 15)
                pcVal = v;
            else
                c.r[i] = v;
            addr += 4;
        }
        c.idle(1);
        if (userBank) c.switchMode(oldMode);
        if (list & 0x8000) {
            if (S && c.hasSpsr()) c.setCpsr(c.spsr);
            c.branchTo(pcVal);
        }
    } else {
        bool first = true;
        for (u32 i = 0; i < 16; i++) {
            if (!(list & (1u << i))) continue;
            u32 v = (i == 15) ? c.r[15] + 4 : c.r[i];
            b.write<u32>(addr, v, seq);
            seq = true;
            if (first && WB) c.r[rn] = final;
            first = false;
            addr += 4;
        }
        if (userBank) c.switchMode(oldMode);
    }
}

// ---------------------------------------------------------------- branches / misc
template <bool LINK>
static void armBranch(Cpu& c, u32 op) {
    const u32 off = (u32)((s32)(op << 8) >> 6);
    if (LINK) c.r[14] = c.r[15] - 4;
    c.branchTo(c.r[15] + off);
}

static void armBx(Cpu& c, u32 op) {
    const u32 v = c.r[op & 15];
    c.thumb = v & 1;
    c.branchTo(v);
}

template <bool SPSR>
static void armMrs(Cpu& c, u32 op) {
    const u32 rd = (op >> 12) & 15;
    c.r[rd] = SPSR ? (c.hasSpsr() ? c.spsr : c.cpsr()) : c.cpsr();
}

template <bool IMM, bool SPSR>
static void armMsr(Cpu& c, u32 op) {
    const u32 v = IMM ? ror32(op & 0xFF, ((op >> 8) & 15) * 2) : c.r[op & 15];
    u32 mask = 0;
    if (op & (1u << 19)) mask |= 0xFF000000;
    if (op & (1u << 18)) mask |= 0x00FF0000;
    if (op & (1u << 17)) mask |= 0x0000FF00;
    if (op & (1u << 16)) mask |= 0x000000FF;
    if (SPSR) {
        if (c.hasSpsr()) c.spsr = (c.spsr & ~mask) | (v & mask);
    } else {
        if (c.mode == MODE_USR) mask &= 0xFF000000;
        u32 nv = (c.cpsr() & ~mask) | (v & mask);
        nv = (nv & ~0x20u) | (c.thumb ? 0x20u : 0u);
        c.setCpsr(nv);
    }
}

static void armSwi(Cpu& c, u32) { c.raiseSwi(); }

static void armUndefined(Cpu& c, u32 op) {
    const u32 pc = c.r[15] - 8;
    if (pc < 0x4000 && (op & kHleTrapMask) == kHleTrapBase) {
        c.g->hleCall((op & 0xF) | ((op >> 4) & 0xFF0));
        return;
    }
    c.raiseUndefined();
}

// ---------------------------------------------------------------- decoding
// idx = bits 27..20 << 4 | bits 7..4
enum ArmKind : u32 {
    AK_UNDEF,
    AK_DP_REG,
    AK_DP_IMM,
    AK_MUL,
    AK_MULL,
    AK_SWP,
    AK_HDT,
    AK_BX,
    AK_MRS,
    AK_MSR_REG,
    AK_MSR_IMM,
    AK_SDT_IMM,
    AK_SDT_REG,
    AK_BLOCK,
    AK_BRANCH,
    AK_SWI,
};

static constexpr u32 armKind(u32 idx) {
    const u32 hi = idx >> 4, lo = idx & 0xF;
    switch (hi >> 5) {
        case 0:
            if (lo == 0x9) {
                if ((hi & 0xFC) == 0x00) return AK_MUL;
                if ((hi & 0xF8) == 0x08) return AK_MULL;
                if ((hi & 0xFB) == 0x10) return AK_SWP;
                return AK_UNDEF;
            }
            if ((lo & 0x9) == 0x9) return AK_HDT;
            if ((hi & 0xF9) == 0x10) {
                if (hi == 0x12 && lo == 0x1) return AK_BX;
                if ((hi & 0xFB) == 0x10 && lo == 0) return AK_MRS;
                if ((hi & 0xFB) == 0x12 && lo == 0) return AK_MSR_REG;
                return AK_UNDEF;
            }
            return AK_DP_REG;
        case 1:
            if ((hi & 0xF9) == 0x30) return (hi & 0xFB) == 0x32 ? AK_MSR_IMM : AK_UNDEF;
            return AK_DP_IMM;
        case 2: return AK_SDT_IMM;
        case 3: return (lo & 1) ? AK_UNDEF : AK_SDT_REG;
        case 4: return AK_BLOCK;
        case 5: return AK_BRANCH;
        case 6: return AK_UNDEF;
        default: return (hi & 0x10) ? AK_SWI : AK_UNDEF;
    }
}

// Reduce the index to the bits a handler actually depends on (fewer instantiations).
static constexpr u32 armCanon(u32 idx) {
    const u32 hi = idx >> 4, lo = idx & 0xF;
    switch (armKind(idx)) {
        case AK_UNDEF: return 0xE00 | 0x1;  // canonical undefined slot (cond. space 111, bit4)
        case AK_DP_REG: return (hi << 4) | ((lo & 1) ? (lo & 0x7) : (lo & 0x6));
        case AK_DP_IMM: return hi << 4;
        case AK_MUL:
        case AK_MULL:
        case AK_SWP: return idx;
        case AK_HDT: return idx;
        case AK_BX: return idx;
        case AK_MRS:
        case AK_MSR_REG: return idx;
        case AK_MSR_IMM: return hi << 4;
        case AK_SDT_IMM: return hi << 4;
        case AK_SDT_REG: return (hi << 4) | (lo & 0x6);
        case AK_BLOCK: return hi << 4;
        case AK_BRANCH: return (hi & 0xF0) << 4;
        default: return 0xF00;  // SWI
    }
}

template <u32 IDX>
static void armExec(Cpu& c, u32 op) {
    constexpr u32 hi = IDX >> 4, lo = IDX & 0xF;
    constexpr u32 kind = armKind(IDX);
    if constexpr (IDX == (0xE00 | 0x1)) {
        armUndefined(c, op);
    } else if constexpr (kind == AK_DP_REG) {
        armDataProc<(hi >> 1) & 15, (hi & 1) != 0, false, (lo >> 1) & 3, (lo & 1) != 0>(c, op);
    } else if constexpr (kind == AK_DP_IMM) {
        armDataProc<(hi >> 1) & 15, (hi & 1) != 0, true, 0, false>(c, op);
    } else if constexpr (kind == AK_MUL) {
        armMul<(hi & 2) != 0, (hi & 1) != 0>(c, op);
    } else if constexpr (kind == AK_MULL) {
        armMulLong<(hi & 4) != 0, (hi & 2) != 0, (hi & 1) != 0>(c, op);
    } else if constexpr (kind == AK_SWP) {
        armSwap<(hi & 4) != 0>(c, op);
    } else if constexpr (kind == AK_HDT) {
        armHdt<(hi & 0x10) != 0, (hi & 8) != 0, (hi & 4) != 0, (hi & 2) != 0, (hi & 1) != 0, (lo >> 1) & 3>(c, op);
    } else if constexpr (kind == AK_BX) {
        armBx(c, op);
    } else if constexpr (kind == AK_MRS) {
        armMrs<(hi & 4) != 0>(c, op);
    } else if constexpr (kind == AK_MSR_REG) {
        armMsr<false, (hi & 4) != 0>(c, op);
    } else if constexpr (kind == AK_MSR_IMM) {
        armMsr<true, (hi & 4) != 0>(c, op);
    } else if constexpr (kind == AK_SDT_IMM) {
        armSdt<false, (hi & 0x10) != 0, (hi & 8) != 0, (hi & 4) != 0, (hi & 2) != 0, (hi & 1) != 0, 0>(c, op);
    } else if constexpr (kind == AK_SDT_REG) {
        armSdt<true, (hi & 0x10) != 0, (hi & 8) != 0, (hi & 4) != 0, (hi & 2) != 0, (hi & 1) != 0, (lo >> 1) & 3>(
            c, op);
    } else if constexpr (kind == AK_BLOCK) {
        armBlock<(hi & 0x10) != 0, (hi & 8) != 0, (hi & 4) != 0, (hi & 2) != 0, (hi & 1) != 0>(c, op);
    } else if constexpr (kind == AK_BRANCH) {
        armBranch<(hi & 0x10) != 0>(c, op);
    } else if constexpr (kind == AK_SWI) {
        armSwi(c, op);
    } else {
        armUndefined(c, op);
    }
}

template <u32... I>
static constexpr std::array<ArmHandler, 4096> makeArmTable(std::integer_sequence<u32, I...>) {
    return {{&armExec<armCanon(I)>...}};
}

static const std::array<ArmHandler, 4096> kArmTable = makeArmTable(std::make_integer_sequence<u32, 4096>{});

static constexpr u16 makeCondMask(u32 cond) {
    u16 m = 0;
    for (u32 f = 0; f < 16; f++) {
        const bool n = f & 8, z = f & 4, c = f & 2, v = f & 1;
        bool r = false;
        switch (cond) {
            case 0x0: r = z; break;
            case 0x1: r = !z; break;
            case 0x2: r = c; break;
            case 0x3: r = !c; break;
            case 0x4: r = n; break;
            case 0x5: r = !n; break;
            case 0x6: r = v; break;
            case 0x7: r = !v; break;
            case 0x8: r = c && !z; break;
            case 0x9: r = !c || z; break;
            case 0xA: r = n == v; break;
            case 0xB: r = n != v; break;
            case 0xC: r = !z && n == v; break;
            case 0xD: r = z || n != v; break;
            case 0xE: r = true; break;
            default: r = false; break;
        }
        if (r) m |= (u16)(1u << f);
    }
    return m;
}

static constexpr u16 kCondTable[16] = {
    makeCondMask(0), makeCondMask(1), makeCondMask(2),  makeCondMask(3),  makeCondMask(4),  makeCondMask(5),
    makeCondMask(6), makeCondMask(7), makeCondMask(8),  makeCondMask(9),  makeCondMask(10), makeCondMask(11),
    makeCondMask(12), makeCondMask(13), makeCondMask(14), makeCondMask(15),
};

void Cpu::stepArm() {
    const u32 pc = r[15] - 8;
    const u32 op = g->bus.fetch32(pc, true);
    const u32 cond = op >> 28;
    if (RETRA_LIKELY(cond == 0xE) ||
        ((kCondTable[cond] >> ((fN << 3) | (fZ << 2) | (fC << 1) | fV)) & 1)) {
        kArmTable[((op >> 16) & 0xFF0) | ((op >> 4) & 0xF)](*this, op);
    }
    r[15] += thumb ? 2 : 4;
}

}  // namespace retra
