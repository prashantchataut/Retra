/* Turbo Drift - race simulation, AI, collisions, HUD and the race loop. */
#include "game.h"
#include "render.h"

const CarStats car_stats[NUM_CARS] = {
    { "KITSUNE", 246, 70, 100, 100, 0 },
    { "RONIN", 266, 66, 86, 112, 7500 },
    { "KAZE ZR", 284, 82, 96, 104, 18000 },
    { "MOCHI GT", 298, 88, 108, 128, 0 },
};
const char *const rival_names[5] = { "VEGA", "JUNO", "RIKU", "ZARA", "KAI" };
static const u8 rival_paints[5] = { 1, 2, 7, 3, 5 };

#define MAX_RIVALS 5
#define MAX_TRAFFIC 22
#define NCARS_MAX (1 + MAX_RIVALS + MAX_TRAFFIC)
#define CAR_LEN 560
#define CAR_WQ8 118
#define LANE_X 168
#define CARHALF_Q8 62
#define CURVE_K 40

typedef struct {
    s32 z, x, tx, speed, top, acc;
    s32 lap, finish;
    s16 lane_t, bump_t, nitro_t, yaw;
    u8 kind, body, pal, done, passed, prevahead;
} Car;

static Car cars[NCARS_MAX];
static int ncars, nrivals;
static const TrackDef *trk;
static const ObjDef *odefs;
static s32 track_len;
static RaceSetup setup;
static RoadView view;

static struct {
    s32 nitro, nitro_max, top, accel, grip;
    int nitro_on, drift, drift_t, spin_t, boost_t, steer_v, anim, frame, flip, loaded_frame;
    s32 cam_x, sky_x;
    u32 time, lap_start, best_lap, last_lap;
    int finished, finish_t, offroad, shake, popup_t, banner_t, banner, pos, last_pos;
    const char *popup;
    int launch, a_since, sx, sy, focal, brake, ai, rpm;
} P;

/* ------------------------------------------------------------------ helpers */
static inline s32 wrapz(s32 z) {
    while (z >= track_len) z -= track_len;
    while (z < 0) z += track_len;
    return z;
}
static inline s32 dz_wrap(s32 d) {
    if (d > (track_len >> 1)) d -= track_len;
    else if (d < -(track_len >> 1)) d += track_len;
    return d;
}
static inline int seg_wrap(int s) { while (s >= trk->n) s -= trk->n; while (s < 0) s += trk->n; return s; }
static s32 road_y(s32 z) {
    int s = z >> 8, f = z & 255;
    int s2 = s + 1 >= trk->n ? 0 : s + 1;
    return trk->y[s] + (((trk->y[s2] - trk->y[s]) * f) >> 8);
}
static int curve_at(s32 z) { return trk->curve[z >> 8]; }
static int curve_ahead(s32 z, int segs) {
    int s = z >> 8, m = 0;
    for (int i = 0; i < segs; i += 2) {
        int c = trk->curve[seg_wrap(s + i)];
        if (iabs(c) > iabs(m)) m = c;
    }
    return m;
}
static s32 total_of(const Car *c) { return c->lap * track_len + c->z; }

/* ------------------------------------------------------------------ setup */
static void race_init(const RaceSetup *rs) {
    setup = *rs;
    trk = &track_defs[rs->track];
    track_len = trk->n << 8;
    odefs = objdefs[trk->env];
    rnd_seed(0x9E37 + rs->track * 977 + save.total_races * 131 + rs->mode * 7);
    memset(cars, 0, sizeof(cars));
    memset(&P, 0, sizeof(P));
    const CarStats *cs = &car_stats[rs->car];
    P.top = (cs->top + rs->upg[0] * 9) << 8;
    P.accel = cs->accel + rs->upg[1] * 10;
    P.grip = cs->grip + rs->upg[2] * 8;
    P.nitro_max = 1024;
    P.nitro = rs->mode == MODE_ATTRACT ? 0 : 256 + rs->upg[3] * 64;
    P.loaded_frame = -1;
    P.focal = FOCAL;
    P.best_lap = 0;
    P.ai = rs->mode == MODE_ATTRACT;
    nrivals = rs->rivals > MAX_RIVALS ? MAX_RIVALS : rs->rivals;
    ncars = 1 + nrivals;
    /* grid: two columns behind the line; player at the back */
    int nslots = nrivals + 1;
    for (int i = 0; i < nslots; i++) {
        int slot = i;
        Car *c = &cars[i == nslots - 1 ? 0 : i + 1];
        c->z = track_len - 420 - slot * 700;
        c->x = (slot & 1) ? LANE_X / 2 + 30 : -(LANE_X / 2 + 30);
        if (nslots == 1) c->x = 0;
        c->tx = c->x;
        c->lap = -1;
    }
    cars[0].kind = 0;
    cars[0].top = P.top;
    static const u8 skill_base[4] = { 88, 93, 98, 103 };
    for (int i = 1; i <= nrivals; i++) {
        Car *c = &cars[i];
        c->kind = 1;
        c->body = (u8)((i - 1) % 3);
        c->pal = (u8)i;
        int sk = skill_base[rs->skill & 3] + (i - 1) * 2;
        if (rs->mode == MODE_ATTRACT) sk = 70 + i * 3;
        c->top = (250 * sk / 100) << 8;
        c->acc = 150 + i * 12 + rs->skill * 20;
        make_car_pal(PAL_OBJ + i * 16, paint_ramps + rival_paints[i - 1] * 4, 0);
    }
    /* traffic spread around the lap, away from the grid */
    int nt = rs->traffic > MAX_TRAFFIC ? MAX_TRAFFIC : rs->traffic;
    for (int i = 0; i < nt; i++) {
        Car *c = &cars[ncars++];
        c->kind = 2;
        c->body = (rnd() % 10) < 3 ? 1 : 0;
        c->pal = (u8)(6 + rnd() % 3);
        s32 spacing = (track_len - 60 * 256) / (nt ? nt : 1);
        c->z = 30 * 256 + spacing * i + (s32)(rnd() % (u32)(spacing / 2 + 1));
        c->x = ((s32)(rnd() % 3) - 1) * LANE_X;
        c->tx = c->x;
        c->speed = (s32)(92 + rnd() % 50) << 8;
        c->top = c->speed;
        c->prevahead = 1;
    }
    make_car_pal(PAL_OBJ, paint_ramps + rs->paint * 4, 1);
    P.cam_x = cars[0].x * ROAD_W >> 8;
}

