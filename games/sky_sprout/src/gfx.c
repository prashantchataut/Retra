/* Sky Sprout - video: frame pacing, OAM staging, text, banners, fades, HDMA parallax. */
#include "game.h"

#define REG_TM0CNT_L REG16(0x04000100)
#define REG_TM0CNT_H REG16(0x04000102)

u16 key_cur, key_prev, key_rep;
u32 frame_count;
int fade = 16, flash;
int shake_x, shake_y;
u32 scroll_dist;
int bg_shift_y;

static u16 oam_buf[512] __attribute__((aligned(4)));
static u16 disp_pending = 0x0080;

/* OAM staging: categories in display order; caps sum to 288 entries. */
const u8 oc_cap[OC_N] = { 12, MAX_EBUL, 12, 10, 48, 48, 48, 32 };
const u16 oc_base[OC_N] = { 0, 12, 12 + MAX_EBUL, 24 + MAX_EBUL, 34 + MAX_EBUL, 82 + MAX_EBUL,
                            130 + MAX_EBUL, 178 + MAX_EBUL };
#define OSTAGE_N (210 + MAX_EBUL)
u16 ostage[OSTAGE_N * 3] __attribute__((aligned(4)));
u8 oc_n[OC_N];
static const u8 keep_order[OC_N] = { OC_TOP, OC_PLAYER, OC_BOSS, OC_ENEMY, OC_EBUL, OC_ITEM, OC_SHOT, OC_FX };

const u8 ss_w[12] = { 8, 16, 32, 64, 16, 32, 32, 64, 8, 8, 16, 32 };
const u8 ss_h[12] = { 8, 16, 32, 64, 8, 8, 16, 32, 16, 32, 32, 64 };

#ifdef DEBUG_PERF
int perf_max, perf_last, perf_over;
static int perf_sum, perf_frames;
#define DBG_ENABLE REG16(0x04FFF780)
#define DBG_FLAGS  REG16(0x04FFF700)
#define DBG_STR    ((volatile char *)0x04FFF600)
static void dbg_print(const char *s) {
    DBG_ENABLE = 0xC0DE;
    int i = 0;
    while (s[i] && i < 255) { DBG_STR[i] = s[i]; i++; }
    DBG_STR[i] = 0;
    DBG_FLAGS = 0x100 | 2;
}
static char *dbg_num(char *p, int n) {
    char tmp[10];
    int i = 0;
    if (n < 0) { *p++ = '-'; n = -n; }
    do { tmp[i++] = '0' + n % 10; n /= 10; } while (n);
    while (i) *p++ = tmp[--i];
    return p;
}
#endif

/* gcc may emit calls to these for struct copies/clears */
void *memset(void *d, int c, unsigned n) { u8 *p = d; while (n--) *p++ = (u8)c; return d; }
void *memcpy(void *d, const void *s, unsigned n) { u8 *p = d; const u8 *q = s; while (n--) *p++ = *q++; return d; }

static u32 rng_state = 0x2F6B1D3;
u32 rnd(void) {
    rng_state ^= rng_state << 13;
    rng_state ^= rng_state >> 17;
    rng_state ^= rng_state << 5;
    return rng_state;
}
void rnd_seed(u32 s) { rng_state = s ? s : 0x2F6B1D3; }

