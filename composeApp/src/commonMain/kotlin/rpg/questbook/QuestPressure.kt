package rpg.questbook

/**
 * The single global Quest Pressure meter (docs/GAME_CONCEPT_LOCK.md).
 * Pressure only affects the current map and resets on map transition.
 */
enum class QuestPressure {
    LOW,
    MEDIUM,
    HIGH;

    /** Next level up, capped at [HIGH]. Pressure can never exceed HIGH (docs note 6). */
    fun raised(): QuestPressure = when (this) {
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> HIGH
    }

    val isMax: Boolean get() = this == HIGH
}
