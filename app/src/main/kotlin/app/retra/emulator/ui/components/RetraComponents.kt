package app.retra.emulator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings
import app.retra.emulator.GlassPanel
import app.retra.emulator.RetraBrandMark

/**
 * Archive Glass panel bound to [MaterialTheme.shapes]. Prefer this over raw corner radii.
 */
@Composable
fun RetraPanel(
    modifier: Modifier = Modifier,
    settings: AppSettings? = null,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    GlassPanel(
        modifier = modifier,
        settings = settings,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Operate-first page chrome: title, optional one-line subtitle, optional action.
 * No essay body. No mandatory uppercase eyebrow.
 */
@Composable
fun RetraPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
        if (actionIcon != null && onAction != null) {
            FilledIconButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = actionLabel)
            }
        }
    }
}

@Composable
fun RetraBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.32f))
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun RetraEmptyState(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        RetraPanel(
            modifier = Modifier.widthIn(max = 500.dp),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RetraBrandMark(size = 72.dp)
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    body,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onPrimary) { Text(primaryLabel) }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
                }
            }
        }
    }
}

@Composable
fun RetraSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
