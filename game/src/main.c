/* Mochi Hop - scenes: title, world map, credits. */
#include "game.h"

static void frame(void) {
    vsync();
    oam_commit();
    audio_update();
    poll_keys();
}

/* a bouncy Mochi used by the menus: hops in place with squash & stretch */
typedef struct { int y, vy, sx, sy, vsx, vsy, t, ground; } Hopper;

static void hopper_update(Hopper *h, int auto_hop) {
    h->t++;
    if (h->ground && auto_hop && (h->t % auto_hop) == 0) {
        h->vy = -760; h->ground = 0; h->sx = 205; h->sy = 320;
    }
    if (!h->ground) {
        h->vy += 64;
        h->y += h->vy;
        if (h->y >= 0) { h->y = 0; h->ground = 1; h->sx = 320; h->sy = 190; }
    }
    int tx = 256, ty = 256;
    if (h->ground) { int b = isin(h->t * 3) >> 5; tx -= b; ty += b; }
    h->vsx += (tx - h->sx) >> 2; h->vsx = (h->vsx * 3) >> 2; h->sx += h->vsx;
    h->vsy += (ty - h->sy) >> 2; h->vsy = (h->vsy * 3) >> 2; h->sy += h->vsy;
}

static int hopper_frame(Hopper *h) {
    if (!h->ground) return h->vy < 0 ? 6 : 7;
    return ((h->t % 180) > 172) ? 1 : 0;
}

/* ------------------------------------------------------------------ title */
static void setup_title_bg(int with_logo) {
    REG_DISPCNT = 0x0080;
    load_theme_bg(0);
    load_terrain(0);
    copy16(CHARBLOCK(0), title_tiles, sizeof(title_tiles) / 2);
    copy16(PAL_BG + 2 * 16, logo_pal, 16);
    fill16(SCREENBLOCK(24), 0, 2048);
    for (int y = 0; y < 32; y++) {
        if (!with_logo && y < 10) continue;
        copy16(SCREENBLOCK(24) + y * 32, title_map + y * 32, 32);
    }
    REG_BGCNT(0) = 0x1E08;
    REG_BGCNT(1) = 0x1801;          /* 32x32 for the title layout */
    REG_BGCNT(2) = 0x5A0A;
    REG_BGCNT(3) = 0x1C07;
    REG_BGHOFS(1) = 0; REG_BGVOFS(1) = 0;
    REG_BGHOFS(0) = 0; REG_BGVOFS(0) = 0;
    REG_BGVOFS(2) = 70;
    REG_BGVOFS(3) = 64;
    text_clear();
}

static void title_scene(void) {
    setup_title_bg(1);
    fade = 16;
    fade_apply();
    REG_DISPCNT = 0x1F40;
    music_play(SONG_TITLE);
    Hopper m = { 0, 0, 256, 256, 0, 0, 0, 1 };
    int leaving = 0, t = 0;
    for (;;) {
        frame();
        t++;
        if (leaving) { if (++fade >= 16) { fade_apply(); break; } }
        else if (fade > 0) fade--;
        fade_apply();
        REG_BGHOFS(3) = t >> 2;
        REG_BGHOFS(2) = t >> 1;
        if (!leaving && t > 20 && KEY_HIT(KEY_START | KEY_A)) {
            leaving = 1;
            sfx_play(SFX_SELECT);
            m.vy = -900; m.ground = 0; m.sx = 190; m.sy = 340;
        }
        if ((t & 31) < 22 || leaving) text_center(12, "PRESS START", 0);
        else text_clear_rows(12, 12);
        text_center(18, "MADE FOR RETRA", 0);
        hopper_update(&m, 70);
        oam_begin();
        /* twinkling stars around the logo */
        static const u8 tw[5][2] = { { 14, 22 }, { 220, 14 }, { 200, 76 }, { 30, 80 }, { 120, 10 } };
        for (int i = 0; i < 5; i++) {
            int ph = (t + i * 23) & 63;
            if (ph < 24) spr8(tw[i][0], tw[i][1], SPR_FX + 3 + ((ph >> 3) & 1), 7);
        }
        spr_mochi(84, 128 + (m.y >> 8), hopper_frame(&m), m.sx, m.sy, 0, 0);
        int b = (t >> 4) & 1;
        spr16(140, 112 - b, SPR_SPROUT + b * 4, 4, 1);
        spr16(160, 112 - (b ^ 1), SPR_SPROUT + (b ^ 1) * 4, 4, 1);
        spr16(196, 104 + (isin(t * 4) >> 7), SPR_STAR + ((t >> 3) & 3) * 4, 1, 0);
        spr16(40, 96 + (isin(t * 4 + 80) >> 7), SPR_STAR + (((t >> 3) + 2) & 3) * 4, 1, 0);
    }
    text_clear();
}

