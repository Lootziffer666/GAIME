import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import rpg.SlicePhase
import rpg.bark.AmbientBarks

/**
 * Tests for the ambient/exploration/idle bark pools (review issues #3 and #5).
 *
 * Selection is driven by an injected [Random], so identical seeds produce
 * identical picks — making the previously untestable idle-bark behaviour
 * reproducible.
 */
class AmbientBarksTest {

    // --- Determinism ---

    @Test
    fun pickIsDeterministicForAGivenSeed() {
        val a = AmbientBarks.pick(AmbientBarks.SEWER_ATMOSPHERE, Random(123))
        val b = AmbientBarks.pick(AmbientBarks.SEWER_ATMOSPHERE, Random(123))
        assertEquals(a, b, "same seed must yield the same bark")
    }

    @Test
    fun pickIdleIsDeterministicForAGivenSeed() {
        val a = AmbientBarks.pickIdle(SlicePhase.TAVERN, Random(7))
        val b = AmbientBarks.pickIdle(SlicePhase.TAVERN, Random(7))
        assertEquals(a, b, "same seed must yield the same idle bark")
    }

    @Test
    fun pickReturnsAMemberOfThePool() {
        repeat(20) { seed ->
            val picked = AmbientBarks.pick(AmbientBarks.ENEMY_WARNING, Random(seed))
            assertTrue(picked in AmbientBarks.ENEMY_WARNING, "pick must come from the pool")
        }
    }

    // --- Empty pools ---

    @Test
    fun pickReturnsNullForEmptyPool() {
        assertNull(AmbientBarks.pick(emptyList(), Random(1)))
    }

    @Test
    fun idleBarksAreEmptyForNonExplorationPhases() {
        val nonExploration = listOf(
            SlicePhase.INTRO_CUTSCENE,
            SlicePhase.NPC_DIALOGUE,
            SlicePhase.BOSS_COMBAT,
            SlicePhase.QUESTBOOK_FULL,
            SlicePhase.VICTORY,
            SlicePhase.GAME_OVER
        )
        nonExploration.forEach { phase ->
            assertTrue(AmbientBarks.idleBarks(phase).isEmpty(), "$phase should have no idle pool")
            assertNull(AmbientBarks.pickIdle(phase, Random(0)), "$phase pickIdle should be null")
        }
    }

    // --- Exploration phases have non-empty pools ---

    @Test
    fun explorationPhasesHaveIdleBarks() {
        val exploration = listOf(
            SlicePhase.TAVERN,
            SlicePhase.SEWER,
            SlicePhase.BOSS_ROOM,
            SlicePhase.CHAPTER2_MARKET,
            SlicePhase.CHAPTER2_FOREST,
            SlicePhase.CHAPTER2_SHRINE
        )
        exploration.forEach { phase ->
            assertTrue(AmbientBarks.idleBarks(phase).isNotEmpty(), "$phase should have idle barks")
            assertTrue(AmbientBarks.pickIdle(phase, Random(0)) != null, "$phase should pick an idle bark")
        }
    }

    @Test
    fun explorationPoolsAreNonEmpty() {
        assertTrue(AmbientBarks.SEWER_ENTRY.isNotEmpty())
        assertTrue(AmbientBarks.SEWER_ATMOSPHERE.isNotEmpty())
        assertTrue(AmbientBarks.ENEMY_WARNING.isNotEmpty())
        assertTrue(AmbientBarks.FOREST_WARNING.isNotEmpty())
        assertTrue(AmbientBarks.BOSS_DISCOVERY.isNotEmpty())
    }
}
