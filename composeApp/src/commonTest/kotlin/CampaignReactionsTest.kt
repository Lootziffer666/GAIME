import rpg.bark.BarkEvent
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.RoomContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Reactions added for Chapters 2-5 + Finale (docs/CAMPAIGN.md). */
class CampaignReactionsTest {

    private fun room(
        map: String,
        roomId: String,
        builder: RoomContext.() -> RoomContext = { this }
    ) = RoomContext(mapId = map, roomId = roomId).builder()

    @Test
    fun smellDragonIsTreatedAsADefectReportAndForcesHighPressure() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_SMELL_DRAGON, room("island_cave", "cave"))
        assertTrue(r.questbookText.contains("DEFEAT THE DRAGON"))
        assertTrue(r.effect is QuestbookEffect.SpawnQuestMarker)
        assertEquals(QuestPressure.HIGH, r.pressureAfter)
        assertEquals(QuestPressure.HIGH, qb.pressure)
    }

    @Test
    fun anotherBarrelOpensAnOptionalInspectEveryBarrelQuest() {
        val qb = QuestbookProcessor()
        val r = qb.process(
            BarkEvent.NIB_ANOTHER_BARREL,
            room("harbour", "ship") { copy(hasContainer = true) }
        )
        assertTrue(r.questbookText.contains("Inspect Every Barrel"))
        assertTrue(r.effect is QuestbookEffect.SpawnQuestMarker)
        assertEquals(QuestPressure.MEDIUM, r.pressureAfter)
    }

    @Test
    fun retreatIsDeniedByPaperworkInsideABossRoom() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.BRUGG_RETREAT, room("harbour", RoomContext.ROOM_BOSS))
        assertTrue(r.questbookText.contains("Retreat denied by paperwork"))
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun secretEntranceIsReclassifiedAsAPublicRightOfWay() {
        val qb = QuestbookProcessor()
        val r = qb.process(
            BarkEvent.NIB_SECRET_ENTRANCE,
            room("woods", "trail") { copy(hasPuzzleElement = true) }
        )
        assertTrue(r.questbookText.contains("Public Right of Way"))
        assertEquals(QuestbookEffect.RevealHidden, r.effect)
    }

    @Test
    fun shipRiggingBarksProduceDefinedFlavorEffects() {
        val qb = QuestbookProcessor()
        val anchor = qb.process(BarkEvent.BRUGG_DROP_ANCHOR, room("harbour", "deck"))
        assertTrue(anchor.questbookText.contains("Mooring Logged"))
        assertEquals(QuestbookEffect.FlavorText, anchor.effect)

        val underway = qb.process(BarkEvent.BRUGG_LETS_BE_UNDERWAY, room("harbour", "deck"))
        assertTrue(underway.questbookText.contains("Heading: Reverse"))
    }

    @Test
    fun falseOrdersInChapterFiveRaisePressureButStayLocalFlavor() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.BRUGG_HOLD_THE_LINE, room("island_cave", "cave"))
        assertEquals(QuestPressure.MEDIUM, r.pressureAfter)
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun newCampaignReactionsAreDeterministic() {
        val a = QuestbookProcessor().process(BarkEvent.NIB_SMELL_DRAGON, room("island_cave", "cave"))
        val b = QuestbookProcessor().process(BarkEvent.NIB_SMELL_DRAGON, room("island_cave", "cave"))
        assertEquals(a.questbookText, b.questbookText)
        assertEquals(a.effect, b.effect)
        assertEquals(a.pressureAfter, b.pressureAfter)
    }
}
