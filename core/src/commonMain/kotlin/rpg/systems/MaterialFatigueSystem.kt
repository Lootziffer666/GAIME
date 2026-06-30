package rpg.systems

import rpg.weather.MaterialFatigue

/**
 * Owns a [MaterialFatigue] grid and drives stress accumulation over time.
 *
 * Stress slowly accumulates on all cells (environmental wear). The heal
 * rate counters this slightly, representing natural material recovery.
 */
class MaterialFatigueSystem(
    val grid: MaterialFatigue,
    private val stressRate: Float = 0.001f,
    private val healRate: Float = 0.0002f,
) : WorldSystem {

    override val id: String = "material_fatigue"

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        // Accumulate stress uniformly (time wears on material)
        val increment = stressRate * dtSeconds
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val wx = x + grid.offsetX
                val wy = y + grid.offsetY
                grid.addStress(wx, wy, increment)
            }
        }
        // Slight natural healing
        grid.heal(healRate * dtSeconds)
    }

    /** Manually add stress at a position (from events/combat). */
    fun impact(x: Int, y: Int, amount: Float) {
        grid.addStress(x, y, amount)
    }

    /** Add stress in a radius (from explosions, heavy impacts). */
    fun impactRadius(cx: Int, cy: Int, radius: Int, amount: Float) {
        grid.addStressRadius(cx, cy, radius, amount)
    }
}
