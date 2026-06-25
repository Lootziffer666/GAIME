import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side

/**
 * Integration tests verifying that combat bark events fire at the correct moments:
 * taunts on combat start, damage reactions, death barks, victory barks, and healing barks.
 */
class CombatBarkIntegrationTest {

    private fun member(id: String, hp: Int, atk: Int) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    // --- Taunt on combat start ---

    @Test
    fun firstTickFiresTauntBark() {
        val party = listOf(member("nib", hp = 50, atk = 20))
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat")
        val engine = CombatEngine(party, listOf(rat), random = Random(42))

        val events = engine.tick(CombatAction.Wait)

        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        assertTrue(barks.isNotEmpty(), "First tick should fire a taunt bark")
        // The taunt should be from one of Nib's taunts
        val nibTaunts = listOf(
            BarkEvent.NIB_FROM_THE_SHADOWS,
            BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT,
            BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK
        )
        assertTrue(barks.any { it in nibTaunts }, "Taunt should be from nib's taunt set: got $barks")
    }

    @Test
    fun secondTickDoesNotFireTauntAgain() {
        val party = listOf(member("brugg", hp = 50, atk = 20))
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat")
        val engine = CombatEngine(party, listOf(rat), random = Random(42))

        engine.tick(CombatAction.Wait) // first tick: taunt fires
        val events2 = engine.tick(CombatAction.Wait) // second tick: no taunt

        val barks = events2.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        val allTaunts = listOf(
            BarkEvent.NIB_FROM_THE_SHADOWS, BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT,
            BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK,
            BarkEvent.BRUGG_HAVE_AT_THEE, BarkEvent.BRUGG_SURRENDER_OR_DIE,
            BarkEvent.BRUGG_SHOW_YOURSELVES,
            BarkEvent.VELLUM_LETS_SEE_IF_YOU_CAN_DODGE, BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE,
            BarkEvent.VELLUM_I_SMITE_YOU
        )
        assertTrue(barks.none { it in allTaunts }, "Second tick should not fire another taunt")
    }

    // --- Damage reaction barks ---

