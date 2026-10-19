package app.retra.patcher

/**
 * Minimal .xz (LZMA2-only) decoder for xdelta3's "lzma" secondary compression.
 *
 * xdelta3 keeps one liblzma stream per section type (data/inst/addr) for the whole patch and
 * flushes it with LZMA_SYNC_FLUSH after every window. So the first window's section starts with an
 * xz stream + block header, later windows just continue the LZMA2 chunk sequence (and may refer to
 * the dictionary built by earlier windows), and the stream is never finished (no index/footer).
 * Each section is prefixed (by xdelta) with its decompressed size.
 */
internal class XzSectionStream {
    private var lz: Lzma2Decoder? = null
    private var checkSize = 0
    private var dictSize = 1 shl 20
    private var streamStart = 0

    private fun corrupt(detail: String) = PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt("xdelta", "compressed section: $detail"))

    /** Decodes [outSize] bytes from inp[off, off+len); returns (buffer, offset of the decoded bytes). */
    fun decode(inp: ByteArray, off: Int, len: Int, outSize: Int): Pair<ByteArray, Int> {
        val end = off + len
        var p = off
        if (len >= 6 && isMagic(inp, off)) {
            // (Re)start: some xdelta builds may begin a fresh stream per window.
            if (len < 12) throw corrupt("too short")
            if (inp[off + 6].toInt() != 0) throw corrupt("bad xz flags")
            checkSize = when (inp[off + 7].toInt() and 0x0F) {
                0 -> 0
                in 1..3 -> 4
                in 4..6 -> 8
                in 7..9 -> 16
                in 10..12 -> 32
                else -> 64
            }
            streamStart = off
            p = blockHeader(inp, off + 12, end)
            lz = Lzma2Decoder(inp)
        }
        val d = lz ?: throw corrupt("missing xz header")
        d.reserve(outSize, dictSize)
        val startPos = d.outPos
        val target = startPos + outSize
        while (d.outPos < target) {
            p = d.decode(p, end, target)
            if (d.outPos < target && d.ended) {
                // Block finished: skip padding + check, then expect another block.
                while ((p - streamStart) and 3 != 0) p++
                p += checkSize
                p = blockHeader(inp, p, end)
                d.ended = false
            }
        }
        return d.buffer to startPos
    }

    private fun isMagic(b: ByteArray, o: Int) = b[o] == 0xFD.toByte() && b[o + 1] == '7'.code.toByte() &&
        b[o + 2] == 'z'.code.toByte() && b[o + 3] == 'X'.code.toByte() && b[o + 4] == 'Z'.code.toByte() && b[o + 5].toInt() == 0

    /** Parses a block header at [p]; returns the position of the compressed data. */
    private fun blockHeader(inp: ByteArray, p: Int, end: Int): Int {
        if (p >= end) throw corrupt("truncated")
        val hb = inp[p].toInt() and 0xFF
        if (hb == 0) throw corrupt("stream ended early")
        val hSize = (hb + 1) * 4
        if (p + hSize > end) throw corrupt("truncated block header")
        val flags = inp[p + 1].toInt() and 0xFF
        val filters = (flags and 3) + 1
        var q = p + 2
        fun vli(): Long {
            var r = 0L
            var shift = 0
            while (true) {
                if (q >= p + hSize || shift > 63) throw corrupt("bad block header")
                val b = inp[q++].toInt() and 0xFF
                r = r or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return r
                shift += 7
            }
        }
        if (flags and 0x40 != 0) vli()
        if (flags and 0x80 != 0) vli()
        for (i in 0 until filters) {
            val id = vli()
            val propSize = vli()
            if (id != 0x21L || filters != 1) {
                throw PatchException(PatchError.UNSUPPORTED, "This xdelta patch uses an unsupported xz filter (0x${id.toString(16)}).")
            }
            if (propSize != 1L || q >= p + hSize) throw corrupt("bad block header")
            val b = inp[q++].toInt() and 0xFF
            if (b > 40) throw corrupt("bad dictionary size")
            dictSize = if (b == 40) Int.MAX_VALUE else (2 or (b and 1)) shl (b / 2 + 11)
        }
        return p + hSize
    }
}

internal class Lzma2Decoder(private val inp: ByteArray) {
    private var out = ByteArray(0)
    val buffer: ByteArray get() = out
    var outPos = 0
        private set

    /** Set when an LZMA2 end marker was read. */
    var ended = false

    /** Makes room for [n] more output bytes, keeping at most [keep] bytes of history. */
    fun reserve(n: Int, keep: Int) {
        if (outPos.toLong() + n <= out.size) return
        val keepLen = minOf(outPos, keep)
        val shift = outPos - keepLen
        val nb = ByteArray(keepLen + n)
        System.arraycopy(out, shift, nb, 0, keepLen)
        out = nb
        outPos = keepLen
        dictStart = maxOf(0, dictStart - shift)
    }

