package app.retra.emulator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.input.InputManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import app.retra.core.emulation.EmulatorButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RetraViewModel by viewModels()
    private var noisyReceiverRegistered = false
    private var inputListenerRegistered = false
    private val inputManager by lazy { getSystemService(InputManager::class.java) }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                viewModel.onAudioBecomingNoisy()
            }
        }
    }
    private val inputListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = Unit
        override fun onInputDeviceChanged(deviceId: Int) = Unit
        override fun onInputDeviceRemoved(deviceId: Int) = viewModel.onControllerDisconnected()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RetraRoot(viewModel) }
        if (savedInstanceState == null) importFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importFromIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun importFromIntent(intent: Intent?) {
        if (intent == null) return
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }
            else -> null
        } ?: return
        if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.importFile(uri)
    }

    override fun onStart() {
        super.onStart()
        if (!noisyReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        }
        if (!inputListenerRegistered) {
            inputManager.registerInputDeviceListener(inputListener, null)
            inputListenerRegistered = true
        }
        viewModel.onHostForegrounded()
    }

    override fun onStop() {
        if (inputListenerRegistered) {
            inputManager.unregisterInputDeviceListener(inputListener)
            inputListenerRegistered = false
        }
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
        viewModel.onHostBackgrounded()
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val captureInput = viewModel.activeGame.value != null || viewModel.controllerTestEnabled.value
        if (!captureInput) return super.dispatchKeyEvent(event)
        val button = event.keyCode.toEmulatorButton(viewModel.activeGame.value != null)
            ?: return super.dispatchKeyEvent(event)
        if (event.repeatCount == 0) {
            viewModel.setButtonPressed(button, event.action == KeyEvent.ACTION_DOWN)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (viewModel.activeGame.value == null && !viewModel.controllerTestEnabled.value) return super.onGenericMotionEvent(event)
        val isJoystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (!isJoystick || event.action != MotionEvent.ACTION_MOVE) return super.onGenericMotionEvent(event)
        val horizontal = event.getAxisValue(MotionEvent.AXIS_HAT_X).takeUnless { it == 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_X)
        val vertical = event.getAxisValue(MotionEvent.AXIS_HAT_Y).takeUnless { it == 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_Y)
        viewModel.setDirectionalAxes(horizontal, vertical)
        return true
    }
}

private fun Int.toEmulatorButton(gameplayActive: Boolean): EmulatorButton? = when (this) {
    KeyEvent.KEYCODE_BUTTON_A -> EmulatorButton.A
    KeyEvent.KEYCODE_BUTTON_B -> EmulatorButton.B
    KeyEvent.KEYCODE_BUTTON_X -> EmulatorButton.B
    KeyEvent.KEYCODE_BUTTON_Y -> EmulatorButton.A
    KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> EmulatorButton.L
    KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> EmulatorButton.R
    KeyEvent.KEYCODE_BUTTON_START -> EmulatorButton.START
    KeyEvent.KEYCODE_BUTTON_SELECT -> EmulatorButton.SELECT
    KeyEvent.KEYCODE_BUTTON_THUMBR -> EmulatorButton.FAST_FORWARD
    KeyEvent.KEYCODE_BUTTON_THUMBL -> EmulatorButton.REWIND
    KeyEvent.KEYCODE_DPAD_UP -> EmulatorButton.UP
    KeyEvent.KEYCODE_DPAD_DOWN -> EmulatorButton.DOWN
    KeyEvent.KEYCODE_DPAD_LEFT -> EmulatorButton.LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> EmulatorButton.RIGHT
    KeyEvent.KEYCODE_Z -> if (gameplayActive) EmulatorButton.A else null
    KeyEvent.KEYCODE_X -> if (gameplayActive) EmulatorButton.B else null
    KeyEvent.KEYCODE_A -> if (gameplayActive) EmulatorButton.L else null
    KeyEvent.KEYCODE_S -> if (gameplayActive) EmulatorButton.R else null
    KeyEvent.KEYCODE_ENTER -> if (gameplayActive) EmulatorButton.START else null
    KeyEvent.KEYCODE_SPACE -> if (gameplayActive) EmulatorButton.SELECT else null
    else -> null
}
