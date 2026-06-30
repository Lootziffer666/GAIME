package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DrunkSystemTest {

    private fun ctx(idle: Boolean = false, gold: Int = 50): TestWorldContext {
        return TestWorldContext(
            player = Combatant("nib", "Nib", 80, Side.PLAYER, 12),
            inventory = Inventory(initialGold = gold),
            playerCellX = 5,
            playerCellY = 5,
            isPlayerIdle = idle,
        )
    }

    private class TestWorldContext(
        override val player: Combatant,
        override val inventory: Inventory,
        override val playerCellX: Int,
        override val playerCellY: Int,
        override val isPlayerIdle: Boolean,
    ) : WorldContext

    @Test
    fun soberTickReducesDrunkLevel() {
        val system = DrunkSystem()
        system.drink(0.5f)
        val initial = system.drunkLevel
        system.tick(1f, ctx())
        assertTrue(system.drunkLevel < initial, "Sober tick should reduce drunk level")
    }

    @Test
    fun delayedDamageAppliedOnSoberUp() {
        val system = DrunkSystem()
        system.drink(0.35f) // above stumble threshold so delayPain works
        system.state.delayPain(10)
        val c = ctx()
        // Tick enough to sober up fully
        repeat(200) { system.tick(0.1f, c) }
        assertTrue(c.player.hp < 80,
            "Delayed damage should be applied when sobering up, hp=${c.player.hp}")
    }

    @Test
    fun noDamageWhenSober() {
        val system = DrunkSystem()
        val c = ctx()
        system.tick(1f, c)
        assertEquals(0, system.lastSoberDamage, "No damage when sober")
        assertEquals(80, c.player.hp, "HP unchanged when sober")
    }

    @Test
    fun goldStolenWhenAsleep() {
        val system = DrunkSystem()
        system.drink(0.7f) // heavily drunk
        val c = ctx(idle = true, gold = 100)
        // Tick long enough to trigger sleep (>8 seconds idle)
        repeat(100) { system.tick(0.1f, c) }
        assertTrue(system.lastGoldStolen > 0 || c.inventory.gold < 100,
            "Gold should be stolen when drunk and idle")
    }

    @Test
    fun noTheftWhenSober() {
        val system = DrunkSystem()
        val c = ctx(idle = true, gold = 100)
        repeat(100) { system.tick(0.1f, c) }
        assertEquals(0, system.lastGoldStolen, "No theft when sober")
        assertEquals(100, c.inventory.gold, "Gold unchanged when sober")
    }

    @Test
    fun stumbleChanceScalesWithDrunk() {
        val system = DrunkSystem()
        assertEquals(0f, system.stumbleChance, "Sober = no stumble")
        system.drink(0.5f)
        assertTrue(system.stumbleChance > 0f, "Drunk = stumble chance")
    }

    @Test
    fun resetIdlePreventssSleep() {
        val system = DrunkSystem()
        system.drink(0.7f)
        val c = ctx(idle = true)
        // Tick for 5 seconds (below sleep threshold of 8)
        repeat(50) { system.tick(0.1f, c) }
        system.resetIdle()
        // Tick for 5 more seconds — total idle should be reset, not 10
        repeat(50) { system.tick(0.1f, c) }
        assertFalse(system.state.isAsleep, "Reset idle should prevent sleep")
    }
}
