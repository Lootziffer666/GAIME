package rpg.weather

/**
 * Fog simulation state: density and wind-driven drift.
 * Density controls visibility reduction. Drift offsets represent the
 * apparent movement of fog layers in the world, driven by [WindState].
 *
 * Engine-agnostic -- lives in :core for unit testing.
 */
class FogState {

    /** Fog density: 0.0 = clear, 1.0 = completely opaque. */
    var density: Float = 0f
        private set

    /** Accumulated drift offset X (for parallax/scroll effect). */
    var driftX: Float = 0f
        private set

    /** Accumulated drift offset Y (for parallax/scroll effect). */
    var driftY: Float = 0f
        private set

    /** Set fog density (clamped 0..1). */
    fun setDensity(d: Float) {
        density = d.coerceIn(0f, 1f)
    }

    /**
     * Advance drift based on wind direction and elapsed time.
     * Drift accumulates continuously (renderer uses modulo for wrap).
     */
    fun drift(dt: Float, windState: WindState) {
        driftX += windState.dx * windState.effectiveStrength * dt
        driftY += windState.dy * windState.effectiveStrength * dt
    }
}
