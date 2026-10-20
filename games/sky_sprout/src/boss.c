/* Sky Sprout - mid-bosses and bosses: composite sprites, phases, bullet patterns. */
#include "game.h"

Boss boss;
extern int res_boss_bonus;

enum { BS_ENTER, BS_FIGHT, BS_DYING, BS_FLEE };
static const s16 boss_hp[5] = { 1400, 1700, 2000, 2300, 3000 };
static const s16 mid_hp[5] = { 320, 400, 480, 560, 640 };
static const u8 boss_phases[5] = { 3, 3, 3, 4, 4 };
static int pal_state = -1;
static int name_t;

/* ---------------------------------------------------------------- helpers */
static int every(int n) {
    int k = (n * G.ratemul) >> 8;
    if (k < 1) k = 1;
    return boss.pt % k == 0;
}

static void draw_pf(const PartFrame *f, int cx, int cy, int pal, int fl) {
    for (int i = 0; i < f->n; i++) {
        const Piece *p = &f->p[i];
        int w = ss_w[p->ss], h = ss_h[p->ss];
        int dx = p->dx, dy = p->dy;
        if (fl & SPR_HF) dx = -dx - w;
        if (fl & SPR_VF) dy = -dy - h;
        spr(OC_BOSS, cx + dx, cy + dy, p->tile, p->ss, pal, fl);
    }
}

static void draw_part(int part, int frame, int ox, int oy, int fl) {
    int id = boss.id;
    const PartFrame *f = &boss_frames[boss_part_first[id][part] + frame];
    draw_pf(f, (boss.x >> 8) + ox, (boss.y >> 8) + oy, 12 + boss_part_pal[id][part], fl);
}

/* a single 32x32 piece of a part drawn with an affine matrix (hands, gears) */
static void draw_part_aff(int part, int ox, int oy, int aff) {
    const PartFrame *f = &boss_frames[boss_part_first[boss.id][part]];
    const Piece *p = &f->p[0];
    spr_aff(OC_BOSS, (boss.x >> 8) + ox - 16, (boss.y >> 8) + oy - 16, p->tile, S32, 12 + boss_part_pal[boss.id][part], aff, 0);
}

static void set_box(int i, int x0, int y0, int x1, int y1) {
    int bx = boss.x >> 8, by = boss.y >> 8;
    bhit[i][0] = (s16)(bx + x0);
    bhit[i][1] = (s16)(by + y0);
    bhit[i][2] = (s16)(bx + x1);
    bhit[i][3] = (s16)(by + y1);
    if (i + 1 > bhit_n) bhit_n = i + 1;
}

static void move_to(int k) {
    boss.x += (boss.tx - boss.x) >> k;
    boss.y += (boss.ty - boss.y) >> k;
}

#define BX(o) (boss.x + FIX(o))
#define BY(o) (boss.y + FIX(o))

static int aimf(int ox, int oy) { return aim_player(BX(ox), BY(oy)); }

/* ---------------------------------------------------------------- lifecycle */
void boss_start(int stage, int mid) {
    memset(&boss, 0, sizeof(boss));
    boss.active = 1;
    boss.id = stage;
    boss.mid = mid;
    boss.state = BS_ENTER;
    int hp = mid ? mid_hp[stage] : boss_hp[stage];
    if (G.diff == 0) hp = (hp * 4) / 5;
    if (G.diff == 2) hp = (hp * 9) / 8;
    boss.hp = boss.maxhp = hp;
    boss.nphase = mid ? 2 : boss_phases[stage];
    boss.timer = mid ? 2400 : 6000;
    boss.x = FIX(300);
    boss.y = FIX(80);
    boss.tx = FIX(mid ? 178 : 172);
    boss.ty = FIX(80);
    if (!mid && stage == 2) { boss.x = FIX(170); boss.y = FIX(230); }
    boss.bar_shown = 1;
    pal_state = -1;
    name_t = 100;
    if (mid) text_center(2, mid_names[stage], TS_HUD);
}

int boss_alive(void) { return boss.active; }

static void boss_pal_update(void) {
    int want = boss.flash ? 1 : (boss.rage ? 2 : 0);
    if (want == pal_state) return;
    pal_state = want;
    int s = boss.id;
    if (boss.mid) {
        pal_queue(10, want == 1 ? mid_hit_pals + s * 16 : mid_pals + s * 16);
    } else {
        pal_queue(12, want == 1 ? boss_hit_pals + s * 32 : (want == 2 ? boss_rage_pals + s * 16 : boss_pals + s * 32));
        pal_queue(13, want == 1 ? boss_hit_pals + s * 32 + 16 : boss_pals + s * 32 + 16);
    }
}

