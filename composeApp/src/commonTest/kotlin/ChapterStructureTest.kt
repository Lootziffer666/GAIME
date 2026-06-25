import rpg.CampaignBoss
import rpg.Chapter
import rpg.QuestbookPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies the canonical campaign structure (docs/CAMPAIGN.md). */
class ChapterStructureTest {

    @Test
    fun campaignHasSevenStagesInOrder() {
        val order = Chapter.entries.sortedBy { it.order }.map { it.order }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), order)
        assertEquals(Chapter.PROLOGUE, Chapter.ofOrder(0))
        assertEquals(Chapter.FINALE, Chapter.ofOrder(6))
    }

    @Test
    fun chaptersChainViaNextUntilFinale() {
        var chapter: Chapter? = Chapter.PROLOGUE
        val visited = mutableListOf<Chapter>()
        while (chapter != null) {
            visited.add(chapter)
            chapter = chapter.next()
        }
        assertEquals(Chapter.entries.size, visited.size)
        assertEquals(Chapter.FINALE, visited.last())
        assertNull(Chapter.FINALE.next())
    }

    @Test
    fun bossChaptersMatchCampaignBosses() {
        val bossChapters = Chapter.withBoss()
        assertEquals(
            listOf(
                CampaignBoss.RAT_ACCOUNTANT,
                CampaignBoss.HELPFUL_TREE,
                CampaignBoss.CAPTAIN_FORMBEARD,
                CampaignBoss.ADMINISTRAGON
            ),
            bossChapters.map { it.boss }
        )
        // Prologue and the town-guard chapter have no combat boss.
        assertNull(Chapter.PROLOGUE.boss)
        assertNull(Chapter.CH2_TOWN_GUARD.boss)
    }

    @Test
    fun fourPageBearingChaptersRecoverDistinctPages() {
        val pages = Chapter.entries.mapNotNull { it.pageReward }
        assertEquals(4, pages.size)
        assertEquals(pages.toSet().size, pages.size, "pages must be distinct")
        assertTrue(pages.contains(QuestbookPage.PAGE_OF_BEGINNINGS))
        assertTrue(pages.contains(QuestbookPage.PAGE_OF_CLAIMS_AND_REWARDS))
    }
}
