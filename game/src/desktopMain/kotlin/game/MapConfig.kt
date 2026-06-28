package game

/**
 * Identifies one of the two available TMX locations.
 */
enum class MapId { INTERIOR, EXTERIOR }

/**
 * A configured exit tile: stepping on (tileX, tileY) triggers a transition to
 * [destination], spawning the player at (spawnX, spawnY) on the new map.
 */
data class MapExit(
    val tileX: Int,
    val tileY: Int,
    val destination: MapId,
    val spawnX: Int,
    val spawnY: Int,
)

/**
 * Complete configuration for one map location. WorldScene reads this from
 * [WorldScene.pendingConfig] and never stores renderer state here.
 *
 * [id]          — which map this is (used as display name + for transitions).
 * [tmxDir]      — directory containing the TMX file and its referenced tilesets.
 * [tmxFile]     — file name of the TMX map (relative to tmxDir).
 * [spawnX/Y]    — tile coordinates where the player appears on entry.
 * [bgmPath]     — asset path for background music (passed to AudioManager.playMusic).
 * [npcs]        — NPC entities placed on this map.
 * [exits]       — exit tiles that trigger a transition to another map.
 * [displayName] — shown in the HUD location label.
 */
data class MapConfig(
    val id: MapId,
    val tmxDir: String,
    val tmxFile: String,
    val spawnX: Int,
    val spawnY: Int,
    val bgmPath: String,
    val npcs: List<NpcDefinition>,
    val exits: List<MapExit>,
    val displayName: String,
) {
    val tmxPath: String get() = "$tmxDir/$tmxFile"

    companion object {
        /**
         * Heroes' Home interior — Interior1.tmx.
         * NPCs: Barkeep at (4,8), Patron at (12,16).
         * Exit: Tile (8,1) → EXTERIOR, spawn (8,20).
         */
        // Heroes' Home interior — Interior1.tmx. INFINITE map: real walkable room is
        // tileX -7..2 / tileY -2..4 (chunk offsets, NOT 0-based — see KNOWN_BUGS).
        // Spawn (-5,1) room centre. NPCs: Barkeep (-6,0), Patron (-3,2).
        // Exit (-5,4) → EXTERIOR, arrival (-5,9). All coords verified WALKABLE.
        fun interior(): MapConfig = MapConfig(
            id = MapId.INTERIOR,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Interior1.tmx",
            spawnX = -5,
            spawnY = 1,
            bgmPath = "assets/audio/music/Quest_Accepted_Unfortunately_.mp3",
            displayName = "Heroes' Home",
            npcs = listOf(
                NpcDefinition(
                    tileX = -6,
                    tileY = 0,
                    idleSheetPath = "assets/HD/characters/swordsman/PNG/Swordsman_lvl2/Without_shadow/Swordsman_lvl2_Idle_without_shadow.png",
                    facing = Facing.RIGHT,
                    dialog = listOf(
                        DialogLine("Barkeep", "Spend some coin or get out."),
                        DialogLine("Barkeep", "You've been officially registered as a Hero Party.\nDon't ask how."),
                        DialogLine("Nib", "...by who?"),
                        DialogLine("Barkeep", "The Questbook. It fell on the desk and opened\nto the right page. Fate, probably."),
                    )
                ),
                NpcDefinition(
                    tileX = -3,
                    tileY = 2,
                    idleSheetPath = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow/Vampires1_Idle_without_shadow.png",
                    facing = Facing.LEFT,
                    dialog = listOf(
                        DialogLine("Patron", "He sure is slow for a four-armed bartender."),
                        DialogLine("Patron", "I hear the king likes to wear evening gowns."),
                    )
                ),
            ),
            exits = listOf(
                MapExit(tileX = -5, tileY = 4, destination = MapId.EXTERIOR, spawnX = -5, spawnY = 9),
            ),
        )

        // Heroes' Home exterior — Exterior.tmx. INFINITE map: open courtyard is
        // tileX -18..8 / tileY 7..11 (fully walkable). Spawn (-5,9).
        // NPCs: Guard (-3,9), Traveler (0,10). Exit (-5,7) → INTERIOR, arrival (-5,1).
        fun exterior(): MapConfig = MapConfig(
            id = MapId.EXTERIOR,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -5,
            spawnY = 9,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Village Exterior",
            npcs = listOf(
                NpcDefinition(
                    tileX = -3,
                    tileY = 9,
                    idleSheetPath = "assets/HD/characters/swordsman/PNG/Swordsman_lvl3/Without_shadow/Swordsman_lvl3_Idle_without_shadow.png",
                    facing = Facing.RIGHT,
                    dialog = listOf(
                        DialogLine("Guard", "Who goes there?"),
                        DialogLine("Guard", "The forest trail east of here has been\noverrun by wolves."),
                        DialogLine("Guard", "If you're looking for trouble, you'll find it there."),
                        DialogLine("Nib", "Just keep to the trail."),
                    )
                ),
                NpcDefinition(
                    tileX = 0,
                    tileY = 10,
                    idleSheetPath = null,
                    facing = Facing.DOWN,
                    dialog = listOf(
                        DialogLine("Traveler", "Where's the nearest inn?"),
                        DialogLine("Traveler", "I've been walking since sunrise."),
                    )
                ),
            ),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
            ),
        )

        /** Returns the MapConfig for [id] using the default spawn coordinates. */
        fun forId(id: MapId): MapConfig = when (id) {
            MapId.INTERIOR -> interior()
            MapId.EXTERIOR -> exterior()
        }

        /**
         * Returns a MapConfig for [id] with overridden spawn position.
         * Used by map exits to land the player at the correct entry tile.
         */
        fun forId(id: MapId, spawnX: Int, spawnY: Int): MapConfig =
            forId(id).copy(spawnX = spawnX, spawnY = spawnY)
    }
}
