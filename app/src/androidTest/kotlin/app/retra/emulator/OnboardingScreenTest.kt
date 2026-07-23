package app.retra.emulator

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.retra.core.model.ThemeMode
import app.retra.emulator.ui.theme.RetraTheme
import org.junit.Rule
import org.junit.Test

/**
 * Smoke coverage for the Compose theme path used by Retra 2.3 onboarding.
 * The old [OnboardingScreen] composable was removed with the V23 shell.
 */
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeRendersBrandCopy() {
        composeRule.setContent {
            RetraTheme(ThemeMode.DARK, dynamicColor = false) {
                Text("Your games. Your saves. One quiet archive.")
            }
        }
        composeRule.onNodeWithText("Your games. Your saves. One quiet archive.").assertIsDisplayed()
    }
}
