/* Turbo Drift - VRAM loading per environment, scaled world sprites, particles. */
#include "game.h"
#include "render.h"

static const u16 car_mip_tile[4] = { 0, 32, 40, 42 };
static const u8 car_mip_shape[4] = { 1, 1, 1, 0 };
static const u8 car_mip_size[4] = { 3, 2, 0, 0 };
static const u8 car_mip_w[4] = { 64, 32, 16, 8 };
static const u8 car_mip_h[4] = { 32, 16, 8, 8 };
static const u16 van_mip_tile[4] = { 0, 64, 80, 84 };
static const u8 van_mip_shape[4] = { 0, 0, 0, 0 };
static const u8 van_mip_size[4] = { 3, 2, 1, 0 };
static const u8 van_mip_w[4] = { 64, 32, 16, 8 };
static const u8 van_mip_h[4] = { 64, 32, 16, 8 };

int cur_env = -1;

void make_car_pal(volatile u16 *dst, const u16 *ramp, int driver) {
    for (int i = 0; i < 16; i++) dst[i] = car_pal_base[i];
    for (int i = 0; i < 4; i++) dst[4 + i] = ramp[i];
    if (!driver) { dst[14] = car_pal_base[8]; dst[15] = car_pal_base[8]; }
}

void race_load_env(int env, int menu) {
    const EnvDef *e = &env_defs[env];
    road_on = 0;
    REG_DISPCNT = 0x0080;
    copy16(CHARBLOCK(0), e->road_tiles, e->road_tiles_len);
    copy16(SCREENBLOCK(24), e->road_map, 512);
    copy16(PAL_BG + 1, e->road_pal, e->road_pal_len);
    copy16(CHARBLOCK(1), e->skyline_tiles, e->skyline_tiles_len);
    fill16(SCREENBLOCK(26), 0, 2048);
    for (int y = 0; y < SKY_H / 8; y++) {
        copy16(SCREENBLOCK(26) + y * 32, e->skyline_map + y * 64, 32);
        copy16(SCREENBLOCK(27) + y * 32, e->skyline_map + y * 64 + 32, 32);
    }
    copy16(PAL_BG + 10 * 16, e->skyline_pal, 16);
    ui_load();
    if (!menu) copy16(CHARBLOCK(2) + UI_EXTRA * 16, banner_tiles, sizeof(banner_tiles) / 2);
    else copy16(CHARBLOCK(2) + UI_EXTRA * 16, logo_tiles, sizeof(logo_tiles) / 2);
    text_clear();
    /* sprites */
    copy16(OBJ_VRAM + T_RIVAL * 16, rival_tiles, sizeof(rival_tiles) / 2);
    copy16(OBJ_VRAM + T_TRAFFIC * 16, traffic_tiles, sizeof(traffic_tiles) / 2);
    copy16(OBJ_VRAM + T_SIGNS * 16, sign_tiles, sizeof(sign_tiles) / 2);
    copy16(OBJ_VRAM + T_SCENERY * 16, scen_tiles[env], scen_tiles_len[env]);
    copy16(OBJ_VRAM + T_FX * 16, fx_tiles, sizeof(fx_tiles) / 2);
    copy16(OBJ_VRAM + T_HUD * 16, hud_tiles, sizeof(hud_tiles) / 2);
    copy16(PAL_OBJ + 9 * 16, scen_pals[env], 64);
    copy16(PAL_OBJ + 13 * 16, fx_pal, 16);
    copy16(PAL_OBJ + 14 * 16, hudspr_pal, 16);
    copy16(PAL_OBJ + 15 * 16, sign_pal, 16);
    for (int t = 0; t < 3; t++) make_car_pal(PAL_OBJ + (6 + t) * 16, traffic_ramps + t * 4, 0);
    REG_BGCNT(0) = 0x1E08;
    REG_BGCNT(1) = 0x5A07;
    REG_BGCNT(2) = 0x5802;
    REG_BGHOFS(0) = 0;
    REG_BGVOFS(0) = 0;
    memset(line_regs, 0, sizeof(line_regs));
    cur_env = env;
}

/* Scaled sprite with mip selection.  s = scale relative to mip 0 (Q8).
   Anchor is the bottom centre of the art at (sx, sy). */