/* ------------------------------------------------------------------ player physics */
static void spin_out(int dir) {
    if (P.spin_t) return;
    P.spin_t = 51;
    P.flip = dir < 0;
    P.drift = 0;
    P.drift_t = 0;
    P.nitro_on = 0;
    P.shake = 18;
    sfx_play(SFX_CRASH);
}

static void popup(const char *s) { P.popup = s; P.popup_t = 70; }

static void player_update(int steer, int gas, int brake, int drift_btn, int nitro_btn) {
    Car *c = &cars[0];
    s32 top = P.top;
    s32 tgt = top;
    if (P.nitro_on) tgt += top * 3 / 20;
    if (P.boost_t) { tgt += top >> 4; P.boost_t--; }
    P.offroad = iabs(c->x) > 256 + 24;
    if (P.offroad) tgt = top * 45 / 100;

    if (P.spin_t) {
        P.spin_t--;
        c->speed -= c->speed / 22;
        c->x += P.flip ? 2 : -2;
        if (P.spin_t == 0) P.anim = 0;
        steer = 0;
        gas = brake = 0;
    }
    P.brake = brake && c->speed > 0;
    if (gas) {
        if (c->speed < tgt) {
            s32 a = (P.accel * 5) * (tgt - c->speed) / (tgt >> 8 ? (tgt >> 8) : 1);
            a = (a >> 8) + 40;
            if (P.nitro_on) a += 120;
            c->speed += a;
            if (c->speed > tgt) c->speed = tgt;
        } else {
            c->speed -= (c->speed - tgt) >> 5;
        }
    } else if (brake) {
        c->speed -= 900;
    } else {
        c->speed -= 160 + (c->speed >> 9);
    }
    if (P.offroad && c->speed > tgt) c->speed -= (c->speed - tgt) >> 4;
    if (c->speed < 0) c->speed = 0;

    s32 spct = (c->speed << 8) / (top >> 8 ? top >> 8 : 1) >> 8;      /* 0..~300 */
    /* drift state */
    if (!P.spin_t && drift_btn && steer && spct > 110 && !P.offroad) {
        if (!P.drift) { P.drift = steer; P.drift_t = 0; }
        else if (steer == -P.drift && P.drift_t > 8) { P.drift = steer; P.drift_t = 4; }
    } else if (P.drift) {
        if (P.drift_t > 40 && !P.offroad) {
            P.boost_t = 34;
            c->speed += top >> 4;
            popup("DRIFT BOOST!");
            sfx_play(SFX_BOOST);
        }
        P.drift = 0;
        P.drift_t = 0;
    }
    /* steering */
    int target_v = steer * 256;
    if (P.steer_v < target_v) P.steer_v += P.steer_v < 0 ? 64 : 40;
    if (P.steer_v > target_v) P.steer_v -= P.steer_v > 0 ? 64 : 40;
    if (iabs(P.steer_v - target_v) < 40) P.steer_v = target_v;
    s32 grip = P.grip;
    s32 dx = (((P.steer_v * spct) >> 8) * grip * 9) >> 15;
    int curve = curve_at(c->z);
    s32 cent = (curve * ((spct * spct) >> 8)) >> 10;
    if (P.drift) {
        P.drift_t++;
        dx = dx * 4 / 3;
        cent >>= 1;
        c->speed -= c->speed >> 9;
        if (P.nitro < P.nitro_max && !P.nitro_on) P.nitro += spct >> 4;
    }
    if (P.ai) cent = 0;
    c->x += dx - cent;
    if (c->x > 700) { c->x = 700; c->speed -= c->speed >> 5; }
    if (c->x < -700) { c->x = -700; c->speed -= c->speed >> 5; }

    /* nitro */
    if (nitro_btn && P.nitro > 0 && !P.spin_t && gas) {
        if (!P.nitro_on) sfx_play(SFX_NITRO);
        P.nitro_on = 1;
        P.nitro -= 7 - setup.upg[3];
        if (P.nitro <= 0) { P.nitro = 0; P.nitro_on = 0; }
    } else {
        P.nitro_on = 0;
    }
    if (P.nitro > P.nitro_max) P.nitro = P.nitro_max;

    /* animation frame */
    int ta;
    if (P.drift) ta = P.drift * (P.drift_t < 10 ? 4 : P.drift_t < 22 ? 5 : 6);
    else ta = (P.steer_v * 3) / 256;
    if (P.anim < ta) P.anim++;
    else if (P.anim > ta) P.anim--;

    c->z += c->speed >> 8;
    if (c->z >= track_len) { c->z -= track_len; c->lap++; }
    if (P.shake) P.shake--;
    if (P.offroad && c->speed > (20 << 8) && !P.shake) P.shake = 2;
}

