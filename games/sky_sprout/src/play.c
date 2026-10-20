/* Sky Sprout - gameplay: player, weapons, items, effects, HUD and stage flow. */
#include "game.h"

Game G;
Player pl;
int stage_t;
int res_boss_bonus;

extern s16 hitfx_x[8], hitfx_y[8];
extern int hitfx_n;

/* ---------------------------------------------------------------- pools */
typedef struct { s32 x, y; s16 vx, vy; u8 type, t, life, a; } Fx;
#define MAX_FX 48
static Fx fx[MAX_FX];

typedef struct { s32 x, y; s16 vx, vy; u8 kind, active, cyc, bounces; u16 t; } Item;
#define MAX_ITEMS 6
static Item items[MAX_ITEMS];

enum { IT_P, IT_S, IT_L, IT_H, IT_O, IT_B, IT_D, IT_1UP, IT_CYCLE };
static const s16 spd_tab[4] = { 320, 448, 576, 704 };
static const u32 extend_tab[] = { 100000, 300000, 600000, 1000000 };

static u32 scroll_acc;
static int scroll_speed;
static int msg_t;
static int seed_cd, opt_cd, shot_sfx_cd;
static int phase, phase_t;
static int hold, hold_t;
static const StageEv *evs;
static int ev_i, ev_n;
enum { PH_INTRO, PH_PLAY, PH_WARNING, PH_BOSS, PH_CLEAR };
enum { HOLD_NONE, HOLD_MID, HOLD_WAIT };

/* ---------------------------------------------------------------- helpers */
int aim_player(s32 x, s32 y) { return iatan2(pl.y - y, pl.x - x); }

static void msg_show(const char *s) {
    text_clear_rows(17, 17);
    text_center(17, s, TS_HUD);
    msg_t = 75;
}

void add_score(u32 pts) {
    G.score += pts;
    if (G.score > 99999999u) G.score = 99999999u;
    while (G.score >= G.next_extend) {
        if (G.lives < 9) G.lives++;
        sfx_play(SFX_1UP);
        msg_show("EXTEND!");
        int k = 0;
        while (k < 4 && extend_tab[k] <= G.next_extend) k++;
        G.next_extend = k < 4 ? extend_tab[k] : G.next_extend + 500000;
    }
}

void kill_chain(void) {
    G.chain = G.chain_t > 0 ? G.chain + 1 : 1;
    if (G.chain > 99) G.chain = 99;
    G.chain_t = 64;
    if (G.chain > G.max_chain) G.max_chain = G.chain;
}

void fx_spawn(int type, s32 x, s32 y, int vx, int vy) {
    static const u8 life[] = { 24, 30, 6, 15, 44, 44, 22 };
    for (int i = 0; i < MAX_FX; i++) {
        Fx *f = &fx[i];
        if (f->life) continue;
        f->type = (u8)type;
        f->x = x;
        f->y = y;
        f->vx = (s16)vx;
        f->vy = (s16)vy;
        f->t = 0;
        f->life = life[type];
        f->a = 0;
        return;
    }
}

void popup_mult(s32 x, s32 y, int mult) {
    for (int i = 0; i < MAX_FX; i++) {
        if (fx[i].life) continue;
        fx_spawn(FX_POPUP, x, y, 0, -100);
        fx[i].a = (u8)(mult - 2);
        return;
    }
}

void explode(s32 x, s32 y, int big) {
    fx_spawn(big ? FX_BIGEXPL : FX_EXPL, x, y, 0, 0);
    int n = big ? 6 : 3;
    for (int i = 0; i < n; i++) {
        int a = rnd() & 255, s = 200 + (rnd() & 255);
        fx_spawn(FX_DEBRIS, x, y, (icos(a) * s) >> 8, ((isin(a) * s) >> 8) - 200);
    }
    if (big) {
        fx_spawn(FX_EXPL, x + FIX(10), y - FIX(6), 0, 0);
        fx_spawn(FX_EXPL, x - FIX(8), y + FIX(8), 0, 0);
        sfx_play(SFX_BOOM);
    } else {
        sfx_play(SFX_POP);
    }
}

void item_spawn(int kind, s32 x, s32 y) {
    for (int i = 0; i < MAX_ITEMS; i++) {
        Item *it = &items[i];
        if (it->active) continue;
        it->active = 1;
        it->cyc = kind == IT_CYCLE;
        it->kind = (u8)(it->cyc ? IT_S + (rnd() % 3) : kind);
        it->x = x;
        it->y = y;
        it->vx = -140;
        it->vy = -300;
        it->t = 0;
        it->bounces = 0;
        return;
    }
}

void cancel_bullets(void) {
    int any = 0;
    for (int i = 0; i < MAX_EBUL; i++) {
        Bullet *b = &ebul[i];
        if (!b->active || b->beh == BEH_CANCEL) continue;
        b->beh = BEH_CANCEL;
        b->kind = BK_PETAL;
        b->t = 0;
        any = 1;
    }
    if (any) sfx_play(SFX_CANCEL);
}

/* ---------------------------------------------------------------- HUD */
static u32 h_score = 0xFFFFFFFF;
static int h_lives = -1, h_bombs = -1, h_mult = -1, h_gauge = -1, h_weap = -1, h_lvl = -1, h_spd = -1, h_bar = -1, h_opt = -1;

