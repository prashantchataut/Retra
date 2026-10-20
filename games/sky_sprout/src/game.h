/* Sky Sprout - shared declarations. */
#ifndef GAME_H
#define GAME_H
#include "gba.h"
#include "assets.h"

#define IWRAM_FN __attribute__((long_call))
#define FIX(v) ((v) << 8)

/* ---------------------------------------------------------------- gfx.c */
extern u16 key_cur, key_prev, key_rep;
extern u32 frame_count;
#define KEY_HIT(k)    ((key_cur & ~key_prev) & (k))
#define KEY_HELD(k)   (key_cur & (k))
#define KEY_REPEAT(k) (key_rep & (k))

/* OAM categories: display order (first = on top). */
enum { OC_TOP, OC_EBUL, OC_PLAYER, OC_ITEM, OC_FX, OC_SHOT, OC_ENEMY, OC_BOSS, OC_N };
/* shape/size codes = shape * 4 + size */
enum { S8, S16, S32, S64, W16x8, W32x8, W32x16, W64x32, T8x16, T8x32, T16x32, T32x64 };
#define SPR_HF   0x1000
#define SPR_VF   0x2000
#define SPR_SEMI 0x10000

extern u16 ostage[];
extern u8 oc_n[OC_N];
extern const u16 oc_base[OC_N];
extern const u8 oc_cap[OC_N];
extern const u8 ss_w[12], ss_h[12];
extern int shake_x, shake_y;

void video_init(void);
void frame_wait(void);
void poll_keys(void);
void copy16(volatile u16 *dst, const u16 *src, u32 count);
void copy32(volatile void *dst, const void *src, u32 bytes);
void fill16(volatile u16 *dst, u16 val, u32 count);

void oam_clear(void);
int  spr(int cat, int x, int y, int tile, int ss, int pal, int flags);
int  spr_aff(int cat, int x, int y, int tile, int ss, int pal, int aff, int dbl);
void oam_affine(int aff, int pa, int pb, int pc, int pd);
void oam_rotscale(int aff, int ang, int scale);

void text_clear(void);
void text_clear_rows(int y0, int y1);
void text_tile(int x, int y, u16 tile);
void text_put(int x, int y, const char *s, int style);
void text_center(int y, const char *s, int style);
void text_num(int x, int y, u32 n, int width, int style, int zero_pad);
void panel_draw(int x, int y, int w, int h);
int  str_len(const char *s);
void banner_show(int slot, int id, int y, int pal);
void banner_clear(int y);
int  banner_width(int id);
enum { TS_HUD, TS_PANEL, TS_GOLD };
enum { BP_GOLD = 14, BP_PINK = 12, BP_RED = 11, BP_SKY = 10 };

extern int fade, flash;
void fade_out(int speed);
void fade_in(int speed);

/* backgrounds */
extern u32 scroll_dist;
extern int bg_shift_y;
void load_ui(void);
void load_common_sprites(void);
void load_stage_bg(int st);
void load_stage_sprites(int st);
void load_title_bg(void);
void bg_update(void);
void hdma_stop(void);
void set_display(u16 dispcnt);
void blend_setup(void);

/* ---------------------------------------------------------------- audio.c */
enum { SFX_SHOT, SFX_POP, SFX_BOOM, SFX_POWER, SFX_1UP, SFX_BOMB, SFX_DEATH, SFX_WARN,
       SFX_SHIELD, SFX_HIT, SFX_MOVE, SFX_SELECT, SFX_BACK, SFX_TICK, SFX_PAUSE, SFX_CANCEL,
       SFX_PHASE, SFX_COUNT };
void audio_init(void);
void audio_update(void);
void music_play(int song);
void music_stop(void);
void music_pause(int paused);
int  music_done(void);
void sfx_play(int id);

/* ---------------------------------------------------------------- save.c */
#define NUM_SCORES 5
typedef struct {
    u32 score[3][NUM_SCORES];
    u8 name[3][NUM_SCORES][3];
    u8 stage[3][NUM_SCORES];   /* 1..5 reached, 6 = all clear */
    u8 reached[3];             /* furthest stage reached per difficulty (1..6) */
    u8 clears[3];
    u8 last_diff;
    u8 pad;
    u16 plays;
} SaveData;
extern SaveData save;
void save_load(void);
void save_write(void);
int  score_rank(int diff, u32 score);
void score_insert(int diff, int rank, u32 score, const u8 *name, int stage);
int  max_reached(void);

/* ---------------------------------------------------------------- entities */
typedef struct {
    s32 x, y;
    s16 vx, vy;
    u16 ang;        /* 8.8 angle, 256 = full turn */
    s16 spd;        /* 8.8 px/frame */
    u8 kind, beh, active, anim;
    u16 t;
    s16 p, q;
} Bullet;
#define MAX_EBUL 88
extern Bullet ebul[MAX_EBUL];

