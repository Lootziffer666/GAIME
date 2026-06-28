package rpg.weather

/**
 * Seasons of the year for the atmosphere system.
 */
enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

/**
 * Per-cell seasonal data for the world grid.
 * Engine-agnostic -- lives in :core for unit testing.
 *
 * Holds per-cell values for spring flowers, summer grass displacement,
 * and autumn fallen leaves. Methods to trample, bend, drop, kick,
 * regrow, and sway are all deterministic (no Random).
 */
class SeasonalGrid(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    /** Spring: flower intensity per cell (0.0 = none, 1.0 = full bloom). */
    private val flowerIntensity = Array(height) { FloatArray(width) { 0f } }

    /** Summer: grass bend displacement (0.0 = upright, 1.0 = fully bent). */
    private val grassBend = Array(height) { FloatArray(width) { 0f } }

    /** Autumn: accumulated leaf count per cell (0.0 = bare, higher = more leaves). */
    private val leafCount = Array(height) { FloatArray(width) { 0f } }

    companion object {
        const val MAX_FLOWER = 1.0f
        const val MAX_LEAF = 1.0f
    }

    // --- Coordinate helpers ---

    private fun toLocal(worldX: Int, worldY: Int): Pair<Int, Int>? {
        val lx = worldX - offsetX; val ly = worldY - offsetY
        if (lx < 0 || lx >= width || ly < 0 || ly >= height) return null
        return lx to ly
    }

    // --- Spring: Flowers ---

    /** Get flower intensity at world coordinates. */
    fun flowerAt(x: Int, y: Int): Float {
        val (lx, ly) = toLocal(x, y) ?: return 0f
        return flowerIntensity[ly][lx]
    }

    /** Set flower intensity directly (for initialization/testing). */
    fun setFlower(x: Int, y: Int, value: Float) {
        val (lx, ly) = toLocal(x, y) ?: return
        flowerIntensity[ly][lx] = value.coerceIn(0f, MAX_FLOWER)
    }

    /** Trample flowers at a cell (player stepped on them). Reduces by [amount]. */
    fun trampleFlower(x: Int, y: Int, amount: Float = 0.4f) {
        val (lx, ly) = toLocal(x, y) ?: return
        flowerIntensity[ly][lx] = (flowerIntensity[ly][lx] - amount).coerceAtLeast(0f)
    }

    /** Slowly regrow flowers across all cells. */
    fun regrowFlowers(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            if (flowerIntensity[y][x] < MAX_FLOWER) {
                flowerIntensity[y][x] = (flowerIntensity[y][x] + rate).coerceAtMost(MAX_FLOWER)
            }
        }
    }

    /**
     * Initialize flowers deterministically based on cell position.
     * Only cells where the hash function produces a specific residue get flowers.
     */
    fun initFlowers(density: Float = 0.6f) {
        for (ly in 0 until height) for (lx in 0 until width) {
            val hash = ((lx * 7 + ly * 13) % 5)
            if (hash < 3) { // ~60% of cells get some flowers
                val intensity = density * ((hash + 1).toFloat() / 3f)
                flowerIntensity[ly][lx] = intensity.coerceIn(0f, MAX_FLOWER)
            }
        }
    }

    // --- Summer: Grass ---

    /** Get grass bend at world coordinates. */
    fun grassBendAt(x: Int, y: Int): Float {
        val (lx, ly) = toLocal(x, y) ?: return 0f
        return grassBend[ly][lx]
    }

    /** Bend grass at a cell (player walked over). Sets bend to 1.0. */
    fun bendGrass(x: Int, y: Int) {
        val (lx, ly) = toLocal(x, y) ?: return
        grassBend[ly][lx] = 1.0f
    }

    /** Gradually unbend grass across all cells. */
    fun unbendGrass(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            if (grassBend[y][x] > 0f) {
                grassBend[y][x] = (grassBend[y][x] - rate).coerceAtLeast(0f)
            }
        }
    }

    /**
     * Apply wind sway factor. This does not modify grassBend itself but returns
     * a global wind offset that the renderer uses for visual displacement.
     * Keeping the method here for API completeness; actual offset calculated
     * from WindState in the overlay.
     */
    fun windSwayGrass(windDx: Float): Float = windDx * 0.3f

    // --- Autumn: Leaves ---

    /** Get leaf count at world coordinates. */
    fun leafCountAt(x: Int, y: Int): Float {
        val (lx, ly) = toLocal(x, y) ?: return 0f
        return leafCount[ly][lx]
    }

    /** Set leaf count directly (for testing). */
    fun setLeafCount(x: Int, y: Int, value: Float) {
        val (lx, ly) = toLocal(x, y) ?: return
        leafCount[ly][lx] = value.coerceIn(0f, MAX_LEAF)
    }

    /**
     * Drop leaves across the grid. Uses deterministic placement based on
     * a time-step counter to avoid randomness.
     */
    fun dropLeaves(rate: Float, timeStep: Int = 0) {
        for (ly in 0 until height) for (lx in 0 until width) {
            val hash = (lx * 7 + ly * 13 + timeStep * 3) % 11
            if (hash < 3) { // ~27% of cells gain leaves each drop
                leafCount[ly][lx] = (leafCount[ly][lx] + rate).coerceAtMost(MAX_LEAF)
            }
        }
    }

    /** Kick leaves at a position (player walks over). Clears leaf count. */
    fun kickLeaves(x: Int, y: Int) {
        val (lx, ly) = toLocal(x, y) ?: return
        leafCount[ly][lx] = 0f
    }
}