static void hud_invalidate(void) {
    h_score = 0xFFFFFFFF;
    h_lives = h_bombs = h_mult = h_gauge = h_weap = h_lvl = h_spd = h_bar = h_opt = -1;
}

static void hud_update(void) {
    if (G.score != h_score) {
        h_score = G.score;
        text_num(0, 0, G.score, 8, TS_HUD, 1);
    }
    int mult = 1 + G.chain / 6;
    if (mult > 8) mult = 8;
    if (!G.chain) mult = 0;
    if (mult != h_mult) {
        h_mult = mult;
        if (mult >= 1) {
            text_put(9, 0, "x", TS_HUD);
            text_num(10, 0, (u32)mult, 1, TS_GOLD, 0);
        } else {
            text_tile(9, 0, 0);
            text_tile(10, 0, 0);
        }
    }
    int gauge = G.chain ? (G.chain_t + 12) / 13 : 0;
    if (gauge != h_gauge) {
        h_gauge = gauge;
        for (int i = 0; i < 5; i++) text_tile(11 + i, 0, G.chain ? UI_GAUGE + (i < gauge) : 0);
    }
    if (G.bombs != h_bombs) {
        h_bombs = G.bombs;
        for (int i = 0; i < 5; i++) text_tile(19 + i, 0, i < G.bombs ? UI_BOMB : 0);
        if (G.bombs > 5) text_put(23, 0, "+", TS_HUD);
    }
    if (G.lives != h_lives) {
        h_lives = G.lives;
        for (int i = 0; i < 5; i++) text_tile(29 - i, 0, i < G.lives ? UI_LIFE : 0);
        if (G.lives > 5) text_num(24, 0, (u32)G.lives, 1, TS_GOLD, 0);
        else text_tile(24, 0, 0);
    }
    if (G.weapon != h_weap || G.level != h_lvl) {
        h_weap = G.weapon;
        h_lvl = G.level;
        text_tile(0, 19, UI_WEAP + G.weapon);
        for (int i = 0; i < 4; i++) text_tile(1 + i, 19, UI_PIP + (i <= G.level ? 0 : 1));
    }
    if (G.options != h_opt) {
        h_opt = G.options;
        text_tile(6, 19, G.options >= 1 ? UI_HUD + 3 : 0);
        text_tile(7, 19, G.options >= 2 ? UI_HUD + 3 : 0);
        if (G.options) text_put(6, 19, G.options == 2 ? "OO" : "O", TS_HUD);
    }
    if (G.speed != h_spd) {
        h_spd = G.speed;
        for (int i = 0; i < 4; i++) text_tile(26 + i, 19, i <= G.speed ? UI_SPEED : 0);
    }
    /* boss health bar */
    int bar = -1;
    if (boss.active && boss.bar_shown) bar = boss.hp <= 0 ? 0 : udiv(boss.hp * 192, boss.maxhp);
    if (bar != h_bar) {
        if (bar < 0) {
            text_clear_rows(1, 1);
        } else {
            text_tile(2, 1, UI_BAR);
            for (int i = 0; i < 24; i++) {
                int v = bar - i * 8;
                text_tile(3 + i, 1, UI_BAR + 1 + (v < 0 ? 0 : (v > 8 ? 8 : v)));
            }
            text_tile(27, 1, UI_BAR + 10);
        }
        h_bar = bar;
    }
    if (msg_t && --msg_t == 0) text_clear_rows(17, 17);
#ifdef DEBUG_PERF
    if ((frame_count & 15) == 0) {
        text_put(10, 19, "CPU", TS_HUD);
        text_num(13, 19, (u32)perf_last, 3, TS_HUD, 0);
        text_put(17, 19, "MAX", TS_HUD);
        text_num(20, 19, (u32)perf_max, 3, TS_HUD, 0);
    }
#endif
}

/* ---------------------------------------------------------------- shots */
static void shot_add(int kind, s32 x, s32 y, int ang, int spd, int dmg) {
    for (int i = 0; i < MAX_SHOTS; i++) {
        Shot *s = &shots[i];
        if (s->active) continue;
        s->active = 1;
        s->kind = (u8)kind;
        s->x = x;
        s->y = y;
        s->vx = (s16)((icos(ang) * spd) >> 8);
        s->vy = (s16)((isin(ang) * spd) >> 8);
        s->dmg = (u8)dmg;
        s->t = 0;
        s->dir = (u8)ang;
        s->target = -1;
        s->hit = 0;
        return;
    }
}

