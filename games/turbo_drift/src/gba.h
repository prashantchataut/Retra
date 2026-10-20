/* Minimal GBA hardware definitions for Turbo Drift (written from public hardware docs). */
#ifndef GBA_H
#define GBA_H

typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef signed char s8;
typedef signed short s16;
typedef signed int s32;
typedef long long s64;

#define REG8(a)  (*(volatile u8 *)(a))
#define REG16(a) (*(volatile u16 *)(a))
#define REG32(a) (*(volatile u32 *)(a))

#define REG_DISPCNT   REG16(0x04000000)
#define REG_DISPSTAT  REG16(0x04000004)
#define REG_VCOUNT    REG16(0x04000006)
#define REG_BGCNT(n)  REG16(0x04000008 + 2 * (n))
#define REG_BGHOFS(n) REG16(0x04000010 + 4 * (n))
#define REG_BGVOFS(n) REG16(0x04000012 + 4 * (n))
#define REG_BG2PA     REG16(0x04000020)
#define REG_BG2PB     REG16(0x04000022)
#define REG_BG2PC     REG16(0x04000024)
#define REG_BG2PD     REG16(0x04000026)
#define REG_BG2X      REG32(0x04000028)
#define REG_BG2Y      REG32(0x0400002C)
#define REG_WIN0H     REG16(0x04000040)
#define REG_WIN1H     REG16(0x04000042)
#define REG_WIN0V     REG16(0x04000044)
#define REG_WIN1V     REG16(0x04000046)
#define REG_WININ     REG16(0x04000048)
#define REG_WINOUT    REG16(0x0400004A)
#define REG_MOSAIC    REG16(0x0400004C)
#define REG_BLDCNT    REG16(0x04000050)
#define REG_BLDALPHA  REG16(0x04000052)
#define REG_BLDY      REG16(0x04000054)

#define REG_SOUND1CNT_L REG16(0x04000060)
#define REG_SOUND1CNT_H REG16(0x04000062)
#define REG_SOUND1CNT_X REG16(0x04000064)
#define REG_SOUND2CNT_L REG16(0x04000068)
#define REG_SOUND2CNT_H REG16(0x0400006C)
#define REG_SOUND3CNT_L REG16(0x04000070)
#define REG_SOUND3CNT_H REG16(0x04000072)
#define REG_SOUND3CNT_X REG16(0x04000074)
#define REG_SOUND4CNT_L REG16(0x04000078)
#define REG_SOUND4CNT_H REG16(0x0400007C)
#define REG_SOUNDCNT_L  REG16(0x04000080)
#define REG_SOUNDCNT_H  REG16(0x04000082)
#define REG_SOUNDCNT_X  REG16(0x04000084)
#define REG_SOUNDBIAS   REG16(0x04000088)
#define WAVE_RAM        ((volatile u32 *)0x04000090)

#define REG_DMA0SAD   REG32(0x040000B0)
#define REG_DMA0DAD   REG32(0x040000B4)
#define REG_DMA0CNT   REG32(0x040000B8)
#define REG_DMA2SAD   REG32(0x040000C8)
#define REG_DMA2DAD   REG32(0x040000CC)
#define REG_DMA2CNT   REG32(0x040000D0)
#define REG_DMA3SAD   REG32(0x040000D4)
#define REG_DMA3DAD   REG32(0x040000D8)
#define REG_DMA3CNT   REG32(0x040000DC)

#define DMA_ENABLE    0x80000000u
#define DMA_HBLANK    0x20000000u
#define DMA_REPEAT    0x02000000u
#define DMA_32        0x04000000u
#define DMA_DST_FIX   0x00400000u
#define DMA_DST_RELOAD 0x00600000u

#define REG_KEYINPUT  REG16(0x04000130)
#define REG_IE        REG16(0x04000200)
#define REG_IF        REG16(0x04000202)
#define REG_WAITCNT   REG16(0x04000204)
#define REG_IME       REG16(0x04000208)
#define BIOS_IF       (*(volatile u16 *)0x03007FF8)
#define IRQ_VECTOR    (*(volatile u32 *)0x03007FFC)
#define IRQ_VBLANK    0x0001

#define PAL_BG   ((volatile u16 *)0x05000000)
#define PAL_OBJ  ((volatile u16 *)0x05000200)
#define VRAM     ((volatile u16 *)0x06000000)
#define CHARBLOCK(n) ((volatile u16 *)(0x06000000 + (n) * 0x4000))
#define SCREENBLOCK(n) ((volatile u16 *)(0x06000000 + (n) * 0x800))
#define OBJ_VRAM ((volatile u16 *)0x06010000)
#define OAM      ((volatile u16 *)0x07000000)
#define SRAM     ((volatile u8 *)0x0E000000)

#define KEY_A      0x0001
#define KEY_B      0x0002
#define KEY_SELECT 0x0004
#define KEY_START  0x0008
#define KEY_RIGHT  0x0010
#define KEY_LEFT   0x0020
#define KEY_UP     0x0040
#define KEY_DOWN   0x0080
#define KEY_R      0x0100
#define KEY_L      0x0200

#define IWRAM_CODE __attribute__((section(".iwram"), long_call, noinline))
#define EWRAM_BSS  __attribute__((section(".sbss")))

#endif
