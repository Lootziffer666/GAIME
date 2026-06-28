package rpg.tiled

/**
 * Tile-derived collision raster generated from layer names of a [TiledMap].
 *
 * Strategy: honour Tiled's LAYER ORDER. Layers are declared bottom → top; the
 * topmost non-empty, non-decorative layer at a cell decides its collision type,
 * exactly as Tiled renders the visible surface. Role → type:
 *   FLOOR / BRIDGE → WALKABLE,  WATER → WATER,  SOLID → BLOCKED,  TRIGGER → TRIGGER.
 * DECORATIVE layers are skipped (flowers/objects never change collision).
 * Cells covered by no collision layer default to BLOCKED.
 *
 * This makes both base-layer cases correct without special priority rules:
 *   - water canvas at the bottom + land on top  → land wins → WALKABLE (ruined-temple)
 *   - ground at the bottom + a pond on top       → water wins → WATER
 *   - water + a bridge on top                    → bridge wins → WALKABLE (Bridges.tmx)
 *
 * Bounding box is derived from all non-empty cells across all layers.
 * Negative grid coordinates (infinite maps) are normalized to 0-based indices.
 */

enum class TileType { WALKABLE, BLOCKED, WATER, TRIGGER, DECORATIVE }

class CollisionGrid private constructor(
    val cols: Int,
    val rows: Int,
    val offsetX: Int,
    val offsetY: Int,
    private val grid: Array<Array<TileType>>,
) {
    /**
     * Query collision at normalized coordinates (0-based).
     * Use [offsetX]/[offsetY] to translate from world grid coords.
     */
    operator fun get(x: Int, y: Int): TileType {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return TileType.BLOCKED
        return grid[y][x]
    }

    companion object {
        fun from(map: TiledMap): CollisionGrid {
            // 1. Classify layers
            data class ClassifiedLayer(val role: LayerRole, val cells: List<TileCell>)

            val classified = map.layers.map { ClassifiedLayer(layerType(it.name), it.cells) }

            // 2. Bounding box over all non-empty cells
            var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE
            for (cl in classified) {
                for (c in cl.cells) {
                    if (c.gridX < minX) minX = c.gridX
                    if (c.gridY < minY) minY = c.gridY
                    if (c.gridX > maxX) maxX = c.gridX
                    if (c.gridY > maxY) maxY = c.gridY
                }
            }
            if (minX > maxX) {
                // Empty map
                return CollisionGrid(0, 0, 0, 0, emptyArray())
            }
            val cols = maxX - minX + 1
            val rows = maxY - minY + 1

            // 3. Build grid — default BLOCKED (cell never covered by a collision layer).
            val grid = Array(rows) { Array(cols) { TileType.BLOCKED } }

            // Honour TILED LAYER ORDER (declared bottom → top): the topmost non-empty,
            // non-decorative layer at a cell decides its collision type — exactly how
            // Tiled renders the visible surface. This makes both base-layer cases work:
            //  - water canvas at the bottom + land on top  → land wins → WALKABLE
            //  - ground at the bottom + a pond on top      → water wins → WATER
            // DECORATIVE layers are skipped so flowers/objects never change collision.
            for (cl in classified) {
                val type = when (cl.role) {
                    LayerRole.FLOOR, LayerRole.BRIDGE -> TileType.WALKABLE
                    LayerRole.WATER -> TileType.WATER
                    LayerRole.SOLID -> TileType.BLOCKED
                    LayerRole.TRIGGER -> TileType.TRIGGER
                    LayerRole.DECORATIVE -> continue
                }
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = type
                }
            }

            return CollisionGrid(cols, rows, minX, minY, grid)
        }

        private fun layerType(name: String): LayerRole {
            val lower = name.lowercase()
            return when {
                // FLOOR
                lower.startsWith("floor") || lower == "ground" || lower.startsWith("road") ||
                    lower.startsWith("grass") || lower.startsWith("carpet") ||
                    lower.startsWith("spots") || lower.startsWith("plates") ||
                    lower.startsWith("stairs") || lower == "site" -> LayerRole.FLOOR

                // WATER
                lower.startsWith("water") -> LayerRole.WATER

                // BRIDGE — walkable surface that spans water (must override WATER).
                lower.startsWith("bridge") -> LayerRole.BRIDGE

                // SOLID — trees block movement (walk around them; destructible via the
                // BRUGG_ATTACK → ClearObstacle "Demolition Permit" bark is a future hook).
                lower.startsWith("wall") || lower.startsWith("house") ||
                    lower.startsWith("roof") || lower.startsWith("fence") ||
                    lower.startsWith("statues") || lower.startsWith("columns") ||
                    lower.startsWith("bricks") || lower.startsWith("trees") ||
                    lower.startsWith("boxes") ||
                    lower.startsWith("tent") || lower.startsWith("stovepipe") ||
                    lower.startsWith("forge") || lower.startsWith("barrel") ||
                    lower.startsWith("altar") || lower.startsWith("spikes") ||
                    lower.startsWith("blades") -> LayerRole.SOLID

                // TRIGGER
                lower.startsWith("door") || lower.startsWith("ladder") ||
                    lower.startsWith("entrance") || lower.startsWith("sign") ||
                    lower.startsWith("chest") || lower.startsWith("lever") -> LayerRole.TRIGGER

                // Everything else: decorative (no collision)
                else -> LayerRole.DECORATIVE
            }
        }
    }
}

private enum class LayerRole { FLOOR, WATER, BRIDGE, SOLID, TRIGGER, DECORATIVE }