/* ------------------------------------------------------------------ world map */
static const u8 node_pos[NUM_LEVELS][2] = {
    { 30, 92 }, { 66, 72 }, { 104, 96 }, { 138, 74 }, { 176, 98 }, { 212, 76 },
};

static void map_info(int cur) {
    const LevelDef *d = &level_defs[cur];
    text_clear();
    /* header */
    char w[8] = "WORLD 1";
    w[6] = '1' + d->theme;
    text_center(1, w, 0);
    text_center(2, theme_names[d->theme], 0);
    text_tile(22, 0, UI_ISTAR);
    text_num(23, 0, save_total_stars(), 3, 0);
    text_put(26, 0, "/", 0);
    text_num(27, 0, total_level_stars(), 3, 0);
    /* info panel */
    panel_draw(2, 13, 26, 6);
    for (int y = 14; y < 18; y++) for (int x = 3; x < 27; x++) text_tile(x, y, UI_SOLID);
    char num[4] = { '1' + d->theme, '-', '1' + (cur & 1), 0 };
    text_put(4, 14, num, 1);
    text_put(8, 14, d->name, 1);
    if (cur < save.unlocked) {
        text_tile(4, 16, UI_JSTAR);
        text_num(6, 16, save.best[cur], 2, 1);
        text_put(8, 16, "/", 1);
        text_num(9, 16, d->nstars, 2, 1);
        for (int i = 0; i < 3; i++) text_tile(13 + i, 16, (save.sprouts[cur] & (1 << i)) ? UI_JSPROUT : UI_JSPROUT0);
        text_put(19, 16, "A PLAY", 1);
    } else {
        text_put(4, 16, "LOCKED", 1);
    }
}

