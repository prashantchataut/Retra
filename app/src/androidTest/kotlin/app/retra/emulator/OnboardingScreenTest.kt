
package app.retra.emulator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.retra.core.model.ThemeMode
import app.retra.emulator.ui.theme.RetraTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsBrandAndLegalNotice() {
        composeRule.setContent {
            RetraTheme(ThemeMode.DARK, dynamicColor = false) { OnboardingScreen(onContinue = {}) }
        }
        composeRule.onNodeWithText("Retra").assertIsDisplayed()
        composeRule.onNodeWithText("Retra does not include commercial games or proprietary BIOS files.").assertIsDisplayed()
    }
}
