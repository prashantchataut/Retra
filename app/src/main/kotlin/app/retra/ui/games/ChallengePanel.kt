package app.retra.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.AppContainer
import app.retra.core.ChallengeMode
import app.retra.core.ChallengePolicy
import app.retra.core.ChallengeRun
import app.retra.core.Encounter
import app.retra.core.EncounterStatus
import app.retra.core.GameClassifier
import app.retra.core.Nuzlocke
import app.retra.core.RouteSuggestions
import app.retra.core.RunState
import app.retra.core.formatPlaytime
import app.retra.data.GameCategory
import app.retra.data.MessageTone
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraChip
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.Tag
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import java.util.UUID

/**
 * Challenge modes for one game: start a No-Cheat Run or a Nuzlocke, watch the run clock, and keep the Nuzlocke log.
 * Used on the game page and inside the player menu, so a catch can be logged without leaving the game.
 */
@Composable
fun ChallengePanel(container: AppContainer, gameId: String, modifier: Modifier = Modifier) {
    val c = Retra.colors
    val runs by container.challenges.runs.collectAsStateWithLifecycle()
    val games by container.library.games.collectAsStateWithLifecycle()
    val game = games.firstOrNull { it.id == gameId } ?: return
    val run = runs[gameId]
    val pokemon = game.category == GameCategory.POKEMON || game.category == GameCategory.FAN_GAME || GameClassifier.isPokemon(game.gameCode, game.romTitle)
    var confirmEnd by remember { mutableStateOf(false) }

    Column(modifier.animateContentSize()) {
        if (run == null || !run.active) {
            if (run != null) ResultCard(run, game.playTimeMs)
            Text("Pick a challenge", style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(vertical = Space.xxs))
            ChallengePolicy.available(pokemon).forEach { mode ->
                ModeCard(mode) { container.challenges.start(gameId, mode, game.playTimeMs) ; container.messages.show("${mode.label} started. Good luck!", MessageTone.SUCCESS) }
                Spacer(Modifier.height(Space.xs))
            }
            if (!pokemon) Text("Nuzlocke appears for Pokémon games.", style = RetraType.caption, color = c.inkMuted)
            return@Column
        }

        SealCard(run, game.playTimeMs)
        Spacer(Modifier.height(Space.xs))
        if (run.mode == ChallengeMode.NUZLOCKE) {
            NuzlockeTracker(container, gameId, game.gameCode, run)
            Spacer(Modifier.height(Space.xs))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            RetraButton("I beat it!", { container.challenges.finish(gameId, RunState.COMPLETED); container.messages.show("Run complete. Nicely done!", MessageTone.SUCCESS) }, Modifier.weight(1f), icon = Icons.Rounded.EmojiEvents)
            RetraButton(if (confirmEnd) "Really end" else "End run", { if (confirmEnd) { container.challenges.finish(gameId, RunState.ABANDONED); confirmEnd = false } else confirmEnd = true }, style = if (confirmEnd) ButtonStyle.DANGER else ButtonStyle.SECONDARY)
        }
    }
}

@Composable
private fun ModeCard(mode: ChallengeMode, onStart: () -> Unit) {
    val c = Retra.colors
    val (icon, blurb) = when (mode) {
        ChallengeMode.HONEST -> Icons.Rounded.Shield to "Cheats, rewind, save-state loading and reset are locked. Fast-forward and in-game saves stay. A sealed run you can be proud of."
        else -> Icons.Rounded.LocalFireDepartment to "Everything in the No-Cheat Run, plus the classic rules: one encounter per area, fainted Pokémon are gone, nickname them all. Retra keeps your log."
    }
    GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape) {
        Row(Modifier.padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.onAccentSoft)
            }
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text(mode.label, style = RetraType.headline, color = c.ink)
                Text(blurb, style = RetraType.label, color = c.inkMuted)
            }
            Spacer(Modifier.width(Space.xs))
            RetraButton("Start", onStart, compact = true)
        }
    }
}

@Composable
internal fun SealCard(run: ChallengeRun, playMs: Long) {
    val c = Retra.colors
    val pulse by rememberInfiniteTransition(label = "seal").animateFloat(
        1f, if (Retra.reduceMotion) 1f else 1.08f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse",
    )
    GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape, wash = c.accentSoft) {
        Column(Modifier.padding(Space.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).scale(pulse).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, null, tint = c.onAccent)
                }
                Spacer(Modifier.width(Space.s))
                Column(Modifier.weight(1f)) {
                    Text(run.mode.label + " · sealed", style = RetraType.headline, color = c.onAccentSoft)
                    Text("Run time ${formatPlaytime((playMs - run.playMsAtStart).coerceAtLeast(0))}", style = RetraType.label, color = c.onAccentSoft)
                }
            }
            Spacer(Modifier.height(Space.xs))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Cheats", "Rewind", "Load states", "Reset").forEach { Tag("$it locked", icon = Icons.Rounded.Lock, color = c.onAccentSoft, background = c.accent.copy(alpha = 0.14f)) }
            }
            if (run.blockedAttempts > 0) {
                Spacer(Modifier.height(Space.xs))
                Text("Temptations resisted: ${run.blockedAttempts}", style = RetraType.caption, color = c.onAccentSoft)
            }
        }
    }
}

