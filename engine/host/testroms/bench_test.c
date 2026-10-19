/* Worst-case load for benchmarking: CPU never idles (ROM THUMB code, prefetch on), mode 0 with
   four text BGs, 128 sprites and alpha blending enabled every line. */
#include "testlib.h"

int main(void) {
    REG16(0x04000204) = 0x4317;                    /* waitstates as used by many games + prefetch */
    vu16* pal = (vu16*)0x05000000;
    for (int i = 0; i < 512; i++) pal[i] = (u16)(i * 0x0421 + 0x1234);
    vu32* vram = (vu32*)0x06000000;
    for (int i = 0; i < 0x4000; i++) vram[i] = 0x12345678u * (u32)(i + 1);  /* tiles + maps */
    vu32* obj = (vu32*)0x06010000;
    for (int i = 0; i < 0x2000; i++) obj[i] = 0x11111111u * (u32)((i & 15) | 1);
    vu16* oam = (vu16*)0x07000000;
    for (int i = 0; i < 128; i++) {
        oam[i * 4 + 0] = (u16)((i * 13) & 0xFF) | (i & 1 ? 0x0400 : 0) | 0x4000; /* wide, some semi */
        oam[i * 4 + 1] = (u16)((i * 29) & 0x1FF) | 0x4000;                       /* 16x... size 1 */
        oam[i * 4 + 2] = (u16)(i * 4);
    }
    REG16(0x04000008) = 0x0000 | (8 << 8);
    REG16(0x0400000A) = 0x0001 | (10 << 8) | (1 << 2);
    REG16(0x0400000C) = 0x0002 | (12 << 8) | 0x80;
    REG16(0x0400000E) = 0x0003 | (14 << 8) | 0x4000;
    REG16(0x04000050) = 0x3F41;                    /* alpha: BG0 over everything */
    REG16(0x04000052) = 0x0808;
    REG16(0x04000000) = 0x1F40;                    /* mode 0, BG0-3, OBJ, 1D */
    u32 acc = 1, frame = 0;
    for (;;) {
        for (int i = 0; i < 4000; i++) acc = acc * 1664525u + 1013904223u + (acc >> 7);
        REG16(0x04000010) = (u16)frame;            /* scroll so every line differs */
        REG16(0x04000014) = (u16)(acc & 0x1FF);
        frame++;
    }
}
