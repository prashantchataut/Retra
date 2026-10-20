package app.retra.patcher

import app.retra.patcher.TestUtil.err
import app.retra.patcher.TestUtil.ok
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpsTest {
    @Test fun roundTripSameSize() {
        for (seed in 1..5) {
            val src = TestUtil.random(100_000, seed)
            val tgt = TestUtil.mutate(src, seed + 100)
            val r = ok(Patcher.apply(src, TestUtil.ips(src, tgt)))
            assertArrayEquals(tgt, r.output)
            assertEquals(PatchFormat.IPS, r.info.format)
            assertTrue(r.warnings.isEmpty())
        }
    }

    @Test fun growthAndRle() {
        val src = TestUtil.random(50_000, 7)
        val tgt = TestUtil.mutate(src, 8, newSize = 90_000)
        val patch = TestUtil.ips(src, tgt)
        assertArrayEquals(tgt, ok(Patcher.apply(src, patch)).output)
    }

    @Test fun rleRecordExplicit() {
        val patch = byteArrayOf(*"PATCH".toByteArray(), 0, 0, 2, 0, 0, 0, 4, 0x7F, *"EOF".toByteArray())
        val out = ok(Patcher.apply(ByteArray(3), patch)).output
        assertArrayEquals(byteArrayOf(0, 0, 0x7F, 0x7F, 0x7F, 0x7F), out)
    }

    @Test fun truncateExtension() {
        val src = TestUtil.random(10_000, 3)
        val tgt = TestUtil.mutate(src, 4).copyOf(6_000)
        val patch = TestUtil.ips(src, tgt)
        assertEquals(6_000L, Patcher.inspect(patch)!!.targetSize)
        assertArrayEquals(tgt, ok(Patcher.apply(src, patch)).output)
    }

    @Test fun offsetsBeyondEofMarker() {
        // Data spanning across 0x454F46 must be encoded starting one byte earlier.
        val src = ByteArray(0x454F50)
        val tgt = src.copyOf(); tgt[0x454F46] = 1; tgt[0x454F47] = 2
        val patch = TestUtil.ips(src, tgt)
        assertArrayEquals(tgt, ok(Patcher.apply(src, patch)).output)
    }

    @Test fun truncatedPatch() {
        val src = TestUtil.random(10_000, 3)
        val patch = TestUtil.ips(src, TestUtil.mutate(src, 9))
        val e = err(Patcher.apply(src, patch.copyOf(patch.size - 10)))
        assertEquals(PatchError.CORRUPT_PATCH, e.code)
        assertTrue(e.message, e.message.contains("IPS"))
    }

    @Test fun missingEofIsWarning() {
        val patch = byteArrayOf(*"PATCH".toByteArray(), 0, 0, 1, 0, 1, 9)
        val r = ok(Patcher.apply(ByteArray(2), patch))
        assertArrayEquals(byteArrayOf(0, 9), r.output)
        assertEquals(1, r.warnings.size)
    }
}

class UpsTest {
    @Test fun roundTripVariousSizes() {
        val sizes = listOf(1000 to 1000, 1000 to 5000, 5000 to 1000, 0 to 300, 200_000 to 400_000)
        for ((i, s) in sizes.withIndex()) {
            val src = TestUtil.random(s.first, i)
            val tgt = TestUtil.mutate(src, i + 50, newSize = s.second)
            val patch = TestUtil.ups(src, tgt)
            val r = ok(Patcher.apply(src, patch))
            assertArrayEquals("case $s", tgt, r.output)
            val info = Patcher.inspect(patch)!!
            assertEquals(PatchFormat.UPS, info.format)
            assertEquals(s.first.toLong(), info.sourceSize)
            assertEquals(s.second.toLong(), info.targetSize)
            assertEquals(Checksums.crc32(src), info.sourceCrc32)
            assertEquals(Checksums.crc32(tgt), info.targetCrc32)
        }
    }

