package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryCoral

@Composable
internal fun V3Stat(label: String, value: String, helper: String, icon: ImageVector, modifier: Modifier = Modifier) {
    RetraPanel(modifier, shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, fontWeight = FontWeight.Bold)
            Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun V3PosterCard(game: GameRecord, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
    ) {
        Column {
            Box {
                GameArtwork(game, Modifier.fillMaxWidth().aspectRatio(0.76f), ContentScale.Crop)
                if (game.favorite) {
                    Surface(Modifier.align(Alignment.TopEnd).padding(9.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)) {
                        Icon(Icons.Default.Favorite, null, Modifier.padding(7.dp).size(17.dp), tint = MemoryCoral)
                    }
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(gameOriginLabel(game), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun V3LibraryRow(game: GameRecord, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GameArtwork(game, Modifier.size(width = 72.dp, height = 94.dp).clip(MaterialTheme.shapes.small), ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(gameOriginLabel(game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${game.gameCode.ifBlank { "GBA" }} · ${formatBytes(game.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (game.favorite) Icon(Icons.Default.Favorite, null, tint = MemoryCoral)
        }
    }
}

internal fun gameOriginLabel(game: GameRecord): String = when {
    game.patchSha256 != null -> "Patched locally"
    game.origin == "BUNDLED_HOMEBREW" -> "Retra original homebrew"
    game.origin.contains("HOMEBREW") || game.origin.contains("CATALOG") -> game.creator?.let { "Homebrew · $it" } ?: "Licensed homebrew"
    else -> "Owned local backup"
}

@Composable
internal fun V3Achievement(status: AchievementStatus) {
    val unlocked = status.progress.unlockedAtEpochMillis != null
    RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(shape = CircleShape, color = (if (unlocked) AdventureGold else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.18f)) {
                Icon(if (unlocked) Icons.Default.Star else Icons.Default.Lock, null, Modifier.padding(11.dp), tint = if (unlocked) AdventureGold else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(status.definition.title, fontWeight = FontWeight.Bold)
                Text(status.definition.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!unlocked) LinearProgressIndicator(progress = { status.completionRatio }, modifier = Modifier.fillMaxWidth())
            }
            Text("${status.definition.points}", style = MaterialTheme.typography.labelLarge, color = if (unlocked) AdventureGold else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
internal fun V3DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun V3Badge(label: String, color: Color) {
    RetraBadge(label, color)
}

internal fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024L -> "%.1f MiB".format(value / (1024f * 1024f))
    value >= 1024L -> "%.1f KiB".format(value / 1024f)
    else -> "$value B"
}

