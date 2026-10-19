package app.retra.patcher

import java.util.zip.Adler32

/**
 * VCDIFF (RFC 3284) decoder with the xdelta3 extensions: VCD_ADLER32 per-window checksums,
 * application header, and LZMA secondary compression. DJW/FGK secondary compression and
 * application-defined code tables are reported as unsupported.
 */
internal object Vcdiff {
    private const val FMT = "xdelta"

    private const val VCD_DECOMPRESS = 0x01
    private const val VCD_CODETABLE = 0x02
    private const val VCD_APPHEADER = 0x04

    private const val VCD_SOURCE = 0x01
    private const val VCD_TARGET = 0x02
    private const val VCD_ADLER32 = 0x04

    private const val NOOP = 0
    private const val ADD = 1
    private const val RUN = 2
    private const val COPY = 3

    private const val NEAR = 4
    private const val SAME = 3

    // Default code table (RFC 3284 section 5.6).
    private val TYPE1 = IntArray(256)
    private val SIZE1 = IntArray(256)
    private val MODE1 = IntArray(256)
    private val TYPE2 = IntArray(256)
    private val SIZE2 = IntArray(256)
    private val MODE2 = IntArray(256)

    init {
        var i = 0
        TYPE1[i] = RUN; i++
        for (s in 0..17) { TYPE1[i] = ADD; SIZE1[i] = s; i++ }
        for (m in 0..8) {
            TYPE1[i] = COPY; SIZE1[i] = 0; MODE1[i] = m; i++
            for (s in 4..18) { TYPE1[i] = COPY; SIZE1[i] = s; MODE1[i] = m; i++ }
        }
        for (m in 0..5) for (a in 1..4) for (c in 4..6) {
            TYPE1[i] = ADD; SIZE1[i] = a; TYPE2[i] = COPY; SIZE2[i] = c; MODE2[i] = m; i++
        }
        for (m in 6..8) for (a in 1..4) {
            TYPE1[i] = ADD; SIZE1[i] = a; TYPE2[i] = COPY; SIZE2[i] = 4; MODE2[i] = m; i++
        }
        for (m in 0..8) {
            TYPE1[i] = COPY; SIZE1[i] = 4; MODE1[i] = m; TYPE2[i] = ADD; SIZE2[i] = 1; i++
        }
        check(i == 256)
    }

