package app.retra.ui.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import app.retra.data.CatalogEntry
import app.retra.ui.GameUi
import app.retra.ui.LocalAppActions
import app.retra.ui.coverModel
import app.retra.ui.greeting
import app.retra.ui.gutter
import app.retra.ui.listPadding
import app.retra.ui.relativeTime
import app.retra.ui.toUi
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.glassHighlights
import app.retra.ui.components.Mochi
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.RetraButton
import app.retra.ui.components.SectionHeader
import app.retra.ui.components.Tag
import app.retra.ui.components.sharedGameBounds
import app.retra.ui.components.squishClickable
import app.retra.ui.components.staggerIn
import app.retra.ui.theme.Elevation
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

@Immutable
data class HomeState(
    val greeting: String = "Hello",
    val continueGame: GameUi? = null,
    val recent: List<GameUi> = emptyList(),
    val featured: List<CatalogEntry> = emptyList(),
    val gameCount: Int = 0,
    val loaded: Boolean = false,
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    val state = combine(container.library.games, container.catalog.state) { games, catalog ->
        val played = games.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }
        val hero = (played.firstOrNull() ?: games.maxByOrNull { it.addedAt })?.toUi(container.saves)
        HomeState(
            greeting = greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
            continueGame = hero,
            recent = (played.drop(1) + games.filter { it.lastPlayedAt == null }.sortedByDescending { it.addedAt })
                .filter { it.id != hero?.id }.take(10).map { it.toUi(container.saves) },
            featured = catalog.catalog?.entries?.filter { it.featured }.orEmpty(),
            gameCount = games.size,
            loaded = true,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { HomeViewModel(c) } }
    }
}

@Composable
fun HomeScreen(container: AppContainer) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    HomeContent(state)
}

