import rpg.bark.BarkEvent
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.RoomContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Reactions added for Chapters 3-5 + Finale (docs/CAMPAIGN.md). */
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
    fun retreatIsDeniedByPaperworkInsideABossRoom() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.BRUGG_RETREAT, room("harbour", RoomContext.ROOM_BOSS))
        assertTrue(r.questbookText.contains("Retreat denied by paperwork"))
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun retreatOutsideABossRoomIsJustLogged() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.BRUGG_RETREAT, room("harbour", "deck"))
        assertTrue(r.questbookText.contains("Tactical Withdrawal"))
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun shipRiggingBarksProduceDefinedFlavorEffects() {
        val qb = QuestbookProcessor()
        val anchor = qb.process(BarkEvent.BRUGG_DROP_ANCHOR, room("harbour", "deck"))
        assertTrue(anchor.questbookText.contains("Mooring Logged"))
        assertEquals(QuestbookEffect.FlavorText, anchor.effect)
    }

    @Test
    fun mapBarkRevealsCartographicAssetWhenTargetPresent() {
        val qb = QuestbookProcessor()
        val r = qb.process(
            BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_MAP,
            room("woods", "trail") { copy(hasInteractableTarget = true) }
        )
        assertTrue(r.questbookText.contains("Cartographic Asset"))
        assertEquals(QuestbookEffect.RevealHidden, r.effect)
    }

    @Test
    fun falseOrdersInChapterFiveRaisePressureButStayLocalFlavor() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.BRUGG_HOLD_THE_LINE, room("island_cave", "cave"))
        assertEquals(QuestPressure.MEDIUM, r.pressureAfter)
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun finaleBanalBarkAcceptsAContradictoryQuest() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_NOT_A_HORSE, room("questbook", "finale"))
        assertTrue(r.questbookText.contains("IDENTIFY THE HORSE"))
        assertTrue(r.effect is QuestbookEffect.SpawnQuestMarker)
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
