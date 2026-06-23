package rpg.world

/**
 * A static, top-down tile grid: a collision layer, a render layer (atlas tile
 * indices), and named trigger tiles. Pure data + lookups, no rendering and no
 * Compose dependency, so the map and movement logic are unit-testable.
 */
class TileMap(
    val width: Int,
    val height: Int,
    private val blocked: BooleanArray,
    private val tiles: IntArray,
    /** Cell index (y*width + x) -> trigger id, e.g. the cellar door. */
    val triggers: Map<Int, String>,
    /** Player start tile. */
    val spawnX: Int,
    val spawnY: Int
) {
    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    /** Out-of-bounds counts as blocked, so the player can never leave the map. */
    fun isBlocked(x: Int, y: Int): Boolean = !inBounds(x, y) || blocked[index(x, y)]

    /** Atlas tile index used to render the cell. */
    fun tileAt(x: Int, y: Int): Int = tiles[index(x, y)]

    fun triggerAt(x: Int, y: Int): String? = triggers[index(x, y)]

    private fun index(x: Int, y: Int) = y * width + x
}
