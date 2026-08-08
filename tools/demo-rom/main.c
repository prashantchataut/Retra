typedef unsigned short u16;
typedef unsigned int u32;

#define REG_DISPCNT (*(volatile u16*)0x04000000)
#define REG_VCOUNT  (*(volatile u16*)0x04000006)
#define REG_KEYINPUT (*(volatile u16*)0x04000130)
#define VRAM ((volatile u16*)0x06000000)

#define KEY_A      (1u << 0)
#define KEY_B      (1u << 1)
#define KEY_RIGHT  (1u << 4)
#define KEY_LEFT   (1u << 5)
#define KEY_UP     (1u << 6)
#define KEY_DOWN   (1u << 7)

static u16 rgb(unsigned r, unsigned g, unsigned b) {
    return (u16)((r & 31u) | ((g & 31u) << 5) | ((b & 31u) << 10));
}

static void wait_vblank(void) {
    while (REG_VCOUNT >= 160) { }
    while (REG_VCOUNT < 160) { }
}

static void pixel(int x, int y, u16 color) {
    if ((unsigned)x < 240u && (unsigned)y < 160u) VRAM[y * 240 + x] = color;
}

static void rect(int x, int y, int w, int h, u16 color) {
    for (int yy = 0; yy < h; ++yy) {
        for (int xx = 0; xx < w; ++xx) pixel(x + xx, y + yy, color);
    }
}

static void frame(int x, int y, int w, int h, u16 color) {
    rect(x, y, w, 1, color); rect(x, y + h - 1, w, 1, color);
    rect(x, y, 1, h, color); rect(x + w - 1, y, 1, h, color);
}

static void draw_background(void) {
    for (int y = 0; y < 160; ++y) {
        unsigned band = (unsigned)(y >> 4);
        u16 c = rgb(1u + (band >> 2), 3u + (band >> 1), 7u + band);
        for (int x = 0; x < 240; ++x) VRAM[y * 240 + x] = c;
    }
    frame(8, 8, 224, 144, rgb(8, 18, 23));
    frame(10, 10, 220, 140, rgb(2, 7, 12));

    /* Symmetric archive portal: Retra's original mark, rendered in pixels. */
    u16 ice = rgb(23, 30, 31);
    u16 aqua = rgb(6, 25, 26);
    rect(20, 22, 36, 4, ice); rect(20, 22, 4, 34, ice);
    rect(20, 52, 18, 4, ice); rect(52, 22, 4, 34, ice);
    rect(38, 38, 18, 4, ice); rect(38, 38, 4, 18, ice);
    rect(44, 44, 6, 6, aqua);

    /* Header bars and playfield. */
    rect(68, 24, 112, 4, ice);
    rect(68, 34, 76, 3, rgb(9, 20, 24));
    rect(68, 43, 96, 3, rgb(6, 15, 21));
    frame(18, 68, 204, 68, rgb(5, 16, 22));
    rect(20, 70, 200, 64, rgb(1, 5, 10));
    rect(18, 142, 204, 3, rgb(5, 18, 22));
}

static void draw_orb(int x, int y, u16 outer, u16 inner) {
    rect(x - 3, y - 6, 7, 13, outer);
    rect(x - 6, y - 3, 13, 7, outer);
    rect(x - 3, y - 3, 7, 7, inner);
    pixel(x - 2, y - 2, rgb(31, 31, 31));
}

static void draw_target(int x, int y, u16 color) {
    rect(x - 1, y - 6, 3, 13, color);
    rect(x - 6, y - 1, 13, 3, color);
    rect(x - 3, y - 3, 7, 7, color);
    pixel(x, y, rgb(31, 31, 31));
}

static void draw_score(unsigned score) {
    rect(184, 23, 38, 25, rgb(1, 5, 10));
    frame(184, 23, 38, 25, rgb(7, 19, 23));
    unsigned bars = score & 7u;
    for (unsigned i = 0; i < bars; ++i) {
        rect(189 + (int)i * 4, 39 - (int)i * 2, 3, 5 + (int)i * 2, rgb(4, 28, 22));
    }
}

int main(void) {
    REG_DISPCNT = 0x0403; /* Mode 3 + BG2 */
    draw_background();

    int x = 70, y = 102;
    int tx = 176, ty = 92;
    unsigned score = 0;
    unsigned rng = 0x51A7u;
    draw_orb(x, y, rgb(4, 22, 24), rgb(12, 31, 28));
    draw_target(tx, ty, rgb(31, 12, 8));
    draw_score(score);

    for (;;) {
        wait_vblank();
        u16 keys = (u16)(~REG_KEYINPUT);
        int oldx = x, oldy = y;
        if ((keys & KEY_LEFT) && x > 28) --x;
        if ((keys & KEY_RIGHT) && x < 212) ++x;
        if ((keys & KEY_UP) && y > 78) --y;
        if ((keys & KEY_DOWN) && y < 126) ++y;
        if (oldx != x || oldy != y) {
            draw_orb(oldx, oldy, rgb(1, 5, 10), rgb(1, 5, 10));
            draw_orb(x, y, rgb(4, 22, 24), rgb(12, 31, 28));
        }
        int dx = x - tx; if (dx < 0) dx = -dx;
        int dy = y - ty; if (dy < 0) dy = -dy;
        if (dx < 8 && dy < 8) {
            draw_target(tx, ty, rgb(1, 5, 10));
            ++score;
            rng = rng * 1664525u + 1013904223u;
            tx = 30 + (int)((rng >> 8) & 127u) + (int)((rng >> 19) & 31u);
            rng = rng * 1664525u + 1013904223u;
            ty = 78 + (int)((rng >> 10) & 31u) + (int)((rng >> 21) & 15u);
            draw_target(tx, ty, (score & 1u) ? rgb(31, 12, 8) : rgb(5, 28, 24));
            draw_score(score);
        }
        if (keys & KEY_A) {
            draw_orb(x, y, rgb(20, 27, 31), rgb(31, 19, 10));
        } else if (keys & KEY_B) {
            draw_orb(x, y, rgb(9, 26, 29), rgb(10, 20, 31));
        }
    }
}
