package app.retra.patcher

/** Supported patch container formats. */
enum class PatchFormat { IPS, UPS, BPS, VCDIFF }

/**
 * Information that can be read from a patch without applying it.
 * Fields are null when the format does not record them (e.g. IPS has no checksums,
 * xdelta/VCDIFF does not record the source size or CRC).
 */
data class PatchInfo(
    val format: PatchFormat,
    val sourceSize: Long?,
    val targetSize: Long?,
    val sourceCrc32: Long?,
    val targetCrc32: Long?,
    val metadata: String?,
)

sealed interface PatchResult {
    data class Ok(val output: ByteArray, val info: PatchInfo, val warnings: List<String>) : PatchResult {
        override fun equals(other: Any?): Boolean =
            other is Ok && output.contentEquals(other.output) && info == other.info && warnings == other.warnings

        override fun hashCode(): Int = (output.contentHashCode() * 31 + info.hashCode()) * 31 + warnings.hashCode()

        override fun toString(): String = "Ok(output=${output.size} bytes, info=$info, warnings=$warnings)"
    }

    data class Error(val code: PatchError, val message: String) : PatchResult
}

enum class PatchError { UNKNOWN_FORMAT, CORRUPT_PATCH, SOURCE_MISMATCH, TARGET_MISMATCH, PATCH_CHECKSUM, UNSUPPORTED }

/** Internal control-flow exception carrying a user-facing message. */
internal class PatchException(val code: PatchError, message: String) : Exception(message)

/** Collects warnings and decides whether a soft failure is fatal (strict) or a warning (lenient). */
internal class ApplyContext(val strict: Boolean, private val progress: ((Float) -> Unit)?) {
    val warnings = ArrayList<String>()
    private var lastReported = -1

    /** Fatal in strict mode, warning otherwise. */
    fun soft(code: PatchError, message: String) {
        if (strict) throw PatchException(code, message)
        warnings += message
    }

    fun progress(done: Long, total: Long) {
        val p = progress ?: return
        if (total <= 0) return
        val pct = ((done * 100) / total).toInt().coerceIn(0, 100)
        if (pct != lastReported) {
            lastReported = pct
            p(pct / 100f)
        }
    }
}

object Patcher {
    /** Hard upper bound on output size; GBA ROMs are at most 32 MiB. */
    internal const val MAX_OUTPUT: Long = 512L * 1024 * 1024

    fun detect(patch: ByteArray): PatchFormat? = when {
        patch.startsWith("PATCH") -> PatchFormat.IPS
        patch.startsWith("UPS1") -> PatchFormat.UPS
        patch.startsWith("BPS1") -> PatchFormat.BPS
        patch.size >= 4 && patch[0] == 0xD6.toByte() && patch[1] == 0xC3.toByte() &&
            patch[2] == 0xC4.toByte() && patch[3] == 0.toByte() -> PatchFormat.VCDIFF
        else -> null
    }

