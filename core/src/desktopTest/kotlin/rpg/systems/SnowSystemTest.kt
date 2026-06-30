package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.SnowGrid
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SnowSystemTest {

    private fun ctx(idle: Boolean = false, cx: Int = 5, cy: Int = 5) = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = cx
        override val playerCellY = cy
        override val isPlayerIdle = idle
    }

    @Test
    fun accumulatesWhenSnowing() {
        val grid = SnowGrid(10, 10)
        val system = SnowSystem(grid, isSnowing = true, accumulationRate = 0.5f)
        system.tick(1f, ctx())
        assertTrue(grid.depthAt(3, 3) > 0f, "Snow should accumulate when snowing")
    }

    @Test
    fun regrowsWhenNotSnowing() {
        val grid = SnowGrid(10, 10)
        grid[5, 5] = 0.3f
        grid.clearAt(5, 5, 0.2f) // create depression
        val depthBefore = grid.depthAt(5, 5)
        val system = SnowSystem(grid, isSnowing = false, regrowRate = 0.5f)
        system.tick(1f, ctx(idle = true))
        assertTrue(grid.depthAt(5, 5) > depthBefore, "Snow should regrow when not snowing")
    }

    @Test
    fun playerTramplesClearsSnow() {
        val grid = SnowGrid(10, 10)
        grid.accumulate(0.8f)
        val depthBefore = grid.depthAt(5, 5)
        val system = SnowSystem(grid, isSnowing = false, clearAmount = 1.0f)
        system.tick(1f, ctx(idle = false))
        assertTrue(grid.depthAt(5, 5) < depthBefore, "Player movement should clear snow")
    }

    @Test
    fun idlePlayerDoesNotTrample() {
        val grid = SnowGrid(10, 10)
        grid.accumulate(0.8f)
        val depthBefore = grid.depthAt(5, 5)
        val system = SnowSystem(grid, isSnowing = false, clearAmount = 1.0f, regrowRate = 0f)
        system.tick(1f, ctx(idle = true))
        assertEquals(depthBefore, grid.depthAt(5, 5), "Idle player should not trample snow")
    }

    @Test
    fun snowCapAtMaxDepth() {
        val grid = SnowGrid(10, 10)
        grid.accumulate(0.9f) // near max
        val system = SnowSystem(grid, isSnowing = true, accumulationRate = 5.0f)
        system.tick(1f, ctx())
        assertTrue(grid.depthAt(0, 0) <= SnowGrid.MAX_DEPTH, "Snow should cap at MAX_DEPTH")
    }
}
