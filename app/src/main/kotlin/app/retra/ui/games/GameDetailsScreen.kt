package app.retra.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.retra.AppContainer
import app.retra.core.formatBytes
import app.retra.core.formatPlaytime
import app.retra.data.Cheat
import app.retra.data.GameSource
import app.retra.data.StateSlot
import app.retra.ui.GameUi
import app.retra.ui.LocalAppActions
import app.retra.ui.gutter
import app.retra.ui.relativeTime
import app.retra.ui.toUi
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassIconButton
import app.retra.ui.components.InfoRow
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.RetraButton
import app.retra.ui.components.SectionHeader
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
import java.text.DateFormat
import java.util.Date

@Immutable
data class DetailsState(
    val game: GameUi? = null,
    val slots: List<StateSlot> = emptyList(),
    val cheats: List<Cheat> = emptyList(),
    val saveBytes: Long? = null,
    val backups: Int = 0,
    val missing: Boolean = false,
)

class DetailsViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state = combine(container.library.games, refresh) { games, _ ->
        val g = games.firstOrNull { it.id == id } ?: return@combine DetailsState(missing = true)
        DetailsState(
            game = g.toUi(container.saves),
            slots = container.saves.slots(id),
            cheats = container.cheats.cheats(id),
            saveBytes = container.saves.savFile(id).takeIf { it.exists() }?.length(),
            backups = container.saves.backups(id).size,
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailsState())

    fun reload() { refresh.value++ }
    fun setFavorite(fav: Boolean) = viewModelScope.launch { container.library.setFavorite(id, fav) }
    fun setCheats(list: List<Cheat>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { container.cheats.save(id, list) }
        reload()
    }
    fun deleteSlot(slot: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) { container.saves.deleteState(id, slot) }
        reload()
    }

    companion object {
        fun factory(c: AppContainer, id: String) = viewModelFactory { initializer { DetailsViewModel(c, id) } }
    }
}

@Composable
fun GameDetailsScreen(container: AppContainer, gameId: String, origin: String) {
    val vm: DetailsViewModel = viewModel(key = "details-$gameId", factory = DetailsViewModel.factory(container, gameId))
    val state by vm.state.collectAsStateWithLifecycle()
    // Refresh slots/screenshots when coming back from the player.
    LaunchedEffect(Unit) { vm.reload() }
    GameDetailsContent(state, origin, onFavorite = vm::setFavorite, onCheats = vm::setCheats, onDeleteSlot = vm::deleteSlot)
}

