package app.retra.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class Tab { HOME, GAMES, STORE, PATCHER, SETTINGS }

/** Navigation + global actions available to every screen. */
interface AppActions {
    fun importGames()
    fun openGame(id: String, origin: String)
    fun play(id: String, resume: Boolean, origin: String) = playSlot(id, if (resume) 0 else -1, origin)
    /** Starts a game; [slot] -1 boots normally, 0 is the auto (Continue) state, 1-6 manual states. */
    fun playSlot(id: String, slot: Int, origin: String)
    fun openStore(entryId: String? = null)
    fun openPatcher(baseGameId: String? = null)
    fun openTab(tab: Tab)
    fun openUrl(url: String)
    fun back()
}

object NoopActions : AppActions {
    override fun importGames() {}
    override fun openGame(id: String, origin: String) {}
    override fun playSlot(id: String, slot: Int, origin: String) {}
    override fun openStore(entryId: String?) {}
    override fun openPatcher(baseGameId: String?) {}
    override fun openTab(tab: Tab) {}
    override fun openUrl(url: String) {}
    override fun back() {}
}

val LocalAppActions = staticCompositionLocalOf<AppActions> { NoopActions }

/** Space screens must leave at the bottom so content can scroll above the floating dock. */
val LocalDockPadding = staticCompositionLocalOf { PaddingValues(bottom = 112.dp) }
