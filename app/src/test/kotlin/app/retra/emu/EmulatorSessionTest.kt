package app.retra.emu

import app.retra.engine.LoadResult
import app.retra.engine.RomInfo
import app.retra.engine.SaveType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** A JVM fake of the native core: counts frames, "writes" to save memory on frame 30. */
class FakeEmuCore : EmuCore {
    val frames = AtomicInteger()
    var keys = 0
    var thread: Thread? = null
    private var dirty = false
    var closed = false
    override fun loadRom(rom: ByteArray, bios: ByteArray?): LoadResult {
        thread = Thread.currentThread()
        return LoadResult.Ok(RomInfo("FAKE", "FAKE", "01", 0, SaveType.SRAM, false))
    }
    override fun reset() { frames.set(0) }
    override fun setKeys(mask: Int) { keys = mask }
    override fun runFrame(video: IntArray, audio: ShortArray): Int {
        check(Thread.currentThread() == thread) { "core used from two threads" }
        val f = frames.incrementAndGet()
        video.fill(0xFF000000.toInt() or f)
        if (f == 30) dirty = true
        return 548
    }
    override fun batterySave(): ByteArray = byteArrayOf(frames.get().toByte())
    override fun loadBatterySave(data: ByteArray) = true
    override fun consumeSaveDirty(): Boolean = dirty.also { dirty = false }
    override fun saveState(): ByteArray = ByteArray(8) { frames.get().toByte() }
    override fun loadState(state: ByteArray): Boolean { frames.set(state[0].toInt()); return true }
    override fun setCheats(codes: List<String>) = codes.size
    override fun setColorCorrection(enabled: Boolean) {}
    override fun setRtcOffsetSeconds(offset: Long) {}
    override fun close() { closed = true }
}

class EmulatorSessionTest {
    @Test fun runsPacedFramesOnOneThreadAndDebouncesSaves() = runBlocking {
        val core = FakeEmuCore()
        val writes = CopyOnWriteArrayList<Pair<Int, Boolean>>()
        val session = EmulatorSession(core, null) { data, first -> writes += data[0].toInt() to first }
        session.start()
        session.run { it.loadRom(ByteArray(0), null) }
        session.touchKeys = 1
        session.isPaused = false
        Thread.sleep(1_000)
        val ran = core.frames.get()
        // ~59.7 frames per second, with generous slack for a busy CI machine.
        assertTrue("ran $ran frames", ran in 35..75)
        assertEquals(1, core.keys)
        // The write on frame 30 is flushed ~1 s later (debounce) or on pause.
        session.isPaused = true
        Thread.sleep(300)
        assertTrue("save written", writes.isNotEmpty())
        assertTrue("first write rotates backups", writes.first().second)
        val pausedAt = core.frames.get()
        Thread.sleep(200)
        assertEquals(pausedAt, core.frames.get())
        session.stop()
        assertTrue(core.closed)
    }

    @Test fun fastForwardRunsSeveralFramesPerTick() = runBlocking {
        val core = FakeEmuCore()
        val session = EmulatorSession(core, null) { _, _ -> }
        session.start()
        session.run { it.loadRom(ByteArray(0), null) }
        session.fastForward = true
        session.fastForwardSpeed = 4
        session.isPaused = false
        Thread.sleep(500)
        session.isPaused = true
        val ran = core.frames.get()
        assertTrue("ran $ran frames", ran > 80)
        session.stop()
    }

    @Test fun rewindGoesBack() = runBlocking {
        val core = FakeEmuCore()
        val session = EmulatorSession(core, null) { _, _ -> }
        session.start()
        session.run { it.loadRom(ByteArray(0), null) }
        session.isPaused = false
        Thread.sleep(700)
        val before = core.frames.get()
        session.rewinding = true
        Thread.sleep(300)
        session.isPaused = true
        assertTrue("rewound from $before to ${core.frames.get()}", core.frames.get() < before)
        session.stop()
    }
}
