package app.retra.emulator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RetraApplication : Application() {
    @Inject lateinit var notifications: RetraNotificationCoordinator

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
    }
}