/* ---------------------------------------------------------------- mid-bosses */
static void mid_ai(void) {
    int s = boss.id, p = boss.phase, t = boss.pt;
    boss.tx = FIX(178);
    boss.ty = FIX(78) + isin(boss.t * 2) * 18;
    switch (s) {
    case 0: /* Thresher */
        if (p == 0) {
            if (every(40)) shoot_fan(BX(-22), BY(4), aimf(-22, 4), 3, 12, 300, BK_RB);
        } else {
            if (every(50)) shoot_ring(BX(-10), BY(0), t, 10 + G.diff * 2, 240, BK_SV);
            if (every(14)) eb_spawn(BX(-22), BY(4), aimf(-22, 4), 330, BK_SM, BEH_LINE, 0, 0);
        }
        break;
    case 1: /* Storm Lantern */
        if (p == 0) {
            if (every(26)) { boss.a0 += 9; shoot_ring(BX(0), BY(0), boss.a0, 8 + G.diff * 2, 230, BK_SB); }
        } else if (every(5)) {
            boss.a0 += 13;
            eb_spawn(BX(0), BY(0), boss.a0, 250, BK_SA, BEH_LINE, 0, 0);
            eb_spawn(BX(0), BY(0), boss.a0 + 128, 250, BK_SA, BEH_LINE, 0, 0);
        }
        break;
    case 2: /* Torpedo Ray */
        boss.ty = FIX(80) + isin(boss.t * 2) * 50;
        if (p == 0) {
            if (every(30)) shoot_fan(BX(-24), BY(0), aimf(-24, 0), 5, 10, 300, BK_RM);
        } else if (every(70)) {
            for (int i = 0; i < 10 + G.diff * 2; i++)
                eb_spawn(BX(0), BY(0), i * 256 / (10 + G.diff * 2), 320, BK_MA, BEH_STOPAIM, 10, 30);
        }
        break;
    case 3: /* Cog Knight */
        if (p == 0) {
            if (every(50)) {
                boss.a1 ^= 1;
                for (int i = 0; i < 10; i++)
                    eb_spawn(BX(-14), BY(0), i * 26 + t, 240, BK_SA, BEH_CURVE, boss.a1 ? 90 : -90, 70);
            }
        } else if (every(40)) {
            int a = aimf(-14, 0);
            for (int i = 0; i < 4; i++) eb_spawn(BX(-14), BY(0), a, 260 + i * 45, BK_MB, BEH_LINE, 0, 0);
        }
        break;
    default: /* Gloom Sentinel */
        if (p == 0) {
            if (every(30)) {
                eb_spawn(BX(-12), BY(-18), aimf(-12, -18), 320, BK_MM, BEH_LINE, 0, 0);
                eb_spawn(BX(-12), BY(18), aimf(-12, 18), 320, BK_MM, BEH_LINE, 0, 0);
            }
        } else if (every(4)) {
            boss.a0 += 7;
            for (int k = 0; k < 3; k++) eb_spawn(BX(-2), BY(0), boss.a0 + k * 85, 240, BK_SV, BEH_LINE, 0, 0);
        }
        break;
    }
}

static void mid_draw(void) {
    int s = boss.id;
    const PartFrame *f = &mid_frames[mid_first[s] + ((boss.t >> 3) & 1)];
    draw_pf(f, boss.x >> 8, boss.y >> 8, 10, 0);
}

