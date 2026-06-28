package rpg.weather

import kotlin.math.min

/**
 * Tracks material stress/fatigue for tiles or objects.
 * Each entry accumulates stress over time or from events.
 * Stress levels determine visual crack appearance (rendered by MaterialFatigueOverlay).
 *
 * Thresholds:
 * - stress < 0.3: intact (no visible damage)
 * - 0.3 <= stress < 0.7: cracked (hairline cracks visible)
 * - stress >= 0.7: broken (major fractures, tile may collapse)
 *
 * Pure calculation, no engine dependencies, unit-testable.
 */
class MaterialFatigue(
    val width: Int,
    val height: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    companion object {
        const val CRACK_THRESHOLD = 0.3f
        const val BROKEN_THRESHOLD = 0.7f
        const val MAX_STRESS = 1.0f
    }

    private val stress = FloatArray(width * height)

    private fun index(x: Int, y: Int): Int {
        val lx = x - offsetX
        val ly = y - offsetY
        return if (lx in 0 until width && ly in 0 until height) ly * width + lx else -1
    }

    /** Returns current stress at (x,y) in world coordinates. 0 if out of bounds. */
    fun stressAt(x: Int, y: Int): Float {
        val i = index(x, y)
        return if (i >= 0) stress[i] else 0f
    }

    /** Adds [amount] of stress at (x,y). Capped at [MAX_STRESS]. */
    fun addStress(x: Int, y: Int, amount: Float) {
        val i = index(x, y)
        if (i >= 0) {
            stress[i] = min(stress[i] + amount, MAX_STRESS)
        }
    }

    /** Adds stress in a radius (Manhattan distance) around (cx,cy). */
    fun addStressRadius(cx: Int, cy: Int, radius: Int, amount: Float) {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (kotlin.math.abs(dx) + kotlin.math.abs(dy) <= radius) {
                    addStress(cx + dx, cy + dy, amount)
                }
            }
        }
    }

    /** Whether tile at (x,y) has visible cracks. */
    fun isCracked(x: Int, y: Int): Boolean = stressAt(x, y) >= CRACK_THRESHOLD

    /** Whether tile at (x,y) is broken (major damage). */
    fun isBroken(x: Int, y: Int): Boolean = stressAt(x, y) >= BROKEN_THRESHOLD

    /** Resets stress at (x,y) to 0 (repaired). */
    fun repair(x: Int, y: Int) {
        val i = index(x, y)
        if (i >= 0) stress[i] = 0f
    }

    /** Globally reduce all stress by [amount] (natural healing). Clamped at 0. */
    fun heal(amount: Float) {
        for (i in stress.indices) {
            stress[i] = (stress[i] - amount).coerceAtLeast(0f)
        }
    }
}