@Composable
private fun ResultCard(run: ChallengeRun, playMs: Long) {
    val c = Retra.colors
    val (title, tone) = when (run.state) {
        RunState.COMPLETED -> "Last run: ${run.mode.label} complete" to c.success
        RunState.FAILED -> "Last run: ${run.mode.label} ended in a wipe" to c.danger
        else -> "Last run: ${run.mode.label} ended early" to c.inkMuted
    }
    GlassSurface(Modifier.fillMaxWidth().padding(bottom = Space.xs), shape = Radius.mediumShape) {
        Column(Modifier.padding(Space.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, null, tint = tone)
                Spacer(Modifier.width(Space.xs))
                Text(title, style = RetraType.bodyStrong, color = c.ink)
            }
            if (run.mode == ChallengeMode.NUZLOCKE && run.encounters.isNotEmpty())
                Text("${run.alive.size} survived · ${run.graveyard.size} fainted · ${Nuzlocke.survivalRate(run)}% survival", style = RetraType.label, color = c.inkMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NuzlockeTracker(container: AppContainer, gameId: String, gameCode: String, run: ChallengeRun) {
    val c = Retra.colors
    var area by remember(run.encounters.size) { mutableStateOf("") }
    var species by remember(run.encounters.size) { mutableStateOf("") }
    var nickname by remember(run.encounters.size) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val suggestion = RouteSuggestions.next(gameCode, run)

    GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape) {
        Column(Modifier.padding(Space.m)) {
            Text("Encounter log", style = RetraType.headline, color = c.ink)
            Spacer(Modifier.height(Space.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Stat("Alive", run.alive.size.toString(), c.success)
                Stat("Fainted", run.graveyard.size.toString(), c.danger)
                Stat("Survival", "${Nuzlocke.survivalRate(run)}%", c.ink)
            }
            Spacer(Modifier.height(Space.s))
            RetraTextField(area, { area = it; error = null }, "Area (e.g. Route 101)")
            if (suggestion != null && area.isBlank()) {
                Spacer(Modifier.height(6.dp))
                RetraChip("Next: $suggestion", false, { area = suggestion }, icon = Icons.Rounded.Add)
            }
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                RetraTextField(species, { species = it; error = null }, "Pokémon", Modifier.weight(1f))
                RetraTextField(nickname, { nickname = it }, "Nickname", Modifier.weight(1f))
            }
            error?.let { Text(it, style = RetraType.label, color = c.danger, modifier = Modifier.padding(top = Space.xxs)) }
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                fun log(status: EncounterStatus) {
                    val problem = Nuzlocke.blocker(run, area, species.takeIf { status == EncounterStatus.ALIVE }.orEmpty())
                    if (problem != null) { error = problem; return }
                    if (status == EncounterStatus.ALIVE && run.rules.mustNickname && nickname.isBlank() && species.isBlank()) { error = "Add the Pokémon's name."; return }
                    container.challenges.addEncounter(gameId, Encounter(UUID.randomUUID().toString(), area.trim(), species.trim(), nickname.trim(), status))
                }
                RetraButton("Caught", { log(EncounterStatus.ALIVE) }, Modifier.weight(1f), compact = true)
                RetraButton("Missed", { log(EncounterStatus.MISSED) }, Modifier.weight(1f), style = ButtonStyle.SECONDARY, compact = true)
                RetraButton("Skipped", { log(EncounterStatus.SKIPPED) }, Modifier.weight(1f), style = ButtonStyle.TERTIARY, compact = true)
            }
            if (run.encounters.isNotEmpty()) {
                Spacer(Modifier.height(Space.s))
                GlassGroup {
                    run.encounters.asReversed().take(30).forEachIndexed { i, e ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    listOf(e.nickname, e.species).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "No Pokémon" },
                                    style = RetraType.bodyStrong, color = if (e.status == EncounterStatus.DEAD) c.inkMuted else c.ink,
                                )
                                Text(e.area, style = RetraType.caption, color = c.inkMuted)
                            }
                            Tag(e.status.label, color = when (e.status) { EncounterStatus.ALIVE -> c.success; EncounterStatus.DEAD -> c.danger; else -> c.inkMuted })
                            if (e.status == EncounterStatus.ALIVE) {
                                Spacer(Modifier.width(Space.xxs))
                                RetraButton("Fainted", { container.challenges.setStatus(gameId, e.id, EncounterStatus.DEAD) }, style = ButtonStyle.TERTIARY, compact = true)
                            }
                        }
                        if (i < minOf(run.encounters.size, 30) - 1) Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, tone: androidx.compose.ui.graphics.Color) {
    val c = Retra.colors
    Column(Modifier.clip(Radius.smallShape).background(c.inkFaint).padding(horizontal = Space.s, vertical = Space.xs)) {
        Text(value, style = RetraType.title, color = tone)
        Text(label, style = RetraType.caption, color = c.inkMuted)
    }
}
