/* Mochi Hop - platforming gameplay. All positions are 24.8 fixed point,
   anchored at the bottom-centre of each actor. */
#include "game.h"

enum { C_EMPTY = 0, C_SOLID = 1, C_ONEWAY = 2, C_SPIKE = 3 };
enum { O_STAR = 1, O_SPROUT, O_DROP, O_BEE, O_SPIKY, O_CHECK, O_GOAL, O_SPRING };

/* ---- tuning (1/256 px per frame) ---- */
#define G_NORMAL   100
#define G_HOLD     48
#define G_APEX     30
#define JUMP_V     1180
#define STOMP_V    840
#define STOMP_V_HI 1200
#define SPRING_V   2050
#define MAX_FALL   1400
#define WALK_MAX   576
#define RUN_MAX    780
#define ACC_GROUND 38
#define ACC_AIR    26
#define TURN_ACC   70
#define FRICTION   34
#define COYOTE     6
#define JBUFFER    7
#define HW         6     /* half width of Mochi's hitbox */
#define HH         14    /* hitbox height */

typedef struct {
    s32 x, y, vx, vy;
    s16 ox, oy, timer, anim;
    u8 type, state, dir, alive;
} Ent;

typedef struct {
    s32 x, y, vx, vy;
    s16 grav;
    u8 life, maxlife, kind, pad;
} Particle;

#define MAX_ENTS 100
#define MAX_PARTS 40
#define HIST 64

static Ent ents[MAX_ENTS];
static int nents;
static Particle parts[MAX_PARTS];

static const LevelDef *lv;
static int lvl_index;
static int lw, lh, lpw, lph, max_cy;

/* player */
static s32 px, py, pvx, pvy;
static int on_ground, coyote, jbuf, facing, jumping;
static int hearts, invuln, dead, clear_t;
static int sq_x, sq_y, sq_vx, sq_vy;   /* squash & stretch springs, 8.8 */
static int run_anim, blink_t, idle_t;
static int last_vy;
static s32 cp_x, cp_y;                  /* respawn point */
static s16 hist_x[HIST], hist_y[HIST];
static u8 hist_f[HIST];
static int hist_head;

/* camera */
static s32 camx, camy;                  /* 8.8 */
static int look;
static int col_lo, col_hi;              /* streamed tile columns */
static int scr_x, scr_y;                /* integer camera used for drawing */

static int stars_got, sprouts_mask, sprouts_n;
static int hud_dirty;
static int paused, pause_sel;
static int banner_t;
int res_stars, res_sprouts;

/* ------------------------------------------------------------------ helpers */
static inline int cell(int cx, int cy) {
    if (cx < 0 || cx >= lw) return C_SOLID;
    if (cy < 0 || cy >= lh) return C_EMPTY;
    return lv->coll[cy * lw + cx];
}

static inline int cell_px(int x, int y) {
    if (x < 0 || y < 0) return cell(x < 0 ? -1 : x >> 4, y < 0 ? -1 : y >> 4);
    return cell(x >> 4, y >> 4);
}

static void spawn_part(int x, int y, int vx, int vy, int grav, int life, int kind) {
    for (int i = 0; i < MAX_PARTS; i++) {
        Particle *p = &parts[i];
        if (p->life) continue;
        p->x = x << 8; p->y = y << 8; p->vx = vx; p->vy = vy;
        p->grav = grav; p->life = p->maxlife = life; p->kind = kind;
        return;
    }
}

static void burst(int x, int y, int n, int kind, int speed, int grav, int life) {
    for (int i = 0; i < n; i++) {
        int a = (i * 256) / n + (rnd() & 15);
        spawn_part(x, y, (icos(a) * speed) >> 8, (isin(a) * speed) >> 8, grav, life + (rnd() & 7), kind);
    }
}

/* ------------------------------------------------------------------ streaming */
static void load_column(int c) {
    if (c < 0 || c >= lw * 2) return;
    const u16 *src = lv->map + c * 32;
    volatile u16 *dst = SCREENBLOCK(24 + ((c >> 5) & 1)) + (c & 31);
    for (int r = 0; r < 32; r++) dst[r * 32] = src[r];
}

