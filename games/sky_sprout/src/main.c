/* Sky Sprout - scenes: title, menus, records, name entry, ending and credits. */
#include "game.h"

extern int near_hdma;

static void scene_reset(void) {
    set_display(0x0080);
    frame_wait();
    hdma_stop();
    REG_DISPCNT = 0x0080;
    text_clear();
    oam_clear();
    shake_x = shake_y = 0;
    flash = dim = 0;
    bg_shift_y = 0;
}

/* Mochi's ship drawn by the menus (no gameplay state) */
static void menu_ship(int x, int y, int t, int opts) {
    spr(OC_PLAYER, x, y - 1, SPR_PROP + ((t >> 1) % 3) * 4, W32x8, 0, 0);
    spr(OC_PLAYER, x, y, SPR_PLAYER + (t % 240 > 228 ? 4 * 16 : 0), S32, 0, 0);
    for (int k = 0; k < opts; k++) {
        int bob = isin(t * 4 + k * 128) >> 6;
        spr(OC_PLAYER, x - 14 - k * 16, y + 10 + bob, SPR_SPROUT + ((t >> 3) & 1) * 4, S16, 0, 0);
    }
    if ((t & 7) == 0) { /* exhaust puffs are drawn by the caller-less tiny trail */ }
}

static void title_setup(void) {
    scene_reset();
    load_title_bg();
    near_hdma = 0;
    REG_BGCNT(1) = 0x1F09;          /* logo: prio 1, CBB2, SBB31 */
    REG_BGHOFS(1) = 0;
    REG_BGVOFS(1) = 0;
    set_display(0x1F40);
    music_play(SONG_TITLE);
}

static void title_footer(void) {
    text_put(1, 18, "HI", TS_HUD);
    int d = save.last_diff;
    text_num(4, 18, save.score[d][0], 8, TS_GOLD, 1);
    static const char *const dn[3] = { "EASY", "NORMAL", "HARD" };
    text_put(13, 18, dn[d], TS_HUD);
    text_put(3, 19, "A RETRA ORIGINAL  2026", TS_HUD);
}

/* returns 0 start, 1 practice, 2 records */
static int title_scene(void) {
    title_setup();
    title_footer();
    int t = 0, menu = 0, sel = 0;
    int nopt = max_reached() > 1 ? 3 : 2;
    static const char *const items3[3] = { "START GAME", "PRACTICE", "RECORDS" };
    static const char *const items2[2] = { "START GAME", "RECORDS" };
    fade = 16;
    for (;;) {
        t++;
        oam_clear();
        if (fade > 0 && (t & 1)) fade--;
        int bob = isin(t * 2) >> 6;
        REG_BGVOFS(1) = (u16)(-bob);
        menu_ship(24 + (isin(t) >> 5), 92 + (isin(t * 3) >> 6), t, 2);
        if (!menu) {
            if ((t & 31) == 0) text_center(13, "PRESS START", TS_HUD);
            if ((t & 31) == 20) text_clear_rows(13, 13);
            if (KEY_HIT(KEY_START | KEY_A)) {
                sfx_play(SFX_SELECT);
                menu = 1;
                text_clear_rows(13, 13);
            }
        } else {
            for (int i = 0; i < nopt; i++) {
                const char *s = nopt == 3 ? items3[i] : items2[i];
                text_put(11, 12 + i * 2, s, TS_HUD);
                text_tile(9, 12 + i * 2, i == sel ? UI_CURSOR : 0);
            }
            if (KEY_REPEAT(KEY_DOWN)) { sel = (sel + 1) % nopt; sfx_play(SFX_MOVE); }
            if (KEY_REPEAT(KEY_UP)) { sel = (sel + nopt - 1) % nopt; sfx_play(SFX_MOVE); }
            if (KEY_HIT(KEY_B)) { menu = 0; sfx_play(SFX_BACK); text_clear_rows(11, 17); }
            if (KEY_HIT(KEY_A | KEY_START)) {
                sfx_play(SFX_SELECT);
                int r = nopt == 3 ? sel : (sel == 0 ? 0 : 2);
                return r;
            }
        }
        scroll_dist = t;
        bg_update();
        frame_wait();
    }
}

