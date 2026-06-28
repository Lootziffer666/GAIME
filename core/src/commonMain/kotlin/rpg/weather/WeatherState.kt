package rpg.weather

/**
 * Orchestrates all weather sub-systems: wind, snow accumulation, and blood fade.
 * Call [tick] each frame to advance the simulation.
 */
class WeatherState(
    val config: WeatherConfig,
    gridWidth: Int,
    gridHeight: Int
) {
    val wind: WindState = WindState(driftSpeed = config.windDriftSpeed)
    val snow: SnowGrid = SnowGrid(gridWidth, gridHeight)
    val blood: BloodGrid = BloodGrid(gridWidth, gridHeight)

    /**
     * Advances the weather simulation by [dt] seconds:
     * 1. Updates wind direction drift
     * 2. Accumulates snow (driven by wind)
     * 3. Refills footprints in snow
     * 4. Fades blood splatters
     */
    fun tick(dt: Float) {
        wind.update(dt)
        snow.accumulate(dt, config.snowRate, wind.dx, wind.dy)
        snow.refill(dt, config.footprintRefillRate)
        blood.fade(dt, config.bloodFadeRate)
    }
}
