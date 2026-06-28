package rpg.weather

/**
 * Global wind state for the world. Affects rain direction, particle drift,
 * fire spread, scent/geruch transport, and sound propagation.
 *
 * Wind direction is a normalized 2D vector. Strength scales effects.
 */
class WindState {
    /** Wind direction X component (-1..1). Positive = blowing east. */
    var dx: Float = 0.2f

    /** Wind direction Y component (-1..1). Positive = blowing south. */
    var dy: Float = 0.1f

    /** Wind strength (0.0 = calm, 1.0 = storm). */
    var strength: Float = 0.3f

    /** Gusts: temporary strength peaks. */
    var gustTimer: Float = 0f
    private var gustStrength: Float = 0f

    /** Effective strength including gusts. */
    val effectiveStrength: Float get() = (strength + gustStrength).coerceAtMost(1f)

    /** Update wind (shift direction slowly, occasional gusts). */
    fun tick(dtSeconds: Float, time: Float) {
        // Slow direction drift
        dx += kotlin.math.sin(time * 0.1f) * 0.001f * dtSeconds
        dy += kotlin.math.cos(time * 0.13f) * 0.001f * dtSeconds
        dx = dx.coerceIn(-1f, 1f)
        dy = dy.coerceIn(-1f, 1f)

        // Gust logic
        if (gustTimer > 0f) {
            gustTimer -= dtSeconds
            if (gustTimer <= 0f) gustStrength = 0f
        }
    }

    /** Trigger a wind gust (lasts a few seconds). */
    fun gust(duration: Float = 2f, extraStrength: Float = 0.4f) {
        gustTimer = duration
        gustStrength = extraStrength
    }

    /** Angle in radians (for rain filter windAngle parameter). */
    val windAngle: Float get() = kotlin.math.atan2(dx, 1f) * effectiveStrength
}
