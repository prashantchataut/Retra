package app.retra.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import app.retra.data.CatalogSource
import app.retra.data.Cheat
import app.retra.data.Settings
import app.retra.data.StateSlot
import app.retra.engine.GbaKey
import app.retra.ui.coverModel
import app.retra.ui.games.DetailsState
import app.retra.ui.games.GameDetailsContent
import app.retra.ui.games.GamesState
import app.retra.ui.games.MyGamesContent
import app.retra.ui.home.HomeContent
import app.retra.ui.home.HomeState
import app.retra.ui.onboarding.OnboardingScreen
import app.retra.ui.patcher.BaseInfo
import app.retra.ui.patcher.PatchPhase
import app.retra.ui.patcher.PatchUi
import app.retra.ui.patcher.PatcherContent
import app.retra.ui.patcher.PatcherState
import app.retra.ui.player.BitmapScreen
import app.retra.ui.player.MenuPage
import app.retra.ui.player.PlayerCallbacks
import app.retra.ui.player.PlayerContent
import app.retra.ui.player.PlayerStatus
import app.retra.ui.player.PlayerUiState
import app.retra.ui.settings.LicenceItem
import app.retra.ui.settings.SettingsContent
import app.retra.ui.settings.StorageUsage
import app.retra.ui.store.EntryJob
import app.retra.ui.store.StoreContent
import app.retra.ui.store.StoreState
import org.junit.Before
import app.retra.ui.games.CheatBook
import app.retra.data.CheatPackCheat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots of every screen in light and dark. Recorded PNGs go to docs/screenshots. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w393dp-h852dp-xxhdpi")
class ScreensTest(private val dark: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule val compose = createComposeRule()

    @Before
    fun preload() {
        Fakes.preload(Fakes.mochi.coverModel(null), Fakes.ui(Fakes.mochi).cover)
        Fakes.preload(*Fakes.catalog.entries.map { it.coverModel() }.toTypedArray())
    }

    private val now = System.currentTimeMillis()

    @Test fun onboarding_welcome() = compose.snap("onboarding-1", dark) { OnboardingScreen({}, {}, {}) }

    @Test fun onboarding_last() = compose.snap("onboarding-3", dark) { OnboardingScreen({}, {}, {}, initialPage = 2) }

    @Test fun home_empty() = compose.snap("home-empty", dark, tab = 0) {
        run { HomeContent(HomeState(greeting = "Good evening", featured = Fakes.catalog.entries.filter { it.featured }, loaded = true)) }
    }

    @Test fun home() {
        val hero = Fakes.ui(Fakes.mochi, withShot = true)
        Fakes.preload(hero.cover)
        compose.snap("home", dark, tab = 0) {
            run {
                HomeContent(
                    HomeState(
                        greeting = "Good evening",
                        continueGame = hero,
                        recent = listOf(Fakes.emerald, Fakes.hns, Fakes.apotris, Fakes.skyland).map { Fakes.ui(it) },
                        featured = Fakes.catalog.entries.filter { it.featured },
                        ready = Fakes.catalog.entries.filter { it.isPatch && it.base?.crc32 == "1F1C08FB" }.take(3),
                        gameCount = 6, loaded = true,
                    ),
                )
            }
        }
    }

    @Test fun games_grid() = compose.snap("my-games", dark, tab = 1) {
        run { MyGamesContent(GamesState(games = Fakes.games.map { Fakes.ui(it) }, total = 6, loaded = true)) }
    }

    @Test fun games_list_actions() = compose.snap("my-games-actions", dark, tab = 1) {
        run {
            MyGamesContent(
                GamesState(games = Fakes.games.map { Fakes.ui(it) }, total = 6, grid = false, loaded = true),
                initialSheetFor = Fakes.ui(Fakes.emerald),
            )
        }
    }

    @Test fun details() = compose.snap("game-details", dark) {
        GameDetailsContent(
            DetailsState(
                game = Fakes.ui(Fakes.hns),
                slots = listOf(
                    StateSlot(0, true, now - 3_600_000, Fakes.screenFile("slot0").absolutePath),
                    StateSlot(1, true, now - 86_400_000, Fakes.screenFile("slot1").absolutePath),
                    StateSlot(2, true, now - 86_400_000 * 2, null),
                ),
                cheats = listOf(Cheat("1", "Infinite money", "82025BC4 FFFF", true), Cheat("2", "Walk through walls", "509197D3 C2A1F5F2\n1C88D3E2 7A2B1C08", false)),
                saveBytes = 131072, backups = 3,
            ),
            origin = "games",
        )
    }

    private fun storeState(jobs: Map<String, EntryJob> = emptyMap(), ready: Set<String> = emptySet(), owned: Boolean = true) = StoreState(
        catalog = Fakes.catalog, source = CatalogSource.ONLINE,
        installed = mapOf("mochi-hop" to "a1"),
        ownedBases = if (owned) mapOf("1F1C08FB" to ("b2" to "Pokémon Emerald")) else emptyMap(),
        patchesReady = ready, jobs = jobs,
    )

    @Test fun store() = compose.snap("game-library", dark, tab = 2) {
        run { StoreContent(storeState(), selectedId = null, onSelect = {}) }
    }

    @Test fun store_entry_creator() = compose.snap("game-library-creator-patch", dark) {
        StoreContent(storeState(), selectedId = "pokemon-radical-red", onSelect = {})
    }

    @Test fun store_make_game() = compose.snap("patch-flow-1-ready", dark) {
        StoreContent(storeState(ready = setOf("pokemon-heart-and-soul")), selectedId = "pokemon-heart-and-soul", onSelect = {})
    }

    @Test fun store_make_game_need_base() = compose.snap("patch-flow-0-download", dark) {
        StoreContent(storeState(jobs = mapOf("pokemon-heart-and-soul" to EntryJob.Downloading(0.42f)), owned = false), selectedId = "pokemon-heart-and-soul", onSelect = {})
    }

    @Test fun store_need_base() = compose.snap("patch-flow-0-need-base", dark) {
        StoreContent(storeState(owned = false), selectedId = "pokemon-heart-and-soul", onSelect = {})
    }

    @Test fun store_make_game_patching() = compose.snap("patch-flow-2-patching", dark) {
        StoreContent(storeState(jobs = mapOf("pokemon-heart-and-soul" to EntryJob.Patching(0.64f)), ready = setOf("pokemon-heart-and-soul")), selectedId = "pokemon-heart-and-soul", onSelect = {})
    }

    @Test fun store_make_game_done() = compose.snap("patch-flow-3-done", dark) {
        StoreContent(storeState(jobs = mapOf("pokemon-heart-and-soul" to EntryJob.Done("c3")), ready = setOf("pokemon-heart-and-soul")), selectedId = "pokemon-heart-and-soul", onSelect = {})
    }

    @Test fun store_rom() = compose.snap("game-library-homebrew", dark) {
        StoreContent(storeState(), selectedId = "apotris", onSelect = {})
    }

    private val base = BaseInfo("Pokémon Emerald", 16L * 1024 * 1024, "1F1C08FB", "BPEE", "b2")
    private val patch = PatchUi("pokemonHnS_v1.2.1.ups", 32_558_217, "UPS", 16L * 1024 * 1024, 32L * 1024 * 1024, "1F1C08FB", "4C0B2E11", null)

    @Test fun patcher_empty() = compose.snap("patcher-empty", dark, tab = 3) {
        run { PatcherContent(PatcherState(), Fakes.games) }
    }

    @Test fun patcher_ready() = compose.snap("patcher-ready", dark, tab = 3) {
        run { PatcherContent(PatcherState(base, patch, outputName = "Pokémon Heart & Soul"), Fakes.games) }
    }

    @Test fun patcher_mismatch() = compose.snap("patcher-mismatch", dark, tab = 3) {
        run { PatcherContent(PatcherState(base.copy(name = "Pokémon Emerald (Europe)", crc32 = "A1B2C3D4"), patch, outputName = "Pokémon Heart & Soul"), Fakes.games) }
    }

    @Test fun patcher_applying() = compose.snap("patcher-applying", dark, tab = 3) {
        run { PatcherContent(PatcherState(base, patch, phase = PatchPhase.Applying(0.58f)), Fakes.games) }
    }

    @Test fun patcher_done() = compose.snap("patcher-done", dark, tab = 3) {
        run { PatcherContent(PatcherState(base, patch, phase = PatchPhase.Done(32L * 1024 * 1024, "4C0B2E11", emptyList(), null)), Fakes.games) }
    }

    @Test fun patcher_error() = compose.snap("patcher-error", dark, tab = 3) {
        run {
            PatcherContent(
                PatcherState(base, patch.copy(name = "broken.bps", format = "BPS"), phase = PatchPhase.Failed("The BPS patch is damaged or incomplete (it ends unexpectedly). Try downloading it again.")),
                Fakes.games,
            )
        }
    }

    private fun player(menu: Boolean, page: MenuPage = MenuPage.MAIN, keys: Int = 0) {
        val frame = Fakes.frame()
        compose.snap(
            when {
                menu && page == MenuPage.CHEATS -> "player-cheats"
                menu && page == MenuPage.EXIT -> "player-exit"
                menu -> "player-menu"
                else -> "player"
            },
            dark,
        ) {
            PlayerContent(
                state = PlayerUiState(
                    status = PlayerStatus.Running, title = "Mochi Hop", menuOpen = menu, fastForward = !menu,
                    slots = listOf(
                        StateSlot(0, true, now - 600_000, Fakes.screenFile("slot0").absolutePath),
                        StateSlot(1, true, now - 3_600_000, Fakes.screenFile("slot1").absolutePath),
                    ) + (2..6).map { StateSlot(it, false, null, null) },
                    cheats = listOf(Cheat("1", "Infinite hearts", "0300203C:03", true)),
                    cheatBook = CheatBook(
                        "Mochi Hop",
                        listOf(
                            CheatPackCheat("Master code", "D8BAE4D9 4864DCE5", master = true),
                            CheatPackCheat("Infinite hearts", "0300203C:03"),
                            CheatPackCheat("Every level unlocked", "0300204A:06"),
                            CheatPackCheat("99 stars", "03002050:63"),
                            CheatPackCheat("Moon jump (hold A)", "74000130 03FE\n0300201C:F0"),
                        ),
                    ),
                ),
                settings = Settings(showFps = true),
                gamepadConnected = false,
                fps = 59.7f,
                origin = "home",
                gameId = "a1",
                callbacks = PlayerCallbacks(),
                screen = { m -> BitmapScreen(frame, smooth = false, modifier = m) },
                menuPage = page,
                pressedPreview = keys,
            )
        }
    }

    @Test fun player_portrait() = player(menu = false, keys = GbaKey.A or GbaKey.RIGHT)

    @Test fun player_menu() = player(menu = true)

    @Test fun player_cheats() = player(menu = true, page = MenuPage.CHEATS)

    @Test fun player_exit() = player(menu = true, page = MenuPage.EXIT)

    @Test fun settings() = compose.snap("settings", dark) {
        SettingsContent(
            Settings(rtcOffsetSeconds = 3600 * 5), biosPresent = false,
            storage = StorageUsage(52_428_800, 393_216, 4_718_592, 1_048_576, 0),
            licences = listOf(LicenceItem("Fraunces", "Display typeface", "OFL-1.1")),
        )
    }
}

/** Landscape player, separate class for the landscape qualifier. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w852dp-h393dp-land-xxhdpi")
class PlayerLandscapeTest(private val dark: Boolean) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule val compose = createComposeRule()

    @Test fun player_landscape() {
        val frame = Fakes.frame()
        compose.snap("player-landscape", dark) {
            PlayerContent(
                state = PlayerUiState(status = PlayerStatus.Running, title = "Mochi Hop"),
                settings = Settings(showFps = true),
                gamepadConnected = false, fps = 59.7f, origin = "home", gameId = "a1",
                callbacks = PlayerCallbacks(),
                screen = { m -> BitmapScreen(frame, smooth = false, modifier = m) },
                pressedPreview = GbaKey.B or GbaKey.UP,
            )
        }
    }
}
