package rpg.duel

import rpg.duel.InsultType.AUTHORITY
import rpg.duel.InsultType.BUREAUCRACY
import rpg.duel.InsultType.COWARDICE
import rpg.duel.InsultType.GRAMMAR
import rpg.duel.InsultType.HEROISM
import rpg.duel.InsultType.INTELLIGENCE
import rpg.duel.InsultType.LOOT
import rpg.duel.InsultType.SMELL

/**
 * The Chapter 2 insult-duel content (docs/COMEDY_BIBLE.md). Each builder returns
 * a fresh [InsultDuel] so it can be replayed. In every round exactly one option
 * is the correct counter; the wrong options are deliberately funny party barks.
 */
object DuelLibrary {

    /** Tutorial duel at the guardhouse door. Win to be granted access. */
    fun guardClerkDuel(): InsultDuel = InsultDuel(
        opponentName = "Guard Clerk",
        rounds = listOf(
            DuelRound(
                Insult("You lot look like heroes assembled from rejected tavern furniture.", HEROISM),
                listOf(
                    Counter("Then your guardhouse must be the matching drawer.", HEROISM, "Brugg"),
                    Counter("I smell sewage.", SMELL, "Nib"),
                    Counter("Furniture has better benefits.", BUREAUCRACY, "Vellum")
                )
            ),
            DuelRound(
                Insult("Your rogue looks like he was pickpocketed by his own shadow.", LOOT),
                listOf(
                    Counter("It wasn't me.", COWARDICE, "Nib"),
                    Counter("My shadow and I have a complicated revenue agreement.", LOOT, "Nib"),
                    Counter("Shadows are vulnerable to lightning.", INTELLIGENCE, "Vellum")
                )
            ),
            DuelRound(
                Insult("Your mage smells of old books and recent panic.", INTELLIGENCE),
                listOf(
                    Counter("Knowledge is the answer.", BUREAUCRACY, "Vellum"),
                    Counter("Panic is simply research with deadlines.", INTELLIGENCE, "Vellum"),
                    Counter("This calls for flame.", AUTHORITY, "Vellum")
                )
            )
        )
    )

    /**
     * The Guard Captain boss phase ("Warden's Mandate"). Run as a phase of the
     * fight: landing all counters destabilises the mandate and makes the boss
     * vulnerable (docs/CAMPAIGN.md).
     */
    fun guardCaptainDuel(): InsultDuel = InsultDuel(
        opponentName = "The Guard Captain Who Cannot Legally Move",
        rounds = listOf(
            DuelRound(
                Insult("I have arrested myself to preserve order. You would not understand discipline.", AUTHORITY),
                listOf(
                    Counter("Discipline is not the same as tying your own boots together.", AUTHORITY, "Brugg"),
                    Counter("I smell treasure.", LOOT, "Nib"),
                    Counter("Drop your weapons and surrender.", BUREAUCRACY, "Brugg")
                )
            ),
            DuelRound(
                Insult("You trespassers are a threat to lawful stillness.", BUREAUCRACY),
                listOf(
                    Counter("Lawful stillness is what cowards call furniture.", BUREAUCRACY, "Brugg"),
                    Counter("We are out maneuvered.", COWARDICE, "Brugg"),
                    Counter("This looks like a passage.", SMELL, "Nib")
                )
            ),
            DuelRound(
                Insult("I command this room without taking a single step.", AUTHORITY),
                listOf(
                    Counter("Then the room is leading by example.", GRAMMAR, "Vellum"),
                    Counter("That is because your legs require approval.", AUTHORITY, "Nib"),
                    Counter("What are your orders?", BUREAUCRACY, "Brugg")
                )
            )
        )
    )

    /** All Chapter 2 duels (for iteration/validation). */
    fun all(): List<InsultDuel> = listOf(guardClerkDuel(), guardCaptainDuel())
}
