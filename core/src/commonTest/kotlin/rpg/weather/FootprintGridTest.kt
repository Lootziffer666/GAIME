package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FootprintGridTest {

    @Test
    fun `stamp creates fresh footprint`() {
        val grid = FootprintGrid(10, 10)
        grid.stamp(5, 5)
        assertTrue(grid.hasFootprint(5, 5))
        assertEquals(1.0f, grid[5, 5], 0.01f)
    }

    @Test
    fun `footprints fade over time`() {
        val grid = FootprintGrid(10, 10)
        grid.stamp(3, 3)
        grid.fade(10f) // 10 seconds
        assertTrue(grid[3, 3] < 1.0f)
        assertTrue(grid[3, 3] > 0f) // not fully gone yet
    }

    @Test
    fun `rain fades footprints faster`() {
        val grid1 = FootprintGrid(10, 10)
        val grid2 = FootprintGrid(10, 10)
        grid1.stamp(0, 0); grid2.stamp(0, 0)
        grid1.fade(5f, isRaining = false)
        grid2.fade(5f, isRaining = true)
        assertTrue(grid2[0, 0] < grid1[0, 0]) // rain fades faster
    }

    @Test
    fun `activeCount tracks visible prints`() {
        val grid = FootprintGrid(10, 10)
        assertEquals(0, grid.activeCount())
        grid.stamp(1, 1); grid.stamp(2, 2); grid.stamp(3, 3)
        assertEquals(3, grid.activeCount())
    }

    @Test
    fun `out of bounds stamps are ignored`() {
        val grid = FootprintGrid(5, 5, offsetX = 2, offsetY = 2)
        grid.stamp(1, 1) // outside (offset makes this x=-1 internally)
        assertEquals(0, grid.activeCount())
        grid.stamp(3, 3) // inside (x=1, y=1 internally)
        assertEquals(1, grid.activeCount())
    }
}
