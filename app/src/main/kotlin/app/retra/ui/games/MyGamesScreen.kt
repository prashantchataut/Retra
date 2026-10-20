package app.retra.ui.games

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.core.formatPlaytime
import app.retra.data.GameCategory
import app.retra.data.MessageTone
import app.retra.ui.GameUi
import app.retra.ui.LocalAppActions
import app.retra.ui.gutter
import app.retra.ui.listPadding
import app.retra.ui.relativeTime
import app.retra.ui.toUi
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassIconButton
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.NavRow
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraChip
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.ScreenHeader
import app.retra.ui.components.Segmented
import app.retra.ui.components.Tag
import app.retra.ui.components.glassHighlights
import app.retra.ui.components.sharedGameBounds
import app.retra.ui.components.squishClickable
import app.retra.ui.components.staggerIn
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) { RECENT("Recent"), NAME("Name"), PLAYTIME("Playtime") }
enum class GameFilter(val label: String) { ALL("All"), FAVORITES("Favorites"), POKEMON("Pokémon"), FAN_GAMES("Fan games"), HOMEBREW("Homebrew") }

@Immutable
data class GamesState(
    val query: String = "",
    val sort: SortMode = SortMode.RECENT,
    val filter: GameFilter = GameFilter.ALL,
    val grid: Boolean = true,
    val games: List<GameUi> = emptyList(),
    val total: Int = 0,
    val loaded: Boolean = false,
)

private data class GamesQuery(val query: String, val sort: SortMode, val filter: GameFilter, val grid: Boolean)

class GamesViewModel(private val container: AppContainer) : ViewModel() {
    private val query = MutableStateFlow(GamesQuery("", SortMode.RECENT, GameFilter.ALL, true))

    val state = combine(container.library.games, query) { games, q ->
        val filtered = games.asSequence()
            .filter { g ->
                when (q.filter) {
                    GameFilter.ALL -> true
                    GameFilter.FAVORITES -> g.favorite
                    GameFilter.POKEMON -> g.category == GameCategory.POKEMON
                    GameFilter.FAN_GAMES -> g.category == GameCategory.FAN_GAME
                    GameFilter.HOMEBREW -> g.category == GameCategory.HOMEBREW
                }
            }
            .filter { g -> q.query.isBlank() || g.title.contains(q.query.trim(), true) || g.gameCode.contains(q.query.trim(), true) }
            .let { s ->
                when (q.sort) {
                    SortMode.RECENT -> s.sortedByDescending { it.lastPlayedAt ?: it.addedAt }
                    SortMode.NAME -> s.sortedBy { it.title.lowercase() }
                    SortMode.PLAYTIME -> s.sortedByDescending { it.playTimeMs }
                }
            }
            .map { it.toUi(container.saves) }
            .toList()
        GamesState(q.query, q.sort, q.filter, q.grid, filtered, games.size, loaded = true)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GamesState())

    fun setQuery(s: String) { query.value = query.value.copy(query = s) }
    fun setSort(s: SortMode) { query.value = query.value.copy(sort = s) }
    fun setFilter(f: GameFilter) { query.value = query.value.copy(filter = f) }
    fun toggleLayout() { query.value = query.value.copy(grid = !query.value.grid) }

    fun setFavorite(id: String, fav: Boolean) = viewModelScope.launch { container.library.setFavorite(id, fav) }
    fun rename(id: String, title: String) = viewModelScope.launch { container.library.rename(id, title) }
    fun delete(id: String) = viewModelScope.launch {
        val title = container.library.game(id)?.title
        container.deleteGame(id)
        container.messages.show("Deleted ${title ?: "game"}")
    }

    fun importSave(id: String, uri: Uri) = viewModelScope.launch {
        runCatching {
            val bytes = container.importer.readBytes(uri, 512 * 1024)
            require(bytes.isNotEmpty() && bytes.size <= 256 * 1024) { "That file isn't a GBA save (.sav files are 512 bytes to 128 KB)." }
            withContext(Dispatchers.IO) { container.saves.writeBatterySave(id, bytes, backup = true) }
        }.onSuccess { container.messages.show("Save imported. Your old save was backed up.", MessageTone.SUCCESS) }
            .onFailure { container.messages.show(it.message ?: "Couldn't import that save.", MessageTone.ERROR) }
    }

    fun exportSave(id: String, uri: Uri, resolver: android.content.ContentResolver) = viewModelScope.launch {
        runCatching {
            val bytes = withContext(Dispatchers.IO) { container.saves.readBatterySave(id) } ?: error("This game has no save yet. Play and save in-game first.")
            withContext(Dispatchers.IO) { resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Couldn't write the file.") }
        }.onSuccess { container.messages.show("Save exported", MessageTone.SUCCESS) }
            .onFailure { container.messages.show(it.message ?: "Couldn't export the save.", MessageTone.ERROR) }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { GamesViewModel(c) } }
    }
}

