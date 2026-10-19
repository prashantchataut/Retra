package app.retra.core

/** Accumulates active play time; paused time is not counted. Clock is injectable for tests. */
class PlaytimeTracker(private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private var startedAt: Long? = null
    private var accumulated = 0L

    val isRunning get() = startedAt != null

    fun start() {
        if (startedAt == null) startedAt = clock()
    }

    fun pause() {
        val s = startedAt ?: return
        accumulated += (clock() - s).coerceAtLeast(0)
        startedAt = null
    }

    /** Returns time played since the last call and resets the counter (keeps running if running). */
    fun drain(): Long {
        val s = startedAt
        if (s != null) {
            val now = clock()
            accumulated += (now - s).coerceAtLeast(0)
            startedAt = now
        }
        val v = accumulated
        accumulated = 0
        return v
    }
}
