package rpg.systems

import rpg.weather.BloodGrid

/**
 * Owns a [BloodGrid] and drives blood aging over time.
 *
 * Blood freshness decays each tick (drying out). The renderer reads
 * freshness to determine visual brightness (fresh=bright red, old=dark).
 */
class BloodSystem(
    val grid: BloodGrid,
    private val agingRate: Float = 0.05f,
) : WorldSystem {

    override val id: String = "blood"

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        grid.age(agingRate * dtSeconds)
    }

    /** Spill blood at a world position (called by combat or events). */
    fun spill(x: Int, y: Int, amount: Float = 0.8f) {
        grid.spill(x, y, amount)
    }
}