static int map_scene(int *cursor) {
    int cur = *cursor;
    if (cur >= save.unlocked) cur = save.unlocked - 1;
    int theme = level_defs[cur].theme;
    REG_DISPCNT = 0x0080;
    load_theme_bg(theme);
    PAL_BG[0] = theme_defs[theme].far_pal[1];
    REG_BGCNT(0) = 0x1E08;
    REG_BGCNT(2) = 0x5A0A;
    REG_BGCNT(3) = 0x1C07;
    REG_BGHOFS(0) = 0; REG_BGVOFS(0) = 0;
    REG_BGVOFS(2) = 72;
    REG_BGVOFS(3) = 64;
    map_info(cur);
    fade = 16;
    fade_apply();
    REG_DISPCNT = 0x1D40;           /* BG0, BG2, BG3, OBJ */
    music_play(SONG_TITLE);
    Hopper m = { 0, 0, 256, 256, 0, 0, 0, 1 };
    int from = cur, move_t = 0, leaving = 0, result = 0, t = 0, reload = -1;
    for (;;) {
        frame();
        t++;
        if (leaving) { if (++fade >= 16) { fade_apply(); break; } }
        else if (reload >= 0) {
            fade += 2;
            if (fade >= 16) {
                fade = 16;
                fade_apply();
                theme = reload;
                load_theme_bg(theme);
                PAL_BG[0] = theme_defs[theme].far_pal[1];
                map_info(cur);
                reload = -1;
            }
        } else if (fade > 0) fade--;
        fade_apply();
        REG_BGHOFS(3) = t >> 3;
        REG_BGHOFS(2) = t >> 2;

        if (!leaving && !move_t && reload < 0) {
            int nd = 0;
            if (KEY_HIT(KEY_RIGHT) && cur + 1 < NUM_LEVELS) nd = 1;
            if (KEY_HIT(KEY_LEFT) && cur > 0) nd = -1;
            if (nd > 0 && cur + 1 >= save.unlocked) { nd = 0; if (KEY_HIT(KEY_RIGHT)) sfx_play(SFX_BACK); }
            if (nd) {
                from = cur;
                cur += nd;
                move_t = 1;
                sfx_play(SFX_MOVE);
                m.ground = 1;
            } else if (KEY_HIT(KEY_A | KEY_START)) {
                sfx_play(SFX_SELECT);
                leaving = 1;
                result = 1;
            } else if (KEY_HIT(KEY_B)) {
                sfx_play(SFX_BACK);
                leaving = 1;
                result = -1;
            }
        }
        int mx, my;
        if (move_t) {
            /* hop along the path */
            int f = move_t;
            mx = node_pos[from][0] + ((node_pos[cur][0] - node_pos[from][0]) * f) / 20;
            my = node_pos[from][1] + ((node_pos[cur][1] - node_pos[from][1]) * f) / 20;
            my -= (isin(f * 128 / 20) * 22) >> 8;
            m.sx = 230; m.sy = 290;
            if (++move_t > 20) {
                move_t = 0;
                m.sx = 320; m.sy = 190; m.vsx = m.vsy = 0;
                if (level_defs[cur].theme != theme) reload = level_defs[cur].theme;
                else map_info(cur);
            }
        } else {
            mx = node_pos[cur][0];
            my = node_pos[cur][1];
        }
        if (!move_t) hopper_update(&m, 0);
        else { m.t++; }
        oam_begin();
        int face_left = move_t ? (cur < from) : 0;
        spr_mochi(mx, my - 4 + (m.y >> 8), move_t ? 6 : hopper_frame(&m), m.sx, m.sy, face_left, 0);
        for (int i = 0; i < NUM_LEVELS; i++) {
            int kind = i >= save.unlocked ? 0 : (save.cleared[i] ? (save.best[i] >= level_defs[i].nstars ? 3 : 2) : 1);
            spr16(node_pos[i][0] - 8, node_pos[i][1] - 8, SPR_NODE + kind * 4, 6, 0);
            if (i + 1 < NUM_LEVELS) {
                for (int k = 1; k < 4; k++) {
                    int x = node_pos[i][0] + ((node_pos[i + 1][0] - node_pos[i][0]) * k) / 4;
                    int y = node_pos[i][1] + ((node_pos[i + 1][1] - node_pos[i][1]) * k) / 4;
                    spr8(x - 4, y - 2, SPR_FX + 2, 7);
                }
            }
        }
        /* sprouts rescued in total hop beside the path */
        int sp = 0;
        for (int i = 0; i < NUM_LEVELS; i++) for (int b = 0; b < 3; b++) if (save.sprouts[i] & (1 << b)) sp++;
        for (int i = 0; i < sp && i < 6; i++) {
            int b = ((t >> 4) + i) & 1;
            spr16(16 + i * 18, 118 - b, SPR_SPROUT + b * 4, 4, 0);
        }
    }
    *cursor = cur;
    text_clear();
    return result;
}

/* ------------------------------------------------------------------ credits */
static const char *const credits[] = {
    "MOCHI HOP", "", "MADE FOR RETRA", "", "", "STARRING", "MOCHI", "& THE SPROUTS", "", "",
    "GAME DESIGN", "CODE, ART & MUSIC", "THE RETRA TEAM", "", "",
    "ALL ART & MUSIC", "ORIGINAL", "MADE WITH LOVE", "", "",
    "THANK YOU", "FOR PLAYING!", "", "", "", "", "THE END",
};
#define NCRED ((int)(sizeof(credits) / sizeof(credits[0])))

