/* Turbo Drift - boot, title, menus (career, quick race, time attack, garage,
   records, options) and results.  Menus run over a live attract-mode drive. */
#include "game.h"
#include "render.h"

static int win_on;
static int menu_track;         /* track shown behind the menus */
static int slide;              /* BG0 slide-in offset */

/* ------------------------------------------------------------------ helpers */
static void disp_update(void) {
    REG_DISPCNT = (0x1741 | (win_on ? 0x2000 : 0))
#ifdef DBG_MASK
        & ~DBG_MASK
#endif
        ;
}

static void set_win(int x0, int y0, int x1, int y1) {
    REG_WIN0H = (u16)((x0 << 8) | x1);
    REG_WIN0V = (u16)((y0 << 8) | y1);
    win_on = 1;
}

static void fmt_credits(char *b, u32 v) {
    char t[12];
    fmt_num(t, v);
    int l = str_len(t), j = 0;
    b[j++] = '$';
    for (int i = 0; i < l; i++) {
        b[j++] = t[i];
        int rem = l - 1 - i;
        if (rem && rem % 3 == 0) b[j++] = ',';
    }
    b[j] = 0;
}

typedef void (*SprFn)(void);
static SprFn menu_sprites;

static void menu_frame(void) {
    oam_begin();
    if (menu_sprites) menu_sprites();
    attract_step(oam_count());
    oam_end();
    if (slide > 0) {
        slide = slide * 3 / 4;
        if (slide < 2) slide = 0;
    }
    REG_BGHOFS(0) = (u16)(-slide);
    disp_update();
    fade_apply();
    frame_end();
}

static void fade_in(void) {
    while (fade > 0) { fade -= 2; if (fade < 0) fade = 0; menu_frame(); }
}

static void fade_out(void) {
    while (fade < 16) { fade += 2; menu_frame(); }
}

extern int menu_dim;

static void enter_env(int track) {
    fade = 16;
    fade_apply();
    menu_track = track;
    attract_init(track);
    copy16(CHARBLOCK(2) + UI_EXTRA * 16, logo_tiles, sizeof(logo_tiles) / 2);
    text_clear();
}

static void draw_logo(int y) {
    for (int j = 0; j < LOGO_H; j++)
        for (int i = 0; i < LOGO_W; i++) {
            u16 e = logo_map[j * LOGO_W + i];
            SCREENBLOCK(30)[(y + j) * 32 + i] = (e & 0x3FF) ? e : 0;
        }
}

static void draw_credits_line(int y) {
    char b[16];
    fmt_credits(b, save.credits);
    text_clear_rect(18, y, 12, 1);
    text_put(29 - str_len(b), y, b, PAL_HI);
}

/* ------------------------------------------------------------------ title */
static int title_t;
static void title_sprites(void) {}

static void title_screen(void) {
    enter_env(0);
    menu_dim = 0;
    win_on = 0;
    menu_sprites = title_sprites;
    draw_logo(2);
    text_center(17, "MADE FOR RETRA", PAL_DIM);
    music_play(SONG_TITLE);
    title_t = 0;
    for (;;) {
        title_t++;
        if (fade > 0) fade--;
        if ((title_t & 31) < 22) text_center(13, "PRESS START", PAL_HI);
        else text_clear_rect(0, 13, 30, 1);
        menu_frame();
        if (title_t > 20 && KEY_HIT(KEY_START | KEY_A)) break;
    }
    sfx_play(SFX_SELECT);
    for (int i = 0; i < 12; i++) {
        if (i & 2) text_center(13, "PRESS START", PAL_MAIN);
        else text_center(13, "PRESS START", PAL_HI);
        menu_frame();
    }
}

