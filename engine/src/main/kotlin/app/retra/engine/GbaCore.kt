package app.retra.engine

/**
 * Game Boy Advance emulator core (native, `libretra_engine.so`).
 *
 * NOT thread-safe: create it, load ROMs, run frames and query it from a single emulation thread.
 * Call [close] when done to release native memory (~35 MiB while a ROM is loaded).
 */
class GbaCore : AutoCloseable {

    companion object {
        const val WIDTH = 240
        const val HEIGHT = 160
        const val SAMPLE_RATE = 32768
        const val FRAME_RATE = 59.7275
        const val MAX_AUDIO_FRAMES = 2048

        init {
            System.loadLibrary("retra_engine")
        }

        @JvmStatic private external fun nativeCreate(): Long
        @JvmStatic private external fun nativeDestroy(handle: Long)
        @JvmStatic private external fun nativeLoadRom(handle: Long, rom: ByteArray, bios: ByteArray?): String?
        @JvmStatic private external fun nativeRomInfo(handle: Long): Array<String>?
        @JvmStatic private external fun nativeEepromSizeUnknown(handle: Long): Boolean
        @JvmStatic private external fun nativeReset(handle: Long)
        @JvmStatic private external fun nativeSetKeys(handle: Long, mask: Int)
        @JvmStatic private external fun nativeRunFrame(handle: Long, video: IntArray, audio: ShortArray): Int
        @JvmStatic private external fun nativeBatterySave(handle: Long): ByteArray?
        @JvmStatic private external fun nativeLoadBatterySave(handle: Long, data: ByteArray): Boolean
        @JvmStatic private external fun nativeConsumeSaveDirty(handle: Long): Boolean
        @JvmStatic private external fun nativeSaveState(handle: Long): ByteArray
        @JvmStatic private external fun nativeLoadState(handle: Long, state: ByteArray): Boolean
        @JvmStatic private external fun nativeSetCheats(handle: Long, codes: Array<String>): Int
        @JvmStatic private external fun nativeSetColorCorrection(handle: Long, enabled: Boolean)
        @JvmStatic private external fun nativeSetRtcOffsetSeconds(handle: Long, offset: Long)
    }

    private var handle: Long = nativeCreate()
    private var romLoaded = false

    init {
        check(handle != 0L) { "Unable to allocate the emulator core" }
    }

    private fun requireOpen(): Long {
        check(handle != 0L) { "GbaCore is closed" }
        return handle
    }

    /** Loads a ROM. [bios] null -> built-in replacement BIOS; otherwise a 16 KiB BIOS image. */
    fun loadRom(rom: ByteArray, bios: ByteArray? = null): LoadResult {
        val h = requireOpen()
        val error = nativeLoadRom(h, rom, bios)
        if (error != null) {
            romLoaded = false
            return LoadResult.Error(error)
        }
        romLoaded = true
        val f = nativeRomInfo(h) ?: return LoadResult.Error("ROM information unavailable")
        val saveOrdinal = f[4].toIntOrNull() ?: 0
        var saveType = SaveType.entries.getOrElse(saveOrdinal) { SaveType.NONE }
        if ((saveType == SaveType.EEPROM_8K || saveType == SaveType.EEPROM_512) && nativeEepromSizeUnknown(h)) {
            saveType = SaveType.AUTO
        }
        return LoadResult.Ok(
            RomInfo(
                title = f[0],
                gameCode = f[1],
                makerCode = f[2],
                version = f[3].toIntOrNull() ?: 0,
                saveType = saveType,
                hasRtc = f[5] == "1",
            ),
        )
    }

    /** Power-cycles the console (battery save contents are kept). */
    fun reset() = nativeReset(requireOpen())

    /** Sets the pressed buttons ([GbaKey] bits, 1 = pressed). */
    fun setKeys(mask: Int) = nativeSetKeys(requireOpen(), mask)

    /**
     * Emulates one video frame.
     * @param video at least 240*160 pixels, receives ARGB_8888 (0xFFRRGGBB)
     * @param audio interleaved stereo s16, size >= 2 * [MAX_AUDIO_FRAMES]
     * @return number of stereo audio frames written (about 548-549 per video frame)
     */
    fun runFrame(video: IntArray, audio: ShortArray): Int {
        require(video.size >= WIDTH * HEIGHT) { "video buffer must hold ${WIDTH * HEIGHT} pixels" }
        require(audio.size >= 2 * MAX_AUDIO_FRAMES) { "audio buffer must hold ${2 * MAX_AUDIO_FRAMES} samples" }
        val h = requireOpen()
        if (!romLoaded) return 0
        return nativeRunFrame(h, video, audio)
    }

    /** Current battery save contents, or null when the game has no backup memory. */
    fun batterySave(): ByteArray? = nativeBatterySave(requireOpen())

    /** Imports a battery save (.sav). Returns false if the game has no backup memory. */
    fun loadBatterySave(data: ByteArray): Boolean = nativeLoadBatterySave(requireOpen(), data)

    /** True once after the game wrote to its backup memory (persist [batterySave] then). */
    fun consumeSaveDirty(): Boolean = nativeConsumeSaveDirty(requireOpen())

    /** Serializes the complete machine state (versioned format). */
    fun saveState(): ByteArray = nativeSaveState(requireOpen())

    /** Restores a state produced by [saveState] for the same game. */
    fun loadState(state: ByteArray): Boolean = nativeLoadState(requireOpen(), state)

    /**
     * Replaces the active cheat list. Supports raw "AAAAAAAA:VV/VVVV/VVVVVVVV", CodeBreaker
     * "XXXXXXXX YYYY", and encrypted GameShark v1/v2 / Action Replay v3 "XXXXXXXX YYYYYYYY"
     * (auto-detected; prefix "GS:" or "AR:" to force). An entry may hold several lines.
     * @return number of accepted code lines
     */
    fun setCheats(codes: List<String>): Int = nativeSetCheats(requireOpen(), codes.toTypedArray())

    /** Enables an approximation of the GBA LCD's gamma/colour response. */
    fun setColorCorrection(enabled: Boolean) = nativeSetColorCorrection(requireOpen(), enabled)

    /** Offset added to the host clock for the cartridge real-time clock. */
    fun setRtcOffsetSeconds(offset: Long) = nativeSetRtcOffsetSeconds(requireOpen(), offset)

    override fun close() {
        val h = handle
        if (h != 0L) {
            handle = 0L
            romLoaded = false
            nativeDestroy(h)
        }
    }
}
