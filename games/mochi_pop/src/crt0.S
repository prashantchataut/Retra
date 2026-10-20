@ Mochi Hop - ROM header and startup code (original).
    .section .crt0, "ax"
    .global _start
    .arm
    .align 2
_start:
    b       rom_start
    @ 0x04: Nintendo logo area (left zero; emulators boot without it)
    .fill   156, 1, 0
    @ 0xA0: game title (12 bytes)
    .ascii  "MOCHIHOP"
    .byte   0, 0, 0, 0
    @ 0xAC: game code, 0xB0: maker code
    .ascii  "RMHE"
    .ascii  "00"
    .byte   0x96            @ 0xB2 fixed value
    .byte   0               @ 0xB3 main unit code
    .byte   0               @ 0xB4 device type
    .fill   7, 1, 0         @ 0xB5 reserved
    .byte   0               @ 0xBC software version
    .byte   0               @ 0xBD complement check (patched by tools/fix_header.py)
    .hword  0               @ 0xBE reserved

rom_start:
    @ IRQ mode stack, then system mode stack
    mov     r0, #0x12
    msr     cpsr_c, r0
    ldr     sp, =__sp_irq
    mov     r0, #0x1F
    msr     cpsr_c, r0
    ldr     sp, =__sp_usr

    @ copy initialised data (and IWRAM code) from ROM to IWRAM
    ldr     r0, =__data_lma
    ldr     r1, =__data_start
    ldr     r2, =__data_end
1:  cmp     r1, r2
    ldrlo   r3, [r0], #4
    strlo   r3, [r1], #4
    blo     1b

    @ clear .bss
    ldr     r1, =__bss_start
    ldr     r2, =__bss_end
    mov     r3, #0
2:  cmp     r1, r2
    strlo   r3, [r1], #4
    blo     2b

    ldr     r0, =main
    mov     lr, pc
    bx      r0
3:  b       3b
    .pool
