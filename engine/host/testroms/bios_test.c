/* BIOS decompression / copy SWIs, checked against data produced by independent encoders
   (gen_bios_data.py). Results go to the debug port. */
#include "testlib.h"
#include "bios_data.h"

#define SWI2(num, a, b)                                                                 \\
    do {                                                                                \\
        register u32 r0_ asm("r0") = (u32)(a);                                          \\
        register u32 r1_ asm("r1") = (u32)(b);                                          \\
        asm volatile("swi " #num : "+r"(r0_), "+r"(r1_) : : "r2", "r3", "r12", "lr", "memory"); \\
    } while (0)
#define SWI3(num, a, b, c)                                                              \\
    do {                                                                                \\
        register u32 r0_ asm("r0") = (u32)(a);                                          \\
        register u32 r1_ asm("r1") = (u32)(b);                                          \\
        register u32 r2_ asm("r2") = (u32)(c);                                          \\
        asm volatile("swi " #num : "+r"(r0_), "+r"(r1_), "+r"(r2_) : : "r3", "r12", "lr", "memory"); \\
    } while (0)

static int fails = 0;

static void check(const char* name, const volatile u8* got, const u8* want, u32 n) {
    u32 bad = 0;
    for (u32 i = 0; i < n; i++)
        if (got[i] != want[i]) bad++;
    char buf[80], *p = fmt_str(buf, bad ? "FAIL " : "ok   ");
    p = fmt_str(p, name);
    if (bad) {
        p = fmt_str(p, " mismatches=");
        fmt_hex(p, bad, 4);
        fails++;
    }
    dbg_print(buf);
}

static void clear(volatile u8* p, u32 n) {
    for (u32 i = 0; i < n; i += 2) *(vu16*)(p + i) = 0;
}

struct UnpackInfo {
    u16 len;
    u8 srcw, dstw;
    u32 offset;
};

int main(void) {
    volatile u8* ew = (volatile u8*)0x02010000;
    volatile u8* vr = (volatile u8*)0x06004000;
    clear(ew, 2048);
    SWI2(0x11, lz_src, ew);
    check("LZ77 WRAM", ew, plain_words, sizeof(plain_words));
    clear(vr, 2048);
    SWI2(0x12, lz_src, vr);
    check("LZ77 VRAM", vr, plain_words, sizeof(plain_words));
    clear(ew, 2048);
    SWI2(0x11, lz_text_src, ew);
    check("LZ77 text", ew, plain_text, sizeof(plain_text));
    clear(ew, 2048);
    SWI2(0x14, rl_src, ew);
    check("RL WRAM", ew, plain_words, sizeof(plain_words));
    clear(vr, 2048);
    SWI2(0x15, rl_src, vr);
    check("RL VRAM", vr, plain_words, sizeof(plain_words));
    clear(ew, 2048);
    SWI2(0x13, huff8_src, ew);
    check("Huffman 8-bit", ew, plain_text, sizeof(plain_text));
    clear(ew, 2048);
    SWI2(0x13, huff4_src, ew);
    check("Huffman 4-bit", ew, plain_small, sizeof(plain_small));
    clear(ew, 2048);
    SWI2(0x16, diff8_src, ew);
    check("Diff8 WRAM", ew, plain_words, 256);
    clear(vr, 2048);
    SWI2(0x17, diff8_src, vr);
    check("Diff8 VRAM", vr, plain_words, 256);
    clear(ew, 2048);
    SWI2(0x18, diff16_src, ew);
    check("Diff16", ew, plain_words, 256);
    static const struct UnpackInfo info = {sizeof(unpack_src), 2, 8, 1};
    clear(ew, 2048);
    SWI3(0x10, unpack_src, ew, &info);
    check("BitUnPack 2->8", ew, unpack_expected, sizeof(unpack_expected));
    /* CpuSet copy (16-bit) and fill (32-bit), CpuFastSet copy */
    clear(ew, 2048);
    SWI3(0x0B, plain_text, ew, 128);
    check("CpuSet copy16", ew, plain_text, 256);
    static const u32 fillv = 0x5A5AA5A5u;
    static u8 fillx[64];
    for (int i = 0; i < 64; i++) fillx[i] = (u8)((i & 3) == 0 ? 0xA5 : (i & 3) == 1 ? 0xA5 : 0x5A);
    clear(ew, 2048);
    SWI3(0x0B, &fillv, ew, 16 | (1 << 24) | (1 << 26));
    check("CpuSet fill32", ew, fillx, 64);
    clear(ew, 2048);
    SWI3(0x0C, plain_words, ew, 200); /* rounds up to 208 words */
    check("CpuFastSet copy", ew, plain_words, 832);
    char buf[32], *p = fmt_str(buf, fails ? "RESULT FAIL " : "RESULT PASS ");
    fmt_hex(p, (u32)fails, 2);
    dbg_print(buf);
    for (;;) wait_vblank();
}
