import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import rpg.BarkOutcome
import rpg.SliceDirector
import rpg.bark.BarkEvent
import rpg.bark.BarkRegistry
import rpg.bark.BarkType
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.RoomContext

/**
 * End-to-end check of the vertical slice's Acceptance Criteria: a bark fires,
 * the Questbook reacts with text, a visible effect is produced, the trace
 * Bark -> Questbook -> Effect holds, at least one UTILITY_BARK is used, and
 * Quest Pressure escalates LOW -> MEDIUM -> HIGH across the slice.
 */
class SlicePipelineTest {

    private var now = 0L

    @Test
    fun fullSlicePipelineIsTraceableAndEscalates() {
        val director = SliceDirector { now }
        val firedBarks = mutableListOf<BarkEvent>()

        // 1. Tavern: Nib smells treasure -> mandatory quest + marker on cellar door.
        director.enterRoom(
            RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true)
        )
        val tavern = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)
        assertTrue(tavern is BarkOutcome.Fired)
        val tavernReaction = (tavern as BarkOutcome.Fired).reaction
        firedBarks += tavernReaction.bark
        // Traceability: same bark in, text + visible effect out.
        assertEquals(BarkEvent.NIB_SMELL_TREASURE, tavernReaction.bark)
        assertTrue(tavernReaction.questbookText.contains("Official Quest Registered"))
        assertTrue(tavernReaction.effect is QuestbookEffect.SpawnQuestMarker)
        assertTrue(director.questMarkers.isNotEmpty())
        assertEquals(QuestPressure.LOW, director.pressure)

        // 2/3. Map transition into the sewer resets local state; corridor combat
        // ends and Nib denies everything -> pressure LOW -> MEDIUM, false marker.
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_SEWER_CORRIDOR))
        assertTrue(director.questMarkers.isEmpty(), "markers are map-local")
        now += 60_000
        val denial = director.fireBark(BarkEvent.NIB_IT_WASNT_ME)
        assertTrue(denial is BarkOutcome.Fired)
        firedBarks += BarkEvent.NIB_IT_WASNT_ME
        assertEquals(QuestPressure.MEDIUM, director.pressure)
        assertTrue(director.falseMarkers.isNotEmpty())

        // 4. Mini-dungeon: a UTILITY_BARK intentionally clears the rubble.
        director.enterRoom(
            RoomContext(
                "sewer", RoomContext.ROOM_MINI_DUNGEON,
                hasPuzzleElement = true, hasBreakableObstacle = true
            )
        )
        now += 60_000
        director.fireBark(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER)
        now += 60_000
        val demolition = director.fireBark(BarkEvent.BRUGG_ATTACK)
        assertTrue(demolition is BarkOutcome.Fired)
        firedBarks += BarkEvent.BRUGG_ATTACK
        assertTrue((demolition as BarkOutcome.Fired).reaction.effect is QuestbookEffect.ClearObstacle)
        assertTrue(director.clearedObstacles.isNotEmpty())

        // Escalate to HIGH via a major misinterpretation (still same map).
        now += 100_000
        director.fireBark(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
        assertEquals(QuestPressure.HIGH, director.pressure)

        // 6. Boss room: the name-registration story beat fires even at HIGH.
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS))
        now += 100_000
        val naming = director.fireBark(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
        assertTrue(naming is BarkOutcome.Fired)
        assertEquals("Everything Changes", director.partyName)
        assertTrue(director.questbookOpen)

        // --- Acceptance assertions ---
        // At least one UTILITY_BARK was used intentionally.
        assertTrue(firedBarks.any { BarkRegistry[it].type == BarkType.UTILITY_BARK })
        // Pressure escalated all the way to HIGH.
        assertEquals(QuestPressure.HIGH, director.pressure)
    }

    @Test
    fun barkOnCooldownDoesNotReachTheQuestbook() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(
            RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true)
        )
        assertTrue(director.fireBark(BarkEvent.NIB_SMELL_TREASURE) is BarkOutcome.Fired)
        // Immediately again -> suppressed (30s cooldown).
        val suppressed = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)
        assertTrue(suppressed is BarkOutcome.Suppressed)
    }

    @Test
    fun combatBossBarkRoutesThroughQuestbook() {
        now = 0L
        val director = SliceDirector { now }
        director.enterRoom(RoomContext("sewer", RoomContext.ROOM_BOSS))
        val party = listOf(
            rpg.combat.Combatant("h1", "Nib", 200, rpg.combat.Side.PLAYER, 4),
            rpg.combat.Combatant("h2", "Brugg", 200, rpg.combat.Side.PLAYER, 4),
            rpg.combat.Combatant("h3", "Vellum", 200, rpg.combat.Side.PLAYER, 4)
        )
        val boss = rpg.combat.EnemyArchetype.RAT_ACCOUNTANT.spawn("rat_accountant")
        director.startCombat(rpg.combat.CombatEngine(party, emptyList(), boss, rpg.combat.BossController()))

        // Burn adds, then whittle the boss into Phase 2 (pressure -> HIGH) and beyond.
        director.combatAction(rpg.combat.CombatAction.UtilityBark(BarkEvent.VELLUM_CALLS_FOR_FLAME))
        repeat(5) { director.combatAction(rpg.combat.CombatAction.Attack("rat_accountant")) }

        // Phase 2 escalated pressure systemically, and the boss log recorded reactions.
        assertEquals(QuestPressure.HIGH, director.pressure)
        assertNotNull(director.questbook.log.firstOrNull { it.bark == BarkEvent.NIB_SMELL_TREASURE })
    }
}
