
package app.retra.core.rom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GbaRomParserTest {
    @Test
    fun parsesValidHeader() {
        val rom = TestRomFactory.create(title = "RETRA TEST", gameCode = "RTRE")
        val header = GbaRomParser.parse(rom)
        assertEquals("RETRA TEST", header.title)
        assertEquals("RTRE", header.gameCode)
        assertTrue(header.fixedValueValid)
        assertTrue(header.headerChecksumValid)
    }

    @Test
    fun rejectsBadFixedValue() {
        val rom = TestRomFactory.create()
        rom[0xB2] = 0
        assertFailsWith<InvalidRomException> { GbaRomParser.parse(rom) }
    }
}

internal object TestRomFactory {
    fun create(title: String = "RETRA", gameCode: String = "RTRA"): ByteArray {
        val bytes = ByteArray(1024 * 1024)
        title.encodeToByteArray().copyInto(bytes, 0xA0, endIndex = title.length.coerceAtMost(12))
        gameCode.encodeToByteArray().copyInto(bytes, 0xAC, endIndex = gameCode.length.coerceAtMost(4))
        "01".encodeToByteArray().copyInto(bytes, 0xB0)
        bytes[0xB2] = 0x96.toByte()
        bytes[0xBC] = 0
        bytes[0xBD] = GbaRomParser.calculateHeaderChecksum(bytes).toByte()
        return bytes
    }
}
