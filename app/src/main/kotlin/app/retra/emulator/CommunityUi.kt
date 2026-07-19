package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.multiplayer.MultiplayerMode
import app.retra.core.multiplayer.MultiplayerPhase
import app.retra.core.social.SocialConnectionState
import app.retra.core.social.SocialProvider

@Composable
internal fun OnlineCatalogImportCard(viewModel: RetraViewModel) {
    var url by rememberSaveable { mutableStateOf("") }
    var sha256 by rememberSaveable { mutableStateOf("") }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Public, null)
                Column {
                    Text("Add an internet game catalog", style = MaterialTheme.typography.titleLarge)
                    Text("Custom catalogs must contain only content the publisher is authorized to distribute.", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("HTTPS manifest URL (.json or .catalog)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sha256,
                onValueChange = { sha256 = it.filter(Char::isLetterOrDigit).take(64) },
                label = { Text("Expected manifest SHA-256") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.importCatalogFromUrl(url, sha256) },
                enabled = url.isNotBlank() && sha256.length == 64,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VerifiedUser, null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Download, verify, and add catalog")
            }
            Text(
                "Retra blocks private-network targets and cross-host redirects, then verifies the exact hash before parsing the manifest.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun CommunityHub(viewModel: RetraViewModel) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val profile by viewModel.socialProfile.collectAsStateWithLifecycle()
    val connections by viewModel.socialConnections.collectAsStateWithLifecycle()
    val multiplayer by viewModel.multiplayerSession.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()

    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var providerName by rememberSaveable { mutableStateOf("") }
    var providerUrl by rememberSaveable { mutableStateOf("") }
    var selectedProviderName by rememberSaveable { mutableStateOf(SocialProvider.DISCORD.name) }
    var selectedModeName by rememberSaveable { mutableStateOf(MultiplayerMode.LAN.name) }
    var roomCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(profile.profileId) {
        displayName = profile.displayName
        bio = profile.bio
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Community & achievements", style = MaterialTheme.typography.headlineMedium)

        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Your private-first profile", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(displayName, { displayName = it.take(40) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it.take(240) }, label = { Text("Bio") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Friend code", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.friendCode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { viewModel.updateSocialProfile(displayName, bio) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save local profile")
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Connected social identities", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Retra stores only the public profile label and HTTPS profile link you enter. Provider OAuth remains an adapter boundary until real app credentials and redirect URIs are supplied.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialProvider.entries.take(4).forEach { provider ->
                        FilterChip(
                            selected = selectedProviderName == provider.name,
                            onClick = { selectedProviderName = provider.name },
                            label = { Text(provider.name.lowercase().replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                OutlinedTextField(providerName, { providerName = it.take(80) }, label = { Text("Public profile name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(providerUrl, { providerUrl = it.take(512) }, label = { Text("HTTPS profile URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { viewModel.configureSocialProvider(SocialProvider.valueOf(selectedProviderName), providerName, providerUrl) },
                    enabled = providerName.isNotBlank() || providerUrl.startsWith("https://"),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save social connection") }
                connections.forEach { connection ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(connection.provider.name.lowercase().replaceFirstChar(Char::titlecase))
                        Text(
                            if (connection.state == SocialConnectionState.CONNECTED) connection.displayName ?: "Connected" else "Needs configuration",
                            color = if (connection.state == SocialConnectionState.CONNECTED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Retra achievements", style = MaterialTheme.typography.titleLarge)
                }
                val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }
                Text("$unlocked of ${achievements.size} unlocked")
                achievements.forEach { status ->
                    val unlockedAt = status.progress.unlockedAtEpochMillis
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(status.definition.title, style = MaterialTheme.typography.titleMedium)
                                Text(status.definition.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${status.definition.points} pts", style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { viewModel.shareAchievement(status) }, enabled = unlockedAt != null) {
                                Icon(Icons.Default.Share, contentDescription = "Share ${status.definition.title}")
                            }
                        }
                        LinearProgressIndicator(progress = { status.completionRatio }, modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider()
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Multiplayer link architecture", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Every room is gated by exact ROM hash, patch identity, core build, cheat state, protocol version, and player capability before link data can flow.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MultiplayerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedModeName == mode.name,
                            onClick = { selectedModeName = mode.name },
                            label = { Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it.uppercase().filter(Char::isLetterOrDigit).take(6) },
                    label = { Text("Six-character room code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.hostMultiplayer(MultiplayerMode.valueOf(selectedModeName)) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Host") }
                    OutlinedButton(
                        onClick = { viewModel.joinMultiplayer(MultiplayerMode.valueOf(selectedModeName), roomCode) },
                        enabled = roomCode.length == 6,
                        modifier = Modifier.weight(1f)
                    ) { Text("Join") }
                }
                Text(
                    when {
                        multiplayer.phase == MultiplayerPhase.IDLE && activeGame == null -> "Launch a game before hosting or joining."
                        multiplayer.phase == MultiplayerPhase.IDLE && !viewModel.coreDescriptor.supportsLinkCable -> "Protocol, packet codec, room negotiation, and compatibility gates are implemented; the selected core still lacks link-cable callbacks."
                        else -> "${multiplayer.phase.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)}${multiplayer.roomCode?.let { " · $it" }.orEmpty()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
