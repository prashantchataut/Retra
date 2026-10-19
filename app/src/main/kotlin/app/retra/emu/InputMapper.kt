package app.retra.emu

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import app.retra.engine.GbaKey

/** Maps gamepad buttons, keyboards and analog sticks to GBA keys. */
object InputMapper {
    /** Extra actions that aren't GBA buttons. */
    enum class Hotkey { FAST_FORWARD, REWIND, MENU }

    fun gbaKey(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_X -> GbaKey.A
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_Z -> GbaKey.B
        KeyEvent.KEYCODE_BUTTON_X -> GbaKey.B
        KeyEvent.KEYCODE_BUTTON_Y -> GbaKey.A
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_Q -> GbaKey.L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_W -> GbaKey.R
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> GbaKey.START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_SPACE -> GbaKey.SELECT
        KeyEvent.KEYCODE_DPAD_UP -> GbaKey.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GbaKey.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GbaKey.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GbaKey.RIGHT
        else -> 0
    }

    fun hotkey(keyCode: Int): Hotkey? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_TAB -> Hotkey.FAST_FORWARD
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_GRAVE -> Hotkey.REWIND
        KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_MENU -> Hotkey.MENU
        else -> null
    }

    /** D-pad bits from hat and left stick axes. */
    fun axesToKeys(event: MotionEvent): Int {
        var mask = 0
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        if (hatX < -0.5f || x < -0.5f) mask = mask or GbaKey.LEFT
        if (hatX > 0.5f || x > 0.5f) mask = mask or GbaKey.RIGHT
        if (hatY < -0.5f || y < -0.5f) mask = mask or GbaKey.UP
        if (hatY > 0.5f || y > 0.5f) mask = mask or GbaKey.DOWN
        return mask
    }

    fun triggers(event: MotionEvent): Pair<Boolean, Boolean> {
        val l = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val r = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        return (l > 0.5f) to (r > 0.5f)
    }

    fun isGamepad(device: InputDevice?): Boolean {
        device ?: return false
        if (device.isVirtual) return false
        val s = device.sources
        return s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }

    fun isFromController(event: KeyEvent): Boolean {
        val s = event.source
        return s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            s and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD ||
            s and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD && !event.isSystem
    }
}

/** Activity-level key routing: the player registers a handler while visible. */
object InputRouter {
    interface Handler {
        fun onKeyEvent(event: KeyEvent): Boolean
        fun onMotionEvent(event: MotionEvent): Boolean
    }

    @Volatile var handler: Handler? = null
}
