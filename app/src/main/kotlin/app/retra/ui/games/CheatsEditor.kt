package app.retra.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import app.retra.core.CheatParser
import app.retra.data.Cheat
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.PlainIconButton
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraTextField
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import java.util.UUID

/** Cheat list with toggles + an inline "add code" form. Shared by game details and the player menu. */
@Composable
fun CheatsEditor(
    cheats: List<Cheat>,
    onChange: (List<Cheat>) -> Unit,
    modifier: Modifier = Modifier,
    startAdding: Boolean = false,
) {
    val c = Retra.colors
    var adding by remember { mutableStateOf(startAdding) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth()) {
        if (cheats.isEmpty() && !adding) {
            Text("No cheats yet. Codes are saved per game and turn on the next time you play.", style = RetraType.body, color = c.inkMuted)
            Spacer(Modifier.height(Space.s))
        }
        cheats.forEachIndexed { i, cheat ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().padding(vertical = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cheat.name, style = RetraType.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cheat.code.lines().first() + if (cheat.code.lines().size > 1) " +${cheat.code.lines().size - 1}" else "", style = RetraType.caption.copy(fontFamily = FontFamily.Monospace), color = c.inkMuted, maxLines = 1)
                }
                PlainIconButton(Icons.Rounded.DeleteOutline, "Delete ${cheat.name}", { onChange(cheats.filterNot { it.id == cheat.id }) }, tint = c.inkMuted)
                Switch(
                    checked = cheat.enabled,
                    onCheckedChange = { on -> onChange(cheats.map { if (it.id == cheat.id) it.copy(enabled = on) else it }) },
                    colors = SwitchDefaults.colors(checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, uncheckedThumbColor = c.inkMuted, uncheckedTrackColor = c.inkFaint),
                )
            }
        }
        if (adding) {
            Spacer(Modifier.height(Space.s))
            RetraTextField(name, { name = it.take(40) }, "Name (e.g. Infinite money)", Modifier.fillMaxWidth(), imeAction = ImeAction.Next)
            Spacer(Modifier.height(Space.xs))
            RetraTextField(code, { code = it; error = null }, "Code, one per line", Modifier.fillMaxWidth(), singleLine = false, monospace = true)
            if (error != null) {
                Spacer(Modifier.height(Space.xxs))
                Text(error!!, style = RetraType.label, color = c.danger)
            }
            Text("Raw AAAAAAAA:VV, CodeBreaker, GameShark and Action Replay codes work.", style = RetraType.caption, color = c.inkMuted, modifier = Modifier.padding(top = Space.xxs))
            Spacer(Modifier.height(Space.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                RetraButton("Cancel", { adding = false; error = null }, Modifier.weight(1f), style = ButtonStyle.SECONDARY, compact = true)
                RetraButton("Add cheat", {
                    when (val r = CheatParser.parse(code)) {
                        is CheatParser.Result.Error -> error = r.message
                        is CheatParser.Result.Ok -> {
                            onChange(cheats + Cheat(UUID.randomUUID().toString(), name.ifBlank { "Cheat ${cheats.size + 1}" }, r.normalized, true))
                            name = ""; code = ""; adding = false
                        }
                    }
                }, Modifier.weight(1f), compact = true)
            }
        } else {
            RetraButton("Add a cheat", { adding = true }, icon = Icons.Rounded.Add, style = ButtonStyle.TERTIARY, compact = true)
        }
    }
}