/* frame selection (handles spin sequence) */
static void player_frame(void) {
    int fr, flip;
    if (P.spin_t) {
        static const u8 seq[17] = { 0, 5, 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7, 5, 0 };
        int step = (51 - P.spin_t) / 3;
        if (step > 16) step = 16;
        fr = seq[step];
        flip = P.flip ^ (step > 8);
    } else {
        fr = iabs(P.anim);
        flip = P.anim < 0;
    }
    P.frame = fr;
    P.flip = P.spin_t ? P.flip : flip;
    if (fr != P.loaded_frame) {
        vb_copy(OBJ_VRAM + T_PLAYER * 16, car_frames[setup.car] + fr * 1024, 2048);
        P.loaded_frame = fr;
    }
    if (P.spin_t) P.flip = flip;
}

/* ------------------------------------------------------------------ collisions */
static void collide_scenery(void) {
    Car *c = &cars[0];
    if (iabs(c->x) < 256 + 20 || P.spin_t) return;
    int s0 = c->z >> 8;
    for (int ds = 0; ds <= 1; ds++) {
        int s = seg_wrap(s0 + ds);
        for (int j = trk->sidx[s]; j < trk->sidx[s + 1]; j++) {
            const TrackSprite *sp = &trk->spr[j];
            const ObjDef *od = &odefs[sp->type & 127];
            s32 ox = sp->x * 16;
            if (iabs(c->x - ox) < od->solid + CARHALF_Q8) {
                spin_out(c->x > ox ? 1 : -1);
                c->speed = c->speed / 3;
                c->x += c->x > 0 ? -40 : 40;
                return;
            }
        }
    }
}

static void collide_cars(void) {
    Car *p = &cars[0];
    for (int i = 1; i < ncars; i++) {
        Car *o = &cars[i];
        s32 dz = dz_wrap(o->z - p->z);
        s32 dx = o->x - p->x;
        int ahead = dz > 0;
        if (o->kind == 2 && !P.ai) {
            /* near miss: we just passed it closely */
            if (o->prevahead && !ahead && iabs(dz) < 2000 && iabs(dx) < CAR_WQ8 + 90 && iabs(dx) >= CAR_WQ8 &&
                p->speed > (p->speed - o->speed) && p->speed > P.top / 2 && !P.spin_t) {
                P.nitro += 150;
                popup("NEAR MISS!");
                sfx_play(SFX_NEAR);
            }
            o->prevahead = (u8)ahead;
        }
        if (iabs(dz) >= CAR_LEN || iabs(dx) >= CAR_WQ8) continue;
        if (o->bump_t) continue;
        if (P.ai && setup.mode == MODE_ATTRACT) continue;
        o->bump_t = 20;
        if (ahead) {
            s32 rel = p->speed - o->speed;
            if (rel > P.top * 3 / 10 && o->kind == 2) {
                spin_out(dx > 0 ? -1 : 1);
                p->speed = o->speed * 6 / 10;
            } else {
                p->speed = o->speed - (o->speed >> 3);
                if (p->speed < 0) p->speed = 0;
                sfx_play(SFX_BUMP);
                P.shake = 8;
            }
            p->z = wrapz(o->z - CAR_LEN);
            if (o->kind == 1) o->speed += rel > 0 ? rel >> 2 : 0;
        } else {
            o->speed = p->speed - (p->speed >> 3);
            o->z = wrapz(p->z - CAR_LEN);
            sfx_play(SFX_BUMP);
        }
        s32 push = dx > 0 ? 10 : -10;
        p->x -= push;
        if (o->kind == 1) o->x += push;
    }
}

