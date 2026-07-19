package app.retra.emulator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.retra.core.model.AppSettings
import app.retra.core.model.ThemeMode
import app.retra.emulator.auth.AuthOperation
import app.retra.emulator.ui.theme.RetraTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsBrandAndPrivateFirstMessage() {
        composeRule.setContent {
            RetraTheme(ThemeMode.DARK, dynamicColor = false) {
                OnboardingScreen(
                    settings = AppSettings(themeMode = ThemeMode.DARK),
                    account = null,
                    authOperation = AuthOperation.IDLE,
                    googleConfigured = false,
                    onThemeChanged = {},
                    onAccentChanged = {},
                    onGoogleSignIn = {},
                    onComplete = {}
                )
            }
        }
        composeRule.onNodeWithText("Retra").assertIsDisplayed()
        composeRule.onNodeWithText("Private by default").assertIsDisplayed()
    }
}
