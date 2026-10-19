/* Tiny support library for Retra's own test ROMs (original code). */
#pragma once
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef signed char s8;
typedef signed short s16;
typedef signed int s32;
typedef volatile u8 vu8;
typedef volatile u16 vu16;
typedef volatile u32 vu32;

#define REG16(a) (*(vu16*)(a))
#define REG32(a) (*(vu32*)(a))

/* Debug print port understood by retra-headless --debug-log. */
static inline void dbg_print(const char* s) {
    REG16(0x04FFF780) = 0xC0DE;
    if (REG16(0x04FFF780) != 0x1DEA) return;
    vu8* out = (vu8*)0x04FFF600;
    int i = 0;
    while (s[i] && i < 255) { out[i] = s[i]; i++; }
    out[i] = 0;
    REG16(0x04FFF700) = 0x100 | 2;
}

static inline char* fmt_hex(char* p, u32 v, int digits) {
    for (int i = digits - 1; i >= 0; i--) {
        u32 n = (v >> (i * 4)) & 15;
        *p++ = (char)(n < 10 ? '0' + n : 'A' + n - 10);
    }
    *p = 0;
    return p;
}

static inline char* fmt_str(char* p, const char* s) {
    while (*s) *p++ = *s++;
    *p = 0;
    return p;
}

static inline void wait_vblank(void) {
    while (REG16(0x04000006) >= 160) {}
    while (REG16(0x04000006) < 160) {}
}
