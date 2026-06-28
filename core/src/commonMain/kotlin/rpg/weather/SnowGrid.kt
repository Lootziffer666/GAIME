package rpg.weather

/**
 * Simulates snow accumulation and clearance on a tile grid.
 * Engine-agnostic -- lives in :core for unit testing.
 *
 * Each cell holds a snowDepth (0.0 = bare ground). Snow accumulates uniformly,
 * can be cleared at specific cells (footprints, shoveling), and slowly regrows
 * (fresh snowfall fills in cleared areas).
 */
class SnowGrid(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    private val grid = Array(height) { FloatArray(width) { 0f } }

    companion object {
        const val MAX_DEPTH = 1.0f
    }

    /** Current snow depth at grid coordinates (bounds-checked). */
    fun depthAt(x: Int, y: Int): Float {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return 0f
        return grid[ny][nx]
    }

    /** Set snow depth directly (for testing / initialization). */
    operator fun set(x: Int, y: Int, value: Float) {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return
        grid[ny][nx] = value.coerceIn(0f, MAX_DEPTH)
    }

    /** Add snow uniformly to all cells. */
    fun accumulate(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            grid[y][x] = (grid[y][x] + rate).coerceAtMost(MAX_DEPTH)
        }
    }

    /** Clear snow at a specific cell (footprint depression). Reduces depth by [amount]. */
    fun clearAt(x: Int, y: Int, amount: Float) {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return
        grid[ny][nx] = (grid[ny][nx] - amount).coerceAtLeast(0f)
    }

    /** Slowly refill cleared cells (fresh snowfall covering tracks). */
    fun regrow(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            if (grid[y][x] < MAX_DEPTH) {
                grid[y][x] = (grid[y][x] + rate).coerceAtMost(MAX_DEPTH)
            }
        }
    }
}
