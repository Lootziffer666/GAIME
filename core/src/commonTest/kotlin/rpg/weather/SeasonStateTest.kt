package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeasonStateTest {

    // --- Spring: Flowers ---

    @Test
    fun `trampleFlower reduces flower intensity`() {
        val grid = SeasonalGrid(5, 5)
        grid.setFlower(2, 2, 0.8f)
        grid.trampleFlower(2, 2, 0.3f)
        assertEquals(0.5f, grid.flowerAt(2, 2), 0.001f)
    }

    @Test
    fun `trampleFlower does not go below zero`() {
        val grid = SeasonalGrid(5, 5)
        grid.setFlower(1, 1, 0.2f)
        grid.trampleFlower(1, 1, 0.5f)
        assertEquals(0.0f, grid.flowerAt(1, 1), 0.001f)
    }

    @Test
    fun `regrowFlowers recovers trampled flowers`() {
        val grid = SeasonalGrid(3, 3)
        grid.setFlower(1, 1, 0.5f)
        grid.trampleFlower(1, 1, 0.5f) // now 0.0
        grid.regrowFlowers(0.1f)
        assertEquals(0.1f, grid.flowerAt(1, 1), 0.001f)
    }

    @Test
    fun `regrowFlowers caps at MAX_FLOWER`() {
        val grid = SeasonalGrid(2, 2)
        grid.setFlower(0, 0, 0.9f)
        grid.regrowFlowers(0.2f)
        assertEquals(SeasonalGrid.MAX_FLOWER, grid.flowerAt(0, 0), 0.001f)
    }

    @Test
    fun `initFlowers fills deterministically`() {
        val grid = SeasonalGrid(5, 5)
        grid.initFlowers(0.6f)
        // Same call produces same result (deterministic)
        val grid2 = SeasonalGrid(5, 5)
        grid2.initFlowers(0.6f)
        for (y in 0 until 5) for (x in 0 until 5) {
            assertEquals(grid.flowerAt(x, y), grid2.flowerAt(x, y), 0.001f)
        }
        // At least some cells have flowers
        var hasFlower = false
        for (y in 0 until 5) for (x in 0 until 5) {
            if (grid.flowerAt(x, y) > 0f) hasFlower = true
        }
        assertTrue(hasFlower, "Expected some cells to have flowers")
    }

    // --- Summer: Grass ---

    @Test
    fun `bendGrass sets grass bend to 1`() {
        val grid = SeasonalGrid(5, 5)
        grid.bendGrass(3, 3)
        assertEquals(1.0f, grid.grassBendAt(3, 3), 0.001f)
    }

    @Test
    fun `unbendGrass decays grass bend`() {
        val grid = SeasonalGrid(5, 5)
        grid.bendGrass(2, 2)
        grid.unbendGrass(0.3f)
        assertEquals(0.7f, grid.grassBendAt(2, 2), 0.001f)
    }

    @Test
    fun `unbendGrass does not go below zero`() {
        val grid = SeasonalGrid(3, 3)
        grid.bendGrass(1, 1)
        grid.unbendGrass(1.5f)
        assertEquals(0.0f, grid.grassBendAt(1, 1), 0.001f)
    }

    @Test
    fun `windSwayGrass returns proportional offset`() {
        val grid = SeasonalGrid(3, 3)
        val sway = grid.windSwayGrass(0.5f)
        assertEquals(0.15f, sway, 0.001f)
    }

    // --- Autumn: Leaves ---

    @Test
    fun `dropLeaves accumulates leaves`() {
        val grid = SeasonalGrid(5, 5)
        grid.dropLeaves(0.3f, timeStep = 0)
        // At least some cells should have leaves
        var hasLeaf = false
        for (y in 0 until 5) for (x in 0 until 5) {
            if (grid.leafCountAt(x, y) > 0f) hasLeaf = true
        }
        assertTrue(hasLeaf, "Expected some cells to have leaves")
    }

    @Test
    fun `dropLeaves caps at MAX_LEAF`() {
        val grid = SeasonalGrid(3, 3)
        // Fill with leaves
        for (y in 0 until 3) for (x in 0 until 3) grid.setLeafCount(x, y, 0.9f)
        grid.dropLeaves(0.5f, timeStep = 0)
        for (y in 0 until 3) for (x in 0 until 3) {
            assertTrue(grid.leafCountAt(x, y) <= SeasonalGrid.MAX_LEAF)
        }
    }

    @Test
    fun `kickLeaves clears leaves at position`() {
        val grid = SeasonalGrid(5, 5)
        grid.setLeafCount(2, 3, 0.7f)
        grid.kickLeaves(2, 3)
        assertEquals(0.0f, grid.leafCountAt(2, 3), 0.001f)
    }

    @Test
    fun `kickLeaves does not affect other cells`() {
        val grid = SeasonalGrid(5, 5)
        grid.setLeafCount(2, 3, 0.7f)
        grid.setLeafCount(3, 3, 0.5f)
        grid.kickLeaves(2, 3)
        assertEquals(0.5f, grid.leafCountAt(3, 3), 0.001f)
    }

    // --- Offset handling ---

    @Test
    fun `offset shifts coordinate space for flowers`() {
        val grid = SeasonalGrid(5, 5, offsetX = -10, offsetY = -5)
        grid.setFlower(-8, -3, 0.6f)
        assertEquals(0.6f, grid.flowerAt(-8, -3), 0.001f)
        assertEquals(0.0f, grid.flowerAt(0, 0), 0.001f) // out of range
    }

    @Test
    fun `out of bounds access returns zero`() {
        val grid = SeasonalGrid(3, 3)
        assertEquals(0.0f, grid.flowerAt(-1, 0), 0.001f)
        assertEquals(0.0f, grid.grassBendAt(5, 0), 0.001f)
        assertEquals(0.0f, grid.leafCountAt(0, 10), 0.001f)
    }
}
