package app.retra.emulator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.retra.core.model.AppSettings
import app.retra.emulator.ui.components.RetraEmptyState
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.RetraTheme
import org.junit.Rule
import org.junit.Test

/**
 * Smoke coverage for Archive Glass Operate chrome.
 */
class RetraChromeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pageTitleRendersOperateCopy() {
        composeRule.setContent {
            RetraTheme(AppSettings()) {
                RetraPageTitle(
                    title = "Home",
                    subtitle = "Continue where you left off."
                )
            }
        }
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Continue where you left off.").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsPrimaryAction() {
        composeRule.setContent {
            RetraTheme(AppSettings()) {
                RetraEmptyState(
                    title = "No games yet",
                    body = "Import a GBA file you are allowed to use.",
                    primaryLabel = "Import file",
                    onPrimary = {}
                )
            }
        }
        composeRule.onNodeWithText("No games yet").assertIsDisplayed()
        composeRule.onNodeWithText("Import file").assertIsDisplayed()
    }

    @Test
    fun sectionHeaderShowsAction() {
        composeRule.setContent {
            RetraTheme(AppSettings()) {
                RetraSectionHeader(title = "Recent", action = "Library", onAction = {})
            }
        }
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("Library").assertIsDisplayed()
    }
}
