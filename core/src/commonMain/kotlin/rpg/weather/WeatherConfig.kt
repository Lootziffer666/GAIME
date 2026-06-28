package rpg.weather

/**
 * Configuration parameters for the weather simulation.
 */
data class WeatherConfig(
    val snowRate: Float = 0.1f,
    val meltRate: Float = 0.05f,
    val bloodFadeRate: Float = 0.02f,
    val footprintRefillRate: Float = 0.08f,
    val windDriftSpeed: Float = 0.3f
)
