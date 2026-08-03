package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.SaveMint
import java.text.DateFormat
import java.util.Date

@Composable
internal fun V3Home(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    vaultCount: Int,
    showStatistics: Boolean,
    coreReady: Boolean,
    coreStatus: String,
    onContinue: (GameRecord) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImport: () -> Unit,
    onLibrary: () -> Unit,
    onPatchStudio: () -> Unit,
    onSettings: () -> Unit
) {
    val recent = games.sortedByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
    val continueGame = recent.firstOrNull()
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            RetraPageTitle(
                title = "Home",
                subtitle = if (continueGame != null) "Continue where you left off." else "Import a game you own to start.",
                actionIcon = Icons.Default.Settings,
                actionLabel = "Settings",
                onAction = onSettings
            )
        }
        item {
            if (continueGame != null) {
                V3HeroGame(
                    game = continueGame,
                    coreReady = coreReady,
                    coreStatus = coreStatus,
                    onPlay = { onContinue(continueGame) },
                    onDetails = { onGame(continueGame) }
                )
            } else {
                V3HeroEmpty(onImport)
            }
        }
        if (recent.isNotEmpty()) {
            item { RetraSectionHeader("Recent", "Library", onLibrary) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recent.take(8), key = { it.id }) { game ->
                        V3PosterCard(game, Modifier.width(156.dp)) { onGame(game) }
                    }
                }
            }
        }
        if (showStatistics && games.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    V3Stat("Saves", vaultCount.toString(), "local records", Icons.Default.Save, Modifier.weight(1f))
                    V3Stat("Milestones", "$unlocked/${achievements.size}", "on device", Icons.Default.Star, Modifier.weight(1f))
                }
            }
        }
        item {
            Text(
                "Games and saves stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Keep patch entry discoverable without equal-weight action cards.
        item {
            TextButton(onClick = onPatchStudio) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Patch Studio")
            }
        }
    }
}

@Composable
internal fun V3HeroGame(
    game: GameRecord,
    coreReady: Boolean,
    coreStatus: String,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    GlassPanel(shape = MaterialTheme.shapes.extraLarge) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 580.dp
            if (stacked) {
                Column {
                    GameArtwork(game, Modifier.fillMaxWidth().height(220.dp), ContentScale.Crop)
                    V3HeroCopy(game, coreReady, coreStatus, onPlay, onDetails)
                }
            } else {
                Row(Modifier.heightIn(min = 280.dp)) {
                    GameArtwork(game, Modifier.weight(0.43f).fillMaxHeight(), ContentScale.Crop)
                    V3HeroCopy(game, coreReady, coreStatus, onPlay, onDetails, Modifier.weight(0.57f))
                }
            }
        }
    }
}

@Composable
internal fun V3HeroCopy(
    game: GameRecord,
    coreReady: Boolean,
    coreStatus: String,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RetraBadge(if (game.lastPlayedAtEpochMillis == null) "READY" else "CONTINUE", MaterialTheme.colorScheme.primary)
            if (game.origin == "BUNDLED_HOMEBREW") RetraBadge("HOMEBREW", SaveMint)
        }
        Text(game.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            game.lastPlayedAtEpochMillis?.let { "Last played ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}" }
                ?: "Ready to play.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!coreReady) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = AdventureGold.copy(alpha = 0.13f)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = AdventureGold)
                    Text(coreStatus, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onPlay, enabled = coreReady, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(if (game.lastPlayedAtEpochMillis == null) "Play" else "Continue")
            }
            OutlinedButton(onClick = onDetails) { Text("Details") }
        }
    }
}

@Composable
internal fun V3HeroEmpty(onImport: () -> Unit) {
    RetraPanel(shape = MaterialTheme.shapes.extraLarge, contentPadding = PaddingValues(26.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.Start) {
            RetraBrandMark(size = 72.dp)
            Text("Add a game you own", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Import a GBA backup, ZIP, or patch file. Retra Drift is available from Discover if you want a built-in demo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onImport) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Import")
            }
        }
    }
}
