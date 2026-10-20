/* Sky Sprout - the Gloom Fleet's common enemies, their AI, and bullet patterns. */
#include "game.h"

const EnemyType etype[ET_N] = {
    /* hw hh size frames tile        anim pal hp score */
    { 5, 5, S16, 2, SPR_GNAT, 4, 6, 2, 100 },
    { 6, 4, S16, 2, SPR_SWOOP, 6, 6, 3, 150 },
    { 6, 5, S16, 2, SPR_TURRET, 16, 6, 9, 300 },
    { 5, 5, S16, 2, SPR_MINE, 20, 6, 6, 200 },
    { 6, 6, S16, 4, SPR_SPIN, 4, 6, 5, 250 },
    { 6, 4, S16, 2, SPR_DIVER, 3, 6, 3, 200 },
    { 12, 9, S32, 2, SPR_CARRIER, 8, 6, 16, 500 },
    { 13, 11, S32, 2, SPR_HEAVY, 10, 7, 42, 2000 },
};

const u16 bk_tile[BK_N] = {
    SPR_EB_SM, SPR_EB_SB, SPR_EB_SV, SPR_EB_SA, SPR_EB_MM, SPR_EB_MB, SPR_EB_MV, SPR_EB_MA,
    SPR_EB_RM, SPR_EB_RB, SPR_EB_RV, SPR_EB_BM, SPR_EB_BB, SPR_EB_BV, SPR_EB_BA, SPR_EB_STARA,
    SPR_EB_STARM, SPR_PETALPICK,
};
const u8 bk_ss[BK_N] = { S8, S8, S8, S8, S8, S8, S8, S8, S8, S8, S8, S16, S16, S16, S16, S8, S8, S8 };
const u8 bk_half[BK_N] = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 8, 8, 4, 4, 4 };
const u8 bk_rad[BK_N] = { 2, 2, 2, 2, 3, 3, 3, 3, 2, 2, 2, 5, 5, 5, 5, 3, 3, 0 };

static const u8 fire_iv[] = { 0, 100, 120, 140, 130, 120, 70, 0, 6, 40 };

static u8 grp_killed[64];

void groups_reset(void) { memset(grp_killed, 0, sizeof(grp_killed)); }

/* ---------------------------------------------------------------- bullets */
int eb_spawn(s32 x, s32 y, int ang, int spd, int kind, int beh, int p, int q) {
    for (int i = 0; i < MAX_EBUL; i++) {
        Bullet *b = &ebul[i];
        if (b->active) continue;
        b->active = 1;
        b->x = x;
        b->y = y;
        b->ang = (u16)((ang & 255) << 8);
        b->spd = (s16)((spd * G.spdmul) >> 8);
        b->kind = (u8)kind;
        b->beh = (u8)beh;
        b->t = 0;
        b->p = (s16)p;
        b->q = (s16)q;
        int a = ang & 255;
        b->vx = (s16)((icos(a) * b->spd) >> 8);
        b->vy = (s16)((isin(a) * b->spd) >> 8);
        return i;
    }
    return -1;
}

void shoot_fan(s32 x, s32 y, int ang, int n, int spread, int spd, int kind) {
    int a = ang - ((n - 1) * spread) / 2;
    for (int i = 0; i < n; i++, a += spread) eb_spawn(x, y, a, spd, kind, BEH_LINE, 0, 0);
}

void shoot_ring(s32 x, s32 y, int ang0, int n, int spd, int kind) {
    if (G.diff == 0) n = (n * 3) >> 2;
    if (n < 3) n = 3;
    for (int i = 0; i < n; i++) eb_spawn(x, y, ang0 + udiv(i * 256, n), spd, kind, BEH_LINE, 0, 0);
}

void bullets_clear(void) {
    for (int i = 0; i < MAX_EBUL; i++) ebul[i].active = 0;
}

int bullets_count(void) {
    int n = 0;
    for (int i = 0; i < MAX_EBUL; i++) n += ebul[i].active;
    return n;
}

/* ---------------------------------------------------------------- enemies */
void enemies_reset(void) {
    memset(en, 0, sizeof(en));
}

int enemies_alive(void) {
    int n = 0;
    for (int i = 0; i < MAX_ENEMIES; i++) n += en[i].active;
    return n;
}

static Enemy *enemy_new(void) {
    for (int i = 0; i < MAX_ENEMIES; i++)
        if (!en[i].active) return &en[i];
    return 0;
}

