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

void Cpu::idle(u32 n) { g->sched.now += n; }

void Cpu::branchTo(u32 addr) {
    Bus& b = g->bus;
    if (thumb) {
        addr &= ~1u;
        r[15] = addr + 2;
        u32 region = (addr >> 24) & 0xF;
        g->sched.now += b.timing[1][0][region] + b.timing[1][1][region];
    } else {
        addr &= ~3u;
        r[15] = addr + 4;
        u32 region = (addr >> 24) & 0xF;
        g->sched.now += b.timing[2][0][region] + b.timing[2][1][region];
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
    r[15] = 0x18 + 8;
    g->sched.now += 3;
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

bool Cpu::condPassed(u32 cond) const {
    switch (cond) {
        case 0x0: return fZ;
        case 0x1: return !fZ;
        case 0x2: return fC;
        case 0x3: return !fC;
        case 0x4: return fN;
        case 0x5: return !fN;
        case 0x6: return fV;
        case 0x7: return !fV;
        case 0x8: return fC && !fZ;
        case 0x9: return !fC || fZ;
        case 0xA: return fN == fV;
        case 0xB: return fN != fV;
        case 0xC: return !fZ && fN == fV;
        case 0xD: return fZ || fN != fV;
        case 0xE: return true;
        default: return false;
    }
}

u32 Cpu::openBus() {
    if (!thumb) return g->bus.peek<u32>(r[15]);
    u32 h = g->bus.peek<u16>(r[15]);
    return h | (h << 16);
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
    fN &= 1;
    fZ &= 1;
    fC &= 1;
    fV &= 1;
}

}  // namespace retra
