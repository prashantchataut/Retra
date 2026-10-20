/* Turbo Drift - video helpers: frame sync, OAM shadow, BG0 text, fades. */
#include "game.h"

u16 key_cur, key_prev, key_rep;
u32 frame_count;
int fade = 16;
int menu_dim;
static u8 rep_t;
static int oam_n;
static int mat_next;

void *memset(void *d, int c, unsigned n) { u8 *p = d; while (n--) *p++ = (u8)c; return d; }
void *memcpy(void *d, const void *s, unsigned n) { u8 *p = d; const u8 *q = s; while (n--) *p++ = *q++; return d; }

static u32 rng_state = 0x2545F491;
void rnd_seed(u32 s) { rng_state = s ? s : 0x2545F491; }
u32 rnd(void) {
    rng_state ^= rng_state << 13;
    rng_state ^= rng_state >> 17;
    rng_state ^= rng_state << 5;
    return rng_state;
}

void copy16(volatile void *dst, const void *src, u32 count) {
    if ((((u32)dst | (u32)src) & 3) == 0) {
        volatile u32 *d = (volatile u32 *)dst;
        const u32 *s = (const u32 *)src;
        u32 n = count >> 1;
        while (n--) *d++ = *s++;
        if (count & 1) *(volatile u16 *)d = *(const u16 *)s;
        return;
    }
    volatile u16 *d = dst;
    const u16 *s = src;
    while (count--) *d++ = *s++;
}

void fill16(volatile void *dst, u16 val, u32 count) {
    volatile u16 *d = dst;
    while (count--) *d++ = val;
}

/* queue a DMA copy to run at the next VBlank (keeps VRAM streaming tear-free) */
void vb_copy(volatile void *dst, const void *src, u32 bytes) {
    for (int i = 0; i < 4; i++) {
        if (!vb_words[i]) {
            vb_src[i] = src;
            vb_dst[i] = dst;
            vb_words[i] = bytes >> 2;
            return;
        }
    }
}

static inline void vblank_wait(void) {
    __asm__ volatile("swi 0x05" ::: "r0", "r1", "r2", "r3", "memory");
}

void video_init(void) {
    REG_WAITCNT = 0x4317;
    REG_DISPCNT = 0x0080;
    REG_BLDCNT = 0x00FF;
    REG_BLDY = 16;
    fill16(VRAM, 0, 0x18000 / 2);
    fill16(PAL_BG, 0, 512);
    for (int i = 0; i < 128; i++) oam_shadow[i * 4] = 0x0200;
    copy16(OAM, oam_shadow, 512);
    IRQ_VECTOR = (u32)irq_handler;
    REG_DISPSTAT = 0x0008;
    REG_IE = IRQ_VBLANK;
    REG_IF = 0xFFFF;
    REG_IME = 1;
}

void poll_keys(void) {
    key_prev = key_cur;
    key_cur = (~REG_KEYINPUT) & 0x3FF;
    key_rep = key_cur & ~key_prev;
    if (key_cur & 0xF0) {
        if (key_cur != key_prev) rep_t = 0;
        else if (++rep_t >= 16) { rep_t = 12; key_rep |= key_cur & 0xF0; }
    } else {
        rep_t = 0;
    }
}

void frame_end(void) {
    frame_ready = 1;
    do { vblank_wait(); } while (frame_ready);
    frame_count++;
    poll_keys();
}

void fade_apply(void) {
    if (fade > 0) {
        REG_BLDCNT = 0x00FF;
        REG_BLDY = fade > 16 ? 16 : fade;
        REG_WININ = 0x3F3F;
        REG_WINOUT = 0x3F;
    } else if (menu_dim > 0) {
        REG_BLDCNT = 0x00FE;
        REG_BLDY = menu_dim;
        REG_WININ = 0x1F1F;
        REG_WINOUT = 0x3F;
    } else {
        REG_BLDCNT = 0;
        REG_WININ = 0x3F3F;
        REG_WINOUT = 0x3F;
    }
}

/* ---------------------------------------------------------------- OAM */
void oam_begin(void) { oam_n = 0; }
int oam_count(void) { return oam_n; }

int oam_add(u16 a0, u16 a1, u16 a2) {
    if (oam_n >= 128) return -1;
    u16 *o = &oam_shadow[oam_n * 4];
    o[0] = a0; o[1] = a1; o[2] = a2;
    return oam_n++;
}

void oam_end(void) {
    for (int i = oam_n; i < 128; i++) oam_shadow[i * 4] = 0x0200;
}

void mat_set(int i, s16 pa, s16 pb, s16 pc, s16 pd) {
    oam_shadow[i * 16 + 3] = (u16)pa;
    oam_shadow[i * 16 + 7] = (u16)pb;
    oam_shadow[i * 16 + 11] = (u16)pc;
    oam_shadow[i * 16 + 15] = (u16)pd;
}