    @Test fun reverseApply() {
        val src = TestUtil.random(4000, 1)
        val tgt = TestUtil.mutate(src, 2, newSize = 6000)
        val patch = TestUtil.ups(src, tgt)
        val r = ok(Patcher.apply(tgt, patch))
        assertArrayEquals(src, r.output)
        assertEquals(1, r.warnings.size)
    }

    @Test fun sourceMismatchStrictAndLenient() {
        val src = TestUtil.random(4000, 1)
        val tgt = TestUtil.mutate(src, 2)
        val patch = TestUtil.ups(src, tgt)
        val wrong = src.copyOf().also { it[10] = (it[10] + 1).toByte() }
        val e = err(Patcher.apply(wrong, patch))
        assertEquals(PatchError.SOURCE_MISMATCH, e.code)
        assertTrue(e.message, e.message.contains("needs CRC32 ${Checksums.crc32Hex(src)}"))
        assertTrue(e.message, e.message.contains(Checksums.crc32Hex(wrong)))
        val r = ok(Patcher.apply(wrong, patch, strict = false))
        assertEquals(2, r.warnings.size) // source mismatch + resulting target mismatch
        assertEquals(tgt.size, r.output.size)
    }

    @Test fun patchChecksum() {
        val src = TestUtil.random(4000, 1)
        val patch = TestUtil.ups(src, TestUtil.mutate(src, 2))
        patch[10] = (patch[10] + 1).toByte()
        assertEquals(PatchError.PATCH_CHECKSUM, err(Patcher.apply(src, patch)).code)
    }

    @Test fun truncated() {
        val src = TestUtil.random(4000, 1)
        val patch = TestUtil.ups(src, TestUtil.mutate(src, 2))
        for (cut in listOf(3, 8, 15, patch.size / 2)) {
            val r = Patcher.apply(src, patch.copyOf(cut))
            assertTrue(r is PatchResult.Error)
        }
        assertNull(Patcher.inspect(patch.copyOf(8)))
    }
}

class BpsTest {
    @Test fun roundTripWithCreator() {
        val cases = listOf(100_000 to 100_000, 100_000 to 250_000, 250_000 to 100_000, 0 to 1000, 1000 to 0)
        for ((i, c) in cases.withIndex()) {
            val src = TestUtil.random(c.first, i)
            val tgt = if (c.second == 0) ByteArray(0) else TestUtil.mutate(src, i + 9, newSize = c.second)
            val patch = BpsCreator.create(src, tgt, "author=test")
            val r = ok(Patcher.apply(src, patch))
            assertArrayEquals("case $c", tgt, r.output)
            assertEquals("author=test", r.info.metadata)
            assertEquals(Checksums.crc32(src), r.info.sourceCrc32)
            assertEquals(Checksums.crc32(tgt), r.info.targetCrc32)
            if (c.first == c.second) assertTrue("patch too big: ${patch.size}", patch.size < 30_000)
        }
    }

    @Test fun overlappingTargetCopy() {
        // Hand-built: TargetRead "AB", then TargetCopy len 6 from offset 0 -> "ABABABAB".
        val body = java.io.ByteArrayOutputStream()
        body.write("BPS1".toByteArray())
        body.write(0x80) // source size 0
        body.write(0x88) // target size 8
        body.write(0x80) // no metadata
        body.write(0x80 or ((1 shl 2) or 1)) // TargetRead len 2
        body.write("AB".toByteArray())
        body.write(0x80 or ((5 shl 2) or 3)) // TargetCopy len 6
        body.write(0x80) // offset +0
        val target = "ABABABAB".toByteArray()
        fun le32(v: Long) { for (k in 0 until 4) body.write(((v ushr (8 * k)) and 0xFF).toInt()) }
        le32(Checksums.crc32(ByteArray(0))); le32(Checksums.crc32(target))
        le32(Checksums.crc32(body.toByteArray()))
        val r = ok(Patcher.apply(ByteArray(0), body.toByteArray()))
        assertArrayEquals(target, r.output)
        assertNull(r.info.metadata)
    }

