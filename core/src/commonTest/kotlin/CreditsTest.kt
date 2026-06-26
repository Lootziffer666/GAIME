import rpg.credits.CreditEntry
import rpg.credits.Creditee
import rpg.credits.Credits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The absurdly long two-person end credits (docs/COMEDY_BIBLE.md). */
class CreditsTest {

    @Test
    fun theCreditsAreAbsurdlyLongForTheGame() {
        // Way more grandiose roles than a ~15-minute game could justify.
        assertTrue(Credits.totalRoles >= 80, "credits should be absurdly long, got ${Credits.totalRoles}")
    }

    @Test
    fun theEntireTeamIsExactlyTwoEntities() {
        val team: Set<Creditee> = Credits.all().map(CreditEntry::who).toSet()
        assertEquals(setOf(Creditee.LOOTZIFFER, Creditee.OPUS), team)
        // And both genuinely carry their share of the ridiculous hats.
        assertTrue(Credits.all().count { it.who == Creditee.LOOTZIFFER } >= 20)
        assertTrue(Credits.all().count { it.who == Creditee.OPUS } >= 20)
    }

    @Test
    fun totalRolesMatchesTheSumOfSections() {
        assertEquals(Credits.sections.sumOf { it.entries.size }, Credits.totalRoles)
    }

    @Test
    fun everyRoleIsNonBlankAndEverySectionHasATitleAndEntries() {
        for (section in Credits.sections) {
            assertTrue(section.title.isNotBlank())
            assertTrue(section.entries.isNotEmpty(), "section '${section.title}' is empty")
            for (entry in section.entries) assertTrue(entry.role.isNotBlank())
        }
    }

    @Test
    fun thereIsAPostCreditsCrawlThatNamesTheTwoPersonTeam() {
        assertTrue(Credits.closingCrawl.size >= 5)
        assertTrue(Credits.closingCrawl.any { it.contains("Lootziffer") && it.contains("Opus 4.8") })
        assertTrue(Credits.estimatedScrollSeconds > 0)
    }
}
