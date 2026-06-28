package rpg.weather

/**
 * Tracks wetness for an entity (player/NPC). Engine-agnostic, unit-testable.
 *
 * [wetness] ranges 0.0 (dry) to 1.0 (soaked). Affects movement (slippery),
 * fire interaction (resistant), and social visibility (dripping).
 */
class WetnessState {
    var wetness: Float = 0f
        private set

    companion object {
        const val WET_THRESHOLD = 0.05f
        const val SLIPPERY_THRESHOLD = 0.4f
    }

    val isWet: Boolean get() = wetness > WET_THRESHOLD
    val isSlippery: Boolean get() = wetness > SLIPPERY_THRESHOLD

    /** Get soaked (rain, puddle). Clamped to 1.0. */
    fun soak(amount: Float) {
        wetness = (wetness + amount).coerceIn(0f, 1f)
    }

    /** Dry off near a heat source. Clamped to 0.0. */
    fun dryNearHeat(rate: Float) {
        wetness = (wetness - rate).coerceIn(0f, 1f)
    }

    /** Reset to fully dry. */
    fun reset() { wetness = 0f }
}
