package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayNightClockMoonTest {

    @Test
    fun `moonIntensity is zero at noon`() {
        val clock = DayNightClock(initialTime = 0.5f) // noon
        assertEquals(0f, clock.moonIntensity(), 0.01f)
    }

    @Test
    fun `moonIntensity peaks at midnight`() {
        val clock = DayNightClock(initialTime = 0f) // midnight
        assertEquals(0.7f, clock.moonIntensity(), 0.01f)
    }

    @Test
    fun `moonIntensity is moderate at dusk`() {
        // At 0.75 (dusk/dawn equivalent) expect intermediate value
        val clock = DayNightClock(initialTime = 0.75f)
        val intensity = clock.moonIntensity()
        assertTrue(intensity > 0.1f)
        assertTrue(intensity < 0.7f)
    }

    @Test
    fun `moonColor returns silver-blue scaled by intensity`() {
        val clock = DayNightClock(initialTime = 0f) // midnight, max intensity
        val (r, g, b) = clock.moonColor()
        // Silver-blue: b > g > r
        assertTrue(b > g)
        assertTrue(g > r)
        assertTrue(r > 0f) // not zero
        assertEquals(0.6f * 0.7f, r, 0.01f)
        assertEquals(0.7f * 0.7f, g, 0.01f)
        assertEquals(0.9f * 0.7f, b, 0.01f)
    }

    @Test
    fun `moonColor at noon is all zero`() {
        val clock = DayNightClock(initialTime = 0.5f) // noon
        val (r, g, b) = clock.moonColor()
        assertEquals(0f, r, 0.01f)
        assertEquals(0f, g, 0.01f)
        assertEquals(0f, b, 0.01f)
    }
}
