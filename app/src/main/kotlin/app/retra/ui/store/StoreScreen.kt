package app.retra.ui.store

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.retra.AppContainer
import app.retra.core.formatBytes
import app.retra.data.CatalogEntry
import app.retra.data.CatalogSource
import app.retra.ui.LocalAppActions
import app.retra.ui.coverModel
import app.retra.ui.gutter
import app.retra.ui.listPadding
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassIconButton
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.PatchProgressRing
import app.retra.ui.components.ProgressPill
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.ScreenHeader
import app.retra.ui.components.SectionHeader
import app.retra.ui.components.Tag
import app.retra.ui.components.glassHighlights
import app.retra.ui.components.squishClickable
import app.retra.ui.components.staggerIn
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space

@Composable
fun StoreScreen(container: AppContainer, openEntryId: String?) {
    val vm: StoreViewModel = viewModel(factory = StoreViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(openEntryId) }
    LaunchedEffect(openEntryId) { if (openEntryId != null) selected = openEntryId }
    var pickingFor by remember { mutableStateOf<CatalogEntry?>(null) }
    val pickBase = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val e = pickingFor
        if (uri != null && e != null) vm.pickBase(e, uri)
    }
    StoreContent(
        state = state,
        selectedId = selected,
        onSelect = { selected = it },
        onQuery = vm::setQuery,
        onRefresh = { vm.refresh() },
        handlers = EntryHandlers(
            getRom = vm::getRom,
            downloadPatch = vm::downloadPatch,
            cancel = vm::cancel,
            pickBase = { e -> pickingFor = e; pickBase.launch(arrayOf("*/*")) },
            makeGame = { e, base, force -> vm.makeGame(e, base, force) },
            reset = vm::resetJob,
        ),
    )
}

class EntryHandlers(
    val getRom: (CatalogEntry) -> Unit = {},
    val downloadPatch: (CatalogEntry) -> Unit = {},
    val cancel: (String) -> Unit = {},
    val pickBase: (CatalogEntry) -> Unit = {},
    val makeGame: (CatalogEntry, String, Boolean) -> Unit = { _, _, _ -> },
    val reset: (String) -> Unit = {},
)

@Composable
fun StoreContent(
    state: StoreState,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onQuery: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    handlers: EntryHandlers = EntryHandlers(),
) {
    val c = Retra.colors
    val g = gutter()
    val catalog = state.catalog
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(Space.l)) {
            item("header") {
                Column(Modifier.padding(g)) {
                    ScreenHeader("Game Library", subtitle = "Free homebrew and Pokémon fan-game patches") {
                        GlassIconButton(Icons.Rounded.Sync, "Refresh the list", onRefresh)
                    }
                    Spacer(Modifier.height(Space.m))
                    RetraTextField(state.query, onQuery, "Search games, creators, tags", Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Search)
                    Spacer(Modifier.height(Space.s))
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = Space.xxs)) {
                        Icon(Icons.Rounded.Shield, null, tint = c.success, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            "Retra never provides commercial games. Fan games come as patches you apply to a cartridge you dumped yourself.",
                            style = RetraType.caption, color = c.inkMuted,
                        )
                    }
                }
            }
            if (catalog == null) {
                item("loading") { MochiMessage("Loading the library…", "Mochi is fetching the shelves.", mood = MochiMood.LOADING) }
            } else if (state.query.isNotBlank()) {
                val results = state.results()
                item("results-title") { SectionHeader("${results.size} results", Modifier.padding(g).padding(top = Space.l, bottom = Space.s)) }
                if (results.isEmpty()) item("none") { MochiMessage("No matches", "Try a game title, a creator or a tag like \"kanto\".", mood = MochiMood.SURPRISED) }
                items(results, key = { "r-" + it.id }) { e ->
                    StoreRow(e, state, Modifier.padding(g).padding(bottom = Space.s).animateItem()) { onSelect(e.id) }
                }
            } else {
                catalog.collections.forEachIndexed { i, col ->
                    val shelf = catalog.shelf(col)
                    if (shelf.isEmpty()) return@forEachIndexed
                    item("t-" + col.id) {
                        SectionHeader(col.title, Modifier.padding(g).padding(top = Space.xl, bottom = Space.s).staggerIn(i * 2), subtitle = col.subtitle)
                    }
                    item("s-" + col.id) {
                        LazyRow(contentPadding = g, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.staggerIn(i * 2 + 1)) {
                            items(shelf, key = { col.id + it.id }) { e -> StoreCard(e, state, large = col.id == "featured") { onSelect(e.id) } }
                        }
                    }
                }
                item("footer") {
                    Row(Modifier.padding(g).padding(top = Space.xl), verticalAlignment = Alignment.CenterVertically) {
                        if (state.source != CatalogSource.ONLINE) Icon(Icons.Rounded.CloudOff, null, tint = c.inkMuted, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        Text(
                            buildString {
                                append("List updated ${catalog.updated}")
                                when {
                                    state.refreshing -> append(" · checking for updates…")
                                    state.source != CatalogSource.ONLINE -> append(" · saved copy")
                                }
                            },
                            style = RetraType.caption, color = c.inkMuted,
                        )
                    }
                }
            }
        }
        val entry = selectedId?.let { catalog?.entry(it) }
        GlassSheet(visible = entry != null, onDismiss = { onSelect(null) }) {
            if (entry != null) EntryDetail(entry, state, handlers)
        }
    }
}