    /** Reads header information; returns null for unknown or unreadable patches. */
    fun inspect(patch: ByteArray): PatchInfo? = try {
        when (detect(patch)) {
            PatchFormat.IPS -> Ips.inspect(patch)
            PatchFormat.UPS -> Ups.inspect(patch)
            PatchFormat.BPS -> Bps.inspect(patch)
            PatchFormat.VCDIFF -> Vcdiff.inspect(patch)
            null -> null
        }
    } catch (e: PatchException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    /**
     * Applies [patch] to [source]. With [strict] = false, a base-ROM checksum mismatch (and the
     * resulting output checksum mismatch) is reported as a warning instead of an error, so the user
     * can force-apply. Structural corruption is always an error.
     */
    fun apply(
        source: ByteArray,
        patch: ByteArray,
        strict: Boolean = true,
        progress: ((Float) -> Unit)? = null,
    ): PatchResult {
        val format = detect(patch) ?: return PatchResult.Error(
            PatchError.UNKNOWN_FORMAT,
            if (patch.isEmpty()) "The patch file is empty."
            else "This file isn't a recognised patch. Supported formats are IPS, UPS, BPS and xdelta/VCDIFF.",
        )
        val ctx = ApplyContext(strict, progress)
        return try {
            val (out, info) = when (format) {
                PatchFormat.IPS -> Ips.apply(source, patch, ctx)
                PatchFormat.UPS -> Ups.apply(source, patch, ctx)
                PatchFormat.BPS -> Bps.apply(source, patch, ctx)
                PatchFormat.VCDIFF -> Vcdiff.apply(source, patch, ctx)
            }
            progress?.invoke(1f)
            PatchResult.Ok(out, info, ctx.warnings.toList())
        } catch (e: PatchException) {
            PatchResult.Error(e.code, e.message ?: e.code.name)
        } catch (e: OutOfMemoryError) {
            PatchResult.Error(PatchError.UNSUPPORTED, "Not enough memory to apply this patch.")
        } catch (e: IndexOutOfBoundsException) {
            PatchResult.Error(PatchError.CORRUPT_PATCH, "The ${formatName(format)} patch is damaged or incomplete (it ends unexpectedly). Try downloading it again.")
        } catch (e: RuntimeException) {
            PatchResult.Error(PatchError.CORRUPT_PATCH, "The ${formatName(format)} patch could not be read: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    internal fun formatName(f: PatchFormat) = when (f) {
        PatchFormat.IPS -> "IPS"
        PatchFormat.UPS -> "UPS"
        PatchFormat.BPS -> "BPS"
        PatchFormat.VCDIFF -> "xdelta"
    }

    private fun ByteArray.startsWith(magic: String): Boolean {
        if (size < magic.length) return false
        for (i in magic.indices) if (this[i] != magic[i].code.toByte()) return false
        return true
    }
}

internal object Msg {
    fun hex(crc: Long) = "%08X".format(crc and 0xFFFFFFFFL)

    fun size(n: Long): String = when {
        n >= 1024 * 1024 && n % (1024 * 1024) == 0L -> "${n / (1024 * 1024)} MiB"
        n >= 1024 * 1024 -> "%.1f MiB".format(java.util.Locale.ROOT, n / (1024.0 * 1024.0))
        n >= 1024 -> "%.1f KiB".format(java.util.Locale.ROOT, n / 1024.0)
        else -> "$n bytes"
    }

    fun wrongSource(expectedCrc: Long, expectedSize: Long, actual: ByteArray, actualCrc: Long): String {
        val sizeNote = if (expectedSize != actual.size.toLong())
            " The expected ROM is ${size(expectedSize)}, yours is ${size(actual.size.toLong())}." else ""
        return "This patch expects a different base game (needs CRC32 ${hex(expectedCrc)}, you picked ${hex(actualCrc)}).$sizeNote"
    }

    fun corrupt(fmt: String, detail: String) = "The $fmt patch is damaged or incomplete ($detail). Try downloading it again."
}

/** Big helper for "beat"-style varints (UPS/BPS) and fixed-width reads. */
internal class BeatReader(val buf: ByteArray, var pos: Int, val end: Int, private val fmt: String) {
    fun u8(): Int {
        if (pos >= end) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(fmt, "it ends unexpectedly"))
        return buf[pos++].toInt() and 0xFF
    }

    fun varint(): Long {
        var data = 0L
        var shift = 1L
        while (true) {
            val x = u8()
            data += (x and 0x7F) * shift
            if (data < 0 || shift > (1L shl 56)) throw PatchException(PatchError.CORRUPT_PATCH, Msg.corrupt(fmt, "invalid number"))
            if (x and 0x80 != 0) return data
            shift = shift shl 7
            data += shift
        }
    }

    fun le32(at: Int): Long =
        (buf[at].toLong() and 0xFF) or ((buf[at + 1].toLong() and 0xFF) shl 8) or
            ((buf[at + 2].toLong() and 0xFF) shl 16) or ((buf[at + 3].toLong() and 0xFF) shl 24)
}
