/* EEPROM (serial via DMA3) test. Build with -DEEPROM_BITS=6 for 512 B parts, 14 for 8 KiB. */
#include "testlib.h"

const char eep_tag[] __attribute__((used, aligned(4))) = "EEPROM_V124";

#ifndef EEPROM_BITS
#define EEPROM_BITS 14
#endif

static u16 bits[128];

static void dma3(const void* src, void* dst, u32 count) {
    REG32(0x040000D4) = (u32)src;
    REG32(0x040000D8) = (u32)dst;
    REG32(0x040000DC) = count | 0x80000000u;
    while (REG32(0x040000DC) & 0x80000000u) {}
}

static int put_addr(int n, u32 addr) {
    for (int i = EEPROM_BITS - 1; i >= 0; i--) bits[n++] = (addr >> i) & 1;
    return n;
}

static void eep_write(u32 addr, const u8* data) {
    int n = 0;
    bits[n++] = 1;
    bits[n++] = 0;
    n = put_addr(n, addr);
    for (int b = 0; b < 8; b++)
        for (int i = 7; i >= 0; i--) bits[n++] = (data[b] >> i) & 1;
    bits[n++] = 0;
    dma3(bits, (void*)0x0D000000, (u32)n);
    int timeout = 100000;
    while (!(REG16(0x0D000000) & 1) && --timeout) {}
}

static void eep_read(u32 addr, u8* data) {
    int n = 0;
    bits[n++] = 1;
    bits[n++] = 1;
    n = put_addr(n, addr);
    bits[n++] = 0;
    dma3(bits, (void*)0x0D000000, (u32)n);
    dma3((void*)0x0D000000, bits, 68);
    for (int b = 0; b < 8; b++) {
        u8 v = 0;
        for (int i = 0; i < 8; i++) v = (u8)((v << 1) | (bits[4 + b * 8 + i] & 1));
        data[b] = v;
    }
}

int main(void) {
    char buf[96], *p;
    int fail = 0;
    const u32 blocks = EEPROM_BITS == 6 ? 64 : 1024;
    u8 w[8], r[8];
    for (u32 blk = 0; blk < blocks; blk += (blocks / 16)) {
        for (int i = 0; i < 8; i++) w[i] = (u8)(blk * 7 + i * 31 + 1);
        eep_write(blk, w);
    }
    for (u32 blk = 0; blk < blocks; blk += (blocks / 16)) {
        eep_read(blk, r);
        for (int i = 0; i < 8; i++)
            if (r[i] != (u8)(blk * 7 + i * 31 + 1)) fail++;
    }
    /* an untouched block reads as erased (FF) */
    eep_read(1, r);
    for (int i = 0; i < 8; i++)
        if (r[i] != 0xFF) fail++;
    p = fmt_str(buf, fail ? "RESULT FAIL " : "RESULT PASS ");
    fmt_hex(p, (u32)fail, 4);
    dbg_print(buf);
    for (;;) wait_vblank();
}