Enemy *enemy_spawn(int type, int ai, int x, int y, int p0, int p1, int p2, int fire) {
    Enemy *e = enemy_new();
    if (!e) return 0;
    memset(e, 0, sizeof(*e));
    e->active = 1;
    e->type = (u8)type;
    e->ai = (u8)ai;
    e->x = x << 8;
    e->y = y << 8;
    e->ox = (s16)x;
    e->oy = (s16)y;
    e->p0 = (s16)p0;
    e->p1 = (s16)p1;
    e->p2 = (s16)p2;
    e->fire = (u8)fire;
    e->hp = (s16)(etype[type].hp + (G.stage >> 1) * (etype[type].hp >> 2));
    e->fcd = (u8)(30 + (rnd() & 31));
    switch (ai) {
    case AI_LINE: e->vx = (s16)p0; e->vy = (s16)p1; break;
    case AI_SINE: case AI_SWOOP: case AI_STOP: case AI_CHASE: case AI_LOOP: case AI_DRIFT: case AI_HOVER:
        e->vx = (s16)p0; break;
    case AI_DIVE: e->vy = (s16)(y < 80 ? p0 : -p0); break;
    case AI_ARC: e->vx = (s16)p0; e->vy = (s16)p1; break;
    }
    return e;
}

void enemy_spawn_ev(const StageEv *ev) {
    int y = ev->y;
    if (y == 999) {
        y = pl.y >> 8;
        if (y < 24) y = 24;
        if (y > 140) y = 140;
    }
    Enemy *e = enemy_spawn(ev->type, ev->ai, ev->x, y, ev->p0, ev->p1, ev->p2, ev->fire);
    if (!e) return;
    e->drop = ev->drop;
    e->group = ev->group;
    e->killed = (u8)(ev->gsize & 0x7F);   /* bit 7 = has entered the screen */
}

static int can_fire(Enemy *e) {
    int x = e->x >> 8, y = e->y >> 8;
    if (x < 16 || x > 228 || y < 8 || y > 152 || !pl.alive) return 0;
    int dx = iabs(x - (pl.x >> 8)), dy = iabs(y - (pl.y >> 8));
    return dx + dy > 36;
}

static void enemy_fire(Enemy *e, int pattern) {
    s32 x = e->x, y = e->y;
    int a = aim_player(x, y);
    switch (pattern) {
    case F_AIM: eb_spawn(x, y, a, 300, BK_SM, BEH_LINE, 0, 0); break;
    case F_AIM3: shoot_fan(x, y, a, G.diff == 2 ? 5 : 3, 12, 320, BK_RB); break;
    case F_RING: shoot_ring(x, y, rnd() & 31, 8 + G.diff * 2, 260, BK_SV); break;
    case F_BURST:
        for (int i = 0; i < 3; i++) eb_spawn(x, y, a, 300 + i * 50, BK_MA, BEH_LINE, 0, 0);
        break;
    case F_SPRAY: shoot_fan(x, y, a, 5, 10, 280, BK_SB); break;
    case F_HEAVY:
        if ((e->t / 70) & 1) shoot_fan(x - FIX(10), y, a, 5 + G.diff, 14, 300, BK_RM);
        else eb_spawn(x - FIX(10), y, a, 240, BK_BV, BEH_ACCEL, 6, 520);
        break;
    case F_SPIRAL:
        eb_spawn(x, y, e->t * 7, 240, BK_SA, BEH_LINE, 0, 0);
        eb_spawn(x, y, e->t * 7 + 128, 240, BK_SA, BEH_LINE, 0, 0);
        break;
    case F_RAIN:
        eb_spawn(x, y, 128 + 16 + (rnd() & 15), 200, BK_SB, BEH_GRAV, 7, 640);
        break;
    }
}

void enemy_kill(Enemy *e, int by_player) {
    const EnemyType *et = &etype[e->type];
    int big = et->size == S32;
    explode(e->x, e->y, big);
    if (by_player) {
        kill_chain();
        int mult = 1 + G.chain / 6;
        if (mult > 8) mult = 8;
        add_score((u32)et->score * (u32)mult);
        G.kills++;
        if (mult >= 2 && (G.chain % 6) == 0) popup_mult(e->x, e->y - FIX(10), mult);
        if (e->group) {
            int g = e->group & 63;
            grp_killed[g]++;
            if (grp_killed[g] == (e->killed & 0x7F) && e->drop) item_spawn(e->drop - 1, e->x, e->y);
        } else if (e->drop) {
            item_spawn(e->drop - 1, e->x, e->y);
        }
        if (e->fire == F_DEATHRING || (G.diff == 2 && !big && (rnd() & 3) == 0)) {
            if (e->fire == F_DEATHRING) shoot_ring(e->x, e->y, rnd() & 63, 8 + G.diff * 2, 220, BK_MM);
            else eb_spawn(e->x, e->y, aim_player(e->x, e->y), 260, BK_SM, BEH_LINE, 0, 0);
        }
    }
    e->active = 0;
}

