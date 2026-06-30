package rpg.systems

import rpg.weather.WaterGrid

/**
 * Owns a [WaterGrid] and drives the water simulation per tick.
 *
 * When raining: adds rain → flow step → puddles form emergently.
 * When dry: evaporation reduces standing water.
 *
 * The renderer reads [grid] to draw puddles via GridOverlay.
 */
class WaterSystem(
    val grid: WaterGrid,
    var isRaining: Boolean = false,
    private val rainRate: Float = 0.005f,
    private val evaporationRate: Float = 0.001f,
) : WorldSystem {

    override val id: String = "water"

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        if (isRaining) {
            grid.addRain(rainRate * dtSeconds)
        } else {
            grid.evaporate(evaporationRate * dtSeconds)
        }
        grid.flowStep()
    }
}