@Composable
fun HomeContent(state: HomeState) {
    val actions = LocalAppActions.current
    val c = Retra.colors
    val g = gutter()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(top = Space.l)) {
        item("header") {
            Row(Modifier.padding(g).staggerIn(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.greeting, style = RetraType.display, color = c.ink)
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        when {
                            state.continueGame?.game?.lastPlayedAt != null -> "Mochi kept your spot warm."
                            state.gameCount > 0 -> "Pick a game and press play."
                            else -> "Let's set up your game shelf."
                        },
                        style = RetraType.body, color = c.inkMuted,
                    )
                }
                Mochi(mood = MochiMood.HAPPY, size = 76.dp, description = "Mochi waving hello")
            }
        }
        item("hero") {
            Spacer(Modifier.height(Space.l))
            val hero = state.continueGame
            if (hero != null) {
                ContinueCard(hero, Modifier.padding(g).staggerIn(1))
            } else if (state.loaded) {
                GlassSurface(Modifier.padding(g).fillMaxWidth(), shape = Radius.largeShape) {
                    MochiMessage(
                        "No games yet",
                        "Import a .gba file you own, or grab a free game from the Game Library.",
                        Modifier.padding(Space.m),
                        action = { RetraButton("Import a game", actions::importGames, icon = Icons.Rounded.AddCircle) },
                    )
                }
            }
        }
        item("quick") {
            Spacer(Modifier.height(Space.l))
            QuickActions(Modifier.padding(g).staggerIn(2))
        }
        if (state.recent.isNotEmpty()) {
            item("recent-title") {
                Spacer(Modifier.height(Space.xl))
                SectionHeader("Jump back in", Modifier.padding(g).staggerIn(3))
                Spacer(Modifier.height(Space.s))
            }
            item("recent") {
                LazyRow(contentPadding = g, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.staggerIn(4)) {
                    items(state.recent, key = { it.id }) { game -> RecentCard(game) }
                }
            }
        }
        if (state.featured.isNotEmpty()) {
            item("featured-title") {
                Spacer(Modifier.height(Space.xl))
                SectionHeader(
                    "From the Game Library", Modifier.padding(g).staggerIn(5),
                    subtitle = "Free homebrew and fan-game patches",
                    action = { RetraButton("See all", { actions.openStore() }, style = ButtonStyle.TERTIARY, compact = true) },
                )
                Spacer(Modifier.height(Space.s))
            }
            item("featured") {
                LazyRow(contentPadding = g, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.staggerIn(6)) {
                    items(state.featured, key = { it.id }) { e -> FeaturedCard(e) { actions.openStore(e.id) } }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(game: GameUi, modifier: Modifier = Modifier) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val origin = "home-hero"
    val played = game.game.lastPlayedAt != null
    val resume = played && game.hasAutoState
    GlassSurface(
        modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .sharedGameBounds("game-$origin-${game.id}")
            .squishClickable(onClick = { actions.openGame(game.id, origin) }, pressed = 0.98f, label = "Open ${game.game.title}"),
        shape = Radius.largeShape,
        elevation = Elevation.raised,
    ) {
        Column(Modifier.padding(Space.xs)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .clip(Radius.mediumShape),
            ) {
                GameCover(game.cover, Modifier.fillMaxSize())
                Tag(
                    if (resume) "Continue" else if (played) "Last played" else "Ready to play",
                    Modifier.align(Alignment.TopStart).padding(Space.s),
                    color = c.onAccentSoft, background = c.accentSoft,
                )
            }
            Row(Modifier.padding(start = Space.s, end = Space.xxs, top = Space.s, bottom = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(game.game.title, style = RetraType.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        game.game.lastPlayedAt?.let { "${relativeTime(it)} · ${formatPlaytime(game.game.playTimeMs)}" } ?: "New in your games",
                        style = RetraType.label, color = c.inkMuted, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(Space.s))
                RetraButton(
                    if (resume) "Continue" else "Play",
                    { actions.play(game.id, resume, origin) },
                    icon = Icons.Rounded.PlayArrow,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun QuickActions(modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    val c = Retra.colors
    Row(modifier.fillMaxWidth().height(148.dp), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        // Primary action gets the tall tile; the other two stack (asymmetric bento, not three equal cards).
        QuickTile(
            "Import game", "Add .gba or .zip files you own", Icons.Rounded.AddCircle,
            Modifier.weight(1f).fillMaxSize(), wash = c.accentSoft.copy(alpha = 0.55f), large = true,
            onClick = actions::importGames,
        )
        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            QuickTile("Patcher", null, Icons.Rounded.AutoFixHigh, Modifier.weight(1f).fillMaxWidth(), onClick = { actions.openPatcher() })
            QuickTile("Game Library", null, Icons.Rounded.Storefront, Modifier.weight(1f).fillMaxWidth(), onClick = { actions.openStore() })
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    modifier: Modifier,
    wash: Color? = null,
    large: Boolean = false,
    onClick: () -> Unit,
) {
    val c = Retra.colors
    GlassSurface(modifier.squishClickable(onClick, label = title), shape = Radius.mediumShape, wash = wash) {
        if (large) {
            Column(Modifier.fillMaxSize().padding(Space.m), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(44.dp).clip(Radius.pillShape).background(c.accent), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = c.onAccent, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(title, style = RetraType.bodyStrong, color = c.ink)
                    if (subtitle != null) Text(subtitle, style = RetraType.caption, color = c.inkMuted, maxLines = 2)
                }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = Space.m), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(Space.xs))
                Text(title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentCard(game: GameUi) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val origin = "home-recent"
    Column(
        Modifier
            .width(168.dp)
            .squishClickable({ actions.openGame(game.id, origin) }, label = "Open ${game.game.title}")
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .sharedGameBounds("game-$origin-${game.id}")
                .shadow(Elevation.raised, Radius.mediumShape, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(Radius.mediumShape)
                .glassHighlights(Radius.mediumShape, c),
        ) { GameCover(game.cover, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(Space.xs))
        Text(game.game.title, style = RetraType.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Space.xxs))
        Text(
            game.game.lastPlayedAt?.let { relativeTime(it) } ?: "Not played yet",
            style = RetraType.caption, color = c.inkMuted, modifier = Modifier.padding(horizontal = Space.xxs),
        )
    }
}

@Composable
fun FeaturedCard(entry: CatalogEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Retra.colors
    val accent = Color(entry.accentArgb)
    GlassSurface(
        modifier.width(232.dp).squishClickable(onClick, label = entry.title),
        shape = Radius.mediumShape,
        wash = accent.copy(alpha = if (c.isDark) 0.16f else 0.45f),
    ) {
        Column(Modifier.padding(Space.xs)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(Radius.smallShape)) {
                GameCover(entry.coverModel(), Modifier.fillMaxSize())
            }
            Column(Modifier.padding(horizontal = Space.xs, vertical = Space.s)) {
                Text(entry.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.author, style = RetraType.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Tag(if (entry.isRom) "Free download" else "Patch · ${entry.base?.gameCode ?: ""}")
            }
        }
    }
}
