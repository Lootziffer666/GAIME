package rpg.save

/**
 * Minimal persistent storage for a serialized [GameSaveState] JSON string.
 *
 * Lives in :composeApp (not :core) because the platform actuals need
 * androidMain/desktopMain source sets, which only :composeApp provides.
 * [GameSaveState] itself stays in :core as pure data.
 *
 * Implementations write/read a single save slot and must be robust: failures
 * should be swallowed (saving is best-effort) and [loadGame] returns null when
 * no valid save is present.
 */
expect fun saveGame(json: String)

/** Returns the persisted save JSON, or null if none exists / on error. */
expect fun loadGame(): String?
