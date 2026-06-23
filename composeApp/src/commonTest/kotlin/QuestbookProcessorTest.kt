import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.RoomContext

class QuestbookProcessorTest {

    private fun tavern(hasTarget: Boolean = true) =
        RoomContext(mapId = "tavern", roomId = RoomContext.ROOM_TAVERN, hasInteractableTarget = hasTarget)

    private fun sewer(room: String, builder: RoomContext.() -> RoomContext = { this }) =
        RoomContext(mapId = "sewer", roomId = room).builder()

    @Test
    fun smellTreasureInTavernRegistersAMandatoryQuestWithMarker() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_SMELL_TREASURE, tavern(hasTarget = true))
        assertTrue(r.questbookText.contains("Official Quest Registered"))
        assertTrue(r.effect is QuestbookEffect.SpawnQuestMarker)
        assertEquals(QuestPressure.LOW, qb.pressure)
    }

    @Test
    fun smellTreasureFailsafeMarksNibHerself() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_SMELL_TREASURE, tavern(hasTarget = false))
        assertTrue(r.questbookText.contains("Source: Self"))
        assertEquals("Nib", (r.effect as QuestbookEffect.SpawnQuestMarker).targetHint)
    }

    @Test
    fun smellTreasureInBossRoomIsFlavorOnly() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_SMELL_TREASURE, sewer(RoomContext.ROOM_BOSS))
        assertTrue(r.questbookText.contains("Filing Cabinet"))
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun itWasntMeRaisesPressureAndSpawnsFalseMarker() {
        val qb = QuestbookProcessor()
        val r = qb.process(BarkEvent.NIB_IT_WASNT_ME, sewer(RoomContext.ROOM_SEWER_CORRIDOR))
        assertEquals(QuestPressure.LOW, r.pressureBefore)
        assertEquals(QuestPressure.MEDIUM, r.pressureAfter)
        assertTrue(r.effect is QuestbookEffect.SpawnFalseMarker)
    }

    @Test
    fun pressureEscalatesLowMediumHighThenCaps() {
        val qb = QuestbookProcessor()
        val ctx = sewer(RoomContext.ROOM_SEWER_CORRIDOR)
        qb.process(BarkEvent.NIB_IT_WASNT_ME, ctx) // LOW -> MEDIUM
        assertEquals(QuestPressure.MEDIUM, qb.pressure)
        qb.process(BarkEvent.NIB_IT_WASNT_ME, ctx) // MEDIUM -> HIGH
        assertEquals(QuestPressure.HIGH, qb.pressure)
        // At HIGH, a PRESSURE bark degrades to flavor only and pressure is capped.
        val capped = qb.process(BarkEvent.NIB_IT_WASNT_ME, ctx)
        assertEquals(QuestbookEffect.FlavorText, capped.effect)
        assertEquals(QuestPressure.HIGH, qb.pressure)
        assertFalse(capped.pressureChanged)
    }

    @Test
    fun knowledgeFailsafeWhenNoPuzzle() {
        val qb = QuestbookProcessor()
        val r = qb.process(
            BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER,
            sewer(RoomContext.ROOM_MINI_DUNGEON) { copy(hasPuzzleElement = false) }
        )
        assertTrue(r.questbookText.contains("No findings"))
        assertEquals(QuestbookEffect.FlavorText, r.effect)
    }

    @Test
    fun bruggAttackClearsBreakableObstacle() {
        val qb = QuestbookProcessor()
        val r = qb.process(
            BarkEvent.BRUGG_ATTACK,
            sewer(RoomContext.ROOM_MINI_DUNGEON) { copy(hasBreakableObstacle = true) }
        )
        assertTrue(r.questbookText.contains("Demolition Permit"))
        assertTrue(r.effect is QuestbookEffect.ClearObstacle)
    }

    @Test
    fun nameRegistrationFiresInBossRoomEvenAtHighPressure() {
        val qb = QuestbookProcessor()
        qb.escalateTo(QuestPressure.HIGH)
        val r = qb.process(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING, sewer(RoomContext.ROOM_BOSS))
        val effect = r.effect
        assertTrue(effect is QuestbookEffect.RegisterPartyName)
        assertEquals("Everything Changes", (effect as QuestbookEffect.RegisterPartyName).name)
    }

    @Test
    fun reactionsAreDeterministic() {
        val a = QuestbookProcessor().process(BarkEvent.NIB_SMELL_TREASURE, tavern())
        val b = QuestbookProcessor().process(BarkEvent.NIB_SMELL_TREASURE, tavern())
        assertEquals(a.questbookText, b.questbookText)
        assertEquals(a.effect, b.effect)
    }

    @Test
    fun mapTransitionResetsPressureAndLog() {
        val qb = QuestbookProcessor()
        qb.process(BarkEvent.NIB_IT_WASNT_ME, sewer(RoomContext.ROOM_SEWER_CORRIDOR))
        assertEquals(QuestPressure.MEDIUM, qb.pressure)
        assertTrue(qb.log.isNotEmpty())
        qb.onMapTransition()
        assertEquals(QuestPressure.LOW, qb.pressure)
        assertTrue(qb.log.isEmpty())
    }
}
