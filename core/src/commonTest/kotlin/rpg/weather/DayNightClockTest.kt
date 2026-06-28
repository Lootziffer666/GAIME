package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayNightClockTest {

    @Test
    fun `initial time is set correctly`() {
        val clock = DayNightClock(0.25f)
        assertEquals(0.25f, clock.timeOfDay, 0.001f)
    }

    @Test
    fun `advance increases time`() {
        val clock = DayNightClock(0.0f)
        clock.advance(1f, speed = 0.1f) // dt=1s, speed=0.1 -> +0.1
        assertEquals(0.1f, clock.timeOfDay, 0.001f)
    }

    @Test
    fun `advance wraps cyclically past 1`() {
        val clock = DayNightClock(0.9f)
        clock.advance(1f, speed = 0.2f) // 0.9 + 0.2 = 1.1 -> wraps to 0.1
        assertEquals(0.1f, clock.timeOfDay, 0.001f)
    }

    @Test
    fun `darkness is high at midnight`() {
        val clock = DayNightClock(0.0f) // midnight
        val d = clock.darkness()
        assertTrue(d > 0.5f, "Darkness at midnight should be > 0.5, was $d")
    }

    @Test
    fun `darkness is low at noon`() {
        val clock = DayNightClock(0.5f) // noon
        val d = clock.darkness()
        assertTrue(d < 0.05f, "Darkness at noon should be near 0, was $d")
    }

    @Test
    fun `darkness is symmetric around midnight`() {
        val early = DayNightClock(0.1f).darkness()
        val late = DayNightClock(0.9f).darkness()
        assertEquals(early, late, 0.01f)
    }

    @Test
    fun `ambientColor is warm at noon`() {
        val clock = DayNightClock(0.5f)
        val (r, g, b) = clock.ambientColor()
        assertTrue(r > 0.9f, "Day R should be high, was $r")
        assertTrue(g > 0.9f, "Day G should be high, was $g")
    }

    @Test
    fun `ambientColor is blue-cool at midnight`() {
        val clock = DayNightClock(0.0f)
        val (r, g, b) = clock.ambientColor()
        assertTrue(r < 0.5f, "Night R should be low, was $r")
        assertTrue(b > 0.7f, "Night B should be high, was $b")
    }

    @Test
    fun `full day cycle returns to same time`() {
        val clock = DayNightClock(0.3f)
        // Advance by full cycle: speed=1 means 1 second = 1 full day
        clock.advance(1f, speed = 1f)
        assertEquals(0.3f, clock.timeOfDay, 0.001f)
    }
}
