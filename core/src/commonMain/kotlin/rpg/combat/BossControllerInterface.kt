package rpg.combat

/**
 * Common interface for boss-fight controllers. Both [BossController] (Rat Accountant)
 * and [TaxCollectorController] (Tax Collector Badger) implement this so CombatEngine
 * can drive any boss fight through a single parameter type.
 */
interface BossControllerInterface {
    val currentPhase: BossPhase

    /** Called once at combat start to set up adds/initial events. */
    fun onCombatStart(engine: CombatEngine, events: MutableList<CombatEvent>)

    /** Called each enemy turn; returns the damage the boss deals this tick. */
    fun takeTurn(boss: Combatant, events: MutableList<CombatEvent>): Int
}