static void fire_weapon(void) {
    s32 x = pl.x + FIX(12), y = pl.y;
    int L = G.level;
    switch (G.weapon) {
    case 0:
        pl.fire_cd = 5;
        if (L >= 2) {
            shot_add(SK_PEA, x, y - FIX(5), 0, 2048, 1);
            shot_add(SK_PEA, x + FIX(2), y, 0, 2048, 1);
            shot_add(SK_PEA, x, y + FIX(5), 0, 2048, 1);
        } else {
            shot_add(SK_PEA, x, y - FIX(3), 0, 2048, 1);
            shot_add(SK_PEA, x, y + FIX(3), 0, 2048, 1);
        }
        if (L >= 1) {
            shot_add(SK_PEA, x - FIX(4), y - FIX(2), 248, 1920, 1);
            shot_add(SK_PEA, x - FIX(4), y + FIX(2), 8, 1920, 1);
        }
        if (L >= 3) {
            shot_add(SK_PEA, x - FIX(6), y - FIX(3), 240, 1800, 1);
            shot_add(SK_PEA, x - FIX(6), y + FIX(3), 16, 1800, 1);
        }
        break;
    case 1: {
        static const s8 fans[4][7] = { { -12, 0, 12 }, { -20, -10, 0, 10, 20 }, { -28, -14, 0, 14, 28 },
                                       { -30, -20, -10, 0, 10, 20, 30 } };
        static const u8 cnt[4] = { 3, 5, 5, 7 };
        pl.fire_cd = 7;
        for (int i = 0; i < cnt[L]; i++) shot_add(SK_PETAL, x, y, fans[L][i] & 255, L >= 2 ? 1700 : 1536, 1);
        break;
    }
    case 2:
        pl.fire_cd = 2;
        if (L == 0) shot_add(SK_LASER, x + FIX(8), y, 0, 3072, 1);
        else if (L == 1) {
            shot_add(SK_LASER, x + FIX(8), y - FIX(3), 0, 3072, 1);
            shot_add(SK_LASER, x + FIX(8), y + FIX(3), 0, 3072, 1);
        } else {
            shot_add(SK_LASER, x + FIX(8), y - FIX(5), 0, 3072, 1);
            shot_add(SK_LASER, x + FIX(10), y, 0, 3072, 1);
            shot_add(SK_LASER, x + FIX(8), y + FIX(5), 0, 3072, 1);
            if (L == 3 && (frame_count & 2)) {
                shot_add(SK_LASER, x + FIX(4), y - FIX(9), 252, 3072, 1);
                shot_add(SK_LASER, x + FIX(4), y + FIX(9), 4, 3072, 1);
            }
        }
        break;
    case 3:
        pl.fire_cd = 6;
        shot_add(SK_PEA, x, y - FIX(3), 0, 2048, 1);
        shot_add(SK_PEA, x, y + FIX(3), 0, 2048, 1);
        if (seed_cd <= 0) {
            seed_cd = 10;
            static const s8 dirs[5] = { -40, 40, -20, 20, 0 };
            for (int i = 0; i < 2 + L; i++) shot_add(SK_SEED, pl.x, y, dirs[i] & 255, 1280, 2);
        }
        break;
    }
    if (shot_sfx_cd <= 0) { sfx_play(SFX_SHOT); shot_sfx_cd = 9; }
}

static void options_fire(void) {
    for (int k = 0; k < G.options; k++) {
        int j = (pl.hidx - 10 * (k + 1)) & 31;
        s32 x = FIX(pl.hx[j] + 6), y = FIX(pl.hy[j]);
        switch (G.weapon) {
        case 1:
            shot_add(SK_PETAL, x, y, k ? 8 : 248, 1536, 1);
            break;
        case 2:
            shot_add(SK_LASER, x + FIX(10), y, 0, 3072, 1);
            break;
        case 3:
            shot_add(SK_SEED, x, y, k ? 24 : 232, 1280, 1);
            break;
        default:
            shot_add(SK_OPT, x, y, 0, 1920, 1);
        }
    }
}

static int nearest_target(s32 x, s32 y, s32 *tx, s32 *ty) {
    int best = 0x7FFFFFFF, found = 0;
    for (int i = 0; i < MAX_ENEMIES; i++) {
        Enemy *e = &en[i];
        if (!e->active || e->dead || (e->x >> 8) > 236 || e->x < x - FIX(24)) continue;
        int d = iabs((e->x - x) >> 8) + iabs((e->y - y) >> 8);
        if (d < best) { best = d; *tx = e->x; *ty = e->y; found = 1; }
    }
    if (boss_hittable && bhit_n) {
        s32 bx = FIX((bhit[0][0] + bhit[0][2]) >> 1), by = FIX((bhit[0][1] + bhit[0][3]) >> 1);
        int d = iabs((bx - x) >> 8) + iabs((by - y) >> 8);
        if (d < best) { *tx = bx; *ty = by; found = 1; }
    }
    return found;
}

static void shots_update(void) {
    for (int i = 0; i < MAX_SHOTS; i++) {
        Shot *s = &shots[i];
        if (!s->active) continue;
        s->t++;
        if (s->kind == SK_SEED && s->t > 4 && (s->t & 1) == 0) {
            s32 tx, ty;
            if (nearest_target(s->x, s->y, &tx, &ty)) {
                int want = iatan2(ty - s->y, tx - s->x);
                int d = (s8)(want - s->dir);
                if (d > 14) d = 14;
                if (d < -14) d = -14;
                s->dir = (u8)(s->dir + d);
            }
            int sp = 1280 + s->t * 24;
            if (sp > 1800) sp = 1800;
            s->vx = (s16)((icos(s->dir) * sp) >> 8);
            s->vy = (s16)((isin(s->dir) * sp) >> 8);
        }
        s->x += s->vx;
        s->y += s->vy;
        int x = s->x >> 8, y = s->y >> 8;
        if (x > 256 || x < -16 || y < -8 || y > 168 || (s->kind == SK_SEED && s->t > 120)) s->active = 0;
    }
}

