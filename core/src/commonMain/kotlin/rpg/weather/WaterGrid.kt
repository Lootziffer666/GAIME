package rpg.weather

/**
 * Simulates water accumulation, flow, and drainage on a tile grid.
 * Engine-agnostic — lives in :core for unit testing.
 *
 * Each cell holds a `waterDepth` (0.0 = dry). Pfützen form when depth exceeds
 * [PUDDLE_THRESHOLD]. Flow transfers water from high to low neighbors.
 * Drain-tiles act as sinks (gutters/channels).
 */
class WaterGrid(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    private val drainTiles: Set<Pair<Int, Int>> = emptySet(),
    private val blockedTiles: Set<Pair<Int, Int>> = emptySet(),
) {
    private val grid = Array(height) { FloatArray(width) { 0f } }

    companion object {
        const val PUDDLE_THRESHOLD = 0.15f
        const val FLOW_RATE = 0.1f
        const val DRAIN_RATE = 0.3f
    }

    /** Current water depth at normalized grid coordinates (0-based). */
    operator fun get(x: Int, y: Int): Float {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0f
        return grid[y][x]
    }

    /** Set water depth directly (for testing / initialization). */
    operator fun set(x: Int, y: Int, value: Float) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        grid[y][x] = value.coerceAtLeast(0f)
    }

    /** Add rain uniformly to all non-blocked cells. */
    fun addRain(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            if ((x to y) !in blockedTiles) {
                grid[y][x] += rate
            }
        }
    }

    /**
     * One simulation step: water flows from high to low neighbors (4-connected).
     * Drain-tiles lose water at [DRAIN_RATE]. Pfützen form and connect emergently.
     */
    fun flowStep() {
        val transfer = Array(height) { FloatArray(width) { 0f } }

        for (y in 0 until height) for (x in 0 until width) {
            val depth = grid[y][x]
            if (depth <= 0f) continue

            // Drain tiles: reduce water
            if ((x to y) in drainTiles) {
                transfer[y][x] -= (depth * DRAIN_RATE).coerceAtMost(depth)
                continue
            }

            // Flow to lower neighbors
            val neighbors = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                .filter { (nx, ny) -> nx in 0 until width && ny in 0 until height }
                .filter { (nx, ny) -> (nx to ny) !in blockedTiles }

            for ((nx, ny) in neighbors) {
                val nDepth = grid[ny][nx]
                if (nDepth < depth - 0.01f) {
                    val flow = ((depth - nDepth) * FLOW_RATE).coerceAtMost(depth * 0.25f)
                    transfer[y][x] -= flow
                    transfer[ny][nx] += flow
                }
            }
        }

        // Apply transfers
        for (y in 0 until height) for (x in 0 until width) {
            grid[y][x] = (grid[y][x] + transfer[y][x]).coerceAtLeast(0f)
        }
    }

    /** Evaporate water uniformly (after rain stops). */
    fun evaporate(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            grid[y][x] = (grid[y][x] - rate).coerceAtLeast(0f)
        }
    }

    /** True if the cell has enough water to form a visible puddle. */
    fun puddleAt(x: Int, y: Int): Boolean = this[x, y] > PUDDLE_THRESHOLD

    /**
     * Flood-fill of connected puddle cells starting at (x,y).
     * Returns empty set if (x,y) is not a puddle.
     */
    fun connectedPuddle(x: Int, y: Int): Set<Pair<Int, Int>> {
        if (!puddleAt(x, y)) return emptySet()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(x to y)
        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            if ((cx to cy) in visited) continue
            if (!puddleAt(cx, cy)) continue
            visited.add(cx to cy)
            if (cx > 0) queue.add(cx - 1 to cy)
            if (cx < width - 1) queue.add(cx + 1 to cy)
            if (cy > 0) queue.add(cx to cy - 1)
            if (cy < height - 1) queue.add(cx to cy + 1)
        }
        return visited
    }
}
