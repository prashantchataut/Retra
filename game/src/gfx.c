/* Mochi Hop - video helpers: VRAM loading, sprites, text. */
#include "game.h"

u16 key_cur, key_prev;
u32 frame_count;
int fade = 16;

static u16 oam_buf[512] __attribute__((aligned(4)));
static int oam_n;

/* gcc may emit calls to these for struct copies/clears */
void *memset(void *d, int c, unsigned n) { u8 *p = d; while (n--) *p++ = (u8)c; return d; }
void *memcpy(void *d, const void *s, unsigned n) { u8 *p = d; const u8 *q = s; while (n--) *p++ = *q++; return d; }

static u32 rng_state = 0x1234567;
u32 rnd(void) {
    rng_state ^= rng_state << 13;
    rng_state ^= rng_state >> 17;
    rng_state ^= rng_state << 5;
    return rng_state;
}

int udiv(int a, int b) {
    /* small non-negative integer division without libgcc */
    int q = 0, neg = 0;
    if (a < 0) { a = -a; neg = 1; }
    if (b <= 0) return 0;
    int bit = 1;
    while (b <= a && !(b & 0x40000000)) { b <<= 1; bit <<= 1; }
    while (bit) {
        if (a >= b) { a -= b; q |= bit; }
        b >>= 1; bit >>= 1;
    }
    return neg ? -q : q;
}

void vsync(void) {
    while (REG_VCOUNT >= 160) {}
    while (REG_VCOUNT < 160) {}
    frame_count++;
}

void poll_keys(void) {
    key_prev = key_cur;
    key_cur = (~REG_KEYINPUT) & 0x3FF;
}

void copy16(volatile u16 *dst, const u16 *src, u32 count) {
    if ((((u32)dst | (u32)src) & 3) == 0) {
        volatile u32 *d = (volatile u32 *)dst;
        const u32 *s = (const u32 *)src;
        u32 n = count >> 1;
        while (n--) *d++ = *s++;
        if (count & 1) ((volatile u16 *)d)[0] = ((const u16 *)s)[0];
        return;
    }
    while (count--) *dst++ = *src++;
}

void fill16(volatile u16 *dst, u16 val, u32 count) {
    while (count--) *dst++ = val;
}

void video_init(void) {
    REG_WAITCNT = 0x4317;
    REG_DISPCNT = 0x0080;               /* forced blank while loading */
    REG_BLDCNT = 0x00FF;
    REG_BLDY = 16;
    fill16(VRAM, 0, 0x18000 / 2 - 1);
    fill16(PAL_BG, 0, 512);
    for (int i = 0; i < 128; i++) oam_buf[i * 4] = 0x0200;
    oam_commit();
}

void fade_apply(void) {
    REG_BLDCNT = 0x00FF;              /* darken all layers + backdrop */
    REG_BLDY = fade < 0 ? 0 : (fade > 16 ? 16 : fade);
}

void load_ui(void) {
    copy16(CHARBLOCK(2), ui_tiles, sizeof(ui_tiles) / 2);
    copy16(PAL_BG + 15 * 16, ui_pal, 16);
}

void load_sprites(void) {
    copy16(OBJ_VRAM, obj_tiles, sizeof(obj_tiles) / 2);
    copy16(PAL_OBJ, obj_pals, sizeof(obj_pals) / 2);
}

void load_theme_bg(int t) {
    const ThemeDef *th = &theme_defs[t];
    copy16(CHARBLOCK(1), th->far_tiles, th->far_n * 16);
    copy16(SCREENBLOCK(28), th->far_map, 1024);
    copy16(PAL_BG + 4 * 16, th->far_pal, 16);
    copy16(CHARBLOCK(2) + MID_TILE_BASE * 16, th->mid_tiles, th->mid_n * 16);
    for (int y = 0; y < 32; y++) {
        copy16(SCREENBLOCK(26) + y * 32, th->mid_map + y * 64, 32);
        copy16(SCREENBLOCK(27) + y * 32, th->mid_map + y * 64 + 32, 32);
    }
    copy16(PAL_BG + 5 * 16, th->mid_pal, 16);
}

void load_terrain(int t) {
    const ThemeDef *th = &theme_defs[t];
    copy16(CHARBLOCK(0), th->terrain_tiles, th->terrain_n * 16);
    copy16(PAL_BG, th->terrain_pal, 16);
    PAL_BG[0] = th->far_pal[1];
}

/* ---------------------------------------------------------------- OAM */
void oam_begin(void) { oam_n = 0; }

int oam_add(u16 a0, u16 a1, u16 a2) {
    if (oam_n >= 128) return -1;
    u16 *o = &oam_buf[oam_n * 4];
    o[0] = a0; o[1] = a1; o[2] = a2;
    return oam_n++;
}