/* ---------------------------------------------------------------- boss 1: Baron Buzzsaw */
static void b1_ai(void) {
    int p = boss.phase, t = boss.pt;
    if (p < 2) {
        boss.tx = FIX(174);
        boss.ty = FIX(80) + isin(boss.t * 2) * 22;
    }
    if (p == 0) {
        if (every(46)) shoot_fan(BX(-42), BY(2), aimf(-42, 2), 5, 10, 310, BK_RM);
        if (t > 60 && every(150)) {
            enemy_spawn(ET_GNAT, AI_SINE, (boss.x >> 8) + 30, (boss.y >> 8) - 30, -330, 4, 18, F_NONE);
            enemy_spawn(ET_GNAT, AI_SINE, (boss.x >> 8) + 30, (boss.y >> 8) + 34, -330, 4, -18, F_NONE);
        }
    } else if (p == 1) {
        if (every(4)) {
            boss.a0 += 7;
            eb_spawn(BX(-8), BY(0), boss.a0, 250, BK_SA, BEH_LINE, 0, 0);
            eb_spawn(BX(-8), BY(0), boss.a0 + 128, 250, BK_SA, BEH_LINE, 0, 0);
        }
        if (every(72)) eb_spawn(BX(-42), BY(2), aimf(-42, 2), 160, BK_BM, BEH_ACCEL, 6, 520);
    } else {
        int c = t % 220;
        if (c < 40) {
            boss.tx = FIX(176);
            boss.ty = FIX(80);
            if (c == 39) boss.ey = pl.y;
            if (c > 20) shake_x = 1;
        } else if (c < 80) {
            boss.tx = FIX(70);
            boss.ty = boss.ey;
        } else if (c < 150) {
            boss.tx = FIX(176);
            boss.ty = FIX(80);
            if (c == 82) shoot_ring(BX(-46), BY(24), t, 16, 230, BK_SM);
        }
        if (every(30)) { boss.a1 += 13; shoot_ring(BX(-8), BY(0), boss.a1, 10, 210, BK_MM); }
    }
    move_to(p == 2 ? 3 : 5);
    set_box(0, -60, -20, 10, 28);
    set_box(1, 0, -26, 60, 30);
}

static void b1_draw(void) {
    int spin = boss.phase == 2 ? boss.t % 3 : (boss.t >> 1) % 3;
    draw_part(2, spin, -46, 24, 0);
    draw_part(0, 0, 0, 0, 0);
    draw_part(1, (boss.t >> 1) & 1, 2, -30, 0);
}

/* ---------------------------------------------------------------- boss 2: Thunderhull */
static void b2_ai(void) {
    int p = boss.phase, t = boss.pt;
    boss.tx = FIX(170);
    boss.ty = FIX(66) + isin(boss.t) * 14;
    static const s8 tx[3] = { -28, 6, 34 };
    if (p == 0) {
        if (every(7)) {
            int k = rnd() % 3;
            eb_spawn(BX(tx[k]), BY(24), 140 + (rnd() & 31), 400, BK_SB, BEH_GRAV, 7, 640);
        }
        if (every(70)) shoot_fan(BX(-62), BY(7), aimf(-62, 7), 3, 14, 320, BK_RB);
    } else if (p == 1) {
        int c = t % 80;
        int coil = (t / 80) & 1;
        int cx = coil ? 28 : -30;
        if (c == 0) boss.a1 = aimf(cx, -34);
        if (c < 24 && (c % 3) == 0) eb_spawn(BX(cx), BY(-34), boss.a1, 400, BK_MB, BEH_LINE, 0, 0);
        if (every(50)) shoot_ring(BX(-4), BY(4), t * 3, 12, 230, BK_SV);
    } else {
        if (every(3)) {
            boss.a0 += 11;
            for (int k = 0; k < 3; k++) eb_spawn(BX(-4), BY(4), boss.a0 + k * 85, 230, BK_SA, BEH_LINE, 0, 0);
        }
        if (every(13)) {
            int k = rnd() % 3;
            eb_spawn(BX(tx[k]), BY(24), 140 + (rnd() & 31), 380, BK_SB, BEH_GRAV, 7, 640);
        }
    }
    move_to(5);
    set_box(0, -60, -14, 60, 20);
    set_box(1, -18, -32, 18, -12);
}

static void b2_draw(void) {
    int sf = (boss.t >> 2) % 3;
    if (boss.phase == 1 && (boss.pt % 80) < 24) {
        int coil = (boss.pt / 80) & 1;
        draw_part(2, sf, coil ? 28 : -30, -34, 0);
    } else if (boss.t & 16) {
        draw_part(2, sf, -30, -34, 0);
    } else {
        draw_part(2, sf, 28, -34, 0);
    }
    draw_part(0, 0, 0, 0, 0);
    draw_part(1, (boss.t >> 4) & 1, 0, 28, 0);
}

