package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherStateTest {

    @Test
    fun `tick advances snow accumulation`() {
        val config = WeatherConfig(snowRate = 0.5f)
        val state = WeatherState(config, gridWidth = 4, gridHeight = 4)
        state.tick(dt = 1f)
        assertTrue(state.snow[0, 0] > 0f, "Snow should accumulate after tick")
        assertEquals(0.5f, state.snow[0, 0], 0.001f)
    }

    @Test
    fun `tick fades blood`() {
        val config = WeatherConfig(bloodFadeRate = 0.2f)
        val state = WeatherState(config, gridWidth = 4, gridHeight = 4)
        state.blood.splatter(2, 2, intensity = 1f, radius = 0)
        state.tick(dt = 1f)
        assertTrue(state.blood[2, 2] < 1f, "Blood should fade after tick")
        assertEquals(0.8f, state.blood[2, 2], 0.001f)
    }

    @Test
    fun `tick advances wind`() {
        val config = WeatherConfig(windDriftSpeed = 1f)
        val state = WeatherState(config, gridWidth = 4, gridHeight = 4)
        val initialDirection = state.wind.direction
        repeat(50) { state.tick(dt = 0.1f) }
        assertTrue(
            state.wind.direction != initialDirection,
            "Wind direction should change over time"
        )
    }

    @Test
    fun `config parameters are respected`() {
        val fastConfig = WeatherConfig(snowRate = 1f, bloodFadeRate = 1f)
        val slowConfig = WeatherConfig(snowRate = 0.01f, bloodFadeRate = 0.01f)

        val fastState = WeatherState(fastConfig, gridWidth = 4, gridHeight = 4)
        val slowState = WeatherState(slowConfig, gridWidth = 4, gridHeight = 4)

        fastState.blood.splatter(1, 1, 1f, 0)
        slowState.blood.splatter(1, 1, 1f, 0)

        fastState.tick(dt = 1f)
        slowState.tick(dt = 1f)

        // Fast snow accumulates more
        assertTrue(fastState.snow[0, 0] > slowState.snow[0, 0])
        // Fast blood fades more
        assertTrue(fastState.blood[1, 1] < slowState.blood[1, 1])
    }
}
