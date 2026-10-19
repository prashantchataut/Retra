package app.retra

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import app.retra.data.IncomingFile
import app.retra.emu.InputMapper
import app.retra.emu.InputRouter
import app.retra.ui.nav.RetraRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent { RetraRoot(container) }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Files opened with or shared to Retra: games go to the library, patches to the Patcher. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri: Uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        } ?: return
        lifecycleScope.launch {
            val outcome = container.importer.importUris(listOf(uri))
            val patch = outcome.patch
            when {
                patch != null -> container.incoming.offer(patch)
                outcome.added.isNotEmpty() -> container.incoming.offer(IncomingFile.Rom(outcome.added[0].id))
                outcome.duplicates.isNotEmpty() -> container.incoming.offer(IncomingFile.Rom(outcome.duplicates[0].id))
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = InputRouter.handler
        if (handler != null && InputMapper.isFromController(event) && event.keyCode != KeyEvent.KEYCODE_BACK &&
            event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
        ) {
            if (handler.onKeyEvent(event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handler = InputRouter.handler
        if (handler != null && event.action == MotionEvent.ACTION_MOVE &&
            (event.isFromSource(android.view.InputDevice.SOURCE_JOYSTICK) || event.isFromSource(android.view.InputDevice.SOURCE_GAMEPAD))
        ) {
            if (handler.onMotionEvent(event)) return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
