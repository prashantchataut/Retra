package app.retra.emulator

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.core.model.AccentPalette
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight

@Composable
internal fun RetraProfile(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    accountName: String?,
    saveCount: Int,
    corruptedSaves: Int,
    authOperation: AuthOperation = AuthOperation.IDLE,
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onSettings: () -> Unit,
    onGame: (GameRecord) -> Unit
) {
    val recent = games.filter { it.lastPlayedAtEpochMillis != null }
        .sortedByDescending { it.lastPlayedAtEpochMillis }
        .take(4)
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Title & Settings Shortcut
        item {
            RetraPageTitle(
                title = accountName ?: "Local Profile",
                subtitle = if (accountName == null) "Private on device. No account required." else "Connected Google identity with local library.",
                actionIcon = Icons.Default.Settings,
                actionLabel = "Settings",
                onAction = onSettings
            )
        }

        // Hero Profile Card
        item {
            RetraPanel(
                tier = GlassTier.ELEVATED,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mascot Avatar
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = SurfaceMidnight,
                        border = BorderStroke(1.dp, ElectricLilac.copy(alpha = 0.5f))
                    ) {
                        RetraMascot(
                            modifier = Modifier.padding(10.dp),
                            size = 64.dp,
                            state = if (accountName != null) MascotState.SUCCESS else MascotState.IDLE,
                            interactive = true
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                accountName ?: "Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            RetraBadge(
                                if (accountName != null) "CONNECTED" else "OFFLINE",
                                if (accountName != null) SaveMint else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Saves, milestones, and settings remain local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Google Identity Connection
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = SurfaceMidnight.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            if (accountName != null) "Google Account" else "Optional Account Link",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (accountName != null) "Signed in as $accountName" else "Link Google identity for profile personalization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (accountName != null) {
                        OutlinedButton(
                            onClick = onGoogleSignOut,
                            enabled = authOperation == AuthOperation.IDLE
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = onGoogleSignIn,
                            enabled = authOperation == AuthOperation.IDLE
                        ) {
                            if (authOperation == AuthOperation.SIGNING_IN) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RetraStat("Total Games", games.size.toString(), "In your archive", Icons.Default.LibraryBooks, Modifier.weight(1f))
                RetraStat("Milestones", "$unlocked / ${achievements.size}", "Unlocked records", Icons.Default.Star, Modifier.weight(1f))
            }
        }

        // Save Health Card
        item {
            RetraPanel(
                tier = GlassTier.REGULAR,
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (corruptedSaves == 0) SaveMint else MemoryCoral).copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            null,
                            Modifier.padding(12.dp).size(22.dp),
                            tint = if (corruptedSaves == 0) SaveMint else MemoryCoral
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Save health", fontWeight = FontWeight.Bold)
                        Text(
                            if (corruptedSaves == 0) "$saveCount healthy local record${if (saveCount == 1) "" else "s"} verified."
                            else "$corruptedSaves record${if (corruptedSaves == 1) "" else "s"} need verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Milestones
        if (achievements.isNotEmpty()) {
            item { RetraSectionHeader("Milestones & Achievements") }
            items(achievements.take(5), key = { it.definition.id }) { status ->
                RetraAchievementCard(status)
            }
        }

        // Recently Played
        if (recent.isNotEmpty()) {
            item { RetraSectionHeader("Recently Played") }
            items(recent, key = { it.id }) { game ->
                RetraLibraryRow(game) { onGame(game) }
            }
        }
    }
}
