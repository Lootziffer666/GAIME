import rpg.humor.SatiricalQuestbook
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Questbook's "harmless need -> radicalised objective" content. */
class SatiricalQuestbookTest {

    @Test
    fun everySatiricalBitHasBothHalves() {
        for (q in SatiricalQuestbook.all()) {
            assertTrue(q.accepted.isNotBlank(), "accepted line blank")
            assertTrue(q.objectiveUpdate.isNotBlank(), "objective line blank for '${q.accepted}'")
        }
    }

    @Test
    fun theCoreRadicalizedNeedsAndEasterEggsArePresent() {
        assertTrue(SatiricalQuestbook.radicalizedNeeds.size >= 5)
        assertTrue(SatiricalQuestbook.easterEggs.size >= 10)
        // The signature bit must exist.
        assertTrue(SatiricalQuestbook.radicalizedNeeds.any { it.accepted == "ARREST ALL MOVEMENT" })
        assertTrue(SatiricalQuestbook.easterEggs.any { it.accepted == "TOUCH GRASS" })
    }
}
