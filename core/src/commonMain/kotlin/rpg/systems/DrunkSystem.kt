package rpg.systems

import rpg.weather.DrunkState

/**
 * Owns a [DrunkState] and orchestrates all drunk-related gameplay logic:
 * - Sobering over time
 * - Delayed damage on sober-up
 * - Gold theft when asleep
 * - Idle tracking for sleep trigger
 *
 * Previously this logic was inline in WorldScene (595 lines of spaghetti).
 * Now it's in :core, fully testable, renderer-agnostic.
 */
class DrunkSystem(
    val state: DrunkState = DrunkState(),
) : WorldSystem {

    override val id: String = "drunk"

    /** Damage applied this tick (read by renderer for toast/feedback). */
    var lastSoberDamage: Int = 0
        private set

    /** Gold stolen this tick (read by renderer for toast/feedback). */
    var lastGoldStolen: Int = 0
        private set

    /** The drunk level for shader/visual binding. */
    val drunkLevel: Float get() = state.drunkLevel

    /** Stumble chance for movement modification. */
    val stumbleChance: Float get() = state.stumbleChance

    override fun tick(dtSeconds: Float, ctx: WorldContext) {
        lastSoberDamage = 0
        lastGoldStolen = 0

        if (state.drunkLevel <= 0f) return

        // Sober tick: returns delayed damage when sobering up
        val soberDmg = state.soberTick(dtSeconds)
        if (soberDmg > 0) {
            ctx.player.takeDamage(soberDmg)
            lastSoberDamage = soberDmg
        }

        // Idle tracking
        if (ctx.isPlayerIdle) {
            state.tickIdle(dtSeconds)
        }

        // Gold theft when asleep
        if (state.isAsleep) {
            val stolen = state.goldStolenWhileAsleep(ctx.inventory.gold)
            if (stolen > 0) {
                ctx.inventory.steal(stolen)
                state.wakeUp()
                lastGoldStolen = stolen
            }
        }
    }

    /** Call when player drinks (e.g. Barkeep interaction). */
    fun drink(amount: Float = 0.34f) {
        state.drink(amount)
    }

    /** Call when player performs any action (movement, interaction). */
    fun resetIdle() {
        state.resetIdle()
    }
}
