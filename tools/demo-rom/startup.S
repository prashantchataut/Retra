.syntax unified
.cpu arm7tdmi
.arm
.section .text.startup,"ax",%progbits
.global _start
.type _start,%function
_start:
    b reset
    .space 0x9C
    .ascii "RETRA DRIFT "      @ 12-byte title
    .ascii "RDRT"              @ game code
    .ascii "RT"                @ maker code
    .byte 0x96                  @ fixed value
    .byte 0x00                  @ main unit code
    .byte 0x00                  @ device type
    .space 7                    @ reserved
    .byte 0x00                  @ software version
    .byte 0x00                  @ header checksum, fixed by build script
    .byte 0x00, 0x00            @ reserved

reset:
    ldr sp, =0x03007F00
    bl main
1:  b 1b
.size _start, .-_start
