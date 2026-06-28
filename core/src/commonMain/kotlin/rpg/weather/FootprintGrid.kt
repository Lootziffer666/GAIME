package rpg.weather

/**
 * Tracks footprints left by entities on the world grid.
 * Each cell holds a footprint intensity (0.0 = none, 1.0 = fresh).
 * Footprints fade over time and are affected by weather (rain washes, wind covers in sand).
 *
 * The renderer reads this grid to overlay imprint marks on tiles.
 * Different surfaces show footprints differently (mud=deep, snow=crisp, sand=soft).
 */
class FootprintGrid(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    private val grid = Array(height) { FloatArray(width) { 0f } }

    companion object {
        const val FADE_RATE = 0.005f   // per second (natural fade)
        const val RAIN_FADE = 0.02f    // per second during rain
        const val WIND_FADE = 0.01f    // per second (sand/dust blown over)
    }

    /** Stamp a fresh footprint at (x,y). Intensity 1.0 = just stepped. */
    fun stamp(x: Int, y: Int) {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return
        grid[ny][nx] = 1.0f
    }

    /** Get footprint intensity at grid position. */
    operator fun get(x: Int, y: Int): Float {
        val nx = x - offsetX; val ny = y - offsetY
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return 0f
        return grid[ny][nx]
    }

    /** Fade all footprints (natural decay). Call per frame with dtSeconds. */
    fun fade(dtSeconds: Float, isRaining: Boolean = false, isWindy: Boolean = false) {
        val rate = FADE_RATE + (if (isRaining) RAIN_FADE else 0f) + (if (isWindy) WIND_FADE else 0f)
        for (y in 0 until height) for (x in 0 until width) {
            if (grid[y][x] > 0f) {
                grid[y][x] = (grid[y][x] - rate * dtSeconds).coerceAtLeast(0f)
            }
        }
    }

    /** True if there's a visible footprint at this position. */
    fun hasFootprint(x: Int, y: Int): Boolean = this[x, y] > 0.05f

    /** Count active footprints (for tests/debugging). */
    fun activeCount(): Int {
        var count = 0
        for (y in 0 until height) for (x in 0 until width) {
            if (grid[y][x] > 0.05f) count++
        }
        return count
    }
}