void oam_affine(int aff, s16 pa, s16 pb, s16 pc, s16 pd) {
    oam_buf[aff * 16 + 3] = (u16)pa;
    oam_buf[aff * 16 + 7] = (u16)pb;
    oam_buf[aff * 16 + 11] = (u16)pc;
    oam_buf[aff * 16 + 15] = (u16)pd;
}

void oam_commit(void) {
    for (int i = oam_n; i < 128; i++) oam_buf[i * 4] = 0x0200;
    copy16(OAM, oam_buf, 512);
}

#define PRIO (1 << 10)

int spr16(int x, int y, int tile, int pal, int hflip) {
    if (x <= -16 || x >= 240 || y <= -16 || y >= 160) return -1;
    return oam_add((y & 0xFF), (x & 0x1FF) | (1 << 14) | (hflip ? 0x1000 : 0), tile | PRIO | (pal << 12));
}

int spr8(int x, int y, int tile, int pal) {
    if (x <= -8 || x >= 240 || y <= -8 || y >= 160) return -1;
    return oam_add((y & 0xFF), (x & 0x1FF), tile | PRIO | (pal << 12));
}

int spr32(int x, int y, int tile, int pal, int hflip) {
    if (x <= -32 || x >= 240 || y <= -32 || y >= 160) return -1;
    return oam_add((y & 0xFF), (x & 0x1FF) | (2 << 14) | (hflip ? 0x1000 : 0), tile | PRIO | (pal << 12));
}

int spr16x32(int x, int y, int tile, int pal, int hflip) {
    if (x <= -16 || x >= 240 || y <= -32 || y >= 160) return -1;
    return oam_add((y & 0xFF) | (2 << 14), (x & 0x1FF) | (2 << 14) | (hflip ? 0x1000 : 0), tile | PRIO | (pal << 12));
}

/* Mochi: 32x32 affine double-size sprite. (sx,sy) = feet centre on screen.
   scale in 8.8 (256 = 1.0). */
void spr_mochi(int sx, int sy, int frame, int scx, int scy, int flip, int aff) {
    if (scx < 64) scx = 64;
    if (scy < 64) scy = 64;
    int cy = sy - ((15 * scy) >> 8);
    int x = sx - 32, y = cy - 32;
    if (x <= -64 || x >= 240 || y <= -64 || y >= 160) return;
    s16 pa = (s16)udiv(65536, scx);
    s16 pd = (s16)udiv(65536, scy);
    if (flip) pa = -pa;
    oam_affine(aff, pa, 0, 0, pd);
    oam_add((y & 0xFF) | 0x0300, (x & 0x1FF) | (aff << 9) | (2 << 14),
            (SPR_MOCHI + frame * 16) | PRIO | (0 << 12));
}

/* ---------------------------------------------------------------- text (BG0, SBB 30) */
#define TXT SCREENBLOCK(30)

void text_clear(void) { fill16(TXT, 0, 1024); }

void text_clear_rows(int y0, int y1) {
    for (int y = y0; y <= y1; y++) fill16(TXT + (y & 31) * 32, 0, 32);
}

void text_tile(int x, int y, u16 tile) { TXT[(y & 31) * 32 + (x & 31)] = tile | (15 << 12); }

int str_len(const char *s) { int n = 0; while (s[n]) n++; return n; }

void text_put(int x, int y, const char *s, int panel) {
    u16 base = panel ? UI_PANFONT : UI_HUDFONT;
    while (*s) {
        u8 c = (u8)*s++;
        text_tile(x++, y, base + font_map[c & 127]);
    }
}

void text_center(int y, const char *s, int panel) {
    text_put((30 - str_len(s)) >> 1, y, s, panel);
}

void text_num(int x, int y, int n, int width, int panel) {
    char buf[8];
    int i = 7;
    buf[7] = 0;
    if (n < 0) n = 0;
    do {
        int q = (n * 0xCCCD) >> 19;
        buf[--i] = '0' + (n - q * 10);
        n = q;
    } while (n && i > 0);
    while (7 - i < width && i > 0) buf[--i] = panel == 2 ? ' ' : '0';
    text_put(x, y, &buf[i], panel == 2 ? 1 : panel);
}

void panel_draw(int x, int y, int w, int h) {
    for (int j = 0; j < h; j++) {
        int r = j == 0 ? 0 : (j == h - 1 ? 2 : 1);
        for (int i = 0; i < w; i++) {
            int c = i == 0 ? 0 : (i == w - 1 ? 2 : 1);
            text_tile(x + i, y + j, UI_PANEL0 + r * 3 + c);
        }
    }
}
