package app.retra.patcher

import java.io.ByteArrayOutputStream
import kotlin.random.Random

object TestUtil {
    fun resource(name: String): ByteArray =
        TestUtil::class.java.getResourceAsStream("/$name")?.use { it.readBytes() } ?: error("missing resource $name")

    fun random(size: Int, seed: Int): ByteArray = Random(seed).nextBytes(size)

    /** Mutates a copy of [src]: random byte edits, an inserted block, a run, and optional growth. */
    fun mutate(src: ByteArray, seed: Int, newSize: Int = src.size, edits: Int = 200): ByteArray {
        val r = Random(seed)
        val t = src.copyOf(newSize)
        if (newSize > src.size) {
            // Fill growth area with a mix of copied source and new data.
            var p = src.size
            while (p < newSize) {
                val len = minOf(newSize - p, r.nextInt(1, 70000))
                if (r.nextBoolean() && src.size > len) System.arraycopy(src, r.nextInt(src.size - len), t, p, len)
                else for (i in 0 until len) t[p + i] = r.nextInt().toByte()
                p += len
            }
        }
        repeat(edits) {
            val p = r.nextInt(newSize)
            val len = minOf(newSize - p, r.nextInt(1, 64))
            for (i in 0 until len) t[p + i] = r.nextInt().toByte()
        }
        if (newSize > 300) java.util.Arrays.fill(t, newSize / 3, newSize / 3 + minOf(200, newSize / 3), 0x5A)
        return t
    }

    fun ips(source: ByteArray, target: ByteArray, forceTruncate: Boolean = false): ByteArray {
        val o = ByteArrayOutputStream()
        o.write("PATCH".toByteArray())
        val n = target.size
        var i = 0
        fun u24(v: Int) { o.write(v ushr 16); o.write(v ushr 8 and 0xFF); o.write(v and 0xFF) }
        fun u16(v: Int) { o.write(v ushr 8); o.write(v and 0xFF) }
        fun differs(k: Int) = k >= source.size || source[k] != target[k]
        while (i < n) {
            if (!differs(i)) { i++; continue }
            var start = i
            if (start == 0x454F46) start-- // avoid the "EOF" offset
            var end = i
            while (end < n && end - start < 0xFFFF && (differs(end) || (end + 1 < n && differs(end + 1)))) end++
            // RLE if the whole chunk is one value and long enough
            val v = target[start]
            if (end - start >= 8 && (start until end).all { target[it] == v }) {
                u24(start); u16(0); u16(end - start); o.write(v.toInt() and 0xFF)
            } else {
                u24(start); u16(end - start); o.write(target, start, end - start)
            }
            i = end
        }
        o.write("EOF".toByteArray())
        if (forceTruncate || n < source.size) u24(n)
        return o.toByteArray()
    }

    private fun ByteArrayOutputStream.beatVarint(v: Long) {
        var data = v
        while (true) {
            val x = (data and 0x7F).toInt()
            data = data ushr 7
            if (data == 0L) { write(0x80 or x); return }
            write(x)
            data--
        }
    }

    private fun ByteArrayOutputStream.le32(v: Long) { for (k in 0 until 4) write(((v ushr (8 * k)) and 0xFF).toInt()) }

    fun ups(source: ByteArray, target: ByteArray): ByteArray {
        val o = ByteArrayOutputStream()
        o.write("UPS1".toByteArray())
        o.beatVarint(source.size.toLong()); o.beatVarint(target.size.toLong())
        val max = maxOf(source.size, target.size)
        fun x(k: Int): Int = ((if (k < source.size) source[k].toInt() else 0) xor (if (k < target.size) target[k].toInt() else 0)) and 0xFF
        var i = 0
        var rel = 0
        while (i < max) {
            if (x(i) == 0) { i++; continue }
            o.beatVarint((i - rel).toLong())
            while (true) {
                if (i >= max) { o.write(0); i++; break }
                val v = x(i); o.write(v); i++
                if (v == 0) break
            }
            rel = i
        }
        o.le32(Checksums.crc32(source)); o.le32(Checksums.crc32(target))
        val body = o.toByteArray()
        o.le32(Checksums.crc32(body))
        return o.toByteArray()
    }

    fun ok(r: PatchResult): PatchResult.Ok {
        if (r is PatchResult.Error) throw AssertionError("expected Ok, got $r")
        return r as PatchResult.Ok
    }

    fun err(r: PatchResult): PatchResult.Error {
        if (r is PatchResult.Ok) throw AssertionError("expected Error, got $r")
        return r as PatchResult.Error
    }
}