static void shots_draw(void) {
    for (int i = 0; i < MAX_SHOTS; i++) {
        Shot *s = &shots[i];
        if (!s->active) continue;
        int x = s->x >> 8, y = s->y >> 8;
        switch (s->kind) {
        case SK_PEA: case SK_OPT: spr(OC_SHOT, x - 4, y - 4, SPR_PEA, S8, 1, 0); break;
        case SK_PETAL: spr(OC_SHOT, x - 4, y - 4, SPR_PETAL + ((s->t >> 2) & 1), S8, 1, 0); break;
        case SK_LASER: spr(OC_SHOT, x - 16, y - 4, SPR_LASER + ((s->t >> 1) & 1) * 4, W32x8, 1, 0); break;
        case SK_SEED: spr(OC_SHOT, x - 4, y - 4, SPR_SEED + (((s->dir + 16) >> 5) & 7), S8, 1, 0); break;
        }
    }
}

/* ---------------------------------------------------------------- player */
static void player_reset(void) {
    pl.alive = 1;
    pl.x = FIX(-20);
    pl.y = FIX(84);
    pl.invuln = 120;
    pl.respawn = 0;
    pl.hurt_t = 50;
    pl.bomb_t = 0;
    pl.fire_cd = 0;
    for (int i = 0; i < 32; i++) { pl.hx[i] = -20; pl.hy[i] = 84; }
    pl.hidx = 0;
}

static void player_die(void) {
    pl.alive = 0;
    pl.respawn = 70;
    explode(pl.x, pl.y, 1);
    for (int i = 0; i < 8; i++) {
        int a = i * 32;
        fx_spawn(FX_PETAL, pl.x, pl.y, (icos(a) * 3) >> 1, (isin(a) * 3) >> 1);
    }
    sfx_play(SFX_DEATH);
    shake_x = 0;
    G.misses++;
    G.chain = 0;
    G.chain_t = 0;
    if (G.level > 0) G.level--;
    if (G.options > 0) {
        item_spawn(IT_O, pl.x + FIX(20), pl.y);
        G.options = 0;
    }
    G.shield = 0;
    if (G.bombs < (G.diff == 0 ? 4 : 3)) G.bombs = G.diff == 0 ? 4 : 3;
    cancel_bullets();
}

static void player_hit(void) {
#ifdef DEBUG_INVULN
    return;
#endif
    if (G.shield) {
        G.shield = 0;
        pl.invuln = 90;
        sfx_play(SFX_SHIELD);
        for (int i = 0; i < 6; i++) fx_spawn(FX_SPARK, pl.x + FIX((int)(rnd() & 15) - 8), pl.y + FIX((int)(rnd() & 15) - 8), 0, 0);
        cancel_bullets();
        return;
    }
    player_die();
}

static void bomb_start(void) {
    G.bombs--;
    pl.bomb_t = 96;
    if (pl.invuln < 130) pl.invuln = 130;
    flash = 14;
    shake_x = 0;
    cancel_bullets();
    sfx_play(SFX_BOMB);
    for (int i = 0; i < 16; i++) {
        int a = i * 16;
        fx_spawn(FX_PETAL, pl.x, pl.y, (icos(a) * 5) >> 1, (isin(a) * 5) >> 1);
    }
}

static void player_update(int control) {
    if (!pl.alive) {
        if (pl.respawn > 0 && --pl.respawn == 0) {
            if (G.lives > 0) {
                G.lives--;
                player_reset();
                pl.invuln = 150;
            }
        }
        return;
    }
    if (pl.invuln) pl.invuln--;
    int dx = 0, dy = 0;
    if (pl.hurt_t > 0) {
        pl.hurt_t--;
        pl.x += FIX(1) + (pl.hurt_t << 3);
        if (pl.x > FIX(48)) pl.x = FIX(48);
    } else if (control) {
        if (KEY_HELD(KEY_LEFT)) dx = -1;
        if (KEY_HELD(KEY_RIGHT)) dx = 1;
        if (KEY_HELD(KEY_UP)) dy = -1;
        if (KEY_HELD(KEY_DOWN)) dy = 1;
        int s = spd_tab[G.speed];
        if (dx && dy) s = (s * 181) >> 8;
        pl.x += dx * s;
        pl.y += dy * s;
        if (pl.x < FIX(10)) pl.x = FIX(10);
        if (pl.x > FIX(226)) pl.x = FIX(226);
        if (pl.y < FIX(14)) pl.y = FIX(14);
        if (pl.y > FIX(150)) pl.y = FIX(150);
        if (KEY_HIT(KEY_R)) { G.speed = (G.speed + 1) & 3; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_L)) { G.speed = (G.speed + 3) & 3; sfx_play(SFX_MOVE); }
        if (pl.fire_cd) pl.fire_cd--;
        if (seed_cd > 0) seed_cd--;
        if (opt_cd > 0) opt_cd--;
        if (KEY_HELD(KEY_A)) {
            if (!pl.fire_cd) fire_weapon();
            if (G.options && opt_cd <= 0) { options_fire(); opt_cd = G.weapon == 2 ? 3 : 8; }
        }
        if (KEY_HIT(KEY_B) && G.bombs > 0 && !pl.bomb_t) bomb_start();
    }
    pl.tilt = dy < 0 ? 1 : (dy > 0 ? -1 : 0);
    pl.prop++;
    int hx = pl.x >> 8, hy = pl.y >> 8;
    int last = (pl.hidx - 1) & 31;
    if (pl.hx[last] != hx || pl.hy[last] != hy) {
        pl.hx[pl.hidx] = (s16)hx;
        pl.hy[pl.hidx] = (s16)hy;
        pl.hidx = (pl.hidx + 1) & 31;
    }
    if ((frame_count & 7) == 0) fx_spawn(FX_PUFF, pl.x - FIX(13), pl.y + FIX(5), -260, 0);
    /* bomb damage over time */
    if (pl.bomb_t) {
        pl.bomb_t--;
        if ((pl.bomb_t & 7) == 0) {
            for (int i = 0; i < MAX_ENEMIES; i++) {
                Enemy *e = &en[i];
                if (!e->active || e->dead || (e->x >> 8) > 240) continue;
                e->hp -= 5;
                e->flash = 3;
                if (e->hp <= 0) e->dead = 1;
            }
            if (boss_hittable) boss_dmg += 8;
            if (pl.bomb_t > 40) cancel_bullets();
        }
        if (flash > 0 && (frame_count & 1)) flash--;
    }
}

