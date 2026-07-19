package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.emulation.EmulatorButton

@Composable
fun ControllerInputTester(viewModel: RetraViewModel) {
    DisposableEffect(viewModel) {
        viewModel.setControllerTestEnabled(true)
        onDispose { viewModel.setControllerTestEnabled(false) }
    }
    val pressed by viewModel.controllerInput.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Controller input tester", style = MaterialTheme.typography.titleMedium)
        Text(
            "Press a Bluetooth, USB, keyboard, or D-pad control. Retra uses the same normalized input state during gameplay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(
            listOf(EmulatorButton.UP, EmulatorButton.DOWN, EmulatorButton.LEFT, EmulatorButton.RIGHT),
            listOf(EmulatorButton.A, EmulatorButton.B, EmulatorButton.L, EmulatorButton.R),
            listOf(EmulatorButton.START, EmulatorButton.SELECT)
        ).forEach { rowButtons ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowButtons.forEach { button ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (button in pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            button.name,
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = if (button in pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
