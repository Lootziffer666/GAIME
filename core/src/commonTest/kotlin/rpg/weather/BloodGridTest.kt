package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BloodGridTest {

    @Test
    fun `spill sets blood amount and marks fresh`() {
        val grid = BloodGrid(3, 3)
        grid.spill(1, 1, 0.5f)
        assertEquals(0.5f, grid.amountAt(1, 1), 0.001f)
        assertEquals(1.0f, grid.freshnessAt(1, 1), 0.001f)
        assertTrue(grid.isFresh(1, 1))
    }

    @Test
    fun `spill out of bounds is ignored`() {
        val grid = BloodGrid(2, 2)
        grid.spill(-1, 0, 1.0f)
        grid.spill(5, 5, 1.0f)
        // No crash, no effect
        assertEquals(0.0f, grid.amountAt(0, 0), 0.001f)
    }

    @Test
    fun `age decays freshness`() {
        val grid = BloodGrid(3, 3)
        grid.spill(1, 1, 0.8f)
        grid.age(0.3f)
        assertEquals(0.7f, grid.freshnessAt(1, 1), 0.001f)
        assertTrue(grid.isFresh(1, 1)) // still above 0.5
        grid.age(0.3f)
        assertEquals(0.4f, grid.freshnessAt(1, 1), 0.001f)
        assertFalse(grid.isFresh(1, 1)) // now below 0.5
    }

    @Test
    fun `age does not reduce amount`() {
        val grid = BloodGrid(2, 2)
        grid.spill(0, 0, 0.6f)
        grid.age(1.0f) // full decay
        assertEquals(0.6f, grid.amountAt(0, 0), 0.001f)
        assertEquals(0.0f, grid.freshnessAt(0, 0), 0.001f)
    }

    @Test
    fun `isFresh threshold is 0_5`() {
        val grid = BloodGrid(3, 3)
        grid.spill(0, 0, 0.5f)
        grid.age(0.49f)
        assertTrue(grid.isFresh(0, 0)) // freshness = 0.51
        grid.age(0.02f)
        assertFalse(grid.isFresh(0, 0)) // freshness = 0.49
    }

    @Test
    fun `offset shifts coordinate space`() {
        val grid = BloodGrid(3, 3, offsetX = 5, offsetY = 5)
        grid.spill(5, 5, 1.0f)
        assertEquals(1.0f, grid.amountAt(5, 5), 0.001f)
        assertEquals(0.0f, grid.amountAt(0, 0), 0.001f)
    }
}