static void player_draw(void) {
    if (!pl.alive) return;
    if (pl.invuln && (pl.invuln & 4) && !pl.bomb_t) return;
    int x = (pl.x >> 8) - 16, y = (pl.y >> 8) - 17;
    int fr = pl.tilt > 0 ? 1 : (pl.tilt < 0 ? 2 : 0);
    spr(OC_PLAYER, x + 12, y + 13, SPR_HITBOX, S8, 0, 0);
    spr(OC_PLAYER, x, y - (pl.tilt > 0 ? 1 : 0), SPR_PROP + ((pl.prop >> 1) % 3) * 4, W32x8, 0, 0);
    spr(OC_PLAYER, x, y, SPR_PLAYER + fr * 16, S32, 0, 0);
    if (G.shield) spr(OC_PLAYER, x, y + 1, SPR_SHIELD + ((frame_count >> 3) & 1) * 16, S32, 4, SPR_SEMI);
    for (int k = 0; k < G.options; k++) {
        int j = (pl.hidx - 10 * (k + 1)) & 31;
        int bob = isin(frame_count * 4 + k * 128) >> 7;
        spr(OC_PLAYER, pl.hx[j] - 8, pl.hy[j] - 8 + bob, SPR_SPROUT + ((frame_count >> 3) & 1) * 4, S16, 0, 0);
    }
}

/* ---------------------------------------------------------------- items */
static void item_collect(Item *it) {
    static const char *const wname[4] = { "PEA SHOT", "PETAL SPREAD", "SKY LASER", "HOMING SEEDS" };
    int k = it->kind;
    sfx_play(k == IT_1UP ? SFX_1UP : SFX_POWER);
    switch (k) {
    case IT_P:
        if (G.level < 3) { G.level++; msg_show("POWER UP!"); } else { add_score(5000); msg_show("MAX POWER  5000"); }
        break;
    case IT_S: case IT_L: case IT_H: {
        int w = k - IT_S + 1;
        if (G.weapon == w) {
            if (G.level < 3) G.level++;
            else add_score(5000);
        } else {
            G.weapon = w;
        }
        msg_show(wname[w]);
        break;
    }
    case IT_O:
        if (G.options < 2) { G.options++; msg_show("SPROUTLING!"); } else { add_score(5000); msg_show("5000"); }
        break;
    case IT_B:
        if (G.bombs < 6) G.bombs++;
        msg_show("BLOOM BOMB!");
        break;
    case IT_D:
        G.shield = 1;
        sfx_play(SFX_SHIELD);
        msg_show("BUBBLE SHIELD!");
        break;
    case IT_1UP:
        if (G.lives < 9) G.lives++;
        msg_show("1UP!");
        break;
    }
    fx_spawn(FX_SPARK, it->x, it->y, 0, 0);
    it->active = 0;
}

static void items_update(void) {
    for (int i = 0; i < MAX_ITEMS; i++) {
        Item *it = &items[i];
        if (!it->active) continue;
        it->t++;
        if (it->cyc && (it->t % 80) == 0) it->kind = (u8)(IT_S + (it->kind - IT_S + 1) % 3);
        if (it->vy < 120) it->vy += 12;
        it->x += it->vx;
        it->y += it->vy + ((isin(it->t * 4) * 40) >> 8);
        int x = it->x >> 8, y = it->y >> 8;
        if (y < 16 && it->vy < 0) it->vy = -it->vy;
        if (y > 148) { it->y = FIX(148); it->vy = -360; }
        if (x < 8 && it->vx < 0 && it->bounces < 2) { it->vx = 160; it->bounces++; }
        if (x > 232 && it->vx > 0) it->vx = -160;
        if (x < -16) { it->active = 0; continue; }
        if (pl.alive && iabs(x - (pl.x >> 8)) < 15 && iabs(y - (pl.y >> 8)) < 15) item_collect(it);
    }
}

static void items_draw(void) {
    for (int i = 0; i < MAX_ITEMS; i++) {
        Item *it = &items[i];
        if (!it->active) continue;
        if (it->t > 900 && (it->t & 4)) continue;
        int fr = ((it->t >> 3) & 7) == 0;
        spr(OC_ITEM, (it->x >> 8) - 8, (it->y >> 8) - 8, SPR_ITEM + (it->kind * 2 + fr) * 4, S16, 4, 0);
    }
}