/* ------------------------------------------------------------------ AI + traffic */
static void ai_update(void) {
    Car *p = &cars[0];
    s32 ptot = total_of(p);
    for (int i = 1; i < ncars; i++) {
        Car *c = &cars[i];
        if (c->bump_t) c->bump_t--;
        if (c->kind == 2) {
            c->z += c->speed >> 8;
            if (c->z >= track_len) c->z -= track_len;
            if (c->x != c->tx) c->x += c->tx > c->x ? 2 : -2;
            if (--c->lane_t <= 0) {
                c->lane_t = (s16)(300 + rnd() % 600);
                if ((rnd() & 3) == 0) c->tx = ((s32)(rnd() % 3) - 1) * LANE_X;
            }
            continue;
        }
        /* rival */
        s32 tgt = c->top;
        int ca = iabs(curve_ahead(c->z, 14));
        if (ca > 26) tgt -= tgt * (ca - 26) / 180;
        if (c->nitro_t > 0) { c->nitro_t--; tgt += tgt / 9; }
        else if (ca < 6 && (rnd() & 1023) < 3 && !c->done && setup.mode != MODE_ATTRACT) c->nitro_t = 80;
        if (setup.mode != MODE_ATTRACT && !c->done) {
            s32 d = total_of(c) - ptot;
            if (d > 5000) tgt -= tgt * (d > 30000 ? 25000 : d - 5000) / 260000;
            else if (d < -5000) tgt += tgt * (d < -30000 ? 25000 : -5000 - d) / 300000;
        }
        if (P.finished && !c->done) tgt += tgt >> 4;
        if (c->speed < tgt) { c->speed += c->acc; if (c->speed > tgt) c->speed = tgt; }
        else c->speed -= (c->speed - tgt) >> 5;
        /* look ahead for blockers in our lane */
        if (--c->lane_t <= 0) {
            c->lane_t = 6;
            int blocked = 0;
            s32 bspeed = 0;
            for (int j = 0; j < ncars; j++) {
                if (j == i) continue;
                Car *o = &cars[j];
                s32 dz = dz_wrap(o->z - c->z);
                if (dz <= 0 || dz > 2600) continue;
                if (iabs(o->x - c->tx) < CAR_WQ8 + 20 && o->speed < c->speed) { blocked = 1; bspeed = o->speed; }
            }
            if (blocked) {
                static const s16 lanes[3] = { -LANE_X, 0, LANE_X };
                int best = -1, bestd = 9999;
                for (int l = 0; l < 3; l++) {
                    int free = 1;
                    for (int j = 0; j < ncars && free; j++) {
                        if (j == i) continue;
                        Car *o = &cars[j];
                        s32 dz = dz_wrap(o->z - c->z);
                        if (dz > -900 && dz < 3000 && iabs(o->x - lanes[l]) < CAR_WQ8 + 16) free = 0;
                    }
                    int d = iabs(lanes[l] - c->x);
                    if (free && d < bestd) { best = l; bestd = d; }
                }
                if (best >= 0) c->tx = lanes[best];
                else if (c->speed > bspeed) c->speed = bspeed;
            }
        }
        if (c->x != c->tx) {
            s32 st = c->tx > c->x ? 5 : -5;
            if (iabs(c->tx - c->x) < 5) c->x = c->tx; else c->x += st;
            c->yaw = (s16)(st > 0 ? 1 : -1);
        } else {
            int cv = curve_at(c->z);
            c->yaw = (s16)(cv > 18 ? 1 : cv < -18 ? -1 : 0);
        }
        /* rival vs rival / traffic contact */
        for (int j = 1; j < ncars; j++) {
            if (j == i) continue;
            Car *o = &cars[j];
            s32 dz = dz_wrap(o->z - c->z);
            if (dz > 0 && dz < CAR_LEN && iabs(o->x - c->x) < CAR_WQ8) {
                if (c->speed > o->speed) c->speed = o->speed;
            }
        }
        c->z += c->speed >> 8;
        if (c->z >= track_len) {
            c->z -= track_len;
            c->lap++;
            if (c->lap >= setup.laps && !c->done && setup.mode != MODE_ATTRACT) {
                c->done = 1;
                c->finish = (s32)P.time;
            }
        }
    }
}

static int player_rank(void) {
    s32 pt = total_of(&cars[0]);
    int r = 1;
    for (int i = 1; i <= nrivals; i++) {
        Car *c = &cars[i];
        if (c->done && (!P.finished || (u32)c->finish < P.time)) r++;
        else if (!c->done && !P.finished && total_of(c) > pt) r++;
    }
    return r;
}

/* ------------------------------------------------------------------ camera + world drawing */
static int bucket_head[DRAW_DIST + 2];
static s8 bucket_next[NCARS_MAX];

static void build_view(void) {
    Car *p = &cars[0];
    s32 camz = wrapz(p->z - CAM_BACK);
    s32 target = p->x * ROAD_W >> 8;
    P.cam_x += (target - P.cam_x) >> 2;
    view.cam_z = camz;
    view.cam_x = P.cam_x;
    view.cam_y = road_y(p->z) + CAM_H;
    if (P.shake) {
        view.cam_y += (s32)(rnd() & 31) - 16;
        view.cam_x += (s32)(rnd() & 31) - 16;
    }
    P.sky_x += (curve_at(p->z) * (p->speed >> 8)) >> 7;
    view.sky_x = P.sky_x;
    int ft = P.nitro_on ? FOCAL - 14 : FOCAL;
    if (P.focal < ft) P.focal++;
    if (P.focal > ft) P.focal--;
    view.focal = P.focal;
    view.horizon = HORIZON_Y;
    view.curve_k = CURVE_K;
    view.curve = trk->curve;
    view.ty = trk->y;
    view.n = trk->n;
    const EnvDef *e = &env_defs[trk->env];
    view.sky = e->sky;
    view.ground = e->ground;
    view.band_row = e->band;
    view.sky_shift0 = 7;
    view.sky_shift1 = 5;
}

static void draw_player_car(void) {
    int x = P.sx - 64;
    int y = P.sy - 31;
    if (!P.spin_t && !P.finished && cars[0].speed > (180 << 8) && ((frame_count >> 1) & 3) == 0) y--;
    if (P.offroad && cars[0].speed > (30 << 8)) y -= (int)(frame_count & 1);
    if (y < -32 || y > 160) return;
    u16 a0 = (u16)((y & 255) | (1 << 14));
    if (!P.flip) {
        oam_add(a0, (u16)((x & 511) | (3 << 14)), T_PLAYER | (1 << 10));
        oam_add(a0, (u16)(((x + 64) & 511) | (3 << 14)), (T_PLAYER + 32) | (1 << 10));
    } else {
        oam_add(a0, (u16)((x & 511) | (3 << 14) | 0x1000), (T_PLAYER + 32) | (1 << 10));
        oam_add(a0, (u16)(((x + 64) & 511) | (3 << 14) | 0x1000), T_PLAYER | (1 << 10));
    }
}

