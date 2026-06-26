import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.bark.BarkEventBus
import rpg.bark.BarkFireResult
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.Combatant
import rpg.combat.Side
import rpg.questbook.RoomContext

/**
 * Tests for the combat-origin cooldown bypass (review issues #1 and #2).
 *
 * Combat barks are driven by deterministic game state and the engine may
 * legitimately reuse a key within the cooldown window (e.g. a taunt key that
 * also appears in the victory set). Routing them with `bypassCooldown = true`
 * guarantees story-critical lines are never silently swallowed by the bus.
 */
class BarkCooldownBypassTest {

    private fun playerParty(vararg members: Combatant) = members.toList()

    // --- BarkEventBus level ---

    @Test
    fun busSuppressesRepeatedKeyWithoutBypass() {
        var now = 0L
        val bus = BarkEventBus { now }
        assertTrue(bus.fire(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT) is BarkFireResult.Emitted)
        val second = bus.fire(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT)
        assertTrue(second is BarkFireResult.OnCooldown, "second fire within cooldown should be suppressed")
    }

    @Test
    fun busBypassEmitsRepeatedKeyWithinCooldown() {
        var now = 0L
        val bus = BarkEventBus { now }
        assertTrue(bus.fire(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT) is BarkFireResult.Emitted)
        // Same key, no time elapsed, but combat-origin -> must still emit.
        val second = bus.fire(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT, bypassCooldown = true)
        assertTrue(second is BarkFireResult.Emitted, "bypass should emit within cooldown window")
    }

    // --- SliceDirector level ---

    @Test
    fun directorBypassFiresEvenWhenOnCooldown() {
        var now = 0L
        val director = SliceDirector { now }
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS))

        assertTrue(director.fireBark(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT) is BarkOutcome.Fired)
        // Without bypass it would be suppressed; with bypass it fires again.
        assertTrue(director.fireBark(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT) is BarkOutcome.Suppressed)
        assertTrue(
            director.fireBark(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT, bypassCooldown = true) is BarkOutcome.Fired
        )
    }

    // --- Integration: victory bark reusing a taunt key (issue #2) ---

    @Test
    fun victoryBarkIsNotSwallowedWhenItReusesTauntKey() {
        // NIB_IS_THAT_ALL_YOUVE_GOT appears in both Nib's taunt set and victory set.
        // We search for a seed where the tick-1 taunt and the tick-1 victory both
        // resolve to that key, then assert BOTH reach the Questbook (count == 2),
        // proving the combat-origin bypass prevents the victory from being dropped.
        val collisionKey = BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT
        var verified = false

        for (seed in 0..400) {
            var now = 0L
            val director = SliceDirector { now }
            director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS))
            val nib = Combatant("nib", "Nib", maxHp = 50, side = Side.PLAYER, attackPower = 50)
            val weakRat = Combatant("rat", "Rat", maxHp = 5, side = Side.ENEMY, attackPower = 1)
            director.startCombat(CombatEngine(playerParty(nib), listOf(weakRat), random = Random(seed)))

            val turn = director.combatAction(CombatAction.Attack("rat"))
            val triggered = turn.events
                .filterIsInstance<rpg.combat.CombatEvent.BarkTriggered>()
                .map { it.bark }

            // We want the engine to have emitted the collision key twice (taunt + victory).
            if (triggered.count { it == collisionKey } == 2) {
                val logged = director.questbook.log.count { it.bark == collisionKey }
                assertEquals(
                    2, logged,
                    "Both the taunt and the victory bark (same key) must reach the Questbook"
                )
                verified = true
                break
            }
        }

        assertTrue(
            verified,
            "Expected at least one seed where taunt and victory resolve to the same key"
        )
    }
}
