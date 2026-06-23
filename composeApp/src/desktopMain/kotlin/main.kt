import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GAIME - AI Waitroom",
        state = rememberWindowState(width = 900.dp, height = 600.dp)
    ) {
        App()
    }
}
