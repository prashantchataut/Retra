package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.SaveMint

@Composable
internal fun V3Profile(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    accountName: String?,
    saveCount: Int,
    corruptedSaves: Int,
    onSettings: () -> Unit,
    onGame: (GameRecord) -> Unit
) {
    val recent = games.filter { it.lastPlayedAtEpochMillis != null }.sortedByDescending { it.lastPlayedAtEpochMillis }.take(4)
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            RetraPageTitle(
                title = accountName ?: "You",
                subtitle = if (accountName == null) "Local play. No account required." else "Local library with optional identity.",
                actionIcon = Icons.Default.Settings,
                actionLabel = "Settings",
                onAction = onSettings
            )
        }
        item {
            RetraPanel(shape = MaterialTheme.shapes.extraLarge, contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        RetraLogo(Modifier.padding(14.dp), 62.dp, markColor = MaterialTheme.colorScheme.onPrimaryContainer, cutoutColor = MaterialTheme.colorScheme.primaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("On this device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Achievements and saves stay private unless you choose otherwise.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                V3Stat("Games", games.size.toString(), "verified items", Icons.Default.LibraryBooks, Modifier.weight(1f))
                V3Stat("Unlocked", "$unlocked/${achievements.size}", "milestones", Icons.Default.Star, Modifier.weight(1f))
            }
        }
        item {
            RetraPanel(shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = (if (corruptedSaves == 0) SaveMint else MemoryCoral).copy(alpha = 0.15f)) {
                        Icon(Icons.Default.Shield, null, Modifier.padding(13.dp), tint = if (corruptedSaves == 0) SaveMint else MemoryCoral)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Save health", fontWeight = FontWeight.Bold)
                        Text(
                            if (corruptedSaves == 0) "$saveCount readable record${if (saveCount == 1) "" else "s"}; no corruption detected." else "$corruptedSaves record${if (corruptedSaves == 1) "" else "s"} need attention.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { RetraSectionHeader("Milestones") }
        items(achievements.take(6), key = { it.definition.id }) { status -> V3Achievement(status) }
        if (recent.isNotEmpty()) {
            item { RetraSectionHeader("Recently played") }
            items(recent, key = { it.id }) { game -> V3LibraryRow(game) { onGame(game) } }
        }
    }
}
