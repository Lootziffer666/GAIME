package rpg.combat

import rpg.bark.BarkEvent

/** A player's action for one combat tick. */
sealed class CombatAction {
    /** Strike a specific enemy with the party's combined attack power. */
    data class Attack(val targetId: String) : CombatAction()

    /** Brace: avoid all enemy damage this tick (the dodge window). */
    data object Dodge : CombatAction()

    /** Spend a utility bark in combat (e.g. flame to burn paper adds). */
    data class UtilityBark(val bark: BarkEvent) : CombatAction()

    /** Do nothing this tick. */
    data object Wait : CombatAction()

    /** Heal the lowest-HP living party member. */
    data object Heal : CombatAction()
}