static void stream_columns(int force) {
    int first = scr_x >> 3;
    int want_lo = first - 2, want_hi = first + 33;
    if (want_lo < 0) want_lo = 0;
    if (want_hi > lw * 2 - 1) want_hi = lw * 2 - 1;
    if (force) {
        for (int c = want_lo; c <= want_hi; c++) load_column(c);
        col_lo = want_lo; col_hi = want_hi;
        return;
    }
    while (col_hi < want_hi) load_column(++col_hi);
    while (col_lo > want_lo) load_column(--col_lo);
    if (col_lo < want_lo) col_lo = want_lo;
    if (col_hi > want_hi) col_hi = want_hi;
}

/* ------------------------------------------------------------------ HUD */
static void draw_hud(void) {
    text_clear_rows(0, 2);
    text_tile(1, 1, UI_ISTAR);
    text_num(2, 1, stars_got, 2, 0);
    text_put(4, 1, "/", 0);
    text_num(5, 1, lv->nstars, 2, 0);
    for (int i = 0; i < 3; i++)
        text_tile(9 + i, 1, (sprouts_mask & (1 << i)) ? UI_ISPROUT : UI_ISPROUT0);
    for (int i = 0; i < 3; i++)
        text_tile(25 + i, 1, i < hearts ? UI_IHEART : UI_ISPROUT0 + 0);
    if (hearts < 3)
        for (int i = hearts; i < 3; i++) text_tile(25 + i, 1, 0);
    hud_dirty = 0;
}

static void draw_banner(void) {
    char num[4] = { '1' + lv->theme, '-', '1' + (lvl_index & 1), 0 };
    int w = str_len(lv->name) + 4;
    if (w < 14) w = 14;
    int x = (30 - w) >> 1;
    panel_draw(x, 7, w, 5);
    for (int i = 1; i < w - 1; i++) { text_tile(x + i, 8, UI_SOLID); text_tile(x + i, 9, UI_SOLID); text_tile(x + i, 10, UI_SOLID); }
    text_put((30 - 3) >> 1, 8, num, 1);
    text_put((30 - str_len(lv->name)) >> 1, 10, lv->name, 1);
}

/* ------------------------------------------------------------------ setup */
static void reset_player(void) {
    px = cp_x; py = cp_y;
    pvx = pvy = 0;
    on_ground = 0; coyote = 0; jbuf = 0; jumping = 0;
    facing = 1;
    hearts = 3; invuln = 60; dead = 0;
    sq_x = sq_y = 256; sq_vx = sq_vy = 0;
    for (int i = 0; i < HIST; i++) { hist_x[i] = px >> 8; hist_y[i] = py >> 8; hist_f[i] = 1; }
    hud_dirty = 1;
}

static void setup_level(int idx) {
    lvl_index = idx;
    lv = &level_defs[idx];
    lw = lv->w; lh = lv->h;
    lpw = lw * 16; lph = lh * 16;
    max_cy = lph - 160;
    nents = 0;
    const u8 *o = lv->objs;
    for (int i = 0; i < lv->nobjs && nents < MAX_ENTS; i++, o += 4) {
        Ent *e = &ents[nents++];
        memset(e, 0, sizeof(*e));
        e->type = o[0];
        e->x = (o[1] * 16 + 8) << 8;
        e->y = ((o[2] + 1) * 16) << 8;
        e->ox = o[1] * 16 + 8;
        e->oy = (o[2] + 1) * 16;
        e->alive = 1;
        e->dir = 0;
        e->anim = (s16)(o[1] * 13);
    }
    for (int i = 0; i < MAX_PARTS; i++) parts[i].life = 0;
    cp_x = (lv->sx * 16 + 8) << 8;
    cp_y = ((lv->sy + 1) * 16) << 8;
    stars_got = 0; sprouts_mask = 0; sprouts_n = 0;
    clear_t = 0;
    paused = 0;
    reset_player();
    look = 0;
    int cx = (px >> 8) - 120, cy = (py >> 8) - 104;
    if (cx < 0) cx = 0;
    if (cx > lpw - 240) cx = lpw - 240;
    if (cy < 0) cy = 0;
    if (cy > max_cy) cy = max_cy;
    camx = cx << 8; camy = cy << 8;
    scr_x = cx; scr_y = cy;
}