int spr_scaled(const u16 *tile, const u8 *shape, const u8 *size, const u8 *ww, const u8 *hh,
               int sx, int sy, int s, int pal, int flip, int prio) {
    if (s < 16) return -1;
    int mip, q;
    if (s >= 128) { mip = 0; q = s; }
    else if (s >= 64) { mip = 1; q = s << 1; }
    else if (s >= 32) { mip = 2; q = s << 2; }
    else { mip = 3; q = s << 3; }
    if (q > 500) q = 500;
    int w = ww[mip], h = hh[mip];
    int dbl = q > 255;
    int cy = sy - ((h * q) >> 9);
    int x = sx - (dbl ? w : (w >> 1));
    int y = cy - (dbl ? h : (h >> 1));
    int bw = dbl ? w << 1 : w, bh = dbl ? h << 1 : h;
    if (x >= 240 || x + bw <= 0 || y >= 160 || y + bh <= 0) return -1;
    int m = -1;
    if (dbl || flip) {
        m = mat_alloc();
        if (m >= 0) {
            s16 pa = (s16)inv_scale[q];
            mat_set(m, flip ? -pa : pa, 0, 0, pa);
        } else if (dbl) {
            return -1;
        }
    }
    if (m < 0) m = quant_idx[q - 128];
    return oam_add((u16)((y & 255) | 0x100 | (dbl ? 0x200 : 0) | (shape[mip] << 14)),
                   (u16)((x & 511) | (m << 9) | (size[mip] << 14)),
                   (u16)(tile[mip] | (prio << 10) | (pal << 12)));
}

int spr_object(const ObjDef *od, int sx, int sy, int s, int pal, int flip) {
    return spr_scaled(od->tile, od->shape, od->size, od->w, od->h, sx, sy, s, pal, flip, 1);
}

int spr_car(int kind, int frame_tile, int sx, int sy, int s, int pal, int flip) {
    u16 t[4];
    if (kind == 0) {
        for (int i = 0; i < 4; i++) t[i] = (u16)(frame_tile + car_mip_tile[i]);
        return spr_scaled(t, car_mip_shape, car_mip_size, car_mip_w, car_mip_h, sx, sy, s, pal, flip, 1);
    }
    for (int i = 0; i < 4; i++) t[i] = (u16)(frame_tile + van_mip_tile[i]);
    return spr_scaled(t, van_mip_shape, van_mip_size, van_mip_w, van_mip_h, sx, sy, s, pal, flip, 1);
}

/* ---------------------------------------------------------------- particles (screen space) */
typedef struct { s16 x, y, vx, vy; u8 life, max, kind, pad; } Particle;
#define MAX_PART 28
static Particle parts[MAX_PART];

void parts_clear(void) { memset(parts, 0, sizeof(parts)); }

void part_spawn(int kind, int x, int y, int vx, int vy, int life) {
    for (int i = 0; i < MAX_PART; i++) {
        if (!parts[i].life) {
            Particle *p = &parts[i];
            p->x = (s16)(x << 4); p->y = (s16)(y << 4);
            p->vx = (s16)vx; p->vy = (s16)vy;
            p->life = p->max = (u8)life;
            p->kind = (u8)kind;
            return;
        }
    }
}

void parts_update(void) {
    for (int i = 0; i < MAX_PART; i++) {
        Particle *p = &parts[i];
        if (!p->life) continue;
        p->x += p->vx;
        p->y += p->vy;
        if (p->kind == PART_SPARK) p->vy += 6;
        p->life--;
    }
}

void parts_draw(void) {
    for (int i = 0; i < MAX_PART; i++) {
        Particle *p = &parts[i];
        if (!p->life) continue;
        int x = p->x >> 4, y = p->y >> 4;
        int age = p->max - p->life;
        switch (p->kind) {
        case PART_SPARK:
            if (x > -8 && x < 240 && y > -8 && y < 160)
                oam_add((u16)((y - 4) & 255), (u16)((x - 4) & 511), (u16)((FX_SPARK + ((age >> 1) & 1)) | (1 << 10) | (13 << 12)));
            break;
        case PART_SMOKE: {
            int f = age * 3 / p->max;
            if (f > 2) f = 2;
            if (p->life < 4 && (p->life & 1)) break;
            oam_add((u16)(((y - 8) & 255)), (u16)(((x - 8) & 511) | (1 << 14)), (u16)((FX_SMOKE + f * 4) | (1 << 10) | (13 << 12)));
            break;
        }
        case PART_DUST: {
            int f = age * 2 / p->max;
            if (f > 1) f = 1;
            oam_add((u16)(((y - 8) & 255)), (u16)(((x - 8) & 511) | (1 << 14)), (u16)((FX_DUST + f * 4) | (1 << 10) | (13 << 12)));
            break;
        }
        case PART_STREAK:
            oam_add((u16)(((y - 16) & 255) | (2 << 14)), (u16)(((x - 4) & 511) | (1 << 14)), (u16)(FX_STREAK | (0 << 10) | (13 << 12)));
            break;
        }
    }
}