/* ------------------------------------------------------------------ generic list menu */
static int list_menu(const char *title, const char *const *items, const char *const *help, int n, int sel,
                     int x, int y, int w) {
    int h = n * 2 + 3;
    menu_dim = 7;
    text_clear();
    draw_logo(0);
    panel_draw(x, y, w, h);
    text_put(x + 2, y + 1, title, PAL_DIM);
    draw_credits_line(19);
    slide = 120;
    int redraw = 1;
    for (;;) {
        if (redraw) {
            for (int i = 0; i < n; i++) {
                text_put(x + 2, y + 2 + i * 2, i == sel ? "}" : " ", PAL_HI);
                text_put(x + 4, y + 2 + i * 2, items[i], i == sel ? PAL_HI : PAL_MAIN);
            }
            text_clear_rect(0, 18, 30, 1);
            if (help) text_center(18, help[sel], PAL_DIM);
            redraw = 0;
        }
        if (fade > 0) fade -= 2;
        menu_frame();
        if (KEY_REP(KEY_UP)) { sel = (sel + n - 1) % n; sfx_play(SFX_MOVE); redraw = 1; }
        if (KEY_REP(KEY_DOWN)) { sel = (sel + 1) % n; sfx_play(SFX_MOVE); redraw = 1; }
        if (KEY_HIT(KEY_A | KEY_START)) { sfx_play(SFX_SELECT); return sel; }
        if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); return -1; }
    }
}

/* ------------------------------------------------------------------ race launching + results */
static const u16 reward_base[6] = { 3000, 2000, 1400, 900, 600, 400 };
static const char *const ordinal[7] = { "", "1ST", "2ND", "3RD", "4TH", "5TH", "6TH" };
static int res_mood;

static void results_sprites(void) {
    oam_add((u16)((96) & 255), (u16)((188) & 511) | (2 << 14), (u16)(T_MOCHI | (0 << 10) | (5 << 12)));
}

static void show_results(const RaceSetup *rs, const RaceResult *r, u32 reward, int unlocked_new, int champion) {
    enter_env(rs->track);
    menu_dim = 8;
    copy16(PAL_OBJ + 5 * 16, mochi_pal, 16);
    res_mood = (rs->mode == MODE_TIME || r->pos <= 3) ? 1 : 3;
    copy16(OBJ_VRAM + T_MOCHI * 16, mochi_tiles + res_mood * 256, 256);
    menu_sprites = results_sprites;
    set_win(184, 92, 224, 132);
    panel_draw(1, 1, 28, 18);
    const TrackDef *td = &track_defs[rs->track];
    text_put(3, 2, td->name, PAL_HI);
    text_put(3, 3, rs->mode == MODE_CAREER ? "CAREER" : rs->mode == MODE_QUICK ? "QUICK RACE" : "TIME ATTACK", PAL_DIM);
    int y = 5;
    if (rs->mode != MODE_TIME) {
        for (int i = 0; i < r->nracers; i++) {
            int who = r->order[i];
            int pal = who == 0 ? PAL_HI : PAL_MAIN;
            text_put(3, y + i, ordinal[i + 1], pal);
            text_put(7, y + i, who == 0 ? "YOU" : rival_names[who - 1], pal);
            text_time(18, y + i, r->times[i], pal);
        }
        y += r->nracers + 1;
    } else {
        text_put(3, y, "TOTAL", PAL_MAIN);
        text_time(18, y, r->time, PAL_HI);
        y += 2;
    }
    text_put(3, y, "BEST LAP", PAL_MAIN);
    text_time(18, y, r->best_lap, PAL_MAIN);
    if (r->new_lap_record) text_put(3, y + 1, "NEW LAP RECORD!", PAL_HI);
    if (r->new_race_record) text_put(3, y + 1, "NEW TRACK RECORD!", PAL_HI);
    y += 3;
    if (reward) {
        char b[16];
        fmt_credits(b, reward);
        text_put(3, y, "EARNED", PAL_MAIN);
        text_put(11, y, "+", PAL_HI);
        text_put(12, y, b, PAL_HI);
        y++;
    }
    if (champion) text_put(3, y, "CHAMPION! MOCHI GT UNLOCKED", PAL_HI);
    else if (unlocked_new) text_put(3, y, "NEXT EVENT UNLOCKED!", PAL_HI);
    text_put(3, 17, "A: CONTINUE", PAL_DIM);
    music_play(SONG_MENU);
    slide = 160;
    for (int t = 0;; t++) {
        if (fade > 0) fade -= 2;
        if (res_mood == 1 && (t & 15) == 0) {
            copy16(OBJ_VRAM + T_MOCHI * 16, mochi_tiles + ((t >> 4) & 1 ? 0 : 1) * 256, 256);
        }
        menu_frame();
        if (t > 30 && KEY_HIT(KEY_A | KEY_START)) break;
    }
    sfx_play(SFX_SELECT);
    fade_out();
    win_on = 0;
    menu_sprites = 0;
}

