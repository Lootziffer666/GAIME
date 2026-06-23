import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.combat.BossController
import rpg.combat.BossPhase
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side

class CombatEngineTest {

    private fun member(id: String, hp: Int, atk: Int) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    @Test
    fun bossPhaseThresholdsMatchDocs() {
        assertEquals(BossPhase.PHASE_1, BossPhase.forHpFraction(0.6f))
        assertEquals(BossPhase.PHASE_2, BossPhase.forHpFraction(0.4f))
        assertEquals(BossPhase.PHASE_3, BossPhase.forHpFraction(0.2f))
    }

    @Test
    fun partyDefeatsASewerRat() {
        val party = listOf(member("hero", hp = 30, atk = 5))
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat") // 12 hp
        val engine = CombatEngine(party, listOf(rat))

        var result = CombatResult.ONGOING
        repeat(5) {
            if (result == CombatResult.ONGOING) result = engine.tick(CombatAction.Attack("rat")).let { engine.result }
        }
        assertEquals(CombatResult.VICTORY, engine.result)
    }

    @Test
    fun partyWipeEndsInDefeat() {
        val party = listOf(member("squishy", hp = 3, atk = 1))
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat") // atk 3
        val engine = CombatEngine(party, listOf(rat))
        engine.tick(CombatAction.Wait)
        assertEquals(CombatResult.DEFEAT, engine.result)
    }

    @Test
    fun dodgeAvoidsIncomingDamage() {
        val hero = member("hero", hp = 10, atk = 1)
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat")
        val engine = CombatEngine(listOf(hero), listOf(rat))
        engine.tick(CombatAction.Dodge)
        assertEquals(10, hero.hp)
    }

    @Test
    fun flameUtilityBarkBurnsPaperAdds() {
        val party = listOf(member("h1", 100, 4), member("h2", 100, 4), member("h3", 100, 4))
        val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant")
        val engine = CombatEngine(party, emptyList(), boss, BossController())
        // Boss start summons 2 paper adds; flame should burn them.
        val events = engine.tick(CombatAction.UtilityBark(BarkEvent.VELLUM_CALLS_FOR_FLAME))
        assertTrue(events.any { it is CombatEvent.BarkTriggered })
        val livingNonBoss = engine.livingEnemies().filter { it !== boss }
        assertTrue(livingNonBoss.isEmpty(), "paper adds should be burned")
    }

    @Test
    fun bossProgressesThroughPhasesAndFiresDeskBark() {
        val party = listOf(member("h1", 200, 4), member("h2", 200, 4), member("h3", 200, 4)) // total atk 12
        val boss = EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant") // 60 hp
        val engine = CombatEngine(party, emptyList(), boss, BossController())

        val allEvents = mutableListOf<CombatEvent>()
        // First clear the paper adds so only the boss whittles down predictably.
        allEvents += engine.tick(CombatAction.UtilityBark(BarkEvent.VELLUM_CALLS_FOR_FLAME))
        // 60 -> 48 -> 36 -> 24(PHASE_2) -> 12(PHASE_3) -> 0
        repeat(5) { allEvents += engine.tick(CombatAction.Attack("rat_accountant")) }

        assertTrue(allEvents.any { it is CombatEvent.AddsSummoned })
        assertTrue(allEvents.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_2 })
        assertTrue(allEvents.any { it is CombatEvent.BossPhaseChanged && it.phase == BossPhase.PHASE_3 })
        assertTrue(allEvents.any { it is CombatEvent.BarkTriggered && it.bark == BarkEvent.NIB_SMELL_TREASURE })
        assertEquals(CombatResult.VICTORY, engine.result)
    }
}
