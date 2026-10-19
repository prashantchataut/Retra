package app.retra.emu

import app.retra.engine.GbaCore
import app.retra.engine.LoadResult

/**
 * The emulator as the app sees it. [NativeEmuCore] wraps the real engine; tests use a fake,
 * because the native library can't load on the JVM.
 */
interface EmuCore : AutoCloseable {
    fun loadRom(rom: ByteArray, bios: ByteArray?): LoadResult
    fun reset()
    fun setKeys(mask: Int)
    fun runFrame(video: IntArray, audio: ShortArray): Int
    fun batterySave(): ByteArray?
    fun loadBatterySave(data: ByteArray): Boolean
    fun consumeSaveDirty(): Boolean
    fun saveState(): ByteArray
    fun loadState(state: ByteArray): Boolean
    fun setCheats(codes: List<String>): Int
    fun setColorCorrection(enabled: Boolean)
    fun setRtcOffsetSeconds(offset: Long)

    companion object {
        const val WIDTH = 240
        const val HEIGHT = 160
        const val SAMPLE_RATE = 32768
        const val FRAME_RATE = 59.7275
        const val MAX_AUDIO_FRAMES = 2048
    }
}

class NativeEmuCore : EmuCore {
    private val core = GbaCore()
    override fun loadRom(rom: ByteArray, bios: ByteArray?) = core.loadRom(rom, bios)
    override fun reset() = core.reset()
    override fun setKeys(mask: Int) = core.setKeys(mask)
    override fun runFrame(video: IntArray, audio: ShortArray) = core.runFrame(video, audio)
    override fun batterySave() = core.batterySave()
    override fun loadBatterySave(data: ByteArray) = core.loadBatterySave(data)
    override fun consumeSaveDirty() = core.consumeSaveDirty()
    override fun saveState() = core.saveState()
    override fun loadState(state: ByteArray) = core.loadState(state)
    override fun setCheats(codes: List<String>) = core.setCheats(codes)
    override fun setColorCorrection(enabled: Boolean) = core.setColorCorrection(enabled)
    override fun setRtcOffsetSeconds(offset: Long) = core.setRtcOffsetSeconds(offset)
    override fun close() = core.close()
}