enum { BEH_LINE, BEH_ACCEL, BEH_STOPAIM, BEH_CURVE, BEH_GRAV, BEH_SPLIT, BEH_CANCEL };
enum { BK_SM, BK_SB, BK_SV, BK_SA, BK_MM, BK_MB, BK_MV, BK_MA, BK_RM, BK_RB, BK_RV,
       BK_BM, BK_BB, BK_BV, BK_BA, BK_STARA, BK_STARM, BK_PETAL, BK_N };
extern const u16 bk_tile[BK_N];
extern const u8 bk_ss[BK_N], bk_rad[BK_N], bk_half[BK_N];

typedef struct {
    s32 x, y;
    s16 vx, vy;
    u8 kind, active, dmg, t;
    u8 dir; s8 target; u8 hit; u8 pad;
} Shot;
#define MAX_SHOTS 40
extern Shot shots[MAX_SHOTS];
enum { SK_PEA, SK_PETAL, SK_LASER, SK_SEED, SK_OPT };

typedef struct {
    s32 x, y;
    s16 vx, vy;
    s16 hp;
    u8 type, ai, active, flash;
    u16 t;
    s16 p0, p1, p2;
    s16 ox, oy;
    u8 fire, drop, group, state;
    u8 fcd, lcd, dead, killed;
} Enemy;
#define MAX_ENEMIES 24
extern Enemy en[MAX_ENEMIES];

typedef struct { u8 hw, hh, size, frames; u16 tile; u8 anim, pal; s16 hp; u16 score; } EnemyType;
enum { ET_GNAT, ET_SWOOP, ET_TURRET, ET_MINE, ET_SPIN, ET_DIVER, ET_CARRIER, ET_HEAVY, ET_N };
extern const EnemyType etype[ET_N];

typedef struct {
    s32 x, y;
    int alive, invuln, respawn, tilt, prop, fire_cd, bomb_t, hurt_t;
    s16 hx[32], hy[32];
    int hidx;
} Player;
extern Player pl;

/* boss hit boxes (absolute px), filled by boss.c each frame */
extern s16 bhit[3][4];
extern int bhit_n, boss_dmg, boss_hittable;

/* hot.c (IWRAM, ARM) */
int  bullets_update(int px, int py, int vulnerable) IWRAM_FN;
void shots_collide(void) IWRAM_FN;
int  iatan2(int dy, int dx) IWRAM_FN;
extern int cancel_collected;

/* ---------------------------------------------------------------- play.c */
typedef struct {
    int diff, practice, stage;
    u32 score, run_best, next_extend;
    int lives, bombs, continues;
    int weapon, level, options, shield, speed;
    int kills, spawned, chain, chain_t, max_chain, misses;
    int stage_reached, all_clear;
    int spdmul, ratemul;
} Game;
extern Game G;
enum { PLAY_CLEAR, PLAY_GAMEOVER, PLAY_QUIT };
int  play_stage(void);
void add_score(u32 pts);
void kill_chain(void);
int  aim_player(s32 x, s32 y);
void fx_spawn(int type, s32 x, s32 y, int vx, int vy);
void explode(s32 x, s32 y, int big);
void item_spawn(int kind, s32 x, s32 y);
void cancel_bullets(void);
void popup_mult(s32 x, s32 y, int mult);
enum { FX_EXPL, FX_BIGEXPL, FX_SPARK, FX_PUFF, FX_PETAL, FX_POPUP, FX_DEBRIS };
extern int stage_t;

/* ---------------------------------------------------------------- enemy.c */
void enemies_reset(void);
void enemies_update(void);
void enemies_draw(void);
int  enemies_alive(void);
void enemy_spawn_ev(const StageEv *ev);
void enemy_kill(Enemy *e, int by_player);
int  eb_spawn(s32 x, s32 y, int ang, int spd, int kind, int beh, int p, int q);
void shoot_fan(s32 x, s32 y, int ang, int n, int spread, int spd, int kind);
void shoot_ring(s32 x, s32 y, int ang0, int n, int spd, int kind);
void bullets_clear(void);
int  bullets_count(void);
void groups_reset(void);

/* ---------------------------------------------------------------- boss.c */
typedef struct {
    int active, id, mid, state;
    s32 x, y, tx, ty;
    int hp, maxhp, phase, nphase, t, pt, flash, invul, timer;
    int a0, a1, a2, a3, ex, ey, rage, dying;
    int bar_shown;
} Boss;
extern Boss boss;
void boss_start(int stage, int mid);
void boss_update(void);
void boss_draw(void);
int  boss_alive(void);

/* ---------------------------------------------------------------- util */
void *memset(void *d, int c, unsigned n);
void *memcpy(void *d, const void *s, unsigned n);
static inline int iabs(int v) { return v < 0 ? -v : v; }
static inline int isin(int a) { return sin_lut[a & 255]; }
static inline int icos(int a) { return sin_lut[(a + 64) & 255]; }
u32 rnd(void);
void rnd_seed(u32 s);
int udiv(int a, int b);

#ifdef DEBUG_PERF
extern int perf_max, perf_last, perf_over;
#endif

#endif
