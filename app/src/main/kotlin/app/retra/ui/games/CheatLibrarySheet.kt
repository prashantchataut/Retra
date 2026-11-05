package app.retra.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.AppContainer
import app.retra.core.CheatImport
import app.retra.data.Cheat
import app.retra.data.MessageTone
import app.retra.data.OpenRouterClient
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.NavRow
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.Segmented
import app.retra.ui.components.SwitchRow
import app.retra.ui.components.Tag
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.launch

private enum class LibTab { THIS, BROWSE, ONLINE }

/**
 * Retra's cheat library in one sheet: the built-in list for this game, a browser over every game in the built-in
 * library (handy for another region or a ROM hack of the same base), and an online search through OpenRouter.
 * Everything added lands switched off.
 */
@Composable
fun CheatLibrarySheet(
    container: AppContainer,
    gameId: String,
    existing: List<Cheat>,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<Cheat>) -> Unit,
) {
    val c = Retra.colors
    val scope = rememberCoroutineScope()
    val games by container.library.games.collectAsStateWithLifecycle()
    val game = games.firstOrNull { it.id == gameId }
    var tab by remember { mutableStateOf(LibTab.THIS) }
    var query by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<app.retra.core.OpenRouterCheats.Suggested>>(emptyList()) }
    val picked = remember { mutableListOf<Boolean>().toMutableStateList() }
    val book = remember(game) { container.cheatBook(game) }

    fun add(list: List<CheatImport.Parsed>) = onSave(container.mergeImported(existing, CheatImport.Result(list, 0)))

    GlassSheet(visible, onDismiss, title = "Cheat library") {
        Text(
            "${container.cheatBooks.gameCount} games and ${container.cheatBooks.cheatCount} codes built in. For anything else, search online.",
            style = RetraType.label, color = c.inkMuted,
        )
        Spacer(Modifier.height(Space.xs))
        Segmented(listOf(LibTab.THIS to "This game", LibTab.BROWSE to "Browse", LibTab.ONLINE to "Online"), tab, { tab = it })
        Spacer(Modifier.height(Space.s))
        when (tab) {
            LibTab.THIS -> {
                if (book == null || book.cheats.isEmpty()) {
                    Text("Retra has no built-in list for ${game?.title ?: "this game"}. Browse another region or version, or search online.", style = RetraType.body, color = c.inkMuted)
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        RetraButton("Browse", { tab = LibTab.BROWSE }, style = ButtonStyle.SECONDARY, compact = true)
                        RetraButton("Search online", { tab = LibTab.ONLINE }, compact = true, icon = Icons.Rounded.AutoAwesome)
                    }
                } else {
                    Text("${book.cheats.size} codes for ${book.title}", style = RetraType.bodyStrong, color = c.ink)
                    Spacer(Modifier.height(Space.xs))
                    RetraButton("Add all (switched off)", { add(book.cheats.map { CheatImport.Parsed(it.name, it.code) }) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Space.xs))
                    Text("Or pick single codes in the Cheats list; the cheat book is shown under it.", style = RetraType.caption, color = c.inkMuted)
                }
            }
            LibTab.BROWSE -> {
                RetraTextField(query, { query = it }, "Search ${container.cheatBooks.gameCount} games", leadingIcon = Icons.Rounded.Search)
                Spacer(Modifier.height(Space.xs))
                val hits = remember(query) { container.cheatBooks.search(query, 40) }
                GlassGroup {
                    hits.forEachIndexed { i, g ->
                        NavRow(g.title, { add(g.cheats.map { CheatImport.Parsed(it.name, it.code) }) }, trailing = "${g.cheats.size} codes", subtitle = g.serials.joinToString(" · ").ifBlank { null })
                        if (i < hits.lastIndex) Divider()
                    }
                }
                if (hits.isEmpty()) Text("Nothing matches “$query”.", style = RetraType.body, color = c.inkMuted)
                Spacer(Modifier.height(Space.xs))
                Text("Tapping a game adds its codes to this game. Addresses only line up when it's the same game or a close version.", style = RetraType.caption, color = c.inkMuted)
            }
            LibTab.ONLINE -> {
                if (!container.openRouter.hasKey) {
                    Text("Add your OpenRouter API key in Settings → Cheats, cloud and friends to search the web for codes.", style = RetraType.body, color = c.inkMuted)
                } else {
                    RetraTextField(request, { request = it }, "What do you want? (optional) e.g. infinite money", leadingIcon = Icons.Rounded.AutoAwesome)
                    Spacer(Modifier.height(Space.xs))
                    RetraButton(
                        if (searching) "Searching the web…" else "Search for ${game?.title ?: "cheats"}",
                        {
                            val g = game ?: return@RetraButton
                            scope.launch {
                                searching = true; error = null
                                when (val r = container.openRouter.search(g, request)) {
                                    is OpenRouterClient.Outcome.Found -> {
                                        results = r.parsed.cheats
                                        picked.clear(); picked.addAll(results.map { true })
                                    }
                                    is OpenRouterClient.Outcome.Failed -> { error = r.message; results = emptyList() }
                                }
                                searching = false
                            }
                        },
                        Modifier.fillMaxWidth(), enabled = !searching && game != null, icon = Icons.Rounded.AutoAwesome,
                    )
                    error?.let { Text(it, style = RetraType.label, color = c.danger, modifier = Modifier.padding(top = Space.xs)) }
                    if (results.isNotEmpty()) {
                        Spacer(Modifier.height(Space.s))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${results.size} suggestions", style = RetraType.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
                            Tag("AI-suggested · unverified", color = c.warning)
                        }
                        Spacer(Modifier.height(Space.xxs))
                        GlassGroup {
                            results.forEachIndexed { i, s ->
                                SwitchRow(s.name, picked.getOrElse(i) { true }, { v -> if (i < picked.size) picked[i] = v }, subtitle = s.note.ifBlank { s.code.lines().first() })
                                if (i < results.lastIndex) Divider()
                            }
                        }
                        Spacer(Modifier.height(Space.xs))
                        val count = picked.count { it }
                        RetraButton(
                            "Add $count to my cheats",
                            { add(results.filterIndexed { i, _ -> picked.getOrElse(i) { true } }.map { CheatImport.Parsed(it.name, it.code) }); results = emptyList() },
                            Modifier.fillMaxWidth(), enabled = count > 0,
                        )
                        Spacer(Modifier.width(0.dp))
                    }
                }
            }
        }
    }
}
