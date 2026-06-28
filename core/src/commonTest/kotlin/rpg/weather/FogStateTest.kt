package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FogStateTest {

    @Test
    fun `initial density is zero`() {
        val fog = FogState()
        assertEquals(0f, fog.density, 0.001f)
    }

    @Test
    fun `setDensity updates density`() {
        val fog = FogState()
        fog.setDensity(0.7f)
        assertEquals(0.7f, fog.density, 0.001f)
    }

    @Test
    fun `setDensity clamps to 0 and 1`() {
        val fog = FogState()
        fog.setDensity(1.5f)
        assertEquals(1.0f, fog.density, 0.001f)
        fog.setDensity(-0.3f)
        assertEquals(0.0f, fog.density, 0.001f)
    }

    @Test
    fun `drift changes offset based on wind`() {
        val fog = FogState()
        val wind = WindState()
        wind.dx = 1.0f
        wind.dy = 0.5f
        wind.strength = 0.8f
        fog.drift(2.0f, wind)
        // driftX = 1.0 * 0.8 * 2.0 = 1.6
        // driftY = 0.5 * 0.8 * 2.0 = 0.8
        assertEquals(1.6f, fog.driftX, 0.01f)
        assertEquals(0.8f, fog.driftY, 0.01f)
    }

    @Test
    fun `drift accumulates over multiple calls`() {
        val fog = FogState()
        val wind = WindState()
        wind.dx = 0.5f
        wind.dy = 0.0f
        wind.strength = 1.0f
        fog.drift(1.0f, wind)
        fog.drift(1.0f, wind)
        // 0.5 * 1.0 * 1.0 * 2 calls = 1.0
        assertEquals(1.0f, fog.driftX, 0.01f)
    }

    @Test
    fun `zero wind produces no drift`() {
        val fog = FogState()
        val wind = WindState()
        wind.dx = 0.0f
        wind.dy = 0.0f
        wind.strength = 0.0f
        fog.drift(10.0f, wind)
        assertEquals(0.0f, fog.driftX, 0.001f)
        assertEquals(0.0f, fog.driftY, 0.001f)
    }
}
