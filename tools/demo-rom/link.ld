ENTRY(_start)
SECTIONS
{
  . = 0x08000000;
  .text : { KEEP(*(.text.startup)) *(.text*) *(.rodata*) }
  .data : { *(.data*) }
  .bss : { *(.bss*) *(COMMON) }
  /DISCARD/ : { *(.ARM.exidx*) *(.ARM.extab*) *(.comment*) *(.note*) }
}
