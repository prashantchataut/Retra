/* Turbo Drift - battery-backed SRAM save (magic + version + checksum). */
#include "game.h"

/* Tag so emulators detect the 32 KiB SRAM save type. */
const char save_type_tag[] __attribute__((used, aligned(4))) = "SRAM_V113";

SaveData save;

#define SAVE_VERSION 1
static const u8 magic[4] = { 'T', 'D', 'R', 'F' };

static u8 checksum(const u8 *p, int n) {
    u8 s = 0x5A;
    for (int i = 0; i < n; i++) s = (u8)((s << 1 | s >> 7) ^ p[i]);
    return s;
}

void save_reset(void) {
    memset(&save, 0, sizeof(save));
    save.credits = 1500;
    save.owned = 1;
    save.unlocked = 1;
    save.sound = 1;
    save.paint[0] = 0;
    save.paint[1] = 1;
    save.paint[2] = 2;
    save.paint[3] = 4;
}

void save_load(void) {
    u8 buf[sizeof(SaveData)];
    /* reference the tag so --gc-sections keeps it in the ROM */
    if (((volatile const char *)save_type_tag)[0] != 'S') return;
    for (int i = 0; i < 4; i++)
        if (SRAM[i] != magic[i]) { save_reset(); return; }
    if (SRAM[4] != SAVE_VERSION) { save_reset(); return; }
    for (unsigned i = 0; i < sizeof(SaveData); i++) buf[i] = SRAM[8 + i];
    if (SRAM[5] != checksum(buf, sizeof(SaveData))) { save_reset(); return; }
    memcpy(&save, buf, sizeof(SaveData));
    if (save.unlocked < 1 || save.unlocked > NUM_EVENTS) save.unlocked = 1;
    if (!(save.owned & 1)) save.owned |= 1;
    if (save.car >= NUM_CARS || !(save.owned & (1 << save.car))) save.car = 0;
    for (int i = 0; i < NUM_CARS; i++) {
        if (save.paint[i] >= NUM_PAINTS) save.paint[i] = 0;
        for (int j = 0; j < 4; j++) if (save.upg[i][j] > 3) save.upg[i][j] = 3;
    }
}

void save_write(void) {
    const u8 *p = (const u8 *)&save;
    for (unsigned i = 0; i < sizeof(SaveData); i++) SRAM[8 + i] = p[i];
    SRAM[5] = checksum(p, sizeof(SaveData));
    SRAM[4] = SAVE_VERSION;
    for (int i = 0; i < 4; i++) SRAM[i] = magic[i];
}
