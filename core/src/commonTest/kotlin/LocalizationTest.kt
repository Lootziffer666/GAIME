import rpg.Chapter
import rpg.CampaignBoss
import rpg.QuestbookPage
import rpg.bark.BarkEvent
import rpg.combat.EnemyArchetype
import rpg.i18n.GameLocale
import rpg.i18n.Locale
import rpg.i18n.Localizer
import rpg.i18n.displayText
import rpg.i18n.localized
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookProcessor
import rpg.questbook.RoomContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Verifies the complete German localization of game content (docs/SONGBOOK.md
 *  notwithstanding: audio stays English). */
class LocalizationTest {

    @AfterTest
    fun reset() { GameLocale.current = Locale.EN }

    @Test
    fun englishLocaleReturnsTheCanonicalStringUnchanged() {
        val s = "Demolition Permit Issued: Immediate Effect"
        assertEquals(s, Localizer.localize(s, Locale.EN))
    }

    @Test
    fun unknownStringFallsBackToEnglishGracefully() {
        val s = "Totally unknown string #42"
        assertEquals(s, Localizer.localize(s, Locale.DE))
    }

    @Test
    fun everyChapterTitleHasAGermanTranslation() {
        for (chapter in Chapter.entries) {
            assertTrue(Localizer.hasGerman(chapter.title), "Missing DE for chapter '${chapter.title}'")
            assertNotEquals(chapter.title, Localizer.localize(chapter.title, Locale.DE))
        }
    }

    @Test
    fun everyBossNameHasAGermanTranslation() {
        for (boss in CampaignBoss.entries) {
            assertTrue(Localizer.hasGerman(boss.displayName), "Missing DE for boss '${boss.displayName}'")
        }
    }

    @Test
    fun everyEnemyNameHasAGermanTranslation() {
        for (enemy in EnemyArchetype.entries) {
            assertTrue(Localizer.hasGerman(enemy.displayName), "Missing DE for enemy '${enemy.displayName}'")
        }
    }

    @Test
    fun everyQuestbookPageHasAGermanTranslation() {
        for (page in QuestbookPage.entries) {
            assertTrue(Localizer.hasGerman(page.displayTitle), "Missing DE for page '${page.displayTitle}'")
        }
    }

    @Test
    fun questPressureLabelsAreLocalized() {
        assertEquals("NIEDRIG", Localizer.pressureLabel("LOW", Locale.DE))
        assertEquals("MITTEL", Localizer.pressureLabel("MEDIUM", Locale.DE))
        assertEquals("HOCH", Localizer.pressureLabel("HIGH", Locale.DE))
        assertEquals("LOW", Localizer.pressureLabel("LOW", Locale.EN))
    }

    @Test
    fun signatureQuestbookReactionsAreLocalizedThroughTheRealPipeline() {
        // Drive the real Questbook and assert no English leaks in DE for a
        // representative set of branches.
        val qb = QuestbookProcessor()
        val dragon = qb.process(BarkEvent.NIB_SMELL_DRAGON, RoomContext("island_cave", "cave"))
        assertEquals("DRINGENDE QUEST ANGENOMMEN: BESIEGE DEN DRACHEN", dragon.displayText(Locale.DE))
        assertTrue(Localizer.hasGerman(dragon.questbookText))

        val treasure = QuestbookProcessor().process(
            BarkEvent.NIB_SMELL_TREASURE,
            RoomContext("tavern", RoomContext.ROOM_TAVERN, hasInteractableTarget = true)
        )
        assertTrue(treasure.displayText(Locale.DE).startsWith("Offizielle Quest registriert"))
    }

    @Test
    fun globalLocaleHolderDrivesTheExtensionHelper() {
        GameLocale.current = Locale.DE
        assertEquals("Atmosphärische Beobachtung vermerkt", "Atmospheric observation noted".localized())
        GameLocale.current = Locale.EN
        assertEquals("Atmospheric observation noted", "Atmospheric observation noted".localized())
    }

    @Test
    fun pressureEnumValuesAllHaveGermanLabels() {
        for (p in QuestPressure.entries) {
            assertNotEquals(p.name, Localizer.pressureLabel(p.name, Locale.DE))
        }
    }
}
