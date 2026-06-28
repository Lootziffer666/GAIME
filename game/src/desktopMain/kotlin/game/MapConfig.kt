package game

import rpg.bark.BarkEvent

/**
 * Identifies one of the available TMX locations.
 */
enum class MapId { INTERIOR, EXTERIOR, FROZEN_APPROACH, SPRING_APPROACH, SUMMER_APPROACH, AUTUMN_APPROACH, CHAPEL, GUILD_HALL, GLASSBLOWERS, RUINED_TEMPLE, BRIDGE }

/**
 * Weather type for the atmosphere system.
 */
enum class Weather { CLEAR, RAIN, SNOW }

/**
 * Atmospheric settings for a map: season, time of day, weather type, and fog density.
 */
data class WorldAtmosphere(
    val season: String = "summer",
    val timeOfDay: Float = 0.5f,
    val weather: Weather = Weather.CLEAR,
    val fog: Float = 0f,
)

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
    val atmosphere: WorldAtmosphere = WorldAtmosphere(),
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
                    ),
                    barkEvent = BarkEvent.BARKEEP_SPEND_SOME_COIN,
                ),
                NpcDefinition(
                    tileX = -3,
                    tileY = 2,
                    idleSheetPath = "assets/HD/characters/vampire/PNG/Vampires1/Without_shadow/Vampires1_Idle_without_shadow.png",
                    facing = Facing.LEFT,
                    dialog = listOf(
                        DialogLine("Patron", "He sure is slow for a four-armed bartender."),
                        DialogLine("Patron", "I hear the king likes to wear evening gowns."),
                    ),
                    barkEvent = BarkEvent.PATRON_HE_SURE_IS_SLOW,
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
                    ),
                    barkEvent = BarkEvent.GUARD_BACK_ALREADY,
                ),
                NpcDefinition(
                    tileX = 0,
                    tileY = 10,
                    idleSheetPath = null,
                    facing = Facing.DOWN,
                    dialog = listOf(
                        DialogLine("Traveler", "Where's the nearest inn?"),
                        DialogLine("Traveler", "I've been walking since sunrise."),
                    ),
                    barkEvent = null,
                ),
            ),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
                // Step 10: Exits to Stokeport locations (use walkable tiles on the edges)
                MapExit(tileX = -8, tileY = 7, destination = MapId.CHAPEL, spawnX = -4, spawnY = -3),
                MapExit(tileX = -12, tileY = 9, destination = MapId.GUILD_HALL, spawnX = 0, spawnY = -1),
                MapExit(tileX = 5, tileY = 7, destination = MapId.GLASSBLOWERS, spawnX = 0, spawnY = 2),
            ),
        )

        /** Returns the MapConfig for [id] using the default spawn coordinates. */
        fun forId(id: MapId): MapConfig = when (id) {
            MapId.INTERIOR -> interior()
            MapId.EXTERIOR -> exterior()
            MapId.FROZEN_APPROACH -> frozenApproach()
            MapId.SPRING_APPROACH -> springApproach()
            MapId.SUMMER_APPROACH -> summerApproach()
            MapId.AUTUMN_APPROACH -> autumnApproach()
            MapId.CHAPEL -> chapel()
            MapId.GUILD_HALL -> guildHall()
            MapId.GLASSBLOWERS -> glassblowers()
            MapId.RUINED_TEMPLE -> ruinedTemple()
            MapId.BRIDGE -> bridge()
        }

        /**
         * Returns a MapConfig for [id] with overridden spawn position.
         * Used by map exits to land the player at the correct entry tile.
         */
        fun forId(id: MapId, spawnX: Int, spawnY: Int): MapConfig =
            forId(id).copy(spawnX = spawnX, spawnY = spawnY)

        /**
         * The Frozen Approach — uses Exterior.tmx in winter conditions.
         * Night time, heavy snow, fog. A cold and dangerous path.
         */
        fun frozenApproach(): MapConfig = MapConfig(
            id = MapId.FROZEN_APPROACH,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -5,
            spawnY = 9,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "The Frozen Approach",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
            ),
            atmosphere = WorldAtmosphere(
                season = "winter",
                timeOfDay = 0.1f,
                weather = Weather.SNOW,
                fog = 0.4f,
            ),
        )

        /**
         * Spring Approach — uses Exterior.tmx in bright spring morning.
         * Clear sky, flowers blooming, gentle light.
         */
        fun springApproach(): MapConfig = MapConfig(
            id = MapId.SPRING_APPROACH,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -5,
            spawnY = 9,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "The Spring Approach",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
            ),
            atmosphere = WorldAtmosphere(
                season = "spring",
                timeOfDay = 0.55f,
                weather = Weather.CLEAR,
                fog = 0.1f,
            ),
        )

        /**
         * Summer Approach — uses Exterior.tmx in long summer daylight.
         * Clear sky, warm light, lush grass.
         */
        fun summerApproach(): MapConfig = MapConfig(
            id = MapId.SUMMER_APPROACH,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -5,
            spawnY = 9,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "The Summer Approach",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
            ),
            atmosphere = WorldAtmosphere(
                season = "summer",
                timeOfDay = 0.65f,
                weather = Weather.CLEAR,
                fog = 0.0f,
            ),
        )

        /**
         * Autumn Approach — uses Exterior.tmx in rainy autumn overcast.
         * Rain, wind, fallen leaves, darker sky.
         */
        fun autumnApproach(): MapConfig = MapConfig(
            id = MapId.AUTUMN_APPROACH,
            tmxDir = "assets/HD/locations/heroes-home/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -5,
            spawnY = 9,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "The Autumn Approach",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -5, tileY = 7, destination = MapId.INTERIOR, spawnX = -5, spawnY = 1),
            ),
            atmosphere = WorldAtmosphere(
                season = "autumn",
                timeOfDay = 0.4f,
                weather = Weather.RAIN,
                fog = 0.2f,
            ),
        )

        // =====================================================================
        // Step 10: Stokeport Locations
        // =====================================================================

        /**
         * Chapel Exterior — chapel/Tiled_files/Exterior.tmx.
         * Infinite map. Walkable bbox: x=-14..15, y=-14..7.
         * Spawn (-4,-3) verified WALKABLE (nearest to center).
         * Exit: top edge → back to EXTERIOR.
         */
        fun chapel(): MapConfig = MapConfig(
            id = MapId.CHAPEL,
            tmxDir = "assets/HD/locations/chapel/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = -4,
            spawnY = -3,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Chapel Grounds",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -4, tileY = -14, destination = MapId.EXTERIOR, spawnX = -3, spawnY = 9),
            ),
        )

        /**
         * Guild Hall Exterior — guild-hall/Tiled_files/Exterior.tmx.
         * Infinite map. Grid: 20x13, offset=(-9,-8). Walkable bbox: x=-9..10, y=-8..4.
         * Spawn (0,-1) verified WALKABLE (nearest to center).
         * Exit: top edge → back to EXTERIOR.
         */
        fun guildHall(): MapConfig = MapConfig(
            id = MapId.GUILD_HALL,
            tmxDir = "assets/HD/locations/guild-hall/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = 0,
            spawnY = -1,
            bgmPath = "assets/audio/music/Quest_Accepted_Unfortunately_.mp3",
            displayName = "Guild Hall",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = 0, tileY = -8, destination = MapId.EXTERIOR, spawnX = 0, spawnY = 9),
                MapExit(tileX = 0, tileY = 4, destination = MapId.BRIDGE, spawnX = 0, spawnY = 18),
            ),
        )

        /**
         * Glassblowers Workshop Exterior — glassblowers-workshop/Tiled_files/Exterior.tmx.
         * Infinite map. Grid: 22x15, offset=(-10,-10). Walkable bbox: x=-10..11, y=-3..4.
         * Spawn (0,2) verified WALKABLE (nearest to center).
         * Exit: left edge → back to EXTERIOR.
         */
        fun glassblowers(): MapConfig = MapConfig(
            id = MapId.GLASSBLOWERS,
            tmxDir = "assets/HD/locations/glassblowers-workshop/Tiled_files",
            tmxFile = "Exterior.tmx",
            spawnX = 0,
            spawnY = 2,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Glassblowers Workshop",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -10, tileY = 0, destination = MapId.EXTERIOR, spawnX = 0, spawnY = 9),
            ),
        )

        /**
         * Ruined Temple Exterior — ruined-temple/Tiled_files/Ruined_temple_exterior.tmx.
         * Infinite map. Grid: 23x17, offset=(-11,-13). ~86 walkable cells.
         * Spawn (0,-2) verified WALKABLE (temple courtyard). Trees block (walk around).
         * Exit: bottom edge → back to BRIDGE.
         */
        fun ruinedTemple(): MapConfig = MapConfig(
            id = MapId.RUINED_TEMPLE,
            tmxDir = "assets/HD/locations/ruined-temple/Tiled_files",
            tmxFile = "Ruined_temple_exterior.tmx",
            spawnX = 0,
            spawnY = -2,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Ruined Temple",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = 0, tileY = 3, destination = MapId.BRIDGE, spawnX = 0, spawnY = 18),
            ),
        )

        /**
         * Bridge — bridges/PNG_n_Tiled/Bridges.tmx.
         * Infinite map. Grid: 85x69, offset=(-32,-16). Bridge surface is WALKABLE
         * (spans the water); spawn (0,18) verified walkable with open neighbours.
         * Exits: west → GUILD_HALL, east → RUINED_TEMPLE.
         */
        fun bridge(): MapConfig = MapConfig(
            id = MapId.BRIDGE,
            tmxDir = "assets/HD/locations/bridges/PNG_n_Tiled",
            tmxFile = "Bridges.tmx",
            spawnX = 0,
            spawnY = 18,
            bgmPath = "assets/audio/music/Sovereign_Heights.mp3",
            displayName = "Stone Bridge",
            npcs = emptyList(),
            exits = listOf(
                MapExit(tileX = -20, tileY = 10, destination = MapId.GUILD_HALL, spawnX = 0, spawnY = -1),
                MapExit(tileX = 40, tileY = 10, destination = MapId.RUINED_TEMPLE, spawnX = 0, spawnY = -2),
            ),
        )
    }
}
