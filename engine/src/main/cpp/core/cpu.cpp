// Retra GBA core - ARM7TDMI common (modes, exceptions, run loop).
#include "cpu.h"

#include "bus_inl.h"
#include "gba.h"
#include "state.h"

namespace retra {

int Cpu::bankIndex(u32 m) {
    switch (m) {
        case MODE_FIQ: return 1;
        case MODE_IRQ: return 2;
        case MODE_SVC: return 3;
        case MODE_ABT: return 4;
        case MODE_UND: return 5;
        default: return 0;
    }
}

void Cpu::reset() {
    std::memset(r, 0, sizeof(r));
    std::memset(bankR13, 0, sizeof(bankR13));
    std::memset(bankR14, 0, sizeof(bankR14));
    std::memset(bankSpsr, 0, sizeof(bankSpsr));
    std::memset(bankFiq, 0, sizeof(bankFiq));
    std::memset(bankUsr, 0, sizeof(bankUsr));
    fN = fZ = fC = fV = 0;
    mode = MODE_SVC;
    thumb = false;
    irqDisable = true;
    fiqDisable = true;
    spsr = 0;
    halted = false;
    irqLine = false;
    r[15] = 8;
}

void Cpu::switchMode(u32 newMode) {
    int ob = bankIndex(mode), nb = bankIndex(newMode);
    if (ob != nb) {
        bankR13[ob] = r[13];
        bankR14[ob] = r[14];
        bankSpsr[ob] = spsr;
        if (ob == 1) {
            for (int i = 0; i < 5; i++) {
                bankFiq[i] = r[8 + i];
                r[8 + i] = bankUsr[i];
            }
        } else if (nb == 1) {
            for (int i = 0; i < 5; i++) {
                bankUsr[i] = r[8 + i];
                r[8 + i] = bankFiq[i];
            }
        }
        r[13] = bankR13[nb];
        r[14] = bankR14[nb];
        spsr = bankSpsr[nb];
    }
    mode = newMode;
}

void Cpu::setCpsr(u32 v) {
    setFlags(v);
    irqDisable = (v >> 7) & 1;
    fiqDisable = (v >> 6) & 1;
    thumb = (v >> 5) & 1;
    u32 m = v & 0x1F;
    if (bankIndex(m) == 0 && m != MODE_USR && m != MODE_SYS) m = MODE_SYS;  // invalid -> sys
    switchMode(m);
}


void Cpu::flushPipeline() {
    Bus& b = g->bus;
    if (thumb) {
        r[15] &= ~1u;
        pipe[0] = b.peek<u16>(r[15] - 4);
        pipe[1] = b.peek<u16>(r[15] - 2);
    } else {
        r[15] &= ~3u;
        pipe[0] = b.peek<u32>(r[15] - 8);
        pipe[1] = b.peek<u32>(r[15] - 4);
    }
}

void Cpu::raiseIrq() {
    // Called between instructions: r[15] = next + 8 (ARM) / next + 4 (THUMB).
    u32 next = r[15] - (thumb ? 4 : 8);
    u32 old = cpsr();
    switchMode(MODE_IRQ);
    spsr = old;
    r[14] = next + 4;
    irqDisable = true;
    thumb = false;
    halted = false;
    pipe[0] = g->bus.fetch32(0x18, false);
    pipe[1] = g->bus.fetch32(0x1C, true);
    r[15] = 0x18 + 8;
}

void Cpu::raiseSwi() {
    // Called during execution of the SWI instruction.
    u32 next = r[15] - (thumb ? 2 : 4);
    u32 old = cpsr();
    switchMode(MODE_SVC);
    spsr = old;
    r[14] = next;
    irqDisable = true;
    thumb = false;
    branchTo(0x08);
}

void Cpu::raiseUndefined() {
    u32 next = r[15] - (thumb ? 2 : 4);
    u32 old = cpsr();
    switchMode(MODE_UND);
    spsr = old;
    r[14] = next;
    irqDisable = true;
    thumb = false;
    branchTo(0x04);
}

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

const u16 Cpu::kCondTable[16] = {
    makeCondMask(0),  makeCondMask(1),  makeCondMask(2),  makeCondMask(3), makeCondMask(4),  makeCondMask(5),
    makeCondMask(6),  makeCondMask(7),  makeCondMask(8),  makeCondMask(9), makeCondMask(10), makeCondMask(11),
    makeCondMask(12), makeCondMask(13), makeCondMask(14), makeCondMask(15),
};

void Cpu::idleCheck() {
    const u32 pc = r[15];
    if (pc == idlePc && idleBackoff) {
        // This loop recently proved to be doing real work; check again later.
        idleBackoff--;
        return;
    }
    const u32 fl = cpsr();
    const u32 writes = g->bus.writeCount + g->bus.volatileReads * 0x10001u;
    if (pc == idlePc) {
        if (writes == idleWrites && fl == idleFlags && std::memcmp(r, idleRegs, sizeof(idleRegs)) == 0) {
            Scheduler& s = g->sched;
            if (s.next > s.now && s.next != kNever) s.now = s.next;
            return;
        }
        // A change caused by an event (e.g. VCOUNT advancing) is expected for a polling loop;
        // only changes made by the loop itself count as evidence of real work.
        if (g->eventEpoch == idleEpoch && ++idleFails >= 8) {
            idleFails = 0;
            idleBackoff = 256;
        }
    } else {
        idleFails = 0;
        idleBackoff = 0;
    }
    idlePc = pc;
    idleEpoch = g->eventEpoch;
    idleFlags = fl;
    idleWrites = writes;
    std::memcpy(idleRegs, r, sizeof(idleRegs));
}

u32 Cpu::openBus() {
    if (!thumb) return pipe[1];
    switch (r[15] >> 24) {
        case 0x0:
        case 0x7: return (pipe[0] & 0xFFFF) | (pipe[1] << 16);
        case 0x3:
            if (r[15] & 2) return (pipe[0] & 0xFFFF) | (pipe[1] << 16);
            return (pipe[1] & 0xFFFF) | (pipe[0] << 16);
        default: return (pipe[1] & 0xFFFF) | (pipe[1] << 16);
    }
}

void Cpu::run() {
    Scheduler& s = g->sched;
    while (s.now < s.next) {
        if (RETRA_UNLIKELY(halted)) {
            s.now = s.next;
            break;
        }
        if (RETRA_UNLIKELY(irqLine) && !irqDisable) raiseIrq();
        if (thumb)
            stepThumb();
        else
            stepArm();
    }
}

void Cpu::serialize(StateWriter& w) const {
    w.tag("CPU0");
    w.pod(r);
    w.pod(fN);
    w.pod(fZ);
    w.pod(fC);
    w.pod(fV);
    w.pod(mode);
    w.pod(thumb);
    w.pod(irqDisable);
    w.pod(fiqDisable);
    w.pod(spsr);
    w.pod(bankR13);
    w.pod(bankR14);
    w.pod(bankSpsr);
    w.pod(bankFiq);
    w.pod(bankUsr);
    w.pod(halted);
    w.pod(pipe);
    w.pod(idlePc);
    w.pod(idleFlags);
    w.pod(idleWrites);
    w.pod(idleRegs);
    w.pod(idleFails);
    w.pod(idleBackoff);
    w.pod(idleEpoch);
}

void Cpu::deserialize(StateReader& rd) {
    rd.tag("CPU0");
    rd.pod(r);
    rd.pod(fN);
    rd.pod(fZ);
    rd.pod(fC);
    rd.pod(fV);
    rd.pod(mode);
    rd.pod(thumb);
    rd.pod(irqDisable);
    rd.pod(fiqDisable);
    rd.pod(spsr);
    rd.pod(bankR13);
    rd.pod(bankR14);
    rd.pod(bankSpsr);
    rd.pod(bankFiq);
    rd.pod(bankUsr);
    rd.pod(halted);
    rd.pod(pipe);
    rd.pod(idlePc);
    rd.pod(idleFlags);
    rd.pod(idleWrites);
    rd.pod(idleRegs);
    rd.pod(idleFails);
    rd.pod(idleBackoff);
    rd.pod(idleEpoch);
    fN &= 1;
    fZ &= 1;
    fC &= 1;
    fV &= 1;
}

}  // namespace retra
