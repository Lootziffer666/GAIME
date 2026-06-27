package rpg.save

import java.io.File

/**
 * Desktop save storage: a single JSON file in a stable app-local directory
 * (`~/.gaime/savegame.json`). Boring and obvious on purpose.
 */
private val saveFile: File by lazy {
    val dir = File(System.getProperty("user.home") ?: ".", ".gaime")
    if (!dir.exists()) dir.mkdirs()
    File(dir, "savegame.json")
}

actual fun saveGame(json: String) {
    runCatching { saveFile.writeText(json) }
}

actual fun loadGame(): String? =
    runCatching { if (saveFile.exists()) saveFile.readText() else null }.getOrNull()