static void launch_race(RaceSetup *rs, int event) {
    RaceResult r;
    fade_out();
    menu_sprites = 0;
    win_on = 0;
    menu_dim = 0;
    REG_BGHOFS(0) = 0;
    for (;;) {
        race_run(rs, &r);
        if (!r.restart) break;
    }
    save.total_races++;
    if (r.quit) {
        save_write();
        enter_env(rs->track);
        return;
    }
    u32 reward = 0;
    int unlocked_new = 0, champion = 0;
    int tier = rs->track >> 1;
    if (rs->mode == MODE_CAREER) {
        reward = (u32)reward_base[r.pos - 1] * (u32)(100 + tier * 50) / 100;
        if (!save.career[event] || r.pos < save.career[event]) save.career[event] = r.pos;
        if (r.pos <= 3 && event + 2 > save.unlocked && event + 1 < NUM_EVENTS) {
            save.unlocked = (u8)(event + 2);
            unlocked_new = 1;
        }
        if (r.pos <= 3 && event == NUM_EVENTS - 1 && !save.champion) {
            save.champion = 1;
            save.owned |= 8;
            reward += 5000;
            champion = 1;
        }
        if (r.pos == 1) save.races_won++;
    } else if (rs->mode == MODE_QUICK) {
        reward = (u32)reward_base[r.pos - 1] * (u32)(100 + tier * 50) / 300;
    }
    if (r.best_lap && (!save.best_lap[rs->track] || r.best_lap < save.best_lap[rs->track])) {
        save.best_lap[rs->track] = (u16)(r.best_lap > 65535 ? 65535 : r.best_lap);
        r.new_lap_record = 1;
    }
    if (rs->mode == MODE_TIME && (!save.best_race[rs->track] || r.time < save.best_race[rs->track])) {
        save.best_race[rs->track] = r.time;
        r.new_race_record = 1;
        r.new_lap_record = 0;
        reward = 250;
    }
    save.credits += reward;
    if (save.credits > 9999999) save.credits = 9999999;
    save_write();
    show_results(rs, &r, reward, unlocked_new, champion);
    enter_env(menu_track);
}

static void fill_setup(RaceSetup *rs, int track, int mode) {
    memset(rs, 0, sizeof(*rs));
    rs->track = (u8)track;
    rs->mode = (u8)mode;
    rs->car = save.car;
    rs->paint = save.paint[save.car];
    for (int i = 0; i < 4; i++) rs->upg[i] = save.upg[save.car][i];
    rs->laps = 3;
    rs->skill = (u8)(track >> 1);
    if (mode == MODE_TIME) { rs->rivals = 0; rs->traffic = 0; }
    else { rs->rivals = 5; rs->traffic = (u8)(12 + (track & 1) * 4); }
    if (mode == MODE_CAREER && track < 2) rs->laps = 2;
}

/* ------------------------------------------------------------------ track select */
static void draw_minimap_bg(int track, int x, int y) {
    copy16(CHARBLOCK(2) + MENU_MM_TILE * 16, track_defs[track].mm_tiles, 25 * 16);
    for (int j = 0; j < 5; j++)
        for (int i = 0; i < 5; i++) text_tile(x + i, y + j, MENU_MM_TILE + j * 5 + i, PAL_MAIN);
}

