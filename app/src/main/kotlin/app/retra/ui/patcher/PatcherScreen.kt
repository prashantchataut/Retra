package app.retra.ui.patcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.retra.AppContainer
import app.retra.core.formatBytes
import app.retra.data.Game
import app.retra.ui.LocalAppActions
import app.retra.ui.coverModel
import app.retra.ui.gutter
import app.retra.ui.listPadding
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.InfoRow
import app.retra.ui.components.MochiMessage
import app.retra.ui.components.MochiMood
import app.retra.ui.components.PatchProgressRing
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.ScreenHeader
import app.retra.ui.components.SwitchRow
import app.retra.ui.components.squishClickable
import app.retra.ui.components.staggerIn
import app.retra.ui.store.Notice
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space

@Composable
fun PatcherScreen(container: AppContainer, baseGameId: String?, patchPath: String?, patchName: String?) {
    val vm: PatcherViewModel = viewModel(factory = PatcherViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(baseGameId, patchPath) { vm.preselect(baseGameId, patchPath, patchName) }
    val pickBase = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::chooseBaseFile) }
    val pickPatch = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::choosePatchFile) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.export(uri, context.contentResolver)
    }
    PatcherContent(
        state = state,
        games = games,
        onLibraryBase = vm::chooseLibraryBase,
        onBaseFile = { pickBase.launch(arrayOf("*/*")) },
        onPatchFile = { pickPatch.launch(arrayOf("*/*")) },
        onStrict = vm::setStrict,
        onName = vm::setOutputName,
        onApply = vm::apply,
        onAdd = vm::addToLibrary,
        onExport = { export.launch("${state.outputName.ifBlank { "patched" }}.gba") },
        onReset = vm::reset,
        onDismissError = vm::dismissError,
    )
}

