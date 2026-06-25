import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.RoomContext

class Chapter2QuestbookTest {

    private fun market() =
        RoomContext(mapId = "stokeport_market", roomId = RoomContext.ROOM_MARKET, hasInteractableTarget = true)

    private fun forest() =
        RoomContext(mapId = "forest_trail", roomId = RoomContext.ROOM_FOREST, hasEnemies = true)

    private fun shrine() =
        RoomContext(mapId = "forest_trail", roomId = RoomContext.ROOM_FOREST_SHRINE, hasPuzzleElement = true)

    private fun forestBoss() =
        RoomContext(mapId = "forest_trail", roomId = RoomContext.ROOM_FOREST_BOSS)

    @Test
    fun nibSmellGoldInMarketProducesCommercialSurveyWithSpawnQuestMarker() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.NIB_SMELL_GOLD, market())
        assertTrue(reaction.questbookText.contains("Commercial Survey Initiated"))
        assertTrue(reaction.questbookText.contains("Mandatory"))
        assertTrue(reaction.effect is QuestbookEffect.SpawnQuestMarker)
    }

    @Test
    fun nibSmellGoldOutsideMarketIsFlavor() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.NIB_SMELL_GOLD, forest())
        assertTrue(reaction.questbookText.contains("Gold detected"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun bruggWhoRunsThisCityProducesMunicipalInquiryWithFlavorText() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.BRUGG_WHO_RUNS_THIS_CITY, market())
        assertTrue(reaction.questbookText.contains("Municipal Inquiry Filed"))
        assertTrue(reaction.questbookText.contains("Authority Structure Report"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun vellumCreaturesInWoodsProducesWildlifeCensusWithSpawnQuestMarker() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.VELLUM_CREATURES_IN_WOODS, forest())
        assertTrue(reaction.questbookText.contains("Wildlife Census Ordered"))
        assertTrue(reaction.questbookText.contains("Immediate"))
        assertTrue(reaction.effect is QuestbookEffect.SpawnQuestMarker)
    }

    @Test
    fun vellumCallsForLightningInShrineProducesElectricalWorksPermitWithRevealHidden() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING, shrine())
        assertTrue(reaction.questbookText.contains("Electrical Works Permit"))
        assertTrue(reaction.questbookText.contains("Granted"))
        assertTrue(reaction.effect is QuestbookEffect.RevealHidden)
    }

    @Test
    fun vellumCallsForLightningOutsideShrineIsFlavor() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING, forest())
        assertTrue(reaction.questbookText.contains("No valid conductor"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun vellumBalanceLifeDeathProducesExistentialRiskAssessmentWithFlavorText() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.VELLUM_BALANCE_LIFE_DEATH, forestBoss())
        assertTrue(reaction.questbookText.contains("Existential Risk Assessment"))
        assertTrue(reaction.questbookText.contains("Philosophical"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun vellumElementsMineToCommandProducesElementalClaimWithFlavorText() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.VELLUM_ELEMENTS_MINE_TO_COMMAND, shrine())
        assertTrue(reaction.questbookText.contains("Elemental Claim Registered"))
        assertTrue(reaction.questbookText.contains("Undefined"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun bruggDropYourWeaponsInBossRoomProducesSurrenderFiling() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.BRUGG_DROP_YOUR_WEAPONS, forestBoss())
        assertTrue(reaction.questbookText.contains("Surrender Filing: Rejected"))
        assertTrue(reaction.questbookText.contains("Insufficient Paperwork"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun bruggDropYourWeaponsOutsideBossRoomIsAtmosphericFlavor() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.BRUGG_DROP_YOUR_WEAPONS, market())
        assertTrue(reaction.questbookText.contains("Atmospheric"))
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun nibHowMuchIsFlavorText() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.NIB_HOW_MUCH, market())
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun bruggExperienceIsHowWeGrowIsFlavorText() {
        val processor = QuestbookProcessor()
        val reaction = processor.process(BarkEvent.BRUGG_EXPERIENCE_IS_HOW_WE_GROW, forest())
        assertEquals(QuestbookEffect.FlavorText, reaction.effect)
    }

    @Test
    fun merchantAndGuardBarksAreFlavorText() {
        val processor = QuestbookProcessor()
        val merchantBarks = listOf(
            BarkEvent.MERCHANT_SEE_IF_THIS_STRIKES_FANCY,
            BarkEvent.MERCHANT_MAKE_ME_AN_OFFER,
            BarkEvent.MERCHANT_NAME_YOUR_PRICE,
            BarkEvent.GUARD_BACK_ALREADY
        )
        for (bark in merchantBarks) {
            val reaction = processor.process(bark, market())
            assertEquals(QuestbookEffect.FlavorText, reaction.effect, "Expected FlavorText for $bark")
        }
    }
}
