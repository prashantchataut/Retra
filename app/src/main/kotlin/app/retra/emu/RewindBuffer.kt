package app.retra.emu

/** Fixed-capacity ring of save states, newest last. Not thread-safe (emulation thread only). */
class RewindBuffer(val capacity: Int) {
    private val items = arrayOfNulls<ByteArray>(capacity)
    private var head = 0 // next write position
    var size = 0
        private set

    fun push(state: ByteArray) {
        items[head] = state
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /** Removes and returns the newest state. */
    fun pop(): ByteArray? {
        if (size == 0) return null
        head = (head - 1 + capacity) % capacity
        val s = items[head]
        items[head] = null
        size--
        return s
    }

    fun clear() {
        items.fill(null)
        head = 0
        size = 0
    }
}
