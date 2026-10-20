/* Sky Sprout - battery-backed SRAM save (high scores, progress). */
#include "game.h"

/* Tag so emulators detect the 32 KiB SRAM save type. */
const char save_type_tag[] __attribute__((used, aligned(4))) = "SRAM_V113";

SaveData save;

#define SAVE_VERSION 1
static const u8 magic[4] = { 'S', 'K', 'Y', 'S' };

static u8 checksum(const u8 *p, int n) {
    u8 s = 0x5A;
    for (int i = 0; i < n; i++) s = (u8)((s << 1 | s >> 7) ^ p[i]);
    return s;
}

static const u8 default_names[NUM_SCORES][3] = {
    { 'M', 'O', 'C' }, { 'S', 'P', 'R' }, { 'L', 'E', 'A' }, { 'P', 'E', 'A' }, { 'B', 'U', 'D' }
};

static void defaults(void) {
    memset(&save, 0, sizeof(save));
    for (int d = 0; d < 3; d++) {
        for (int i = 0; i < NUM_SCORES; i++) {
            save.score[d][i] = (u32)(50000 - i * 10000) * (u32)(d + 1);
            memcpy(save.name[d][i], default_names[i], 3);
            save.stage[d][i] = (u8)(NUM_SCORES - i > 3 ? 2 : 1);
        }
        save.reached[d] = 1;
    }
    save.last_diff = 1;
}

void save_load(void) {
    u8 buf[sizeof(SaveData)];
    for (int i = 0; i < 4; i++)
        if (SRAM[i] != magic[i]) { defaults(); return; }
    if (SRAM[4] != SAVE_VERSION) { defaults(); return; }
    for (unsigned i = 0; i < sizeof(SaveData); i++) buf[i] = SRAM[8 + i];
    if (SRAM[5] != checksum(buf, sizeof(SaveData))) { defaults(); return; }
    memcpy(&save, buf, sizeof(SaveData));
    for (int d = 0; d < 3; d++)
        if (save.reached[d] < 1 || save.reached[d] > 6) save.reached[d] = 1;
    if (save.last_diff > 2) save.last_diff = 1;
}

void save_write(void) {
    const u8 *p = (const u8 *)&save;
    for (unsigned i = 0; i < sizeof(SaveData); i++) SRAM[8 + i] = p[i];
    SRAM[5] = checksum(p, sizeof(SaveData));
    SRAM[4] = SAVE_VERSION;
    for (int i = 0; i < 4; i++) SRAM[i] = magic[i];
}

int score_rank(int diff, u32 score) {
    for (int i = 0; i < NUM_SCORES; i++)
        if (score > save.score[diff][i]) return i;
    return -1;
}

void score_insert(int diff, int rank, u32 score, const u8 *name, int stage) {
    if (rank < 0 || rank >= NUM_SCORES) return;
    for (int i = NUM_SCORES - 1; i > rank; i--) {
        save.score[diff][i] = save.score[diff][i - 1];
        memcpy(save.name[diff][i], save.name[diff][i - 1], 3);
        save.stage[diff][i] = save.stage[diff][i - 1];
    }
    save.score[diff][rank] = score;
    memcpy(save.name[diff][rank], name, 3);
    save.stage[diff][rank] = (u8)stage;
}

int max_reached(void) {
    int m = 1;
    for (int d = 0; d < 3; d++)
        if (save.reached[d] > m) m = save.reached[d];
    return m > 5 ? 5 : m;
}
