import rpg.bark.BarkEvent
import rpg.finale.OverloadState
import rpg.finale.QuestbookOverload
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.QuestbookReaction
import rpg.questbook.RoomContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The Finale "System Overload" climax mechanic (docs/CAMPAIGN.md, Phase 4). */
class QuestbookOverloadTest {

    private fun questMarkerReaction(hint: String, pressure: QuestPressure) =
        QuestbookReaction(
            bark = BarkEvent.NIB_THIS_LOOKS_LIKE_GOLD,
            questbookText = "QUEST ACCEPTED: $hint",
            effect = QuestbookEffect.SpawnQuestMarker(hint),
            pressureBefore = pressure,
            pressureAfter = pressure
        )

    @Test
    fun sevenDistinctContradictoryQuestsAtHighPressureCollapseTheBook() {
        val overload = QuestbookOverload()
        val hints = listOf("privvy", "horse", "roast", "authority", "gold", "treasure", "chest")
        var state: OverloadState = OverloadState.Stable
        hints.forEach { state = overload.offer(questMarkerReaction(it, QuestPressure.HIGH)) }

        assertTrue(overload.isCollapsed)
        val collapsed = assertIs<OverloadState.Collapsed>(state)
        assertEquals(7, collapsed.quests.size)
    }

    @Test
    fun overloadDoesNotBuildBelowHighPressure() {
        val overload = QuestbookOverload()
        repeat(10) { overload.offer(questMarkerReaction("quest_$it", QuestPressure.MEDIUM)) }
        assertEquals(OverloadState.Stable, overload.state)
        assertFalse(overload.isCollapsed)
    }

    @Test
    fun duplicateQuestsDoNotDoubleCount() {
        val overload = QuestbookOverload()
        repeat(10) { overload.offer(questMarkerReaction("privvy", QuestPressure.HIGH)) }
        val state = overload.state
        assertIs<OverloadState.Building>(state)
        assertEquals(1, state.accepted)
        assertFalse(overload.isCollapsed)
    }

    @Test
    fun flavorReactionsNeverContributeToOverload() {
        val overload = QuestbookOverload()
        val flavor = QuestbookReaction(
            bark = BarkEvent.VELLUM_REEKS_OF_DEATH,
            questbookText = "Atmospheric observation noted",
            effect = QuestbookEffect.FlavorText,
            pressureBefore = QuestPressure.HIGH,
            pressureAfter = QuestPressure.HIGH
        )
        repeat(10) { overload.offer(flavor) }
        assertEquals(OverloadState.Stable, overload.state)
    }

    @Test
    fun resetClearsAccumulatedOverload() {
        val overload = QuestbookOverload()
        overload.offer(questMarkerReaction("privvy", QuestPressure.HIGH))
        assertIs<OverloadState.Building>(overload.state)
        overload.reset()
        assertEquals(OverloadState.Stable, overload.state)
        assertTrue(overload.pendingQuests.isEmpty())
    }

    @Test
    fun realFinaleBarksDriveTheOverloadThroughThePipeline() {
        // Wire the real Questbook to the overload, as a finale director would.
        val qb = QuestbookProcessor()
        qb.escalateTo(QuestPressure.HIGH)
        val overload = QuestbookOverload()
        val ctx = RoomContext(mapId = "island_cave", roomId = "cave")

        val finaleBarks = listOf(
            BarkEvent.NIB_WHERES_THE_PRIVVY,
            BarkEvent.NIB_IS_THAT_ROAST,
            BarkEvent.NIB_NOT_A_HORSE,
            BarkEvent.NIB_WHO_RUNS_THIS_CITY,
            BarkEvent.NIB_THIS_LOOKS_LIKE_GOLD,
            BarkEvent.NIB_THIS_LOOKS_LIKE_TREASURE,
            BarkEvent.NIB_CHEST_ALMOST_UNLOCKED // no container -> QUEST ACCEPTED: OPEN THE CHEST
        )
        finaleBarks.forEach { overload.offer(qb.process(it, ctx)) }

        assertTrue(overload.isCollapsed, "the book should crash under contradiction")
        assertEquals(QuestbookOverload.COLLAPSE_VERDICT, "GAME OVER")
    }
}