static void draw_flames(void) {
    if (!P.nitro_on || P.spin_t) return;
    int f = (frame_count >> 1) % 3;
    int off = P.anim * 2;
    for (int s = -1; s <= 1; s += 2) {
        int x = P.sx + s * 9 + off - 8;
        int y = P.sy - 9;
        oam_add((u16)(y & 255), (u16)((x & 511) | (1 << 14)), (u16)((FX_FLAME + f * 4) | (1 << 10) | (13 << 12)));
    }
}

static void emit_fx(void) {
    Car *p = &cars[0];
    if (P.drift && (frame_count & 1)) {
        int side = P.drift > 0 ? -1 : 1;
        for (int s = -1; s <= 1; s += 2) {
            int wx = P.sx + s * 18 + P.anim * 2;
            part_spawn(PART_SPARK, wx, P.sy - 3, side * (int)(rnd() % 24) + s * 6, -(int)(rnd() % 30) - 10, 10);
        }
        if ((frame_count & 3) == 0)
            part_spawn(PART_SMOKE, P.sx + side * 14 + P.anim * 3, P.sy - 6, side * 10, 6, 22);
    }
    if (P.offroad && p->speed > (40 << 8) && (frame_count & 3) == 0) {
        int s = (rnd() & 1) ? 1 : -1;
        part_spawn(PART_DUST, P.sx + s * 16, P.sy - 6, s * 8, 10, 20);
    }
    if (P.nitro_on && (frame_count & 3) == 0) {
        int s = (rnd() & 1) ? 1 : -1;
        part_spawn(PART_STREAK, 120 + s * (70 + (int)(rnd() % 40)), 40 + (int)(rnd() % 60), s * 40, 70, 12);
    }
}

static void draw_world(int draw_player) {
    s32 camz = view.cam_z;
    int base = camz >> 8;
    for (int k = 0; k < DRAW_DIST + 2; k++) bucket_head[k] = -1;
    for (int i = 1; i < ncars; i++) {
        s32 d = cars[i].z - (base << 8);
        if (d < 0) d += track_len;
        int k = d >> 8;
        if (k < 1 || k >= DRAW_DIST - 1) continue;
        /* insert sorted by distance (near first) */
        int prev = -1, cur = bucket_head[k];
        while (cur >= 0 && cars[cur].z <= cars[i].z) { prev = cur; cur = bucket_next[cur]; }
        bucket_next[i] = (s8)cur;
        if (prev < 0) bucket_head[k] = i;
        else bucket_next[prev] = (s8)i;
    }
    int kp = ((camz & 255) + CAM_BACK) >> 8;
    for (int k = 1; k < DRAW_DIST - 1; k++) {
        if (k == kp + 1 && draw_player) {
            parts_draw();
            draw_flames();
            draw_player_car();
        }
        const SegScr *a = &segscr[k];
        int seg = seg_wrap(base + k);
        /* roadside objects at the start of this segment */
        if (k >= 2 && a->y < a->clip + 3 && a->y > -40) {
            for (int j = trk->sidx[seg]; j < trk->sidx[seg + 1]; j++) {
                const TrackSprite *sp = &trk->spr[j];
                const ObjDef *od = &odefs[sp->type & 127];
                int x = a->x + ((sp->x * 16 * a->w * 7) >> 15);
                int s = (a->w * od->kscale) >> 12;
                spr_object(od, x, a->y, s, od->pal, sp->type >> 7);
            }
        }
        /* cars on this segment */
        const SegScr *b = &segscr[k + 1];
        for (int i = bucket_head[k]; i >= 0; i = bucket_next[i]) {
            Car *c = &cars[i];
            int f = c->z & 255;
            int y = a->y + (((b->y - a->y) * f) >> 8);
            if (y >= b->clip + 2) continue;
            int x = a->x + (((b->x - a->x) * f) >> 8);
            int w = a->w + (((b->w - a->w) * f) >> 8);
            x += (c->x * w * 7) >> 15;
            int s = (w * K_CAR) >> 12;
            if (c->kind == 1) {
                int yaw = c->yaw != 0;
                spr_car(0, T_RIVAL + (c->body * 2 + yaw) * RIVAL_FRAME_TILES, x, y, s, c->pal, c->yaw < 0);
            } else {
                spr_car(1, T_TRAFFIC + c->body * TRAFFIC_FRAME_TILES, x, y, s, c->pal, 0);
            }
        }
    }
}

/* ------------------------------------------------------------------ HUD */
static const char *const pos_suffix[7] = { "TH", "ST", "ND", "RD", "TH", "TH", "TH" };
static int hud_speed = -1, hud_pos = -1, hud_lap = -1, hud_nitro = -1, hud_tach = -1;
static int cd_loaded = -1;

static void hud_init(void) {
    hud_speed = hud_pos = hud_lap = hud_nitro = hud_tach = -1;
    cd_loaded = -1;
    text_clear();
    if (setup.mode == MODE_ATTRACT) return;
    copy16(CHARBLOCK(2) + UI_MINIMAP * 16, trk->mm_tiles, 25 * 16);
    for (int y = 0; y < 5; y++)
        for (int x = 0; x < 5; x++) text_tile(25 + x, 2 + y, UI_MINIMAP + y * 5 + x, PAL_MAIN);
    text_put(1, 17, "NITRO", PAL_MAIN);
    text_put(26, 19, "KM/H", PAL_MAIN);
    if (setup.mode == MODE_TIME) text_put(12, 0, "TIME ATTACK", PAL_HI);
    text_put(19, 1, "LAP", PAL_DIM);
}