@Composable
fun MyGamesScreen(container: AppContainer) {
    val vm: GamesViewModel = viewModel(factory = GamesViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingSaveFor by remember { mutableStateOf<String?>(null) }
    val importSav = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = pendingSaveFor
        if (uri != null && id != null) vm.importSave(id, uri)
    }
    val exportSav = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val id = pendingSaveFor
        if (uri != null && id != null) vm.exportSave(id, uri, context.contentResolver)
    }
    MyGamesContent(
        state = state,
        onQuery = vm::setQuery,
        onSort = vm::setSort,
        onFilter = vm::setFilter,
        onToggleLayout = vm::toggleLayout,
        onFavorite = { id, f -> vm.setFavorite(id, f) },
        onRename = { id, t -> vm.rename(id, t) },
        onDelete = { vm.delete(it) },
        onImportSave = { pendingSaveFor = it; importSav.launch(arrayOf("*/*")) },
        onExportSave = { g -> pendingSaveFor = g.id; exportSav.launch("${g.title.replace(Regex("[^A-Za-z0-9 _-]"), "")}.sav") },
    )
}

@Composable
fun MyGamesContent(
    state: GamesState,
    onQuery: (String) -> Unit = {},
    onSort: (SortMode) -> Unit = {},
    onFilter: (GameFilter) -> Unit = {},
    onToggleLayout: () -> Unit = {},
    onFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onRename: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onImportSave: (String) -> Unit = {},
    onExportSave: (app.retra.data.Game) -> Unit = {},
    initialSheetFor: GameUi? = null,
) {
    val actions = LocalAppActions.current
    val c = Retra.colors
    val g = gutter()
    var sheetFor by remember { mutableStateOf(initialSheetFor) }
    var renaming by remember { mutableStateOf<GameUi?>(null) }
    var confirmDelete by remember { mutableStateOf<GameUi?>(null) }
    val columns = if (state.grid) GridCells.Adaptive(156.dp) else GridCells.Fixed(1)

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = columns,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = g.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = g.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = listPadding(Space.l).calculateTopPadding(),
                bottom = listPadding().calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(if (state.grid) Space.m else Space.s),
            modifier = Modifier.fillMaxSize(),
        ) {
            item("header", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    ScreenHeader(
                        "My Games",
                        subtitle = if (state.total == 1) "1 game" else "${state.total} games",
                    ) {
                        GlassIconButton(
                            if (state.grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            if (state.grid) "Show as list" else "Show as grid",
                            onToggleLayout,
                        )
                        Spacer(Modifier.width(Space.xs))
                        GlassIconButton(Icons.Rounded.Add, "Import games", actions::importGames, tint = c.accent)
                    }
                    Spacer(Modifier.height(Space.m))
                    RetraTextField(
                        state.query, onQuery, "Search your games", Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Search,
                        imeAction = ImeAction.Search,
                        onImeAction = { focusManager.clearFocus() },
                    )
                }
            }
            item("filters", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        items(GameFilter.entries, key = { it.name }) { f -> RetraChip(f.label, state.filter == f, { onFilter(f) }) }
                    }
                    Spacer(Modifier.height(Space.xxs))
                    Segmented(SortMode.entries.map { it to it.label }, state.sort, onSort)
                }
            }
            if (state.loaded && state.games.isEmpty()) {
                item("empty", span = { GridItemSpan(maxLineSpan) }) {
                    if (state.total == 0) {
                        MochiMessage(
                            "Your shelf is empty",
                            "Import .gba games you own, or download free homebrew from the Game Library.",
                            mood = MochiMood.SLEEPY,
                            action = {
                                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                                    RetraButton("Import", actions::importGames, icon = Icons.Rounded.Add)
                                    RetraButton("Game Library", { actions.openStore() }, style = ButtonStyle.SECONDARY)
                                }
                            },
                        )
                    } else {
                        MochiMessage("Nothing matches", "Try another search or filter.", mood = MochiMood.SURPRISED)
                    }
                }
            }
            itemsIndexed(state.games, key = { _, it -> it.id }) { index, game ->
                val mod = Modifier.animateItem().staggerIn(index, state.filter)
                if (state.grid) GameGridCard(game, mod, onLongPress = { sheetFor = game })
                else GameListRow(game, mod, onLongPress = { sheetFor = game })
            }
        }

        // Actions sheet.
        val target = sheetFor
        GlassSheet(visible = target != null, onDismiss = { sheetFor = null }, title = target?.game?.title) {
            if (target != null) GameActions(
                target,
                onDismiss = { sheetFor = null },
                onFavorite = { onFavorite(target.id, !target.game.favorite); sheetFor = null },
                onRename = { renaming = target; sheetFor = null },
                onDelete = { confirmDelete = target; sheetFor = null },
                onImportSave = { onImportSave(target.id); sheetFor = null },
                onExportSave = { onExportSave(target.game); sheetFor = null },
            )
        }
        RenameSheet(renaming, onDismiss = { renaming = null }, onRename = { id, t -> onRename(id, t); renaming = null })
        val del = confirmDelete
        GlassSheet(visible = del != null, onDismiss = { confirmDelete = null }, title = "Delete ${del?.game?.title ?: ""}?") {
            Text(
                "This removes the game, its saves, save states and cheats from Retra. Export the save first if you want to keep it.",
                style = RetraType.body, color = c.inkMuted,
            )
            Spacer(Modifier.height(Space.l))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                RetraButton("Keep it", { confirmDelete = null }, Modifier.weight(1f), style = ButtonStyle.SECONDARY)
                RetraButton("Delete", { del?.let { onDelete(it.id) }; confirmDelete = null }, Modifier.weight(1f), icon = Icons.Rounded.Delete, style = ButtonStyle.DANGER)
            }
        }
    }
}

