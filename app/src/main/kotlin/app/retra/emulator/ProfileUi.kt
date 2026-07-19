package app.retra.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        account?.initials ?: profile.displayName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(account?.displayName ?: profile.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(account?.email ?: "Offline Retra profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.friendCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                }
                RetraLogoTile(size = 52.dp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileStat("Achievements", "$unlocked/${achievements.size}", Modifier.weight(1f))
                ProfileStat("Identity", if (account == null) "Offline" else "Google", Modifier.weight(1f))
            }

            if (account == null) {
                FilledTonalButton(
                    onClick = { viewModel.signInWithGoogle(context) },
                    enabled = viewModel.googleAuthConfigured && operation == AuthOperation.IDLE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Login, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (operation == AuthOperation.SIGNING_IN) "Connecting…" else "Connect Google account")
                }
                if (!viewModel.googleAuthConfigured) {
                    Text(
                        "This build needs RETRA_GOOGLE_WEB_CLIENT_ID before Google sign-in can open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) Icons.Default.VerifiedUser else Icons.Default.CloudOff,
                            null
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) "Cloud identity verified" else "Device identity connected",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (account!!.trustLevel == AccountTrustLevel.SERVER_VERIFIED) "This account may use verified cloud services." else "A production backend must still validate the Google ID token and nonce.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.signOutGoogle(context) },
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
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