static void hud_update(void) {
    if (setup.mode == MODE_ATTRACT) return;
    Car *p = &cars[0];
    int lap = p->lap + 1;
    if (lap < 1) lap = 1;
    if (lap > setup.laps) lap = setup.laps;
    if (lap != hud_lap) {
        hud_lap = lap;
        if (setup.mode != MODE_TIME) {
            text_put(11, 0, "LAP", PAL_MAIN);
            text_num(15, 0, (u32)lap, 1, PAL_HI, 0);
            text_put(16, 0, "/", PAL_MAIN);
            text_num(17, 0, setup.laps, 1, PAL_MAIN, 0);
        } else {
            text_num(26, 1, (u32)lap, 1, PAL_HI, 0);
            text_put(27, 1, "/", PAL_MAIN);
            text_num(28, 1, setup.laps, 1, PAL_MAIN, 0);
        }
    }
    text_time(22, 0, P.time, PAL_MAIN);
    if (setup.mode != MODE_TIME) text_time(22, 1, P.time - P.lap_start, PAL_DIM);
    else text_time(19, 1, P.time - P.lap_start, PAL_DIM);
    if (setup.mode == MODE_TIME) text_clear_rect(19, 1, 3, 0);
    if (nrivals) {
        int pos = P.finished ? P.pos : player_rank();
        P.pos = pos;
        if (pos != hud_pos) {
            hud_pos = pos;
            text_big_num(1, 0, (u32)pos, 1, PAL_HI);
            text_put(2, 0, pos_suffix[pos], PAL_HI);
            text_put(2, 1, "/", PAL_DIM);
            text_num(3, 1, (u32)(nrivals + 1), 1, PAL_DIM, 0);
        }
    }
    int kmh = (p->speed >> 8) * 9 / 8;
    if (kmh != hud_speed) {
        hud_speed = kmh;
        text_big_num(22, 18, (u32)kmh, 3, P.nitro_on ? PAL_HI : PAL_MAIN);
    }
    int tach = (P.rpm * 8) >> 10;
    if (tach > 8) tach = 8;
    if (tach != hud_tach) {
        hud_tach = tach;
        for (int i = 0; i < 8; i++) {
            int t = i < tach ? (i < 4 ? 1 : i < 6 ? 2 : 3) : 0;
            text_tile(21 + i, 17, UI_BAR + t, PAL_MAIN);
        }
    }
    int nb = (P.nitro * 16) >> 10;
    if (nb > 16) nb = 16;
    int nkey = nb | (P.nitro_on << 5) | ((frame_count >> 3) & 1) << 6;
    if (nkey != hud_nitro) {
        hud_nitro = nkey;
        text_tile(1, 18, UI_FONT + font_map[']'], P.nitro_on ? PAL_HI : PAL_MAIN);
        for (int i = 0; i < 8; i++) {
            int segs = nb - i * 2;
            int t = segs >= 2 ? (P.nitro_on && ((frame_count >> 2) & 1) ? 5 : 4) : segs == 1 ? 6 : 0;
            text_tile(2 + i, 18, UI_BAR + t, PAL_MAIN);
        }
        if (nb == 16 && !P.nitro_on && ((frame_count >> 4) & 1)) text_put(7, 17, " R!", PAL_HI);
        else text_put(7, 17, "   ", PAL_MAIN);
    }
    /* popup + banner */
    if (P.popup_t) {
        P.popup_t--;
        if (P.popup_t == 69) { text_clear_rect(0, 12, 30, 1); text_center(12, P.popup, PAL_HI); }
        if (P.popup_t == 0) text_clear_rect(0, 12, 30, 1);
    }
    if (P.banner_t) {
        P.banner_t--;
        const BannerDef *b = &banner_defs[P.banner];
        int bx = (30 - b->w) >> 1;
        if (P.banner_t == 89 || (P.banner_t > 20 && P.banner_t % 30 == 0)) {
            for (int y = 0; y < b->h; y++)
                for (int x = 0; x < b->w; x++) {
                    u16 e = b->map[y * b->w + x];
                    SCREENBLOCK(30)[(5 + y) * 32 + bx + x] = (e & 0x3FF) ? e : 0;
                }
        }
        if (P.banner_t == 0 && !P.finished) text_clear_rect(0, 5, 30, b->h);
    }
}

static void show_banner(int i) { P.banner = i; P.banner_t = 90; }

static void draw_hud_sprites(int countdown_t) {
    if (setup.mode == MODE_ATTRACT) return;
    /* minimap dots: player last on top -> add first */
    for (int i = 0; i < 1 + nrivals; i++) {
        Car *c = &cars[i];
        int idx = (c->z >> 8) >> 4;
        int mx = 200 + trk->mm_pts[idx * 2] - 4;
        int my = 16 + trk->mm_pts[idx * 2 + 1] - 4;
        int t = i == 0 ? 0 : 1 + ((i - 1) % 6);
        oam_add((u16)(my & 255), (u16)(mx & 511), (u16)((T_HUD + t) | (0 << 10) | (14 << 12)));
    }
    /* countdown digit with zoom */
    if (countdown_t >= 0 && countdown_t < 240) {
        int ph = countdown_t / 60, lt = countdown_t % 60;
        if (ph != cd_loaded) {
            vb_copy(OBJ_VRAM + T_COUNT * 16, cd_tiles[ph], ph == 3 ? 1024 : 512);
            cd_loaded = ph;
        }
        int sc = lt < 10 ? 256 + (10 - lt) * 40 : 256;
        if (lt > 50) sc = 256 + (lt - 50) * 20;
        if (ph == 3 && lt > 40 && (lt & 2)) return;
        mat_set(31, (s16)inv_scale[sc > 1023 ? 1023 : sc] , 0, 0, (s16)inv_scale[sc > 1023 ? 1023 : sc]);
        if (ph < 3)
            oam_add((u16)(((52 - 32) & 255) | 0x300 | (0 << 14)), (u16)(((120 - 32) & 511) | (31 << 9) | (2 << 14)),
                    (u16)(T_COUNT | (0 << 10) | (14 << 12)));
        else
            oam_add((u16)(((52 - 32) & 255) | 0x300 | (1 << 14)), (u16)(((120 - 64) & 511) | (31 << 9) | (3 << 14)),
                    (u16)(T_COUNT | (0 << 10) | (14 << 12)));
    }
}

