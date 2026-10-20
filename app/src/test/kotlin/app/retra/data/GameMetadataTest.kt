package app.retra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class GameMetadataTest {
    private fun gz(text: String): ByteArray = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(text.toByteArray()) } }.toByteArray()

    private fun game(crc: String, code: String, source: GameSource = GameSource.IMPORTED, catalogId: String? = null) = Game(
        id = crc.lowercase(), title = "T", romTitle = "T", gameCode = code, sizeBytes = 1, crc32 = crc, sha1 = "", addedAt = 0,
        source = source, catalogId = catalogId,
    )

    private val pack = """{"schema":1,"source":"libretro-database test","license":"CC-BY-SA-4.0","games":[
        {"title":"Pokemon - Emerald Version (USA, Europe)","crc32s":["1F1C08FB"],"serials":["BPEE"],"catalogIds":[],
         "cheats":[{"name":"Master Code","code":"D8BAE4D9 4864DCE5","master":true},{"name":"Infinite Money","code":"82025838 0000"}]},
        {"title":"Pokemon Unbound","crc32s":[],"serials":[],"catalogIds":["pokemon-unbound"],"cheats":[{"name":"Max money","code":"02000000:FF"}]}
    ]}"""

    @Test fun cheatBookMatchesByCrcSerialAndCatalogId() {
        val db = CheatDatabase { ByteArrayInputStream(gz(pack)) }
        assertEquals(2, db.lookup(game("1F1C08FB", "BPEE"))?.cheats?.size)
        // Another dump of the same cartridge still finds the list by serial.
        assertEquals("Pokemon - Emerald Version (USA, Europe)", db.lookup(game("DEADBEEF", "BPEE"))?.title)
        // A fan game built on Emerald must not get Emerald's codes...
        assertNull(db.lookup(game("12345678", "BPEE", GameSource.PATCHED)))
        // ...but gets its own list when there is one.
        assertEquals("Pokemon Unbound", db.lookup(game("12345678", "BPRE", GameSource.PATCHED, "pokemon-unbound"))?.title)
    }

    @Test fun missingAssetsAreEmptyNotFatal() {
        assertNull(CheatDatabase { null }.lookup(game("1F1C08FB", "BPEE")))
        assertNull(GameIndex { null }.byCrc("1F1C08FB"))
    }

    @Test fun gameIndexReadsTsv() {
        val idx = GameIndex { ByteArrayInputStream(gz("crc32\tserial\tname\n1F1C08FB\tBPEE\tPokemon - Emerald Version (USA, Europe)\n")) }
        assertEquals("BPEE", idx.byCrc("1f1c08fb")?.serial)
    }

    @Test fun boxArtUrlsUseLibretroNaming() {
        assertEquals("Mario _ Luigi - Superstar Saga (USA)", BoxArtRepository.thumbnailName("Mario & Luigi - Superstar Saga (USA)"))
        val urls = BoxArtRepository.urls("Pokemon - Emerald Version (USA, Europe)")
        assertEquals(
            "https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/Pokemon%20-%20Emerald%20Version%20%28USA%2C%20Europe%29.png",
            urls.first(),
        )
    }
}
