package app.retra.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import app.retra.core.ChallengeMode
import app.retra.core.ChallengeRun
import app.retra.data.AppPalette
import app.retra.data.ControlSkin
import app.retra.data.Settings
import app.retra.engine.GbaKey
import app.retra.ui.components.GlassSurface
import app.retra.ui.games.SealCard
import app.retra.ui.home.HomeContent
import app.retra.ui.home.HomeState
import app.retra.ui.onboarding.OnboardingScreen
import app.retra.ui.player.BitmapScreen
import app.retra.ui.player.MenuPage
import app.retra.ui.player.PlayerCallbacks
import app.retra.ui.player.PlayerContent
import app.retra.ui.player.PlayerStatus
import app.retra.ui.player.PlayerUiState
import app.retra.ui.player.SkinConfig
import app.retra.ui.player.SkinPreview
import app.retra.ui.settings.PalettePicker
import app.retra.ui.settings.SkinPicker
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots for the customisation, onboarding and challenge work. Recorded PNGs go to docs/screenshots. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w393dp-h852dp-xxhdpi")
class NewFeaturesTest(private val dark: Boolean) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule val compose = createComposeRule()

    @Test fun onboarding_shelf() = compose.snap("onboarding-shelf", dark) { OnboardingScreen({}, {}, {}, initialPage = 1) }

    @Test fun onboarding_palette() = compose.snap("onboarding-palette", dark, palette = AppPalette.SAKURA) {
        OnboardingScreen({}, {}, {}, initialPage = 2, settings = Settings(palette = AppPalette.SAKURA))
    }

    @Test fun onboarding_play() = compose.snap("onboarding-play", dark) { OnboardingScreen({}, {}, {}, initialPage = 3) }

    @Test fun onboarding_cloud() = compose.snap("onboarding-cloud", dark) { OnboardingScreen({}, {}, {}, initialPage = 4) }

    @Test fun palettes() = compose.snap("palettes", dark) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Colour worlds", style = RetraType.title, color = Retra.colors.ink)
            PalettePicker(AppPalette.OCEAN, {})
            Text("Button skins", style = RetraType.title, color = Retra.colors.ink)
            ControlSkin.entries.forEach { s ->
                GlassSurface(Modifier.padding(0.dp), shape = Radius.mediumShape, strong = true) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(s.label, style = RetraType.caption, color = Retra.colors.inkMuted, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                        SkinPreview(SkinConfig(s, null, true), Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
    }

    @Test fun skin_picker() = compose.snap("skin-picker", dark) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
            SkinPicker(Settings(controlSkin = ControlSkin.NEON, controlTint = 0xFFE8628F), {})
        }
    }

    @Test fun home_sakura() = compose.snap("home-sakura", dark, tab = 0, palette = AppPalette.SAKURA) {
        run { HomeContent(HomeState(greeting = "Good evening", featured = Fakes.catalog.entries.filter { it.featured }, loaded = true)) }
    }

    @Test fun home_pocket() = compose.snap("home-pocket", dark, tab = 0, palette = AppPalette.POCKET) {
        run { HomeContent(HomeState(greeting = "Good evening", featured = Fakes.catalog.entries.filter { it.featured }, loaded = true)) }
    }

    private fun player(skin: ControlSkin, name: String, tint: Long = 0, palette: AppPalette = AppPalette.MINT, keys: Int = GbaKey.A or GbaKey.RIGHT) {
        val frame = Fakes.frame()
        compose.snap(name, dark, palette = palette) {
            PlayerContent(
                state = PlayerUiState(status = PlayerStatus.Running, title = "Mochi Hop"),
                settings = Settings(controlSkin = skin, controlTint = tint, showFps = true),
                gamepadConnected = false, fps = 59.7f, origin = "home", gameId = "a1",
                callbacks = PlayerCallbacks(),
                screen = { m -> BitmapScreen(frame, smooth = false, modifier = m) },
                pressedPreview = keys,
            )
        }
    }

    @Test fun player_neon() = player(ControlSkin.NEON, "player-neon", tint = 0xFF4DF3FF, palette = AppPalette.LAVENDER)

    @Test fun player_classic() = player(ControlSkin.CLASSIC, "player-classic")

    @Test fun player_candy() = player(ControlSkin.CANDY, "player-candy", palette = AppPalette.SAKURA)

    @Test fun player_challenge() {
        val frame = Fakes.frame()
        val run = ChallengeRun(ChallengeMode.NUZLOCKE, startedAt = 0, blockedAttempts = 3)
        compose.snap("player-challenge", dark) {
            PlayerContent(
                state = PlayerUiState(status = PlayerStatus.Running, title = "Pokémon Emerald", menuOpen = true, challenge = run),
                settings = Settings(),
                gamepadConnected = false, fps = 59.7f, origin = "home", gameId = "a1",
                callbacks = PlayerCallbacks(challengePanel = { SealCard(run, 5_400_000) }),
                screen = { m -> BitmapScreen(frame, smooth = false, modifier = m) },
                menuPage = MenuPage.CHALLENGE,
            )
        }
    }
}
