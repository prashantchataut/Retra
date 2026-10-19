package app.retra.data

import app.retra.core.RomHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun fakeRom(title: String, code: String, size: Int = 4096, seed: Int = 0): ByteArray {
    val rom = ByteArray(size) { ((it * 31 + seed) and 0xFF).toByte() }
    rom[3] = 0xEA.toByte()
    for (i in 0 until 12) rom[0xA0 + i] = (title.getOrNull(i)?.code ?: 0).toByte()
    for (i in 0 until 4) rom[0xAC + i] = code[i].code.toByte()
    rom[0xB0] = '0'.code.toByte(); rom[0xB1] = '1'.code.toByte()
    rom[0xB2] = 0x96.toByte()
    rom[0xBC] = 1
    return rom
}

fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
    return out.toByteArray()
}

class LibraryRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000L
    private fun repo() = LibraryRepository(tmp.root, Dispatchers.Unconfined, clock = { now })

    @Test fun parsesHeader() {
        val h = RomHeader.parse(fakeRom("POKEMON EMER", "BPEE"))!!
        assertEquals("POKEMON EMER", h.title)
        assertEquals("BPEE", h.gameCode)
        assertEquals("01", h.makerCode)
        assertEquals(1, h.version)
        assertTrue(h.looksValid)
    }

    @Test fun importsAndClassifiesPokemon() = runTest {
        val r = repo().import(fakeRom("POKEMON EMER", "BPEE"), "Pokemon - Emerald (USA).gba")
        val g = (r as ImportResult.Added).game
        assertEquals("Pokemon - Emerald", g.title)
        assertEquals("BPEE", g.gameCode)
        assertEquals(GameCategory.POKEMON, g.category)
        assertEquals(16, g.id.length)
        assertTrue(tmp.root.resolve("games/${g.id}.gba").exists())
    }

    @Test fun dedupesBySha1() = runTest {
        val repo = repo()
        val rom = fakeRom("MOCHI HOP", "RMHE")
        val first = repo.import(rom, "a.gba") as ImportResult.Added
        val second = repo.import(rom.copyOf(), "renamed copy.gba")
        assertTrue(second is ImportResult.Duplicate)
        assertEquals(first.game.id, (second as ImportResult.Duplicate).game.id)
        assertEquals(1, repo.games.value.size)
        // A different ROM is a new game.
        assertTrue(repo.import(fakeRom("MOCHI HOP", "RMHE", seed = 7), "b.gba") is ImportResult.Added)
        assertEquals(2, repo.games.value.size)
    }

    @Test fun extractsRomFromZipAndDedupesAgainstPlainFile() = runTest {
        val repo = repo()
        val rom = fakeRom("SKYLAND", "SKYL")
        val zip = zipOf("readme.txt" to "hi".toByteArray(), "game/Skyland.gba" to rom)
        val g = (repo.import(zip, "skyland.zip") as ImportResult.Added).game
        assertEquals("Skyland", g.title)
        assertTrue(repo.import(rom, "Skyland.gba") is ImportResult.Duplicate)
    }

    @Test fun rejectsNonGames() = runTest {
        val repo = repo()
        assertTrue(repo.import(ByteArray(10), "tiny.gba") is ImportResult.Error)
        assertTrue(repo.import(zipOf("notes.txt" to ByteArray(500)), "x.zip") is ImportResult.Error)
        assertTrue(repo.import(ByteArray(4096), "photo.jpg") is ImportResult.Error)
    }

    @Test fun persistsEditsAndPlaytime() = runTest {
        val repo = repo()
        val g = (repo.import(fakeRom("APOTRIS", "APOT"), "apotris.gba") as ImportResult.Added).game
        repo.rename(g.id, "  Apotris  ")
        repo.setFavorite(g.id, true)
        now = 5_000
        repo.recordSession(g.id, 60_000)
        repo.recordSession(g.id, 30_000)
        repo.recordSession(g.id, -5) // ignored
        val reloaded = repo().game(g.id)
        assertNotNull(reloaded)
        assertEquals("Apotris", reloaded!!.title)
        assertTrue(reloaded.favorite)
        assertEquals(90_000L, reloaded.playTimeMs)
        assertEquals(5_000L, reloaded.lastPlayedAt)
    }

    @Test fun deleteRemovesRomFile() = runTest {
        val repo = repo()
        val g = (repo.import(fakeRom("APOTRIS", "APOT"), "apotris.gba") as ImportResult.Added).game
        repo.delete(g.id)
        assertTrue(repo.games.value.isEmpty())
        assertTrue(!tmp.root.resolve("games/${g.id}.gba").exists())
    }

    @Test fun patchedGamesAreFanGamesWithProvenance() = runTest {
        val repo = repo()
        val prov = Provenance("Heart & Soul", "ups", "Pokémon Emerald", "1F1C08FB", "pokemon-heart-and-soul", 1)
        val g = (repo.addRom(fakeRom("POKEMON EMER", "BPEE", seed = 3), "hns.gba", ImportOrigin(GameSource.PATCHED, title = "Pokémon Heart & Soul", provenance = prov)) as ImportResult.Added).game
        assertEquals(GameCategory.FAN_GAME, g.category)
        assertEquals(prov, repo().game(g.id)!!.provenance)
        assertEquals(g.id, repo.findByCrc(g.crc32)?.id)
    }

    @Test fun knownHomebrewIsTaggedByCatalogSha() = runTest {
        val rom = fakeRom("MINICRAFT", "MCRF")
        val sha = app.retra.patcher.Checksums.sha256Hex(rom)
        val repo = LibraryRepository(tmp.root, Dispatchers.Unconfined, homebrewLookup = { if (it == sha) "minicraft-gba" else null })
        val g = (repo.import(rom, "minicraft.gba") as ImportResult.Added).game
        assertEquals(GameCategory.HOMEBREW, g.category)
        assertEquals("minicraft-gba", g.catalogId)
    }
}