    @Test
    fun partyDamageCanTriggerDamageReactionBark() {
        // Use a seeded random that guarantees the 40% probability triggers
        // Random(0) gives specific float sequences -- we try multiple seeds
        val party = listOf(member("nib", hp = 50, atk = 1))
        val enemy = Combatant("big_enemy", "Big Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 5)

        // Run with multiple seeds until we find one that fires a damage bark
        var foundDamageBark = false
        for (seed in 0..20) {
            val testParty = listOf(member("nib", hp = 50, atk = 1))
            val testEnemy = Combatant("big_enemy", "Big Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 5)
            val engine = CombatEngine(testParty, listOf(testEnemy), random = Random(seed))

            // Multiple ticks to allow enemy to hit us
            val allEvents = mutableListOf<CombatEvent>()
            repeat(5) {
                if (engine.result == CombatResult.ONGOING) {
                    allEvents += engine.tick(CombatAction.Wait)
                }
            }

            val damageBarks = listOf(
                BarkEvent.NIB_THAT_STINGS, BarkEvent.NIB_LUCKY_HIT,
                BarkEvent.BRUGG_THATS_GOING_TO_LEAVE_A_MARK,
                BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT, BarkEvent.BRUGG_THAT_DREW_BLOOD,
                BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT, BarkEvent.VELLUM_I_NEED_A_HEALER
            )
            val triggered = allEvents.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
            if (triggered.any { it in damageBarks }) {
                foundDamageBark = true
                break
            }
        }
        assertTrue(foundDamageBark, "At least one seed should trigger a damage reaction bark within 5 ticks")
    }

    // --- Death bark ---

    @Test
    fun partyMemberDeathTriggersDeathBark() {
        // Low-HP party member that will die in one hit
        val party = listOf(member("vellum", hp = 3, atk = 1))
        val enemy = Combatant("big_enemy", "Big Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 10)
        val engine = CombatEngine(party, listOf(enemy), random = Random(1))

        val events = engine.tick(CombatAction.Wait)

        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        assertTrue(
            BarkEvent.VELLUM_I_DIDNT_THINK_IT_WOULD_END in barks,
            "Vellum's death should trigger death bark: got $barks"
        )
    }

    @Test
    fun nibDeathTriggersAvengeMeBark() {
        val party = listOf(member("nib", hp = 3, atk = 1))
        val enemy = Combatant("big_enemy", "Big Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 10)
        val engine = CombatEngine(party, listOf(enemy), random = Random(1))

        val events = engine.tick(CombatAction.Wait)

        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        assertTrue(
            BarkEvent.NIB_AVENGE_ME in barks,
            "Nib's death should trigger AVENGE_ME bark: got $barks"
        )
    }

    // --- Victory bark ---

    @Test
    fun victoryTriggersVictoryBark() {
        val party = listOf(member("brugg", hp = 50, atk = 20))
        // Rat with low HP that will die in one hit
        val rat = Combatant("weak_rat", "Weak Rat", maxHp = 5, side = Side.ENEMY, attackPower = 1)
        val engine = CombatEngine(party, listOf(rat), random = Random(7))

        val events = engine.tick(CombatAction.Attack("weak_rat"))

        assertEquals(CombatResult.VICTORY, engine.result)
        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        val victoryBarks = listOf(
            BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT,
            BarkEvent.BRUGG_SURRENDER_OR_DIE,
            BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE
        )
        assertTrue(
            barks.any { it in victoryBarks },
            "Victory should trigger a victory bark: got $barks"
        )
    }

    // --- Heal action ---

    @Test
    fun healActionRestoresHpAndTriggersHealingBark() {
        val nib = member("nib", hp = 20, atk = 5)
        val brugg = member("brugg", hp = 30, atk = 5)
        val enemy = Combatant("enemy", "Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 5)
        val engine = CombatEngine(listOf(nib, brugg), listOf(enemy), random = Random(42))

        // First damage nib
        nib.takeDamage(10) // nib now at 10/20
        assertEquals(10, nib.hp)

        val events = engine.tick(CombatAction.Heal)

        // Nib is lowest HP (10) so should be healed
        assertTrue(nib.hp > 10, "Nib should be healed: hp=${nib.hp}")
        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        assertTrue(
            BarkEvent.NIB_GOOD_AS_NEW in barks,
            "Healing nib should trigger GOOD_AS_NEW bark: got $barks"
        )
    }

    @Test
    fun healActionHealsLowestHpPartyMember() {
        val nib = member("nib", hp = 20, atk = 5)
        val brugg = member("brugg", hp = 30, atk = 5)
        val vellum = member("vellum", hp = 18, atk = 4)
        val enemy = Combatant("enemy", "Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 1)
        val engine = CombatEngine(listOf(nib, brugg, vellum), listOf(enemy), random = Random(42))

        // Damage vellum the most
        vellum.takeDamage(15) // vellum at 3/18
        nib.takeDamage(5)     // nib at 15/20
        brugg.takeDamage(5)   // brugg at 25/30

        val events = engine.tick(CombatAction.Heal)

        // Vellum should be healed (lowest at 3 HP)
        assertTrue(vellum.hp > 3, "Vellum (lowest HP) should be healed: hp=${vellum.hp}")
        val barks = events.filterIsInstance<CombatEvent.BarkTriggered>().map { it.bark }
        assertTrue(
            BarkEvent.VELLUM_IM_BACK_ON_MY_FEET in barks,
            "Healing vellum should trigger IM_BACK_ON_MY_FEET bark: got $barks"
        )
    }

    @Test
    fun healDoesNotExceedMaxHp() {
        val nib = member("nib", hp = 20, atk = 5)
        val enemy = Combatant("enemy", "Enemy", maxHp = 200, side = Side.ENEMY, attackPower = 1)
        val engine = CombatEngine(listOf(nib), listOf(enemy), random = Random(42))

        // Damage nib by only 2
        nib.takeDamage(2) // nib at 18/20

        engine.tick(CombatAction.Heal)

        // After heal (+8 capped at maxHp -> 20) then enemy hits for 1 -> 19
        // The key assertion: HP should not exceed maxHp
        assertTrue(nib.hp <= nib.maxHp, "Heal should not exceed maxHp: hp=${nib.hp}, max=${nib.maxHp}")
    }

    // --- Combatant.heal() unit tests ---

    @Test
    fun combatantHealCapsAtMaxHp() {
        val c = member("nib", hp = 20, atk = 1)
        c.takeDamage(5) // 15/20
        val healed = c.heal(100)
        assertEquals(5, healed)
        assertEquals(20, c.hp)
    }

    @Test
    fun combatantHealReturnsActualAmountHealed() {
        val c = member("nib", hp = 20, atk = 1)
        c.takeDamage(3) // 17/20
        val healed = c.heal(8)
        assertEquals(3, healed)
        assertEquals(20, c.hp)
    }

    @Test
    fun combatantHealDoesNothingWhenDead() {
        val c = member("nib", hp = 20, atk = 1)
        c.kill()
        val healed = c.heal(10)
        assertEquals(0, healed)
        assertEquals(0, c.hp)
    }

    @Test
    fun combatantHealIgnoresNegativeAmount() {
        val c = member("nib", hp = 20, atk = 1)
        c.takeDamage(5)
        val healed = c.heal(-5)
        assertEquals(0, healed)
        assertEquals(15, c.hp)
    }
}
