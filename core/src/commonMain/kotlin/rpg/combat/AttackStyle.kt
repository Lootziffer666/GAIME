package rpg.combat

/**
 * Determines how an enemy selects targets and paces its attacks.
 *
 * - [MELEE]: hits the first living party member every tick.
 * - [RANGED_SLOW]: picks a random living party member but only attacks every 2nd tick.
 */
enum class AttackStyle {
    MELEE,
    RANGED_SLOW
}
