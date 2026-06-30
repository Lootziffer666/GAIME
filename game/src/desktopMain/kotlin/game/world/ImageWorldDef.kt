package game.world

import game.DialogLine
import game.Facing
import rpg.bark.BarkEvent
import rpg.weather.Season

/**
 * Step 14: Data definitions for the Bild+Grid (Image World) runtime.
 *
 * These replace MapConfig for the HD painted-background maps. Each map is a painted
 * image + invisible collision grid (TMX), where NPCs are invisible interaction hotspots
 * (the painted background already shows the characters).
 *
 * Grid coordinates are 0-based for these finite (non-infinite) TMX maps.
 */

/**
 * An invisible NPC interaction hotspot on the grid.
 * No sprite rendered — the painted background provides the visual.
 */
data class ImageNpcHotspot(
    val cellX: Int,
    val cellY: Int,
    val facingHint: Facing,
    val dialog: List<DialogLine>,
    val barkEvent: BarkEvent?,
)

/**
 * An exit tile: stepping on it transitions to another image map.
 */
data class ImageMapExit(
    val cellX: Int,
    val cellY: Int,
    val destination: ImageMapId,
    val destSpawnX: Int,
    val destSpawnY: Int,
)

/**
 * Identifies image-based maps in the HD painted world.
 */
enum class ImageMapId {
    TAVERN_INTERIOR,
    SYLVANORIA_WILDWOOD,
}

/**
 * Complete definition for one image-based map.
 *
 * [imagePath]   — resource path to the painted background PNG.
 * [gridTmxPath] — resource path to the collision TMX.
 * [spawn]       — grid cell where the player appears; null = derive from CollisionGrid center.
 * [npcs]        — invisible interaction hotspots (placed on walkable cells near painted figures).
 * [exits]       — cells that trigger map transitions.
 */
data class ImageWorldDef(
    val id: ImageMapId,
    val imagePath: String,
    val gridTmxPath: String,
    val displayName: String,
    val spawn: Pair<Int, Int>?,
    val npcs: List<ImageNpcHotspot>,
    val exits: List<ImageMapExit>,
    val season: Season? = null,
) {
    companion object {
        /**
         * Tavern Interior — 78x78 grid, 1254x1254px painted background.
         * Barkeep near counter area, Patron at tables.
         * Exit at bottom edge → Wildwood.
         *
         * Hotspot cells verified WALKABLE against CollisionGrid:
         * - Barkeep at (25, 30) — bar counter area, Floor layer present, no Wall.
         * - Patron at (40, 40) — table area, Floor layer present, no Wall.
         * - Exit at (35, 73) — bottom walkable edge.
         */
        fun tavernInterior(): ImageWorldDef = ImageWorldDef(
            id = ImageMapId.TAVERN_INTERIOR,
            imagePath = "assets/HD/backgrounds/tavern_interior.png",
            gridTmxPath = "assets/HD/backgrounds/tavern_interior.tmx",
            displayName = "Heroes' Home",
            spawn = 39 to 50, // center of large walkable area
            npcs = listOf(
                ImageNpcHotspot(
                    cellX = 25,
                    cellY = 30,
                    facingHint = Facing.RIGHT,
                    dialog = listOf(
                        DialogLine("Barkeep", "Spend some coin or get out."),
                        DialogLine("Barkeep", "You've been officially registered as a Hero Party.\nDon't ask how."),
                        DialogLine("Nib", "...by who?"),
                        DialogLine("Barkeep", "The Questbook. It fell on the desk and opened\nto the right page. Fate, probably."),
                    ),
                    barkEvent = BarkEvent.BARKEEP_SPEND_SOME_COIN,
                ),
                ImageNpcHotspot(
                    cellX = 40,
                    cellY = 40,
                    facingHint = Facing.LEFT,
                    dialog = listOf(
                        DialogLine("Patron", "He sure is slow for a four-armed bartender."),
                        DialogLine("Patron", "I hear the king likes to wear evening gowns."),
                    ),
                    barkEvent = BarkEvent.PATRON_HE_SURE_IS_SLOW,
                ),
            ),
            exits = listOf(
                ImageMapExit(
                    cellX = 35,
                    cellY = 73,
                    destination = ImageMapId.SYLVANORIA_WILDWOOD,
                    destSpawnX = 2,
                    destSpawnY = 24,
                ),
            ),
        )

        /**
         * Sylvanoria Wildwood — 86x48 grid, wide landscape.
         * Guard on the eastern path. Exit on left edge → Tavern.
         *
         * Hotspot cells verified WALKABLE:
         * - Guard at (62, 4) — path area, Floor present, no Wall.
         * - Exit at (0, 24) — left edge, walkable.
         */
        fun sylvanoriaWildwood(): ImageWorldDef = ImageWorldDef(
            id = ImageMapId.SYLVANORIA_WILDWOOD,
            imagePath = "assets/HD/backgrounds/sylvanoria_wildwood.png",
            gridTmxPath = "assets/HD/backgrounds/sylvanoria_wildwood.tmx",
            displayName = "Sylvanoria Wildwood",
            spawn = 40 to 24, // center of map, walkable
            npcs = listOf(
                ImageNpcHotspot(
                    cellX = 62,
                    cellY = 4,
                    facingHint = Facing.LEFT,
                    dialog = listOf(
                        DialogLine("Guard", "Back already? The forest doesn't forgive\ntwice."),
                        DialogLine("Guard", "Keep your weapons ready. Things stir\nin the underbrush."),
                    ),
                    barkEvent = BarkEvent.GUARD_BACK_ALREADY,
                ),
                ImageNpcHotspot(
                    cellX = 45,
                    cellY = 22,
                    facingHint = Facing.DOWN,
                    dialog = listOf(
                        DialogLine("Traveler", "The old bridge to the north collapsed\nlast winter."),
                        DialogLine("Traveler", "If you're heading east, take the\npath through the clearing."),
                    ),
                    barkEvent = null,
                ),
            ),
            exits = listOf(
                ImageMapExit(
                    cellX = 0,
                    cellY = 24,
                    destination = ImageMapId.TAVERN_INTERIOR,
                    destSpawnX = 39,
                    destSpawnY = 50,
                ),
            ),
        )

        /** Look up the definition for a given map ID. */
        fun forId(id: ImageMapId): ImageWorldDef = when (id) {
            ImageMapId.TAVERN_INTERIOR -> tavernInterior()
            ImageMapId.SYLVANORIA_WILDWOOD -> sylvanoriaWildwood()
        }
    }
}