/* ---------------------------------------------------------------- effects */
static void fx_update_draw(void) {
    for (int i = 0; i < MAX_FX; i++) {
        Fx *f = &fx[i];
        if (!f->life) continue;
        int t = f->t++;
        if (f->t >= f->life) { f->life = 0; continue; }
        f->x += f->vx;
        f->y += f->vy;
        int x = f->x >> 8, y = f->y >> 8;
        switch (f->type) {
        case FX_EXPL: spr(OC_FX, x - 8, y - 8, SPR_EXPL16 + (t >> 2) * 4, S16, 3, 0); break;
        case FX_BIGEXPL: spr(OC_FX, x - 16, y - 16, SPR_EXPL32 + (t / 5) * 16, S32, 3, 0); break;
        case FX_SPARK: spr(OC_FX, x - 4, y - 4, SPR_SPARK + (t >> 1), S8, 3, 0); break;
        case FX_PUFF: spr(OC_FX, x - 4, y - 4, SPR_PUFF + t / 5, S8, 3, 0); break;
        case FX_PETAL:
            f->vx = (s16)((f->vx * 31) >> 5);
            f->vy = (s16)((f->vy * 31) >> 5);
            spr(OC_FX, x - 8, y - 8, SPR_BOMBPETAL + ((t >> 2) & 1) * 4, S16, 3, 0);
            break;
        case FX_POPUP: spr(OC_TOP, x - 8, y - 4, SPR_POPUP + f->a * 2, W16x8, 15, 0); break;
        case FX_DEBRIS:
            f->vy += 24;
            spr(OC_FX, x - 4, y - 4, SPR_SPARK + 1 + (t > 12), S8, 3, 0);
            break;
        }
    }
}

/* ---------------------------------------------------------------- stage script */
static void script_step(void) {
    if (hold == HOLD_MID) {
        if (boss_alive()) return;
        hold = HOLD_NONE;
    } else if (hold == HOLD_WAIT) {
        if (enemies_alive() && --hold_t > 0) return;
        hold = HOLD_NONE;
    }
    while (ev_i < ev_n && evs[ev_i].t <= stage_t) {
        const StageEv *e = &evs[ev_i++];
        switch (e->kind) {
        case EV_SPAWN: enemy_spawn_ev(e); break;
        case EV_MID: boss_start(G.stage, 1); hold = HOLD_MID; return;
        case EV_WAIT: hold = HOLD_WAIT; hold_t = e->p0; return;
        case EV_BOSS: phase = PH_WARNING; phase_t = 0; return;
        case EV_SPEED: scroll_speed = e->p0; break;
        }
    }
    stage_t++;
}

static void skip_to(int kind) {
    for (int i = 0; i < ev_n; i++) {
        if (evs[i].kind == kind) {
            ev_i = i;
            stage_t = evs[i].t;
            return;
        }
    }
}

/* ---------------------------------------------------------------- overlays */
static int pause_menu(void) {
    int sel = 0;
    music_pause(1);
    sfx_play(SFX_PAUSE);
    dim = 7;
    banner_show(0, BANNER_PAUSED, 6, BP_SKY);
    panel_draw(9, 10, 12, 5);
    int quit = -1;
    while (quit < 0) {
        text_put(12, 11, "RESUME ", TS_PANEL);
        text_put(12, 13, "RETIRE ", TS_PANEL);
        text_tile(10, 11, sel == 0 ? UI_CURSOR : UI_PAN);
        text_tile(10, 13, sel == 1 ? UI_CURSOR : UI_PAN);
        frame_wait();
        if (KEY_HIT(KEY_UP | KEY_DOWN)) { sel ^= 1; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_START)) quit = 0;
        if (KEY_HIT(KEY_A)) { quit = sel; sfx_play(sel ? SFX_BACK : SFX_SELECT); }
        if (KEY_HIT(KEY_B)) quit = 0;
    }
    text_clear_rows(6, 15);
    dim = 0;
    music_pause(0);
    return quit;
}

static void draw_frozen(void) {
    /* keep the last OAM staging; just wait */
    frame_wait();
}

static int continue_screen(void) {
    if (G.practice) return 0;
    music_stop();
    dim = 8;
    banner_show(0, BANNER_CONTINUE, 5, BP_PINK);
    panel_draw(6, 12, 18, 4);
    text_put(8, 13, "CREDITS LEFT", TS_PANEL);
    text_num(21, 13, (u32)(3 - G.continues), 1, TS_PANEL, 0);
    text_put(8, 14, "A: YES  B: FAST", TS_PANEL);
    int count = 9, t = 0, result = -1;
    if (G.continues >= 3) count = -1;
    while (result < 0) {
        if (count < 0) { result = 0; break; }
        if (t == 0) {
            banner_show(1, BANNER_DIGIT0 + count, 8, BP_GOLD);
            sfx_play(SFX_TICK);
        }
        draw_frozen();
        if (KEY_HIT(KEY_A | KEY_START)) { result = 1; sfx_play(SFX_SELECT); break; }
        t++;
        if (KEY_HIT(KEY_B)) t = 60;
        if (t >= 60) { t = 0; count--; }
    }
    text_clear_rows(5, 16);
    dim = 0;
    if (result) {
        if (G.score > G.run_best) G.run_best = G.score;
        G.continues++;
        G.score = 0;
        G.next_extend = extend_tab[0];
        G.lives = G.diff == 0 ? 3 : 2;
        G.bombs = G.diff == 0 ? 4 : 3;
        player_reset();
        pl.invuln = 180;
        hud_invalidate();
        music_play(SONG_STAGE1 + G.stage);
        if (phase == PH_BOSS) music_play(G.stage == 4 ? SONG_FINAL : SONG_BOSS);
    }
    return result;
}

