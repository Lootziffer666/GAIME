package rpg.systems

/**
 * The spine abstraction for GAIME's physics/world systems.
 *
 * Each system encapsulates one aspect of "Zeit wirkt auf Materie":
 * water flow, drunkenness, snow accumulation, material fatigue, etc.
 *
 * Systems live in :core (pure, testable). The renderer (:game) reads their
 * state via system-specific accessors and draws accordingly.
 */
interface WorldSystem {
    /** Unique identifier for this system (used in registry, logging, debug). */
    val id: String

    /** Advance the simulation by [dtSeconds]. Mutate internal state only. */
    fun tick(dtSeconds: Float, ctx: WorldContext)
}