/* ---------------------------------------------------------------- boss 3: Admiral Clamp */
static void b3_ai(void) {
    int p = boss.phase, t = boss.pt;
    boss.tx = FIX(170);
    boss.ty = FIX(80) + isin(boss.t) * 12;
    if (p == 2) boss.ty = FIX(80) + isin(boss.t * 3) * 42;
    if (p == 0) {
        if (every(60)) {
            eb_spawn(BX(-78), BY(-14), aimf(-78, -14), 110, BK_BB, BEH_ACCEL, 5, 560);
            eb_spawn(BX(-78), BY(28), aimf(-78, 28), 110, BK_BB, BEH_ACCEL, 5, 560);
        }
        if (every(36)) shoot_fan(BX(-78), BY(-14), aimf(-78, -14), 3, 12, 320, BK_RM);
    } else if (p == 1) {
        if (t % 100 == 50) {
            shoot_fan(BX(-78), BY(-14), aimf(-78, -14), 7, 9, 300, BK_RM);
            shoot_fan(BX(-78), BY(28), aimf(-78, 28), 7, 9, 300, BK_RM);
            sfx_play(SFX_POP);
        }
        if (every(110)) {
            int n = 12 + G.diff * 2;
            for (int i = 0; i < n; i++) eb_spawn(BX(4), BY(-4), i * 256 / n + t, 300, BK_MA, BEH_STOPAIM, 8, 30);
        }
    } else {
        if (every(6)) eb_spawn(BX(10), BY(-28), 192 - 26 + (rnd() % 52), 430, BK_SB, BEH_GRAV, 9, 700);
        if (every(45)) shoot_fan(BX(-78), BY(-14), aimf(-78, -14), 5, 10, 320, BK_RM);
    }
    move_to(p == 2 ? 4 : 5);
    set_box(0, -40, -26, 48, 24);
    set_box(1, -80, -24, -36, 40);
}

static void b3_draw(void) {
    int open = boss.phase == 1 ? ((boss.pt % 100) < 50 ? 0 : 1) : ((boss.t >> 5) & 1);
    draw_part(1, open, -54, -8, 0);
    draw_part(1, open ^ 1, -54, 24, SPR_VF);
    draw_part(0, 0, 0, 0, 0);
}

/* ---------------------------------------------------------------- boss 4: Ticktock Titan */
static void b4_ai(void) {
    int p = boss.phase, t = boss.pt;
    boss.tx = FIX(170);
    boss.ty = FIX(80) + isin(boss.t * 2) * 10;
    int spin = p == 3 ? 5 : (p == 2 ? 3 : 2);
    boss.a0 += spin;                      /* minute hand */
    if ((boss.t & 3) == 0) boss.a1 += p == 3 ? 3 : 1;   /* hour hand */
    boss.a2 += p >= 1 ? 3 : 1;            /* gears */
    if (p == 0) {
        if ((t % (G.diff == 0 ? 5 : 3)) == 0) eb_spawn(BX(0), BY(22), boss.a0, 260, BK_SV, BEH_LINE, 0, 0);
        if (every(60)) shoot_fan(BX(-14), BY(-12), aimf(-14, -12), 3, 10, 320, BK_RM);
    } else if (p == 1) {
        if (every(45)) {
            boss.a3 ^= 1;
            int n = 12 + G.diff * 2;
            for (int i = 0; i < n; i++)
                eb_spawn(BX(0), BY(22), i * 256 / n + t, 240, BK_SA, BEH_CURVE, boss.a3 ? 70 : -70, 80);
        }
    } else if (p == 2) {
        if (every(90)) {
            int n = 14 + G.diff * 2;
            for (int i = 0; i < n; i++) eb_spawn(BX(0), BY(22), i * 256 / n, 330, BK_MB, BEH_STOPAIM, 10, 40);
        }
        if (every(8)) eb_spawn(BX(0), BY(22), boss.a1, 240, BK_SA, BEH_LINE, 0, 0);
    } else {
        if ((t % 3) == 0) {
            eb_spawn(BX(0), BY(22), boss.a0, 250, BK_SV, BEH_LINE, 0, 0);
            eb_spawn(BX(0), BY(22), boss.a1 * 2 + 128, 250, BK_SM, BEH_LINE, 0, 0);
        }
        if (every(64)) {
            boss.a3 ^= 1;
            for (int i = 0; i < 10; i++)
                eb_spawn(BX(0), BY(22), i * 26 + t, 230, BK_SA, BEH_CURVE, boss.a3 ? 80 : -80, 70);
        }
    }
    move_to(5);
    set_box(0, -34, -36, 34, 44);
}

