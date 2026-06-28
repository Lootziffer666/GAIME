package rpg.weather

import kotlin.math.abs

/**
 * 2D per-tile blood level grid. Each cell stores a blood level in [0.0, 1.0].
 * Supports splattering with a radius, fading over time, and clearing.
 */
class BloodGrid(val width: Int, val height: Int) {

    private val data = FloatArray(width * height) { 0f }

    /**
     * Returns the blood level at (x, y), or 0.0 if out of bounds.
     */
    operator fun get(x: Int, y: Int): Float {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0f
        return data[y * width + x]
    }

    /**
     * Splatters blood at (x, y) with given [intensity] within Manhattan distance [radius].
     * Intensity is clamped to [0.0, 1.0].
     */
    fun splatter(x: Int, y: Int, intensity: Float, radius: Int) {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (abs(dx) + abs(dy) <= radius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        data[ny * width + nx] = clampedIntensity
                    }
                }
            }
        }
    }

    /**
     * Reduces all blood levels by [rate] * [dt], clamped to 0.0.
     */
    fun fade(dt: Float, rate: Float) {
        val amount = rate * dt
        for (i in data.indices) {
            data[i] = (data[i] - amount).coerceAtLeast(0f)
        }
    }

    /**
     * Resets the entire grid to 0.
     */
    fun clear() {
        data.fill(0f)
    }
}
