package app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.WaitroomScreen
import ui.rpg.RpgDemoScreen

private enum class Mode { RPG, WAITROOM }

@Composable
fun App(
    lifecycleActive: Boolean = true
) {
    MaterialTheme {
        var mode by remember { mutableStateOf(Mode.RPG) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton("RPG Demo", mode == Mode.RPG) { mode = Mode.RPG }
                ModeButton("Waitroom", mode == Mode.WAITROOM) { mode = Mode.WAITROOM }
            }
            when (mode) {
                Mode.RPG -> RpgDemoScreen()
                Mode.WAITROOM -> WaitroomScreen(lifecycleActive = lifecycleActive)
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (selected) ButtonDefaults.buttonColors()
        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label)
    }
}
