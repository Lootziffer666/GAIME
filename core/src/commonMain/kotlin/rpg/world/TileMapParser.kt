package rpg.world

/** How a single ASCII glyph maps to a tile. */
data class TileSpec(
    val atlasIndex: Int,
    val blocked: Boolean = false,
    val trigger: String? = null,
    val spawn: Boolean = false
)

/**
 * Builds a [TileMap] from ASCII art plus a legend. Authoring maps as text keeps
 * them readable, diffable and easy to test -- a lightweight stand-in for a full
 * Tiled .tmx importer (which can be added later without touching consumers).
 */
object TileMapParser {

    fun fromAscii(rows: List<String>, legend: Map<Char, TileSpec>): TileMap {
        require(rows.isNotEmpty()) { "map must have at least one row" }
        val height = rows.size
        val width = rows.maxOf { it.length }
        require(width > 0) { "map must have at least one column" }

        val blocked = BooleanArray(width * height)
        val tiles = IntArray(width * height)
        val triggers = mutableMapOf<Int, String>()
        var spawnX = -1
        var spawnY = -1

        for (y in 0 until height) {
            val row = rows[y]
            for (x in 0 until width) {
                // Pad short rows with the first legend entry (treated as wall-safe default).
                val glyph = if (x < row.length) row[x] else ' '
                val spec = legend[glyph]
                    ?: error("No legend entry for glyph '$glyph' at ($x,$y)")
                val i = y * width + x
                tiles[i] = spec.atlasIndex
                blocked[i] = spec.blocked
                spec.trigger?.let { triggers[i] = it }
                if (spec.spawn) {
                    spawnX = x
                    spawnY = y
                }
            }
        }

        require(spawnX >= 0) { "map legend must define exactly one spawn tile" }
        return TileMap(width, height, blocked, tiles, triggers, spawnX, spawnY)
    }

    /**
     * Builds a [TileMap] from a baked collision grid ('#' = blocked, anything
     * else = walkable). Used for HD worlds whose visuals come from a pre-rendered
     * background image (see [BakedMaps] / scripts/tmx_render.py) rather than a
     * tile atlas, so [TileMap.tileAt] is irrelevant and all tile indices are 0.
     * Spawn and trigger cells are supplied by coordinate.
     */
    fun fromCollision(
        rows: List<String>,
        spawnX: Int,
        spawnY: Int,
        triggers: Map<Pair<Int, Int>, String> = emptyMap()
    ): TileMap {
        require(rows.isNotEmpty()) { "map must have at least one row" }
        val height = rows.size
        val width = rows.maxOf { it.length }
        require(width > 0) { "map must have at least one column" }
        require(spawnX in 0 until width && spawnY in 0 until height) {
            "spawn ($spawnX,$spawnY) out of bounds ${width}x$height"
        }

        val blocked = BooleanArray(width * height)
        val tiles = IntArray(width * height) // unused: visuals come from a baked image
        for (y in 0 until height) {
            val row = rows[y]
            for (x in 0 until width) {
                val c = if (x < row.length) row[x] else '#'
                blocked[y * width + x] = c == '#'
            }
        }
        require(!blocked[spawnY * width + spawnX]) { "spawn ($spawnX,$spawnY) is on a blocked cell" }

        val triggerMap = triggers.entries.associate { (pos, id) ->
            (pos.second * width + pos.first) to id
        }
        return TileMap(width, height, blocked, tiles, triggerMap, spawnX, spawnY)
    }
}
