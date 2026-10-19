package app.retra.patcher

import java.io.ByteArrayOutputStream

/**
 * Creates BPS patches (greedy, hash-based matcher: linear time, good enough for ROM hacks).
 */
object BpsCreator {
    private const val MIN_MATCH = 4
    private const val HASH_BITS = 20

    fun create(source: ByteArray, target: ByteArray, metadata: String? = null): ByteArray {
        val out = Out(target.size / 4 + 64)
        out.ascii("BPS1")
        out.varint(source.size.toLong())
        out.varint(target.size.toLong())
        val meta = metadata?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        out.varint(meta.size.toLong())
        out.write(meta, 0, meta.size)

        val mask = (1 shl HASH_BITS) - 1
        val srcTable = IntArray(1 shl HASH_BITS) { -1 }
        for (i in 0..source.size - MIN_MATCH) srcTable[hash(source, i) and mask] = i
        val tgtTable = IntArray(1 shl HASH_BITS) { -1 }

        val n = target.size
        var i = 0
        var pendingStart = -1
        var srcRel = 0L
        var tgtRel = 0L
        var tgtHashed = 0 // target positions below this are in tgtTable

        fun flushPending(upTo: Int) {
            if (pendingStart < 0) return
            val len = upTo - pendingStart
            out.varint(((len - 1).toLong() shl 2) or 1L)
            out.write(target, pendingStart, len)
            pendingStart = -1
        }

        while (i < n) {
            // Candidate 1: SourceRead (same offset).
            var readLen = 0
            while (i + readLen < n && i + readLen < source.size && source[i + readLen] == target[i + readLen]) readLen++

            var srcLen = 0
            var srcPos = 0
            var tgtLen = 0
            var tgtPos = 0
            if (i + MIN_MATCH <= n) {
                val h = hash(target, i) and mask
                val s = srcTable[h]
                if (s >= 0 && s != i) {
                    var k = 0
                    while (i + k < n && s + k < source.size && source[s + k] == target[i + k]) k++
                    srcLen = k; srcPos = s
                }
                val t = tgtTable[h]
                if (t >= 0) {
                    var k = 0
                    while (i + k < n && target[t + k] == target[i + k]) k++
                    tgtLen = k; tgtPos = t
                }
            }

            val best = maxOf(readLen, srcLen, tgtLen)
            if (best < MIN_MATCH && !(readLen > 0 && readLen == n - i)) {
                if (pendingStart < 0) pendingStart = i
                i++
            } else {
                flushPending(i)
                val len: Int
                if (readLen >= srcLen && readLen >= tgtLen) {
                    len = readLen
                    out.varint((len - 1).toLong() shl 2)
                } else if (srcLen >= tgtLen) {
                    len = srcLen
                    out.varint(((len - 1).toLong() shl 2) or 2L)
                    val d = srcPos - srcRel
                    out.varint(if (d < 0) ((-d) shl 1) or 1L else d shl 1)
                    srcRel = srcPos.toLong() + len
                } else {
                    len = tgtLen
                    out.varint(((len - 1).toLong() shl 2) or 3L)
                    val d = tgtPos - tgtRel
                    out.varint(if (d < 0) ((-d) shl 1) or 1L else d shl 1)
                    tgtRel = tgtPos.toLong() + len
                }
                i += len
            }
            // Index target positions we've moved past (limit work inside long matches).
            val step = if (i - tgtHashed > 4096) 16 else 1
            while (tgtHashed + MIN_MATCH <= minOf(i, n)) {
                tgtTable[hash(target, tgtHashed) and mask] = tgtHashed
                tgtHashed += step
            }
            tgtHashed = maxOf(tgtHashed, i - MIN_MATCH + 1).coerceAtLeast(0)
        }
        flushPending(n)

        out.le32(Checksums.crc32(source))
        out.le32(Checksums.crc32(target))
        out.le32(Checksums.crc32(out.buf(), 0, out.size()))
        return out.toByteArray()
    }

    private fun hash(b: ByteArray, i: Int): Int {
        val v = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8) or
            ((b[i + 2].toInt() and 0xFF) shl 16) or ((b[i + 3].toInt() and 0xFF) shl 24)
        return (v * -0x61c88647) ushr (32 - HASH_BITS)
    }

    private class Out(cap: Int) : ByteArrayOutputStream(cap) {
        fun buf(): ByteArray = buf

        fun ascii(s: String) = s.forEach { write(it.code) }

        fun varint(v: Long) {
            var data = v
            while (true) {
                val x = (data and 0x7F).toInt()
                data = data ushr 7
                if (data == 0L) {
                    write(0x80 or x); return
                }
                write(x)
                data--
            }
        }

        fun le32(v: Long) {
            for (k in 0 until 4) write(((v ushr (8 * k)) and 0xFF).toInt())
        }
    }
}
