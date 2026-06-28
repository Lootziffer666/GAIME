package rpg.weather

/**
 * 2D per-tile snow depth grid. Each cell stores a depth value in [0.0, 1.0].
 * Supports accumulation, melting, footprint stamping, and gradual refill.
 */
class SnowGrid(val width: Int, val height: Int) {

    private val data = FloatArray(width * height) { 0f }

    /**
     * Returns the snow depth at (x, y), or 0.0 if out of bounds.
     */
    operator fun get(x: Int, y: Int): Float {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0f
        return data[y * width + x]
    }

    /**
     * Sets the snow depth at (x, y), clamped to [0.0, 1.0]. No-op if out of bounds.
     */
    fun set(x: Int, y: Int, value: Float) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        data[y * width + x] = value.coerceIn(0f, 1f)
    }

    /**
     * Adds snow uniformly across the grid at [rate] * [dt], clamped to 1.0.
     * [windDx] and [windDy] are currently available for future directional bias.
     */
    fun accumulate(dt: Float, rate: Float, windDx: Float = 0f, windDy: Float = 0f) {
        val amount = rate * dt
        for (i in data.indices) {
            data[i] = (data[i] + amount).coerceAtMost(1f)
        }
    }

    /**
     * Reduces all snow depths by [rate] * [dt], clamped to 0.0.
     */
    fun melt(dt: Float, rate: Float) {
        val amount = rate * dt
        for (i in data.indices) {
            data[i] = (data[i] - amount).coerceAtLeast(0f)
        }
    }

    /**
     * Stamps a footprint at (x, y), setting that cell's depth to 0.
     */
    fun stampFootprint(x: Int, y: Int) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        data[y * width + x] = 0f
    }

    /**
     * Gradually restores stamped cells (cells at 0) toward the average of surrounding cells
     * or a base accumulation level, at the given [rate] per second.
     */
    fun refill(dt: Float, rate: Float) {
        val amount = rate * dt
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val current = data[idx]
                if (current < 1f) {
                    // Calculate surrounding average
                    val avg = surroundingAverage(x, y)
                    if (current < avg) {
                        data[idx] = (current + amount).coerceAtMost(avg)
                    }
                }
            }
        }
    }

    private fun surroundingAverage(x: Int, y: Int): Float {
        var sum = 0f
        var count = 0
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    sum += data[ny * width + nx]
                    count++
                }
            }
        }
        return if (count > 0) sum / count else 0f
    }
}