static void credits_scene(void) {
    setup_title_bg(0);
    /* text only above the island: window 0 */
    REG_WIN0H = (0 << 8) | 240;
    REG_WIN0V = (0 << 8) | 118;
    REG_WININ = 0x003F;
    REG_WINOUT = 0x003E;
    fade = 16;
    fade_apply();
    REG_DISPCNT = 0x1F40 | 0x2000;
    music_play(SONG_NIGHT);
    Hopper m = { 0, 0, 256, 256, 0, 0, 0, 1 };
    int t = 0, scroll = 0, written = 19, leaving = 0;
    int stop = (20 + 2 * (NCRED - 1)) * 8 - 56;
    char tot[12];
    for (;;) {
        frame();
        t++;
        if (leaving) { if (++fade >= 16) { fade_apply(); break; } }
        else if (fade > 0) fade--;
        fade_apply();
        REG_BGHOFS(3) = t >> 2;
        REG_BGHOFS(2) = t >> 1;
        if (scroll < stop && (t & 1)) scroll++;
        REG_BGVOFS(0) = scroll;
        /* write rows just before they scroll into view */
        while (written < (scroll >> 3) + 21) {
            written++;
            int row = written;
            text_clear_rows(row & 31, row & 31);
            int k = row - 20;
            if (k >= 0 && !(k & 1) && (k >> 1) < NCRED) {
                const char *s = credits[k >> 1];
                text_center(row & 31, s, 0);
            }
            if (k == 2 * 11 + 1) {
                /* total stars line */
                int n = save_total_stars();
                tot[0] = 0;
                (void)tot;
                text_tile(10, row & 31, UI_ISTAR);
                text_num(12, row & 31, n, 3, 0);
                text_put(15, row & 31, "/", 0);
                text_num(16, row & 31, total_level_stars(), 3, 0);
            }
        }
        if (scroll >= stop && KEY_HIT(KEY_A | KEY_START) && !leaving) { leaving = 1; sfx_play(SFX_SELECT); }
        if (t > 120 && KEY_HIT(KEY_START) && !leaving) { leaving = 1; sfx_play(SFX_SELECT); }
        hopper_update(&m, 50);
        oam_begin();
        int x = 40 + ((t >> 1) % 200);
        spr_mochi(x, 128 + (m.y >> 8), hopper_frame(&m), m.sx, m.sy, 0, 0);
        for (int i = 0; i < 3; i++) {
            int b = ((t >> 4) + i) & 1;
            int sx = x - 22 - i * 16;
            if (sx < 50) sx += 200;
            spr16(sx - 8, 112 - b, SPR_SPROUT + b * 4, 4, 0);
        }
        if (scroll >= stop) {
            for (int i = 0; i < 4; i++) {
                int ph = (t + i * 16) & 63;
                spr16(40 + i * 48, 70 + (isin(ph * 4) >> 6), SPR_STAR + ((t >> 3) + i & 3) * 4, 1, 0);
            }
        }
    }
    REG_DISPCNT = 0x0080;
    REG_BGVOFS(0) = 0;
    text_clear();
}

/* ------------------------------------------------------------------ main */
int main(void) {
    video_init();
    audio_init();
    save_load();
    load_sprites();
    load_ui();
    poll_keys();
    int cursor = save.unlocked - 1;
    if (cursor >= NUM_LEVELS) cursor = NUM_LEVELS - 1;
    for (;;) {
        title_scene();
        for (;;) {
            int r = map_scene(&cursor);
            if (r < 0) break;
            int res = play_level(cursor);
            if (res == RES_CLEAR) {
                int first_clear = !save.cleared[cursor];
                save.cleared[cursor] = 1;
                if (res_stars > save.best[cursor]) save.best[cursor] = (u8)res_stars;
                save.sprouts[cursor] |= (u8)res_sprouts;
                if (cursor + 1 < NUM_LEVELS && save.unlocked < cursor + 2) save.unlocked = cursor + 2;
                int ending = (cursor == NUM_LEVELS - 1) && (first_clear || !save.seen_ending);
                if (ending) save.seen_ending = 1;
                save_write();
                if (ending) { credits_scene(); break; }
                if (first_clear && cursor + 1 < NUM_LEVELS) cursor++;
            }
        }
    }
    return 0;
}
