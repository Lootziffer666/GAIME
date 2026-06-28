package rpg.weather

/**
 * Tracks blood spills on the world grid. Each cell holds a blood amount and
 * freshness value. Fresh blood is bright red; as it ages the freshness decays,
 * representing the visual transition from fresh to dark dried blood.
 *
 * Engine-agnostic -- lives in :core for unit testing.
 */
class BloodGrid(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    private val amount = Array(height) { FloatArray(width) { 0f } }
    private val freshness = Array(height) { FloatArray(width) { 0f } }

    /** Spill fresh blood at (x, y). Sets amount and marks as fully fresh. */
    fun spill(x: Int, y: Int, bloodAmount: Float) {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return
        amount[ny][nx] = (amount[ny][nx] + bloodAmount).coerceAtMost(1f)
        freshness[ny][nx] = 1.0f
    }

    /** Age all blood: freshness decays by [rate]. Does not reduce amount. */
    fun age(rate: Float) {
        for (y in 0 until height) for (x in 0 until width) {
            if (freshness[y][x] > 0f) {
                freshness[y][x] = (freshness[y][x] - rate).coerceAtLeast(0f)
            }
        }
    }

    /** Blood amount at (x, y). */
    fun amountAt(x: Int, y: Int): Float {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return 0f
        return amount[ny][nx]
    }

    /** Freshness at (x, y): 1.0 = just spilled, 0.0 = fully dried. */
    fun freshnessAt(x: Int, y: Int): Float {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return 0f
        return freshness[ny][nx]
    }

    /** True if blood at (x, y) is still considered fresh (freshness > 0.5). */
    fun isFresh(x: Int, y: Int): Boolean = freshnessAt(x, y) > 0.5f
}