/* ------------------------------------------------------------------ pause */
static int pause_menu(void) {
    static const char *const items[3] = { "RESUME", "RESTART", "QUIT RACE" };
    int sel = 0;
    engine_set(0, 0, 0);
    skid_set(0);
    text_clear_rect(0, 5, 30, 10);
    panel_draw(8, 5, 14, 9);
    text_center(6, "PAUSED", PAL_HI);
    sfx_play(SFX_SELECT);
    for (;;) {
        for (int i = 0; i < 3; i++) {
            text_put(10, 8 + i * 2, i == sel ? ">" : " ", PAL_HI);
            text_put(11, 8 + i * 2, items[i], i == sel ? PAL_HI : PAL_MAIN);
        }
        frame_end();
        if (KEY_REP(KEY_UP)) { sel = (sel + 2) % 3; sfx_play(SFX_MOVE); }
        if (KEY_REP(KEY_DOWN)) { sel = (sel + 1) % 3; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_START)) { sel = 0; break; }
        if (KEY_HIT(KEY_A)) break;
        if (KEY_HIT(KEY_B)) { sel = 0; break; }
    }
    sfx_play(sel == 0 ? SFX_BACK : SFX_SELECT);
    text_clear_rect(0, 5, 30, 10);
    hud_speed = hud_pos = hud_lap = hud_nitro = hud_tach = -1;
    return sel;
}

/* ------------------------------------------------------------------ race loop */
static void autopilot(int *steer, s32 tx) {
    Car *p = &cars[0];
    int c = curve_ahead(p->z, 6);
    s32 want = tx + (c * 3);
    s32 d = want - p->x;
    *steer = d > 24 ? 1 : d < -24 ? -1 : 0;
    if (P.ai) {
        p->x += clampi((int)(tx - p->x) >> 4, -4, 4);
        *steer = 0;
    }
}

static int stage_frame(int countdown_t, int draw_player) {
    Car *p = &cars[0];
    build_view();
    road_project(&view);
    P.sx = 120 + (((cars[0].x * ROAD_W >> 8) - P.cam_x) * P.focal) / CAM_BACK;
    P.sy = HORIZON_Y + (CAM_H * P.focal) / CAM_BACK;
    oam_begin();
    mats_reset();
    draw_hud_sprites(countdown_t);
    draw_world(draw_player);
    oam_end();
    (void)p;
    return 0;
}