static void results_screen(void) {
    int pct = G.spawned ? udiv(G.kills * 100, G.spawned) : 100;
    if (pct > 100) pct = 100;
    u32 v[5];
    v[0] = (u32)res_boss_bonus;
    v[1] = (u32)pct * 500;
    v[2] = (u32)G.max_chain * 1000;
    v[3] = G.misses ? 0 : 50000;
    v[4] = (u32)G.bombs * 5000;
    u32 total = v[0] + v[1] + v[2] + v[3] + v[4];
    banner_show(0, BANNER_CLEAR, 3, BP_GOLD);
    panel_draw(3, 6, 24, 11);
    static const char *const lbl[5] = { "BOSS BONUS", "ENEMIES", "MAX CHAIN", "NO MISS", "BOMB STOCK" };
    int line = 0, t = 0;
    u32 shown = 0;
    int done = 0, wait = 0;
    while (1) {
        if (!done && t % 18 == 0 && line < 5) {
            int y = 7 + line;
            text_put(4, y, lbl[line], TS_PANEL);
            if (line == 1) { text_num(13, y, (u32)pct, 3, TS_PANEL, 0); text_put(16, y, "%", TS_PANEL); }
            if (line == 2) text_num(14, y, (u32)G.max_chain, 2, TS_PANEL, 0);
            if (line == 3 && G.misses) text_put(4, y, "MISSES    ", TS_PANEL);
            if (line == 3 && G.misses) text_num(13, y, (u32)G.misses, 2, TS_PANEL, 0);
            if (line == 4) { text_put(15, y, "x", TS_PANEL); text_num(16, y, (u32)G.bombs, 1, TS_PANEL, 0); }
            text_num(18, y, v[line], 8, TS_PANEL, 0);
            sfx_play(SFX_TICK);
            line++;
            if (line == 5) {
                text_put(4, 13, "--------------------", TS_PANEL);
                text_put(4, 14, "TOTAL", TS_PANEL);
            }
        }
        if (line == 5 && !done) {
            u32 step = total / 40 + 1;
            if (shown + step >= total) { add_score(total - shown); shown = total; done = 1; }
            else { shown += step; add_score(step); if ((t & 3) == 0) sfx_play(SFX_TICK); }
            text_num(18, 14, shown, 8, TS_PANEL, 0);
        }
        if (KEY_HIT(KEY_A | KEY_START) && !done) {
            /* skip to the end */
            for (; line < 5; line++) {
                text_put(4, 7 + line, lbl[line], TS_PANEL);
                text_num(18, 7 + line, v[line], 8, TS_PANEL, 0);
            }
            text_put(4, 13, "--------------------", TS_PANEL);
            text_put(4, 14, "TOTAL", TS_PANEL);
            add_score(total - shown);
            shown = total;
            text_num(18, 14, shown, 8, TS_PANEL, 0);
            done = 1;
            wait = 0;
        } else if (done) {
            wait++;
            if ((wait > 30 && KEY_HIT(KEY_A | KEY_START)) || wait > 240) break;
        }
        hud_update();
        /* the stage keeps scrolling behind the panel */
        oam_clear();
        player_update(0);
        player_draw();
        fx_update_draw();
        scroll_acc += scroll_speed;
        scroll_dist = scroll_acc >> 8;
        bg_update();
        frame_wait();
        t++;
    }
    text_clear_rows(3, 16);
}

/* ---------------------------------------------------------------- stage */
static void stage_setup(void) {
    set_display(0x0080);
    frame_wait();
    hdma_stop();
    REG_DISPCNT = 0x0080;
    load_stage_bg(G.stage);
    load_stage_sprites(G.stage);
    text_clear();
    enemies_reset();
    bullets_clear();
    groups_reset();
    memset(shots, 0, sizeof(shots));
    memset(fx, 0, sizeof(fx));
    memset(items, 0, sizeof(items));
    memset(&boss, 0, sizeof(boss));
    boss_hittable = 0;
    bhit_n = 0;
    boss_dmg = 0;
    player_reset();
    evs = stage_events[G.stage];
    ev_n = stage_event_count[G.stage];
    ev_i = 0;
    stage_t = 0;
    hold = HOLD_NONE;
    scroll_acc = 0;
    scroll_speed = 256;
    G.kills = G.spawned = G.chain = G.chain_t = G.max_chain = G.misses = 0;
    res_boss_bonus = 0;
    msg_t = 0;
    hud_invalidate();
    phase = PH_INTRO;
    phase_t = 0;
    shake_x = shake_y = 0;
    flash = 0;
    dim = 0;
    rnd_seed(0x51EED + G.stage * 977 + G.diff * 31);
    set_display(0x1F40);
#ifdef DEBUG_BOSS
    skip_to(EV_BOSS);
#elif defined(DEBUG_MID)
    skip_to(EV_MID);
#else
    (void)skip_to;
#endif
}