/* panel with three choices + descriptions; returns -1 on back */
static int difficulty_select(int prompt_practice, int *stage_out) {
    static const char *const dn[3] = { "EASY", "NORMAL", "HARD" };
    static const char *const d1[3] = { "4 LIVES  GENTLE BULLETS", "3 LIVES  THE FULL FLIGHT", "3 LIVES  FAST AND DENSE" };
    static const char *const d2[3] = { "FOR NEW PILOTS", "AS INTENDED", "REVENGE SHOTS!" };
    int sel = save.last_diff, stage = 0, row = 0;
    int maxst = max_reached();
    text_clear_rows(11, 19);
    panel_draw(2, 10, 26, prompt_practice ? 10 : 9);
    for (;;) {
        text_put(4, 11, prompt_practice ? "PRACTICE  SELECT" : "SELECT DIFFICULTY", TS_PANEL);
        for (int i = 0; i < 3; i++) {
            text_put(6 + i * 7, 13, dn[i], TS_PANEL);
            if (i < 2) text_put(6 + i * 7 + str_len(dn[i]), 13, "  ", TS_PANEL);
            text_tile(5 + i * 7, 13, (i == sel && row == 0) ? UI_CURSOR : UI_PAN);
        }
        text_put(3, 15, "                        ", TS_PANEL);
        text_put(3, 16, "                        ", TS_PANEL);
        text_put(4, 15, d1[sel], TS_PANEL);
        text_put(4, 16, d2[sel], TS_PANEL);
        if (prompt_practice) {
            text_put(4, 18, "STAGE", TS_PANEL);
            text_tile(10, 18, row == 1 ? UI_CURSOR : UI_PAN);
            text_num(11, 18, (u32)(stage + 1), 1, TS_PANEL, 0);
            text_put(13, 18, "<>", TS_PANEL);
        }
        frame_wait();
        oam_clear();
        scroll_dist++;
        bg_update();
        if (KEY_REPEAT(KEY_RIGHT)) {
            if (row == 0) sel = (sel + 1) % 3; else stage = (stage + 1) % maxst;
            sfx_play(SFX_MOVE);
        }
        if (KEY_REPEAT(KEY_LEFT)) {
            if (row == 0) sel = (sel + 2) % 3; else stage = (stage + maxst - 1) % maxst;
            sfx_play(SFX_MOVE);
        }
        if (prompt_practice && KEY_HIT(KEY_UP | KEY_DOWN)) { row ^= 1; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_B)) { sfx_play(SFX_BACK); return -1; }
        if (KEY_HIT(KEY_A | KEY_START)) {
            sfx_play(SFX_SELECT);
            if (stage_out) *stage_out = stage;
            return sel;
        }
    }
}

static void records_scene(void) {
    static const char *const dn[3] = { "EASY", "NORMAL", "HARD" };
    int d = save.last_diff;
    int redraw = 1;
    text_clear();
    set_display(0x1D40);            /* hide the logo layer: banners reuse its tiles */
    banner_show(0, BANNER_RECORDS, 1, BP_GOLD);
    for (;;) {
        if (redraw) {
            redraw = 0;
            panel_draw(3, 4, 24, 13);
            text_put(5, 5, "<", TS_PANEL);
            text_put(24, 5, ">", TS_PANEL);
            text_put(15 - (str_len(dn[d]) >> 1), 5, dn[d], TS_PANEL);
            for (int i = 0; i < NUM_SCORES; i++) {
                int y = 7 + i * 2;
                char nm[4] = { (char)save.name[d][i][0], (char)save.name[d][i][1], (char)save.name[d][i][2], 0 };
                text_num(5, y, (u32)(i + 1), 1, TS_PANEL, 0);
                text_put(6, y, ".", TS_PANEL);
                text_put(8, y, nm, TS_PANEL);
                text_num(12, y, save.score[d][i], 8, TS_PANEL, 0);
                int st = save.stage[d][i];
                if (st >= 6) text_put(21, y, "ALL", TS_PANEL);
                else { text_put(21, y, "ST", TS_PANEL); text_num(23, y, (u32)st, 1, TS_PANEL, 0); }
            }
            text_center(18, "B  BACK", TS_HUD);
        }
        oam_clear();
        scroll_dist++;
        bg_update();
        frame_wait();
        if (KEY_REPEAT(KEY_RIGHT)) { d = (d + 1) % 3; redraw = 1; sfx_play(SFX_MOVE); }
        if (KEY_REPEAT(KEY_LEFT)) { d = (d + 2) % 3; redraw = 1; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_B | KEY_START | KEY_A)) { sfx_play(SFX_BACK); break; }
    }
    text_clear();
}