/* ------------------------------------------------------------------ player */
static void hurt(int knock_dir) {
    if (invuln || dead || clear_t) return;
    hearts--;
    hud_dirty = 1;
    if (hearts <= 0) {
        dead = 1;
        pvy = -1100; pvx = 0;
        sfx_play(SFX_FALL);
        return;
    }
    invuln = 90;
    pvy = -800;
    pvx = knock_dir * 420;
    on_ground = 0;
    jumping = 0;
    sq_x = 330; sq_y = 190;
    sfx_play(SFX_HURT);
    burst(px >> 8, (py >> 8) - 8, 6, 5, 300, 8, 24);
}

static void player_physics(void) {
    int dir = 0;
    if (!clear_t) {
        if (KEY_HELD(KEY_RIGHT)) dir = 1;
        else if (KEY_HELD(KEY_LEFT)) dir = -1;
    }
    int maxv = KEY_HELD(KEY_B) ? RUN_MAX : WALK_MAX;
    if (dir) {
        facing = dir;
        int acc = on_ground ? ACC_GROUND : ACC_AIR;
        if (pvx * dir < 0) acc = on_ground ? TURN_ACC : ACC_AIR + 10;
        pvx += acc * dir;
    } else {
        int f = on_ground ? FRICTION : 10;
        if (pvx > f) pvx -= f;
        else if (pvx < -f) pvx += f;
        else pvx = 0;
    }
    if (pvx > maxv) { pvx -= FRICTION; if (pvx < maxv) pvx = maxv; }
    if (pvx < -maxv) { pvx += FRICTION; if (pvx > -maxv) pvx = -maxv; }

    /* jump buffering + coyote time */
    if (!clear_t && KEY_HIT(KEY_A)) jbuf = JBUFFER;
    else if (jbuf) jbuf--;
    if (on_ground) coyote = COYOTE;
    else if (coyote) coyote--;
    if (jbuf && coyote) {
        pvy = -JUMP_V;
        jbuf = 0; coyote = 0; on_ground = 0; jumping = 1;
        sq_x = 200; sq_y = 330; sq_vx = sq_vy = 0;
        sfx_play(SFX_JUMP);
        spawn_part((px >> 8) - 5, (py >> 8) - 3, -160, -40, 0, 14, 0);
        spawn_part((px >> 8) + 5, (py >> 8) - 3, 160, -40, 0, 14, 0);
    }
    /* variable jump height: lighter gravity while A is held on the way up */
    int g = G_NORMAL;
    if (pvy < 0 && jumping && KEY_HELD(KEY_A)) g = G_HOLD;
    else if (pvy < 0) jumping = 0;
    if (iabs(pvy) < 160 && KEY_HELD(KEY_A) && !on_ground) g = G_APEX;
    pvy += g;
    if (pvy > MAX_FALL) pvy = MAX_FALL;

    /* ---- horizontal move ---- */
    px += pvx;
    int cx = px >> 8, fy = py >> 8;
    if (pvx > 0) {
        int ex = cx + HW - 1;
        if (cell_px(ex, fy - 2) == C_SOLID || cell_px(ex, fy - 8) == C_SOLID || cell_px(ex, fy - HH + 1) == C_SOLID) {
            px = (((ex >> 4) << 4) - HW) << 8;
            pvx = 0;
        }
    } else if (pvx < 0) {
        int ex = cx - HW;
        if (cell_px(ex, fy - 2) == C_SOLID || cell_px(ex, fy - 8) == C_SOLID || cell_px(ex, fy - HH + 1) == C_SOLID) {
            px = ((((ex >> 4) + 1) << 4) + HW) << 8;
            pvx = 0;
        }
    }
    if (px < (HW << 8)) { px = HW << 8; pvx = 0; }
    if (px > ((lpw - HW) << 8)) { px = (lpw - HW) << 8; pvx = 0; }

    /* ---- vertical move ---- */
    int old_fy = py >> 8;
    int was_ground = on_ground;
    py += pvy;
    cx = px >> 8;
    fy = py >> 8;
    on_ground = 0;
    if (pvy >= 0) {
        int b = fy - 1;
        int xs[2] = { cx - HW + 1, cx + HW - 2 };
        for (int i = 0; i < 2; i++) {
            int c = cell_px(xs[i], b);
            int top = (b >> 4) << 4;
            if (c == C_SOLID || (c == C_ONEWAY && old_fy <= top && b >= 0)) {
                py = top << 8;
                if (!was_ground && last_vy > 380) {
                    int s = last_vy >> 4;
                    if (s > 80) s = 80;
                    sq_y = 256 - s; sq_x = 256 + s; sq_vx = sq_vy = 0;
                    spawn_part(cx - 6, top - 3, -120, -30, 0, 12, 0);
                    spawn_part(cx + 6, top - 3, 120, -30, 0, 12, 0);
                }
                pvy = 0;
                on_ground = 1;
                jumping = 0;
                break;
            }
        }
    } else {
        int h = fy - HH;
        if (cell_px(cx - HW + 1, h) == C_SOLID || cell_px(cx + HW - 2, h) == C_SOLID) {
            py = ((((h >> 4) + 1) << 4) + HH) << 8;
            pvy = 0;
            jumping = 0;
            sq_x = 290; sq_y = 220;
        }
    }
    last_vy = pvy;

    /* spikes: the lower half of a spike cell hurts */
    fy = py >> 8;
    for (int i = -1; i <= 1; i++) {
        int x = cx + i * (HW - 2);
        int y = fy - 1;
        if (cell_px(x, y) == C_SPIKE && (y & 15) >= 7) {
            if (!invuln) { hurt(-facing); pvy = -1000; }
            break;
        }
    }
    /* fell into a pit */
    if (fy > lph + 24 && !dead) {
        dead = 1;
        hearts = 0;
        pvy = -200;
        sfx_play(SFX_FALL);
    }
}

