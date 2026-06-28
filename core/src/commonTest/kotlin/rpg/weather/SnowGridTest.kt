package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnowGridTest {

    @Test
    fun `accumulation increases depth`() {
        val grid = SnowGrid(4, 4)
        assertEquals(0f, grid[1, 1])
        grid.accumulate(dt = 1f, rate = 0.5f)
        assertEquals(0.5f, grid[1, 1])
    }

    @Test
    fun `accumulation is additive over multiple ticks`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 1f, rate = 0.3f)
        grid.accumulate(dt = 1f, rate = 0.3f)
        assertTrue(grid[0, 0] > 0.5f)
        assertEquals(0.6f, grid[0, 0], 0.001f)
    }

    @Test
    fun `melting decreases depth`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 1f, rate = 0.8f)
        grid.melt(dt = 1f, rate = 0.3f)
        assertEquals(0.5f, grid[0, 0], 0.001f)
    }

    @Test
    fun `stampFootprint zeroes cell`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 1f, rate = 0.5f)
        grid.stampFootprint(2, 2)
        assertEquals(0f, grid[2, 2])
        // Neighboring cells untouched
        assertEquals(0.5f, grid[1, 1], 0.001f)
    }

    @Test
    fun `refill restores stamped cells toward surrounding average`() {
        val grid = SnowGrid(4, 4)
        // Fill all cells
        grid.accumulate(dt = 1f, rate = 0.8f)
        // Stamp center
        grid.stampFootprint(2, 2)
        assertEquals(0f, grid[2, 2])
        // Refill
        grid.refill(dt = 1f, rate = 0.5f)
        assertTrue(grid[2, 2] > 0f, "Stamped cell should refill")
        assertTrue(grid[2, 2] <= 0.8f, "Stamped cell should not exceed surrounding average")
    }

    @Test
    fun `depth is clamped to 1_0 on accumulation`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 10f, rate = 1f) // would be 10.0 unclamped
        assertEquals(1f, grid[0, 0])
    }

    @Test
    fun `depth is clamped to 0_0 on melt`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 1f, rate = 0.1f)
        grid.melt(dt = 10f, rate = 1f) // would be -9.9 unclamped
        assertEquals(0f, grid[0, 0])
    }

    @Test
    fun `out of bounds returns 0_0`() {
        val grid = SnowGrid(4, 4)
        grid.accumulate(dt = 1f, rate = 0.5f)
        assertEquals(0f, grid[-1, 0])
        assertEquals(0f, grid[0, -1])
        assertEquals(0f, grid[4, 0])
        assertEquals(0f, grid[0, 4])
        assertEquals(0f, grid[99, 99])
    }

    @Test
    fun `set clamps value to 0-1 range`() {
        val grid = SnowGrid(4, 4)
        grid.set(0, 0, 2f)
        assertEquals(1f, grid[0, 0])
        grid.set(0, 0, -1f)
        assertEquals(0f, grid[0, 0])
    }
}
