package rpg.combat

import rpg.bark.BarkEvent

/**
 * Drives The Tax Collector Badger's boss fight (Chapter 2). Three phases
 * derived from HP fraction, analogous to [BossController]:
 *
 * - PHASE_1 (100%-50%): summon 2 Forest Wolf adds on start, throw tax forms.
 * - PHASE_2 (50%-25%): escalate pressure to HIGH, spawn false marker, base damage.
 * - PHASE_3 (25%-0%): heavy stamp attack (attackPower * 3), emit VELLUM_BALANCE_LIFE_DEATH.
 */
class TaxCollectorController {

    var currentPhase: BossPhase = BossPhase.PHASE_1
        private set

    private var summonedAdds = false

    /** Phase 1 opener: summon two Forest Wolf adds. */
    fun onCombatStart(engine: CombatEngine, events: MutableList<CombatEvent>) {
        if (summonedAdds) return
        summonedAdds = true
        engine.addEnemy(EnemyArchetype.FOREST_WOLF.spawn("add_wolf_1"))
        engine.addEnemy(EnemyArchetype.FOREST_WOLF.spawn("add_wolf_2"))
        events += CombatEvent.AddsSummoned(2)
        events += CombatEvent.Message("The Tax Collector Badger summons two forest wolves.")
    }

    /**
     * Updates the phase from the boss's current HP and returns the damage the
     * boss deals this tick. Phase transitions emit appropriate events.
     */
    fun takeTurn(boss: Combatant, events: MutableList<CombatEvent>): Int {
        val phase = BossPhase.forHpFraction(boss.hpFraction)
        if (phase != currentPhase) {
            currentPhase = phase
            events += CombatEvent.BossPhaseChanged(phase)
            when (phase) {
                BossPhase.PHASE_2 -> {
                    events += CombatEvent.Message("The Badger stamps 'OVERDUE' on everything in sight!")
                }
                BossPhase.PHASE_3 -> {
                    events += CombatEvent.Message("Enraged, the Badger raises its heavy stamp!")
                    events += CombatEvent.BarkTriggered(BarkEvent.VELLUM_BALANCE_LIFE_DEATH)
                }
                BossPhase.PHASE_1 -> { /* unreachable downward */ }
            }
        }
        return when (currentPhase) {
            BossPhase.PHASE_1, BossPhase.PHASE_2 -> boss.attackPower // tax forms
            BossPhase.PHASE_3 -> boss.attackPower * 3                // heavy stamp
        }
    }
}