    @Test fun mismatchAndCorruption() {
        val src = TestUtil.random(50_000, 1)
        val tgt = TestUtil.mutate(src, 2, newSize = 70_000)
        val patch = BpsCreator.create(src, tgt)
        val wrong = TestUtil.random(50_000, 99)
        assertEquals(PatchError.SOURCE_MISMATCH, err(Patcher.apply(wrong, patch)).code)
        val lenient = ok(Patcher.apply(wrong, patch, strict = false))
        assertTrue(lenient.warnings.any { it.contains("different base game") })
        // Smaller source -> cannot force
        val small = src.copyOf(1000)
        assertEquals(PatchError.SOURCE_MISMATCH, err(Patcher.apply(small, patch, strict = false)).code)
        // Corrupt patch byte
        val bad = patch.copyOf(); bad[bad.size / 2] = (bad[bad.size / 2] + 1).toByte()
        assertEquals(PatchError.PATCH_CHECKSUM, err(Patcher.apply(src, bad)).code)
        // Truncated
        for (cut in listOf(4, 10, patch.size / 2, patch.size - 1)) {
            assertTrue(Patcher.apply(src, patch.copyOf(cut)) is PatchResult.Error)
            Patcher.apply(src, patch.copyOf(cut), strict = false) // must not throw
        }
    }

    @Test fun fuzzNeverThrows() {
        val src = TestUtil.random(5000, 1)
        val tgt = TestUtil.mutate(src, 2)
        val patches = listOf(BpsCreator.create(src, tgt), TestUtil.ups(src, tgt), TestUtil.ips(src, tgt),
            TestUtil.resource("vcdiff/none.xdelta"), TestUtil.resource("vcdiff/default_lzma.xdelta"))
        val r = kotlin.random.Random(5)
        for (p in patches) repeat(300) {
            val m = p.copyOf()
            repeat(r.nextInt(1, 4)) { val i = r.nextInt(4, m.size); m[i] = r.nextInt().toByte() }
            Patcher.apply(src, m, strict = false)
            Patcher.inspect(m)
        }
    }
}

class MiscTest {
    @Test fun detect() {
        assertEquals(PatchFormat.IPS, Patcher.detect("PATCHxxx".toByteArray()))
        assertEquals(PatchFormat.UPS, Patcher.detect("UPS1".toByteArray()))
        assertEquals(PatchFormat.BPS, Patcher.detect("BPS1".toByteArray()))
        assertEquals(PatchFormat.VCDIFF, Patcher.detect(byteArrayOf(0xD6.toByte(), 0xC3.toByte(), 0xC4.toByte(), 0)))
        assertNull(Patcher.detect("hello".toByteArray()))
        assertNull(Patcher.detect(ByteArray(0)))
        assertEquals(PatchError.UNKNOWN_FORMAT, err(Patcher.apply(ByteArray(10), "nope".toByteArray())).code)
    }

    @Test fun checksums() {
        val b = "The quick brown fox jumps over the lazy dog".toByteArray()
        assertEquals(0x414FA339L, Checksums.crc32(b))
        assertEquals("414FA339", Checksums.crc32Hex(b))
        assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", Checksums.sha1Hex(b))
        assertEquals("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592", Checksums.sha256Hex(b))
        assertEquals("00000000", Checksums.crc32Hex(ByteArray(0)))
    }

    @Test fun progressReported() {
        val src = TestUtil.random(300_000, 1)
        val patch = BpsCreator.create(src, TestUtil.mutate(src, 2, edits = 5000))
        val seen = ArrayList<Float>()
        ok(Patcher.apply(src, patch, progress = { seen += it }))
        assertTrue(seen.size > 2)
        assertEquals(1f, seen.last())
        assertTrue(seen.zipWithNext().all { it.first <= it.second })
    }
}
