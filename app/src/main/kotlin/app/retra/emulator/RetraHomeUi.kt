package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight
import java.text.DateFormat
import java.util.Date

@Composable
internal fun RetraHome(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    vaultCount: Int,
    showStatistics: Boolean,
    coreReady: Boolean,
    coreStatus: String,
    accountName: String?,
    onContinue: (GameRecord) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImport: () -> Unit,
    onLibrary: () -> Unit,
    onPatchStudio: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    val recent = games.sortedByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
    val continueGame = recent.firstOrNull()
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar: Identity Chip + Settings Action
        item {
            HomeTopBar(
                accountName = accountName,
                onProfile = onProfile,
                onSettings = onSettings
            )
        }

        // Primary Hero: Continue Game or Friendly Empty State
        item {
            if (continueGame != null) {
                RetraHeroGame(
                    game = continueGame,
                    coreReady = coreReady,
                    coreStatus = coreStatus,
                    onPlay = { onContinue(continueGame) },
                    onDetails = { onGame(continueGame) }
                )
            } else {
                RetraHeroEmpty(onImport)
            }
        }

        // Secondary Shelf: Recently Played
        if (recent.isNotEmpty()) {
            item {
                RetraSectionHeader(
                    title = "Recently Played",
                    action = "View All",
                    onAction = onLibrary
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(recent.take(8), key = { it.id }) { game ->
                        RetraPosterCard(
                            game = game,
                            modifier = Modifier.width(158.dp)
                        ) {
                            onGame(game)
                        }
                    }
                }
            }
        }

        // Fast Action Cards / Studio Banner
        item {
            QuickFeaturesRow(
                onPatchStudio = onPatchStudio,
                onLibrary = onLibrary
            )
        }

        // On-Device Integrity & Stats
        if (showStatistics && games.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RetraStat("Vault Records", vaultCount.toString(), "Local saves & states", Icons.Default.Save, Modifier.weight(1f))
                    RetraStat("Milestones", "$unlocked / ${achievements.size}", "On-device progress", Icons.Default.Star, Modifier.weight(1f))
                }
            }
        }

        // Subdued Security & Privacy Note
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, null, Modifier.size(16.dp), tint = SaveMint)
                Text(
                    "Games and saves stay private on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    accountName: String?,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mascot Profile Avatar Pill
        Surface(
            onClick = onProfile,
            shape = CircleShape,
            color = SurfaceMidnight.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetraMascot(size = 30.dp, state = MascotState.IDLE, interactive = false)
                Text(
                    accountName ?: "Retra Player",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Settings Button
        FilledIconButton(
            onClick = onSettings,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun RetraHeroGame(
    game: GameRecord,
    coreReady: Boolean,
    coreStatus: String,
    onPlay: () -> Unit,
    onDetails: () -> Unit
) {
    RetraPanel(
        tier = GlassTier.ELEVATED,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 600.dp
            if (stacked) {
                Column {
                    Box(Modifier.fillMaxWidth().height(220.dp)) {
                        GameArtwork(game, Modifier.fillMaxSize(), ContentScale.Crop)
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, NightPlum.copy(alpha = 0.90f))
                                )
                            )
                        )
                    }
                    RetraHeroCopy(game, coreReady, coreStatus, onPlay, onDetails)
                }
            } else {
                Row(Modifier.heightIn(min = 280.dp)) {
                    Box(Modifier.weight(0.44f).fillMaxHeight()) {
                        GameArtwork(game, Modifier.fillMaxSize(), ContentScale.Crop)
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, NightPlum.copy(alpha = 0.85f))
                                )
                            )
                        )
                    }
                    RetraHeroCopy(game, coreReady, coreStatus, onPlay, onDetails, Modifier.weight(0.56f))
                }
            }
        }
    }
}

@Composable
internal fun RetraHeroCopy(
    game: GameRecord,
    coreReady: Boolean,
    coreStatus: String,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetraBadge(
                if (game.lastPlayedAtEpochMillis == null) "READY" else "CONTINUE",
                MaterialTheme.colorScheme.primary
            )
            if (game.origin == "BUNDLED_HOMEBREW") {
                RetraBadge("RETRA DRIFT", SaveMint)
            } else if (game.patchSha256 != null) {
                RetraBadge("PATCHED", MemoryCoral)
            }
        }

        Text(
            game.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            game.lastPlayedAtEpochMillis?.let {
                "Last played ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}"
            } ?: "Ready to play.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!coreReady) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = AdventureGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AdventureGold.copy(alpha = 0.35f))
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = AdventureGold)
                    Text(coreStatus, style = MaterialTheme.typography.bodySmall, color = AdventureGold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlay,
                enabled = coreReady,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (game.lastPlayedAtEpochMillis == null) "Play" else "Resume",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onDetails,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Details")
            }
        }
    }
}

@Composable
internal fun RetraHeroEmpty(onImport: () -> Unit) {
    RetraPanel(
        tier = GlassTier.ELEVATED,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RetraMascot(size = 72.dp, state = MascotState.EMPTY)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Your library is waiting",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Import games or try the bundled demo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Import Game File (GBA / ZIP)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickFeaturesRow(
    onPatchStudio: () -> Unit,
    onLibrary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Patch Studio Feature Card
        Surface(
            onClick = onPatchStudio,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            color = SurfaceMidnight.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, ElectricLilac.copy(alpha = 0.40f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricLilac.copy(alpha = 0.16f)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        Modifier.padding(10.dp).size(20.dp),
                        tint = ElectricLilac
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Patch Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Heart & Soul / UPS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Library Browser Feature Card
        Surface(
            onClick = onLibrary,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            color = SurfaceMidnight.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, SaveMint.copy(alpha = 0.40f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SaveMint.copy(alpha = 0.16f)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        Modifier.padding(10.dp).size(20.dp),
                        tint = SaveMint
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Browse Files", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Tags & Filters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
