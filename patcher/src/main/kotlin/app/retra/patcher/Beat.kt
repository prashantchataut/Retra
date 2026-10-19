package app.retra.patcher

/** Shared logic for the "beat" family (UPS, BPS): 12-byte CRC32 footer and checks. */
internal object Beat {
    class Footer(val sourceCrc: Long, val targetCrc: Long, val patchCrc: Long)

    fun footer(patch: ByteArray, fmt: String, headerLen: Int): Footer {
        if (patch.size < headerLen + 12) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(fmt, "it is too short"))
        val r = BeatReader(patch, 0, patch.size, fmt)
        val e = patch.size
        return Footer(r.le32(e - 12), r.le32(e - 8), r.le32(e - 4))
    }

    fun checkPatchCrc(patch: ByteArray, f: Footer, fmt: String, ctx: ApplyContext) {
        val actual = Checksums.crc32(patch, 0, patch.size - 4)
        if (actual != f.patchCrc) {
            ctx.soft(
                PatchError.PATCH_CHECKSUM,
                "The $fmt patch file is damaged (its checksum is ${Msg.hex(actual)}, expected ${Msg.hex(f.patchCrc)}). Try downloading it again.",
            )
        }
    }

    fun checkTarget(out: ByteArray, expectedCrc: Long, ctx: ApplyContext) {
        val crc = Checksums.crc32(out)
        if (crc != expectedCrc) {
            ctx.soft(
                PatchError.TARGET_MISMATCH,
                "The patched game doesn't match what the patch author intended (expected CRC32 ${Msg.hex(expectedCrc)}, got ${Msg.hex(crc)}). It may not work correctly.",
            )
        }
    }

    fun checkSize(n: Long, fmt: String) {
        if (n > Patcher.MAX_OUTPUT) throw PatchException(PatchError.UNSUPPORTED, "The $fmt patch describes a ${Msg.size(n)} file, which is too large.")
    }
}

internal object Ups {
    private const val FMT = "UPS"

    fun inspect(patch: ByteArray): PatchInfo {
        val f = Beat.footer(patch, FMT, 4)
        val r = BeatReader(patch, 4, patch.size - 12, FMT)
        val src = r.varint()
        val tgt = r.varint()
        return PatchInfo(PatchFormat.UPS, src, tgt, f.sourceCrc, f.targetCrc, null)
    }

    fun apply(source: ByteArray, patch: ByteArray, ctx: ApplyContext): Pair<ByteArray, PatchInfo> {
        val f = Beat.footer(patch, FMT, 4)
        Beat.checkPatchCrc(patch, f, FMT, ctx)
        val end = patch.size - 12
        val r = BeatReader(patch, 4, end, FMT)
        val srcSize = r.varint()
        val tgtSize = r.varint()
        Beat.checkSize(srcSize, FMT); Beat.checkSize(tgtSize, FMT)
        val info = PatchInfo(PatchFormat.UPS, srcSize, tgtSize, f.sourceCrc, f.targetCrc, null)

        // UPS is symmetric: it can turn the target back into the source as well.
        val crc = Checksums.crc32(source)
        val outSize: Long
        val expectCrc: Long
        if (source.size.toLong() == srcSize && crc == f.sourceCrc) {
            outSize = tgtSize; expectCrc = f.targetCrc
        } else if (source.size.toLong() == tgtSize && crc == f.targetCrc) {
            outSize = srcSize; expectCrc = f.sourceCrc
            ctx.warnings += "The selected ROM is already the patched version; the patch was applied in reverse to restore the original."
        } else {
            ctx.soft(PatchError.SOURCE_MISMATCH, Msg.wrongSource(f.sourceCrc, srcSize, source, crc))
            outSize = tgtSize; expectCrc = f.targetCrc
        }

        val n = outSize.toInt()
        val out = ByteArray(n)
        System.arraycopy(source, 0, out, 0, minOf(source.size, n))
        val buf = patch
        var pos = 0L
        var p = r.pos
        var iter = 0
        while (p < end) {
            r.pos = p
            pos += r.varint()
            p = r.pos
            while (true) {
                if (p >= end) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "a record is cut off"))
                val x = buf[p++]
                if (pos < n) out[pos.toInt()] = (out[pos.toInt()].toInt() xor x.toInt()).toByte()
                pos++
                if (x.toInt() == 0) break
            }
            if ((++iter and 0x3FF) == 0) ctx.progress(p.toLong(), end.toLong())
        }
        Beat.checkTarget(out, expectCrc, ctx)
        return out to info
    }
}

