package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.BloodGrid
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class BloodSystemTest {

    private fun ctx() = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = 5
        override val playerCellY = 5
        override val isPlayerIdle = false
    }

    @Test
    fun freshnessDecaysOverTime() {
        val grid = BloodGrid(10, 10)
        grid.spill(3, 3, 0.8f)
        assertTrue(grid.isFresh(3, 3), "Should start fresh")
        val system = BloodSystem(grid, agingRate = 2.0f)
        system.tick(1f, ctx())
        assertTrue(grid.freshnessAt(3, 3) < 1.0f, "Freshness should decay")
    }

    @Test
    fun bloodDriesCompletely() {
        val grid = BloodGrid(10, 10)
        grid.spill(4, 4, 0.5f)
        val system = BloodSystem(grid, agingRate = 0.5f)
        // Tick many times to fully dry
        repeat(10) { system.tick(1f, ctx()) }
        assertEquals(0f, grid.freshnessAt(4, 4), "Blood should fully dry after enough time")
    }

    @Test
    fun spillAddsBlood() {
        val grid = BloodGrid(10, 10)
        val system = BloodSystem(grid)
        system.spill(6, 6, 0.7f)
        assertTrue(grid.amountAt(6, 6) > 0f, "Spill should add blood")
        assertTrue(grid.isFresh(6, 6), "Freshly spilled blood should be fresh")
    }

    @Test
    fun noBloodNoEffect() {
        val grid = BloodGrid(10, 10)
        val system = BloodSystem(grid, agingRate = 1.0f)
        system.tick(1f, ctx())
        // No blood present, nothing should change
        assertEquals(0f, grid.amountAt(3, 3), "Empty grid should remain empty")
        assertEquals(0f, grid.freshnessAt(3, 3), "Empty grid freshness stays 0")
    }
}
