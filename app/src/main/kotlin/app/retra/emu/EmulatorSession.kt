package app.retra.emu

import android.os.Process
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

/** Receives finished video frames on the emulation thread (240x160 ARGB). */
fun interface FrameSink {
    fun onFrame(pixels: IntArray)
}

/** Persists battery saves; called on a background executor, never the emulation thread. */
fun interface BatterySaveWriter {
    fun write(data: ByteArray, firstWriteThisSession: Boolean)
}

/**
 * Runs a loaded [EmuCore] on a dedicated thread at 59.7275 Hz.
 *
 * Pacing is nanoTime based with drift correction: each frame's deadline is the previous deadline
 * plus one period (not "now + period"), nudged by ±0.5 % to keep the audio buffer half full; if the
 * thread falls more than a few frames behind it resynchronises instead of fast-forwarding to catch up.
 * All core access happens on that thread; other threads talk to it through [run] or volatile flags.
 */
class EmulatorSession(
    private val core: EmuCore,
    private val audio: AudioSink?,
    private val saveWriter: BatterySaveWriter,
) {
    @Volatile var frameSink: FrameSink? = null
    @Volatile var touchKeys = 0
    @Volatile var padKeys = 0
    @Volatile var fastForward = false
    @Volatile var fastForwardSpeed = 2
    @Volatile var fastForwardAudio = false
    @Volatile var rewinding = false
    @Volatile var rewindEnabled = true
    @Volatile var muted = false
    @Volatile private var paused = true
    @Volatile private var running = false

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _firstFrame = MutableStateFlow(false)
    val firstFrameShown: StateFlow<Boolean> = _firstFrame.asStateFlow()

    private val commands = LinkedBlockingQueue<Runnable>()
    private val video = IntArray(EmuCore.WIDTH * EmuCore.HEIGHT)
    private val audioBuf = ShortArray(EmuCore.MAX_AUDIO_FRAMES * 2)
    private val rewind = RewindBuffer(REWIND_CAPACITY)
    private val ioExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "retra-save-io") }
    private var thread: Thread? = null
    private var saveDirtySince = 0L
    private var wroteSaveThisSession = false
    private var frameCounter = 0L
    private var lastPausedState = true

    companion object {
        const val PERIOD_NANOS = (1_000_000_000.0 / EmuCore.FRAME_RATE).toLong()
        const val REWIND_INTERVAL = 10
        const val REWIND_CAPACITY = 60 // 60 states x 10 frames = ~10 s
        const val SAVE_DEBOUNCE_NANOS = 1_000_000_000L
    }

    fun start() {
        if (thread != null) return
        running = true
        thread = Thread({ loop() }, "retra-emulation").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    var isPaused: Boolean
        get() = paused
        set(value) {
            paused = value
            commands.offer(Runnable { }) // wake the loop
        }

    /** Runs [block] on the emulation thread and returns its result. Works while paused. */
    suspend fun <T> run(block: (EmuCore) -> T): T {
        val result = CompletableDeferred<T>()
        commands.offer(Runnable { result.completeWith(runCatching { block(core) }) })
        return result.await()
    }

    /** A copy of the most recent frame (emulation thread). */
    fun snapshotFrame(): IntArray = video.copyOf()

    fun clearRewind() = commands.offer(Runnable { rewind.clear() })

    /** Stops the thread, flushes the battery save, and releases the core. Blocks briefly. */
    fun stop() {
        if (!running) return
        running = false
        commands.offer(Runnable { })
        thread?.join(2000)
        thread = null
        ioExecutor.shutdown()
        runCatching { ioExecutor.awaitTermination(3, TimeUnit.SECONDS) }
        audio?.release()
        core.close()
    }

    private fun loop() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        var deadline = System.nanoTime()
        var fpsWindowStart = deadline
        var fpsFrames = 0
        var rewindTick = 0
        while (running) {
            while (true) commands.poll()?.run() ?: break
            if (paused) {
                if (!lastPausedState) {
                    lastPausedState = true
                    audio?.pause()
                    flushSave(force = true)
                    _fps.value = 0f
                }
                commands.poll(100, TimeUnit.MILLISECONDS)?.run()
                deadline = System.nanoTime()
                fpsWindowStart = deadline
                fpsFrames = 0
                continue
            }
            if (lastPausedState) {
                lastPausedState = false
                audio?.resume()
            }

            core.setKeys(touchKeys or padKeys)
            val ff = fastForward
            if (rewinding && rewindEnabled) {
                if (rewindTick++ % 2 == 0) rewind.pop()?.let { core.loadState(it) }
                core.runFrame(video, audioBuf)
            } else {
                val n = if (ff) fastForwardSpeed.coerceIn(2, 8) else 1
                val audible = !muted && (!ff || fastForwardAudio)
                for (i in 0 until n) {
                    val frames = core.runFrame(video, audioBuf)
                    if (i == 0 && audible) audio?.write(audioBuf, frames)
                    frameCounter++
                    if (rewindEnabled && frameCounter % REWIND_INTERVAL == 0L) rewind.push(core.saveState())
                }
            }
            frameSink?.onFrame(video)
            if (!_firstFrame.value) _firstFrame.value = true

            val now = System.nanoTime()
            if (core.consumeSaveDirty() && saveDirtySince == 0L) saveDirtySince = now
            if (saveDirtySince != 0L && now - saveDirtySince > SAVE_DEBOUNCE_NANOS) flushSave(force = false)

            fpsFrames++
            if (now - fpsWindowStart >= 1_000_000_000L) {
                _fps.value = fpsFrames * 1e9f / (now - fpsWindowStart)
                fpsWindowStart = now
                fpsFrames = 0
            }

            // Pacing with drift correction.
            val fill = audio?.fillLevel() ?: 0.5f
            val nudge = 1.0 + (fill - 0.5) * 0.01
            deadline += (PERIOD_NANOS * nudge).toLong()
            val wait = deadline - System.nanoTime()
            if (wait > 0) {
                LockSupport.parkNanos(wait)
            } else if (-wait > PERIOD_NANOS * 4) {
                deadline = System.nanoTime() // too far behind: resync instead of bursting
            }
        }
        flushSave(force = true)
    }

    /** Emulation thread only. */
    private fun flushSave(force: Boolean) {
        if (!force && saveDirtySince == 0L) return
        if (force && saveDirtySince == 0L && !core.consumeSaveDirty()) return
        saveDirtySince = 0L
        val data = core.batterySave() ?: return
        val first = !wroteSaveThisSession
        wroteSaveThisSession = true
        ioExecutor.execute { runCatching { saveWriter.write(data, first) } }
    }

    /** Forces the battery save to disk now (e.g. before leaving). */
    suspend fun flushBatterySave() = run { flushSave(force = true) }
}
