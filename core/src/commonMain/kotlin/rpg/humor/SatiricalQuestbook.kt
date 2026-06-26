package rpg.humor

/**
 * The Questbook's signature joke as data (docs/COMEDY_BIBLE.md): a harmless need
 * is "accepted" and then radicalised into a bureaucratic/authoritarian/market
 * objective. The system is always *understandably wrong*.
 *
 * Pure content -- the renderer streams these as QUEST ACCEPTED / OBJECTIVE
 * UPDATED popups. Kept in :core so it is shared, testable, and easy to extend.
 */
data class SatiricalQuest(
    val accepted: String,
    val objectiveUpdate: String,
    /** True if the bit ends on FAILED rather than a radicalised UPDATE. */
    val failed: Boolean = false
)

object SatiricalQuestbook {

    /** The core joke in pure form: every harmless wish, radicalised. */
    val radicalizedNeeds: List<SatiricalQuest> = listOf(
        SatiricalQuest("CENTRALIZE ALL OUTCOMES", "...because someone wanted fairness."),
        SatiricalQuest("MONETIZE ALL CHOICES", "...because someone wanted freedom."),
        SatiricalQuest("ARREST ALL MOVEMENT", "...because someone wanted safety."),
        SatiricalQuest("INSTALL PROPHET", "...because someone wanted meaning."),
        SatiricalQuest("SILENCE DISAGREEMENT", "...because someone wanted peace.")
    )

    /** Stand-alone easter-egg popups to scatter (one per map, sparingly). */
    val easterEggs: List<SatiricalQuest> = listOf(
        SatiricalQuest("TOUCH GRASS", "GRASS REQUIRES LOGIN"),
        SatiricalQuest("READ THE TERMS", "NO MORTAL LIFESPAN DETECTED", failed = true),
        SatiricalQuest("BECOME BASED", "DEFINE BASED WITHOUT CRYING"),
        SatiricalQuest("DEFEND TRADITION", "SELECT A TRADITION FROM PATCH NOTES"),
        SatiricalQuest("OVERTHROW THE ELITES", "INSTALL NEW ELITES"),
        SatiricalQuest("TRUST THE PLAN", "THE PLAN HAS LEFT THE SERVER"),
        SatiricalQuest("FOLLOW THE SCIENCE", "READ BEYOND THE HEADLINE"),
        SatiricalQuest("FOLLOW YOUR HEART", "YOUR HEART HAS SPONSORS"),
        SatiricalQuest("SAVE THE CHILDREN", "USE CHILDREN AS ARGUMENT SHIELDS"),
        SatiricalQuest("RESTORE COMMON SENSE", "COMMON SENSE NOT FOUND IN INVENTORY"),
        SatiricalQuest("UNITE THE PEOPLE", "EXCLUDE NECESSARY PEOPLE"),
        SatiricalQuest("END ALL WAR", "PREPARE FINAL WAR"),
        SatiricalQuest("ADD MEANINGFUL CONTENT", "PLACE THREE BARRELS"),
        SatiricalQuest("CLARIFY YOUR VALUES AGAIN", "(Nib: Absolutely not.)")
    )

    /** Every satirical line, for tooling/tests. */
    fun all(): List<SatiricalQuest> = radicalizedNeeds + easterEggs
}
