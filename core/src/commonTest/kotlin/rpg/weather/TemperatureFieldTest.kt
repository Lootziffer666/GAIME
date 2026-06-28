package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemperatureFieldTest {

    @Test
    fun `no sources returns base temperature`() {
        val field = TemperatureField(baseTemp = -0.5f)
        assertEquals(-0.5f, field.tempAt(5f, 5f), 0.001f)
    }

    @Test
    fun `near heat source is warm`() {
        val field = TemperatureField(baseTemp = -0.5f)
        field.addHeatSource(5f, 5f, radius = 3f, strength = 1.0f)
        val temp = field.tempAt(5f, 5f) // at source center
        assertTrue(temp > 0.4f, "At source center temp should be warm, was $temp")
    }

    @Test
    fun `far from source is cold`() {
        val field = TemperatureField(baseTemp = -0.5f)
        field.addHeatSource(5f, 5f, radius = 3f, strength = 1.0f)
        val temp = field.tempAt(100f, 100f) // far away
        assertEquals(-0.5f, temp, 0.001f) // base temp
    }

    @Test
    fun `heat falls off with distance`() {
        val field = TemperatureField(baseTemp = -0.5f)
        field.addHeatSource(0f, 0f, radius = 10f, strength = 1.0f)
        val atCenter = field.tempAt(0f, 0f)
        val halfWay = field.tempAt(5f, 0f)
        val atEdge = field.tempAt(9.9f, 0f)
        assertTrue(atCenter > halfWay, "Center ($atCenter) > halfway ($halfWay)")
        assertTrue(halfWay > atEdge, "Halfway ($halfWay) > edge ($atEdge)")
    }

    @Test
    fun `multiple sources are additive`() {
        val field = TemperatureField(baseTemp = -0.5f)
        field.addHeatSource(0f, 0f, radius = 5f, strength = 0.5f)
        val withOne = field.tempAt(0f, 0f)
        field.addHeatSource(0f, 0f, radius = 5f, strength = 0.5f)
        val withTwo = field.tempAt(0f, 0f)
        assertTrue(withTwo > withOne, "Two sources ($withTwo) > one source ($withOne)")
    }

    @Test
    fun `result is clamped to plus one`() {
        val field = TemperatureField(baseTemp = 0f)
        field.addHeatSource(0f, 0f, radius = 10f, strength = 5.0f)
        assertEquals(1.0f, field.tempAt(0f, 0f), 0.001f)
    }

    @Test
    fun `clearSources removes all sources`() {
        val field = TemperatureField(baseTemp = -0.5f)
        field.addHeatSource(0f, 0f, radius = 5f, strength = 1.0f)
        field.clearSources()
        assertEquals(-0.5f, field.tempAt(0f, 0f), 0.001f)
    }
}