private fun statusLabel(e: CatalogEntry, state: StoreState): String = when {
    state.installed.containsKey(e.id) -> "In your games"
    e.isRom -> e.download?.let { "Free · ${formatBytes(it.size)}" } ?: "Free"
    e.download == null -> "Patch from creator"
    else -> "${e.formatLabel} patch · ${e.base?.gameCode}"
}

@Composable
private fun StoreCard(e: CatalogEntry, state: StoreState, large: Boolean, onClick: () -> Unit) {
    val c = Retra.colors
    val accent = Color(e.accentArgb)
    GlassSurface(
        Modifier.width(if (large) 264.dp else 200.dp).squishClickable(onClick, label = e.title),
        shape = Radius.mediumShape,
        wash = accent.copy(alpha = if (c.isDark) 0.14f else 0.5f),
    ) {
        Column(Modifier.padding(Space.xs)) {
            Box(Modifier.fillMaxWidth().aspectRatio(if (large) 16f / 10f else 3f / 2f).clip(Radius.smallShape)) {
                GameCover(e.coverModel(), Modifier.fillMaxSize())
                if (state.installed.containsKey(e.id)) {
                    Icon(
                        Icons.Rounded.CheckCircle, "In your games", tint = c.success,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(Radius.pillShape).background(c.glassFillStrong).padding(3.dp),
                    )
                }
            }
            Column(Modifier.padding(horizontal = Space.xs, vertical = Space.s)) {
                Text(e.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.author, style = RetraType.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Tag(statusLabel(e, state))
            }
        }
    }
}

@Composable
private fun StoreRow(e: CatalogEntry, state: StoreState, modifier: Modifier, onClick: () -> Unit) {
    val c = Retra.colors
    GlassSurface(modifier.fillMaxWidth().squishClickable(onClick, pressed = 0.98f, label = e.title), shape = Radius.mediumShape, wash = Color(e.accentArgb).copy(alpha = if (c.isDark) 0.1f else 0.35f)) {
        Row(Modifier.padding(Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(88.dp).aspectRatio(3f / 2f).clip(Radius.smallShape)) { GameCover(e.coverModel(), Modifier.fillMaxSize(), showTitle = false) }
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.author, style = RetraType.caption, color = c.inkMuted, maxLines = 1)
                Text(statusLabel(e, state), style = RetraType.caption, color = c.inkMuted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDetail(e: CatalogEntry, state: StoreState, h: EntryHandlers) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val accent = Color(e.accentArgb)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(Radius.mediumShape)
            .background(accent)
            .glassHighlights(Radius.mediumShape, c),
    ) { GameCover(e.coverModel(), Modifier.fillMaxSize()) }
    Spacer(Modifier.height(Space.m))
    Text(e.title, style = RetraType.title, color = c.ink)
    Text(
        "by ${e.author}" + (e.version?.let { " · v$it" } ?: ""),
        style = RetraType.label, color = c.inkMuted,
    )
    Spacer(Modifier.height(Space.s))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xxs), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
        Tag(e.status.replace('-', ' ').replaceFirstChar { it.uppercase() }, color = c.ink, background = accent.copy(alpha = if (c.isDark) 0.3f else 0.7f))
        if (e.isPatch) Tag("${e.formatLabel} patch")
        e.license?.let { Tag(it) }
        e.tags.take(4).forEach { Tag(it.replace('-', ' ')) }
    }
    Spacer(Modifier.height(Space.m))
    Text(e.description, style = RetraType.body, color = c.ink)
    Spacer(Modifier.height(Space.l))
    if (e.isRom) RomActions(e, state, h) else PatchFlow(e, state, h)
    Spacer(Modifier.height(Space.m))
    RetraButton("Visit the creator's page", { actions.openUrl(e.homepage) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.OpenInNew, style = ButtonStyle.TERTIARY)
}

@Composable
private fun RomActions(e: CatalogEntry, state: StoreState, h: EntryHandlers) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val installed = state.installed[e.id]
    when (val job = state.jobs[e.id] ?: EntryJob.Idle) {
        is EntryJob.Downloading -> {
            Text("Downloading… ${(job.progress * 100).toInt()} %", style = RetraType.label, color = c.inkMuted)
            Spacer(Modifier.height(Space.xs))
            ProgressPill(job.progress)
            Spacer(Modifier.height(Space.s))
            RetraButton("Cancel", { h.cancel(e.id) }, Modifier.fillMaxWidth(), style = ButtonStyle.SECONDARY)
        }
        is EntryJob.Failed -> {
            Notice(Icons.Rounded.WarningAmber, job.message, c.warning)
            Spacer(Modifier.height(Space.s))
            RetraButton("Try again", { h.getRom(e) }, Modifier.fillMaxWidth())
        }
        else -> if (installed != null) {
            RetraButton("Play", { actions.play(installed, false, "store") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        } else {
            val label = if (e.bundled) "Add to My Games" else "Download · ${e.download?.let { formatBytes(it.size) } ?: ""}"
            RetraButton(label, { h.getRom(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CloudDownload)
            Spacer(Modifier.height(Space.xs))
            Text(
                if (e.bundled) "Made for Retra and included with the app." else "Verified with SHA-256 after downloading. Licence: ${e.license ?: "see creator"}.",
                style = RetraType.caption, color = c.inkMuted,
            )
        }
    }
}

/** "Make this game": patch → base ROM → apply. */
@Composable
private fun PatchFlow(e: CatalogEntry, state: StoreState, h: EntryHandlers) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val base = e.base ?: return
    val job = state.jobs[e.id] ?: EntryJob.Idle
    val made = (job as? EntryJob.Done)?.gameId ?: state.installed[e.id]

    if (job is EntryJob.Patching || job is EntryJob.Done) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PatchProgressRing((job as? EntryJob.Patching)?.progress ?: 1f, done = job is EntryJob.Done)
        }
        Spacer(Modifier.height(Space.m))
    }
    if (made != null && job !is EntryJob.Patching) {
        if (job !is EntryJob.Done) Notice(Icons.Rounded.CheckCircle, "You've made this game. It's in My Games.", c.success)
        Spacer(Modifier.height(Space.s))
        RetraButton("Play ${e.title}", { actions.play(made, false, "store") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        return
    }
    if (job is EntryJob.Patching) return

    if (e.download == null) {
        Notice(
            Icons.Rounded.AutoFixHigh,
            "${e.author} shares this patch on their own page. Download the ${e.formatLabel} file there, then open it in the Patcher with your ${base.title} ROM.",
            c.accent,
        )
        Spacer(Modifier.height(Space.s))
        RetraButton("Get patch from creator", { actions.openUrl(e.homepage) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.OpenInNew)
        Spacer(Modifier.height(Space.xs))
        RetraButton("Open the Patcher", { actions.openPatcher(state.ownedBases[base.crc32.uppercase()]?.first) }, Modifier.fillMaxWidth(), style = ButtonStyle.SECONDARY)
        Spacer(Modifier.height(Space.xs))
        Text("Needs: ${base.title} · ${base.gameCode} · CRC32 ${base.crc32}", style = RetraType.caption, color = c.inkMuted)
        return
    }

    // Step 1: the patch file.
    val ready = e.id in state.patchesReady
    Step(1, "Patch file", done = ready) {
        when {
            job is EntryJob.Downloading -> {
                ProgressPill(job.progress)
                Spacer(Modifier.height(Space.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Downloading… ${(job.progress * 100).toInt()} %", style = RetraType.caption, color = c.inkMuted, modifier = Modifier.weight(1f))
                    RetraButton("Cancel", { h.cancel(e.id) }, style = ButtonStyle.TERTIARY, compact = true)
                }
            }
            ready -> Text("Downloaded and verified.", style = RetraType.label, color = c.success)
            else -> RetraButton("Download patch · ${formatBytes(e.download.size)}", { h.downloadPatch(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CloudDownload, compact = true)
        }
    }
    // Step 2: the base ROM.
    val owned = state.ownedBases[base.crc32.uppercase()]
    val picked = state.pickedBases[e.id]
    Step(2, "Your ${base.gameCode} cartridge", done = owned != null || picked?.matches == true) {
        when {
            owned != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = c.success, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("You have it: ${owned.second}", style = RetraType.label, color = c.success)
            }
            picked != null && !picked.matches -> {
                Text(
                    "\"${picked.name}\" has CRC32 ${picked.crc32}, but this patch needs ${base.title} (CRC32 ${base.crc32}). Patching a different revision usually breaks the game.",
                    style = RetraType.label, color = c.warning,
                )
                Spacer(Modifier.height(Space.xs))
                RetraButton("Choose another file", { h.pickBase(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.FolderOpen, style = ButtonStyle.SECONDARY, compact = true)
            }
            else -> {
                Text("Needs ${base.title} (${base.gameCode}, CRC32 ${base.crc32}). Retra never downloads it for you; use your own dump.", style = RetraType.label, color = c.inkMuted)
                Spacer(Modifier.height(Space.xs))
                RetraButton("Choose my ROM file", { h.pickBase(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.FolderOpen, style = ButtonStyle.SECONDARY, compact = true)
            }
        }
    }
    if (job is EntryJob.Failed) {
        Spacer(Modifier.height(Space.s))
        Notice(Icons.Rounded.WarningAmber, job.message, c.warning)
    }
    Spacer(Modifier.height(Space.m))
    val baseId = owned?.first ?: picked?.gameId
    val canMake = ready && baseId != null
    val force = owned == null && picked != null && !picked.matches
    RetraButton(
        if (force) "Make it anyway" else "Make this game",
        { if (baseId != null) h.makeGame(e, baseId, force) },
        Modifier.fillMaxWidth(),
        icon = Icons.Rounded.AutoFixHigh,
        enabled = canMake,
        style = if (force) ButtonStyle.SECONDARY else ButtonStyle.PRIMARY,
    )
}

@Composable
private fun Step(n: Int, title: String, done: Boolean, content: @Composable () -> Unit) {
    val c = Retra.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        Box(
            Modifier.size(28.dp).clip(Radius.pillShape).background(if (done) c.success else c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.CheckCircle, "Done", tint = if (c.isDark) c.sky else Color.White, modifier = Modifier.size(18.dp))
            else Text("$n", style = RetraType.label, color = c.onAccentSoft)
        }
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = RetraType.bodyStrong, color = c.ink)
            Spacer(Modifier.height(Space.xxs))
            content()
        }
    }
    Divider(Modifier.padding(top = Space.xs))
}

@Composable
fun Notice(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color, modifier: Modifier = Modifier) {
    val c = Retra.colors
    Row(
        modifier.fillMaxWidth().clip(Radius.smallShape).background(tint.copy(alpha = 0.12f)).padding(Space.s),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Space.xs))
        Text(text, style = RetraType.label, color = c.ink)
    }
}
