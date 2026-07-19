package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val feedback = LocalRetraFeedback.current

    GlassPanel(cornerRadius = 26.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CommunitySectionHeader(
                icon = Icons.Default.Public,
                title = "Add a trusted catalog",
                subtitle = "Import legal homebrew and authorized releases from an HTTPS manifest."
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Manifest URL") },
                placeholder = { Text("https://example.com/catalog.json") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sha256,
                onValueChange = { sha256 = it.filter(Char::isLetterOrDigit).take(64) },
                label = { Text("Expected SHA-256") },
                supportingText = { Text("Required so Retra can verify the exact manifest before parsing it.") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    feedback(FeedbackCue.CONFIRM)
                    viewModel.importCatalogFromUrl(url, sha256)
                },
                enabled = url.startsWith("https://") && sha256.length == 64,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VerifiedUser, null)
                Spacer(Modifier.width(8.dp))
                Text("Verify and add")
            }
            Text(
                "Private-network targets and cross-host redirects are blocked. Downloads are size-limited and hash-checked.",
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
    val feedback = LocalRetraFeedback.current

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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Community", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Private by default. Share only what you choose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassPanel(cornerRadius = 26.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CommunitySectionHeader(Icons.Default.Person, "Your Retra profile", "Stored locally unless you explicitly connect a service.")
                OutlinedTextField(
                    displayName,
                    { displayName = it.take(40) },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    bio,
                    { bio = it.take(240) },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Friend code", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.friendCode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        viewModel.updateSocialProfile(displayName, bio)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save profile") }
            }
        }

        GlassPanel(cornerRadius = 26.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CommunitySectionHeader(Icons.Default.Link, "Social links", "Add public profile links without giving Retra account credentials.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialProvider.entries.take(4).forEach { provider ->
                        FilterChip(
                            selected = selectedProviderName == provider.name,
                            onClick = {
                                feedback(FeedbackCue.TAP)
                                selectedProviderName = provider.name
                            },
                            label = { Text(provider.name.lowercase().replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                OutlinedTextField(
                    providerName,
                    { providerName = it.take(80) },
                    label = { Text("Public profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    providerUrl,
                    { providerUrl = it.take(512) },
                    label = { Text("HTTPS profile URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        viewModel.configureSocialProvider(SocialProvider.valueOf(selectedProviderName), providerName, providerUrl)
                    },
                    enabled = providerName.isNotBlank() || providerUrl.startsWith("https://"),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save connection") }
                connections.forEach { connection ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(connection.provider.name.lowercase().replaceFirstChar(Char::titlecase))
                        Text(
                            if (connection.state == SocialConnectionState.CONNECTED) connection.displayName ?: "Connected" else "Not configured",
                            color = if (connection.state == SocialConnectionState.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        GlassPanel(cornerRadius = 26.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CommunitySectionHeader(Icons.Default.EmojiEvents, "Achievements", "Local progress that works offline.")
                val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }
                Text("$unlocked of ${achievements.size} unlocked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                achievements.forEachIndexed { index, status ->
                    val unlockedAt = status.progress.unlockedAtEpochMillis
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(status.definition.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    status.definition.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            GlassPill(selected = unlockedAt != null) {
                                Text(
                                    "${status.definition.points} pts",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            IconButton(
                                onClick = {
                                    feedback(FeedbackCue.TAP)
                                    viewModel.shareAchievement(status)
                                },
                                enabled = unlockedAt != null
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share ${status.definition.title}")
                            }
                        }
                        LinearProgressIndicator(
                            progress = { status.completionRatio },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (index != achievements.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        GlassPanel(cornerRadius = 26.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CommunitySectionHeader(Icons.Default.Groups, "Multiplayer", "Compatibility is checked before a room can exchange link data.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MultiplayerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedModeName == mode.name,
                            onClick = {
                                feedback(FeedbackCue.TAP)
                                selectedModeName = mode.name
                            },
                            label = { Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it.uppercase().filter(Char::isLetterOrDigit).take(6) },
                    label = { Text("Room code") },
                    placeholder = { Text("ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            feedback(FeedbackCue.INVITE)
                            viewModel.hostMultiplayer(MultiplayerMode.valueOf(selectedModeName))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Host") }
                    OutlinedButton(
                        onClick = {
                            feedback(FeedbackCue.CONFIRM)
                            viewModel.joinMultiplayer(MultiplayerMode.valueOf(selectedModeName), roomCode)
                        },
                        enabled = roomCode.length == 6,
                        modifier = Modifier.weight(1f)
                    ) { Text("Join") }
                }
                Text(
                    when {
                        multiplayer.phase == MultiplayerPhase.IDLE && activeGame == null -> "Launch a game before hosting or joining."
                        multiplayer.phase == MultiplayerPhase.IDLE && !viewModel.coreDescriptor.supportsLinkCable -> "Room negotiation and compatibility checks are ready; this core still needs deterministic link-cable callbacks."
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

@Composable
private fun CommunitySectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassPill(selected = true) {
            Icon(icon, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
