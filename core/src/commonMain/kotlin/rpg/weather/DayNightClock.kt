package rpg.weather

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tracks the time of day as a cyclic float (0..1).
 * 0.0 = midnight, 0.5 = noon, wraps back to 0.0 at 1.0.
 *
 * Provides derived values for ambient lighting (color tint, darkness level).
 * Pure calculation, no randomness, no engine dependencies.
 */
class DayNightClock(initialTime: Float = 0f) {

    /** Current time of day: 0.0 = midnight, 0.5 = noon. */
    var timeOfDay: Float = initialTime.mod(1f)
        private set

    /**
     * Advance time. [dt] is real seconds, [speed] controls how fast game-time
     * passes (e.g. speed=1/300 means 300 real seconds = one full game day).
     */
    fun advance(dt: Float, speed: Float = 1f / 300f) {
        timeOfDay = (timeOfDay + dt * speed).mod(1f)
    }

    /**
     * Darkness level: 0.0 at noon (brightest), peaks around 0.6 at midnight.
     * Uses a cosine curve centered on noon (0.5).
     */
    fun darkness(): Float {
        // cos curve: at timeOfDay=0.5 (noon) -> cos(0)=1 -> darkness=0
        // at timeOfDay=0.0 or 1.0 (midnight) -> cos(PI)=-1 -> darkness=0.6
        val angle = (timeOfDay - 0.5f) * 2f * PI.toFloat()
        return (1f - cos(angle)) * 0.3f // range 0..0.6
    }

    /**
     * Ambient color tint as (r, g, b) each in 0..1.
     * Daytime: warm white (1.0, 0.95, 0.85).
     * Nighttime: cool blue (0.4, 0.5, 0.8).
     * Interpolates smoothly between them.
     */
    fun ambientColor(): Triple<Float, Float, Float> {
        // sunFactor: 1 at noon, 0 at midnight
        val angle = (timeOfDay - 0.5f) * 2f * PI.toFloat()
        val sunFactor = (1f + cos(angle)) * 0.5f // 0..1

        val r = 0.4f + sunFactor * 0.6f   // 0.4 .. 1.0
        val g = 0.5f + sunFactor * 0.45f   // 0.5 .. 0.95
        val b = 0.8f - sunFactor * 0.0f + sunFactor * 0.05f // night=0.8, day=0.85
        // Simplified: night (0.4, 0.5, 0.8) day (1.0, 0.95, 0.85)
        val bFinal = 0.8f + sunFactor * 0.05f

        return Triple(r, g, bFinal)
    }

    /**
     * Moon intensity: 0.0 during daytime, peaks at ~0.7 at midnight.
     * Creates a cool silver glow effect at night that supplements
     * ambient darkness (stars + moon). Based on inverse sun factor.
     */
    fun moonIntensity(): Float {
        val angle = (timeOfDay - 0.5f) * 2f * PI.toFloat()
        val sunFactor = (1f + cos(angle)) * 0.5f // 1 at noon, 0 at midnight
        // Moon is inverse of sun, but not full brightness
        return (1f - sunFactor) * 0.7f
    }

    /**
     * Moon color tint as (r, g, b). Cool silver-blue.
     * Intensity is modulated by [moonIntensity].
     */
    fun moonColor(): Triple<Float, Float, Float> {
        val intensity = moonIntensity()
        // Silver-blue tint scaled by moon strength
        return Triple(0.6f * intensity, 0.7f * intensity, 0.9f * intensity)
    }
}
