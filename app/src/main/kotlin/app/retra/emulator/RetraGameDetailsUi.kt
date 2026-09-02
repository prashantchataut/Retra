package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.KnownPatchHints
import app.retra.emulator.data.PendingPatch
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RetraGameSheet(
    game: GameRecord,
    coreReady: Boolean,
    coreStatus: String,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onCheats: () -> Unit,
    onArtwork: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var technicalExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        RetraPanel(
            modifier = Modifier.fillMaxWidth(),
            tier = GlassTier.STRONG,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Artwork + Metadata
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        GameArtwork(
                            game = game,
                            modifier = Modifier
                                .width(108.dp)
                                .aspectRatio(0.75f)
                                .clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RetraBadge("GBA", MaterialTheme.colorScheme.primary)
                                if (game.origin == "BUNDLED_HOMEBREW") {
                                    RetraBadge("HOMEBREW", SaveMint)
                                } else if (game.patchSha256 != null) {
                                    RetraBadge("PATCHED", MemoryCoral)
                                }
                            }
                            Text(
                                game.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                game.displayName.ifBlank { game.gameCode.ifBlank { "Game Boy Advance" } },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                game.lastPlayedAtEpochMillis?.let {
                                    "Last played ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}"
                                } ?: "Not yet played",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Core Ready / Warning Banner if core is missing
                if (!coreReady) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = AdventureGold.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AdventureGold.copy(alpha = 0.4f))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, tint = AdventureGold)
                                Text(coreStatus, style = MaterialTheme.typography.bodySmall, color = AdventureGold)
                            }
                        }
                    }
                }

                // Primary Play Button (52dp) & Favorite Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onPlay()
                            },
                            enabled = coreReady,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (game.lastPlayedAtEpochMillis == null) "Play Game" else "Resume Game",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Surface(
                            onClick = onFavorite,
                            shape = MaterialTheme.shapes.medium,
                            color = if (game.favorite) MemoryCoral.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (game.favorite) MemoryCoral else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (game.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    "Favorite",
                                    tint = if (game.favorite) MemoryCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Secondary Actions: Artwork, Cheats, Delete
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onArtwork,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Image, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Artwork")
                        }

                        OutlinedButton(
                            onClick = onCheats,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cheats")
                        }

                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Expandable Technical Identity & Provenance
                item {
                    Surface(
                        onClick = { technicalExpanded = !technicalExpanded },
                        shape = MaterialTheme.shapes.medium,
                        color = SurfaceMidnight.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Memory, null, tint = ElectricLilac, modifier = Modifier.size(18.dp))
                                    Text("ROM Identity & Provenance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Icon(if (technicalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }

                            if (technicalExpanded) {
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RetraDetailLine("Game Code", game.gameCode.ifBlank { "N/A" })
                                    RetraDetailLine("ROM Size", formatBytes(game.sizeBytes))
                                    RetraDetailLine("SHA-256", game.sha256.take(16) + "...")
                                    game.patchSha256?.let {
                                        RetraDetailLine("Patch SHA", it.take(16) + "...")
                                    }
                                    RetraDetailLine("Origin", gameOriginLabel(game))
                                    game.creator?.let { RetraDetailLine("Creator", it) }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove from Library?") },
            text = {
                Text(
                    "Are you sure you want to remove \"${game.title}\"? The ROM and save records remain on disk if you wish to re-import.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDismiss()
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
internal fun RetraPatchDialog(
    patch: PendingPatch,
    compatibleGames: List<GameRecord>,
    onImportBase: () -> Unit,
    onApply: (GameRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val hint = KnownPatchHints.match(patch.descriptor)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null, tint = MemoryCoral) },
        title = { Text("Patch Studio", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(patch.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "This UPS file is a patch, not a standalone ROM. Retra verified the patch format and now needs the compatible locally supplied base ROM.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetraDetailLine("Format", patch.descriptor.format.name)
                        RetraDetailLine("Base size", patch.descriptor.sourceSizeBytes?.let(::formatBytes) ?: "Not declared")
                        RetraDetailLine("Base CRC32", patch.descriptor.sourceCrc32?.let { "%08X".format(it) } ?: "Not declared")
                        RetraDetailLine("Output size", patch.descriptor.targetSizeBytes?.let(::formatBytes) ?: "Not declared")
                        RetraDetailLine("Patch CRC", patch.descriptor.patchCrc32?.let { "%08X".format(it) } ?: "Not declared")
                    }
                }
                if (hint != null) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Required base ROM", fontWeight = FontWeight.Bold)
                            Text("Pokémon Emerald (USA/Europe), revision 0")
                            Text("16 MiB · CRC32 1F1C08FB", style = MaterialTheme.typography.bodySmall)
                            Text("Retra checks the complete file; renaming another ROM will not bypass compatibility.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.80f))
                        }
                    }
                }
                if (compatibleGames.isEmpty()) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No matching base ROM in your library", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Import a legally obtained backup of the required revision.", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onImportBase) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Import base game") }
                        }
                    }
                } else {
                    Text("Select compatible base game:", fontWeight = FontWeight.Bold)
                    for (game in compatibleGames) {
                        Surface(
                            onClick = { onApply(game) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Verified, null, tint = SaveMint)
                                Column(Modifier.weight(1f)) {
                                    Text(game.title, fontWeight = FontWeight.Bold)
                                    Text("CRC32 ${game.crc32?.let { "%08X".format(it) } ?: "verified on apply"}", style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.PlayArrow, "Apply patch")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
