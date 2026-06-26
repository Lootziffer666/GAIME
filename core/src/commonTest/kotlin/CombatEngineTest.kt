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
import kotlin.random.Random

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

    @Test
    fun tauntsForNibReturnsNineEntries() {
        val party = listOf(member("nib", hp = 20, atk = 4))
        val engine = CombatEngine(party, emptyList(), random = Random(42))
        val taunts = engine.tauntsFor("nib")
        assertEquals(9, taunts.size)
        assertTrue(taunts.contains(BarkEvent.NIB_BACK_FOUL_CREATURE))
        assertTrue(taunts.contains(BarkEvent.NIB_DARKNESS_TAKE_YOU))
        assertTrue(taunts.contains(BarkEvent.NIB_DROP_YOUR_WEAPONS))
        assertTrue(taunts.contains(BarkEvent.NIB_IVE_FOUGHT_KOBOLDS_TOUGHER))
        assertTrue(taunts.contains(BarkEvent.NIB_IVE_FOUGHT_PUPPIES_TOUGHER))
        assertTrue(taunts.contains(BarkEvent.NIB_IVE_FOUGHT_ARTICHOKES_TOUGHER))
    }

    @Test
    fun victoriesForBruggReturnsThreeEntriesIncludingNewOnes() {
        val party = listOf(member("brugg", hp = 30, atk = 5))
        val engine = CombatEngine(party, emptyList(), random = Random(42))
        val victories = engine.victoriesFor("brugg")
        assertEquals(3, victories.size)
        assertTrue(victories.contains(BarkEvent.BRUGG_YOULL_NOT_BE_GETTING_UP))
        assertTrue(victories.contains(BarkEvent.BRUGG_NEXT_TIME_WONT_BE_SO_LUCKY))
        assertTrue(victories.contains(BarkEvent.BRUGG_SURRENDER_OR_DIE))
    }

    @Test
    fun enemyNearlyDeadBarkFiresWhenBelowTwentyFivePercent() {
        // Create an enemy with 20 max HP, currently at 4 HP (hpFraction = 0.2 < 0.25).
        // Party attack power = 1, so attack deals 1 damage leaving enemy at 3 HP still alive.
        // We need a seeded Random where:
        //  - first nextInt (taunt speaker pick) can be anything
        //  - first nextInt (taunt selection) can be anything
        //  - then in the attack branch: nextFloat() < 0.3f for the nearly-dead check
        //  - then nextInt (speaker pick for pickEnemyNearlyDeadBark)
        // We'll try multiple seeds to find one that works.
        val enemy = Combatant("foe", "Foe", maxHp = 20, side = Side.ENEMY, attackPower = 1)
        enemy.takeDamage(16) // now at 4 HP -> hpFraction = 0.2

        // Try seed 1: we need the third float drawn to be < 0.3
        val seed = findSeedForNearlyDeadBark()
        val party = listOf(member("nib", hp = 30, atk = 1))
        val engine = CombatEngine(party, listOf(enemy), random = Random(seed))
        val events = engine.tick(CombatAction.Attack("foe"))
        // After attack: enemy at 3 HP, hpFraction = 3/20 = 0.15 < 0.25
        assertTrue(enemy.isAlive, "Enemy should still be alive")
        val nearlyDeadBarks = listOf(
            BarkEvent.NIB_MY_NEXT_STRIKE_WILL_FELL_YOU,
            BarkEvent.BRUGG_MY_NEXT_STRIKE_WILL_FELL_YOU,
            BarkEvent.VELLUM_MY_NEXT_STRIKE_WILL_FELL_YOU
        )
        assertTrue(
            events.any { it is CombatEvent.BarkTriggered && it.bark in nearlyDeadBarks },
            "Should fire a MY_NEXT_STRIKE_WILL_FELL_YOU bark when enemy is below 25% HP"
        )
    }

    @Test
    fun weakAttackMockeryFiresOnLowDamage() {
        // Create a very weak enemy (atk=1) hitting a party member.
        // The party member has high HP so they survive easily.
        // We need a seeded Random where the weak-attack path triggers (25% chance).
        val seed = findSeedForWeakAttackMockery()
        val party = listOf(member("nib", hp = 30, atk = 100)) // will one-shot enemy if attack
        val weakEnemy = Combatant("weak", "Weak Foe", maxHp = 200, side = Side.ENEMY, attackPower = 1)
        val engine = CombatEngine(party, listOf(weakEnemy), random = Random(seed))

        // Wait action so enemy attacks and deals 1 damage (<=2) to nib
        val events = engine.tick(CombatAction.Wait)
        val mockeryBarks = listOf(
            BarkEvent.NIB_YOU_FIGHT_LIKE_A_NEWBORN,
            BarkEvent.NIB_YOU_FIGHT_LIKE_AN_INFANT,
            BarkEvent.NIB_YOU_FIGHT_LIKE_YOUR_MOTHER
        )
        assertTrue(
            events.any { it is CombatEvent.BarkTriggered && it.bark in mockeryBarks },
            "Should fire a YOU_FIGHT_LIKE mockery bark on low enemy damage"
        )
    }

    /**
     * Find a seed where the nearly-dead bark fires.
     * The Random sequence for a tick with Attack action on an enemy below 25%:
     * 1. nextInt(1) for taunt speaker pick (party size 1)
     * 2. nextInt(9) for taunt selection from nib's 9 taunts
     * 3. nextFloat() for the nearly-dead check (needs < 0.3)
     * 4. nextInt(1) for speaker pick in pickEnemyNearlyDeadBark
     * Then enemy turn: nextFloat() for emitBarkIfApplicable check (if enemy hits).
     */
    private fun findSeedForNearlyDeadBark(): Int {
        for (s in 0..1000) {
            val r = Random(s)
            r.nextInt(1) // taunt speaker
            r.nextInt(9) // taunt selection
            val f = r.nextFloat() // nearly-dead probability check
            if (f < 0.3f) return s
        }
        error("Could not find suitable seed")
    }

    /**
     * Find a seed where weak-attack mockery fires.
     * For a Wait action (no attack), the sequence is:
     * 1. nextInt(1) for taunt speaker pick (party size 1)
     * 2. nextInt(9) for taunt selection from nib's 9 taunts
     * Then enemy turn (strikeParty with damage 1, dealt <= 2, target alive):
     * 3. nextFloat() for the weak-attack check (needs < 0.25)
     * 4. nextInt(3) for mockery variant selection
     */
    private fun findSeedForWeakAttackMockery(): Int {
        for (s in 0..1000) {
            val r = Random(s)
            r.nextInt(1) // taunt speaker
            r.nextInt(9) // taunt selection
            val f = r.nextFloat() // weak-attack probability check
            if (f < 0.25f) return s
        }
        error("Could not find suitable seed")
    }
}