static int track_select(int mode, int sel) {
    menu_dim = 6;
    int redraw = 2;
    for (;;) {
        if (redraw) {
            if (redraw == 2) {
                fade_out();
                enter_env(sel);
                menu_dim = 6;
                slide = 60;
            }
            text_clear();
            panel_draw(2, 2, 26, 14);
            const TrackDef *td = &track_defs[sel];
            const EnvDef *ed = &env_defs[td->env];
            text_put(3, 0, mode == MODE_TIME ? "TIME ATTACK" : "QUICK RACE", PAL_DIM);
            draw_credits_line(0);
            text_put(4, 3, "{", PAL_HI);
            text_put(25, 3, "}", PAL_HI);
            text_center(3, td->name, PAL_HI);
            text_put(4, 5, ed->name, PAL_MAIN);
            text_put(4, 6, ed->time, PAL_DIM);
            text_put(4, 8, "LAPS", PAL_DIM);
            text_num(10, 8, td->laps, 1, PAL_MAIN, 0);
            text_put(4, 9, "LENGTH", PAL_DIM);
            text_num(11, 9, (u32)td->n * 256 / 1000 / 10, 1, PAL_MAIN, 0);
            text_put(13, 9, "KM", PAL_DIM);
            text_put(4, 11, "BEST LAP", PAL_DIM);
            if (save.best_lap[sel]) text_time(4, 12, save.best_lap[sel], PAL_MAIN);
            else text_put(4, 12, "--:--.--", PAL_MAIN);
            text_put(14, 11, "BEST RACE", PAL_DIM);
            if (save.best_race[sel]) text_time(14, 12, save.best_race[sel], PAL_MAIN);
            else text_put(14, 12, "--:--.--", PAL_MAIN);
            draw_minimap_bg(sel, 21, 5);
            if (sel + 1 > save.unlocked) {
                text_center(14, "| LOCKED: WIN CAREER EVENTS", PAL_DIM);
            } else {
                text_center(14, "A: RACE   B: BACK", PAL_MAIN);
            }
            text_center(18, "L/R OR LEFT/RIGHT: TRACK", PAL_DIM);
            redraw = 0;
        }
        if (fade > 0) fade -= 2;
        menu_frame();
        int d = KEY_REP(KEY_RIGHT | KEY_R) ? 1 : KEY_REP(KEY_LEFT | KEY_L) ? -1 : 0;
        if (d) {
            sel = (sel + NUM_TRACKS + d) % NUM_TRACKS;
            sfx_play(SFX_MOVE);
            redraw = 2;
        }
        if (KEY_HIT(KEY_A | KEY_START)) {
            if (sel + 1 > save.unlocked) { sfx_play(SFX_ERROR); continue; }
            sfx_play(SFX_SELECT);
            RaceSetup rs;
            fill_setup(&rs, sel, mode);
            launch_race(&rs, -1);
            redraw = 1;
            music_play(SONG_MENU);
        }
        if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); return sel; }
    }
}

/* ------------------------------------------------------------------ career */
static void career_menu(void) {
    int sel = save.unlocked - 1;
    if (sel >= NUM_EVENTS) sel = NUM_EVENTS - 1;
    menu_dim = 7;
    int redraw = 1;
    static const char *const leagues[4] = { "ROOKIE", "STREET", "PRO", "LEGEND" };
    for (;;) {
        if (redraw) {
            text_clear();
            text_put(1, 0, "CAREER", PAL_HI);
            draw_credits_line(0);
            panel_draw(1, 1, 28, 18);
            for (int i = 0; i < NUM_EVENTS; i++) {
                int y = 2 + i * 2;
                int locked = i + 1 > save.unlocked;
                int pal = locked ? PAL_DIM : i == sel ? PAL_HI : PAL_MAIN;
                if ((i & 1) == 0) text_put(21, y, leagues[i >> 1], PAL_DIM);
                text_put(2, y, i == sel ? "}" : " ", PAL_HI);
                text_num(3, y, (u32)(i + 1), 1, pal, 0);
                text_put(5, y, locked ? "| ???" : track_defs[i].name, pal);
                if (save.career[i]) {
                    text_put(17, y + 1, ordinal[save.career[i]], save.career[i] <= 3 ? PAL_HI : PAL_MAIN);
                    if (save.career[i] == 1) text_put(21, y + 1, "[", PAL_HI);
                }
            }
            u32 rw = (u32)reward_base[0] * (u32)(100 + (sel >> 1) * 50) / 100;
            char b[16];
            fmt_credits(b, rw);
            text_clear_rect(0, 19, 30, 1);
            text_put(1, 19, "1ST PRIZE", PAL_DIM);
            text_put(11, 19, b, PAL_HI);
            text_put(19, 19, sel < 2 ? "2 LAPS" : "3 LAPS", PAL_DIM);
            redraw = 0;
        }
        if (fade > 0) fade -= 2;
        menu_frame();
        if (KEY_REP(KEY_UP)) { sel = (sel + NUM_EVENTS - 1) % NUM_EVENTS; sfx_play(SFX_MOVE); redraw = 1; }
        if (KEY_REP(KEY_DOWN)) { sel = (sel + 1) % NUM_EVENTS; sfx_play(SFX_MOVE); redraw = 1; }
        if (KEY_HIT(KEY_A | KEY_START)) {
            if (sel + 1 > save.unlocked) { sfx_play(SFX_ERROR); continue; }
            sfx_play(SFX_SELECT);
            RaceSetup rs;
            fill_setup(&rs, sel, MODE_CAREER);
            launch_race(&rs, sel);
            if (sel + 1 < save.unlocked && sel < NUM_EVENTS - 1 && save.career[sel] && save.career[sel] <= 3) sel++;
            music_play(SONG_MENU);
            redraw = 1;
            menu_dim = 7;
            fade_in();
        }
        if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); return; }
    }
}

