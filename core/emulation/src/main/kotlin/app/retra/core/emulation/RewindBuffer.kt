package app.retra.core.emulation

import java.util.ArrayDeque

/**
 * Thread-safe, bounded in-memory rewind history.
 *
 * Snapshots are copied on both insertion and retrieval so native serialization buffers cannot be
 * mutated after capture. The latest snapshot is retained as the current rewind anchor; rewinding
 * N steps discards N newer anchors and returns a copy of the resulting latest state.
 */
class RewindBuffer(
    private val maximumBytes: Int
) {
    init {
        require(maximumBytes > 0) { "Rewind memory budget must be positive." }
    }

    private val lock = Any()
    private val snapshots = ArrayDeque<ByteArray>()
    private var storedBytes = 0

    val snapshotCount: Int
        get() = synchronized(lock) { snapshots.size }

    val byteCount: Int
        get() = synchronized(lock) { storedBytes }

    fun push(snapshot: ByteArray): Boolean {
        if (snapshot.isEmpty() || snapshot.size > maximumBytes) return false
        val owned = snapshot.copyOf()
        synchronized(lock) {
            snapshots.addLast(owned)
            storedBytes += owned.size
            while (storedBytes > maximumBytes && snapshots.size > 1) {
                storedBytes -= snapshots.removeFirst().size
            }
            if (storedBytes > maximumBytes) {
                storedBytes -= snapshots.removeLast().size
                return false
            }
            return true
        }
    }

    fun rewind(steps: Int = 1): ByteArray = synchronized(lock) {
        require(steps > 0) { "Rewind steps must be positive." }
        require(snapshots.size >= steps + 1) { "Not enough rewind history yet." }
        repeat(steps) {
            storedBytes -= snapshots.removeLast().size
        }
        snapshots.last().copyOf()
    }

    fun clear() = synchronized(lock) {
        snapshots.clear()
        storedBytes = 0
    }
}
