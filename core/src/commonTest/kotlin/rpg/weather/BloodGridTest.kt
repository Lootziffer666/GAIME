package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BloodGridTest {

    @Test
    fun `splatter sets cells at center`() {
        val grid = BloodGrid(8, 8)
        grid.splatter(4, 4, intensity = 0.7f, radius = 0)
        assertEquals(0.7f, grid[4, 4])
    }

    @Test
    fun `splatter radius works with Manhattan distance`() {
        val grid = BloodGrid(8, 8)
        grid.splatter(4, 4, intensity = 0.9f, radius = 2)
        // Center
        assertEquals(0.9f, grid[4, 4])
        // Distance 1 (adjacent)
        assertEquals(0.9f, grid[5, 4])
        assertEquals(0.9f, grid[4, 5])
        // Distance 2 (Manhattan)
        assertEquals(0.9f, grid[6, 4])
        assertEquals(0.9f, grid[5, 5])
        // Distance 3 (outside radius)
        assertEquals(0f, grid[7, 4])
        assertEquals(0f, grid[6, 5])
    }

    @Test
    fun `fade reduces blood over time`() {
        val grid = BloodGrid(4, 4)
        grid.splatter(2, 2, intensity = 1f, radius = 0)
        grid.fade(dt = 1f, rate = 0.3f)
        assertEquals(0.7f, grid[2, 2], 0.001f)
    }

    @Test
    fun `fade clamps to zero`() {
        val grid = BloodGrid(4, 4)
        grid.splatter(2, 2, intensity = 0.1f, radius = 0)
        grid.fade(dt = 10f, rate = 1f) // would go to -9.9 unclamped
        assertEquals(0f, grid[2, 2])
    }

    @Test
    fun `clear resets all cells`() {
        val grid = BloodGrid(4, 4)
        grid.splatter(0, 0, intensity = 1f, radius = 3)
        grid.clear()
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                assertEquals(0f, grid[x, y], "Cell ($x,$y) should be 0 after clear")
            }
        }
    }

    @Test
    fun `intensity clamped to 0-1`() {
        val grid = BloodGrid(4, 4)
        grid.splatter(1, 1, intensity = 5f, radius = 0)
        assertEquals(1f, grid[1, 1])
        grid.splatter(2, 2, intensity = -1f, radius = 0)
        assertEquals(0f, grid[2, 2])
    }

    @Test
    fun `out of bounds returns 0_0`() {
        val grid = BloodGrid(4, 4)
        grid.splatter(2, 2, intensity = 1f, radius = 1)
        assertEquals(0f, grid[-1, 0])
        assertEquals(0f, grid[0, -1])
        assertEquals(0f, grid[4, 0])
        assertEquals(0f, grid[0, 4])
    }
}