/* ------------------------------------------------------------------ garage */
static int gar_car, gar_loaded = -1;
static void garage_sprites(void) {
    int y = 30;
    oam_add((u16)(y & 255), (u16)((56) & 511) | (3 << 14), (u16)(T_GARAGE | (0 << 10) | (0 << 12)));
    oam_add((u16)(y & 255), (u16)((120) & 511) | (3 << 14), (u16)((T_GARAGE + 64) | (0 << 10) | (0 << 12)));
}

static const u16 upg_cost[3] = { 1200, 2800, 5200 };
static const u8 car_cost_mul[4] = { 10, 13, 16, 18 };

static void garage_bar(int x, int y, int v, int extra) {
    /* v and extra in 0..16 segments */
    for (int i = 0; i < 8; i++) {
        int s = v - i * 2, e = v + extra - i * 2;
        int t = s >= 2 ? 1 : (s == 1 ? 6 : (e >= 1 ? 4 : 0));
        if (s >= 2) t = 1;
        text_tile(x + i, y, UI_BAR + t, PAL_MAIN);
    }
}

static void garage_menu(void) {
    gar_car = save.car;
    gar_loaded = -1;
    int row = 0;
    menu_dim = 8;
    menu_sprites = garage_sprites;
    set_win(56, 30, 184, 94);
    int redraw = 1;
    static const char *const upn[4] = { "ENGINE", "TURBO", "GRIP", "NITRO" };
    for (;;) {
        int owned = (save.owned >> gar_car) & 1;
        if (gar_loaded != gar_car) {
            vb_copy(OBJ_VRAM + T_GARAGE * 16, garage_tiles[gar_car], 4096);
            gar_loaded = gar_car;
            redraw = 1;
        }
        if (redraw) {
            make_car_pal(PAL_OBJ, paint_ramps + save.paint[gar_car] * 4, 1);
            text_clear();
            text_put(1, 0, "GARAGE", PAL_HI);
            draw_credits_line(0);
            panel_draw(6, 3, 18, 9);
            text_put(2, 2, row == 0 ? "{" : " ", PAL_HI);
            text_put(27, 2, row == 0 ? "}" : " ", PAL_HI);
            text_center(2, car_names[gar_car], row == 0 ? PAL_HI : PAL_MAIN);
            const CarStats *cs = &car_stats[gar_car];
            const u8 *u = save.upg[gar_car];
            text_put(1, 12, "SPEED", PAL_DIM);
            garage_bar(7, 12, (cs->top - 220) * 16 / 110, u[0] * 2);
            text_put(16, 12, "ACCEL", PAL_DIM);
            garage_bar(22, 12, (cs->accel - 50) * 16 / 60, u[1] * 2);
            text_put(1, 13, "GRIP", PAL_DIM);
            garage_bar(7, 13, (cs->grip - 70) * 16 / 60, u[2] * 2);
            text_put(16, 13, "NITRO", PAL_DIM);
            garage_bar(22, 13, (cs->nitro - 80) * 16 / 60, u[3] * 2);
            /* rows */
            char b[16];
            int y = 15;
            if (owned) {
                text_put(2, y, row == 0 ? "}" : " ", PAL_HI);
                text_put(3, y, save.car == gar_car ? "SELECTED" : "A: DRIVE THIS CAR", row == 0 ? PAL_HI : PAL_MAIN);
            } else if (gar_car == 3 && !save.champion) {
                text_put(3, y, "| WIN THE CAREER TO UNLOCK", PAL_DIM);
            } else {
                fmt_credits(b, cs->price);
                text_put(2, y, row == 0 ? "}" : " ", PAL_HI);
                text_put(3, y, "A: BUY", row == 0 ? PAL_HI : PAL_MAIN);
                text_put(10, y, b, save.credits >= cs->price ? PAL_HI : PAL_DIM);
            }
            if (owned) {
                text_put(2, y + 1, row == 1 ? "}" : " ", PAL_HI);
                text_put(3, y + 1, "PAINT", row == 1 ? PAL_HI : PAL_MAIN);
                text_put(10, y + 1, "{", PAL_DIM);
                text_put(11, y + 1, paint_names[save.paint[gar_car]], row == 1 ? PAL_HI : PAL_MAIN);
                text_put(19, y + 1, "}", PAL_DIM);
                for (int i = 0; i < 4; i++) {
                    int yy = y + 2 + (i >> 1);
                    int xx = (i & 1) ? 16 : 2;
                    int r = 2 + i;
                    text_put(xx, yy, row == r ? "}" : " ", PAL_HI);
                    text_put(xx + 1, yy, upn[i], row == r ? PAL_HI : PAL_MAIN);
                    int lv = u[i];
                    for (int k = 0; k < 3; k++) text_put(xx + 8 + k, yy, k < lv ? "*" : "-", k < lv ? PAL_HI : PAL_DIM);
                }
                if (row >= 2) {
                    int lv = u[row - 2];
                    text_clear_rect(0, 19, 30, 1);
                    if (lv >= 3) text_center(19, "MAX LEVEL", PAL_DIM);
                    else {
                        fmt_credits(b, (u32)upg_cost[lv] * car_cost_mul[gar_car] / 10);
                        text_put(4, 19, "A: UPGRADE", PAL_MAIN);
                        text_put(16, 19, b, save.credits >= (u32)upg_cost[lv] * car_cost_mul[gar_car] / 10 ? PAL_HI : PAL_DIM);
                    }
                }
            }
            redraw = 0;
        }
        if (fade > 0) fade -= 2;
        menu_frame();
        int maxrow = owned ? 5 : 0;
        if (KEY_REP(KEY_UP)) { row = row > 0 ? row - 1 : maxrow; sfx_play(SFX_MOVE); redraw = 1; }
        if (KEY_REP(KEY_DOWN)) { row = row < maxrow ? row + 1 : 0; sfx_play(SFX_MOVE); redraw = 1; }
        int d = KEY_REP(KEY_RIGHT) ? 1 : KEY_REP(KEY_LEFT) ? -1 : 0;
        if (d && row == 0) { gar_car = (gar_car + NUM_CARS + d) % NUM_CARS; row = 0; sfx_play(SFX_MOVE); }
        if (d && row == 1) {
            save.paint[gar_car] = (u8)((save.paint[gar_car] + NUM_PAINTS + d) % NUM_PAINTS);
            sfx_play(SFX_MOVE);
            redraw = 1;
        }
        if (KEY_HIT(KEY_A)) {
            if (row == 0) {
                if (owned) { save.car = (u8)gar_car; sfx_play(SFX_SELECT); save_write(); }
                else if (!(gar_car == 3 && !save.champion) && save.credits >= car_stats[gar_car].price) {
                    save.credits -= car_stats[gar_car].price;
                    save.owned |= (u8)(1 << gar_car);
                    save.car = (u8)gar_car;
                    sfx_play(SFX_BUY);
                    save_write();
                } else sfx_play(SFX_ERROR);
                redraw = 1;
            } else if (row >= 2) {
                int k = row - 2;
                int lv = save.upg[gar_car][k];
                u32 cost = lv < 3 ? (u32)upg_cost[lv] * car_cost_mul[gar_car] / 10 : 0;
                if (lv < 3 && save.credits >= cost) {
                    save.credits -= cost;
                    save.upg[gar_car][k]++;
                    sfx_play(SFX_BUY);
                    save_write();
                } else sfx_play(SFX_ERROR);
                redraw = 1;
            }
        }
        if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); save_write(); break; }
    }
    menu_sprites = 0;
    win_on = 0;
}

