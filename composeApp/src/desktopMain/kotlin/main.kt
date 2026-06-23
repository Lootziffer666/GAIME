import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.App
import desktop.AlwaysOnTopController

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GAIME - AI Waitroom",
        state = rememberWindowState(width = 900.dp, height = 600.dp),
        // Always-on-top is controlled by AlwaysOnTopController.
        // Default is false. To enable: call AlwaysOnTopController.enable()
        // or toggle with AlwaysOnTopController.toggle() (e.g., from a keyboard shortcut).
        alwaysOnTop = AlwaysOnTopController.isAlwaysOnTop
    ) {
        App()
    }
}
