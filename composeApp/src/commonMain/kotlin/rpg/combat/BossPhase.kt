package rpg.combat

/**
 * The Rat Accountant's three phases, keyed to HP thresholds
 * (docs/VERTICAL_SLICE.md, Boss Phases table).
 */
enum class BossPhase {
    /** 100%-50%: throws papers, summons adds. */
    PHASE_1,

    /** 50%-25%: files objections -- pressure rises to HIGH, false markers spam. */
    PHASE_2,

    /** 25%-0%: throws its desk -- one big telegraphed attack. */
    PHASE_3;

    companion object {
        /** Phase implied by a boss HP fraction in [0f, 1f]. */
        fun forHpFraction(fraction: Float): BossPhase = when {
            fraction > 0.5f -> PHASE_1
            fraction > 0.25f -> PHASE_2
            else -> PHASE_3
        }
    }
}
