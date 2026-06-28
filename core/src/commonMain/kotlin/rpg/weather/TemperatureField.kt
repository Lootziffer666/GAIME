package rpg.weather

import kotlin.math.sqrt

/**
 * On-demand temperature calculation from registered heat sources.
 * Not a stepping grid -- temperature at any point is computed from the
 * current set of heat sources and their distance falloff.
 *
 * Temperature range: -1.0 (extreme cold) to +1.0 (extreme heat).
 * Base temperature (no sources nearby) defaults to [baseTemp] (cold world).
 */
class TemperatureField(
    val baseTemp: Float = -0.5f,
) {
    data class HeatSource(
        val x: Float,
        val y: Float,
        val radius: Float,
        val strength: Float,
    )

    private val sources = mutableListOf<HeatSource>()

    /** Register a heat source (torch, campfire, lava). */
    fun addHeatSource(x: Float, y: Float, radius: Float, strength: Float) {
        sources.add(HeatSource(x, y, radius, strength))
    }

    /** Remove all heat sources (e.g. on scene change). */
    fun clearSources() {
        sources.clear()
    }

    /** Number of registered heat sources. */
    fun sourceCount(): Int = sources.size

    /**
     * Calculate temperature at (x, y). Each heat source contributes
     * based on distance: full strength at center, zero at radius.
     * Multiple sources are additive. Result is clamped to -1..+1.
     */
    fun tempAt(x: Float, y: Float): Float {
        var temp = baseTemp
        for (source in sources) {
            val dx = x - source.x
            val dy = y - source.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < source.radius) {
                val falloff = 1f - (dist / source.radius)
                temp += source.strength * falloff
            }
        }
        return temp.coerceIn(-1f, 1f)
    }
}