@Composable
fun GameDetailsContent(
    state: DetailsState,
    origin: String,
    onFavorite: (Boolean) -> Unit = {},
    onCheats: (List<Cheat>) -> Unit = {},
    onDeleteSlot: (Int) -> Unit = {},
) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val g = gutter()
    val ui = state.game
    Box(Modifier.fillMaxSize()) {
        if (ui == null) {
            if (state.missing) MochiMessage(
                "This game isn't here anymore", "It may have been deleted.", Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 96.dp),
                mood = MochiMood.SURPRISED, action = { RetraButton("Back", actions::back, style = ButtonStyle.SECONDARY) },
            )
            return@Box
        }
        val game = ui.game
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.xxl),
        ) {
            item("top") {
                Row(
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(g).padding(top = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions::back)
                    Spacer(Modifier.weight(1f))
                    GlassIconButton(
                        if (game.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (game.favorite) "Remove from favorites" else "Add to favorites",
                        { onFavorite(!game.favorite) },
                        tint = if (game.favorite) c.accent else c.ink,
                    )
                }
            }
            item("cover") {
                Box(
                    Modifier
                        .padding(g)
                        .padding(top = Space.m)
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .aspectRatio(3f / 2f)
                        .sharedGameBounds("game-$origin-${game.id}")
                        .shadow(Elevation.floating, Radius.largeShape, ambientColor = c.shadow, spotColor = c.shadow)
                        .clip(Radius.largeShape)
                        .glassHighlights(Radius.largeShape, c),
                ) { GameCover(ui.cover, Modifier.fillMaxSize()) }
            }
            item("title") {
                Column(Modifier.padding(g).padding(top = Space.l).staggerIn(0)) {
                    Text(game.title, style = RetraType.display, color = c.ink)
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                        categoryLabel(game.category)?.let { Tag(it, color = c.onAccentSoft, background = c.accentSoft) }
                        if (game.gameCode.isNotBlank()) Tag(game.gameCode)
                        Tag(game.lastPlayedAt?.let { formatPlaytime(game.playTimeMs) + " played" } ?: "Not played yet")
                    }
                    Spacer(Modifier.height(Space.l))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (ui.hasAutoState) {
                            RetraButton("Continue", { actions.play(game.id, true, origin) }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                            RetraButton("Restart", { actions.play(game.id, false, origin) }, Modifier.weight(1f), icon = Icons.Rounded.Replay, style = ButtonStyle.SECONDARY)
                        } else {
                            RetraButton("Play", { actions.play(game.id, false, origin) }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                        }
                    }
                    if (ui.hasAutoState) {
                        Text(
                            "Continue picks up exactly where you left off. Restart boots the game; your in-game save is kept.",
                            style = RetraType.caption, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs),
                        )
                    }
                }
            }
            val slots = state.slots.filter { it.exists }
            item("states-title") {
                SectionHeader("Save states", Modifier.padding(g).padding(top = Space.xl, bottom = Space.s).staggerIn(1), subtitle = if (slots.isEmpty()) "Make one from the in-game menu" else "Tap to load, long-press to delete")
            }
            if (slots.isNotEmpty()) {
                item("states") {
                    LazyRow(contentPadding = g, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.staggerIn(2)) {
                        items(slots, key = { it.slot }) { slot -> SlotCard(slot, { actions.play(game.id, slot.slot == 0, origin) }, { onDeleteSlot(slot.slot) }) }
                    }
                }
            }
            item("cheats") {
                SectionHeader("Cheats", Modifier.padding(g).padding(top = Space.xl, bottom = Space.s))
                GlassGroup(Modifier.padding(g)) {
                    Box(Modifier.padding(vertical = Space.xs)) { CheatsEditor(state.cheats, onCheats) }
                }
            }
            item("info") {
                SectionHeader("About this file", Modifier.padding(g).padding(top = Space.xl, bottom = Space.s))
                GlassGroup(Modifier.padding(g)) {
                    InfoRow("Header title", game.romTitle.ifBlank { "—" })
                    InfoRow("Game code", game.gameCode.ifBlank { "—" } + if (game.makerCode.isNotBlank()) " · maker ${game.makerCode}" else "")
                    InfoRow("Revision", "v1.${game.romVersion}".let { if (game.romVersion == 0) "1.0" else "1.${game.romVersion}" })
                    InfoRow("Save type", game.saveType?.let(::saveTypeLabel) ?: "Detected the first time you play")
                    InfoRow("Real-time clock", when (game.hasRtc) { true -> "Yes"; false -> "No"; null -> "Detected when you play" })
                    InfoRow("Size", formatBytes(game.sizeBytes))
                    InfoRow("CRC32", game.crc32)
                    InfoRow("SHA-1", game.sha1)
                    InfoRow("Battery save", state.saveBytes?.let { formatBytes(it) + if (state.backups > 0) " · ${state.backups} backups" else "" } ?: "None yet")
                    InfoRow("Added", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(game.addedAt)))
                    InfoRow("Source", sourceLabel(game.source))
                    game.provenance?.let { p ->
                        InfoRow("Patch", "${p.patchName} (${p.patchFormat.uppercase()})")
                        InfoRow("Base game", "${p.baseTitle} · CRC ${p.baseCrc32}")
                        InfoRow("Patched", relativeTime(p.appliedAt) + if (p.forced) " · checks skipped" else "")
                    }
                }
            }
        }
    }
}

@Composable
fun SlotCard(slot: StateSlot, onClick: () -> Unit, onLongClick: (() -> Unit)?, modifier: Modifier = Modifier, selected: Boolean = false) {
    val c = Retra.colors
    Column(modifier.width(144.dp).squishClickable(onClick, onLongClick = onLongClick, label = slotLabel(slot.slot))) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .clip(Radius.smallShape)
                .then(if (selected) Modifier.glassHighlights(Radius.smallShape, c, 2.dp) else Modifier.glassHighlights(Radius.smallShape, c)),
        ) {
            val path = slot.thumbnailPath
            if (path != null) {
                GameCover(app.retra.ui.components.CoverModel("", "", 0, filePath = path, fileStamp = slot.savedAt ?: 0), Modifier.fillMaxSize(), showTitle = false)
            } else {
                Box(Modifier.fillMaxSize().then(Modifier.padding(0.dp)), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().then(androidx.compose.ui.Modifier).padding(0.dp).clip(Radius.smallShape).then(Modifier).let { it }) {}
                    Text(if (slot.exists) "Saved" else "Empty", style = RetraType.label, color = c.inkMuted)
                }
            }
        }
        Spacer(Modifier.height(Space.xxs))
        Text(slotLabel(slot.slot), style = RetraType.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(slot.savedAt?.let { relativeTime(it) } ?: "Empty", style = RetraType.caption, color = c.inkMuted)
    }
}

fun slotLabel(slot: Int) = if (slot == 0) "Auto save" else "Slot $slot"

fun saveTypeLabel(raw: String): String = when (raw) {
    "NONE" -> "None"
    "SRAM" -> "SRAM 32 KB"
    "FLASH_64K" -> "Flash 64 KB"
    "FLASH_128K" -> "Flash 128 KB"
    "EEPROM_512" -> "EEPROM 512 B"
    "EEPROM_8K" -> "EEPROM 8 KB"
    "AUTO" -> "EEPROM (size detected in game)"
    else -> raw
}

fun sourceLabel(s: GameSource) = when (s) {
    GameSource.IMPORTED -> "Imported by you"
    GameSource.BUNDLED -> "Included with Retra"
    GameSource.DOWNLOADED -> "Game Library download"
    GameSource.PATCHED -> "Made with the Patcher"
}

@Suppress("unused")
private val unusedColor = Color.Transparent
