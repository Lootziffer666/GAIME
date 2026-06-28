package app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ui.WaitroomScreen

/**
 * Compose entrypoint — Waitroom-only.
 * The gameplay engine has moved to :game (KorGE). This module is retained
 * for the Waitroom (animated demo scenes, particle effects).
 */
@Composable
fun App(
    lifecycleActive: Boolean = true
) {
    MaterialTheme {
        WaitroomScreen(lifecycleActive = lifecycleActive)
    }
}
