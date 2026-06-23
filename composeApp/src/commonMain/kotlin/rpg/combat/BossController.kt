package rpg.combat

import rpg.bark.BarkEvent

/**
 * Drives The Rat Accountant's bureaucratic boss fight. Phases are derived from
 * the boss's HP fraction; transitions surface [CombatEvent]s the SliceDirector
 * reacts to (e.g. raising Quest Pressure to HIGH in Phase 2).
 */
class BossController {

    var currentPhase: BossPhase = BossPhase.PHASE_1
        private set

    private var summonedAdds = false

    /** Phase 1 opener: summon two flammable paper-rat adds. */
    fun onCombatStart(engine: CombatEngine, events: MutableList<CombatEvent>) {
        if (summonedAdds) return
        summonedAdds = true
        engine.addEnemy(EnemyArchetype.SEWER_RAT.spawn("add_paper_1", isPaperAdd = true))
        engine.addEnemy(EnemyArchetype.SEWER_RAT.spawn("add_paper_2", isPaperAdd = true))
        events += CombatEvent.AddsSummoned(2)
        events += CombatEvent.Message("The Rat Accountant files two paper underlings.")
    }

    /**
     * Updates the phase from the boss's current HP and returns the damage the
     * boss deals this tick. Phase 3's desk throw hits much harder (one big
     * telegraphed attack that a dodge fully avoids).
     */
    fun takeTurn(boss: Combatant, events: MutableList<CombatEvent>): Int {
        val phase = BossPhase.forHpFraction(boss.hpFraction)
        if (phase != currentPhase) {
            currentPhase = phase
            events += CombatEvent.BossPhaseChanged(phase)
            when (phase) {
                BossPhase.PHASE_2 ->
                    events += CombatEvent.Message("The Accountant begins filing objections.")
                BossPhase.PHASE_3 -> {
                    events += CombatEvent.Message("Desperate, it hurls its desk!")
                    events += CombatEvent.BarkTriggered(BarkEvent.NIB_SMELL_TREASURE)
                }
                BossPhase.PHASE_1 -> { /* unreachable downward */ }
            }
        }
        return when (currentPhase) {
            BossPhase.PHASE_1, BossPhase.PHASE_2 -> boss.attackPower // thrown papers
            BossPhase.PHASE_3 -> boss.attackPower * 3                // desk throw
        }
    }
}
