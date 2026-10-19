@ Minimal startup for Retra's own test ROMs (original code).
    .section .crt0, "ax"
    .global _start
    .arm
_start:
    b       1f
    .fill   156, 1, 0           @ logo area (not needed by emulators)
    .ascii  "RETRATEST\0\0\0"   @ title
    .ascii  "RTST"              @ game code
    .ascii  "00"
    .byte   0x96, 0, 0
    .fill   7, 1, 0
    .byte   0, 0
    .hword  0
1:  mov     r0, #0x12
    msr     cpsr_c, r0
    ldr     sp, =0x03007FA0
    mov     r0, #0x1F
    msr     cpsr_c, r0
    ldr     sp, =0x03007F00
    ldr     r0, =__data_lma
    ldr     r1, =__data_start
    ldr     r2, =__data_end
2:  cmp     r1, r2
    ldrlo   r3, [r0], #4
    strlo   r3, [r1], #4
    blo     2b
    ldr     r1, =__bss_start
    ldr     r2, =__bss_end
    mov     r3, #0
3:  cmp     r1, r2
    strlo   r3, [r1], #4
    blo     3b
    ldr     r0, =main
    mov     lr, pc
    bx      r0
4:  b       4b
    .pool
