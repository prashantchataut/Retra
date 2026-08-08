package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V3GameSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        RetraPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GameArtwork(game, Modifier.width(116.dp).aspectRatio(0.76f).clip(MaterialTheme.shapes.large), ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(game.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(gameOriginLabel(game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RetraBadge(game.gameCode.ifBlank { "GBA" }, RetraBlue)
                            if (game.patchSha256 != null) RetraBadge("PATCHED", MemoryCoral)
                        }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.medium, color = if (coreReady) SaveMint.copy(alpha = 0.12f) else AdventureGold.copy(alpha = 0.13f)) {
                    Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (coreReady) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = if (coreReady) SaveMint else AdventureGold)
                        Text(if (coreReady) "Ready with ${coreStatus}" else coreStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Button(onClick = onPlay, enabled = coreReady, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onFavorite, modifier = Modifier.weight(1f)) {
                        Icon(if (game.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Spacer(Modifier.width(5.dp)); Text(if (game.favorite) "Favorited" else "Favorite")
                    }
                    FilledTonalButton(onClick = onArtwork, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Palette, null); Spacer(Modifier.width(5.dp)); Text("Artwork")
                    }
                }
            }
            item {
                RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        V3DetailLine("File", game.displayName)
                        V3DetailLine("Size", formatBytes(game.sizeBytes))
                        V3DetailLine("Game code", game.gameCode.ifBlank { "Unknown" })
                        V3DetailLine("CRC32", game.crc32?.let { "%08X".format(it) } ?: "Not indexed")
                        V3DetailLine("SHA-256", game.sha256.take(16) + "…")
                        game.creator?.let { V3DetailLine("Creator", it) }
                        game.license?.let { V3DetailLine("License", it) }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onCheats, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Memory, null); Spacer(Modifier.width(6.dp)); Text("Install compatible cheat catalog")
                }
            }
            item {
                if (confirmDelete) {
                    RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Remove this library item?", fontWeight = FontWeight.Bold)
                            Text("The managed ROM copy is removed. Save records remain separate.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onDelete(); onDismiss() }) { Text("Remove") }
                                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Remove from library")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
internal fun V3PatchDialog(
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
        title = { Text("Patch Studio", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(patch.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("This UPS file is not a game. Retra verified the patch container and now needs the exact base ROM it was authored for.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        V3DetailLine("Format", patch.descriptor.format.name)
                        V3DetailLine("Base size", patch.descriptor.sourceSizeBytes?.let(::formatBytes) ?: "Not declared")
                        V3DetailLine("Base CRC32", patch.descriptor.sourceCrc32?.let { "%08X".format(it) } ?: "Not declared")
                        V3DetailLine("Output size", patch.descriptor.targetSizeBytes?.let(::formatBytes) ?: "Not declared")
                        V3DetailLine("Patch CRC", patch.descriptor.patchCrc32?.let { "%08X".format(it) } ?: "Not declared")
                    }
                }
                if (hint != null) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Required base", fontWeight = FontWeight.Bold)
                            Text("Pokémon Emerald (USA/Europe), revision 0")
                            Text("16 MiB · CRC32 1F1C08FB", style = MaterialTheme.typography.bodySmall)
                            Text("Retra checks the complete file; renaming another ROM will not bypass compatibility.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f))
                        }
                    }
                }
                if (compatibleGames.isEmpty()) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No matching base is in your library", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Import a legally obtained backup of the exact required revision. The patch will remain queued while you choose it.", color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(onClick = onImportBase) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Import base game") }
                        }
                    }
                } else {
                    Text("Compatible base", fontWeight = FontWeight.Bold)
                    compatibleGames.forEach { game ->
                        Surface(
                            onClick = { onApply(game) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
