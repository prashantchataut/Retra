/* Turbo Drift - hot code in IWRAM (compiled as ARM): VBlank ISR and the
   raster road projector that fills per-scanline register tables which two
   HBlank DMA channels feed to the PPU (BG1 scroll + BG2 affine + backdrop). */
#include "game.h"

LineRegs line_regs[2][161] __attribute__((aligned(4)));
u16 line_bd[2][162] __attribute__((aligned(4)));
volatile int back_buf;
volatile int frame_ready;
volatile u32 vbl_count;
volatile int road_on;
SegScr segscr[DRAW_DIST + 2];
int road_top;

/* deferred VBlank jobs set by the main loop */
volatile const void *vb_src[4];
volatile void *vb_dst[4];
volatile u32 vb_words[4];
u16 oam_shadow[512] __attribute__((aligned(4)));

static inline void dma3_words(volatile void *dst, const volatile void *src, u32 words) {
    REG_DMA3CNT = 0;
    REG_DMA3SAD = (u32)src;
    REG_DMA3DAD = (u32)dst;
    REG_DMA3CNT = words | DMA_ENABLE | DMA_32;
}

IWRAM_CODE void irq_handler(void) {
    u16 f = REG_IF & REG_IE;
    if (f & IRQ_VBLANK) {
        int front = back_buf ^ 1;
        if (frame_ready) {
            front = back_buf;
            back_buf ^= 1;
            frame_ready = 0;
            dma3_words(OAM, oam_shadow, 256);
            for (int i = 0; i < 4; i++) {
                if (vb_words[i]) {
                    dma3_words(vb_dst[i], vb_src[i], vb_words[i]);
                    vb_words[i] = 0;
                }
            }
        }
        REG_DMA0CNT = 0;
        REG_DMA2CNT = 0;
        if (road_on) {
            const u32 *s = (const u32 *)&line_regs[front][0];
            volatile u32 *d = (volatile u32 *)0x04000014;
            d[0] = s[0]; d[1] = s[1]; d[2] = s[2]; d[3] = s[3]; d[4] = s[4]; d[5] = s[5]; d[6] = s[6];
            PAL_BG[0] = line_bd[front][0];
            REG_DMA0SAD = (u32)&line_regs[front][1];
            REG_DMA0DAD = 0x04000014;
            REG_DMA0CNT = 7 | DMA_ENABLE | DMA_HBLANK | DMA_REPEAT | DMA_32 | DMA_DST_RELOAD;
            REG_DMA2SAD = (u32)&line_bd[front][1];
            REG_DMA2DAD = 0x05000000;
            REG_DMA2CNT = 1 | DMA_ENABLE | DMA_HBLANK | DMA_REPEAT | DMA_DST_FIX;
        }
        vbl_count++;
        audio_update();
    }
    REG_IF = f;
    BIOS_IF |= f;
}

IWRAM_CODE u32 udiv32(u32 n, u32 d) {
    u32 q = 0, bit = 1;
    if (!d) return 0xFFFFFFFFu;
    while (d < n && !(d & 0x80000000u)) { d <<= 1; bit <<= 1; }
    while (bit) {
        if (n >= d) { n -= d; q |= bit; }
        d >>= 1;
        bit >>= 1;
    }
    return q;
}

IWRAM_CODE s32 sdiv32(s32 n, s32 d) {
    int neg = 0;
    if (n < 0) { n = -n; neg ^= 1; }
    if (d < 0) { d = -d; neg ^= 1; }
    s32 q = (s32)udiv32((u32)n, (u32)d);
    return neg ? -q : q;
}

static inline s32 mulshift(s32 a, s32 b, int sh) {
    return (s32)(((s64)a * b) >> sh);
}

