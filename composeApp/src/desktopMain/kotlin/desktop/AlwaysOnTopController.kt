package desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller for the desktop window always-on-top behavior.
 *
 * This is prepared but not forced active by default. To enable always-on-top:
 * - Call AlwaysOnTopController.enable() at any point during runtime
 * - Or set AlwaysOnTopController.isAlwaysOnTop = true directly
 *
 * The window in main.kt reads this state and applies it to the Window composable.
 * This can be toggled at runtime, e.g., via a settings menu or keyboard shortcut.
 */
object AlwaysOnTopController {
    /**
     * Whether the window should remain on top of all other windows.
     * Default is false - the window behaves normally.
     */
    var isAlwaysOnTop: Boolean by mutableStateOf(false)

    /** Enable always-on-top mode. */
    fun enable() {
        isAlwaysOnTop = true
    }

    /** Disable always-on-top mode. */
    fun disable() {
        isAlwaysOnTop = false
    }

    /** Toggle always-on-top mode. */
    fun toggle() {
        isAlwaysOnTop = !isAlwaysOnTop
    }
}
