import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.bark.BarkEventBus
import rpg.bark.BarkFireResult

class BarkEventBusTest {

    private var now = 0L
    private fun bus() = BarkEventBus { now }

    @Test
    fun firstFireIsEmitted() {
        now = 0L
        val bus = bus()
        val result = bus.fire(BarkEvent.NIB_SMELL_TREASURE)
        assertTrue(result is BarkFireResult.Emitted)
    }

    @Test
    fun secondFireWithinCooldownIsSuppressed() {
        now = 0L
        val bus = bus()
        bus.fire(BarkEvent.NIB_SMELL_TREASURE) // 30s cooldown
        now = 10_000L
        val result = bus.fire(BarkEvent.NIB_SMELL_TREASURE)
        assertTrue(result is BarkFireResult.OnCooldown)
        assertEquals(20_000L, (result as BarkFireResult.OnCooldown).remainingMillis)
        assertFalse(bus.isReady(BarkEvent.NIB_SMELL_TREASURE))
    }

    @Test
    fun fireAfterCooldownIsEmittedAgain() {
        now = 0L
        val bus = bus()
        bus.fire(BarkEvent.NIB_SMELL_TREASURE)
        now = 30_000L
        assertTrue(bus.isReady(BarkEvent.NIB_SMELL_TREASURE))
        assertTrue(bus.fire(BarkEvent.NIB_SMELL_TREASURE) is BarkFireResult.Emitted)
    }

    @Test
    fun cooldownIsPerKey() {
        now = 0L
        val bus = bus()
        bus.fire(BarkEvent.NIB_SMELL_TREASURE)
        // A different bark is unaffected by another bark's cooldown.
        assertTrue(bus.fire(BarkEvent.BRUGG_ATTACK) is BarkFireResult.Emitted)
    }

    @Test
    fun resetCooldownsAllowsImmediateRefire() {
        now = 0L
        val bus = bus()
        bus.fire(BarkEvent.NIB_SMELL_TREASURE)
        bus.resetCooldowns()
        assertTrue(bus.fire(BarkEvent.NIB_SMELL_TREASURE) is BarkFireResult.Emitted)
    }
}