void enemies_update(void) {
    int px = pl.x >> 8, py = pl.y >> 8;
    for (int i = 0; i < MAX_ENEMIES; i++) {
        Enemy *e = &en[i];
        if (!e->active) continue;
        if (e->dead) { enemy_kill(e, 1); continue; }
        e->t++;
        if (e->flash) e->flash--;
        if (e->lcd) e->lcd--;
        int x = e->x >> 8, y = e->y >> 8;
        switch (e->ai) {
        case AI_LINE:
            break;
        case AI_SINE:
            e->vy = (s16)((icos(e->t * e->p1) * e->p2 * e->p1 * 25) >> 10);
            break;
        case AI_SWOOP:
            if (x < e->p1 || e->state) {
                e->state = 1;
                if (iabs(e->vy) < 560) e->vy += e->p2;
                if (e->vx < -128) e->vx += 10;
            }
            break;
        case AI_STOP:
            if (e->state == 0) {
                if ((e->vx < 0 && x <= e->p1) || (e->vx > 0 && x >= e->p1)) { e->state = 1; e->t = 0; }
            } else if (e->state == 1) {
                e->vx = (s16)((e->vx * 7) >> 3);
                e->vy = (s16)((e->vy * 7) >> 3);
                if (e->t > e->p2) { e->state = 2; e->t = 0; }
            } else {
                if (e->vx < 384) e->vx += 12;
                e->vy += y < 80 ? -6 : 6;
            }
            break;
        case AI_DIVE:
            if (e->state == 0) {
                if ((e->vy > 0 && y >= e->p1) || (e->vy < 0 && y <= e->p1)) e->state = 1;
            } else {
                e->vy = (s16)((e->vy * 14) >> 4);
                if (e->vx > -e->p2) e->vx -= 24;
            }
            break;
        case AI_CHASE:
            if (py > y + 2 && e->vy < 300) e->vy += 12;
            else if (py < y - 2 && e->vy > -300) e->vy -= 12;
            break;
        case AI_ARC:
            e->vy += e->p2;
            break;
        case AI_LOOP:
            if (e->state == 0 && x < e->p1) { e->state = 1; e->t = 0; }
            if (e->state == 1) {
                int a = (e->t * 3) & 255;
                e->vx = (s16)((-icos(a) * e->p2) >> 8);
                e->vy = (s16)((-isin(a) * e->p2) >> 8);
                if (e->t * 3 >= 256) { e->state = 2; e->vx = e->p0; e->vy = 0; }
            }
            break;
        case AI_DRIFT:
            e->vy = (s16)((isin(e->t * 2 + i * 40) * 60) >> 8);
            break;
        case AI_HOVER:
            if (e->state == 0) {
                if (x <= e->p1) { e->state = 1; e->t = 0; }
            } else if (e->state == 1) {
                e->vx = (s16)((e->vx * 7) >> 3);
                e->vy = (s16)((isin(e->t * 2) * 90) >> 8);
                if (e->t > e->p2) e->state = 2;
            } else {
                if (e->vx > -300) e->vx -= 6;
                e->vy = 0;
            }
            break;
        }
        e->x += e->vx;
        e->y += e->vy;
        x = e->x >> 8;
        y = e->y >> 8;
        /* first time on screen counts toward the kill ratio */
        if (!(e->killed & 0x80) && x < 236 && x > 0 && y > 0 && y < 160) {
            e->killed |= 0x80;
            G.spawned++;
        }
        if (x < -40 || x > 300 || y < -48 || y > 208) { e->active = 0; continue; }
        /* firing */
        if (e->fire && e->fire != F_DEATHRING) {
            if (e->ai == AI_STOP && e->state != 1) continue;
            if (e->fcd) e->fcd--;
            if (!e->fcd) {
                int iv = fire_iv[e->fire];
                e->fcd = (u8)((iv * G.ratemul) >> 8);
                if (!e->fcd) e->fcd = 1;
                if (can_fire(e)) enemy_fire(e, e->fire);
            }
        }
    }
}

void enemies_draw(void) {
    for (int i = 0; i < MAX_ENEMIES; i++) {
        Enemy *e = &en[i];
        if (!e->active) continue;
        const EnemyType *et = &etype[e->type];
        int sz = et->size == S32 ? 32 : 16;
        int fr = (e->t / et->anim) % et->frames;
        int tile = et->tile + fr * (sz == 32 ? 16 : 4);
        spr(OC_ENEMY, (e->x >> 8) - sz / 2, (e->y >> 8) - sz / 2, tile, et->size, e->flash ? 5 : et->pal, 0);
    }
}
