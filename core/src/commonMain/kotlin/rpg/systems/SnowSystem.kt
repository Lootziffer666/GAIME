package rpg.systems

import rpg.weather.SnowGrid

/**
 * Owns a [SnowGrid] and drives snow accumulation/clearance per tick.
 *
 * When snowing: accumulate uniformly.
 * Player tramples snow at their current cell (clearAt).
 * When not snowing: slow regrow fills cleared tracks.
 */
class SnowSystem(
    val grid: SnowGrid,
    var isSnowing: Boolean = false,
    private val accumulationRate: Float = 0.01f,
    private val clearAmount: Float = 0.3f,
    private val regrowRate: Float = 0.002f,
) : WorldSystem {

    override val id: String = "snow"

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        if (isSnowing) {
            grid.accumulate(accumulationRate * dtSeconds)
        } else {
            grid.regrow(regrowRate * dtSeconds)
        }
        // Trample: clear snow at player cell
        if (!ctx.isPlayerIdle) {
            grid.clearAt(ctx.playerCellX, ctx.playerCellY, clearAmount * dtSeconds)
        }
    }
}