static void b4_draw(void) {
    oam_rotscale(0, (boss.a1 - 192) & 255, 256);
    oam_rotscale(1, (boss.a0 - 192) & 255, 256);
    oam_rotscale(2, boss.a2 & 255, 256);
    oam_rotscale(3, (-boss.a2) & 255, 256);
    draw_part_aff(2, 0, 22, 1);
    draw_part_aff(1, 0, 22, 0);
    draw_part(0, 0, 0, 0, 0);
    draw_part_aff(3, -48, -6, 2);
    draw_part_aff(3, 46, 12, 3);
}

/* ---------------------------------------------------------------- boss 5: King Gloom */
static void b5_ai(void) {
    int p = boss.phase, t = boss.pt;
    boss.tx = FIX(168);
    boss.ty = FIX(80) + isin(boss.t) * 20;
    if (p == 3) { boss.ty = FIX(80) + isin(boss.t * 2) * 30; boss.tx = FIX(172) + isin(boss.t * 3) * 10; }
    boss.a2 += p == 1 ? 3 : 2;
    int rad = p == 1 ? 66 : 56;
    if (p <= 1) {
        if (every(p == 0 ? 44 : 34)) {
            for (int k = 0; k < 4; k++) {
                int a = boss.a2 + k * 64;
                int ox = 10 + ((icos(a) * rad) >> 8), oy = (isin(a) * rad) >> 8;
                eb_spawn(BX(ox), BY(oy), aimf(ox, oy), 280, BK_SV, BEH_LINE, 0, 0);
            }
        }
    }
    if (p == 0) {
        if (every(70)) shoot_fan(BX(-12), BY(0), aimf(-12, 0), 7, 9, 300, BK_RM);
    } else if (p == 1) {
        if (every(90)) eb_spawn(BX(-12), BY(0), aimf(-12, 0), 200, BK_BM, BEH_SPLIT, 12 + G.diff * 2, 50);
        if (every(30)) {
            eb_spawn(BX(-50), BY(-25), aimf(-50, -25), 330, BK_MA, BEH_LINE, 0, 0);
            eb_spawn(BX(-50), BY(28), aimf(-50, 28), 330, BK_MA, BEH_LINE, 0, 0);
        }
    } else if (p == 2) {
        if (every(24)) { boss.a1 ^= 8; shoot_ring(BX(10), BY(0), boss.a1 + t, 14, 220, BK_SM); }
        if (every(50)) {
            boss.a3 ^= 1;
            for (int i = 0; i < 10; i++)
                eb_spawn(BX(10), BY(0), i * 26, 240, BK_SV, BEH_CURVE, boss.a3 ? 70 : -70, 80);
        }
    } else {
        if ((t % 3) == 0) {
            boss.a0 += 9;
            for (int k = 0; k < 4; k++) eb_spawn(BX(10), BY(0), boss.a0 + k * 64, 220, BK_SA, BEH_LINE, 0, 0);
        }
        if (every(40)) shoot_fan(BX(-12), BY(0), aimf(-12, 0), 5, 12, 320, BK_RM);
        if ((boss.t & 63) == 0) shake_y = 2;
    }
    move_to(p == 3 ? 4 : 5);
    set_box(0, -30, -40, 48, 44);
}

static void b5_draw(void) {
    int p = boss.phase;
    int ex = 10, ey = 0;
    int a = aimf(10, 0);
    int ix = (icos(a) * 7) >> 8, iy = (isin(a) * 6) >> 8;
    draw_part(1, 0, ex + ix, ey + iy, 0);
    if (p <= 1) {
        int rad = p == 1 ? 66 : 56;
        for (int k = 0; k < 4; k++) {
            int aa = boss.a2 + k * 64;
            draw_part(2, (boss.t >> 3) & 1, 10 + ((icos(aa) * rad) >> 8), (isin(aa) * rad) >> 8, 0);
        }
    }
    if (p >= 2) draw_part(3, 0, 0, 0, 0);
    draw_part(0, 0, 0, 0, 0);
}

