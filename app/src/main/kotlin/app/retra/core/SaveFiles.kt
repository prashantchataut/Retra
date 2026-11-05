package app.retra.core

/**
 * Makes battery saves from other emulators and flash carts fit the game they're loaded into.
 *
 * Real GBA saves are one of five sizes. Other tools often pad (a 32 KB SRAM written as 64 KB or 128 KB), append a
 * 16-byte real-time-clock footer, or write a smaller file for a game that has a bigger chip. Anything we can't
 * make sense of is rejected with a message instead of being written over the player's progress.
 */
object SaveFiles {
    const val EEPROM_512 = 512
    const val EEPROM_8K = 8 * 1024
    const val SRAM = 32 * 1024
    const val FLASH_64K = 64 * 1024
    const val FLASH_128K = 128 * 1024
    private const val RTC_FOOTER = 16

    val validSizes = setOf(EEPROM_512, EEPROM_8K, SRAM, FLASH_64K, FLASH_128K)

    /** File extensions that hold a GBA battery save. */
    val extensions = setOf("sav", "srm", "sa1", "save", "dsv")

    sealed interface Result {
        data class Ok(val data: ByteArray, val note: String? = null) : Result
        data class Error(val message: String) : Result
    }

    /** The byte size a save has for the engine's [saveType] name, or null when unknown (AUTO / NONE). */
    fun expectedSize(saveType: String?): Int? = when (saveType?.uppercase()) {
        "SRAM" -> SRAM
        "FLASH_64K" -> FLASH_64K
        "FLASH_128K" -> FLASH_128K
        "EEPROM_512" -> EEPROM_512
        "EEPROM_8K" -> EEPROM_8K
        else -> null
    }

    fun normalize(input: ByteArray, saveType: String?): Result {
        if (input.isEmpty()) return Result.Error("That file is empty.")
        val expected = expectedSize(saveType)
        if (saveType.equals("NONE", true)) return Result.Error("This game doesn't keep a battery save.")

        var data = input
        var note: String? = null
        // Drop an RTC footer (mGBA/VBA append 16 bytes) when what's left is a real save size.
        if (data.size !in validSizes && (data.size - RTC_FOOTER) in validSizes) {
            data = data.copyOf(data.size - RTC_FOOTER)
            note = "Dropped a clock footer."
        }
        if (data.size > FLASH_128K * 2) return Result.Error("That file is too big to be a GBA save (largest is 128 KB).")

        if (expected == null) {
            return if (data.size in validSizes) Result.Ok(data, note)
            else Result.Error("That file isn't a GBA save (saves are 512 B, 8 KB, 32 KB, 64 KB or 128 KB; this one is ${describe(data.size)}).")
        }
        return when {
            data.size == expected -> Result.Ok(data, note)
            data.size > expected -> {
                val extra = data.copyOfRange(expected, data.size)
                if (extra.all { it == 0.toByte() } || extra.all { it == 0xFF.toByte() } || data.size in validSizes && looksPadded(data, expected))
                    Result.Ok(data.copyOf(expected), "Trimmed padding (${describe(data.size)} to ${describe(expected)}).")
                else Result.Error("This save is ${describe(data.size)} but the game uses ${describe(expected)}. It looks like it belongs to a different game.")
            }
            data.size in validSizes -> Result.Ok(data.copyOf(expected).also { java.util.Arrays.fill(it, data.size, expected, 0xFF.toByte()) }, "Padded to ${describe(expected)}.")
            else -> Result.Error("That file isn't a GBA save (${describe(data.size)}).")
        }
    }

    private fun looksPadded(data: ByteArray, expected: Int): Boolean {
        // A save written with its data mirrored (some SRAM dumps) repeats itself exactly.
        if (data.size % expected != 0) return false
        for (i in expected until data.size) if (data[i] != data[i % expected]) return false
        return true
    }

    fun describe(size: Int): String = when {
        size >= 1024 && size % 1024 == 0 -> "${size / 1024} KB"
        else -> "$size bytes"
    }

    fun isSaveName(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in extensions
}

/** What a scanned folder holds, and which save belongs to which cartridge. */
object FolderPlan {
    data class Entry(val name: String, val uri: String, val size: Long)

    data class Plan(
        val roms: List<Entry>,
        val saves: List<Entry>,
        val patches: List<Entry>,
        /** ROM entry -> the save sitting next to it (same name, different extension). */
        val pairs: Map<Entry, Entry>,
        val unpairedSaves: List<Entry>,
    )

    val romExtensions = setOf("gba", "agb", "bin", "zip")
    val patchExtensions = setOf("ips", "ups", "bps", "xdelta", "vcdiff", "xdelta3")

    private fun base(name: String) = name.substringBeforeLast('.').lowercase().replace(Regex("[^a-z0-9]+"), "")

    fun plan(files: List<Entry>): Plan {
        val roms = ArrayList<Entry>()
        val saves = ArrayList<Entry>()
        val patches = ArrayList<Entry>()
        for (f in files) {
            val ext = f.name.substringAfterLast('.', "").lowercase()
            when {
                ext in SaveFiles.extensions -> saves += f
                ext in patchExtensions -> patches += f
                ext in romExtensions && f.size in 1..(64L * 1024 * 1024) -> roms += f
            }
        }
        val pairs = LinkedHashMap<Entry, Entry>()
        val used = HashSet<Entry>()
        for (rom in roms) {
            val key = base(rom.name)
            val match = saves.firstOrNull { it !in used && base(it.name) == key }
                ?: saves.firstOrNull { it !in used && key.isNotEmpty() && (base(it.name).startsWith(key) || key.startsWith(base(it.name))) && base(it.name).length >= 4 }
            if (match != null) {
                pairs[rom] = match
                used += match
            }
        }
        return Plan(roms, saves, patches, pairs, saves.filter { it !in used })
    }
}
