/* Direct Sound A (timer 0 + DMA1 FIFO, left) and PSG channel 2 (440 Hz square, right). */
#include "testlib.h"

/* 16 samples of a sine period: played at 16384 Hz -> 1024 Hz tone */
static const s8 sine16[16] __attribute__((aligned(4))) = {0,   48,  89,  117, 127, 117, 89,  48,
                                                         0,   -48, -89, -117, -127, -117, -89, -48};

int main(void) {
    /* 128 KiB of the 16-sample sine in EWRAM: enough for 8 seconds of playback */
    vu32* ew = (vu32*)0x02000000;
    const u32* src = (const u32*)sine16;
    for (u32 i = 0; i < 0x8000; i++) ew[i] = src[i & 3];
    REG16(0x04000084) = 0x80;                  /* master enable */
    REG16(0x04000080) = 0x0277;                /* PSG vol 7/7, channel 2 -> right */
    REG16(0x04000082) = 0x0A06;                /* PSG 100%, DS A 100%, A left, timer 0, reset A */
    REG16(0x04000088) = 0x0200;
    REG16(0x04000068) = 0xF080;                /* ch2: vol 15, 50% duty */
    REG16(0x0400006C) = 0x8000 | 1750;         /* ch2: ~440 Hz, trigger */
    REG32(0x040000BC) = 0x02000000;            /* DMA1 source */
    REG32(0x040000C0) = 0x040000A0;            /* FIFO A */
    REG16(0x040000C6) = 0xB640 & ~0x0100;      /* enable, special, 32-bit, repeat, src increment */
    REG16(0x04000100) = (u16)(65536 - 1024);   /* 16384 Hz */
    REG16(0x04000102) = 0x0080;
    dbg_print("RESULT RUNNING");
    for (;;) wait_vblank();
}
