package rpg.weather

/**
 * Tracks accumulated dirt/grime on an entity from world contact.
 * Not a binary flag — a layered accumulation of material traces.
 *
 * The world recognizes you by your surface, not by flags.
 */
class DirtState {
    /** Mud from swamps/puddles. Makes footsteps visible and movement slippery. */
    var mud: Float = 0f; private set

    /** Dust from ruins/desert. Makes you visible in bright light. */
    var dust: Float = 0f; private set

    /** Soot from fire/explosions. Provides minor cold resistance, but NPCs suspect you. */
    var soot: Float = 0f; private set

    /** Pollen from forests/meadows. Attracts insects and certain creatures. */
    var pollen: Float = 0f; private set

    /** Ash from burned areas. Marks your passage. */
    var ash: Float = 0f; private set

    /** Total visible dirtiness (for NPC reactions, shader intensity). */
    val totalDirt: Float get() = (mud + dust + soot + pollen + ash).coerceAtMost(1f)

    /** True if visibly dirty enough for NPCs to react. */
    val isVisiblyDirty: Boolean get() = totalDirt > 0.3f

    // --- Accumulation (from world contact) ---

    fun addMud(amount: Float) { mud = (mud + amount).coerceAtMost(1f) }
    fun addDust(amount: Float) { dust = (dust + amount).coerceAtMost(1f) }
    fun addSoot(amount: Float) { soot = (soot + amount).coerceAtMost(1f) }
    fun addPollen(amount: Float) { pollen = (pollen + amount).coerceAtMost(1f) }
    fun addAsh(amount: Float) { ash = (ash + amount).coerceAtMost(1f) }

    // --- Removal (rain, water, cleaning) ---

    /** Rain washes surface dirt slowly. */
    fun rainWash(dtSeconds: Float) {
        val rate = 0.01f * dtSeconds
        mud = (mud - rate * 2f).coerceAtLeast(0f)     // rain washes mud well
        dust = (dust - rate * 1.5f).coerceAtLeast(0f) // dust dissolves
        soot = (soot - rate * 0.5f).coerceAtLeast(0f) // soot is stubborn
        pollen = (pollen - rate * 2f).coerceAtLeast(0f) // pollen washes easily
        ash = (ash - rate).coerceAtLeast(0f)
    }

    /** Submerge in water (fast clean, but now wet). */
    fun waterClean() {
        mud = 0f; dust = 0f; pollen = 0f; ash = (ash * 0.3f)
        // Soot needs scrubbing, not just water
    }

    /** Full scrub/bath. */
    fun fullClean() {
        mud = 0f; dust = 0f; soot = 0f; pollen = 0f; ash = 0f
    }
}
