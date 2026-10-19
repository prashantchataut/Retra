package app.retra.patcher

/**
 * IPS: "PATCH", records of (u24 offset, u16 size, data) or RLE (size 0, u16 count, u8 value),
 * terminated by "EOF", optionally followed by a u24 truncation size (Lunar IPS extension).
 */
internal object Ips {
    private const val EOF_MARK = 0x454F46
    private const val FMT = "IPS"

    private class Scan(val maxEnd: Long, val truncate: Long?, val hasEof: Boolean, val records: Int)

    /** Walks all records, optionally applying them to [out]. */
    private fun walk(patch: ByteArray, out: ByteArray?, ctx: ApplyContext?): Scan {
        var p = 5
        var maxEnd = 0L
        var records = 0
        val n = patch.size
        while (true) {
            if (p == n) return Scan(maxEnd, null, false, records)
            if (p + 3 > n) throw corrupt("a record is cut off")
            val off = ((patch[p].toInt() and 0xFF) shl 16) or ((patch[p + 1].toInt() and 0xFF) shl 8) or
                (patch[p + 2].toInt() and 0xFF)
            p += 3
            if (off == EOF_MARK) {
                // Exactly at EOF marker. An offset of 0x454F46 followed by more records is ambiguous;
                // by convention it always means end of patch.
                val rest = n - p
                val truncate = if (rest >= 3) {
                    ((patch[p].toLong() and 0xFF) shl 16) or ((patch[p + 1].toLong() and 0xFF) shl 8) or
                        (patch[p + 2].toLong() and 0xFF)
                } else null
                return Scan(maxEnd, truncate, true, records)
            }
            if (p + 2 > n) throw corrupt("a record is cut off")
            val size = ((patch[p].toInt() and 0xFF) shl 8) or (patch[p + 1].toInt() and 0xFF)
            p += 2
            records++
            if (size == 0) {
                if (p + 3 > n) throw corrupt("a record is cut off")
                val count = ((patch[p].toInt() and 0xFF) shl 8) or (patch[p + 1].toInt() and 0xFF)
                val value = patch[p + 2]
                p += 3
                if (out != null) java.util.Arrays.fill(out, off, off + count, value)
                maxEnd = maxOf(maxEnd, off.toLong() + count)
            } else {
                if (p + size > n) throw corrupt("a record is cut off")
                if (out != null) System.arraycopy(patch, p, out, off, size)
                p += size
                maxEnd = maxOf(maxEnd, off.toLong() + size)
            }
            if (ctx != null && (records and 0xFFF) == 0) ctx.progress(p.toLong(), n.toLong())
        }
    }

    private fun corrupt(detail: String) = PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, detail))

    fun inspect(patch: ByteArray): PatchInfo {
        val s = walk(patch, null, null)
        return PatchInfo(PatchFormat.IPS, null, s.truncate, null, null, null)
    }

    fun apply(source: ByteArray, patch: ByteArray, ctx: ApplyContext): Pair<ByteArray, PatchInfo> {
        val scan = walk(patch, null, null)
        if (!scan.hasEof) ctx.warnings += "The IPS patch has no end marker; it may be incomplete."
        val size = scan.truncate ?: maxOf(source.size.toLong(), scan.maxEnd)
        if (size > Patcher.MAX_OUTPUT) throw PatchException(PatchError.UNSUPPORTED, "The patched game would be too large.")
        val out: ByteArray
        if (scan.truncate != null && scan.maxEnd > scan.truncate) {
            // Records write past the truncation point: apply on a full buffer then cut.
            val tmp = source.copyOf(maxOf(source.size.toLong(), scan.maxEnd).toInt())
            walk(patch, tmp, ctx)
            out = tmp.copyOf(size.toInt())
        } else {
            out = source.copyOf(size.toInt())
            walk(patch, out, ctx)
        }
        return out to PatchInfo(PatchFormat.IPS, source.size.toLong(), size, null, null, null)
    }
}
