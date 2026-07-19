package app.retra.emulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.retra.core.model.AppSettings

@Composable
fun NotificationPreferences(
    settings: AppSettings,
    viewModel: RetraViewModel
) {
    val context = LocalContext.current
    fun permissionGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(permissionGranted()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        viewModel.emitFeedback(if (allowed) FeedbackCue.CONFIRM else FeedbackCue.ERROR)
    }
    val systemAllowed = granted && viewModel.notificationsAllowed()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(if (systemAllowed) Icons.Default.Notifications else Icons.Default.NotificationsOff, null)
            Column(Modifier.weight(1f)) {
                Text(if (systemAllowed) "Notifications are available" else "Notifications need permission")
                Text(
                    "Retra uses quiet channels for downloads and saves, with separate user-controlled channels for achievements and multiplayer.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Allow notifications")
            }
        }
        ToggleSetting("Enable Retra notifications", settings.notificationsEnabled, viewModel::setNotificationsEnabled)
        ToggleSetting(
            "Achievement notifications",
            settings.notifyAchievements,
            viewModel::setNotifyAchievements,
            enabled = settings.notificationsEnabled
        )
        ToggleSetting(
            "Download and import notifications",
            settings.notifyDownloads,
            viewModel::setNotifyDownloads,
            enabled = settings.notificationsEnabled
        )
        ToggleSetting(
            "Multiplayer room notifications",
            settings.notifyMultiplayer,
            viewModel::setNotifyMultiplayer,
            enabled = settings.notificationsEnabled
        )
        OutlinedButton(onClick = { context.startActivity(viewModel.notificationSettingsIntent()) }) {
            Text("Open Android notification settings")
        }
    }
}
