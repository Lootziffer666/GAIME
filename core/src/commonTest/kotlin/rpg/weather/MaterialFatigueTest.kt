package rpg.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaterialFatigueTest {

    @Test
    fun `initial stress is zero`() {
        val mf = MaterialFatigue(5, 5)
        assertEquals(0f, mf.stressAt(0, 0), 0.001f)
        assertEquals(0f, mf.stressAt(4, 4), 0.001f)
    }

    @Test
    fun `addStress increases stress`() {
        val mf = MaterialFatigue(5, 5)
        mf.addStress(2, 2, 0.4f)
        assertEquals(0.4f, mf.stressAt(2, 2), 0.001f)
        assertEquals(0f, mf.stressAt(1, 1), 0.001f) // other cells unaffected
    }

    @Test
    fun `addStress caps at MAX_STRESS`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(1, 1, 0.8f)
        mf.addStress(1, 1, 0.5f)
        assertEquals(MaterialFatigue.MAX_STRESS, mf.stressAt(1, 1), 0.001f)
    }

    @Test
    fun `isCracked threshold`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(1, 1, 0.29f)
        assertFalse(mf.isCracked(1, 1))
        mf.addStress(1, 1, 0.02f)
        assertTrue(mf.isCracked(1, 1))
    }

    @Test
    fun `isBroken threshold`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(0, 0, 0.69f)
        assertFalse(mf.isBroken(0, 0))
        mf.addStress(0, 0, 0.02f)
        assertTrue(mf.isBroken(0, 0))
    }

    @Test
    fun `addStressRadius spreads in manhattan distance`() {
        val mf = MaterialFatigue(5, 5)
        mf.addStressRadius(2, 2, radius = 1, amount = 0.5f)
        // Center + 4 cardinal neighbors should have stress
        assertEquals(0.5f, mf.stressAt(2, 2), 0.001f)
        assertEquals(0.5f, mf.stressAt(1, 2), 0.001f)
        assertEquals(0.5f, mf.stressAt(3, 2), 0.001f)
        assertEquals(0.5f, mf.stressAt(2, 1), 0.001f)
        assertEquals(0.5f, mf.stressAt(2, 3), 0.001f)
        // Diagonal at distance 2 should NOT have stress
        assertEquals(0f, mf.stressAt(1, 1), 0.001f)
    }

    @Test
    fun `repair resets stress to zero`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(1, 1, 0.8f)
        mf.repair(1, 1)
        assertEquals(0f, mf.stressAt(1, 1), 0.001f)
    }

    @Test
    fun `heal reduces all stress`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(0, 0, 0.5f)
        mf.addStress(2, 2, 0.3f)
        mf.heal(0.2f)
        assertEquals(0.3f, mf.stressAt(0, 0), 0.001f)
        assertEquals(0.1f, mf.stressAt(2, 2), 0.001f)
    }

    @Test
    fun `heal does not go below zero`() {
        val mf = MaterialFatigue(3, 3)
        mf.addStress(1, 1, 0.1f)
        mf.heal(0.5f)
        assertEquals(0f, mf.stressAt(1, 1), 0.001f)
    }

    @Test
    fun `offset coordinates work correctly`() {
        val mf = MaterialFatigue(5, 5, offsetX = -2, offsetY = -3)
        mf.addStress(-2, -3, 0.4f) // maps to local (0,0)
        assertEquals(0.4f, mf.stressAt(-2, -3), 0.001f)
        mf.addStress(2, 1, 0.6f) // maps to local (4,4)
        assertEquals(0.6f, mf.stressAt(2, 1), 0.001f)
    }

    @Test
    fun `out of bounds returns zero`() {
        val mf = MaterialFatigue(3, 3, offsetX = 0, offsetY = 0)
        assertEquals(0f, mf.stressAt(-1, -1), 0.001f)
        assertEquals(0f, mf.stressAt(5, 5), 0.001f)
    }
}
