package app.retra.emulator

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RetraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetraV23Root(viewModel) }
        routeExternalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeExternalIntent(intent)
    }

    // ComponentActivity.dispatchKeyEvent is LIBRARY_GROUP_PREFIX-restricted in current
    // AndroidX, but overriding it remains the correct way to intercept gamepad keys
    // before Compose consumes them.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val device = event.device
        val gameInput = event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD or InputDevice.SOURCE_JOYSTICK) != 0
        if (device != null && gameInput && event.action in setOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val handled = viewModel.handleControllerKeyEvent(
                deviceDescriptor = device.descriptor.orEmpty(),
                deviceName = device.name.orEmpty(),
                keyCode = event.keyCode,
                pressed = event.action == KeyEvent.ACTION_DOWN,
                repeatCount = event.repeatCount
            )
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val joystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (joystick && event.action == MotionEvent.ACTION_MOVE) {
            val device = event.device
            if (device != null) {
                val horizontal = centeredAxis(event, device, MotionEvent.AXIS_X)
                    .takeUnless { kotlin.math.abs(it) < 0.001f }
                    ?: centeredAxis(event, device, MotionEvent.AXIS_HAT_X)
                val vertical = centeredAxis(event, device, MotionEvent.AXIS_Y)
                    .takeUnless { kotlin.math.abs(it) < 0.001f }
                    ?: centeredAxis(event, device, MotionEvent.AXIS_HAT_Y)
                val leftTrigger = positiveAxis(event, device, MotionEvent.AXIS_LTRIGGER)
                    .takeUnless { it < 0.001f }
                    ?: positiveAxis(event, device, MotionEvent.AXIS_BRAKE)
                val rightTrigger = positiveAxis(event, device, MotionEvent.AXIS_RTRIGGER)
                    .takeUnless { it < 0.001f }
                    ?: positiveAxis(event, device, MotionEvent.AXIS_GAS)
                if (viewModel.handleControllerMotionEvent(
                        deviceDescriptor = device.descriptor.orEmpty(),
                        deviceName = device.name.orEmpty(),
                        horizontal = horizontal,
                        vertical = vertical,
                        leftTrigger = leftTrigger,
                        rightTrigger = rightTrigger
                    )
                ) return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    private fun centeredAxis(event: MotionEvent, device: InputDevice, axis: Int): Float {
        val range = device.getMotionRange(axis, event.source) ?: return 0f
        val value = event.getAxisValue(axis)
        val flat = range.flat.coerceAtLeast(0.01f)
        return if (kotlin.math.abs(value) > flat) value.coerceIn(-1f, 1f) else 0f
    }

    private fun positiveAxis(event: MotionEvent, device: InputDevice, axis: Int): Float {
        val range = device.getMotionRange(axis, event.source) ?: return 0f
        val raw = event.getAxisValue(axis)
        val span = (range.max - range.min).takeIf { it > 0f } ?: return 0f
        val normalized = ((raw - range.min) / span).coerceIn(0f, 1f)
        return if (normalized > range.flat.coerceAtLeast(0.01f)) normalized else 0f
    }

    private fun routeExternalIntent(intent: Intent?) {
        val incoming = intent ?: return
        val uri = when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> incoming.sharedStreamUri()
            else -> null
        } ?: return
        viewModel.queueExternalImport(uri)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? {
        return clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
    }
}
