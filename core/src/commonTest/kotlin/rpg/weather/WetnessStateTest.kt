package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WetnessStateTest {

    @Test
    fun `soak increases wetness clamped at 1`() {
        val state = WetnessState()
        state.soak(0.5f)
        assertEquals(0.5f, state.wetness, 0.001f)
        state.soak(0.8f)
        assertEquals(1.0f, state.wetness, 0.001f)
    }

    @Test
    fun `dryNearHeat decreases wetness clamped at 0`() {
        val state = WetnessState()
        state.soak(0.6f)
        state.dryNearHeat(0.3f)
        assertEquals(0.3f, state.wetness, 0.001f)
        state.dryNearHeat(0.5f)
        assertEquals(0.0f, state.wetness, 0.001f)
    }

    @Test
    fun `isWet threshold`() {
        val state = WetnessState()
        assertFalse(state.isWet)
        state.soak(0.04f)
        assertFalse(state.isWet)
        state.soak(0.02f)
        assertTrue(state.isWet)
    }

    @Test
    fun `isSlippery threshold`() {
        val state = WetnessState()
        state.soak(0.3f)
        assertFalse(state.isSlippery)
        state.soak(0.2f)
        assertTrue(state.isSlippery)
    }
}
