package app.retra.engine

/**
 * Cartridge backup memory type, detected from the library ID strings in the ROM.
 *
 * [AUTO] is reported for EEPROM games whose size (512 B or 8 KiB) is only known once the game
 * first touches the EEPROM; the core sizes it automatically.
 */
enum class SaveType { NONE, SRAM, FLASH_64K, FLASH_128K, EEPROM_512, EEPROM_8K, AUTO }
