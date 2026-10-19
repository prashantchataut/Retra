package app.retra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space

/** Glass pill text field. */
@Composable
fun RetraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    monospace: Boolean = false,
) {
    val c = Retra.colors
    val shape = if (singleLine) Radius.pillShape else Radius.mediumShape
    Row(
        modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(c.glassFill)
            .glassHighlights(shape, c)
            .padding(horizontal = Space.m, vertical = if (singleLine) 0.dp else Space.s),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.s))
        }
        Box(Modifier.weight(1f).padding(vertical = if (singleLine) 14.dp else 0.dp)) {
            if (value.isEmpty()) Text(placeholder, style = RetraType.body, color = c.inkMuted)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = (if (monospace) RetraType.body.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) else RetraType.body).copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
                minLines = if (singleLine) 1 else 3,
            )
        }
        if (value.isNotEmpty() && singleLine) {
            PlainIconButton(Icons.Rounded.Close, "Clear", { onValueChange("") }, tint = c.inkMuted)
        }
    }
}

/** A row with title/subtitle and a switch; the whole row toggles. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null) {
    val c = Retra.colors
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(Radius.smallShape)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(title, subtitle, icon, Modifier.weight(1f))
        Spacer(Modifier.width(Space.s))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.inkMuted, uncheckedTrackColor = c.inkFaint, uncheckedBorderColor = c.inkMuted.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
fun NavRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, trailing: String? = null, danger: Boolean = false) {
    val c = Retra.colors
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(Radius.smallShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(title, subtitle, icon, Modifier.weight(1f), danger)
        if (trailing != null) Text(trailing, style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(start = Space.s))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RowLabel(title: String, subtitle: String?, icon: ImageVector?, modifier: Modifier, danger: Boolean = false) {
    val c = Retra.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(Modifier.size(36.dp).clip(Radius.pillShape).background(if (danger) c.danger.copy(alpha = 0.12f) else c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (danger) c.danger else c.onAccentSoft, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Space.s))
        }
        Column {
            Text(title, style = RetraType.bodyStrong, color = if (danger) c.danger else c.ink)
            if (subtitle != null) Text(subtitle, style = RetraType.label, color = c.inkMuted)
        }
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    onChange: (Float) -> Unit,
    valueLabel: String,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    icon: ImageVector? = null,
) {
    val c = Retra.colors
    Column(modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowLabel(title, null, icon, Modifier.weight(1f))
            Text(valueLabel, style = RetraType.label, color = c.inkMuted)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.inkFaint, activeTickColor = c.onAccent, inactiveTickColor = c.inkMuted),
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

/** Segmented choice (2–4 options) as a glass pill with an accent thumb. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Retra.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Radius.pillShape)
            .background(c.inkFaint)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val sel = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(Radius.pillShape)
                    .background(if (sel) c.accent else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(sel, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = RetraType.label, color = if (sel) c.onAccent else c.ink)
            }
        }
    }
}

/** Glass group card for settings-like lists. */
@Composable
fun GlassGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), shape = Radius.mediumShape) {
        Column(Modifier.padding(horizontal = Space.m, vertical = Space.xs)) { content() }
    }
}
