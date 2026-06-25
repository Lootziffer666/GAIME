import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.combat.AttackStyle
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.Combatant
import rpg.combat.EnemyArchetype
import rpg.combat.Side

class SludgeBlobCombatTest {

    private fun member(id: String, hp: Int, atk: Int) =
        Combatant(id, id, maxHp = hp, side = Side.PLAYER, attackPower = atk)

    @Test
    fun blobSkipsAttackOnOddTicksAndAttacksOnEvenTicks() {
        val party = listOf(member("h1", 100, 1))
        val blob = EnemyArchetype.SLUDGE_BLOB.spawn("blob")
        val engine = CombatEngine(party, listOf(blob), random = Random(42))

        // Tick 0 (even): blob attacks
        val events0 = engine.tick(CombatAction.Wait)
        assertTrue(events0.any { it is CombatEvent.Message && "hits" in it.text },
            "Blob should attack on tick 0 (even)")

        // Tick 1 (odd): blob prepares
        val events1 = engine.tick(CombatAction.Wait)
        assertTrue(events1.any { it is CombatEvent.Message && "preparing to spit" in it.text },
            "Blob should prepare on tick 1 (odd)")
        // No hit on this tick from the blob
        val hitMessages1 = events1.filter { it is CombatEvent.Message && "blob" in (it as CombatEvent.Message).text.lowercase() && "hits" in it.text.lowercase() }
        assertTrue(hitMessages1.isEmpty(), "Blob should not hit on odd tick")

        // Tick 2 (even): blob attacks again
        val events2 = engine.tick(CombatAction.Wait)
        assertTrue(events2.any { it is CombatEvent.Message && "hits" in it.text },
            "Blob should attack on tick 2 (even)")
    }

    @Test
    fun blobTargetsRandomPartyMemberNotAlwaysFirst() {
        // Create a 3-member party with high HP to survive multiple attacks
        val party = listOf(
            member("h1", 200, 1),
            member("h2", 200, 1),
            member("h3", 200, 1)
        )
        val blob = EnemyArchetype.SLUDGE_BLOB.spawn("blob")
        val engine = CombatEngine(party, listOf(blob), random = Random(7))

        // Collect targets over several attack ticks
        val targets = mutableSetOf<String>()
        repeat(20) {
            val events = engine.tick(CombatAction.Wait)
            for (event in events) {
                if (event is CombatEvent.Message && "Sludge Blob hits" in event.text) {
                    // Extract target name from "Sludge Blob hits <name> for X."
                    val targetName = event.text.removePrefix("Sludge Blob hits ").substringBefore(" for")
                    targets.add(targetName)
                }
            }
        }
        // Over 20 ticks (10 attack ticks), with seeded random, the blob should
        // hit more than just the first party member
        assertTrue(targets.size > 1,
            "Blob should target multiple party members but only targeted: $targets")
    }

    @Test
    fun blobHasHigherHpThanRat() {
        val blob = EnemyArchetype.SLUDGE_BLOB.spawn("blob")
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat")
        assertEquals(20, blob.maxHp)
        assertEquals(12, rat.maxHp)
        assertTrue(blob.maxHp > rat.maxHp, "Sludge Blob should have more HP than Sewer Rat")
    }

    @Test
    fun sewerRatStillTargetsFirstLivingPartyMember() {
        val party = listOf(
            member("h1", 100, 1),
            member("h2", 100, 1),
            member("h3", 100, 1)
        )
        val rat = EnemyArchetype.SEWER_RAT.spawn("rat")
        val engine = CombatEngine(party, listOf(rat), random = Random(99))

        // Rat should always target h1 (the first living party member)
        repeat(5) {
            val events = engine.tick(CombatAction.Wait)
            val hitEvents = events.filter { it is CombatEvent.Message && "Sewer Rat hits" in it.text }
            for (event in hitEvents) {
                val msg = (event as CombatEvent.Message).text
                assertTrue("hits h1" in msg,
                    "Sewer Rat should always target the first living member, got: $msg")
            }
        }
    }

    @Test
    fun sewerRatHasMeleeAttackStyle() {
        assertEquals(AttackStyle.MELEE, EnemyArchetype.SEWER_RAT.attackStyle)
    }

    @Test
    fun sludgeBlobHasRangedSlowAttackStyle() {
        assertEquals(AttackStyle.RANGED_SLOW, EnemyArchetype.SLUDGE_BLOB.attackStyle)
    }
}
