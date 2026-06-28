package rpg.weather

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindStateTest {

    @Test
    fun `dx dy correspond to angle - angle 0 gives dx equals strength`() {
        val wind = WindState(direction = 0f, strength = 1f)
        assertEquals(1f, wind.dx, 0.001f)
        assertEquals(0f, wind.dy, 0.001f)
    }

    @Test
    fun `dx dy at pi over 2 gives dy equals strength`() {
        val pi = 3.14159265f
        val wind = WindState(direction = pi / 2f, strength = 1f)
        assertEquals(0f, wind.dx, 0.001f)
        assertEquals(1f, wind.dy, 0.001f)
    }

    @Test
    fun `strength 0 gives zero vector`() {
        val wind = WindState(direction = 1.5f, strength = 0f)
        assertEquals(0f, wind.dx, 0.001f)
        assertEquals(0f, wind.dy, 0.001f)
    }

    @Test
    fun `strength scales the components`() {
        val wind = WindState(direction = 0f, strength = 0.5f)
        assertEquals(0.5f, wind.dx, 0.001f)
        assertEquals(0f, wind.dy, 0.001f)
    }

    @Test
    fun `update drifts direction over time`() {
        val wind = WindState(direction = 0f, strength = 0.5f, gustiness = 0.5f)
        val initialDirection = wind.direction
        // Advance multiple steps
        repeat(100) { wind.update(0.1f) }
        assertTrue(
            wind.direction != initialDirection,
            "Direction should drift after updates"
        )
    }

    @Test
    fun `update is deterministic`() {
        val wind1 = WindState(direction = 0f, strength = 1f, gustiness = 0.3f)
        val wind2 = WindState(direction = 0f, strength = 1f, gustiness = 0.3f)
        repeat(50) {
            wind1.update(0.05f)
            wind2.update(0.05f)
        }
        assertEquals(wind1.direction, wind2.direction, 0.0001f)
    }
}
