package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnowGridTest {

    @Test
    fun `accumulate raises depth for all cells`() {
        val grid = SnowGrid(3, 3)
        grid.accumulate(0.2f)
        assertEquals(0.2f, grid.depthAt(0, 0), 0.001f)
        assertEquals(0.2f, grid.depthAt(2, 2), 0.001f)
    }

    @Test
    fun `accumulate caps at MAX_DEPTH`() {
        val grid = SnowGrid(2, 2)
        grid.accumulate(0.8f)
        grid.accumulate(0.5f)
        assertEquals(SnowGrid.MAX_DEPTH, grid.depthAt(0, 0), 0.001f)
    }

    @Test
    fun `clearAt creates footprint depression`() {
        val grid = SnowGrid(3, 3)
        grid.accumulate(0.5f)
        grid.clearAt(1, 1, 0.3f)
        assertEquals(0.2f, grid.depthAt(1, 1), 0.001f)
        assertEquals(0.5f, grid.depthAt(0, 0), 0.001f) // other cells unaffected
    }

    @Test
    fun `clearAt does not go below zero`() {
        val grid = SnowGrid(2, 2)
        grid.accumulate(0.1f)
        grid.clearAt(0, 0, 0.5f)
        assertEquals(0.0f, grid.depthAt(0, 0), 0.001f)
    }

    @Test
    fun `regrow refills cleared cells`() {
        val grid = SnowGrid(3, 3)
        grid.accumulate(0.5f)
        grid.clearAt(1, 1, 0.5f) // now 0.0
        grid.regrow(0.1f)
        assertEquals(0.1f, grid.depthAt(1, 1), 0.001f)
        // Other cells also get regrow but cap at MAX
        assertEquals(0.6f, grid.depthAt(0, 0), 0.001f)
    }

    @Test
    fun `depthAt returns zero for out of bounds`() {
        val grid = SnowGrid(3, 3)
        grid.accumulate(1.0f)
        assertEquals(0.0f, grid.depthAt(-1, 0), 0.001f)
        assertEquals(0.0f, grid.depthAt(0, 5), 0.001f)
    }

    @Test
    fun `offset shifts coordinate space`() {
        val grid = SnowGrid(3, 3, offsetX = 10, offsetY = 20)
        grid[10, 20] = 0.7f
        assertEquals(0.7f, grid.depthAt(10, 20), 0.001f)
        assertEquals(0.0f, grid.depthAt(0, 0), 0.001f) // out of offset range
    }
}