static void name_entry(int diff, int rank, u32 score, int stage) {
    static const char chars[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.!? ";
    int nch = sizeof(chars) - 1;
    u8 name[3] = { 'A', 'A', 'A' };
    int idx[3] = { 0, 0, 0 };
    int pos = 0;
    scene_reset();
    load_stage_bg(0);
    load_ui();
    set_display(0x1F40);
    music_play(SONG_ENDING);
    banner_show(0, BANNER_RECORD, 2, BP_GOLD);
    panel_draw(5, 6, 20, 10);
    text_put(7, 7, "RANK", TS_PANEL);
    text_num(12, 7, (u32)(rank + 1), 1, TS_PANEL, 0);
    text_num(14, 7, score, 8, TS_PANEL, 0);
    text_put(7, 9, "ENTER YOUR NAME", TS_PANEL);
    text_put(7, 14, "A NEXT  B BACK", TS_PANEL);
    fade = 16;
    int t = 0, done = 0;
    while (!done) {
        t++;
        if (fade > 0) fade--;
        for (int i = 0; i < 3; i++) {
            char s[2] = { chars[idx[i]], 0 };
            text_put(13 + i * 2, 11, s, TS_PANEL);
            text_tile(13 + i * 2, 12, (i == pos && (t & 16)) ? UI_PAN + font_map['-'] : UI_PAN);
        }
        oam_clear();
        menu_ship(18, 120, t, 2);
        scroll_dist = t;
        bg_update();
        frame_wait();
        if (KEY_REPEAT(KEY_UP)) { idx[pos] = (idx[pos] + 1) % nch; sfx_play(SFX_MOVE); }
        if (KEY_REPEAT(KEY_DOWN)) { idx[pos] = (idx[pos] + nch - 1) % nch; sfx_play(SFX_MOVE); }
        if (KEY_HIT(KEY_A)) {
            sfx_play(SFX_SELECT);
            if (pos < 2) pos++; else done = 1;
        }
        if (KEY_HIT(KEY_B) && pos > 0) { pos--; sfx_play(SFX_BACK); }
        if (KEY_HIT(KEY_START)) { done = 1; sfx_play(SFX_SELECT); }
    }
    for (int i = 0; i < 3; i++) name[i] = (u8)chars[idx[i]];
    score_insert(diff, rank, score, name, stage);
    save_write();
    fade_out(1);
}

static void game_over_screen(void) {
    music_play(SONG_GAMEOVER);
    dim = 8;
    banner_show(0, BANNER_GAMEOVER, 7, BP_RED);
    for (int t = 0; t < 240; t++) {
        frame_wait();
        if (t > 60 && KEY_HIT(KEY_A | KEY_START)) break;
    }
    dim = 0;
}

static const char *const ending_text[] = {
    "THE GLOOM FLEET SCATTERS",
    "INTO THE MORNING MIST.",
    "",
    "THE SKY GARDENS BLOOM",
    "AGAIN, AND EVERY LITTLE",
    "SPROUT IS SAFE AT HOME.",
    "",
    "THANK YOU, MOCHI!",
};

static const char *const credits[] = {
    "SKY SPROUT",
    "",
    "A RETRA ORIGINAL",
    "",
    "GAME DESIGN",
    "THE RETRA TEAM",
    "",
    "PIXEL ART",
    "PROCEDURAL BRUSHES",
    "AND MANY SPROUTS",
    "",
    "MUSIC AND SOUND",
    "FOUR TINY PSG VOICES",
    "",
    "STARRING",
    "MOCHI",
    "THE SPROUTLINGS",
    "",
    "BOSSES",
    "BARON BUZZSAW",
    "THUNDERHULL",
    "ADMIRAL CLAMP",
    "TICKTOCK TITAN",
    "KING GLOOM",
    "",
    "THANKS FOR PLAYING!",
};

static void ending_scene(void) {
    scene_reset();
    load_stage_bg(0);
    load_ui();
    set_display(0x1F40);
    music_play(SONG_ENDING);
    banner_show(0, BANNER_ALLCLEAR, 1, BP_GOLD);
    fade = 16;
    int nlines = sizeof(ending_text) / sizeof(ending_text[0]);
    int t = 0, shown = 0;
    while (1) {
        t++;
        if (fade > 0 && (t & 1)) fade--;
        if (t > 60 && (t % 50) == 0 && shown < nlines) {
            if (shown == 0) panel_draw(2, 5, 26, nlines + 2);
            text_put(3, 6 + shown, ending_text[shown], TS_PANEL);
            shown++;
        }
        oam_clear();
        menu_ship(-30 + ((t * 2) % 320) / 2 + 20, 132 + (isin(t * 3) >> 6), t, 2);
        scroll_dist = t;
        bg_update();
        frame_wait();
        if (shown >= nlines && (t > 60 + nlines * 50 + 180 || KEY_HIT(KEY_A | KEY_START))) break;
        if (shown < nlines && KEY_HIT(KEY_START)) { while (shown < nlines) { if (!shown) panel_draw(2, 5, 26, nlines + 2); text_put(3, 6 + shown, ending_text[shown], TS_PANEL); shown++; } t = 60 + nlines * 50; }
    }
    fade_out(1);
    /* credits roll on the text layer */
    scene_reset();
    load_stage_bg(3);
    load_ui();
    set_display(0x1F40);
    int nc = sizeof(credits) / sizeof(credits[0]);
    int y_px = 0;
    fade = 16;
    int next_line = 0;
    for (t = 0;; t++) {
        if (fade > 0 && (t & 1)) fade--;
        /* write lines just below the visible window as it scrolls */
        int row_needed = (y_px >> 3) + 21;
        while (next_line < nc && next_line + 21 <= row_needed) {
            text_clear_rows(next_line + 21, next_line + 21);
            text_center(next_line + 21, credits[next_line], next_line == 0 ? TS_GOLD : TS_HUD);
            next_line++;
        }
        if ((y_px & 7) == 0) text_clear_rows((y_px >> 3) - 1, (y_px >> 3) - 1);
        REG_BGVOFS(0) = (u16)y_px;
        oam_clear();
        menu_ship(100 + (isin(t) >> 4), 70 + (isin(t * 2) >> 5), t, 2);
        scroll_dist = t;
        bg_update();
        frame_wait();
        if ((t & 1) && y_px < (nc + 12) * 8) y_px++;
        if (y_px >= (nc + 12) * 8 || KEY_HIT(KEY_START)) break;
    }
    REG_BGVOFS(0) = 0;
    text_clear();
    banner_show(0, BANNER_END, 8, BP_PINK);
    for (t = 0; t < 400; t++) {
        oam_clear();
        menu_ship(100, 110 + (isin(t * 2) >> 5), t, 2);
        scroll_dist += 1;
        bg_update();
        frame_wait();
        if (t > 90 && KEY_HIT(KEY_A | KEY_START)) break;
    }
    fade_out(1);
}

static void run_game(int diff, int start_stage, int practice) {
    memset(&G, 0, sizeof(G));
    G.diff = diff;
    G.practice = practice;
    G.lives = diff == 0 ? 3 : 2;
    G.bombs = diff == 0 ? 4 : 3;
    G.speed = 1;
    G.next_extend = 100000;
    G.spdmul = diff == 0 ? 205 : (diff == 2 ? 300 : 256);
    G.ratemul = diff == 0 ? 358 : (diff == 2 ? 192 : 256);
#ifdef DEBUG_POWER
    G.level = 3;
    G.options = 2;
    G.weapon = DEBUG_POWER;
#endif
    save.last_diff = (u8)diff;
    save.plays++;
    int stage = start_stage, cleared = 0, gameover = 0;
    for (; stage < 5; stage++) {
        G.stage = stage;
        if (!practice && save.reached[diff] < stage + 1) save.reached[diff] = (u8)(stage + 1);
        save_write();
        scene_reset();
        int r = play_stage();
        if (r == PLAY_QUIT) { near_hdma = 1; return; }
        if (r == PLAY_GAMEOVER) { game_over_screen(); gameover = 1; break; }
        if (practice) break;
        if (stage == 4) cleared = 1;
    }
    near_hdma = 1;
    if (practice) return;
    if (cleared) {
        save.reached[diff] = 6;
        save.clears[diff]++;
        save_write();
        ending_scene();
    }
    (void)gameover;
    u32 final = G.run_best > G.score ? G.run_best : G.score;
    int rank = score_rank(diff, final);
    if (rank >= 0) name_entry(diff, rank, final, cleared ? 6 : stage + 1);
}

int main(void) {
    video_init();
    audio_init();
    save_load();
    load_ui();
    load_common_sprites();
#ifdef DEBUG_STAGE
    for (;;) {
        run_game(1, DEBUG_STAGE - 1, 0);
    }
#endif
    for (;;) {
        int choice = title_scene();
        if (choice == 0) {
            int d = difficulty_select(0, 0);
            if (d < 0) continue;
            fade_out(2);
            run_game(d, 0, 0);
        } else if (choice == 1) {
            int st = 0;
            int d = difficulty_select(1, &st);
            if (d < 0) continue;
            fade_out(2);
            run_game(d, st, 1);
        } else {
            records_scene();
        }
    }
}
