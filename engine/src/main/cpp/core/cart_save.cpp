// Retra GBA core - cartridge backup memory.
#include "cart_save.h"

#include "gba.h"
#include "state.h"

namespace retra {

static constexpr u8 kFlash64Maker = 0x32, kFlash64Device = 0x1B;    // Panasonic MN63F805MNP
static constexpr u8 kFlash128Maker = 0xC2, kFlash128Device = 0x09;  // Macronix MX29L010

SaveKind CartSave::detect(const u8* rom, size_t size, bool* eepromUnknownSize) {
    if (eepromUnknownSize) *eepromUnknownSize = false;
    // Library ID strings are word aligned in practice; scan aligned first, then unaligned.
    struct Sig {
        const char* s;
        SaveKind k;
    };
    static const Sig sigs[] = {
        {"EEPROM_V", SaveKind::EEPROM_8K}, {"SRAM_F_V", SaveKind::SRAM},      {"SRAM_V", SaveKind::SRAM},
        {"FLASH1M_V", SaveKind::FLASH_128K}, {"FLASH512_V", SaveKind::FLASH_64K}, {"FLASH_V", SaveKind::FLASH_64K},
    };
    for (int pass = 0; pass < 2; pass++) {
        const size_t stepSize = pass == 0 ? 4 : 1;
        for (size_t i = 0; i + 12 <= size; i += stepSize) {
            const u8 c0 = rom[i];
            if (c0 != 'E' && c0 != 'S' && c0 != 'F') continue;
            for (const Sig& sg : sigs) {
                const size_t n = std::strlen(sg.s);
                if (i + n <= size && std::memcmp(rom + i, sg.s, n) == 0) {
                    if (sg.k == SaveKind::EEPROM_8K && eepromUnknownSize) *eepromUnknownSize = true;
                    return sg.k;
                }
            }
        }
    }
    return SaveKind::NONE;
}

void CartSave::configure(SaveKind k, bool eepromUnknown) {
    kind = k;
    eepromSizeUnknown = eepromUnknown && isEeprom();
    size_t n = 0;
    switch (k) {
        case SaveKind::SRAM: n = 0x8000; break;
        case SaveKind::FLASH_64K: n = 0x10000; break;
        case SaveKind::FLASH_128K: n = 0x20000; break;
        case SaveKind::EEPROM_512: n = 512; break;
        case SaveKind::EEPROM_8K: n = 8192; break;
        default: n = 0; break;
    }
    data.assign(n, 0xFF);
    dirty = false;
    resetState();
}

void CartSave::resetState() {
    flashState = 0;
    flashId = flashErasePrep = flashWriteNext = flashBankNext = false;
    flashBank = 0;
    eepState = 0;
    eepBits = 0;
    eepBuffer = 0;
    eepAddr = 0;
    eepCmd = 0;
    eepReadData = 0;
    eepReadPos = 0;
}

size_t CartSave::saveSize() const { return data.size(); }

bool CartSave::load(const u8* d, size_t n) {
    if (!d || n == 0 || kind == SaveKind::NONE) return false;
    if (isFlash()) {
        if (n >= 0x20000 && kind == SaveKind::FLASH_64K) configure(SaveKind::FLASH_128K, false);
        else if (n >= 0x10000 && n < 0x20000 && kind == SaveKind::FLASH_128K) configure(SaveKind::FLASH_64K, false);
    } else if (isEeprom()) {
        if (n >= 8192) configure(SaveKind::EEPROM_8K, false);
        else if (n >= 512) configure(SaveKind::EEPROM_512, false);
    }
    const size_t m = n < data.size() ? n : data.size();
    std::memcpy(data.data(), d, m);
    dirty = false;
    return true;
}

u8 CartSave::read8(u32 addr) {
    if (kind == SaveKind::SRAM) return data[addr & 0x7FFF];
    if (isFlash()) {
        const u32 off = addr & 0xFFFF;
        if (flashId && off < 2) {
            if (kind == SaveKind::FLASH_128K) return off == 0 ? kFlash128Maker : kFlash128Device;
            return off == 0 ? kFlash64Maker : kFlash64Device;
        }
        return data[(flashBank * 0x10000 + off) % data.size()];
    }
    return 0xFF;
}

void CartSave::write8(u32 addr, u8 v) {
    if (kind == SaveKind::SRAM) {
        u8& d = data[addr & 0x7FFF];
        if (d != v) {
            d = v;
            dirty = true;
        }
        return;
    }
    if (!isFlash()) return;
    const u32 off = addr & 0xFFFF;
    if (flashWriteNext) {
        data[(flashBank * 0x10000 + off) % data.size()] = v;
        flashWriteNext = false;
        dirty = true;
        return;
    }
    if (flashBankNext) {
        flashBankNext = false;
        if (off == 0) {
            flashBank = (kind == SaveKind::FLASH_128K) ? (v & 1) : 0;
            return;
        }
    }
    switch (flashState) {
        case 0:
            if (off == 0x5555 && v == 0xAA)
                flashState = 1;
            else if (v == 0xF0)
                flashId = false;
            break;
        case 1: flashState = (off == 0x2AAA && v == 0x55) ? 2 : 0; break;
        case 2:
            flashState = 0;
            if (off == 0x5555) {
                switch (v) {
                    case 0x90: flashId = true; break;
                    case 0xF0: flashId = false; break;
                    case 0x80: flashErasePrep = true; break;
                    case 0x10:
                        if (flashErasePrep) {
                            std::fill(data.begin(), data.end(), 0xFF);
                            dirty = true;
                        }
                        flashErasePrep = false;
                        break;
                    case 0xA0: flashWriteNext = true; break;
                    case 0xB0: flashBankNext = kind == SaveKind::FLASH_128K; break;
                    default: break;
                }
            } else if (flashErasePrep && v == 0x30) {
                const u32 base = (flashBank * 0x10000 + (off & 0xF000)) % data.size();
                std::fill(data.begin() + base, data.begin() + base + 0x1000, 0xFF);
                flashErasePrep = false;
                dirty = true;
            }
            break;
    }
}

void CartSave::eepromDmaHint(u32 count) {
    if (!eepromSizeUnknown) return;
    if (count == 9 || count == 73) {
        eepromSizeUnknown = false;
        kind = SaveKind::EEPROM_512;
        data.resize(512);
    } else if (count == 17 || count == 81) {
        eepromSizeUnknown = false;
        kind = SaveKind::EEPROM_8K;
        data.resize(8192, 0xFF);
    }
}

u16 CartSave::eepromRead() {
    if (eepState == 4) {
        u16 bit = 0;
        if (eepReadPos >= 4) bit = (u16)((eepReadData >> (63 - (eepReadPos - 4))) & 1);
        if (++eepReadPos >= 68) eepState = 0;
        return bit;
    }
    return 1;  // ready
}

void CartSave::eepromWrite(u16 v) {
    const u32 bit = v & 1;
    if (eepState == 4) {
        // A new command interrupts a pending read.
        eepState = 0;
        eepBits = 0;
        eepBuffer = 0;
    }
    switch (eepState) {
        case 0:
            eepBuffer = (eepBuffer << 1) | bit;
            if (++eepBits == 2) {
                eepCmd = (u32)(eepBuffer & 3);
                eepBits = 0;
                eepBuffer = 0;
                if (eepCmd == 3 || eepCmd == 2) eepState = 1;
            }
            break;
        case 1:
            eepBuffer = (eepBuffer << 1) | bit;
            if (++eepBits == eepAddrBits()) {
                const u32 blocks = (u32)(data.size() / 8);
                eepAddr = (u32)(eepBuffer % (blocks ? blocks : 1));
                eepBits = 0;
                eepBuffer = 0;
                eepState = eepCmd == 3 ? 2 : 3;
            }
            break;
        case 2: {  // end bit of read request
            u64 d = 0;
            for (int i = 0; i < 8; i++) d = (d << 8) | data[eepAddr * 8 + i];
            eepReadData = d;
            eepReadPos = 0;
            eepState = 4;
            break;
        }
        case 3:
            eepBuffer = (eepBuffer << 1) | bit;
            if (++eepBits == 64) {
                eepState = 5;
                eepBits = 0;
            }
            break;
        case 5: {  // end bit of write request
            u64 d = eepBuffer;
            for (int i = 7; i >= 0; i--) {
                data[eepAddr * 8 + i] = (u8)d;
                d >>= 8;
            }
            dirty = true;
            eepBuffer = 0;
            eepState = 0;
            break;
        }
        default: eepState = 0; break;
    }
}

void CartSave::serialize(StateWriter& w) const {
    w.tag("SAV0");
    w.pod(kind);
    w.u8v(data);
    w.pod(eepromSizeUnknown);
    w.pod(flashState);
    w.pod(flashId);
    w.pod(flashErasePrep);
    w.pod(flashWriteNext);
    w.pod(flashBankNext);
    w.pod(flashBank);
    w.pod(eepState);
    w.pod(eepBits);
    w.pod(eepBuffer);
    w.pod(eepAddr);
    w.pod(eepCmd);
    w.pod(eepReadData);
    w.pod(eepReadPos);
}

void CartSave::deserialize(StateReader& rd) {
    rd.tag("SAV0");
    rd.pod(kind);
    rd.u8v(data, 0x20000);
    rd.pod(eepromSizeUnknown);
    rd.pod(flashState);
    rd.pod(flashId);
    rd.pod(flashErasePrep);
    rd.pod(flashWriteNext);
    rd.pod(flashBankNext);
    rd.pod(flashBank);
    rd.pod(eepState);
    rd.pod(eepBits);
    rd.pod(eepBuffer);
    rd.pod(eepAddr);
    rd.pod(eepCmd);
    rd.pod(eepReadData);
    rd.pod(eepReadPos);
    flashBank &= 1;
    // States restore the save memory too (the game's view must match the state);
    // mark dirty so the host persists it.
    dirty = true;
}

}  // namespace retra
