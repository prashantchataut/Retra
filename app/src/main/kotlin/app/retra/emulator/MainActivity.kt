package app.retra.emulator

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.retra.core.emulation.EmulatorButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RetraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RetraRoot(viewModel) }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onHostForegrounded()
    }

    override fun onStop() {
        viewModel.onHostBackgrounded()
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val button = event.keyCode.toEmulatorButton() ?: return super.dispatchKeyEvent(event)
        if (event.repeatCount == 0) {
            viewModel.setButtonPressed(button, event.action == KeyEvent.ACTION_DOWN)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
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

private fun Int.toEmulatorButton(): EmulatorButton? = when (this) {
    KeyEvent.KEYCODE_BUTTON_A -> EmulatorButton.A
    KeyEvent.KEYCODE_BUTTON_B -> EmulatorButton.B
    KeyEvent.KEYCODE_BUTTON_L1 -> EmulatorButton.L
    KeyEvent.KEYCODE_BUTTON_R1 -> EmulatorButton.R
    KeyEvent.KEYCODE_BUTTON_START -> EmulatorButton.START
    KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> EmulatorButton.SELECT
    KeyEvent.KEYCODE_DPAD_UP -> EmulatorButton.UP
    KeyEvent.KEYCODE_DPAD_DOWN -> EmulatorButton.DOWN
    KeyEvent.KEYCODE_DPAD_LEFT -> EmulatorButton.LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> EmulatorButton.RIGHT
    else -> null
}