    private fun corrupt(detail: String) = PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(FMT, detail))

    private class Reader(val buf: ByteArray, var pos: Int, var end: Int) {
        fun u8(): Int {
            if (pos >= end) throw corrupt("it ends unexpectedly")
            return buf[pos++].toInt() and 0xFF
        }

        fun varint(): Long {
            var r = 0L
            for (i in 0 until 10) {
                val b = u8()
                r = (r shl 7) or (b and 0x7F).toLong()
                if (b and 0x80 == 0) {
                    if (r < 0) throw corrupt("invalid number")
                    return r
                }
            }
            throw corrupt("invalid number")
        }

        fun int(what: String): Int {
            val v = varint()
            if (v > Int.MAX_VALUE) throw corrupt("$what is too large")
            return v.toInt()
        }
    }

    private class Header(val secondaryId: Int?, val hasCodeTable: Boolean, val appHeader: String?, val windowsStart: Int)

    private class Window(
        val indicator: Int, val segLen: Long, val segPos: Long, val targetLen: Int, val deltaInd: Int,
        val dataLen: Int, val instLen: Int, val addrLen: Int, val adler: Long?, val sectionsStart: Int,
    )

    private fun header(patch: ByteArray): Header {
        val r = Reader(patch, 4, patch.size)
        val ind = r.u8()
        if (ind and 0xF8 != 0) throw corrupt("unknown header flags")
        var secondary: Int? = null
        if (ind and VCD_DECOMPRESS != 0) secondary = r.u8()
        val hasTable = ind and VCD_CODETABLE != 0
        if (hasTable) {
            val n = r.int("code table")
            if (n > r.end - r.pos) throw corrupt("bad code table length")
            r.pos += n
        }
        var app: String? = null
        if (ind and VCD_APPHEADER != 0) {
            val n = r.int("application header")
            if (n > r.end - r.pos) throw corrupt("bad header length")
            app = String(patch, r.pos, n, Charsets.UTF_8)
            r.pos += n
        }
        return Header(secondary, hasTable, app, r.pos)
    }

    private fun windows(patch: ByteArray, start: Int): List<Window> {
        val list = ArrayList<Window>()
        val r = Reader(patch, start, patch.size)
        while (r.pos < r.end) {
            val ind = r.u8()
            if (ind and 0xF8 != 0 || (ind and VCD_SOURCE != 0 && ind and VCD_TARGET != 0)) throw corrupt("bad window flags")
            var segLen = 0L
            var segPos = 0L
            if (ind and (VCD_SOURCE or VCD_TARGET) != 0) {
                segLen = r.varint(); segPos = r.varint()
            }
            val deltaLen = r.int("window")
            val deltaStart = r.pos
            if (deltaLen > r.end - deltaStart) throw corrupt("a window is cut off")
            val tgtLen = r.int("window")
            val deltaInd = r.u8()
            val dataLen = r.int("data section")
            val instLen = r.int("instruction section")
            val addrLen = r.int("address section")
            var adler: Long? = null
            if (ind and VCD_ADLER32 != 0) {
                adler = (r.u8().toLong() shl 24) or (r.u8().toLong() shl 16) or (r.u8().toLong() shl 8) or r.u8().toLong()
            }
            val sections = r.pos
            if (sections.toLong() + dataLen + instLen + addrLen != deltaStart.toLong() + deltaLen) throw corrupt("inconsistent window sizes")
            list += Window(ind, segLen, segPos, tgtLen, deltaInd, dataLen, instLen, addrLen, adler, sections)
            r.pos = deltaStart + deltaLen
        }
        return list
    }

    fun inspect(patch: ByteArray): PatchInfo {
        val h = header(patch)
        val ws = windows(patch, h.windowsStart)
        val total = ws.sumOf { it.targetLen.toLong() }
        return PatchInfo(PatchFormat.VCDIFF, null, total, null, null, h.appHeader)
    }

    private fun secondaryName(id: Int?) = when (id) {
        1 -> "DJW"
        2 -> "LZMA"
        16 -> "FGK"
        null -> "unknown"
        else -> "unknown (#$id)"
    }

    fun apply(sourceIn: ByteArray, patch: ByteArray, ctx: ApplyContext): Pair<ByteArray, PatchInfo> {
        val h = header(patch)
        if (h.hasCodeTable) {
            throw PatchException(PatchError.UNSUPPORTED, "This xdelta patch uses a custom code table, which isn't supported.")
        }
        val ws = windows(patch, h.windowsStart)
        val total = ws.sumOf { it.targetLen.toLong() }
        if (total > Patcher.MAX_OUTPUT) throw PatchException(PatchError.UNSUPPORTED, "The patched game would be too large (${Msg.size(total)}).")
        val info = PatchInfo(PatchFormat.VCDIFF, null, total, null, null, h.appHeader)

        // Source bounds check (xdelta has no source checksum; size is the only up-front hint).
        var source = sourceIn
        val needed = ws.filter { it.indicator and VCD_SOURCE != 0 }.maxOfOrNull { it.segPos + it.segLen } ?: 0L
        if (needed > source.size) {
            val msg = if (source.isEmpty()) "This xdelta patch needs a base ROM, but none was provided."
            else "This patch expects a different base game: it reads ${Msg.size(needed)} of base ROM but yours is only ${Msg.size(source.size.toLong())}."
            if (ctx.strict || needed > Patcher.MAX_OUTPUT) throw PatchException(PatchError.SOURCE_MISMATCH, msg)
            ctx.warnings += msg
            source = source.copyOf(needed.toInt())
        }

        val out = ByteArray(total.toInt())
        val near = IntArray(NEAR)
        val same = IntArray(SAME * 256)
        val adler = Adler32()
        var outBase = 0
        var checksumWarned = false
        // xdelta3 keeps one compressor stream per section type across all windows.
        val xzData = XzSectionStream()
        val xzInst = XzSectionStream()
        val xzAddr = XzSectionStream()
        for (w in ws) {
            // Resolve sections (decompressing secondary-compressed ones).
            var dataBuf = patch; var dataPos = w.sectionsStart; var dataEnd = dataPos + w.dataLen
            var instBuf = patch; var instPos = dataEnd; var instEnd = instPos + w.instLen
            var addrBuf = patch; var addrPos = instEnd; var addrEnd = addrPos + w.addrLen
            if (w.deltaInd and 0x07.inv() != 0) throw corrupt("bad window flags")
            if (w.deltaInd != 0) {
                if (h.secondaryId != 2) {
                    if (h.secondaryId == null) throw corrupt("compressed section without a compressor")
                    throw PatchException(
                        PatchError.UNSUPPORTED,
                        "This xdelta patch uses ${secondaryName(h.secondaryId)} secondary compression, which isn't supported. " +
                            "Ask for a patch made with default settings or \"xdelta3 -S none\".",
                    )
                }
                if (w.deltaInd and 1 != 0) {
                    val (b, o, n) = lzmaSection(xzData, patch, dataPos, dataEnd); dataBuf = b; dataPos = o; dataEnd = o + n
                }
                if (w.deltaInd and 2 != 0) {
                    val (b, o, n) = lzmaSection(xzInst, patch, instPos, instEnd); instBuf = b; instPos = o; instEnd = o + n
                }
                if (w.deltaInd and 4 != 0) {
                    val (b, o, n) = lzmaSection(xzAddr, patch, addrPos, addrEnd); addrBuf = b; addrPos = o; addrEnd = o + n
                }
            }
            val data = Reader(dataBuf, dataPos, dataEnd)
            val inst = Reader(instBuf, instPos, instEnd)
            val addr = Reader(addrBuf, addrPos, addrEnd)

            val segBuf: ByteArray
            val segStart: Int
            val segLen: Int
            when {
                w.indicator and VCD_SOURCE != 0 -> { segBuf = source; segStart = w.segPos.toInt(); segLen = w.segLen.toInt() }
                w.indicator and VCD_TARGET != 0 -> {
                    if (w.segPos + w.segLen > outBase) throw corrupt("window refers to data not yet produced")
                    segBuf = out; segStart = w.segPos.toInt(); segLen = w.segLen.toInt()
                }
                else -> { segBuf = out; segStart = 0; segLen = 0 }
            }
            val tLen = w.targetLen
            if (outBase.toLong() + tLen > out.size) throw corrupt("window too large")

            java.util.Arrays.fill(near, 0)
            java.util.Arrays.fill(same, 0)
            var nextSlot = 0
            var t = 0 // position within this target window
            while (inst.pos < inst.end) {
                val op = inst.u8()
                for (half in 0..1) {
                    val type = if (half == 0) TYPE1[op] else TYPE2[op]
                    if (type == NOOP) continue
                    var size = if (half == 0) SIZE1[op] else SIZE2[op]
                    if (size == 0) size = inst.int("instruction")
                    if (size > tLen - t) throw corrupt("instruction writes past the window")
                    val dst = outBase + t
                    when (type) {
                        ADD -> {
                            if (size > data.end - data.pos) throw corrupt("data section too short")
                            System.arraycopy(data.buf, data.pos, out, dst, size)
                            data.pos += size
                        }
                        RUN -> java.util.Arrays.fill(out, dst, dst + size, data.u8().toByte())
                        else -> {
                            val mode = if (half == 0) MODE1[op] else MODE2[op]
                            val here = segLen.toLong() + t
                            val a: Long = when {
                                mode == 0 -> addr.varint()
                                mode == 1 -> here - addr.varint()
                                mode < 2 + NEAR -> near[mode - 2].toLong() + addr.varint()
                                else -> same[(mode - 2 - NEAR) * 256 + addr.u8()].toLong()
                            }
                            if (a < 0 || a >= here) throw corrupt("invalid copy address")
                            val ai = a.toInt()
                            near[nextSlot] = ai; nextSlot = (nextSlot + 1) % NEAR
                            same[ai % (SAME * 256)] = ai
                            if (ai < segLen) {
                                if (ai + size <= segLen) {
                                    System.arraycopy(segBuf, segStart + ai, out, dst, size)
                                } else {
                                    // Spans from the source segment into the target window.
                                    val first = segLen - ai
                                    System.arraycopy(segBuf, segStart + ai, out, dst, first)
                                    var s = outBase
                                    for (k in first until size) out[dst + k] = out[s++]
                                }
                            } else {
                                val s = outBase + (ai - segLen)
                                if (s + size <= dst) System.arraycopy(out, s, out, dst, size)
                                else for (k in 0 until size) out[dst + k] = out[s + k]
                            }
                        }
                    }
                    t += size
                }
            }
            if (t != tLen) throw corrupt("a window produced ${t} of $tLen bytes")
            if (w.adler != null) {
                adler.reset()
                adler.update(out, outBase, tLen)
                if (adler.value != w.adler && !checksumWarned) {
                    checksumWarned = true
                    val usesSource = ws.any { it.indicator and VCD_SOURCE != 0 }
                    if (usesSource) {
                        ctx.soft(PatchError.SOURCE_MISMATCH, "The patched game doesn't match what the patch expects. This patch was most likely made for a different base game (yours has CRC32 ${Checksums.crc32Hex(sourceIn)}).")
                    } else {
                        ctx.soft(PatchError.TARGET_MISMATCH, "The patched game doesn't match the checksum stored in the patch. The patch may be damaged.")
                    }
                }
            }
            outBase += tLen
            ctx.progress(outBase.toLong(), total)
        }
        return out to info
    }

    private fun lzmaSection(xz: XzSectionStream, patch: ByteArray, start: Int, end: Int): Triple<ByteArray, Int, Int> {
        val r = Reader(patch, start, end)
        val size = r.int("compressed section")
        if (size > Patcher.MAX_OUTPUT) throw corrupt("compressed section too large")
        val (buf, off) = xz.decode(patch, r.pos, end - r.pos, size)
        return Triple(buf, off, size)
    }
}