void mats_reset(void) {
    for (int i = 0; i < 16; i++) mat_set(i, (s16)quant_pa[i], 0, 0, (s16)quant_pa[i]);
    mat_next = 16;
}

int mat_alloc(void) {
    if (mat_next >= 30) return -1;
    return mat_next++;
}

/* ---------------------------------------------------------------- text (BG0, SBB 30) */
#define TXT SCREENBLOCK(30)

void ui_load(void) {
    copy16(CHARBLOCK(2), ui_tiles, sizeof(ui_tiles) / 2);
    copy16(PAL_BG + PAL_MAIN * 16, ui_pal_main, 16);
    copy16(PAL_BG + PAL_HI * 16, ui_pal_hi, 16);
    copy16(PAL_BG + PAL_DIM * 16, ui_pal_dim, 16);
    copy16(PAL_BG + PAL_LOGO * 16, logo_pal, 16);
}

void text_clear(void) { fill16(TXT, 0, 1024); }

static inline int on_panel(u16 e) {
    u16 t = e & 0x3FF;
    return (t >= UI_PANEL && t < UI_PANEL + 9) || (t >= UI_PFONT && t < UI_PFONT + UI_NFONT);
}

void text_clear_rect(int x, int y, int w, int h) {
    for (int j = 0; j < h; j++)
        for (int i = 0; i < w; i++) {
            volatile u16 *p = &TXT[((y + j) & 31) * 32 + ((x + i) & 31)];
            u16 t = *p & 0x3FF;
            *p = (t >= UI_PFONT && t < UI_PFONT + UI_NFONT) ? (UI_PANEL + 4) | (PAL_MAIN << 12) : 0;
        }
}

void text_tile(int x, int y, u16 tile, int pal) {
    TXT[(y & 31) * 32 + (x & 31)] = tile | (pal << 12);
}

int str_len(const char *s) { int n = 0; while (s[n]) n++; return n; }

void text_put(int x, int y, const char *s, int pal) {
    while (*s) {
        u8 c = (u8)*s++;
        volatile u16 *p = &TXT[(y & 31) * 32 + (x & 31)];
        u16 base = on_panel(*p) ? UI_PFONT : UI_FONT;
        *p = (base + font_map[c & 127]) | (pal << 12);
        x++;
    }
}

void text_center(int y, const char *s, int pal) { text_put((30 - str_len(s)) >> 1, y, s, pal); }

void fmt_num(char *buf, u32 n) {
    char tmp[12];
    int i = 0;
    do { u32 q = udiv32(n, 10); tmp[i++] = (char)('0' + (n - q * 10)); n = q; } while (n);
    int j = 0;
    while (i) buf[j++] = tmp[--i];
    buf[j] = 0;
}

void text_num(int x, int y, u32 n, int width, int pal, int zero) {
    char buf[12];
    fmt_num(buf, n);
    int l = str_len(buf);
    for (int i = l; i < width; i++) text_tile(x++, y, UI_FONT + font_map[zero ? '0' : ' '], pal);
    text_put(x, y, buf, pal);
}

void text_time(int x, int y, u32 frames, int pal) {
    u32 cs = udiv32(frames * 5, 3);         /* centiseconds */
    u32 s = udiv32(cs, 100);
    u32 c = cs - s * 100;
    u32 m = udiv32(s, 60);
    s -= m * 60;
    if (m > 9) { m = 9; s = 59; c = 99; }
    char b[8];
    b[0] = (char)('0' + m);
    b[1] = ':';
    b[2] = (char)('0' + udiv32(s, 10));
    b[3] = (char)('0' + s - udiv32(s, 10) * 10);
    b[4] = '.';
    b[5] = (char)('0' + udiv32(c, 10));
    b[6] = (char)('0' + c - udiv32(c, 10) * 10);
    b[7] = 0;
    text_put(x, y, b, pal);
}

void text_big_num(int x, int y, u32 n, int digits, int pal) {
    for (int i = digits - 1; i >= 0; i--) {
        u32 q = udiv32(n, 10);
        int d = (int)(n - q * 10);
        n = q;
        if (q == 0 && d == 0 && i < digits - 1) {
            text_tile(x + i, y, 0, pal);
            text_tile(x + i, y + 1, 0, pal);
        } else {
            text_tile(x + i, y, UI_BIG + d * 2, pal);
            text_tile(x + i, y + 1, UI_BIG + d * 2 + 1, pal);
        }
    }
}

void panel_draw(int x, int y, int w, int h) {
    for (int j = 0; j < h; j++) {
        int r = j == 0 ? 0 : (j == h - 1 ? 2 : 1);
        for (int i = 0; i < w; i++) {
            int c = i == 0 ? 0 : (i == w - 1 ? 2 : 1);
            text_tile(x + i, y + j, UI_PANEL + r * 3 + c, PAL_MAIN);
        }
    }
}
