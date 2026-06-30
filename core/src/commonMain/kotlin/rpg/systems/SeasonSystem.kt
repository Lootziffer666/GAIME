package rpg.systems

import rpg.weather.Season
import rpg.weather.SeasonalGrid

/**
 * Owns a [SeasonalGrid] and drives season-specific grid logic.
 *
 * - SPRING: regrow flowers, trample at player cell.
 * - SUMMER: unbend grass, bend at player cell.
 * - AUTUMN: drop leaves over time, kick at player cell.
 * - WINTER: no seasonal grid effect (handled by SnowSystem).
 */
class SeasonSystem(
    val grid: SeasonalGrid,
    var season: Season = Season.SPRING,
    private val flowerRegrowRate: Float = 0.002f,
    private val grassUnbendRate: Float = 0.01f,
    private val leafDropRate: Float = 0.003f,
) : WorldSystem {

    override val id: String = "season"

    private var timeAccumulator: Float = 0f

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        timeAccumulator += dtSeconds

        when (season) {
            Season.SPRING -> {
                grid.regrowFlowers(flowerRegrowRate * dtSeconds)
                if (!ctx.isPlayerIdle) {
                    grid.trampleFlower(ctx.playerCellX, ctx.playerCellY)
                }
            }
            Season.SUMMER -> {
                grid.unbendGrass(grassUnbendRate * dtSeconds)
                if (!ctx.isPlayerIdle) {
                    grid.bendGrass(ctx.playerCellX, ctx.playerCellY)
                }
            }
            Season.AUTUMN -> {
                grid.dropLeaves(leafDropRate * dtSeconds, timeStep = timeAccumulator.toInt())
                if (!ctx.isPlayerIdle) {
                    grid.kickLeaves(ctx.playerCellX, ctx.playerCellY)
                }
            }
            Season.WINTER -> {
                // Winter grid effects handled by SnowSystem; SeasonSystem is a no-op.
            }
        }
    }
}