static void update_squash(void) {
    int tx = 256, ty = 256;
    if (on_ground && iabs(pvx) < 40 && !dead) {
        int b = isin(frame_count * 3) >> 5;    /* idle breathing */
        ty = 256 + b; tx = 256 - b;
    }
    sq_vx += (tx - sq_x) >> 2; sq_vx = (sq_vx * 3) >> 2; sq_x += sq_vx;
    sq_vy += (ty - sq_y) >> 2; sq_vy = (sq_vy * 3) >> 2; sq_y += sq_vy;
}

/* ------------------------------------------------------------------ entities */
static int walker_blocked(Ent *e, int dir) {
    int x = (e->x >> 8) + dir * 7, y = e->y >> 8;
    if (cell_px(x, y - 6) == C_SOLID) return 1;
    int below = cell_px(x, y + 2);
    if (below != C_SOLID && below != C_ONEWAY) return 1;
    return 0;
}

static void update_ents(void) {
    int pcx = px >> 8, pfy = py >> 8;
    int old_pfy = (py - pvy) >> 8;
    for (int i = 0; i < nents; i++) {
        Ent *e = &ents[i];
        if (!e->alive) continue;
        int ex = e->x >> 8;
        if (ex < scr_x - 64 || ex > scr_x + 304) {
            /* sleep far away actors */
            if (e->type != O_BEE) continue;
        }
        e->anim++;
        switch (e->type) {
        case O_DROP:
        case O_SPIKY: {
            if (e->state == 1) {
                if (--e->timer <= 0) e->alive = 0;
                continue;
            }
            int sp = e->type == O_DROP ? 0x70 : 0x48;
            int d = e->dir ? 1 : -1;
            if (walker_blocked(e, d)) { e->dir ^= 1; d = -d; }
            e->x += d * sp;
            break;
        }
        case O_BEE: {
            int t = e->anim;
            int nx = e->ox + ((isin(t) * 34) >> 8);
            e->dir = icos(t) > 0;
            e->x = nx << 8;
            e->y = (e->oy + ((isin(t * 2) * 6) >> 8)) << 8;
            break;
        }
        case O_SPRING:
            if (e->timer) e->timer--;
            break;
        case O_CHECK:
            break;
        }
        if (dead || clear_t) continue;
        ex = e->x >> 8;
        int ey = e->y >> 8;
        int dx = ex - pcx;
        switch (e->type) {
        case O_STAR:
            if (iabs(dx) < 12 && iabs((ey - 8) - (pfy - 7)) < 14) {
                e->alive = 0;
                stars_got++;
                hud_dirty = 1;
                sfx_play(SFX_STAR);
                burst(ex, ey - 8, 6, 3, 260, 0, 14);
            }
            break;
        case O_SPROUT:
            if (iabs(dx) < 12 && iabs((ey - 8) - (pfy - 7)) < 14) {
                e->alive = 0;
                sprouts_mask |= 1 << sprouts_n;
                sprouts_n++;
                hud_dirty = 1;
                sfx_play(SFX_SPROUT);
                burst(ex, ey - 8, 5, 5, 200, -4, 30);
            }
            break;
        case O_CHECK:
            if (!e->state && iabs(dx) < 12 && pfy > ey - 40 && pfy <= ey + 4) {
                e->state = 1;
                cp_x = e->x; cp_y = e->y;
                sfx_play(SFX_CHECK);
                burst(ex + 6, ey - 26, 8, 3, 280, 6, 20);
                if (hearts < 3) { hearts = 3; hud_dirty = 1; }
            }
            break;
        case O_SPRING:
            if (pvy > 0 && iabs(dx) < 11 && pfy >= ey - 12 && pfy <= ey + 2) {
                pvy = -SPRING_V;
                py = (ey - 12) << 8;
                jumping = 1;
                on_ground = 0;
                e->timer = 12;
                sq_x = 180; sq_y = 360;
                sfx_play(SFX_SPRING);
                burst(ex, ey - 4, 6, 0, 220, 0, 12);
            }
            break;
        case O_GOAL:
            if (iabs(dx) < 16 && pfy > ey - 40 && pfy <= ey + 4) {
                clear_t = 1;
                music_play(SONG_CLEAR);
                sfx_play(SFX_CHECK);
            }
            break;
        case O_DROP:
        case O_SPIKY:
        case O_BEE: {
            if (e->state) break;
            int top = e->type == O_BEE ? ey - 13 : ey - 12;
            int bot = e->type == O_BEE ? ey - 1 : ey;
            if (iabs(dx) < 6 + HW - 1 && pfy > top + 1 && pfy - HH < bot) {
                if (e->type != O_SPIKY && pvy > 0 && old_pfy <= top + 7) {
                    /* stomp! */
                    pvy = KEY_HELD(KEY_A) ? -STOMP_V_HI : -STOMP_V;
                    jumping = 1;
                    py = top << 8;
                    sq_x = 300; sq_y = 210;
                    sfx_play(SFX_STOMP);
                    if (e->type == O_DROP) {
                        e->state = 1;
                        e->timer = 30;
                        burst(ex, ey - 4, 6, 7, 260, 20, 18);
                    } else {
                        e->alive = 0;
                        burst(ex, ey - 6, 8, 0, 240, 0, 16);
                        burst(ex, ey - 6, 3, 6, 180, 10, 20);
                    }
                } else if (!invuln) {
                    hurt(dx > 0 ? -1 : 1);
                }
            }
            break;
        }
        }
    }
}

