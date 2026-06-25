package rpg.combat

/** Which side of a fight a combatant is on. */
enum class Side { PLAYER, ENEMY }

/**
 * A single participant in combat. Party members and enemies share this type.
 * State is mutable but only mutated through the engine, keeping combat logic
 * deterministic and testable.
 */
class Combatant(
    val id: String,
    val name: String,
    val maxHp: Int,
    val side: Side,
    val attackPower: Int,
    /** Flammable "paper" add summoned by the boss; cleared by a flame utility bark. */
    val isPaperAdd: Boolean = false
) {
    var hp: Int = maxHp
        private set

    /** A one-shot shield that absorbs the next hit. */
    var shielded: Boolean = false

    val isAlive: Boolean get() = hp > 0

    /** Fraction of max HP remaining, in [0f, 1f]. */
    val hpFraction: Float get() = if (maxHp == 0) 0f else hp.toFloat() / maxHp

    /** Applies [amount] damage, honouring a shield. Returns damage actually dealt. */
    fun takeDamage(amount: Int): Int {
        if (amount <= 0 || !isAlive) return 0
        if (shielded) {
            shielded = false
            return 0
        }
        val dealt = minOf(amount, hp)
        hp -= dealt
        return dealt
    }

    /** Restores [amount] HP, capped at [maxHp]. Returns the amount actually healed. */
    fun heal(amount: Int): Int {
        if (amount <= 0 || !isAlive) return 0
        val healed = minOf(amount, maxHp - hp)
        hp += healed
        return healed
    }

    /** Instantly removes this combatant (e.g. burned by flame). */
    fun kill() {
        hp = 0
    }
}
