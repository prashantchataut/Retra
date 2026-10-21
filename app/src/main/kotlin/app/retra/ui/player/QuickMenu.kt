package app.retra.ui.player

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.retra.data.ScreenFilter
import app.retra.data.Settings
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.CoverModel
import app.retra.ui.components.GameCover
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.PlainIconButton
import app.retra.ui.components.RetraButton
import app.retra.ui.components.SectionHeader
import app.retra.ui.components.Segmented
import app.retra.ui.components.SliderRow
import app.retra.ui.games.CheatsEditor
import app.retra.ui.games.SlotCard
import app.retra.ui.store.Notice
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space

/** [EXIT] is the main page with the leave-the-game choice already open. */
enum class MenuPage { MAIN, CHEATS, EXIT }

/** Leaving the game: keep a Continue point, or just go. In-game (battery) saves are kept either way. */
@Composable
private fun ExitChoice(onSaveAndExit: () -> Unit, onExitWithoutSaving: () -> Unit, onCancel: () -> Unit) {
    val c = Retra.colors
    Column(
        Modifier
            .padding(top = Space.s)
            .fillMaxWidth()
            .clip(Radius.mediumShape)
            .background(c.inkFaint)
            .padding(Space.m),
    ) {
        Text("Leave the game?", style = RetraType.headline, color = c.ink)
        Spacer(Modifier.height(Space.xxs))
        Text(
            "Progress you saved inside the game is always kept. Save & exit also keeps a Continue point right here.",
            style = RetraType.label, color = c.inkMuted,
        )
        Spacer(Modifier.height(Space.s))
        RetraButton("Save & exit", onSaveAndExit, Modifier.fillMaxWidth(), icon = Icons.Rounded.Save)
        Spacer(Modifier.height(Space.xs))
        RetraButton("Exit without saving", onExitWithoutSaving, Modifier.fillMaxWidth(), style = ButtonStyle.SECONDARY)
        RetraButton("Keep playing", onCancel, Modifier.fillMaxWidth(), style = ButtonStyle.TERTIARY, compact = true)
    }
}

/** The in-game glass menu. Opening it pauses the game. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickMenu(state: PlayerUiState, settings: Settings, cb: PlayerCallbacks, initialPage: MenuPage = MenuPage.MAIN) {
    val c = Retra.colors
    var page by remember(state.menuOpen) { mutableStateOf(if (initialPage == MenuPage.EXIT) MenuPage.MAIN else initialPage) }
    var saveMode by remember(state.menuOpen) { mutableStateOf(true) }
    var confirmReset by remember(state.menuOpen) { mutableStateOf(false) }
    var confirmExit by remember(state.menuOpen) { mutableStateOf(initialPage == MenuPage.EXIT) }
    GlassSheet(
        visible = state.menuOpen,
        onDismiss = { if (page != MenuPage.MAIN) page = MenuPage.MAIN else cb.onMenu(false) },
        title = if (page == MenuPage.CHEATS) null else state.title,
    ) {
        if (page == MenuPage.CHEATS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlainIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back to menu", { page = MenuPage.MAIN })
                Text("Cheats", style = RetraType.title, color = c.ink)
            }
            Spacer(Modifier.height(Space.s))
            CheatsEditor(state.cheats, cb.onCheats, book = state.cheatBook, onImportFile = cb.onImportCheats)
            return@GlassSheet
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            RetraButton("Resume", { cb.onMenu(false) }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
            RetraButton("Exit", { confirmExit = !confirmExit }, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.ExitToApp, style = ButtonStyle.SECONDARY)
        }
        AnimatedVisibility(confirmExit) {
            ExitChoice(onSaveAndExit = cb.onExit, onExitWithoutSaving = cb.onExitWithoutSaving, onCancel = { confirmExit = false; cb.onMenu(false) })
        }
        Spacer(Modifier.height(Space.l))
        SectionHeader("Save states")
        Spacer(Modifier.height(Space.xs))
        Segmented(listOf(true to "Save here", false to "Load"), saveMode, { saveMode = it })
        Spacer(Modifier.height(Space.s))
        val slots = state.slots.filter { saveMode.not() || it.slot != 0 }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            items(slots, key = { it.slot }) { slot ->
                SlotCard(
                    slot,
                    onClick = {
                        if (saveMode) cb.onSaveState(slot.slot) else if (slot.exists) cb.onLoadState(slot.slot)
                    },
                    onLongClick = null,
                    modifier = Modifier.width(128.dp),
                )
            }
        }
        Spacer(Modifier.height(Space.l))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            RetraButton("Screenshot", cb.onScreenshot, icon = Icons.Rounded.CameraAlt, style = ButtonStyle.SECONDARY, compact = true)
            RetraButton("Cheats" + if (state.cheats.any { it.enabled }) " · ${state.cheats.count { it.enabled }} on" else "", { page = MenuPage.CHEATS }, icon = Icons.Rounded.Code, style = ButtonStyle.SECONDARY, compact = true)
            RetraButton("Reset", { confirmReset = true }, icon = Icons.Rounded.RestartAlt, style = ButtonStyle.SECONDARY, compact = true)
        }
        state.lastCapture?.let { path ->
            Spacer(Modifier.height(Space.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(96.dp).aspectRatio(1.5f).clip(Radius.smallShape)) {
                    GameCover(CoverModel("", "", 0, filePath = path), Modifier.fillMaxSize(), showTitle = false)
                }
                Spacer(Modifier.width(Space.s))
                RetraButton("Share", { cb.onShare(path) }, icon = Icons.Rounded.Share, style = ButtonStyle.TERTIARY, compact = true)
            }
        }
        if (confirmReset) {
            Spacer(Modifier.height(Space.s))
            Notice(Icons.Rounded.RestartAlt, "Reset the game? Anything you haven't saved in-game or in a state is lost.", c.warning)
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                RetraButton("Cancel", { confirmReset = false }, Modifier.weight(1f), style = ButtonStyle.SECONDARY, compact = true)
                RetraButton("Reset", { confirmReset = false; cb.onReset() }, Modifier.weight(1f), style = ButtonStyle.DANGER, compact = true)
            }
        }
        Spacer(Modifier.height(Space.l))
        SectionHeader("Play")
        Spacer(Modifier.height(Space.xs))
        Text("Fast-forward speed", style = RetraType.label, color = c.inkMuted)
        Spacer(Modifier.height(Space.xxs))
        Segmented(listOf(2 to "2×", 3 to "3×", 4 to "4×"), settings.fastForwardSpeed, { v -> cb.onSettings { it.copy(fastForwardSpeed = v) } })
        Spacer(Modifier.height(Space.s))
        Text("Screen", style = RetraType.label, color = c.inkMuted)
        Spacer(Modifier.height(Space.xxs))
        Segmented(listOf(ScreenFilter.SHARP to "Sharp pixels", ScreenFilter.SMOOTH to "Smooth"), settings.screenFilter, { v -> cb.onSettings { it.copy(screenFilter = v) } })
        Spacer(Modifier.height(Space.xs))
        SliderRow(
            "Controls opacity", settings.controlsOpacity, { v -> cb.onSettings { it.copy(controlsOpacity = v) } },
            "${(settings.controlsOpacity * 100).toInt()} %", range = 0.2f..1f,
        )
        Spacer(Modifier.height(Space.xs))
    }
}