    private var ip = 0
    private var ipEnd = 0
    private var range = 0
    private var code = 0

    private var lc = 0
    private var lp = 0
    private var pb = 0
    private var state = 0
    private var rep0 = 0
    private var rep1 = 0
    private var rep2 = 0
    private var rep3 = 0
    private var dictStart = 0
    private var pending = 0
    private var needDictReset = true
    private var needProps = true

    private val probs = ShortArray(TOTAL)

    private fun corrupt(detail: String) = PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt("xdelta", "LZMA data: $detail"))

    /**
     * Decodes LZMA2 chunks starting at [start] until the end marker or until [target] output bytes
     * exist; returns the input position after the last consumed chunk.
     */
    fun decode(start: Int, limit: Int, target: Int): Int {
        var p = start
        while (outPos < target) {
            if (p >= limit) throw corrupt("truncated")
            val c = inp[p++].toInt() and 0xFF
            if (c == 0) {
                ended = true; needDictReset = true; needProps = true
                return p
            }
            if (c == 1 || c == 2) {
                if (c == 1) {
                    dictStart = outPos; needDictReset = false
                } else if (needDictReset) throw corrupt("missing dictionary reset")
                if (p + 2 > limit) throw corrupt("truncated")
                val size = (((inp[p].toInt() and 0xFF) shl 8) or (inp[p + 1].toInt() and 0xFF)) + 1
                p += 2
                if (p + size > limit || outPos + size > target) throw corrupt("bad chunk size")
                System.arraycopy(inp, p, out, outPos, size)
                outPos += size
                p += size
                continue
            }
            if (c < 0x80) throw corrupt("bad chunk type")
            if (p + 4 > limit) throw corrupt("truncated")
            val uSize = ((c and 0x1F) shl 16) + ((inp[p].toInt() and 0xFF) shl 8) + (inp[p + 1].toInt() and 0xFF) + 1
            val cSize = ((inp[p + 2].toInt() and 0xFF) shl 8) + (inp[p + 3].toInt() and 0xFF) + 1
            p += 4
            val reset = (c ushr 5) and 3
            if (reset == 3) {
                dictStart = outPos; needDictReset = false
            } else if (needDictReset) throw corrupt("missing dictionary reset")
            if (reset >= 2) {
                if (p >= limit) throw corrupt("truncated")
                var props = inp[p++].toInt() and 0xFF
                if (props > 224) throw corrupt("bad properties")
                pb = props / 45; props %= 45
                lp = props / 9; lc = props % 9
                if (lc + lp > 4) throw corrupt("bad properties")
                needProps = false
            } else if (needProps) throw corrupt("missing properties")
            if (reset >= 1) resetState()
            if (p + cSize > limit || cSize < 5) throw corrupt("bad chunk size")
            if (outPos + uSize > target) throw corrupt("too much data")
            ip = p
            ipEnd = p + cSize
            if (inp[ip].toInt() != 0) throw corrupt("bad range coder start")
            code = ((inp[ip + 1].toInt() and 0xFF) shl 24) or ((inp[ip + 2].toInt() and 0xFF) shl 16) or
                ((inp[ip + 3].toInt() and 0xFF) shl 8) or (inp[ip + 4].toInt() and 0xFF)
            ip += 5
            range = -1
            decodeChunk(outPos + uSize)
            p += cSize
        }
        return p
    }

    private fun resetState() {
        java.util.Arrays.fill(probs, 1024.toShort())
        state = 0
        rep0 = 0; rep1 = 0; rep2 = 0; rep3 = 0
        pending = 0
    }

    private fun bit(i: Int): Int {
        if (range and 0xFF000000.toInt() == 0) {
            if (ip >= ipEnd) throw corrupt("truncated chunk")
            code = (code shl 8) or (inp[ip++].toInt() and 0xFF)
            range = range shl 8
        }
        val prob = probs[i].toInt()
        val bound = (range ushr 11) * prob
        return if ((code xor Int.MIN_VALUE) < (bound xor Int.MIN_VALUE)) {
            range = bound
            probs[i] = (prob + ((2048 - prob) ushr 5)).toShort()
            0
        } else {
            range -= bound
            code -= bound
            probs[i] = (prob - (prob ushr 5)).toShort()
            1
        }
    }

    private fun tree(base: Int, size: Int): Int {
        var s = 1
        while (s < size) s = (s shl 1) or bit(base + s)
        return s - size
    }

    private fun reverseTree(base: Int, size: Int): Int {
        var s = 1
        var i = 0
        var result = 0
        do {
            val b = bit(base + s)
            s = (s shl 1) or b
            result = result or (b shl i++)
        } while (s < size)
        return result
    }

    private fun direct(count: Int): Int {
        var result = 0
        repeat(count) {
            if (range and 0xFF000000.toInt() == 0) {
                if (ip >= ipEnd) throw corrupt("truncated chunk")
                code = (code shl 8) or (inp[ip++].toInt() and 0xFF)
                range = range shl 8
            }
            range = range ushr 1
            val t = (code - range) ushr 31
            code -= range and (t - 1)
            result = (result shl 1) or (1 - t)
        }
        return result
    }

    private fun length(base: Int, posState: Int): Int {
        if (bit(base) == 0) return tree(base + 2 + posState * 8, 8) + 2
        if (bit(base + 1) == 0) return tree(base + 2 + 128 + posState * 8, 8) + 10
        return tree(base + 2 + 256, 256) + 18
    }

    private fun copyMatch(len: Int, limit: Int) {
        val dist = rep0 + 1
        val n = minOf(len, limit - outPos)
        var src = outPos - dist
        if (dist >= n) {
            System.arraycopy(out, src, out, outPos, n)
            outPos += n
        } else {
            for (k in 0 until n) out[outPos++] = out[src++]
        }
        pending = len - n
    }

    private fun decodeChunk(limit: Int) {
        if (pending > 0) copyMatch(pending, limit)
        val pbMask = (1 shl pb) - 1
        val lpMask = (1 shl lp) - 1
        while (outPos < limit) {
            val posState = outPos and pbMask
            if (bit(IS_MATCH + (state shl 4) + posState) == 0) {
                val prev = if (outPos > dictStart) out[outPos - 1].toInt() and 0xFF else 0
                val base = LITERAL + 0x300 * (((outPos and lpMask) shl lc) + (prev ushr (8 - lc)))
                var s = 1
                if (state < 7) {
                    while (s < 0x100) s = (s shl 1) or bit(base + s)
                } else {
                    var matchByte = out[outPos - rep0 - 1].toInt() and 0xFF
                    var offset = 0x100
                    while (s < 0x100) {
                        matchByte = matchByte shl 1
                        val matchBit = matchByte and offset
                        val b = bit(base + offset + matchBit + s)
                        s = (s shl 1) or b
                        offset = offset and ((0 - b) xor matchBit.inv())
                    }
                }
                out[outPos++] = s.toByte()
                state = if (state < 4) 0 else if (state < 10) state - 3 else state - 6
                continue
            }
            val len: Int
            if (bit(IS_REP + state) == 0) {
                state = if (state < 7) 7 else 10
                rep3 = rep2; rep2 = rep1; rep1 = rep0
                len = length(MATCH_LEN, posState)
                val slot = tree(DIST_SLOT + (minOf(len - 2, 3) shl 6), 64)
                if (slot < 4) {
                    rep0 = slot
                } else {
                    val bits = (slot ushr 1) - 1
                    var d = (2 or (slot and 1)) shl bits
                    if (slot < 14) {
                        d = d or reverseTree(SPECIAL_OFF[slot - 4], 1 shl bits)
                    } else {
                        d = d or (direct(bits - 4) shl 4)
                        d = d or reverseTree(ALIGN, 16)
                    }
                    rep0 = d
                }
            } else {
                if (bit(IS_REP0 + state) == 0) {
                    if (bit(IS_REP0_LONG + (state shl 4) + posState) == 0) {
                        state = if (state < 7) 9 else 11
                        if (rep0 < 0 || rep0 >= outPos - dictStart) throw corrupt("bad distance")
                        out[outPos] = out[outPos - rep0 - 1]
                        outPos++
                        continue
                    }
                } else {
                    val tmp: Int
                    if (bit(IS_REP1 + state) == 0) {
                        tmp = rep1
                    } else {
                        if (bit(IS_REP2 + state) == 0) {
                            tmp = rep2
                        } else {
                            tmp = rep3; rep3 = rep2
                        }
                        rep2 = rep1
                    }
                    rep1 = rep0; rep0 = tmp
                }
                state = if (state < 7) 8 else 11
                len = length(REP_LEN, posState)
            }
            if (rep0 < 0 || rep0 >= outPos - dictStart) throw corrupt("bad distance")
            copyMatch(len, limit)
        }
    }

    private companion object {
        const val IS_MATCH = 0
        const val IS_REP = IS_MATCH + 192
        const val IS_REP0 = IS_REP + 12
        const val IS_REP1 = IS_REP0 + 12
        const val IS_REP2 = IS_REP1 + 12
        const val IS_REP0_LONG = IS_REP2 + 12
        const val DIST_SLOT = IS_REP0_LONG + 192
        const val SPECIAL = DIST_SLOT + 4 * 64
        val SPECIAL_OFF = IntArray(10).also {
            var o = SPECIAL
            for (i in 0 until 10) {
                it[i] = o; o += 1 shl ((i + 4 ushr 1) - 1)
            }
        }
        const val ALIGN = SPECIAL + 124
        const val MATCH_LEN = ALIGN + 16
        const val REP_LEN = MATCH_LEN + 514
        const val LITERAL = REP_LEN + 514
        const val TOTAL = LITERAL + 0x300 * 16
    }
}
