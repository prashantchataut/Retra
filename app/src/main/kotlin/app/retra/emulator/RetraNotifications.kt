package app.retra.emulator

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetraNotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationIds = AtomicInteger(4_000)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notificationAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val achievements = NotificationChannel(
            CHANNEL_ACHIEVEMENTS,
            "Achievements",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Achievement unlocks and milestone celebrations"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 24, 45, 34, 45, 48)
            setSound(resourceUri(R.raw.retra_achievement), notificationAudio)
        }
        val downloads = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "Library and downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Game imports, verified downloads, and library tasks"
            setSound(null, null)
            enableVibration(false)
        }
        val multiplayer = NotificationChannel(
            CHANNEL_MULTIPLAYER,
            "Multiplayer",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Room invitations and multiplayer session updates"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 30, 55, 30)
            setSound(resourceUri(R.raw.retra_invite), notificationAudio)
        }
        val saves = NotificationChannel(
            CHANNEL_SAVES,
            "Saves and protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background suspend states, backups, and save recovery"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannels(listOf(achievements, downloads, multiplayer, saves))
    }

    fun canNotify(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyAchievement(title: String, description: String, points: Int) {
        post(
            channel = CHANNEL_ACHIEVEMENTS,
            title = "Achievement unlocked",
            body = "$title · $points points\n$description",
            category = NotificationCompat.CATEGORY_SOCIAL
        )
    }

    fun notifyDownloadComplete(title: String) {
        post(
            channel = CHANNEL_DOWNLOADS,
            title = "Added to your library",
            body = "$title was verified and imported successfully.",
            category = NotificationCompat.CATEGORY_PROGRESS
        )
    }

    fun notifyMultiplayerInvite(roomCode: String) {
        post(
            channel = CHANNEL_MULTIPLAYER,
            title = "Retra multiplayer room ready",
            body = "Room code: $roomCode",
            category = NotificationCompat.CATEGORY_MESSAGE
        )
    }

    fun notifySuspendSaved(title: String) {
        post(
            channel = CHANNEL_SAVES,
            title = "Session protected",
            body = "$title was suspended safely in the background.",
            category = NotificationCompat.CATEGORY_STATUS
        )
    }

    fun settingsIntent(channelId: String? = null): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId != null) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun post(channel: String, title: String, body: String, category: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_retra_monochrome)
            .setContentTitle(title)
            .setContentText(body.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent())
            .setCategory(category)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.retra_accent))
            .build()
        NotificationManagerCompat.from(context).notify(notificationIds.incrementAndGet(), notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resourceUri(resource: Int): Uri = Uri.parse("android.resource://${context.packageName}/$resource")

    companion object {
        const val CHANNEL_ACHIEVEMENTS = "retra_achievements_v1"
        const val CHANNEL_DOWNLOADS = "retra_downloads_v1"
        const val CHANNEL_MULTIPLAYER = "retra_multiplayer_v1"
        const val CHANNEL_SAVES = "retra_saves_v1"
    }
}
