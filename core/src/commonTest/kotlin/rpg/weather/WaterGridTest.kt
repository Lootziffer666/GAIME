package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaterGridTest {

    @Test
    fun `addRain raises all non-blocked cells`() {
        val grid = WaterGrid(3, 3, blockedTiles = setOf(1 to 1))
        grid.addRain(0.2f)
        assertEquals(0.2f, grid[0, 0], 0.001f)
        assertEquals(0.2f, grid[2, 2], 0.001f)
        assertEquals(0.0f, grid[1, 1], 0.001f) // blocked
    }

    @Test
    fun `flowStep transfers from high to low`() {
        val grid = WaterGrid(3, 1)
        grid[1, 0] = 1.0f  // center high
        grid[0, 0] = 0.0f  // left low
        grid[2, 0] = 0.0f  // right low
        grid.flowStep()
        // Center should have lost some, neighbors gained
        assertTrue(grid[1, 0] < 1.0f)
        assertTrue(grid[0, 0] > 0.0f)
        assertTrue(grid[2, 0] > 0.0f)
    }

    @Test
    fun `drain tile reduces water`() {
        val grid = WaterGrid(3, 1, drainTiles = setOf(1 to 0))
        grid[1, 0] = 0.5f
        grid.flowStep()
        assertTrue(grid[1, 0] < 0.5f) // drained
    }

    @Test
    fun `evaporate reduces all stands`() {
        val grid = WaterGrid(2, 2)
        grid.addRain(0.3f)
        grid.evaporate(0.1f)
        assertEquals(0.2f, grid[0, 0], 0.001f)
    }

    @Test
    fun `puddleAt returns true above threshold`() {
        val grid = WaterGrid(2, 2)
        grid[0, 0] = 0.1f
        grid[1, 0] = 0.3f
        assertFalse(grid.puddleAt(0, 0))
        assertTrue(grid.puddleAt(1, 0))
    }

    @Test
    fun `connectedPuddle flood-fills adjacent puddles`() {
        val grid = WaterGrid(4, 1)
        grid[0, 0] = 0.3f
        grid[1, 0] = 0.3f
        grid[2, 0] = 0.0f  // gap
        grid[3, 0] = 0.3f  // isolated
        val connected = grid.connectedPuddle(0, 0)
        assertEquals(setOf(0 to 0, 1 to 0), connected)
    }

    @Test
    fun `isolated puddles stay separate`() {
        val grid = WaterGrid(3, 1)
        grid[0, 0] = 0.3f
        grid[2, 0] = 0.3f
        val left = grid.connectedPuddle(0, 0)
        val right = grid.connectedPuddle(2, 0)
        assertEquals(setOf(0 to 0), left)
        assertEquals(setOf(2 to 0), right)
    }
}
