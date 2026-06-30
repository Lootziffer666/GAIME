package rpg.systems

import rpg.weather.FootprintGrid

/**
 * Owns a [FootprintGrid] and drives footprint stamping + fading.
 *
 * Stamps a fresh footprint at the player cell when the player moves.
 * Fades all footprints over time (natural decay + weather effects).
 */
class FootprintSystem(
    val grid: FootprintGrid,
    var isRaining: Boolean = false,
    var isWindy: Boolean = false,
) : WorldSystem {

    override val id: String = "footprint"

    private var lastPlayerX: Int = -1
    private var lastPlayerY: Int = -1

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        // Stamp if player moved to a new cell
        val px = ctx.playerCellX
        val py = ctx.playerCellY
        if ((px != lastPlayerX || py != lastPlayerY) && !ctx.isPlayerIdle) {
            grid.stamp(px, py)
            lastPlayerX = px
            lastPlayerY = py
        }

        // Fade all footprints
        grid.fade(dtSeconds, isRaining, isWindy)
    }
}