static void update_parts(void) {
    for (int i = 0; i < MAX_PARTS; i++) {
        Particle *p = &parts[i];
        if (!p->life) continue;
        p->life--;
        p->x += p->vx;
        p->y += p->vy;
        p->vy += p->grav;
        p->vx -= p->vx >> 4;
    }
}

/* ------------------------------------------------------------------ camera */
static void update_camera(int snap) {
    int tl = facing * 28;
    if (look < tl) look++;
    else if (look > tl) look--;
    int tx = (px >> 8) - 120 + look;
    int ty = (py >> 8) - 104;
    if (tx < 0) tx = 0;
    if (tx > lpw - 240) tx = lpw - 240;
    if (ty < 0) ty = 0;
    if (ty > max_cy) ty = max_cy;
    if (snap) { camx = tx << 8; camy = ty << 8; }
    else {
        camx += ((tx << 8) - camx) >> 3;
        camy += ((ty << 8) - camy) >> 3;
    }
    scr_x = (camx + 128) >> 8;
    scr_y = (camy + 128) >> 8;
}

static void apply_scroll(void) {
    REG_BGHOFS(1) = scr_x;
    REG_BGVOFS(1) = scr_y;
    REG_BGHOFS(2) = (scr_x * 85) >> 8;
    REG_BGVOFS(2) = 96 - ((max_cy - scr_y) >> 1);
    int drift = lv->theme == 0 ? (frame_count >> 3) : 0;
    REG_BGHOFS(3) = (scr_x >> 3) + drift;
    REG_BGVOFS(3) = 80 - ((max_cy - scr_y) >> 2);
    REG_BGHOFS(0) = 0;
    REG_BGVOFS(0) = 0;
}

