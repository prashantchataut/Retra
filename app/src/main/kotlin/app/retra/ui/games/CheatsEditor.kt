package app.retra.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.CheatImport
import app.retra.data.Cheat
import app.retra.data.CheatPackCheat
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.PlainIconButton
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.squishClickable
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import java.util.UUID

/** Ready-made cheats Retra found for a game (its built-in cheat book). */
@Immutable
data class CheatBook(val title: String, val cheats: List<CheatPackCheat>)

/**
 * Cheat list with toggles, the game's built-in cheat book (one tap to add), and a paste box that
 * understands whole cheat lists. Shared by game details and the player menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheatsEditor(
    cheats: List<Cheat>,
    onChange: (List<Cheat>) -> Unit,
    modifier: Modifier = Modifier,
    startAdding: Boolean = false,
    book: CheatBook? = null,
    onImportFile: (() -> Unit)? = null,
) {
    val c = Retra.colors
    var adding by remember { mutableStateOf(startAdding) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun addFromBook(entry: CheatPackCheat) {
        // Enabler ("master") codes have to be on for the rest of the book to work; bring them along.
        val have = cheats.map { it.code }.toSet()
        val masters = book?.cheats.orEmpty().filter { it.master && it.code !in have && it.code != entry.code }
        val added = (masters + entry).map { Cheat(UUID.randomUUID().toString(), it.name, it.code, true) }
        onChange(cheats + added)
    }

    Column(modifier.fillMaxWidth()) {
        if (cheats.isEmpty() && !adding) {
            Text(
                if (book != null) "Pick from the cheat book below, or paste your own codes. They turn on the next time you play."
                else "No cheats yet. Paste codes or import a cheat file; they're saved per game and turn on the next time you play.",
                style = RetraType.body, color = c.inkMuted,
            )
            Spacer(Modifier.height(Space.s))
        }
        cheats.forEachIndexed { i, cheat ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().padding(vertical = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cheat.name, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        cheat.code.lines().first() + if (cheat.code.lines().size > 1) " +${cheat.code.lines().size - 1}" else "",
                        style = RetraType.caption.copy(fontFamily = FontFamily.Monospace), color = c.inkMuted, maxLines = 1,
                    )
                }
                PlainIconButton(Icons.Rounded.DeleteOutline, "Delete ${cheat.name}", { onChange(cheats.filterNot { it.id == cheat.id }) }, tint = c.inkMuted)
                Switch(
                    checked = cheat.enabled,
                    onCheckedChange = { on -> onChange(cheats.map { if (it.id == cheat.id) it.copy(enabled = on) else it }) },
                    colors = SwitchDefaults.colors(checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, uncheckedThumbColor = c.inkMuted, uncheckedTrackColor = c.inkFaint),
                )
            }
        }
        AnimatedVisibility(adding) {
            Column {
                Spacer(Modifier.height(Space.s))
                RetraTextField(name, { name = it.take(40) }, "Name (e.g. Infinite money)", Modifier.fillMaxWidth(), imeAction = ImeAction.Next)
                Spacer(Modifier.height(Space.xs))
                RetraTextField(code, { code = it; error = null }, "Codes, or a whole cheat list", Modifier.fillMaxWidth(), singleLine = false, monospace = true)
                if (error != null) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(error!!, style = RetraType.label, color = c.danger)
                }
                Text(
                    "Raw AAAAAAAA:VV, CodeBreaker, GameShark and Action Replay. Paste several cheats at once as \"name\" lines followed by their codes, or a RetroArch .cht file.",
                    style = RetraType.caption, color = c.inkMuted, modifier = Modifier.padding(top = Space.xxs),
                )
                Spacer(Modifier.height(Space.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    RetraButton("Cancel", { adding = false; error = null }, Modifier.weight(1f), style = ButtonStyle.SECONDARY, compact = true)
                    RetraButton("Add", {
                        val parsed = CheatImport.parse(code) { i -> if (i == 0 && name.isNotBlank()) name else "Cheat ${cheats.size + i + 1}" }
                        when {
                            parsed.cheats.isEmpty() -> error = "That doesn't look like a cheat code. Use AAAAAAAA:VV, XXXXXXXX YYYY or XXXXXXXX YYYYYYYY."
                            else -> {
                                val single = parsed.cheats.size == 1 && name.isNotBlank()
                                onChange(cheats + parsed.cheats.map { p -> Cheat(UUID.randomUUID().toString(), if (single) name.trim() else p.name, p.code, true) })
                                name = ""; code = ""; adding = false; error = null
                            }
                        }
                    }, Modifier.weight(1f), compact = true)
                }
            }
        }
        if (!adding) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                RetraButton("Paste codes", { adding = true }, icon = Icons.Rounded.ContentPaste, style = ButtonStyle.TERTIARY, compact = true)
                if (onImportFile != null) RetraButton("Import file", onImportFile, icon = Icons.Rounded.FileOpen, style = ButtonStyle.TERTIARY, compact = true)
            }
        }
        if (book != null && book.cheats.isNotEmpty()) {
            Spacer(Modifier.height(Space.m))
            CheatBookList(book, cheats, ::addFromBook)
        }
    }
}

@Composable
private fun CheatBookList(book: CheatBook, current: List<Cheat>, onAdd: (CheatPackCheat) -> Unit) {
    val c = Retra.colors
    var query by remember(book) { mutableStateOf("") }
    var expanded by remember(book) { mutableStateOf(false) }
    val have = current.map { it.code }.toSet()
    val listed = book.cheats.filter { !it.master && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true)) }
    val shown = if (expanded || query.isNotBlank()) listed else listed.take(PREVIEW)
    Column(Modifier.fillMaxWidth().clip(Radius.mediumShape).background(c.inkFaint).padding(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoStories, null, tint = c.onAccentSoft, modifier = Modifier.size(32.dp).clip(Radius.pillShape).background(c.accentSoft).padding(6.dp))
            Spacer(Modifier.width(Space.s))
            Column(Modifier.weight(1f)) {
                Text("Cheat book", style = RetraType.headline, color = c.ink)
                Text("${listed.size} codes for ${book.title}", style = RetraType.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (book.cheats.size > PREVIEW) {
            Spacer(Modifier.height(Space.s))
            RetraTextField(query, { query = it }, "Search cheats", Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Search)
        }
        Spacer(Modifier.height(Space.xs))
        shown.forEach { entry ->
            val added = entry.code in have
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Radius.smallShape)
                    .squishClickable({ if (!added) onAdd(entry) }, enabled = !added, pressed = 0.98f, label = "Add ${entry.name}")
                    .padding(horizontal = Space.xs, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.name, style = RetraType.label, color = c.ink, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Icon(
                    if (added) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                    if (added) "Added" else null,
                    tint = if (added) c.success else c.inkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (!expanded && query.isBlank() && listed.size > PREVIEW) {
            RetraButton("Show all ${listed.size}", { expanded = true }, icon = Icons.Rounded.Add, style = ButtonStyle.TERTIARY, compact = true)
        }
        Text(
            "From libretro's cheat database (CC BY-SA 4.0). Codes are written for the original cartridge; some may not work on every revision.",
            style = RetraType.caption, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs),
        )
    }
}

private const val PREVIEW = 8
