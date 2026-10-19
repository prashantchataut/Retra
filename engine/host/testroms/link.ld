OUTPUT_FORMAT("elf32-littlearm")
OUTPUT_ARCH(arm)
ENTRY(_start)
MEMORY {
    rom   : ORIGIN = 0x08000000, LENGTH = 32M
    iwram : ORIGIN = 0x03000000, LENGTH = 32K - 0x200
}
SECTIONS {
    .text : { KEEP(*(.crt0)) *(.text .text.*) *(.glue_7 .glue_7t .v4_bx) . = ALIGN(4); *(.rodata .rodata.*) . = ALIGN(4); } > rom
    .data : { . = ALIGN(4); __data_start = .; *(.iwram .iwram.*) *(.data .data.*) . = ALIGN(4); __data_end = .; } > iwram AT > rom
    __data_lma = LOADADDR(.data);
    .bss (NOLOAD) : { . = ALIGN(4); __bss_start = .; *(.bss .bss.*) *(COMMON) . = ALIGN(4); __bss_end = .; } > iwram
    /DISCARD/ : { *(.comment) *(.note*) *(.ARM.exidx*) }
}