@Composable
private fun GameActions(
    game: GameUi,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onImportSave: () -> Unit,
    onExportSave: () -> Unit,
) {
    val actions = LocalAppActions.current
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        RetraButton(if (game.hasAutoState) "Continue" else "Play", { onDismiss(); actions.play(game.id, game.hasAutoState, "games") }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
        RetraButton("Details", { onDismiss(); actions.openGame(game.id, "games") }, Modifier.weight(1f), icon = Icons.Rounded.Info, style = ButtonStyle.SECONDARY)
    }
    Spacer(Modifier.height(Space.s))
    NavRow(if (game.game.favorite) "Remove from favorites" else "Add to favorites", onFavorite, icon = if (game.game.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder)
    NavRow("Rename", onRename, icon = Icons.Rounded.Edit)
    NavRow("Import save file", onImportSave, subtitle = "Replace with a .sav from another emulator", icon = Icons.Rounded.Download)
    NavRow("Export save file", onExportSave, subtitle = "Keep a copy of your progress", icon = Icons.Rounded.Upload)
    NavRow("Delete", onDelete, icon = Icons.Rounded.Delete, danger = true)
}

@Composable
fun RenameSheet(target: GameUi?, onDismiss: () -> Unit, onRename: (String, String) -> Unit) {
    var text by remember(target) { mutableStateOf(target?.game?.title ?: "") }
    GlassSheet(visible = target != null, onDismiss = onDismiss, title = "Rename game") {
        RetraTextField(text, { text = it.take(80) }, "Game name", Modifier.fillMaxWidth(), onImeAction = { target?.let { onRename(it.id, text) } })
        Spacer(Modifier.height(Space.m))
        RetraButton("Save name", { target?.let { onRename(it.id, text) } }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
    }
}

@Composable
fun GameGridCard(game: GameUi, modifier: Modifier = Modifier, onLongPress: () -> Unit) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    Column(modifier.squishClickable({ actions.openGame(game.id, "games") }, onLongClick = onLongPress, label = "Open ${game.game.title}")) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .sharedGameBounds("game-games-${game.id}")
                .shadow(Elevation.raised, Radius.mediumShape, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(Radius.mediumShape)
                .glassHighlights(Radius.mediumShape, c),
        ) {
            GameCover(game.cover, Modifier.fillMaxSize())
            if (game.game.favorite) {
                Icon(
                    Icons.Rounded.Favorite, "Favorite", tint = c.accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Space.xs).size(28.dp)
                        .clip(Radius.pillShape).background(c.glassFillStrong).padding(5.dp),
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(game.game.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Space.xxs))
        Text(metaLine(game), style = RetraType.caption, color = c.inkMuted, maxLines = 1, modifier = Modifier.padding(horizontal = Space.xxs))
    }
}

@Composable
fun GameListRow(game: GameUi, modifier: Modifier = Modifier, onLongPress: () -> Unit) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    GlassSurface(modifier.fillMaxWidth().squishClickable({ actions.openGame(game.id, "games") }, onLongClick = onLongPress, pressed = 0.98f), shape = Radius.mediumShape) {
        Row(Modifier.padding(Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(96.dp).aspectRatio(3f / 2f).sharedGameBounds("game-games-${game.id}").clip(Radius.smallShape)) {
                GameCover(game.cover, Modifier.fillMaxSize(), showTitle = false)
            }
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text(game.game.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(metaLine(game), style = RetraType.caption, color = c.inkMuted, maxLines = 1)
                Spacer(Modifier.height(Space.xxs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    categoryLabel(game.game.category)?.let { Tag(it) }
                    if (game.game.gameCode.isNotBlank()) Tag(game.game.gameCode)
                }
            }
            if (game.game.favorite) Icon(Icons.Rounded.Favorite, "Favorite", tint = c.accent, modifier = Modifier.padding(end = Space.xs).size(20.dp))
        }
    }
}

fun categoryLabel(cat: GameCategory): String? = when (cat) {
    GameCategory.POKEMON -> "Pokémon"
    GameCategory.FAN_GAME -> "Fan game"
    GameCategory.HOMEBREW -> "Homebrew"
    GameCategory.OTHER -> null
}

private fun metaLine(game: GameUi): String {
    val g = game.game
    return if (g.lastPlayedAt != null) "${relativeTime(g.lastPlayedAt)} · ${formatPlaytime(g.playTimeMs)}" else "Not played yet"
}
