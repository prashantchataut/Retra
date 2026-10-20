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
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
    val pickPatch = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val e = pickingFor
        if (uri != null && e != null) vm.importPatch(e, uri)
    }
    StoreContent(
        state = state,
        selectedId = selected,
        onSelect = { selected = it },
        onQuery = vm::setQuery,
        onRefresh = { vm.refresh() },
        handlers = EntryHandlers(
            getRom = vm::getRom,
            install = vm::install,
            cancel = vm::cancel,
            pickBase = { e -> pickingFor = e; pickBase.launch(arrayOf("*/*")) },
            pickPatch = { e -> pickingFor = e; pickPatch.launch(arrayOf("*/*")) },
            makeGame = { e, base, force -> vm.makeGame(e, base, force) },
            reset = vm::resetJob,
        ),
    )
}

class EntryHandlers(
    val getRom: (CatalogEntry) -> Unit = {},
    val install: (CatalogEntry) -> Unit = {},
    val cancel: (String) -> Unit = {},
    val pickBase: (CatalogEntry) -> Unit = {},
    val pickPatch: (CatalogEntry) -> Unit = {},
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
                val ready = catalog.entries.filter { e ->
                    e.isPatch && !state.installed.containsKey(e.id) && e.base?.crc32?.uppercase()?.let { it in state.ownedBases } == true
                }
                if (ready.isNotEmpty()) {
                    item("t-ready") {
                        SectionHeader(
                            "Ready with your games", Modifier.padding(g).padding(top = Space.xl, bottom = Space.s),
                            subtitle = "You already own the base ROM, so these install in one tap",
                        )
                    }
                    item("s-ready") {
                        LazyRow(contentPadding = g, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            items(ready, key = { "ready-" + it.id }) { e -> StoreCard(e, state, large = false) { onSelect(e.id) } }
                        }
                    }
                }
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
    e.isRom && e.bundled -> "Included · free"
    e.isRom -> e.download?.let { "Free · ${formatBytes(it.size)}" } ?: "Free"
    e.base?.crc32?.uppercase()?.let { it in state.ownedBases } == true -> "One-tap install"
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

/**
 * One-tap install for fan games: Retra fetches the patch when the creator hosts a direct file, finds
 * the user's own base ROM in My Games, builds the game and starts it. It only stops to ask for a file
 * it can't get by itself (the base ROM the first time, or a patch that lives on the creator's page).
 */
@Composable
private fun PatchFlow(e: CatalogEntry, state: StoreState, h: EntryHandlers) {
    val c = Retra.colors
    val actions = LocalAppActions.current
    val base = e.base ?: return
    val job = state.jobs[e.id] ?: EntryJob.Idle
    val made = (job as? EntryJob.Done)?.gameId ?: state.installed[e.id]
    // Start the game when an install this sheet started finishes (not if the sheet was closed).
    var autoPlay by remember(e.id) { mutableStateOf(false) }
    LaunchedEffect(job) {
        if (autoPlay && job is EntryJob.Done) {
            autoPlay = false
            actions.play(job.gameId, false, "store")
        }
        if (job is EntryJob.Failed) autoPlay = false
    }
    val owned = state.ownedBases[base.crc32.uppercase()]
    val picked = state.pickedBases[e.id]
    val patchReady = e.id in state.patchesReady
    val baseReady = owned != null || picked?.matches == true

    if (job is EntryJob.Downloading || job is EntryJob.Patching) {
        val downloading = job is EntryJob.Downloading
        val progress = (job as? EntryJob.Downloading)?.progress ?: (job as EntryJob.Patching).progress
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PatchProgressRing(progress, done = false)
            Spacer(Modifier.height(Space.s))
            Text(if (downloading) "Downloading the patch…" else "Building your game…", style = RetraType.bodyStrong, color = c.ink)
            Text(
                if (downloading) "Checked with SHA-256 when it lands." else "Your ${base.gameCode} ROM stays untouched.",
                style = RetraType.caption, color = c.inkMuted,
            )
            if (downloading) RetraButton("Cancel", { autoPlay = false; h.cancel(e.id) }, style = ButtonStyle.TERTIARY, compact = true)
        }
        return
    }
    if (made != null) {
        if (job is EntryJob.Done) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PatchProgressRing(1f, done = true) }
            Spacer(Modifier.height(Space.m))
        }
        Notice(Icons.Rounded.CheckCircle, "Installed. It's in My Games with its own saves.", c.success)
        Spacer(Modifier.height(Space.s))
        RetraButton("Play ${e.title}", { actions.play(made, false, "store") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        return
    }

    Requirement(
        done = patchReady,
        title = "${e.formatLabel} patch",
        detail = when {
            patchReady -> "Ready"
            e.download != null -> "Downloads automatically · ${formatBytes(e.download.size)}"
            else -> "Shared on ${e.author}'s page"
        },
    )
    Requirement(
        done = baseReady,
        warn = picked != null && !picked.matches,
        title = "Your ${base.title}",
        detail = when {
            owned != null -> "Found in My Games: ${owned.second}"
            picked?.matches == true -> "Matched: ${picked.name}"
            picked != null -> "\"${picked.name}\" is another revision (CRC32 ${picked.crc32}); this patch needs CRC32 ${base.crc32}."
            else -> "Not in My Games yet · ${base.gameCode} · CRC32 ${base.crc32}"
        },
    )
    if (job is EntryJob.Failed) {
        Spacer(Modifier.height(Space.s))
        Notice(Icons.Rounded.WarningAmber, job.message, c.warning)
    }
    Spacer(Modifier.height(Space.m))
    when {
        !patchReady && e.download == null -> {
            RetraButton("Get the patch from ${e.author}", { actions.openUrl(e.homepage) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.OpenInNew)
            Spacer(Modifier.height(Space.xs))
            RetraButton("I've downloaded it · choose file", { autoPlay = true; h.pickPatch(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.FolderOpen, style = ButtonStyle.SECONDARY)
            Spacer(Modifier.height(Space.xs))
            Text("Pick the ${e.formatLabel} or zip you saved. Retra builds and starts the game from there.", style = RetraType.caption, color = c.inkMuted)
        }
        !baseReady && picked != null -> {
            RetraButton("Choose another file", { autoPlay = true; h.pickBase(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.FolderOpen)
            Spacer(Modifier.height(Space.xs))
            RetraButton(
                "Use it anyway", { autoPlay = true; h.makeGame(e, picked.gameId, true) }, Modifier.fillMaxWidth(),
                style = ButtonStyle.SECONDARY, enabled = patchReady,
            )
        }
        !baseReady -> {
            RetraButton("Choose my ${base.gameCode} ROM", { autoPlay = true; h.pickBase(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.FolderOpen)
            Spacer(Modifier.height(Space.xs))
            Text(
                "Retra never downloads commercial games. Pick the ROM you dumped from your own cartridge once; every fan game built on it then installs in one tap.",
                style = RetraType.caption, color = c.inkMuted,
            )
        }
        else -> {
            RetraButton("Install & play", { autoPlay = true; h.install(e) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
            Spacer(Modifier.height(Space.xs))
            Text("Takes a few seconds. The game appears in My Games too.", style = RetraType.caption, color = c.inkMuted)
        }
    }
}

@Composable
private fun Requirement(done: Boolean, title: String, detail: String, warn: Boolean = false) {
    val c = Retra.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.Top) {
        Icon(
            when {
                warn -> Icons.Rounded.WarningAmber
                done -> Icons.Rounded.CheckCircle
                else -> Icons.Rounded.RadioButtonUnchecked
            },
            null,
            tint = when {
                warn -> c.warning
                done -> c.success
                else -> c.inkMuted
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(Space.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = RetraType.bodyStrong, color = c.ink)
            Text(detail, style = RetraType.label, color = if (warn) c.warning else c.inkMuted)
        }
    }
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
