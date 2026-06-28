package rpg.tiled

/**
 * Tile-derived collision raster generated from layer names of a [TiledMap].
 *
 * Strategy (matches the floor-based model in scripts/tmx_render.py):
 * 1. Cells covered by a FLOOR layer → WALKABLE
 * 2. Cells not covered by any FLOOR layer → BLOCKED (ground rule)
 * 3. SOLID layers override with BLOCKED
 * 4. WATER layers override with WATER
 * 5. TRIGGER layers override with TRIGGER
 * 6. DECORATIVE layers have no collision effect
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

            // 3. Build grid — default BLOCKED (no floor = blocked)
            val grid = Array(rows) { Array(cols) { TileType.BLOCKED } }

            // Pass 1: FLOOR → WALKABLE
            for (cl in classified) {
                if (cl.role != LayerRole.FLOOR) continue
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = TileType.WALKABLE
                }
            }

            // Pass 2: WATER → WATER
            for (cl in classified) {
                if (cl.role != LayerRole.WATER) continue
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = TileType.WATER
                }
            }

            // Pass 2b: BRIDGE → WALKABLE (spans water — must override the WATER pass)
            for (cl in classified) {
                if (cl.role != LayerRole.BRIDGE) continue
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = TileType.WALKABLE
                }
            }

            // Pass 3: SOLID → BLOCKED (overrides floor)
            for (cl in classified) {
                if (cl.role != LayerRole.SOLID) continue
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = TileType.BLOCKED
                }
            }

            // Pass 4: TRIGGER → TRIGGER
            for (cl in classified) {
                if (cl.role != LayerRole.TRIGGER) continue
                for (c in cl.cells) {
                    grid[c.gridY - minY][c.gridX - minX] = TileType.TRIGGER
                }
            }

            // DECORATIVE: no-op

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

                // SOLID — NOTE: "trees" is intentionally NOT here. Tree layers are
                // decorative canopy/foliage drawn over walkable ground (heroes-home
                // keeps its trees in DECORATIVE "Objects"/"Grass_top_details" layers);
                // classifying "trees*" as SOLID blanketed whole maps (ruined-temple →
                // 0 walkable). Trees fall through to DECORATIVE (non-blocking).
                lower.startsWith("wall") || lower.startsWith("house") ||
                    lower.startsWith("roof") || lower.startsWith("fence") ||
                    lower.startsWith("statues") || lower.startsWith("columns") ||
                    lower.startsWith("bricks") ||
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
