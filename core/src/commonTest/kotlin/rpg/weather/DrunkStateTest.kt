package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrunkStateTest {

    @Test
    fun `drink increases drunkLevel clamped at 1`() {
        val state = DrunkState()
        state.drink(0.34f)
        assertEquals(0.34f, state.drunkLevel, 0.01f)
        state.drink(0.34f)
        assertEquals(0.68f, state.drunkLevel, 0.01f)
        state.drink(0.34f)
        assertEquals(1.0f, state.drunkLevel, 0.01f)
        state.drink(0.5f) // should clamp
        assertEquals(1.0f, state.drunkLevel, 0.01f)
    }

    @Test
    fun `stumbleChance rises with drunkLevel`() {
        val state = DrunkState()
        assertEquals(0f, state.stumbleChance)
        state.drink(0.35f) // just above threshold
        assertTrue(state.stumbleChance > 0f)
        state.drink(0.35f) // heavily drunk
        assertTrue(state.stumbleChance > 0.3f)
    }

    @Test
    fun `attackBonus scales with drunkLevel`() {
        val state = DrunkState()
        assertEquals(0, state.attackBonus)
        state.drink(0.35f)
        assertEquals(2, state.attackBonus)
        state.drink(0.3f) // ~0.65 = heavy
        assertEquals(4, state.attackBonus)
        state.drink(0.3f) // ~0.95 = blackout
        assertEquals(6, state.attackBonus)
    }

    @Test
    fun `soberTick reduces drunkLevel over time`() {
        val state = DrunkState()
        state.drink(0.5f)
        state.soberTick(5f) // 5 seconds
        assertTrue(state.drunkLevel < 0.5f)
        assertTrue(state.drunkLevel > 0.3f) // not instant
    }

    @Test
    fun `delayed damage returned when sobering up`() {
        val state = DrunkState()
        state.drink(0.5f)
        state.delayPain(10)
        state.delayPain(5)
        assertEquals(15, state.delayedDamage)
        // Sober up completely
        val dmg = state.forceSober()
        assertEquals(15, dmg)
        assertEquals(0, state.delayedDamage)
    }

    @Test
    fun `sleep after idle too long while drunk`() {
        val state = DrunkState()
        state.drink(0.5f)
        assertFalse(state.isAsleep)
        state.tickIdle(9f) // over threshold
        assertTrue(state.isAsleep)
    }

    @Test
    fun `not asleep when sober`() {
        val state = DrunkState()
        state.tickIdle(20f)
        assertFalse(state.isAsleep)
    }

    @Test
    fun `goldStolenWhileAsleep scales with drunkLevel`() {
        val state = DrunkState()
        state.drink(0.7f)
        state.tickIdle(10f)
        assertTrue(state.isAsleep)
        val stolen = state.goldStolenWhileAsleep(100)
        assertTrue(stolen > 0)
        assertTrue(stolen <= 30) // ~21 at 0.7 level
    }

    @Test
    fun `wakeUp resets idle`() {
        val state = DrunkState()
        state.drink(0.5f)
        state.tickIdle(10f)
        assertTrue(state.isAsleep)
        state.wakeUp()
        assertFalse(state.isAsleep)
    }

    @Test
    fun `resetIdle on activity`() {
        val state = DrunkState()
        state.drink(0.5f)
        state.tickIdle(5f)
        state.resetIdle()
        assertEquals(0f, state.idleSeconds)
    }
}