/* ------------------------------------------------------------------ drawing */
static int mochi_frame(void) {
    if (dead) return 9;
    if (clear_t) return on_ground ? 8 : 6;
    if (invuln > 70) return 9;
    if (!on_ground) return pvy < 0 ? 6 : 7;
    if (iabs(pvx) > 48) {
        run_anim += iabs(pvx);
        return 2 + ((run_anim >> 11) & 3);
    }
    run_anim = 0;
    if (++blink_t > 200) blink_t = 0;
    return blink_t > 192 ? 1 : 0;
}

static void draw_world(void) {
    oam_begin();
    /* particles on top */
    for (int i = 0; i < MAX_PARTS; i++) {
        Particle *p = &parts[i];
        if (!p->life) continue;
        int f;
        switch (p->kind) {
        case 0: f = 2 - (p->life * 3) / (p->maxlife + 1); break;   /* dust grows small */
        case 3: f = 3 + ((p->life >> 2) & 1); break;               /* sparkle */
        default: f = p->kind; break;
        }
        if (p->kind == 0) f = (p->life * 3 > p->maxlife * 2) ? 0 : (p->life * 3 > p->maxlife ? 1 : 2);
        if (p->life < 6 && (p->life & 1) && p->kind != 0) continue;
        spr8((p->x >> 8) - scr_x - 4, (p->y >> 8) - scr_y - 4, SPR_FX + f, 7);
    }
    /* Mochi (affine sprite 0) */
    int fr = mochi_frame();
    if (!(invuln && !dead && (frame_count & 2)))
        spr_mochi((px >> 8) - scr_x, (py >> 8) - scr_y, fr, sq_x, sq_y, facing < 0, 0);
    /* sprouts that follow Mochi */
    for (int i = 0; i < sprouts_n; i++) {
        int h = (hist_head - 12 * (i + 1)) & (HIST - 1);
        int bob = (frame_count >> 3) + i;
        spr16(hist_x[h] - scr_x - 8, hist_y[h] - scr_y - 16 - ((bob & 2) ? 1 : 0),
              SPR_SPROUT + ((bob >> 1) & 1) * 4, 4, hist_f[h] == 0);
    }
    /* actors */
    for (int i = 0; i < nents; i++) {
        Ent *e = &ents[i];
        if (!e->alive) continue;
        int sx = (e->x >> 8) - scr_x, sy = (e->y >> 8) - scr_y;
        if (sx < -40 || sx > 280) continue;
        switch (e->type) {
        case O_STAR: {
            int f = ((e->anim >> 3)) & 3;
            int bob = isin(e->anim * 3) >> 7;
            spr16(sx - 8, sy - 16 + bob, SPR_STAR + f * 4, 1, 0);
            break;
        }
        case O_SPROUT: {
            int bob = (e->anim >> 4) & 1;
            spr16(sx - 8, sy - 16 - bob, SPR_SPROUT + bob * 4, 4, (e->anim >> 6) & 1);
            break;
        }
        case O_DROP:
            if (e->state) spr16(sx - 8, sy - 16, SPR_DROP + 8, 2, 0);
            else spr16(sx - 8, sy - 16, SPR_DROP + ((e->anim >> 4) & 1) * 4, 2, e->dir);
            break;
        case O_SPIKY:
            spr16(sx - 8, sy - 15, SPR_SPIKY + ((e->anim >> 4) & 1) * 4, 5, e->dir);
            break;
        case O_BEE:
            spr16(sx - 8, sy - 14, SPR_BEE + ((e->anim >> 2) & 1) * 4, 3, e->dir);
            break;
        case O_CHECK:
            spr16x32(sx - 5, sy - 31, SPR_FLAG + (e->state ? 1 + ((e->anim >> 4) & 1) : 0) * 8, 6, 0);
            break;
        case O_SPRING:
            spr16(sx - 8, sy - 15, SPR_SPRING + (e->timer ? 4 : 0), 6, 0);
            break;
        case O_GOAL:
            spr32(sx - 16, sy - 31, SPR_GOAL + ((e->anim >> 4) & 1) * 16, 6, 0);
            break;
        }
    }
}

