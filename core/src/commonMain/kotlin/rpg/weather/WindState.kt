package rpg.weather

import kotlin.math.cos
import kotlin.math.sin

/**
 * Wind state for the world weather system.
 * Direction is an angle in radians, strength ranges 0.0-1.0.
 * Direction drifts deterministically over time using a sine-based function.
 */
data class WindState(
    var direction: Float = 0f,
    var strength: Float = 0.5f,
    var gustiness: Float = 0.1f,
    private var elapsed: Float = 0f,
    private val driftSpeed: Float = 0.3f
) {
    /** Horizontal wind component: cos(direction) * strength */
    val dx: Float get() = cos(direction) * strength

    /** Vertical wind component: sin(direction) * strength */
    val dy: Float get() = sin(direction) * strength

    /**
     * Advances the wind state by [dt] seconds.
     * Direction drifts using a deterministic sine-based oscillation.
     */
    fun update(dt: Float) {
        elapsed += dt
        // Slow sine-based drift for smooth, deterministic direction changes
        direction += sin(elapsed * driftSpeed) * gustiness * dt
    }
}
