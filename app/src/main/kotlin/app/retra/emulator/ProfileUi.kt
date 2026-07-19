package app.retra.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.emulator.auth.AccountTrustLevel
import app.retra.emulator.auth.AuthOperation

@Composable
fun ProfileOverviewCard(viewModel: RetraViewModel) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val operation by viewModel.authOperation.collectAsStateWithLifecycle()
    val profile by viewModel.socialProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = LocalRetraFeedback.current
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    GlassPanel(cornerRadius = 28.dp, contentPadding = PaddingValues(20.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .size(62.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        account?.initials ?: profile.displayName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(account?.displayName ?: profile.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(account?.email ?: "Offline profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.friendCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                }
                RetraLogoTile(size = 46.dp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileStat("Achievements", "$unlocked/${achievements.size}", Modifier.weight(1f))
                ProfileStat("Identity", if (account == null) "Offline" else "Google", Modifier.weight(1f))
            }

            if (account == null) {
                FilledTonalButton(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        viewModel.signInWithGoogle(context)
                    },
                    enabled = viewModel.googleAuthConfigured && operation == AuthOperation.IDLE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Login, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (operation == AuthOperation.SIGNING_IN) "Connecting…" else "Continue with Google")
                }
                Text(
                    if (viewModel.googleAuthConfigured) {
                        "Google is optional. Local games, saves, cheats, and settings never require an account."
                    } else {
                        "Google sign-in is unavailable until RETRA_GOOGLE_WEB_CLIENT_ID is configured for this build."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (viewModel.googleAuthConfigured) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            } else {
                GlassPanel(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) Icons.Default.VerifiedUser else Icons.Default.CloudOff,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) "Cloud identity verified" else "Google identity connected",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) {
                                    "Verified cloud services may be enabled for this profile."
                                } else {
                                    "Cloud privileges remain disabled until a backend validates the ID token and nonce."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackCue.TAP)
                        viewModel.signOutGoogle(context)
                    },
                    enabled = operation == AuthOperation.IDLE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (operation == AuthOperation.SIGNING_OUT) "Disconnecting…" else "Disconnect Google")
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
