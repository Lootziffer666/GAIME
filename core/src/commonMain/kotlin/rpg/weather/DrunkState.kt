package rpg.weather

/**
 * Tracks drunkenness and its gameplay effects. Engine-agnostic, unit-testable.
 *
 * Mechanics:
 * - [drunkLevel] 0.0–1.0 (sober→blackout)
 * - [stumbleChance]: probability of drifting one extra tile when moving (torkeln)
 * - [attackBonus]: drunk = stronger swings (flat bonus to attackPower)
 * - [delayedDamage]: HP loss that hasn't been "felt" yet (verzögerter Schmerz).
 *   Applied in bulk when sobering up.
 * - [sleepTimer]: idle too long while drunk → fall asleep → vulnerable to theft.
 *
 * The renderer (`:game`) reads these values; `:core` provides pure logic.
 */
class DrunkState {
    var drunkLevel: Float = 0f
        private set

    /** Accumulated damage not yet felt (pain is delayed while drunk). */
    var delayedDamage: Int = 0
        private set

    /** Seconds idle without input. When > SLEEP_THRESHOLD → asleep. */
    var idleSeconds: Float = 0f
        private set

    companion object {
        const val STUMBLE_THRESHOLD = 0.3f
        const val HEAVY_DRUNK = 0.6f
        const val BLACKOUT = 0.9f
        const val SLEEP_THRESHOLD = 8f  // seconds idle while drunk → sleep
        const val SOBER_RATE = 0.02f    // per second
    }

    // --- Derived properties ---

    /** Probability (0..1) of drifting one extra tile per move. */
    val stumbleChance: Float get() = when {
        drunkLevel < STUMBLE_THRESHOLD -> 0f
        drunkLevel < HEAVY_DRUNK -> (drunkLevel - STUMBLE_THRESHOLD) * 1.5f
        else -> 0.7f + (drunkLevel - HEAVY_DRUNK) * 0.5f
    }.coerceIn(0f, 0.9f)

    /** Flat attack power bonus from liquid courage. */
    val attackBonus: Int get() = when {
        drunkLevel < STUMBLE_THRESHOLD -> 0
        drunkLevel < HEAVY_DRUNK -> 2
        drunkLevel < BLACKOUT -> 4
        else -> 6
    }

    /** True if too drunk + idle too long → fell asleep. */
    val isAsleep: Boolean get() = drunkLevel > STUMBLE_THRESHOLD && idleSeconds > SLEEP_THRESHOLD

    /** True if heavily intoxicated (for visual effects, bark triggers). */
    val isHeavilyDrunk: Boolean get() = drunkLevel >= HEAVY_DRUNK

    // --- Actions ---

    /** Drink an ale. Each drink adds ~0.34 (3 drinks ≈ blackout). */
    fun drink(amount: Float = 0.34f) {
        drunkLevel = (drunkLevel + amount).coerceIn(0f, 1f)
        idleSeconds = 0f // drinking = activity
    }

    /** Sober up over time. Returns delayed damage that should now be applied. */
    fun soberTick(dtSeconds: Float): Int {
        if (drunkLevel <= 0f) return 0
        drunkLevel = (drunkLevel - SOBER_RATE * dtSeconds).coerceAtLeast(0f)

        // When sobering past the threshold, delayed damage hits all at once
        if (drunkLevel <= 0.01f && delayedDamage > 0) {
            val dmg = delayedDamage
            delayedDamage = 0
            return dmg // "Ow. What happened last night?"
        }
        return 0
    }

    /** Record damage that won't be felt until sober (verzögerter Schmerz). */
    fun delayPain(amount: Int) {
        if (drunkLevel >= STUMBLE_THRESHOLD) {
            delayedDamage += amount
        }
    }

    /** Tick idle timer (call per frame). Reset when player moves/acts. */
    fun tickIdle(dtSeconds: Float) {
        if (drunkLevel > STUMBLE_THRESHOLD) {
            idleSeconds += dtSeconds
        }
    }

    /** Reset idle timer (player did something). */
    fun resetIdle() {
        idleSeconds = 0f
    }

    /** Wake up (after being robbed or nudged). */
    fun wakeUp() {
        idleSeconds = 0f
    }

    /** How much gold gets stolen when asleep. Scales with drunk level. */
    fun goldStolenWhileAsleep(currentGold: Int): Int {
        if (!isAsleep) return 0
        return (currentGold * drunkLevel * 0.3f).toInt().coerceAtLeast(1)
    }

    /** Force fully sober (e.g. splash of cold water). */
    fun forceSober(): Int {
        drunkLevel = 0f
        idleSeconds = 0f
        val dmg = delayedDamage
        delayedDamage = 0
        return dmg
    }
}
