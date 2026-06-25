package rpg.questbook

/**
 * The local situation a bark fires in. Drives each bark's failsafe behaviour
 * (the "Safe/Failsafe Behavior" column of the Bark Table): when a bark's target
 * condition is not met, the Questbook still produces a defined, harmless result.
 */
data class RoomContext(
    /** Identifier of the loaded map (pressure + markers are scoped to this). */
    val mapId: String,
    /** Identifier of the current room/screen within the map. */
    val roomId: String,
    val hasInteractableTarget: Boolean = false,
    val hasBreakableObstacle: Boolean = false,
    val hasFlammableTarget: Boolean = false,
    val hasPuzzleElement: Boolean = false,
    val hasContainer: Boolean = false,
    val hasEnemies: Boolean = false
) {
    companion object {
        /** Well-known room ids the slice reaction table disambiguates on. */
        const val ROOM_TAVERN = "tavern"
        const val ROOM_SEWER_CORRIDOR = "sewer_corridor"
        const val ROOM_MINI_DUNGEON = "mini_dungeon"
        const val ROOM_BOSS = "boss_room"

        // ═══ Chapter 2 ══════════════════════════════════════════════════
        const val ROOM_MARKET = "market"
        const val ROOM_FOREST = "forest"
        const val ROOM_FOREST_SHRINE = "forest_shrine"
        const val ROOM_FOREST_BOSS = "forest_boss"
    }
}
