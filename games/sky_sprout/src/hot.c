/* Sky Sprout - hot loops, compiled as ARM code and run from IWRAM.
   They never call ROM functions, so no interworking veneers are needed. */
#include "game.h"

#define IW __attribute__((section(".iwram.hot"), noinline))

Bullet ebul[MAX_EBUL];
Shot shots[MAX_SHOTS];
Enemy en[MAX_ENEMIES];
s16 bhit[3][4];
int bhit_n, boss_dmg, boss_hittable;
int cancel_collected;
s16 hitfx_x[8], hitfx_y[8];
int hitfx_n;

static inline u32 iw_div(u32 a, u32 b) {
    u32 q = 0, bit = 1;
    if (!b) return 0;
    while (b <= a && !(b & 0x80000000u)) { b <<= 1; bit <<= 1; }
    while (bit) {
        if (a >= b) { a -= b; q |= bit; }
        b >>= 1;
        bit >>= 1;
    }
    return q;
}

IW int iatan2(int dy, int dx) {
    if (!dx && !dy) return 0;
    int ax = dx < 0 ? -dx : dx, ay = dy < 0 ? -dy : dy;
    int a;
    if (ax >= ay) a = atan_lut[iw_div((u32)ay << 6, (u32)ax)];
    else a = 64 - atan_lut[iw_div((u32)ax << 6, (u32)ay)];
    if (dx < 0) a = 128 - a;
    if (dy < 0) a = 256 - a;
    return a & 255;
}

static inline void set_vel(Bullet *b) {
    int a = b->ang >> 8;
    b->vx = (s16)((sin_lut[(a + 64) & 255] * b->spd) >> 8);
    b->vy = (s16)((sin_lut[a & 255] * b->spd) >> 8);
}

static inline Bullet *free_bullet(void) {
    for (int i = MAX_EBUL - 1; i >= 0; i--)
        if (!ebul[i].active) return &ebul[i];
    return 0;
}

static inline void emit(int x, int y, int tile, int ss, int pal) {
    int n = oc_n[OC_EBUL];
    if (n >= MAX_EBUL) return;
    x += shake_x;
    y += shake_y;
    u16 *o = &ostage[(oc_base[OC_EBUL] + n) * 3];
    o[0] = (u16)((y & 0xFF) | ((ss >> 2) << 14));
    o[1] = (u16)((x & 0x1FF) | ((ss & 3) << 14));
    o[2] = (u16)(tile | (1 << 10) | (pal << 12));
    oc_n[OC_EBUL] = (u8)(n + 1);
}

