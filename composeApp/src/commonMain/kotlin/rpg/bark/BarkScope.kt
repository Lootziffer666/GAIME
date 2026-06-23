package rpg.bark

/**
 * How far a bark's effect reaches. No bark effect persists beyond a map
 * transition except explicitly story-locked events (docs note 5).
 */
enum class BarkScope {
    /** The visible screen area. */
    CURRENT_ROOM,

    /** The loaded map file. */
    CURRENT_MAP
}
