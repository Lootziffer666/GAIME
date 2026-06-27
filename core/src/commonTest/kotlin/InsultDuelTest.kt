import rpg.duel.DuelLibrary
import rpg.duel.DuelOutcome
import rpg.duel.InsultDuel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Officially Sanctioned Insult Duel mini-system (docs/COMEDY_BIBLE.md). */
class InsultDuelTest {

    private fun playAllCorrect(duel: InsultDuel) {
        while (duel.currentRound() != null) {
            duel.respond(duel.currentRound()!!.correctOption)
        }
    }

    @Test
    fun chapter2DuelContentIsStructurallyValid() {
        // DuelRound's init enforces exactly one correct counter per round;
        // building the whole library must not throw.
        val duels = DuelLibrary.all()
        assertTrue(duels.isNotEmpty())
        for (duel in duels) {
            for (round in duel.rounds) {
                assertEquals(
                    1, round.options.count { it.counters == round.insult.type },
                    "round '${round.insult.text}' must have exactly one correct counter"
                )
            }
        }
    }

    @Test
    fun landingEveryCounterDestabilizesTheMandate() {
        val duel = DuelLibrary.guardCaptainDuel()
        playAllCorrect(duel)
        assertEquals(DuelOutcome.WON, duel.outcome)
        assertEquals(0, duel.opponentDignity)
        assertEquals(0, duel.missteps)
    }

    @Test
    fun aWrongAnswerCostsTheProceedingAndIsCounted() {
        val duel = DuelLibrary.guardClerkDuel()
        val first = duel.currentRound()!!
        val wrong = first.options.first { it.counters != first.insult.type }
        val result = duel.respond(wrong)
        assertFalse(result.landed)
        assertEquals(duel.startingDignity, duel.opponentDignity) // unmoved
        assertEquals(1, duel.missteps)

        // Finish with correct answers; with one miss the mandate holds.
        playAllCorrect(duel)
        assertEquals(DuelOutcome.LOST, duel.outcome)
    }

    @Test
    fun landedCounterReportsTheBureaucraticLine() {
        val duel = DuelLibrary.guardClerkDuel()
        val r1 = duel.respond(duel.currentRound()!!.correctOption)
        assertTrue(r1.landed)
        assertEquals(InsultDuel.COUNTER_ACCEPTED, r1.questbookLine)
        assertEquals(duel.startingDignity - 1, r1.opponentDignity)
    }

    @Test
    fun finalLandedCounterAnnouncesDestabilization() {
        val duel = DuelLibrary.guardClerkDuel()
        lateinit var last: rpg.duel.RoundResult
        while (duel.currentRound() != null) {
            last = duel.respond(duel.currentRound()!!.correctOption)
        }
        assertEquals(InsultDuel.MANDATE_DESTABILIZED, last.questbookLine)
    }

    @Test
    fun respondingAfterTheDuelIsOverIsSafe() {
        val duel = DuelLibrary.guardClerkDuel()
        playAllCorrect(duel)
        assertNull(duel.currentRound())
        val after = duel.respond(duel.rounds.first().correctOption)
        assertEquals(InsultDuel.DUEL_ALREADY_OVER, after.questbookLine)
        assertEquals(DuelOutcome.WON, duel.outcome) // unchanged
    }
}
