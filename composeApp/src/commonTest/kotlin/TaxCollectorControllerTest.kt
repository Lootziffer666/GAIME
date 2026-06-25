import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.combat.BossPhase
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.combat.TaxCollectorController

class TaxCollectorControllerTest {

    private fun member(id: String, hp: Int, atk: Int) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    @Test
    fun onCombatStartSummonsTwoForestWolfAdds() {
        val party = listOf(member("h1", 200, 10))
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")
        val controller = TaxCollectorController()
        val engine = CombatEngine(party, emptyList(), boss, controller)

        // After engine init, onCombatStart should have been called.
        // Boss (1) + 2 wolf adds = 3 living enemies
        val living = engine.livingEnemies()
        assertEquals(3, living.size, "Should have boss + 2 wolf adds")
        val wolves = living.filter { it.name == "Forest Wolf" }
        assertEquals(2, wolves.size, "Should have exactly 2 Forest Wolf adds")
    }

    @Test
    fun phaseTransitionsAtCorrectHpThresholds() {
        val controller = TaxCollectorController()
        assertEquals(BossPhase.PHASE_1, controller.currentPhase)

        // Create boss with 80 HP and damage it to trigger transitions
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")
        val events = mutableListOf<CombatEvent>()

        // Boss at full HP (80/80 = 1.0) -> still PHASE_1
        controller.takeTurn(boss, events)
        assertEquals(BossPhase.PHASE_1, controller.currentPhase)

        // Damage to 40/80 (0.5) -> boundary is > 0.5 for PHASE_1, so exactly 0.5 -> PHASE_2
        boss.takeDamage(40) // 80 -> 40, fraction = 0.5
        events.clear()
        controller.takeTurn(boss, events)
        assertEquals(BossPhase.PHASE_2, controller.currentPhase)

        // Damage to 20/80 (0.25) -> boundary is > 0.25 for PHASE_2, so exactly 0.25 -> PHASE_3
        boss.takeDamage(20) // 40 -> 20, fraction = 0.25
        events.clear()
        controller.takeTurn(boss, events)
        assertEquals(BossPhase.PHASE_3, controller.currentPhase)
    }

    @Test
    fun phase3DamageIsAttackPowerTimesThree() {
        val controller = TaxCollectorController()
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger") // atk = 7

        // Force into PHASE_3 by damaging boss below 25%
        boss.takeDamage(61) // 80 -> 19, fraction = 0.2375 < 0.25
        val events = mutableListOf<CombatEvent>()
        val damage = controller.takeTurn(boss, events)

        assertEquals(BossPhase.PHASE_3, controller.currentPhase)
        assertEquals(21, damage, "PHASE_3 damage should be attackPower(7) * 3 = 21")
    }

    @Test
    fun bossPhaseChangedEventsAreEmittedOnTransition() {
        val controller = TaxCollectorController()
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")
        val allEvents = mutableListOf<CombatEvent>()

        // Move from PHASE_1 to PHASE_2
        boss.takeDamage(40) // 0.5 fraction
        allEvents += controller.takeTurn(boss, mutableListOf<CombatEvent>().also { allEvents.addAll(it) }).let {
            val evts = mutableListOf<CombatEvent>()
            controller // already called above, re-trigger cleanly:
            evts
        }

        // Fresh start for clean event tracking
        val controller2 = TaxCollectorController()
        val boss2 = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger2")
        val events2 = mutableListOf<CombatEvent>()

        // Still in PHASE_1
        controller2.takeTurn(boss2, events2)
        assertTrue(events2.none { it is CombatEvent.BossPhaseChanged })

        // Transition to PHASE_2
        boss2.takeDamage(40)
        events2.clear()
        controller2.takeTurn(boss2, events2)
        assertTrue(events2.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_2 })

        // Transition to PHASE_3
        boss2.takeDamage(20)
        events2.clear()
        controller2.takeTurn(boss2, events2)
        assertTrue(events2.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_3 })
    }

    @Test
    fun phase2TransitionEmitsBossPhaseChangedEvent() {
        val controller = TaxCollectorController()
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")

        // Damage to trigger PHASE_2
        boss.takeDamage(41) // 80 -> 39, fraction ~0.49 -> PHASE_2
        val events = mutableListOf<CombatEvent>()
        controller.takeTurn(boss, events)

        assertEquals(BossPhase.PHASE_2, controller.currentPhase)
        assertTrue(
            events.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_2 },
            "PHASE_2 transition should emit BossPhaseChanged event"
        )
    }

    @Test
    fun phase3TransitionEmitsBalanceLifeDeathBark() {
        val controller = TaxCollectorController()
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")

        // Force directly into PHASE_3
        boss.takeDamage(61) // 80 -> 19, fraction < 0.25
        val events = mutableListOf<CombatEvent>()
        controller.takeTurn(boss, events)

        assertTrue(
            events.any { it is CombatEvent.BarkTriggered && it.bark == BarkEvent.VELLUM_BALANCE_LIFE_DEATH },
            "PHASE_3 transition should emit VELLUM_BALANCE_LIFE_DEATH bark"
        )
    }

    @Test
    fun phase1And2ReturnBaseAttackPower() {
        val controller = TaxCollectorController()
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger") // atk = 7

        // PHASE_1 damage
        val phase1Damage = controller.takeTurn(boss, mutableListOf())
        assertEquals(7, phase1Damage, "PHASE_1 damage should be base attackPower")

        // Move to PHASE_2
        boss.takeDamage(41) // fraction ~0.49
        val phase2Damage = controller.takeTurn(boss, mutableListOf())
        assertEquals(7, phase2Damage, "PHASE_2 damage should be base attackPower")
    }
}
