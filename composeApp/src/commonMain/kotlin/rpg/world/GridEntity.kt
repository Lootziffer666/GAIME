package rpg.world

/** What role an entity plays on the map. */
enum class GridEntityType { NPC, ENEMY }

/**
 * A non-player entity occupying one tile: an NPC to speak to, or an enemy that
 * blocks movement and starts combat when the player tries to step onto its tile.
 * Entities live in [GridWorld.entities] and are independent of the static tile map,
 * so they can be added, removed, or moved without touching the map data.
 */
data class GridEntity(
    val id: String,
    val tileX: Int,
    val tileY: Int,
    val type: GridEntityType,
    /** Drawable resource key, e.g. "hero_brugg", used by WorldScene to pick the sprite. */
    val sprite: String
)
