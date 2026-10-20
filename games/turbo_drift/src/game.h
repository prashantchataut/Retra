/* Turbo Drift - shared declarations. */
#ifndef GAME_H
#define GAME_H
#include "gba.h"
#include "assets.h"

/* ---------------------------------------------------------------- iwram.c */
typedef struct { s16 bg1h, bg1v, bg2h, bg2v, bg3h, bg3v; s16 pa, pb, pc, pd; s32 x, y; } LineRegs;
typedef struct { s16 x, y, w, clip; } SegScr;
typedef struct {
    s32 cam_z, cam_x, cam_y, sky_x;
    s32 focal, horizon, curve_k;
    const s8 *curve;
    const s16 *ty;
    s32 n;
    const u16 *sky, *ground;
    s32 band_row, sky_shift0, sky_shift1;
} RoadView;

extern LineRegs line_regs[2][161];
extern u16 line_bd[2][162];
extern volatile int back_buf, frame_ready, road_on;
extern volatile u32 vbl_count;
extern SegScr segscr[DRAW_DIST + 2];
extern int road_top;
extern volatile const void *vb_src[4];
extern volatile void *vb_dst[4];
extern volatile u32 vb_words[4];
extern u16 oam_shadow[512];

void irq_handler(void) __attribute__((long_call));
u32 udiv32(u32 n, u32 d) __attribute__((long_call));
s32 sdiv32(s32 n, s32 d) __attribute__((long_call));
void road_project(const RoadView *v) __attribute__((long_call));
void lines_flat(u16 top, u16 bottom) __attribute__((long_call));

/* ---------------------------------------------------------------- gfx.c */
extern u16 key_cur, key_prev, key_rep;
extern u32 frame_count;
#define KEY_HIT(k)  ((key_cur & ~key_prev) & (k))
#define KEY_HELD(k) (key_cur & (k))
#define KEY_REP(k)  (key_rep & (k))

void video_init(void);
void frame_end(void);
void poll_keys(void);
void copy16(volatile void *dst, const void *src, u32 halfwords);
void fill16(volatile void *dst, u16 val, u32 halfwords);
void vb_copy(volatile void *dst, const void *src, u32 bytes);
void *memset(void *d, int c, unsigned n);
void *memcpy(void *d, const void *s, unsigned n);
u32 rnd(void);
void rnd_seed(u32 s);

void oam_begin(void);
int  oam_add(u16 a0, u16 a1, u16 a2);
int  oam_count(void);
void oam_end(void);
void mat_set(int i, s16 pa, s16 pb, s16 pc, s16 pd);
void mats_reset(void);
int  mat_alloc(void);

/* text on BG0 (screenblock 30) */
#define PAL_MAIN 15
#define PAL_HI   14
#define PAL_DIM  13
#define PAL_LOGO 11
void text_clear(void);
void text_clear_rect(int x, int y, int w, int h);
void text_put(int x, int y, const char *s, int pal);
void text_center(int y, const char *s, int pal);
void text_num(int x, int y, u32 n, int width, int pal, int zero);
void text_time(int x, int y, u32 frames, int pal);
void text_tile(int x, int y, u16 tile, int pal);
void text_big_num(int x, int y, u32 n, int digits, int pal);
void panel_draw(int x, int y, int w, int h);
int  str_len(const char *s);
void fmt_num(char *buf, u32 n);
extern int fade;
void fade_apply(void);
void ui_load(void);

/* ---------------------------------------------------------------- audio.c */
enum { SFX_MOVE, SFX_SELECT, SFX_BACK, SFX_BUY, SFX_ERROR, SFX_COUNT, SFX_GO, SFX_LAP, SFX_CRASH,
       SFX_NITRO, SFX_NEAR, SFX_BOOST, SFX_BUMP, SFX_FINISH };
void audio_init(void);
void audio_update(void) __attribute__((long_call));
void music_play(int song);
void music_stop(void);
int  music_done(void);
void sfx_play(int id);
void engine_set(int on, int rpm, int load);
void skid_set(int on);
extern u8 sound_on;

/* ---------------------------------------------------------------- save.c */
#define NUM_CARS 4
#define NUM_EVENTS 8
typedef struct {
    u32 credits;
    u8 owned, car, unlocked, champion;
    u8 paint[NUM_CARS];
    u8 upg[NUM_CARS][4];
    u8 career[NUM_EVENTS];
    u8 sound, races_won, pad0, pad1;
    u16 best_lap[NUM_TRACKS];
    u32 best_race[NUM_TRACKS];
    u32 total_races;
} SaveData;
extern SaveData save;
void save_load(void);
void save_write(void);
void save_reset(void);

/* ---------------------------------------------------------------- race.c */
enum { MODE_CAREER, MODE_QUICK, MODE_TIME, MODE_ATTRACT };
typedef struct {
    u8 track, mode, car, paint;
    u8 upg[4];
    u8 rivals, traffic, skill, laps;
} RaceSetup;
typedef struct {
    u8 quit, restart, pos, nracers;
    u32 time, best_lap;
    u8 order[6];
    u32 times[6];
    u8 new_lap_record, new_race_record, pad0, pad1;
} RaceResult;
typedef struct { const char *name; u16 top, accel, grip, nitro; u32 price; } CarStats;
extern const CarStats car_stats[NUM_CARS];
extern const char *const rival_names[5];
void race_run(const RaceSetup *rs, RaceResult *res);
void attract_init(int track);
void attract_step(int oam_reserved);
void race_load_env(int env, int menu);

/* ---------------------------------------------------------------- util */
static inline int iabs(int v) { return v < 0 ? -v : v; }
static inline int isin(int a) { return sin_lut[a & 255]; }
static inline int icos(int a) { return sin_lut[(a + 64) & 255]; }
static inline int clampi(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }

#endif