internal object Bps {
    private const val FMT = "BPS"

    private class Header(val src: Long, val tgt: Long, val metadata: String?, val dataStart: Int)

    private fun header(patch: ByteArray): Header {
        val r = BeatReader(patch, 4, patch.size - 12, FMT)
        val src = r.varint()
        val tgt = r.varint()
        val metaLen = r.varint()
        if (metaLen > patch.size - 12 - r.pos) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "bad metadata length"))
        val meta = if (metaLen > 0) String(patch, r.pos, metaLen.toInt(), Charsets.UTF_8) else null
        return Header(src, tgt, meta, r.pos + metaLen.toInt())
    }

    fun inspect(patch: ByteArray): PatchInfo {
        val f = Beat.footer(patch, FMT, 4)
        val h = header(patch)
        return PatchInfo(PatchFormat.BPS, h.src, h.tgt, f.sourceCrc, f.targetCrc, h.metadata)
    }

    fun apply(source: ByteArray, patch: ByteArray, ctx: ApplyContext): Pair<ByteArray, PatchInfo> {
        val f = Beat.footer(patch, FMT, 4)
        Beat.checkPatchCrc(patch, f, FMT, ctx)
        val h = header(patch)
        Beat.checkSize(h.src, FMT); Beat.checkSize(h.tgt, FMT)
        val info = PatchInfo(PatchFormat.BPS, h.src, h.tgt, f.sourceCrc, f.targetCrc, h.metadata)
        val crc = Checksums.crc32(source)
        if (source.size.toLong() != h.src || crc != f.sourceCrc) {
            ctx.soft(PatchError.SOURCE_MISMATCH, Msg.wrongSource(f.sourceCrc, h.src, source, crc))
        }

        val end = patch.size - 12
        val r = BeatReader(patch, h.dataStart, end, FMT)
        val n = h.tgt.toInt()
        val out = ByteArray(n)
        var outOff = 0
        var srcRel = 0L
        var tgtRel = 0L
        var iter = 0
        while (r.pos < end) {
            val data = r.varint()
            val len = (data ushr 2) + 1
            if (len > n - outOff) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "it writes past the end of the game"))
            val l = len.toInt()
            when ((data and 3).toInt()) {
                0 -> { // SourceRead
                    if (outOff + l > source.size) throw tooSmall(source, h.src)
                    System.arraycopy(source, outOff, out, outOff, l)
                }
                1 -> { // TargetRead
                    if (l > end - r.pos) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "it ends unexpectedly"))
                    System.arraycopy(patch, r.pos, out, outOff, l)
                    r.pos += l
                }
                2 -> { // SourceCopy
                    val d = r.varint()
                    srcRel += if (d and 1L != 0L) -(d ushr 1) else (d ushr 1)
                    if (srcRel < 0) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "invalid source offset"))
                    if (srcRel + l > source.size) throw tooSmall(source, h.src)
                    System.arraycopy(source, srcRel.toInt(), out, outOff, l)
                    srcRel += l
                }
                else -> { // TargetCopy
                    val d = r.varint()
                    tgtRel += if (d and 1L != 0L) -(d ushr 1) else (d ushr 1)
                    if (tgtRel < 0 || tgtRel >= outOff) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "invalid target offset"))
                    var t = tgtRel.toInt()
                    if (t + l <= outOff) {
                        System.arraycopy(out, t, out, outOff, l)
                    } else {
                        // Overlapping copy (repeating pattern): must go byte by byte.
                        for (i in 0 until l) out[outOff + i] = out[t + i]
                    }
                    t += l
                    tgtRel = t.toLong()
                }
            }
            outOff += l
            if ((++iter and 0x3FF) == 0) ctx.progress(outOff.toLong(), n.toLong())
        }
        if (outOff != n) ctx.soft(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, "it produced only $outOff of $n bytes"))
        Beat.checkTarget(out, f.targetCrc, ctx)
        return out to info
    }

    private fun tooSmall(source: ByteArray, expected: Long) = PatchException(
        PatchError.SOURCE_MISMATCH,
        "This patch expects a different base game: it needs a ${Msg.size(expected)} ROM but yours is ${Msg.size(source.size.toLong())}.",
    )
}