/* ------------------------------------------------------------------ pause / results */
static const char *const pause_items[3] = { "RESUME", "RETRY", "WORLD MAP" };

static void draw_pause(void) {
    panel_draw(9, 5, 12, 10);
    for (int y = 6; y < 14; y++) for (int x = 10; x < 20; x++) text_tile(x, y, UI_SOLID);
    text_put(12, 6, "PAUSED", 1);
    for (int i = 0; i < 3; i++) {
        text_tile(11, 8 + i * 2, i == pause_sel ? UI_JARROW : UI_SOLID);
        text_put(12, 8 + i * 2, pause_items[i], 1);
    }
}

static void set_dim(int on) {
    if (on) { REG_BLDCNT = 0x00FE; REG_BLDY = 9; }   /* darken everything except BG0 text */
    else fade_apply();
}

static void frame_commit(void) {
    vsync();
    oam_commit();
    apply_scroll();
    stream_columns(0);
    audio_update();
    poll_keys();
}

static void results_screen(void) {
    /* stars count up on a cute panel */
    int best = save.best[lvl_index];
    int shown = 0, t = 0;
    set_dim(1);
    text_clear();
    panel_draw(5, 3, 20, 14);
    for (int y = 4; y < 16; y++) for (int x = 6; x < 24; x++) text_tile(x, y, UI_SOLID);
    text_center(5, "LEVEL CLEAR!", 1);
    text_center(7, lv->name, 1);
    text_tile(8, 10, UI_JSTAR);
    text_put(10, 10, "STARS", 1);
    text_put(18, 10, "/", 1);
    text_num(19, 10, lv->nstars, 2, 1);
    text_put(10, 12, "SPROUTS", 1);
    for (int i = 0; i < 3; i++) text_tile(18 + i, 12, (sprouts_mask & (1 << i)) ? UI_JSPROUT : UI_JSPROUT0);
    for (;;) {
        frame_commit();
        t++;
        if (shown < stars_got && (t & 1)) {
            shown++;
            sfx_play(SFX_TICK);
            text_num(16, 10, shown, 2, 1);
        }
        if (shown == 0) text_num(16, 10, 0, 2, 1);
        if (shown >= stars_got && t > 30) {
            if (stars_got > best) {
                if ((t >> 4) & 1) text_center(14, " NEW BEST! ", 1);
                else text_center(14, "           ", 1);
            } else {
                text_put(9, 14, "BEST", 1);
                text_num(14, 14, best, 2, 1);
                text_put(16, 14, "/", 1);
                text_num(17, 14, lv->nstars, 2, 1);
            }
            text_put(11, 17, "A CONTINUE", 0);
            if (KEY_HIT(KEY_A | KEY_START)) { sfx_play(SFX_SELECT); break; }
        }
        /* keep Mochi celebrating behind the panel */
        if (on_ground && (t % 40) == 0) { pvy = -900; on_ground = 0; }
        pvy += G_NORMAL;
        py += pvy;
        if (py >= cp_y && 0) py = cp_y;
        {
            int fy = py >> 8, b = fy - 1;
            int c = cell_px(px >> 8, b);
            if (pvy > 0 && (c == C_SOLID || c == C_ONEWAY)) { py = ((b >> 4) << 4) << 8; pvy = 0; on_ground = 1; }
        }
        update_squash();
        update_parts();
        if ((t & 7) == 0) spawn_part(scr_x + 20 + (rnd() % 200), scr_y - 4, 0, 60, 2, 90, 3 + (rnd() & 1) * 3);
        draw_world();
    }
}

