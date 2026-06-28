package game

/**
 * Defines a non-player character placed at a fixed tile position on the map.
 *
 * [tileX], [tileY]    — logical grid position in the TMX map.
 * [idleSheetPath]     — asset path for the idle sprite sheet (loaded via SpriteLoader).
 *                       May be null to use a procedural fallback (purple rect).
 * [facing]            — initial facing direction (LEFT/RIGHT/UP/DOWN).
 * [dialog]            — lines shown sequentially when player interacts (E key).
 *                       Empty list = NPC is silent (no interaction prompt).
 */
data class NpcDefinition(
    val tileX: Int,
    val tileY: Int,
    val idleSheetPath: String?,
    val facing: Facing = Facing.DOWN,
    val dialog: List<DialogLine>,
    val barkEvent: rpg.bark.BarkEvent? = null,
)
