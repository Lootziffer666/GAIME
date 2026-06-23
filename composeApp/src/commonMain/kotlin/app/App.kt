package app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ui.WaitroomScreen

@Composable
fun App(
    lifecycleActive: Boolean = true
) {
    MaterialTheme {
        WaitroomScreen(lifecycleActive = lifecycleActive)
    }
}
