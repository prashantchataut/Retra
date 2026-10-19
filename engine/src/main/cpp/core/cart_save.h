// Retra GBA core - cartridge backup memory (SRAM, Flash 64K/128K, EEPROM 512B/8KB).
#pragma once

#include <vector>

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

enum class SaveKind : int {
    NONE = 0,
    SRAM = 1,
    FLASH_64K = 2,
    FLASH_128K = 3,
    EEPROM_512 = 4,
    EEPROM_8K = 5,
    AUTO = 6,
};

class CartSave {
public:
    explicit CartSave(Gba* gba) : g(gba) {}

    // Scan ROM for library ID strings. Returns detected kind (EEPROM size may be refined later).
    static SaveKind detect(const u8* rom, size_t size, bool* eepromUnknownSize);
    void configure(SaveKind kind, bool eepromSizeUnknown);
    void resetState();  // command state machines (not contents)

    SaveKind kind = SaveKind::NONE;
    bool isEeprom() const { return kind == SaveKind::EEPROM_512 || kind == SaveKind::EEPROM_8K; }
    bool isFlash() const { return kind == SaveKind::FLASH_64K || kind == SaveKind::FLASH_128K; }

    // 0x0E/0x0F region (SRAM/Flash). Address is the full bus address.
    u8 read8(u32 addr);
    void write8(u32 addr, u8 v);
    // EEPROM serial (bit 0) at 0x0D region.
    u16 eepromRead();
    void eepromWrite(u16 v);
    void eepromDmaHint(u32 count);  // DMA3 transfer length to EEPROM -> size detection

    std::vector<u8> data;
    bool dirty = false;

    bool load(const u8* d, size_t n);  // battery file import
    size_t saveSize() const;

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    Gba* g;

private:
    bool eepromSizeUnknown = false;
    // Flash state
    u32 flashState = 0;  // 0 idle, 1 got AA, 2 got 55
    bool flashId = false;
    bool flashErasePrep = false;
    bool flashWriteNext = false;
    bool flashBankNext = false;
    u32 flashBank = 0;
    // EEPROM state
    u32 eepState = 0;  // 0 idle/cmd, see cart_save.cpp
    u32 eepBits = 0;
    u64 eepBuffer = 0;
    u32 eepAddr = 0;
    u32 eepCmd = 0;
    u64 eepReadData = 0;
    u32 eepReadPos = 0;
    u32 eepAddrBits() const { return kind == SaveKind::EEPROM_8K ? 14 : 6; }
};

}  // namespace retra