@Composable
fun PatcherContent(
    state: PatcherState,
    games: List<Game>,
    onLibraryBase: (String) -> Unit = {},
    onBaseFile: () -> Unit = {},
    onPatchFile: () -> Unit = {},
    onStrict: (Boolean) -> Unit = {},
    onName: (String) -> Unit = {},
    onApply: () -> Unit = {},
    onAdd: () -> Unit = {},
    onExport: () -> Unit = {},
    onReset: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val c = Retra.colors
    val g = gutter()
    val actions = LocalAppActions.current
    var picking by remember { mutableStateOf(false) }
    val phase = state.phase
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(Space.l)) {
            item("header") {
                ScreenHeader("Patcher", Modifier.padding(g), subtitle = "Turn a patch and a game you own into a new game")
            }
            if (phase is PatchPhase.Applying || phase is PatchPhase.Done) {
                item("progress") {
                    GlassSurface(Modifier.padding(g).padding(top = Space.l).fillMaxWidth(), shape = Radius.largeShape) {
                        Column(Modifier.fillMaxWidth().padding(Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
                            PatchProgressRing((phase as? PatchPhase.Applying)?.progress ?: 1f, done = phase is PatchPhase.Done)
                            if (phase is PatchPhase.Done) {
                                Spacer(Modifier.height(Space.xs))
                                Text("${formatBytes(phase.size)} · CRC32 ${phase.crc32}", style = RetraType.label, color = c.inkMuted)
                                phase.warnings.forEach {
                                    Spacer(Modifier.height(Space.xs))
                                    Notice(Icons.Rounded.WarningAmber, it, c.warning)
                                }
                                Spacer(Modifier.height(Space.l))
                                if (phase.gameId != null) {
                                    RetraButton("Play now", { actions.play(phase.gameId, false, "patcher") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
                                } else {
                                    RetraButton("Add to My Games", onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.LibraryAdd)
                                }
                                Spacer(Modifier.height(Space.xs))
                                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                                    RetraButton("Save .gba file", onExport, Modifier.weight(1f), icon = Icons.Rounded.SaveAlt, style = ButtonStyle.SECONDARY, compact = true)
                                    RetraButton("Patch another", onReset, Modifier.weight(1f), icon = Icons.Rounded.Refresh, style = ButtonStyle.SECONDARY, compact = true)
                                }
                            }
                        }
                    }
                }
                return@LazyColumn
            }
            item("base") {
                StepCard(1, "Base game", Modifier.padding(g).padding(top = Space.l).staggerIn(0), done = state.base != null) {
                    val b = state.base
                    if (b != null) {
                        Text(b.name, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${b.gameCode.ifBlank { "No code" }} · ${formatBytes(b.size)} · CRC32 ${b.crc32}", style = RetraType.caption, color = c.inkMuted)
                        Spacer(Modifier.height(Space.s))
                    } else {
                        Text("The clean ROM the patch was made for, dumped from your own cartridge.", style = RetraType.label, color = c.inkMuted)
                        Spacer(Modifier.height(Space.s))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        RetraButton("My Games", { picking = true }, Modifier.weight(1f), icon = Icons.Rounded.SportsEsports, style = ButtonStyle.SECONDARY, compact = true, enabled = games.isNotEmpty())
                        RetraButton("A file", onBaseFile, Modifier.weight(1f), icon = Icons.Rounded.FolderOpen, style = ButtonStyle.SECONDARY, compact = true)
                    }
                }
            }
            item("patch") {
                StepCard(2, "Patch", Modifier.padding(g).padding(top = Space.s).staggerIn(1), done = state.patch != null) {
                    val p = state.patch
                    if (p != null) {
                        Text(p.name, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${p.format} · ${formatBytes(p.size)}", style = RetraType.caption, color = c.inkMuted)
                        Spacer(Modifier.height(Space.xs))
                        InfoRow("Expects base", p.sourceCrc32?.let { "CRC32 $it" + (p.sourceSize?.let { s -> " · ${formatBytes(s)}" } ?: "") } ?: "Not recorded by ${p.format}")
                        InfoRow("Makes", p.targetCrc32?.let { "CRC32 $it" + (p.targetSize?.let { s -> " · ${formatBytes(s)}" } ?: "") } ?: p.targetSize?.let { formatBytes(it) } ?: "—")
                        p.metadata?.let { InfoRow("Notes", it) }
                        Spacer(Modifier.height(Space.xs))
                    } else {
                        Text("IPS, UPS, BPS or xdelta. Zips work too.", style = RetraType.label, color = c.inkMuted)
                        Spacer(Modifier.height(Space.s))
                    }
                    RetraButton(if (p == null) "Choose patch" else "Choose another", onPatchFile, Modifier.fillMaxWidth(), icon = Icons.Rounded.Description, style = ButtonStyle.SECONDARY, compact = true)
                }
            }
            if (state.base != null && state.patch != null) {
                item("match") {
                    Box(Modifier.padding(g).padding(top = Space.s)) {
                        when (state.match) {
                            CrcMatch.MATCH -> Notice(Icons.Rounded.CheckCircle, "Your base game matches what this patch expects.", c.success)
                            CrcMatch.MISMATCH -> Notice(
                                Icons.Rounded.WarningAmber,
                                "This patch was made for a different ROM (CRC32 ${state.patch.sourceCrc32}); yours is ${state.base.crc32}. It's probably another region or revision. Turn off the safety check only if the creator says it's fine.",
                                c.warning,
                            )
                            CrcMatch.UNKNOWN -> Notice(Icons.Rounded.HelpOutline, "${state.patch.format} patches can't check the base game. Make sure it's the version the creator names.", c.accent)
                        }
                    }
                }
                item("options") {
                    GlassGroup(Modifier.padding(g).padding(top = Space.s)) {
                        SwitchRow("Safety check", state.strict, onStrict, subtitle = "Stop if the base game doesn't match")
                        Divider()
                        Spacer(Modifier.height(Space.xs))
                        Text("New game's name", style = RetraType.label, color = c.inkMuted)
                        Spacer(Modifier.height(Space.xxs))
                        RetraTextField(state.outputName, onName, "Name", Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Space.s))
                    }
                }
            }
            if (phase is PatchPhase.Failed) {
                item("error") {
                    Box(Modifier.padding(g).padding(top = Space.s)) { Notice(Icons.Rounded.WarningAmber, phase.message, c.danger) }
                }
            }
            item("apply") {
                RetraButton(
                    "Apply patch", onApply,
                    Modifier.padding(g).padding(top = Space.l).fillMaxWidth(),
                    icon = Icons.Rounded.AutoFixHigh,
                    enabled = state.canApply,
                )
                Text(
                    "Retra never provides commercial ROMs. The Game Library lists fan games whose patches work here.",
                    style = RetraType.caption, color = c.inkMuted,
                    modifier = Modifier.padding(g).padding(top = Space.s),
                )
            }
        }
        GlassSheet(visible = picking, onDismiss = { picking = false }, title = "Pick a base game") {
            if (games.isEmpty()) MochiMessage("No games yet", "Import your ROM first.", mood = MochiMood.SLEEPY)
            games.sortedBy { it.title.lowercase() }.forEach { game ->
                Row(
                    Modifier.fillMaxWidth().squishClickable({ onLibraryBase(game.id); picking = false }, pressed = 0.98f, label = game.title).padding(vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(72.dp).aspectRatio(3f / 2f).clip(Radius.smallShape)) { GameCover(game.coverModel(null), Modifier.fillMaxSize(), showTitle = false) }
                    Spacer(Modifier.width(Space.s))
                    Column(Modifier.weight(1f)) {
                        Text(game.title, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${game.gameCode} · CRC32 ${game.crc32}", style = RetraType.caption, color = c.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(n: Int, title: String, modifier: Modifier, done: Boolean, content: @Composable () -> Unit) {
    val c = Retra.colors
    GlassSurface(modifier.fillMaxWidth().widthIn(max = 640.dp), shape = Radius.mediumShape) {
        Column(Modifier.padding(Space.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step $n", style = RetraType.caption, color = if (done) c.success else c.accent)
                if (done) {
                    Spacer(Modifier.width(Space.xxs))
                    androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, "Done", tint = c.success, modifier = Modifier.height(14.dp))
                }
            }
            Text(title, style = RetraType.headline, color = c.ink)
            Spacer(Modifier.height(Space.xs))
            content()
        }
    }
}
