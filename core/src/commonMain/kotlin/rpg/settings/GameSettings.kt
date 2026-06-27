package rpg.settings

/**
 * Player-adjustable game settings. Pure data with no I/O and no platform
 * dependencies: the renderer/host reads these and persistence is handled
 * elsewhere (see [rpg.save.GameSaveState]).
 */
data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val locale: String = "en"
)