/* ------------------------------------------------------------------ records + options */
static void records_menu(void) {
    menu_dim = 8;
    text_clear();
    text_put(1, 0, "RECORDS", PAL_HI);
    panel_draw(1, 1, 28, 19);
    text_put(3, 2, "TRACK", PAL_DIM);
    text_put(16, 2, "LAP", PAL_DIM);
    text_put(23, 2, "RACE", PAL_DIM);
    for (int i = 0; i < NUM_TRACKS; i++) {
        int y = 4 + i * 2;
        int pal = i + 1 > save.unlocked ? PAL_DIM : PAL_MAIN;
        text_put(2, y, track_defs[i].name, pal);
        if (save.best_lap[i]) text_time(14, y + 1, save.best_lap[i], PAL_HI);
        else text_put(14, y + 1, "-:--.--", PAL_DIM);
        if (save.best_race[i]) text_time(21, y + 1, save.best_race[i], PAL_HI);
        else text_put(21, y + 1, "-:--.--", PAL_DIM);
    }
    slide = 120;
    for (;;) {
        if (fade > 0) fade -= 2;
        menu_frame();
        if (KEY_HIT(KEY_A | KEY_B | KEY_START)) { sfx_play(SFX_BACK); return; }
    }
}

static void options_menu(void) {
    int sel = 0, confirm = 0;
    menu_dim = 8;
    for (;;) {
        text_clear();
        draw_logo(0);
        panel_draw(5, 9, 20, 9);
        text_put(7, 10, "OPTIONS", PAL_DIM);
        text_put(6, 12, sel == 0 ? "}" : " ", PAL_HI);
        text_put(8, 12, save.sound ? "MUSIC     ON" : "MUSIC     OFF", sel == 0 ? PAL_HI : PAL_MAIN);
        text_put(6, 14, sel == 1 ? "}" : " ", PAL_HI);
        text_put(8, 14, confirm ? "SURE? PRESS A" : "ERASE SAVE", sel == 1 ? PAL_HI : PAL_MAIN);
        text_put(6, 16, sel == 2 ? "}" : " ", PAL_HI);
        text_put(8, 16, "BACK", sel == 2 ? PAL_HI : PAL_MAIN);
        text_center(19, "TURBO DRIFT V1.0  MADE FOR RETRA", PAL_DIM);
        for (;;) {
            if (fade > 0) fade -= 2;
            menu_frame();
            if (KEY_REP(KEY_UP)) { sel = (sel + 2) % 3; confirm = 0; sfx_play(SFX_MOVE); break; }
            if (KEY_REP(KEY_DOWN)) { sel = (sel + 1) % 3; confirm = 0; sfx_play(SFX_MOVE); break; }
            if (KEY_HIT(KEY_A)) {
                if (sel == 0) {
                    save.sound ^= 1;
                    sound_on = save.sound;
                    if (sound_on) music_play(SONG_MENU);
                    else music_stop();
                    save_write();
                    sfx_play(SFX_SELECT);
                } else if (sel == 1) {
                    if (!confirm) { confirm = 1; sfx_play(SFX_ERROR); }
                    else { save_reset(); save_write(); confirm = 0; sfx_play(SFX_BUY); }
                } else {
                    sfx_play(SFX_BACK);
                    return;
                }
                break;
            }
            if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); return; }
        }
    }
}

/* ------------------------------------------------------------------ main */
int main(void) {
    video_init();
    audio_init();
    save_load();
    sound_on = save.sound;
    title_screen();
    music_play(SONG_MENU);
    static const char *const items[6] = { "CAREER", "QUICK RACE", "TIME ATTACK", "GARAGE", "RECORDS", "OPTIONS" };
    static const char *const help[6] = {
        "WIN EVENTS, EARN CREDITS", "RACE ANY UNLOCKED TRACK", "BEAT YOUR BEST TIMES",
        "BUY CARS, PAINT, UPGRADE", "YOUR BEST LAPS AND TIMES", "MUSIC AND SAVE DATA" };
    int sel = 0, qtrack = 0, ttrack = 0;
    for (;;) {
        int c = list_menu("MAIN MENU", items, help, 6, sel, 7, 8, 16);
        if (c < 0) { title_screen(); music_play(SONG_MENU); continue; }
        sel = c;
        switch (c) {
        case 0: career_menu(); break;
        case 1: qtrack = track_select(MODE_QUICK, qtrack); break;
        case 2: ttrack = track_select(MODE_TIME, ttrack); break;
        case 3: garage_menu(); break;
        case 4: records_menu(); break;
        case 5: options_menu(); break;
        }
    }
}
