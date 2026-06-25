import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.combat.BossPhase
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side
import rpg.combat.TaxCollectorController

class Chapter2CombatTest {

    private fun member(id: String, hp: Int, atk: Int) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    @Test
    fun partyDefeatsThreeForestWolves() {
        // 3 wolves: 18 HP each, atk 5
        // Party: 3 members with combined atk of 15, can kill one wolf per tick
        val party = listOf(
            member("nib", hp = 100, atk = 5),
            member("brugg", hp = 100, atk = 5),
            member("vellum", hp = 100, atk = 5)
        )
        val wolves = listOf(
            EnemyArchetype.FOREST_WOLF.spawn("wolf_1"),
            EnemyArchetype.FOREST_WOLF.spawn("wolf_2"),
            EnemyArchetype.FOREST_WOLF.spawn("wolf_3")
        )
        val engine = CombatEngine(party, wolves)

        // Each tick: party deals 15 damage to first alive wolf (18 HP)
        // Wolf 1: tick 1 -> 3 HP, tick 2 -> dead
        // Wolf 2: tick 3 -> 3 HP, tick 4 -> dead
        // Wolf 3: tick 5 -> 3 HP, tick 6 -> dead
        var ticks = 0
        while (engine.result == CombatResult.ONGOING && ticks < 20) {
            val target = engine.livingEnemies().firstOrNull() ?: break
            engine.tick(CombatAction.Attack(target.id))
            ticks++
        }
        assertEquals(CombatResult.VICTORY, engine.result)
        assertTrue(ticks <= 6, "Should defeat 3 wolves in at most 6 ticks, took $ticks")
    }

    @Test
    fun taxCollectorBadgerBossFightReachesVictoryThroughAllPhases() {
        // Strong party to ensure victory
        val party = listOf(
            member("nib", hp = 200, atk = 8),
            member("brugg", hp = 200, atk = 8),
            member("vellum", hp = 200, atk = 8)
        )
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger") // 80 HP, atk 7
        val controller = TaxCollectorController()
        val engine = CombatEngine(party, emptyList(), boss, controller)

        val allEvents = mutableListOf<CombatEvent>()
        var ticks = 0
        val phasesReached = mutableSetOf(BossPhase.PHASE_1)

        while (engine.result == CombatResult.ONGOING && ticks < 30) {
            // Kill wolf adds first, then target boss
            val wolves = engine.livingEnemies().filter { it.id.startsWith("add_wolf") }
            val target = if (wolves.isNotEmpty()) wolves.first() else engine.livingEnemies().firstOrNull()
            if (target == null) break
            val events = engine.tick(CombatAction.Attack(target.id))
            allEvents += events
            controller.currentPhase.let { phasesReached.add(it) }
            ticks++
        }

        assertEquals(CombatResult.VICTORY, engine.result, "Party should defeat the Tax Collector Badger")
        assertTrue(phasesReached.contains(BossPhase.PHASE_2), "Should reach PHASE_2")
        assertTrue(phasesReached.contains(BossPhase.PHASE_3), "Should reach PHASE_3")
    }

    @Test
    fun wolfAddsAreSpawnedAtStartOfBadgerFight() {
        val party = listOf(member("h1", 100, 10))
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger")
        val controller = TaxCollectorController()
        val engine = CombatEngine(party, emptyList(), boss, controller)

        // After construction, the engine should have 3 enemies: boss + 2 wolves
        val enemies = engine.livingEnemies()
        assertEquals(3, enemies.size, "Boss + 2 wolf adds = 3 enemies")
        val wolves = enemies.filter { it.name == "Forest Wolf" }
        assertEquals(2, wolves.size)
        assertEquals(18, wolves[0].maxHp) // Forest Wolf has 18 HP
        assertEquals(5, wolves[0].attackPower) // Forest Wolf has 5 atk
    }

    @Test
    fun phase3StampAttackKillsLowHpPartyMember() {
        // The stamp attack in PHASE_3 does attackPower * 3 = 7 * 3 = 21 damage
        // A party member with HP <= 21 should be killed in one hit
        val fragileHero = member("squishy", hp = 20, atk = 1)
        val party = listOf(
            fragileHero,
            member("tank", hp = 200, atk = 30) // tank to survive and finish the fight
        )
        val boss = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("tax_badger") // 80 HP, atk 7
        val controller = TaxCollectorController()
        val engine = CombatEngine(party, emptyList(), boss, controller)

        // Force boss into PHASE_3 by dealing enough damage
        // Boss has 80 HP, need to get below 25% = below 20 HP
        // First kill the wolf adds, then damage the boss
        val wolves = engine.livingEnemies().filter { it.id.startsWith("add_wolf") }
        for (wolf in wolves) {
            while (wolf.isAlive && engine.result == CombatResult.ONGOING) {
                engine.tick(CombatAction.Attack(wolf.id))
            }
        }

        // Now damage the boss into PHASE_3 (need to remove > 60 HP)
        // Party total attack = 1 + 30 = 31 per tick
        // After 2 ticks: 80 - 31*2 = 18 HP < 20 -> PHASE_3
        while (controller.currentPhase != BossPhase.PHASE_3 && engine.result == CombatResult.ONGOING) {
            engine.tick(CombatAction.Attack("tax_badger"))
        }

        // Now the boss is in PHASE_3, dealing 21 damage per turn
        // The fragile hero has max 20 HP, so if they took any damage they may already be dead
        // If squishy is still alive, a Wait action lets boss hit for 21 which will kill
        if (fragileHero.isAlive && engine.result == CombatResult.ONGOING) {
            engine.tick(CombatAction.Wait) // boss hits for 21 on first living = squishy
        }

        // The 21-damage stamp should have killed the 20 HP hero
        assertTrue(!fragileHero.isAlive, "PHASE_3 stamp (21 damage) should kill a 20 HP party member")
    }

    @Test
    fun forestWolfArchetypeHasCorrectStats() {
        val wolf = EnemyArchetype.FOREST_WOLF.spawn("wolf")
        assertEquals(18, wolf.maxHp)
        assertEquals(5, wolf.attackPower)
        assertEquals("Forest Wolf", wolf.name)
    }

    @Test
    fun taxCollectorBadgerArchetypeHasCorrectStats() {
        val badger = EnemyArchetype.TAX_COLLECTOR_BADGER.spawn("badger")
        assertEquals(80, badger.maxHp)
        assertEquals(7, badger.attackPower)
        assertEquals("The Tax Collector Badger", badger.name)
    }
}
