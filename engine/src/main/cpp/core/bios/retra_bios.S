@ Retra replacement GBA BIOS.
@ Original work, written for the Retra emulator. No Nintendo code.
@
@ Exception vectors, IRQ dispatcher and SWI dispatcher are real ARM code (so that
@ games observe realistic register/stack behaviour and BIOS open-bus values).
@ Heavy SWI functions are implemented in C++ and reached through a trap opcode
@ from the permanently-undefined ARM encoding space (see cpu.h kHleTrapBase).
@
@ Build: engine/src/main/cpp/core/bios/build_bios.sh (needs arm-none-eabi-as)

    .arm
    .section .text
    .global _start

.macro TRAP n
    .word 0xE7F000F0 | ((\n) & 0xF) | (((\n) >> 4) << 8)
.endm

_start:
    b       reset_vec           @ 0x00 reset
    b       undef_vec           @ 0x04 undefined instruction
    b       swi_vec             @ 0x08 software interrupt
    b       abort_vec           @ 0x0C prefetch abort
    b       abort_vec           @ 0x10 data abort
    b       abort_vec           @ 0x14 reserved
    b       irq_vec             @ 0x18 IRQ
    b       fiq_vec             @ 0x1C FIQ

reset_vec:
    TRAP    0x100               @ HLE boot: sets up registers and jumps to the cartridge
    b       reset_vec

undef_vec:
    movs    pc, lr

abort_vec:
    subs    pc, lr, #4

fiq_vec:
    subs    pc, lr, #4

    .balign 16
irq_vec:
    stmfd   sp!, {r0-r3, r12, lr}
    mov     r0, #0x04000000
    add     lr, pc, #0
    ldr     pc, [r0, #-4]       @ user handler at [0x03FFFFFC]
    ldmfd   sp!, {r0-r3, r12, lr}
    subs    pc, lr, #4
    .word   0
    .word   0xE55EC002          @ value seen on the bus after an IRQ returned

    .balign 16
swi_vec:
    stmfd   sp!, {r11, r12, lr}
    ldrb    r12, [lr, #-2]
    adr     r11, swi_table
    cmp     r12, #0x2B
    movhs   r12, #0x2B
    ldr     r12, [r11, r12, lsl #2]
    mrs     r11, spsr
    stmfd   sp!, {r11}
    and     r11, r11, #0x80
    orr     r11, r11, #0x1F
    msr     cpsr_fc, r11        @ system mode, caller's IRQ disable bit
    stmfd   sp!, {r2, lr}
    mov     lr, pc
    bx      r12
    ldmfd   sp!, {r2, lr}
    mov     r12, #0xD3
    msr     cpsr_fc, r12
    ldmfd   sp!, {r11}
    msr     spsr_fc, r11
    ldmfd   sp!, {r11, r12, lr}
    movs    pc, lr
    .word   0
    .word   0xE3A02004          @ value seen on the bus after an SWI returned

swi_table:
    .word   hle_00, hle_01, f_halt, f_stop, f_intrwait, f_vblankintrwait, hle_06, hle_07
    .word   hle_08, hle_09, hle_0a, hle_0b, hle_0c, hle_0d, hle_0e, hle_0f
    .word   hle_10, hle_11, hle_12, hle_13, hle_14, hle_15, hle_16, hle_17
    .word   hle_18, hle_19, hle_1a, hle_1b, hle_1c, hle_1d, hle_1e, hle_1f
    .word   hle_20, hle_21, hle_22, hle_23, hle_24, hle_25, hle_26, f_customhalt
    .word   hle_28, hle_29, hle_2a, f_nop

f_nop:
    bx      lr

f_halt:
    mov     r12, #0x04000000
    mov     r2, #0
    strb    r2, [r12, #0x301]
    bx      lr

f_stop:
    mov     r12, #0x04000000
    mov     r2, #0x80
    strb    r2, [r12, #0x301]
    bx      lr

f_customhalt:
    mov     r12, #0x04000000
    strb    r2, [r12, #0x301]
    bx      lr

f_vblankintrwait:
    mov     r0, #1
    mov     r1, #1
f_intrwait:
    mov     r12, #0x04000000
    cmp     r0, #0
    beq     2f
    ldrh    r2, [r12, #-8]      @ discard old flags in BIOS IF (0x03FFFFF8)
    bic     r2, r2, r1
    strh    r2, [r12, #-8]
1:
    mov     r2, #0
    strb    r2, [r12, #0x301]   @ halt until an interrupt is requested
2:
    mov     r2, #0
    strb    r2, [r12, #0x208]   @ IME = 0
    ldrh    r2, [r12, #-8]
    ands    r0, r1, r2
    eorne   r2, r2, r0
    strneh  r2, [r12, #-8]
    mov     r2, #1
    strb    r2, [r12, #0x208]   @ IME = 1
    beq     1b
    bx      lr

.macro HLE name, n
\name:
    TRAP    \n
    bx      lr
.endm

    HLE hle_00, 0x00
    HLE hle_01, 0x01
    HLE hle_06, 0x06
    HLE hle_07, 0x07
    HLE hle_08, 0x08
    HLE hle_09, 0x09
    HLE hle_0a, 0x0A
    HLE hle_0b, 0x0B
    HLE hle_0c, 0x0C
    HLE hle_0d, 0x0D
    HLE hle_0e, 0x0E
    HLE hle_0f, 0x0F
    HLE hle_10, 0x10
    HLE hle_11, 0x11
    HLE hle_12, 0x12
    HLE hle_13, 0x13
    HLE hle_14, 0x14
    HLE hle_15, 0x15
    HLE hle_16, 0x16
    HLE hle_17, 0x17
    HLE hle_18, 0x18
    HLE hle_19, 0x19
    HLE hle_1a, 0x1A
    HLE hle_1b, 0x1B
    HLE hle_1c, 0x1C
    HLE hle_1d, 0x1D
    HLE hle_1e, 0x1E
    HLE hle_1f, 0x1F
    HLE hle_20, 0x20
    HLE hle_21, 0x21
    HLE hle_22, 0x22
    HLE hle_23, 0x23
    HLE hle_24, 0x24
    HLE hle_25, 0x25
    HLE hle_26, 0x26
    HLE hle_28, 0x28
    HLE hle_29, 0x29
    HLE hle_2a, 0x2A

    .balign 4
    .ascii  "Retra replacement BIOS - original work"
    .balign 4