IW int bullets_update(int px, int py, int vulnerable) {
    int hit = 0;
    s32 PX = px << 8, PY = py << 8;
    int anim2 = (frame_count >> 2) & 1;
    for (int i = 0; i < MAX_EBUL; i++) {
        Bullet *b = &ebul[i];
        if (!b->active) continue;
        b->t++;
        switch (b->beh) {
        case BEH_ACCEL:
            b->spd += b->p;
            if ((b->p > 0 && b->spd > b->q) || (b->p < 0 && b->spd < b->q)) { b->spd = b->q; b->beh = BEH_LINE; }
            set_vel(b);
            break;
        case BEH_STOPAIM:
            if (b->spd > 0) {
                b->spd -= b->p;
                if (b->spd <= 0) { b->spd = 0; b->t = 0; }
                set_vel(b);
            } else if (b->t >= b->q) {
                b->ang = (u16)(iatan2(PY - b->y, PX - b->x) << 8);
                b->spd = (s16)((G.spdmul * 470) >> 8);
                b->beh = BEH_LINE;
                set_vel(b);
            }
            break;
        case BEH_CURVE:
            b->ang = (u16)(b->ang + b->p);
            if (b->q && b->t >= b->q) b->beh = BEH_LINE;
            set_vel(b);
            break;
        case BEH_GRAV:
            b->vy += b->p;
            if (b->vy > b->q) b->vy = b->q;
            break;
        case BEH_SPLIT:
            if (b->t >= b->q) {
                int n = b->p, a0 = b->t * 5;
                int kind = b->kind >= BK_BM && b->kind <= BK_BA ? (b->kind - BK_BM) : BK_SM;
                b->active = 0;
                for (int k = 0; k < n; k++) {
                    Bullet *c = free_bullet();
                    if (!c) break;
                    c->active = 1;
                    c->x = b->x; c->y = b->y;
                    c->kind = (u8)kind;
                    c->beh = BEH_LINE;
                    c->t = 0;
                    c->ang = (u16)((a0 + iw_div((u32)k * 256, (u32)n)) << 8);
                    c->spd = (s16)((G.spdmul * 300) >> 8);
                    set_vel(c);
                }
                continue;
            }
            break;
        case BEH_CANCEL: {
            int s = 384 + b->t * 40;
            if (s > 2048) s = 2048;
            b->spd = (s16)s;
            b->ang = (u16)(iatan2(PY - b->y, PX - b->x) << 8);
            set_vel(b);
            int dx = (b->x - PX) >> 8, dy = (b->y - PY) >> 8;
            if (dx < 0) dx = -dx;
            if (dy < 0) dy = -dy;
            if ((dx < 10 && dy < 10) || b->t > 90) {
                b->active = 0;
                cancel_collected++;
                continue;
            }
            break;
        }
        default:
            break;
        }
        b->x += b->vx;
        b->y += b->vy;
        int x = b->x >> 8, y = b->y >> 8;
        if (b->beh != BEH_CANCEL && (x < -16 || x > 256 || y < -16 || y > 176)) {
            b->active = 0;
            continue;
        }
        int k = b->kind;
        if (vulnerable && b->beh != BEH_CANCEL) {
            int r = bk_rad[k];
            int dx = x - px, dy = y - py;
            if (dx < 0) dx = -dx;
            if (dy < 0) dy = -dy;
            if (dx <= r && dy <= r && dx + dy <= r + (r >> 1) + 1) {
                hit = 1;
                b->active = 0;
                continue;
            }
        }
        int tile = bk_tile[k], pal = 2;
        if (k >= BK_RM && k <= BK_RV) tile += ((b->ang >> 8) + 8) >> 4 & 7;
        else if (k == BK_STARA || k == BK_STARM) tile += anim2;
        else if (k == BK_PETAL) { tile += anim2; pal = 3; }
        int h = bk_half[k];
        emit(x - h, y - h, tile, bk_ss[k], pal);
    }
    return hit;
}

/* shot hit-box half extents per shot kind */
static const u8 shot_hw[5] = { 4, 3, 16, 3, 3 };
static const u8 shot_hh[5] = { 3, 3, 3, 3, 2 };

IW void shots_collide(void) {
    for (int i = 0; i < MAX_SHOTS; i++) {
        Shot *s = &shots[i];
        if (!s->active) continue;
        int sx = s->x >> 8, sy = s->y >> 8;
        int hw = shot_hw[s->kind], hh = shot_hh[s->kind];
        int laser = s->kind == SK_LASER;
        int consumed = 0;
        for (int j = 0; j < MAX_ENEMIES && !consumed; j++) {
            Enemy *e = &en[j];
            if (!e->active || e->dead) continue;
            int ex = e->x >> 8, ey = e->y >> 8;
            if (ex > 236) continue;
            const EnemyType *et = &etype[e->type];
            int dx = sx - ex, dy = sy - ey;
            if (dx < 0) dx = -dx;
            if (dy < 0) dy = -dy;
            if (dx >= hw + et->hw || dy >= hh + et->hh) continue;
            if (laser) {
                if (e->lcd) continue;
                e->lcd = 4;
            } else {
                consumed = 1;
            }
            e->hp -= s->dmg;
            e->flash = 3;
            if (e->hp <= 0) e->dead = 1;
            if (hitfx_n < 8) { hitfx_x[hitfx_n] = (s16)(laser ? ex - et->hw : sx + 3); hitfx_y[hitfx_n] = (s16)sy; hitfx_n++; }
        }
        if (!consumed && boss_hittable && !(laser && s->hit)) {
            for (int b = 0; b < bhit_n; b++) {
                if (sx + hw > bhit[b][0] && sx - hw < bhit[b][2] && sy + hh > bhit[b][1] && sy - hh < bhit[b][3]) {
                    boss_dmg += s->dmg;
                    if (laser) s->hit = 1;
                    else consumed = 1;
                    if (hitfx_n < 8) {
                        hitfx_x[hitfx_n] = (s16)(laser ? bhit[b][0] : sx + 3);
                        hitfx_y[hitfx_n] = (s16)sy;
                        hitfx_n++;
                    }
                    break;
                }
            }
        }
        if (consumed) s->active = 0;
    }
}