int play_stage(void) {
    stage_setup();
    music_play(SONG_STAGE1 + G.stage);
    fade = 16;
    int result = -1;
    while (result < 0) {
        phase_t++;
        oam_clear();
        /* phase logic */
        if (phase == PH_INTRO) {
            if (phase_t == 1) {
                banner_show(0, BANNER_STAGE1 + G.stage, 6, BP_GOLD);
                banner_show(1, BANNER_NAME1 + G.stage, 9, BP_PINK);
            }
            if (phase_t < 20) fade = 16 - phase_t;
            else fade = 0;
            if (phase_t == 150) { text_clear_rows(6, 10); phase = PH_PLAY; phase_t = 0; }
        } else if (phase == PH_PLAY) {
            script_step();
        } else if (phase == PH_WARNING) {
            if (phase_t == 1) { music_stop(); bullets_count(); }
            if (phase_t % 64 == 1 && phase_t < 190) sfx_play(SFX_WARN);
            if ((phase_t & 31) == 1) banner_show(0, BANNER_WARNING, 6, BP_RED);
            if ((phase_t & 31) == 24) banner_clear(6);
            if (phase_t == 2) text_center(9, boss_names[G.stage], TS_HUD);
            if (phase_t == 200) {
                text_clear_rows(6, 10);
                boss_start(G.stage, 0);
                music_play(G.stage == 4 ? SONG_FINAL : SONG_BOSS);
                phase = PH_BOSS;
                phase_t = 0;
            }
        } else if (phase == PH_BOSS) {
            if (!boss_alive()) { phase = PH_CLEAR; phase_t = 0; }
        } else if (phase == PH_CLEAR) {
            if (phase_t == 1) { cancel_bullets(); pl.invuln = 400; }
            if (phase_t == 70) music_play(SONG_CLEAR);
            if (phase_t == 100) {
                results_screen();
                result = PLAY_CLEAR;
                break;
            }
        }
        /* entities */
        int control = !(phase == PH_INTRO && phase_t < 50);
        player_update(control);
        shots_update();
        enemies_update();
        boss_update();
        shots_collide();
        for (int i = 0; i < hitfx_n; i++) fx_spawn(FX_SPARK, FIX(hitfx_x[i]), FIX(hitfx_y[i]), 0, 0);
        hitfx_n = 0;
        items_update();
        int vul = pl.alive && !pl.invuln && phase != PH_CLEAR;
        if (bullets_update(pl.x >> 8, pl.y >> 8, vul)) player_hit();
        if (cancel_collected) {
            int mult = 1 + G.chain / 6;
            if (mult > 8) mult = 8;
            add_score((u32)cancel_collected * 20 * (u32)mult);
            cancel_collected = 0;
        }
        /* body collisions */
        if (vul && pl.alive) {
            int px = pl.x >> 8, py = pl.y >> 8;
            for (int i = 0; i < MAX_ENEMIES; i++) {
                Enemy *e = &en[i];
                if (!e->active || e->dead) continue;
                const EnemyType *et = &etype[e->type];
                if (iabs((e->x >> 8) - px) < et->hw - 1 && iabs((e->y >> 8) - py) < et->hh - 1) { player_hit(); break; }
            }
            if (pl.alive && !pl.invuln && boss_hittable) {
                for (int b = 0; b < bhit_n; b++)
                    if (px > bhit[b][0] + 4 && px < bhit[b][2] - 4 && py > bhit[b][1] + 4 && py < bhit[b][3] - 4) { player_hit(); break; }
            }
        }
        if (G.chain_t > 0 && --G.chain_t == 0) G.chain = 0;
        /* draw */
        player_draw();
        shots_draw();
        enemies_draw();
        boss_draw();
        items_draw();
        fx_update_draw();
        hud_update();
        if (shake_x || shake_y) {
            shake_x = shake_x > 0 ? -(shake_x - 1) : -shake_x;
            shake_y = shake_y > 0 ? -(shake_y - 1) : -shake_y;
        }
        if (flash > 0 && !pl.bomb_t) flash--;
        if (shot_sfx_cd > 0) shot_sfx_cd--;
        /* scrolling + subtle vertical camera */
        scroll_acc += scroll_speed;
        scroll_dist = scroll_acc >> 8;
        int cam = ((pl.y >> 8) - 84) / 6;
        if (cam > 8) cam = 8;
        if (cam < -8) cam = -8;
        if (bg_shift_y < cam) bg_shift_y++;
        if (bg_shift_y > cam) bg_shift_y--;
        bg_update();
        frame_wait();
        /* deaths and menus */
        if (!pl.alive && pl.respawn == 1 && G.lives == 0) {
            if (!continue_screen()) { result = PLAY_GAMEOVER; break; }
        }
        if (KEY_HIT(KEY_START) && phase != PH_CLEAR) {
            if (pause_menu()) { result = PLAY_QUIT; break; }
            hud_invalidate();
        }
    }
    if (G.score > G.run_best) G.run_best = G.score;
    fade_out(2);
    hdma_stop();
    return result;
}
