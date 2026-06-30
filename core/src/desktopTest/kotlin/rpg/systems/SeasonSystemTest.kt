package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.Season
import rpg.weather.SeasonalGrid
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SeasonSystemTest {

    private fun ctx(idle: Boolean = false, cx: Int = 5, cy: Int = 5) = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = cx
        override val playerCellY = cy
        override val isPlayerIdle = idle
    }

    @Test
    fun springRegrowsFlowers() {
        val grid = SeasonalGrid(10, 10)
        grid.setFlower(3, 3, 0.2f)
        val system = SeasonSystem(grid, season = Season.SPRING, flowerRegrowRate = 1.0f)
        system.tick(1f, ctx(idle = true))
        assertTrue(grid.flowerAt(3, 3) > 0.2f, "Spring should regrow flowers")
    }

    @Test
    fun springPlayerTramplesFlowers() {
        val grid = SeasonalGrid(10, 10)
        grid.setFlower(5, 5, 0.8f)
        val system = SeasonSystem(grid, season = Season.SPRING)
        system.tick(0.1f, ctx(idle = false, cx = 5, cy = 5))
        assertTrue(grid.flowerAt(5, 5) < 0.8f, "Moving player should trample flowers")
    }

    @Test
    fun summerBendsGrassOnMovement() {
        val grid = SeasonalGrid(10, 10)
        val system = SeasonSystem(grid, season = Season.SUMMER)
        system.tick(0.1f, ctx(idle = false, cx = 4, cy = 4))
        assertTrue(grid.grassBendAt(4, 4) > 0f, "Moving player should bend grass")
    }

    @Test
    fun summerUnbendsGrassOverTime() {
        val grid = SeasonalGrid(10, 10)
        grid.bendGrass(3, 3)
        val system = SeasonSystem(grid, season = Season.SUMMER, grassUnbendRate = 2.0f)
        system.tick(1f, ctx(idle = true))
        assertTrue(grid.grassBendAt(3, 3) < 1.0f, "Grass should unbend over time")
    }

    @Test
    fun autumnDropsLeaves() {
        val grid = SeasonalGrid(10, 10)
        val system = SeasonSystem(grid, season = Season.AUTUMN, leafDropRate = 1.0f)
        system.tick(1f, ctx(idle = true))
        // At least some cells should have leaves
        var hasLeaves = false
        for (x in 0 until 10) for (y in 0 until 10) {
            if (grid.leafCountAt(x, y) > 0f) { hasLeaves = true; break }
        }
        assertTrue(hasLeaves, "Autumn should drop leaves")
    }

    @Test
    fun autumnKicksLeavesOnMovement() {
        val grid = SeasonalGrid(10, 10)
        grid.setLeafCount(5, 5, 0.8f)
        val system = SeasonSystem(grid, season = Season.AUTUMN)
        system.tick(0.1f, ctx(idle = false, cx = 5, cy = 5))
        assertEquals(0f, grid.leafCountAt(5, 5), "Moving player should kick leaves")
    }

    @Test
    fun winterNoOp() {
        val grid = SeasonalGrid(10, 10)
        grid.setFlower(3, 3, 0.5f)
        grid.setLeafCount(4, 4, 0.5f)
        val flowerBefore = grid.flowerAt(3, 3)
        val leafBefore = grid.leafCountAt(4, 4)
        val system = SeasonSystem(grid, season = Season.WINTER)
        system.tick(1f, ctx())
        assertEquals(flowerBefore, grid.flowerAt(3, 3), "Winter should not affect flowers")
        assertEquals(leafBefore, grid.leafCountAt(4, 4), "Winter should not affect leaves")
    }
}
