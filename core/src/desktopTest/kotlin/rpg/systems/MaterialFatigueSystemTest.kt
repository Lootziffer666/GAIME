package rpg.systems

import rpg.combat.Combatant
import rpg.combat.Side
import rpg.items.Inventory
import rpg.weather.MaterialFatigue
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class MaterialFatigueSystemTest {

    private fun ctx() = object : WorldContext {
        override val player = Combatant("nib", "Nib", 80, Side.PLAYER, 12)
        override val inventory = Inventory(initialGold = 50)
        override val playerCellX = 5
        override val playerCellY = 5
        override val isPlayerIdle = false
    }

    @Test
    fun stressAccumulatesOverTime() {
        val grid = MaterialFatigue(10, 10)
        val system = MaterialFatigueSystem(grid, stressRate = 0.5f, healRate = 0f)
        system.tick(1f, ctx())
        assertTrue(grid.stressAt(3, 3) > 0f, "Stress should accumulate over time")
    }

    @Test
    fun healCountersStress() {
        val grid = MaterialFatigue(10, 10)
        grid.addStress(5, 5, 0.5f)
        val system = MaterialFatigueSystem(grid, stressRate = 0f, healRate = 1.0f)
        system.tick(1f, ctx())
        assertTrue(grid.stressAt(5, 5) < 0.5f, "Healing should reduce stress")
    }

    @Test
    fun impactAddsLocalStress() {
        val grid = MaterialFatigue(10, 10)
        val system = MaterialFatigueSystem(grid)
        system.impact(4, 4, 0.6f)
        assertTrue(grid.isCracked(4, 4), "Impact should cause cracks")
    }

    @Test
    fun impactRadiusSpreadsStress() {
        val grid = MaterialFatigue(10, 10)
        val system = MaterialFatigueSystem(grid)
        system.impactRadius(5, 5, 2, 0.8f)
        assertTrue(grid.isBroken(5, 5), "Center of radius impact should be broken")
        assertTrue(grid.stressAt(4, 5) > 0f, "Neighbors should have stress")
    }

    @Test
    fun noStressOnEmptyTick() {
        val grid = MaterialFatigue(10, 10)
        val system = MaterialFatigueSystem(grid, stressRate = 0f, healRate = 0f)
        system.tick(1f, ctx())
        assertEquals(0f, grid.stressAt(0, 0), "Zero rates should produce no change")
    }
}
