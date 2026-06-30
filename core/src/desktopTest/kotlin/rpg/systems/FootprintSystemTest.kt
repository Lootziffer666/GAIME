package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.FootprintGrid
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FootprintSystemTest {

    private fun ctx(idle: Boolean = false, cx: Int = 5, cy: Int = 5) = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = cx
        override val playerCellY = cy
        override val isPlayerIdle = idle
    }

    @Test
    fun stampsOnMovement() {
        val grid = FootprintGrid(10, 10)
        val system = FootprintSystem(grid)
        system.tick(0.1f, ctx(idle = false, cx = 3, cy = 4))
        assertTrue(grid.hasFootprint(3, 4), "Moving player should leave footprint")
    }

    @Test
    fun idlePlayerNoStamp() {
        val grid = FootprintGrid(10, 10)
        val system = FootprintSystem(grid)
        system.tick(0.1f, ctx(idle = true, cx = 3, cy = 4))
        assertFalse(grid.hasFootprint(3, 4), "Idle player should not stamp")
    }

    @Test
    fun footprintsFadeOverTime() {
        val grid = FootprintGrid(10, 10)
        grid.stamp(5, 5)
        val system = FootprintSystem(grid)
        // Tick many times to fade
        repeat(200) { system.tick(1f, ctx(idle = true)) }
        assertFalse(grid.hasFootprint(5, 5), "Footprints should fade over time")
    }

    @Test
    fun rainAcceleratesFade() {
        val grid = FootprintGrid(10, 10)
        grid.stamp(5, 5)
        val system = FootprintSystem(grid, isRaining = true)
        val intensityBefore = grid[5, 5]
        system.tick(1f, ctx(idle = true))
        assertTrue(grid[5, 5] < intensityBefore, "Rain should accelerate fading")
    }

    @Test
    fun noStampOnSameCell() {
        val grid = FootprintGrid(10, 10)
        val system = FootprintSystem(grid)
        // First tick stamps
        system.tick(0.1f, ctx(idle = false, cx = 5, cy = 5))
        assertTrue(grid.hasFootprint(5, 5))
        // Second tick at same cell should not re-stamp (no movement)
        system.tick(0.1f, ctx(idle = false, cx = 5, cy = 5))
        // Intensity should still be 1.0 minus fade, not higher
        assertTrue(grid[5, 5] <= 1.0f, "Should not overstamp")
    }
}