/* ------------------------------------------------------------------ main loop */
int play_level(int idx) {
    REG_DISPCNT = 0x0080;
    setup_level(idx);
    load_theme_bg(lv->theme);
    load_terrain(lv->theme);
    REG_BGCNT(0) = 0x1E08;          /* text: prio 0, cbb2, sbb30 */
    REG_BGCNT(1) = 0x5801;          /* level: prio 1, cbb0, sbb24, 64x32 */
    REG_BGCNT(2) = 0x5A0A;          /* mid: prio 2, cbb2, sbb26, 64x32 */
    REG_BGCNT(3) = 0x1C07;          /* far: prio 3, cbb1, sbb28 */
    fill16(SCREENBLOCK(24), 0, 2048);
    stream_columns(1);
    text_clear();
    draw_hud();
    draw_banner();
    banner_t = 110;
    apply_scroll();
    fade = 16;
    fade_apply();
    REG_DISPCNT = 0x1F40;
    music_play(1 + lv->theme);
    int leaving = 0, result = RES_QUIT, respawn_t = 0;

    for (;;) {
        frame_commit();
        if (paused) {
            if (KEY_HIT(KEY_UP)) { pause_sel = (pause_sel + 2) % 3; sfx_play(SFX_MOVE); draw_pause(); }
            if (KEY_HIT(KEY_DOWN)) { pause_sel = (pause_sel + 1) % 3; sfx_play(SFX_MOVE); draw_pause(); }
            if (KEY_HIT(KEY_START) || (KEY_HIT(KEY_A) && pause_sel == 0) || KEY_HIT(KEY_B)) {
                paused = 0;
                music_pause(0);
                sfx_play(SFX_BACK);
                set_dim(0);
                text_clear();
                draw_hud();
            } else if (KEY_HIT(KEY_A)) {
                paused = 0;
                music_pause(0);
                sfx_play(SFX_SELECT);
                set_dim(0);
                text_clear();
                if (pause_sel == 1) {
                    /* retry: restart the level from scratch */
                    fade = 16;
                    fade_apply();
                    return play_level(idx);
                }
                leaving = 1;
                result = RES_QUIT;
            }
            if (paused) continue;
        }
        if (leaving) {
            fade += 1;
            fade_apply();
            if (fade >= 16) break;
        } else if (fade > 0) {
            fade--;
            fade_apply();
        }

        if (!leaving && !clear_t && !dead && KEY_HIT(KEY_START)) {
            paused = 1;
            pause_sel = 0;
            music_pause(1);
            sfx_play(SFX_SELECT);
            set_dim(1);
            draw_pause();
            continue;
        }

        if (banner_t && --banner_t == 0) { text_clear_rows(6, 13); }

        if (dead) {
            /* little hop then fall off screen, then respawn at the checkpoint */
            pvy += 60;
            py += pvy;
            respawn_t++;
            if (respawn_t > 50) {
                fade = respawn_t - 50;
                if (fade > 16) fade = 16;
                fade_apply();
            }
            if (respawn_t > 70) {
                respawn_t = 0;
                reset_player();
                update_camera(1);
                stream_columns(1);
                fade = 16;
            }
        } else {
            player_physics();
            if (invuln) invuln--;
        }
        update_squash();
        update_ents();
        update_parts();
        if (!dead) {
            hist_head = (hist_head + 1) & (HIST - 1);
            hist_x[hist_head] = px >> 8;
            hist_y[hist_head] = py >> 8;
            hist_f[hist_head] = facing > 0;
        }
        if (!dead) update_camera(0);
        if (hud_dirty) draw_hud();

        if (clear_t) {
            clear_t++;
            facing = 1;
            if (on_ground && clear_t > 20 && (clear_t % 36) == 0) {
                pvy = -1000; on_ground = 0; jumping = 0;
                sq_x = 200; sq_y = 330;
            }
            if ((clear_t & 3) == 0) {
                spawn_part((px >> 8) - 30 + (rnd() & 63), (py >> 8) - 40 - (rnd() & 15),
                           (int)(rnd() & 255) - 128, -100, 6, 40, 3 + ((clear_t >> 2) & 1) * 3);
            }
            if (clear_t > 150 && on_ground) {
                res_stars = stars_got;
                res_sprouts = sprouts_mask;
                results_screen();
                result = RES_CLEAR;
                leaving = 1;
                set_dim(0);
                fade = 9;
                clear_t = 2;   /* keep input locked */
            }
        }
        draw_world();
    }
    text_clear();
    music_stop();
    return result;
}