int udiv(int a, int b) {
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

void copy16(volatile u16 *dst, const u16 *src, u32 count) {
    if ((((u32)dst | (u32)src) & 3) == 0 && count >= 2) {
        copy32(dst, src, (count & ~1u) * 2);
        if (count & 1) dst[count - 1] = src[count - 1];
        return;
    }
    while (count--) *dst++ = *src++;
}

void copy32(volatile void *dst, const void *src, u32 bytes) {
    if (!bytes) return;
    REG_DMA3SAD = (u32)src;
    REG_DMA3DAD = (u32)dst;
    REG_DMA3CNT = DMA_NOW_U32 | (bytes >> 2);
}

void fill16(volatile u16 *dst, u16 val, u32 count) {
    while (count--) *dst++ = val;
}

void poll_keys(void) {
    static u8 rep_t;
    key_prev = key_cur;
    key_cur = (~REG_KEYINPUT) & 0x3FF;
    u16 dirs = key_cur & 0xF0;
    key_rep = key_cur & ~key_prev;
    if (dirs && dirs == (key_prev & 0xF0)) {
        if (++rep_t >= 18) { key_rep |= dirs; rep_t = 13; }
    } else {
        rep_t = 0;
    }
}

void set_display(u16 d) { disp_pending = d; }

void video_init(void) {
    REG_WAITCNT = 0x4317;
    REG_DISPCNT = 0x0080;
    REG_BLDCNT = 0x00FF;
    REG_BLDY = 16;
    fill16(VRAM, 0, 0x18000 / 2);
    fill16(PAL_BG, 0, 512);
    for (int i = 0; i < 128; i++) { oam_buf[i * 4] = 0x0200; oam_buf[i * 4 + 3] = 0; }
    copy16(OAM, oam_buf, 512);
    REG_TM0CNT_H = 0;
    REG_TM0CNT_L = 0;
    REG_TM0CNT_H = 0x0081;   /* enable, F/64 */
}

/* ---------------------------------------------------------------- OAM */
void oam_clear(void) {
    for (int i = 0; i < OC_N; i++) oc_n[i] = 0;
}

static inline int oc_push(int cat, u16 a0, u16 a1, u16 a2) {
    int n = oc_n[cat];
    if (n >= oc_cap[cat]) return -1;
    u16 *o = &ostage[(oc_base[cat] + n) * 3];
    o[0] = a0; o[1] = a1; o[2] = a2;
    oc_n[cat] = n + 1;
    return n;
}

int spr(int cat, int x, int y, int tile, int ss, int pal, int flags) {
    x += shake_x;
    y += shake_y;
    int w = ss_w[ss], h = ss_h[ss];
    if (x <= -w || x >= 240 || y <= -h || y >= 160) return -1;
    u16 a0 = (y & 0xFF) | ((ss >> 2) << 14) | ((flags & SPR_SEMI) ? 0x0400 : 0);
    u16 a1 = (x & 0x1FF) | ((ss & 3) << 14) | (flags & (SPR_HF | SPR_VF));
    return oc_push(cat, a0, a1, tile | (1 << 10) | (pal << 12));
}

int spr_aff(int cat, int x, int y, int tile, int ss, int pal, int aff, int dbl) {
    x += shake_x;
    y += shake_y;
    int w = ss_w[ss] << dbl, h = ss_h[ss] << dbl;
    if (x <= -w || x >= 240 || y <= -h || y >= 160) return -1;
    u16 a0 = (y & 0xFF) | 0x0100 | (dbl ? 0x0200 : 0) | ((ss >> 2) << 14);
    u16 a1 = (x & 0x1FF) | ((aff & 31) << 9) | ((ss & 3) << 14);
    return oc_push(cat, a0, a1, tile | (1 << 10) | (pal << 12));
}

void oam_affine(int aff, int pa, int pb, int pc, int pd) {
    oam_buf[aff * 16 + 3] = (u16)pa;
    oam_buf[aff * 16 + 7] = (u16)pb;
    oam_buf[aff * 16 + 11] = (u16)pc;
    oam_buf[aff * 16 + 15] = (u16)pd;
}

/* rotation by ang (0..255) with uniform scale (8.8, 256 = 1x) */
void oam_rotscale(int aff, int ang, int scale) {
    int inv = udiv(65536, scale < 32 ? 32 : scale);
    int c = icos(ang), s = isin(ang);
    oam_affine(aff, (c * inv) >> 8, (s * inv) >> 8, (-s * inv) >> 8, (c * inv) >> 8);
}

static void oam_build(void) {
    u8 keep[OC_N];
    int left = 128;
    for (int i = 0; i < OC_N; i++) {
        int c = keep_order[i];
        int k = oc_n[c] < left ? oc_n[c] : left;
        keep[c] = k;
        left -= k;
    }
    int o = 0;
    for (int c = 0; c < OC_N; c++) {
        int n = oc_n[c], k = keep[c];
        if (!k) continue;
        /* rotate the visible subset when trimming so nothing vanishes for long */
        int start = (k < n) ? (int)((frame_count * 7) % n) : 0;
        const u16 *base = &ostage[oc_base[c] * 3];
        for (int i = 0; i < k; i++) {
            int j = start + i;
            if (j >= n) j -= n;
            const u16 *s = &base[j * 3];
            u16 *d = &oam_buf[o * 4];
            d[0] = s[0]; d[1] = s[1]; d[2] = s[2];
            o++;
        }
    }
    for (; o < 128; o++) oam_buf[o * 4] = 0x0200;
}

/* ---------------------------------------------------------------- HDMA parallax */
static const StageBG *cur_bg;
static u16 far_line[2][164] __attribute__((aligned(4)));
static u16 near_line[2][164] __attribute__((aligned(4)));
static int hbuf;
static int hdma_on;
static u16 mid_h, mid_v;

void hdma_stop(void) {
    REG_DMA0CNT = 0;
    REG_DMA1CNT = 0;
    REG_DMA2CNT = 0;
    hdma_on = 0;
}

void bg_update(void) {
    if (!cur_bg) return;
    int nb = hbuf ^ 1;
    u32 d = scroll_dist;
    const u16 *ff = cur_bg->far_f, *nf = cur_bg->near_f;
    u16 *fl = far_line[nb], *nl = near_line[nb];
    int sx = shake_x;
    for (int y = 0; y < 160; y++) {
        fl[y] = (u16)(((d * ff[y]) >> 8) - sx);
        nl[y] = (u16)(((d * nf[y]) >> 8) - sx);
    }
    fl[160] = fl[159]; nl[160] = nl[159];
    fl[161] = fl[0]; nl[161] = nl[0];
    mid_h = (u16)((((s32)d * cur_bg->mid_fx) >> 8) - sx);
    mid_v = (u16)(((s32)d * cur_bg->mid_fy) >> 8);
    hbuf = nb;
    hdma_on = 1;
}

static void hdma_restart(void) {
    if (!hdma_on || !cur_bg) return;
    REG_DMA0CNT = 0;
    REG_DMA1CNT = 0;
    REG_DMA2CNT = 0;
    const u16 *g = cur_bg->grad;
    PAL_BG[0] = g[0];
    REG_DMA0SAD = (u32)(g + 1);
    REG_DMA0DAD = (u32)PAL_BG;
    REG_DMA0CNT = DMA_HBLANK_U16;
    REG_BGHOFS(3) = far_line[hbuf][0];
    REG_DMA1SAD = (u32)&far_line[hbuf][1];
    REG_DMA1DAD = 0x0400001C;
    REG_DMA1CNT = DMA_HBLANK_U16;
    REG_BGHOFS(1) = near_line[hbuf][0];
    REG_DMA2SAD = (u32)&near_line[hbuf][1];
    REG_DMA2DAD = 0x04000014;
    REG_DMA2CNT = DMA_HBLANK_U16;
    REG_BGHOFS(2) = mid_h;
    REG_BGVOFS(2) = mid_v + shake_y;
    REG_BGVOFS(1) = shake_y + bg_shift_y;
    REG_BGVOFS(3) = shake_y + bg_shift_y;
}

void blend_setup(void) {
    if (fade > 0) {
        REG_BLDCNT = 0x00FF;
        REG_BLDY = fade > 16 ? 16 : fade;
    } else if (flash > 0) {
        REG_BLDCNT = 0x00AE;          /* brighten BG1-3 + backdrop only: bullets stay readable */
        REG_BLDY = flash > 16 ? 16 : flash;
    } else {
        REG_BLDCNT = 0x2E40;          /* alpha for semi-transparent OBJs over the backgrounds */
        REG_BLDALPHA = 0x0A0A;
        REG_BLDY = 0;
    }
}

/* ---------------------------------------------------------------- frame */
void frame_wait(void) {
#ifdef DEBUG_PERF
    {
        int t = REG_TM0CNT_L;
        int pct = (t * 100) / 4389;
        perf_last = pct;
        if (pct > perf_max) perf_max = pct;
        if (t > 4389) perf_over++;
        perf_sum += pct;
        if (++perf_frames == 120) {
            char buf[96], *p = buf;
            const char *a = "perf avg=";
            while (*a) *p++ = *a++;
            p = dbg_num(p, perf_sum / 120);
            a = "% max=";
            while (*a) *p++ = *a++;
            p = dbg_num(p, perf_max);
            a = "% over=";
            while (*a) *p++ = *a++;
            p = dbg_num(p, perf_over);
            a = " bullets=";
            while (*a) *p++ = *a++;
            p = dbg_num(p, bullets_count());
            a = " oam=";
            while (*a) *p++ = *a++;
            int tot = 0;
            for (int i = 0; i < OC_N; i++) tot += oc_n[i];
            p = dbg_num(p, tot);
            *p = 0;
            dbg_print(buf);
            perf_sum = 0;
            perf_frames = 0;
        }
    }
#endif
    oam_build();
    while (REG_VCOUNT >= 160) {}
    while (REG_VCOUNT < 160) {}
    REG_TM0CNT_H = 0;
    REG_TM0CNT_L = 0;
    REG_TM0CNT_H = 0x0081;
    frame_count++;
    copy32(OAM, oam_buf, 1024);
    REG_DISPCNT = disp_pending;
    hdma_restart();
    blend_setup();
    audio_update();
    poll_keys();
}

void fade_out(int speed) {
    while (fade < 16) {
        fade += speed;
        if (fade > 16) fade = 16;
        bg_update();
        frame_wait();
    }
}

void fade_in(int speed) {
    while (fade > 0) {
        fade -= speed;
        if (fade < 0) fade = 0;
        bg_update();
        frame_wait();
    }
}

/* ---------------------------------------------------------------- loading */
void load_ui(void) {
    copy16(CHARBLOCK(2), ui_tiles, sizeof(ui_tiles) / 2);
    copy16(PAL_BG + 15 * 16, ui_pal, 16);
    copy16(PAL_BG + BP_GOLD * 16, banner_pals + 0, 16);
    copy16(PAL_BG + BP_PINK * 16, banner_pals + 16, 16);
    copy16(PAL_BG + BP_RED * 16, banner_pals + 32, 16);
    copy16(PAL_BG + BP_SKY * 16, banner_pals + 48, 16);
}

void load_common_sprites(void) {
    copy16(OBJ_VRAM, obj_tiles, sizeof(obj_tiles) / 2);
    copy16(PAL_OBJ, obj_pals, 6 * 16);
    copy16(PAL_OBJ + 15 * 16, obj_pals + 6 * 16, 16);
}

void load_stage_sprites(int st) {
    copy16(OBJ_VRAM + STAGE_OBJ_BASE * 16, stage_obj[st].tiles, stage_obj[st].ntiles * 16);
    copy16(PAL_OBJ + 6 * 16, stage_enemy_pals + st * 32, 32);
    copy16(PAL_OBJ + 10 * 16, mid_pals + st * 16, 16);
    copy16(PAL_OBJ + 12 * 16, boss_pals + st * 32, 32);
}

static void load_layer(int sbb, const u16 *map, int size) {
    copy16(SCREENBLOCK(sbb), map, (size & 1) ? 2048 : 1024);
}

void load_stage_bg(int st) {
    const StageBG *b = &stage_bgs[st];
    copy16(CHARBLOCK(0), b->tiles, b->ntiles * 16);
    copy16(PAL_BG, b->pals, 48);
    load_layer(24, b->far_map, b->far_size);
    load_layer(26, b->mid_map, b->mid_size);
    load_layer(28, b->near_map, b->near_size);
    REG_BGCNT(0) = 0x1E08;
    REG_BGCNT(1) = 0x0002 | (28 << 8) | (b->near_size << 14);
    REG_BGCNT(2) = 0x0002 | (26 << 8) | (b->mid_size << 14);
    REG_BGCNT(3) = 0x0003 | (24 << 8) | (b->far_size << 14);
    REG_BGHOFS(0) = 0;
    REG_BGVOFS(0) = 0;
    cur_bg = b;
    scroll_dist = 0;
    bg_update();
}

void load_title_bg(void) {
    load_stage_bg(0);
    /* logo on BG1 (tiles in CBB2 scratch area, map in SBB31) */
    copy16(CHARBLOCK(2) + 256 * 16, logo_tiles, sizeof(logo_tiles) / 2);
    copy16(PAL_BG + 13 * 16, logo_pal, 16);
    copy16(SCREENBLOCK(31), logo_map, 1024);
}

/* ---------------------------------------------------------------- text (BG0, SBB 30) */
#define TXT SCREENBLOCK(30)

void text_clear(void) { fill16(TXT, 0, 1024); }

void text_clear_rows(int y0, int y1) {
    for (int y = y0; y <= y1; y++) fill16(TXT + (y & 31) * 32, 0, 32);
}

void text_tile(int x, int y, u16 tile) { TXT[(y & 31) * 32 + (x & 31)] = tile | (15 << 12); }

int str_len(const char *s) { int n = 0; while (s[n]) n++; return n; }

void text_put(int x, int y, const char *s, int style) {
    while (*s) {
        u8 c = (u8)*s++;
        u16 t;
        if (style == TS_GOLD && c >= '0' && c <= '9') t = UI_GOLD + (c - '0');
        else t = (style == TS_PANEL ? UI_PAN : UI_HUD) + font_map[c & 127];
        if (style != TS_PANEL && c == ' ') t = 0;
        text_tile(x++, y, t);
    }
}

void text_center(int y, const char *s, int style) {
    text_put((30 - str_len(s)) >> 1, y, s, style);
}

void text_num(int x, int y, u32 n, int width, int style, int zero_pad) {
    static const u32 pw[10] = { 1000000000u, 100000000u, 10000000u, 1000000u, 100000u, 10000u, 1000u, 100u, 10u, 1u };
    char buf[12];
    int i = 0, started = 0;
    for (int k = 0; k < 10; k++) {
        int d = 0;
        while (n >= pw[k]) { n -= pw[k]; d++; }
        if (d || started || k == 9 || (10 - k) <= width) {
            if (!d && !started && k != 9 && !zero_pad) buf[i++] = ' ';
            else { buf[i++] = '0' + d; started = 1; }
        }
    }
    buf[i] = 0;
    text_put(x, y, buf + (i > width ? i - width : 0), style);
}

void panel_draw(int x, int y, int w, int h) {
    for (int j = 0; j < h; j++) {
        int r = j == 0 ? 0 : (j == h - 1 ? 2 : 1);
        for (int i = 0; i < w; i++) {
            int c = i == 0 ? 0 : (i == w - 1 ? 2 : 1);
            text_tile(x + i, y + j, UI_PANEL + r * 3 + c);
        }
    }
}

/* ---------------------------------------------------------------- banners */
int banner_width(int id) { return banner_w[id]; }

void banner_show(int slot, int id, int y, int pal) {
    int w = banner_w[id];
    int base = 256 + slot * 128;
    copy16(CHARBLOCK(2) + base * 16, banner_tiles + banner_off[id] * 16, w * 2 * 16);
    int x0 = (30 - w) >> 1;
    for (int r = 0; r < 2; r++)
        for (int i = 0; i < w; i++)
            TXT[((y + r) & 31) * 32 + ((x0 + i) & 31)] = (base + r * w + i) | (pal << 12);
}

void banner_clear(int y) { text_clear_rows(y, y + 1); }
