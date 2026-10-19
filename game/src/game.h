/* Mochi Hop - shared declarations. */
#ifndef GAME_H
#define GAME_H
#include "gba.h"
#include "assets.h"

/* ---- gfx.c ---- */
extern u16 key_cur, key_prev;
extern u32 frame_count;
#define KEY_HIT(k)  ((key_cur & ~key_prev) & (k))
#define KEY_HELD(k) (key_cur & (k))

void video_init(void);
void vsync(void);
void poll_keys(void);
void copy16(volatile u16 *dst, const u16 *src, u32 count);
void fill16(volatile u16 *dst, u16 val, u32 count);
void load_ui(void);
void load_sprites(void);
void load_theme_bg(int theme);
void load_terrain(int theme);

void oam_begin(void);
int  oam_add(u16 a0, u16 a1, u16 a2);
void oam_affine(int aff, s16 pa, s16 pb, s16 pc, s16 pd);
void oam_commit(void);
int  spr16(int x, int y, int tile, int pal, int hflip);   /* screen coords, returns oam idx */
int  spr8(int x, int y, int tile, int pal);
int  spr32(int x, int y, int tile, int pal, int hflip);
int  spr16x32(int x, int y, int tile, int pal, int hflip);
void spr_mochi(int sx, int sy, int frame, int scale_x, int scale_y, int flip, int aff);

void text_clear(void);
void text_clear_rows(int y0, int y1);
void text_put(int x, int y, const char *s, int panel);
void text_center(int y, const char *s, int panel);
void text_num(int x, int y, int n, int width, int panel);
void text_tile(int x, int y, u16 tile);
void panel_draw(int x, int y, int w, int h);
int  str_len(const char *s);

extern int fade;
void fade_apply(void);

/* ---- audio.c ---- */
enum { SFX_JUMP, SFX_STAR, SFX_SPROUT, SFX_STOMP, SFX_HURT, SFX_CHECK, SFX_SPRING,
       SFX_MOVE, SFX_SELECT, SFX_BACK, SFX_TICK, SFX_FALL };
void audio_init(void);
void audio_update(void);
void music_play(int song);
void music_stop(void);
void music_pause(int paused);
int  music_done(void);
void sfx_play(int id);

/* ---- save.c ---- */
typedef struct {
    u8 unlocked;                 /* number of playable levels */
    u8 best[NUM_LEVELS];         /* best star count */
    u8 sprouts[NUM_LEVELS];      /* bitmask of sprouts ever rescued */
    u8 cleared[NUM_LEVELS];
    u8 seen_ending;
} SaveData;
extern SaveData save;
void save_load(void);
void save_write(void);
int  save_total_stars(void);
int  total_level_stars(void);

/* ---- play.c ---- */
enum { RES_QUIT = 0, RES_CLEAR = 1 };
extern int res_stars, res_sprouts;
int play_level(int idx);

/* ---- util ---- */
void *memset(void *d, int c, unsigned n);
void *memcpy(void *d, const void *s, unsigned n);
static inline int iabs(int v) { return v < 0 ? -v : v; }
static inline int isin(int a) { return sin_lut[a & 255]; }
static inline int icos(int a) { return sin_lut[(a + 64) & 255]; }
u32 rnd(void);
int udiv(int a, int b);

#endif