/* ---------------------------------------------------------------- dispatch */
static void boss_ai(void) {
    if (boss.mid) { mid_ai(); move_to(5); set_box(0, -30, -18, 30, 18); return; }
    switch (boss.id) {
    case 0: b1_ai(); break;
    case 1: b2_ai(); break;
    case 2: b3_ai(); break;
    case 3: b4_ai(); break;
    default: b5_ai(); break;
    }
}

static void boss_hitboxes_idle(void) {
    int sv = boss_hittable;
    boss_hittable = 0;
    (void)sv;
}

void boss_update(void) {
    if (name_t && --name_t == 0) text_clear_rows(2, 2);
    if (!boss.active) { boss_hittable = 0; bhit_n = 0; return; }
    boss.t++;
    if (boss.flash) boss.flash--;
    bhit_n = 0;
    switch (boss.state) {
    case BS_ENTER:
        boss_hitboxes_idle();
        move_to(4);
        if (iabs((boss.x - boss.tx) >> 8) < 3 && iabs((boss.y - boss.ty) >> 8) < 3) {
            boss.state = BS_FIGHT;
            boss.pt = 0;
        }
        break;
    case BS_FIGHT:
        boss.pt++;
        if (boss_dmg) {
            if (!boss.invul) {
                boss.hp -= boss_dmg;
                if ((boss.t % 12) == 0) boss.flash = 1;   /* brief blink, not a constant wash-out */
                if ((boss.t & 7) == 0) sfx_play(SFX_HIT);
            }
            boss_dmg = 0;
        }
        if (boss.invul) boss.invul--;
        if (boss.timer && --boss.timer == 0) {
            boss.state = BS_FLEE;
            cancel_bullets();
            boss_hittable = 0;
            break;
        }
        if (boss.hp <= 0) {
            boss.hp = 0;
            boss.state = BS_DYING;
            boss.dying = 0;
            cancel_bullets();
            boss_hittable = 0;
            sfx_play(SFX_BOOM);
            break;
        }
        {
            int next = boss.phase + 1;
            if (next < boss.nphase && boss.hp * boss.nphase <= boss.maxhp * (boss.nphase - next)) {
                boss.phase = next;
                boss.pt = 0;
                boss.invul = 50;
                cancel_bullets();
                sfx_play(SFX_PHASE);
                flash = 6;
                shake_x = 3;
                if (!boss.mid && next == boss.nphase - 1) boss.rage = 1;
            }
        }
        boss_ai();
        boss_hittable = 1;
        break;
    case BS_DYING: {
        int len = boss.mid ? 70 : (boss.id == 4 ? 220 : 150);
        boss.dying++;
        boss_hittable = 0;
        boss.flash = (boss.dying & 4) ? 1 : 0;
        if ((boss.dying & 7) == 0) {
            int w = boss.mid ? 24 : 48, h = boss.mid ? 16 : 36;
            explode(boss.x + FIX((int)(rnd() % (2 * w)) - w), boss.y + FIX((int)(rnd() % (2 * h)) - h), (boss.dying & 15) == 0);
            shake_x = boss.mid ? 1 : 2;
        }
        if (boss.dying >= len) {
            for (int i = 0; i < (boss.mid ? 2 : 5); i++)
                explode(boss.x + FIX((int)(rnd() & 63) - 32), boss.y + FIX((int)(rnd() & 47) - 24), 1);
            flash = 16;
            shake_x = 4;
            shake_y = 2;
            u32 bonus;
            if (boss.mid) bonus = 10000 + 2000 * boss.id;
            else {
                bonus = 30000 + 15000 * boss.id + (u32)(boss.timer / 60) * 100;
                res_boss_bonus = (int)bonus;
            }
            add_score(bonus);
            kill_chain();
            boss.active = 0;
            boss.bar_shown = 0;
            pal_state = -1;
        }
        break;
    }
    case BS_FLEE:
        boss_hittable = 0;
        boss.x += FIX(2);
        boss.y -= FIX(1) >> 1;
        if ((boss.x >> 8) > 320) { boss.active = 0; boss.bar_shown = 0; }
        break;
    }
    boss_pal_update();
}

void boss_draw(void) {
    if (!boss.active) return;
    if (boss.state == BS_DYING && (boss.dying & 2)) return;
    if (boss.mid) { mid_draw(); return; }
    switch (boss.id) {
    case 0: b1_draw(); break;
    case 1: b2_draw(); break;
    case 2: b3_draw(); break;
    case 3: b4_draw(); break;
    default: b5_draw(); break;
    }
}
