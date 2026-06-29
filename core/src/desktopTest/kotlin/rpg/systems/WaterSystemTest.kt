package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.WaterGrid
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class WaterSystemTest {

    private fun ctx() = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = 5
        override val playerCellY = 5
        override val isPlayerIdle = false
    }

    @Test
    fun rainFillsGrid() {
        val grid = WaterGrid(10, 10)
        val system = WaterSystem(grid, isRaining = true, rainRate = 0.1f)
        system.tick(1f, ctx())
        // After 1 second of rain at 0.1 rate, cells should have water
        assertTrue(grid[5, 5] > 0f, "Rain should fill cells")
    }

    @Test
    fun puddlesFormAboveThreshold() {
        val grid = WaterGrid(10, 10)
        val system = WaterSystem(grid, isRaining = true, rainRate = 0.5f)
        // Tick enough to exceed puddle threshold
        system.tick(1f, ctx())
        assertTrue(grid.puddleAt(5, 5), "Heavy rain should form puddles")
    }

    @Test
    fun evaporationDriesGrid() {
        val grid = WaterGrid(10, 10)
        grid[5, 5] = 0.5f
        val system = WaterSystem(grid, isRaining = false, evaporationRate = 0.1f)
        system.tick(1f, ctx())
        assertTrue(grid[5, 5] < 0.5f, "Evaporation should reduce water")
    }

    @Test
    fun noRainNoGrowth() {
        val grid = WaterGrid(10, 10)
        val system = WaterSystem(grid, isRaining = false, evaporationRate = 0f)
        system.tick(1f, ctx())
        assertEquals(0f, grid[5, 5], "No rain + no evaporation = no change")
    }

    @Test
    fun flowDistributesWater() {
        val grid = WaterGrid(10, 10)
        grid[5, 5] = 1.0f // big puddle in center
        val system = WaterSystem(grid, isRaining = false, evaporationRate = 0f)
        system.tick(1f, ctx())
        // After flow, neighbors should have some water
        assertTrue(grid[4, 5] > 0f || grid[6, 5] > 0f || grid[5, 4] > 0f || grid[5, 6] > 0f,
            "Flow should distribute water to neighbors")
    }
}
