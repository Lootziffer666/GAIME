package rpg.duel

/**
 * The "Officially Sanctioned Insult Duel" mini-system (Chapter 2). The Guard
 * cannot legally move (it has arrested itself), but contempt remains "fully
 * mobile", so hostility is resolved verbally under
 * *Verbal Engagement Protocol 12-B: Non-Physical Hostility Resolution*.
 *
 * It is deliberately cheap: no free text, no dialogue tree, no AI. The opponent
 * throws a typed [Insult]; the player picks one of a few [Counter]s; the
 * Questbook only checks `option.counters == insult.type`. Health is replaced by
 * OFFICIAL DIGNITY. Get enough correct counters and the mandate destabilises.
 *
 * Pure and deterministic -- lives in :core like the combat engine.
 */
enum class InsultType {
    AUTHORITY, COWARDICE, INTELLIGENCE, SMELL, LOOT, GRAMMAR, HEROISM, BUREAUCRACY
}

/** A typed insult thrown by the opponent. */
data class Insult(val text: String, val type: InsultType)

/** A selectable response. It lands iff [counters] matches the insult's type. */
data class Counter(val text: String, val counters: InsultType, val speaker: String? = null)

/**
 * One exchange: an [insult] and the response [options]. Exactly one option is
 * the correct counter (its [Counter.counters] equals the insult's type); the
 * rest are wrong-but-hopefully-funny.
 */
data class DuelRound(val insult: Insult, val options: List<Counter>) {
    init {
        require(options.size >= 2) { "a duel round needs at least two options" }
        require(options.count { it.counters == insult.type } == 1) {
            "a duel round must have exactly one correct counter (insult '${insult.text}')"
        }
    }

    val correctOption: Counter get() = options.first { it.counters == insult.type }
}

enum class DuelOutcome { ONGOING, WON, LOST }

/** Result of submitting one response. */
data class RoundResult(
    val landed: Boolean,
    val opponentDignity: Int,
    val outcome: DuelOutcome,
    val questbookLine: String
)

/**
 * A runnable insult duel. [startingDignity] correct counters are required to
 * win; each landed counter drops the opponent's OFFICIAL DIGNITY by one, and the
 * mandate destabilises at zero. Missteps are recorded (the renderer may raise
 * quest pressure) and, since there are exactly enough rounds, leave the mandate
 * standing.
 */
class InsultDuel(
    val opponentName: String,
    val rounds: List<DuelRound>,
    val startingDignity: Int = rounds.size
) {
    init {
        require(rounds.isNotEmpty()) { "a duel needs at least one round" }
        require(startingDignity in 1..rounds.size) { "startingDignity must fit the round count" }
    }

    var opponentDignity: Int = startingDignity
        private set

    var missteps: Int = 0
        private set

    var outcome: DuelOutcome = DuelOutcome.ONGOING
        private set

    private var index = 0

    /** The round awaiting a response, or null once the duel is over. */
    fun currentRound(): DuelRound? =
        if (outcome == DuelOutcome.ONGOING) rounds.getOrNull(index) else null

    /** Submit [option] for the current round. */
    fun respond(option: Counter): RoundResult {
        val round = currentRound()
            ?: return RoundResult(false, opponentDignity, outcome, DUEL_ALREADY_OVER)

        val landed = option.counters == round.insult.type
        if (landed) opponentDignity-- else missteps++
        index++

        outcome = when {
            opponentDignity <= 0 -> DuelOutcome.WON
            index >= rounds.size -> DuelOutcome.LOST
            else -> DuelOutcome.ONGOING
        }

        val line = when {
            outcome == DuelOutcome.WON -> MANDATE_DESTABILIZED
            outcome == DuelOutcome.LOST -> MANDATE_HOLDS
            landed -> COUNTER_ACCEPTED
            else -> COUNTER_REJECTED
        }
        return RoundResult(landed, opponentDignity, outcome, line)
    }

    companion object {
        const val STRIKE_DETECTED = "VERBAL STRIKE DETECTED"
        const val COUNTER_ACCEPTED = "COUNTER-INSULT ACCEPTED -- OFFICIAL DIGNITY -1"
        const val COUNTER_REJECTED = "COUNTER-INSULT REJECTED -- DIGNITY UNMOVED"
        const val MANDATE_DESTABILIZED = "MANDATE DESTABILIZED -- VERBAL PROCEEDING COMPLETE"
        const val MANDATE_HOLDS = "MANDATE HOLDS -- PROCEEDING FILED AS INCONCLUSIVE"
        const val DUEL_ALREADY_OVER = "PROCEEDING ALREADY CONCLUDED"
    }
}
