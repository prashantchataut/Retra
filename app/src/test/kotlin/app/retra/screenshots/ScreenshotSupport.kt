package app.retra.screenshots

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import app.retra.ui.components.Dock
import app.retra.ui.components.SheetHost
import app.retra.ui.components.SheetLayer
import app.retra.ui.components.LocalSheetHost
import androidx.compose.runtime.remember
import app.retra.ui.components.LocalHazeState
import app.retra.ui.nav.dockItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import app.retra.data.CatalogParser
import app.retra.data.Catalog
import app.retra.data.Game
import app.retra.data.GameCategory
import app.retra.data.GameSource
import app.retra.data.Provenance
import app.retra.ui.GameUi
import app.retra.ui.coverModel
import app.retra.ui.components.AmbientSky
import app.retra.ui.components.CoverImages
import app.retra.ui.components.CoverModel
import app.retra.ui.theme.RetraTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import java.io.File

/** Where the PR screenshots go (repo docs/), relative to the :app module dir. */
const val SHOTS = "../docs/screenshots"

val shotOptions = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))

fun ComposeContentTestRule.snap(name: String, dark: Boolean, tab: Int? = null, content: @Composable () -> Unit) {
    setContent {
        RetraTheme(dark = dark, reduceMotion = true) {
            Shell(tab, content)
        }
    }
    waitForIdle()
    mainClock.advanceTimeBy(800)
    waitForIdle()
    onRoot().captureRoboImage("$SHOTS/$name-${if (dark) "dark" else "light"}.png", roborazziOptions = shotOptions)
}

object Fakes {
    val context: Context get() = ApplicationProvider.getApplicationContext()
    private val now = System.currentTimeMillis()

    /** A fake "last session" frame: the Mochi Hop cover scaled to 240x160 (the native core can't run on the JVM). */
    fun frame(): ImageBitmap {
        val full = context.assets.open("games/mochi_hop_cover.png").use { BitmapFactory.decodeStream(it) }
        return android.graphics.Bitmap.createScaledBitmap(full, 240, 160, false).asImageBitmap()
    }

    fun screenFile(name: String): File {
        val f = File(context.cacheDir, "$name.png")
        if (!f.exists()) context.assets.open("games/mochi_hop_cover.png").use { i -> f.outputStream().use { i.copyTo(it) } }
        return f
    }

    fun game(
        id: String, title: String, code: String, crc: String, category: GameCategory, source: GameSource = GameSource.IMPORTED,
        played: Long? = null, playtime: Long = 0, fav: Boolean = false, cover: String? = null, provenance: Provenance? = null,
    ) = Game(
        id = id, title = title, romTitle = title.uppercase().take(12), gameCode = code, makerCode = "01", sizeBytes = 16L * 1024 * 1024,
        crc32 = crc, sha1 = "3a1e6c2b9d7f4e5a8c0b1d2e3f4a5b6c7d8e9f00", addedAt = now - 86_400_000L * 9, lastPlayedAt = played,
        playTimeMs = playtime, favorite = fav, source = source, category = category, coverAsset = cover, provenance = provenance,
        saveType = "FLASH_128K", hasRtc = category == GameCategory.POKEMON,
    )

    val mochi = game("a1", "Mochi Hop", "RMHE", "6F1C2A90", GameCategory.HOMEBREW, GameSource.BUNDLED, played = now - 3_600_000L * 2, playtime = 3_600_000L * 3 + 720_000, fav = true, cover = "games/mochi_hop_cover.png")
    val emerald = game("b2", "Pokémon Emerald", "BPEE", "1F1C08FB", GameCategory.POKEMON, played = now - 86_400_000L, playtime = 3_600_000L * 41)
    val hns = game(
        "c3", "Pokémon Heart & Soul", "BPEE", "4C0B2E11", GameCategory.FAN_GAME, GameSource.PATCHED, played = now - 86_400_000L * 3, playtime = 3_600_000L * 12,
        provenance = Provenance("Pokémon Heart & Soul 1.2.1", "ups", "Pokémon Emerald", "1F1C08FB", "pokemon-heart-and-soul", now - 86_400_000L * 4),
    )
    val skyland = game("d4", "Skyland", "SKYL", "77AA0042", GameCategory.HOMEBREW, GameSource.DOWNLOADED, fav = true)
    val apotris = game("e5", "Apotris", "APOT", "1288FE10", GameCategory.HOMEBREW, GameSource.DOWNLOADED, played = now - 86_400_000L * 6, playtime = 1_800_000)
    val fireRed = game("f6", "Pokémon FireRed", "BPRE", "DD88761C", GameCategory.POKEMON)

    val games = listOf(mochi, emerald, hns, skyland, apotris, fireRed)

    fun ui(g: Game, withShot: Boolean = false): GameUi {
        val shot = if (withShot) screenFile("shot-${g.id}") else null
        return GameUi(g, g.coverModel(shot), hasAutoState = g.lastPlayedAt != null, lastScreenPath = shot?.absolutePath)
    }

    val catalog: Catalog by lazy {
        val json = File("../catalog/manifest.json").readText()
        (CatalogParser.parse(json) as CatalogParser.Result.Ok).catalog
    }

    /** Decodes covers synchronously so screenshots don't race the IO dispatcher. */
    fun preload(vararg models: CoverModel) = runBlocking { models.forEach { CoverImages.load(context, it) } }
}

/** The app shell for screenshots: sky + content as the blur source, the dock, and the sheet layer. */
@Composable
fun Shell(tab: Int?, content: @Composable () -> Unit) {
    val haze = rememberHazeState()
    val sheets = remember { SheetHost() }
    CompositionLocalProvider(LocalSheetHost provides sheets) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                AmbientSky()
                content()
            }
            CompositionLocalProvider(LocalHazeState provides haze) {
                if (tab != null && !sheets.anyVisible) {
                    Dock(
                        dockItems, tab, {},
                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
                SheetLayer(sheets)
            }
        }
    }
}