void race_run(const RaceSetup *rs, RaceResult *res) {
    memset(res, 0, sizeof(*res));
restart:
    race_init(rs);
    race_load_env(trk->env, 0);
    parts_clear();
    hud_init();
    player_frame();
    music_play(env_defs[trk->env].song);
    fade = 16;
    fade_apply();
    REG_DISPCNT = 0x1741;
    road_on = 1;
    int t = 0;
    int phase = 0;      /* 0 countdown, 1 racing, 2 finished */
    int quit = 0, do_restart = 0;
    u32 a_first = 0;
    for (;;) {
        Car *p = &cars[0];
        int steer = KEY_HELD(KEY_RIGHT) ? 1 : KEY_HELD(KEY_LEFT) ? -1 : 0;
        int gas = KEY_HELD(KEY_A) != 0;
        int brake = KEY_HELD(KEY_B) != 0;
        int drift = KEY_HELD(KEY_L) != 0;
        int nitro = KEY_HELD(KEY_R) != 0;
        if (phase == 0) {
            if (fade > 0) fade -= 2;
            if (t % 60 == 0 && t < 180) sfx_play(SFX_COUNT);
            if (gas && !a_first) a_first = (u32)t + 1;
            if (!gas) a_first = 0;
            P.rpm = gas ? clampi(P.rpm + 60, 0, 1023) : clampi(P.rpm - 30, 120, 1023);
            engine_set(1, P.rpm, gas);
            if (t == 180) {
                phase = 1;
                sfx_play(SFX_GO);
                if (a_first >= 121 && a_first <= 175) {
                    p->speed = P.top * 4 / 10;
                    P.boost_t = 40;
                    popup("PERFECT START!");
                } else if (a_first && a_first < 121) {
                    popup("WHEELSPIN...");
                    P.shake = 20;
                }
            }
            ai_update();     /* no motion yet: speeds are zero */
            for (int i = 1; i <= nrivals; i++) cars[i].speed = 0;
            for (int i = 1 + nrivals; i < ncars; i++) cars[i].z -= cars[i].speed >> 8;
        } else if (phase == 1) {
            if (KEY_HIT(KEY_START)) {
                int s = pause_menu();
                if (s == 1) { do_restart = 1; break; }
                if (s == 2) { quit = 1; break; }
            }
            if (P.time < 600 * 60) P.time++;
            int lap0 = p->lap;
            if (P.spin_t && P.spin_t < 20) P.offroad = 0;
            player_update(steer, gas, brake, drift, nitro);
            if (P.spin_t == 0 || P.spin_t < 40) collide_scenery();
            collide_cars();
            ai_update();
            if (p->lap != lap0 && p->lap >= 1) {
                u32 lt = P.time - P.lap_start;
                P.lap_start = P.time;
                P.last_lap = lt;
                int newbest = !P.best_lap || lt < P.best_lap;
                if (newbest) P.best_lap = lt;
                if (p->lap >= setup.laps) {
                    phase = 2;
                    P.finished = 1;
                    P.pos = player_rank();
                    show_banner(0);
                    sfx_play(SFX_FINISH);
                    music_play(P.pos <= 3 || setup.mode == MODE_TIME ? SONG_WIN : SONG_LOSE);
                } else {
                    sfx_play(SFX_LAP);
                    if (p->lap == setup.laps - 1) show_banner(1);
                    else if (newbest && p->lap > 1) show_banner(2);
                }
            }
            /* engine pitch: fake gearbox */
            s32 spd = p->speed >> 8;
            static const u16 gear_top[6] = { 70, 120, 165, 210, 255, 400 };
            int g = 0;
            while (g < 5 && spd > gear_top[g]) g++;
            int lo = g ? gear_top[g - 1] : 0;
            int rpm = 200 + (spd - lo) * 820 / (gear_top[g] - lo);
            if (P.drift) rpm += 80;
            P.rpm = clampi(rpm, 120, 1023);
            engine_set(1, P.rpm, gas);
            skid_set(P.drift != 0 || (P.spin_t > 10));
            emit_fx();
        } else {
            /* finished: autopilot cruises on */
            P.finish_t++;
            int st;
            autopilot(&st, 0);
            player_update(st, 1, 0, 0, 0);
            ai_update();
            engine_set(1, 500 + (P.finish_t & 63), 1);
            skid_set(0);
            if (P.finish_t > 200 || (P.finish_t > 50 && KEY_HIT(KEY_A | KEY_START))) break;
        }
        parts_update();
        player_frame();
        stage_frame(phase == 0 || t < 240 ? t : -1, 1);
        if (phase == 2 && P.finish_t > 170) fade = (P.finish_t - 170) / 2;
        fade_apply();
        frame_end();
        hud_update();
        if (P.brake) PAL_OBJ[10] = car_pal_base[11];
        else PAL_OBJ[10] = car_pal_base[10];
        t++;
    }
    engine_set(0, 0, 0);
    skid_set(0);
    if (do_restart) {
        for (int i = 0; i < 16; i++) { fade = i; fade_apply(); frame_end(); }
        goto restart;
    }
    for (int i = fade; i <= 16; i++) { fade = i; fade_apply(); frame_end(); }
    road_on = 0;
    res->quit = (u8)quit;
    res->nracers = (u8)(1 + nrivals);
    res->pos = (u8)(P.pos ? P.pos : 1);
    res->time = P.time;
    res->best_lap = P.best_lap;
    /* final standings: finished by time, others by distance (estimated time) */
    u32 tm[6];
    u8 idx[6];
    int n = 1 + nrivals;
    for (int i = 0; i < n; i++) {
        Car *c = &cars[i];
        idx[i] = (u8)i;
        if (i == 0) tm[i] = P.time;
        else if (c->done) tm[i] = (u32)c->finish;
        else {
            s32 remain = setup.laps * track_len - total_of(c);
            s32 sp = (c->top >> 8) ? (c->top >> 8) : 200;
            tm[i] = P.time + (u32)(remain > 0 ? remain / sp : 0) + 30;
        }
    }
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            if (tm[idx[j]] < tm[idx[i]]) { u8 x = idx[i]; idx[i] = idx[j]; idx[j] = x; }
    for (int i = 0; i < n; i++) {
        res->order[i] = idx[i];
        res->times[i] = tm[idx[i]];
        if (idx[i] == 0 && !quit) res->pos = (u8)(i + 1);
    }
}

/* ------------------------------------------------------------------ attract mode (behind menus) */
static int attract_track = -1;

void attract_init(int track) {
    RaceSetup rs;
    memset(&rs, 0, sizeof(rs));
    rs.track = (u8)track;
    rs.mode = MODE_ATTRACT;
    rs.rivals = 4;
    rs.traffic = 14;
    rs.laps = 99;
    race_init(&rs);
    if (cur_env != trk->env || attract_track != track) race_load_env(trk->env, 1);
    else { REG_DISPCNT = 0x0080; }
    attract_track = track;
    cars[0].z = 64 * 256;
    cars[0].speed = 200 << 8;
    for (int i = 1; i <= nrivals; i++) {
        cars[i].z = (90 + i * 30) * 256;
        cars[i].speed = cars[i].top;
    }
    road_on = 1;
}

void attract_step(int oam_reserved) {
    int st;
    (void)oam_reserved;
    autopilot(&st, 0);
    player_update(0, 1, 0, 0, 0);
    if (cars[0].speed > (205 << 8)) cars[0].speed = 205 << 8;
    ai_update();
    build_view();
    road_project(&view);
    mats_reset();
    draw_world(0);
}