IWRAM_CODE void road_project(const RoadView *v) {
    LineRegs *L = line_regs[back_buf];
    u16 *BD = line_bd[back_buf];
    const s32 n = v->n;
    const s8 *curve = v->curve;
    const s16 *ty = v->ty;
    s32 base = v->cam_z >> 8;
    s32 frac = v->cam_z & 255;
    while (base >= n) base -= n;
    const s32 focal = v->focal;
    const s32 hz16 = v->horizon << 4;
    const s32 ck = v->curve_k;
    s32 X = 0;
    s32 DX = -((curve[base] * ck * frac) >> 8);
    s32 maxy = 160;
    s32 psx = 0, psy = 0, pw = 0;
    int have_prev = 0;
    s32 seg = base;
    s32 bseg = base + 1;
    if (bseg >= n) bseg -= n;

    for (int k = 1; k <= DRAW_DIST; k++) {
        X += DX;
        DX += curve[seg] * ck;
        s32 dz = (k << 8) - frac;
        if (dz < 24) dz = 24;
        if (dz > RECIP_DZ_MAX) dz = RECIP_DZ_MAX;
        s32 ri = dz >> 4;
        s32 r0 = (s32)recip_dz[ri], r1 = (s32)recip_dz[ri + 1];
        s32 rz = ((r0 - (((r0 - r1) * (dz & 15)) >> 4)) * focal) >> 8;
        s32 sx = (120 << 4) + mulshift((X >> 8) - v->cam_x, rz, 12);
        s32 sy = hz16 - mulshift(ty[bseg] - v->cam_y, rz, 12);
        s32 w = (TEXHALF * rz) >> 12;
        SegScr *ss = &segscr[k];
        s32 cx = sx >> 4, cy = sy >> 4;
        ss->x = (s16)(cx < -30000 ? -30000 : cx > 30000 ? 30000 : cx);
        ss->y = (s16)(cy < -30000 ? -30000 : cy > 30000 ? 30000 : cy);
        ss->w = (s16)(w > 30000 ? 30000 : w);
        ss->clip = (s16)maxy;
        if (have_prev) {
            s32 ytop = (sy + 15) >> 4;
            if (ytop < 0) ytop = 0;
            s32 ybot = (psy + 15) >> 4;
            if (ybot > maxy) ybot = maxy;
            if (ytop < ybot) {
                s32 dyq = psy - sy;
                if (dyq > 2560) dyq = 2560;
                if (dyq < 1) dyq = 1;
                u32 inv = inv_dy[dyq];
                s32 slx = mulshift(psx - sx, (s32)inv, 12);
                s32 slw = mulshift(pw - w, (s32)inv, 12);
                s32 off = (ytop << 4) - sy;
                s32 xq = (sx << 8) + ((off * slx) >> 4);
                s32 wq = (w << 8) + ((off * slw) >> 4);
                /* per-segment look */
                s32 variant, band;
                if (seg < 2) { variant = 2 + (seg & 1); band = 0; }
                else { band = ((seg * 0x5556) >> 16) & 1; variant = band; }
                s32 fz = dz >> 8;
                s32 fog = fog_dz[fz > 255 ? 255 : fz];
                s32 rowy = (((fog * TEX_VARIANTS + variant) << 3) + 4) << 8;
                u16 gc = v->ground[band * FOG_LEVELS + fog];
                for (s32 y = ytop; y < ybot; y++) {
                    s32 w16 = wq >> 8;
                    if (w16 < 1) w16 = 1;
                    if (w16 > 4095) w16 = 4095;
                    s32 pa = recip_w[w16];
                    LineRegs *l = &L[y];
                    l->pa = (s16)pa;
                    l->x = (128 << 8) - (((xq >> 8) * pa) >> 4);
                    l->y = rowy;
                    BD[y] = gc;
                    xq += slx;
                    wq += slw;
                }
                maxy = ytop;
            }
        } else if (((sy + 15) >> 4) < 160) {
            /* nearest boundary already above the bottom (crest): extend it down */
            s32 ytop = (sy + 15) >> 4;
            if (ytop < 0) ytop = 0;
            s32 w16 = w < 1 ? 1 : w > 4095 ? 4095 : w;
            s32 pa = recip_w[w16];
            s32 band = ((seg * 0x5556) >> 16) & 1;
            for (s32 y = ytop; y < 160; y++) {
                L[y].pa = (s16)pa;
                L[y].x = (128 << 8) - ((sx * pa) >> 4);
                L[y].y = ((band << 3) + 4) << 8;
                BD[y] = v->ground[band * FOG_LEVELS];
            }
            maxy = ytop;
        }
        psx = sx; psy = sy; pw = w;
        have_prev = 1;
        seg = bseg;
        if (++bseg >= n) bseg = 0;
    }
    road_top = maxy;
    /* sky lines */
    for (s32 y = 0; y < maxy; y++) {
        s32 d = maxy - 1 - y;
        BD[y] = v->sky[d > 127 ? 127 : d];
        L[y].y = 300 << 8;
        L[y].x = 0;
        L[y].pa = 256;
    }
    /* skyline layer: bottom row sits on the horizon, two parallax bands */
    s32 vofs = SKY_H - maxy;
    s32 h0 = v->sky_x >> v->sky_shift0;
    s32 h1 = v->sky_x >> v->sky_shift1;
    s32 brow = v->band_row - vofs;     /* screen line where band 1 starts */
    for (s32 y = 0; y < 160; y++) {
        L[y].bg1v = (s16)vofs;
        L[y].bg1h = (s16)(y < brow ? h0 : h1);
    }
    L[160] = L[159];
    BD[160] = BD[159];
}

/* Simple full-scanline fill for screens that have no road (menus without attract). */
IWRAM_CODE void lines_flat(u16 bd_top, u16 bd_bottom) {
    LineRegs *L = line_regs[back_buf];
    u16 *BD = line_bd[back_buf];
    for (int y = 0; y <= 160; y++) {
        L[y].y = 300 << 8;
        L[y].x = 0;
        L[y].pa = 256;
        L[y].bg1h = 0;
        L[y].bg1v = 0;
        BD[y] = y < 80 ? bd_top : bd_bottom;
    }
}
