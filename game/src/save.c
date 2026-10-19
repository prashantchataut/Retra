/* Mochi Hop - battery-backed SRAM save. */
#include "game.h"

/* Tag so emulators detect 32 KiB SRAM save type. */
const char save_type_tag[] __attribute__((used, aligned(4))) = "SRAM_V113";

SaveData save;

#define SAVE_VERSION 1
static const u8 magic[4] = { 'M', 'H', 'O', 'P' };

static u8 checksum(const u8 *p, int n) {
    u8 s = 0x5A;
    for (int i = 0; i < n; i++) s = (u8)((s << 1 | s >> 7) ^ p[i]);
    return s;
}

static void defaults(void) {
    memset(&save, 0, sizeof(save));
    save.unlocked = 1;
}

void save_load(void) {
    u8 buf[sizeof(SaveData)];
    for (int i = 0; i < 4; i++)
        if (SRAM[i] != magic[i]) { defaults(); return; }
    if (SRAM[4] != SAVE_VERSION) { defaults(); return; }
    for (unsigned i = 0; i < sizeof(SaveData); i++) buf[i] = SRAM[8 + i];
    if (SRAM[5] != checksum(buf, sizeof(SaveData))) { defaults(); return; }
    memcpy(&save, buf, sizeof(SaveData));
    if (save.unlocked < 1 || save.unlocked > NUM_LEVELS) save.unlocked = 1;
}

void save_write(void) {
    const u8 *p = (const u8 *)&save;
    for (unsigned i = 0; i < sizeof(SaveData); i++) SRAM[8 + i] = p[i];
    SRAM[5] = checksum(p, sizeof(SaveData));
    SRAM[4] = SAVE_VERSION;
    for (int i = 0; i < 4; i++) SRAM[i] = magic[i];
}

int save_total_stars(void) {
    int t = 0;
    for (int i = 0; i < NUM_LEVELS; i++) t += save.best[i];
    return t;
}

int total_level_stars(void) {
    int t = 0;
    for (int i = 0; i < NUM_LEVELS; i++) t += level_defs[i].nstars;
    return t;
}
