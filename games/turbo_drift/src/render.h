/* Turbo Drift - render.c interface. */
#ifndef RENDER_H
#define RENDER_H
#include "game.h"

enum { PART_SPARK = 1, PART_SMOKE, PART_DUST, PART_STREAK };
extern int cur_env;
void make_car_pal(volatile u16 *dst, const u16 *ramp, int driver);
int spr_scaled(const u16 *tile, const u8 *shape, const u8 *size, const u8 *ww, const u8 *hh,
               int sx, int sy, int s, int pal, int flip, int prio);
int spr_object(const ObjDef *od, int sx, int sy, int s, int pal, int flip);
int spr_car(int kind, int frame_tile, int sx, int sy, int s, int pal, int flip);
void parts_clear(void);
void part_spawn(int kind, int x, int y, int vx, int vy, int life);
void parts_update(void);
void parts_draw(void);

#endif
