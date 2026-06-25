package rpg.finale

import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookReaction

/**
 * The climax mechanic of Chapter 5 / the Finale (docs/CAMPAIGN.md, Phase 4 -
 * System Overload).
 *
 * The Questbook cannot be defeated by completing its quests -- that only
 * confirms its jurisdiction. Instead the party overloads it: at maximum
 * ([QuestPressure.HIGH]) pressure they deliberately fire banal, contradictory
 * quest-accepting barks. Once enough *distinct* contradictory quests are
 * accepted simultaneously, the Questbook tries to prioritise everything at once,
 * fails, and declares GAME OVER -- about itself.
 *
 * This tracker is deterministic and side-effect free: feed it the
 * [QuestbookReaction]s coming out of the pipeline and read its [state]. It is
 * intentionally decoupled from combat and the slice director so it can be unit
 * tested and reused by a finale director.
 */
class QuestbookOverload(
    /** Distinct contradictory quests required to crash the book. */
    val threshold: Int = DEFAULT_THRESHOLD
) {
    init {
        require(threshold >= 2) { "Overload threshold must allow contradiction (>= 2)" }
    }

    private val acceptedQuests = LinkedHashSet<String>()

    var state: OverloadState = OverloadState.Stable
        private set

    /** The distinct contradictory quests accepted so far, in arrival order. */
    val pendingQuests: List<String> get() = acceptedQuests.toList()

    val isCollapsed: Boolean get() = state is OverloadState.Collapsed

    /**
     * Offer a reaction to the overload tracker. Only quest-accepting reactions
     * (those that spawn a real quest marker) fired while pressure is at its
     * maximum count toward an overload. Returns the resulting [state].
     *
     * Once [state] is [OverloadState.Collapsed] further reactions are ignored:
     * the book is already broken.
     */
    fun offer(reaction: QuestbookReaction): OverloadState {
        if (isCollapsed) return state

        val marker = reaction.effect as? QuestbookEffect.SpawnQuestMarker ?: return state
        // Overload only builds at maximum pressure -- a calm book just files them.
        if (reaction.pressureAfter != QuestPressure.HIGH) return state

        acceptedQuests.add(marker.targetHint)
        state = if (acceptedQuests.size >= threshold) {
            OverloadState.Collapsed(acceptedQuests.toList())
        } else {
            OverloadState.Building(acceptedQuests.size, threshold)
        }
        return state
    }

    /** Resets the tracker (e.g. when re-entering the encounter). */
    fun reset() {
        acceptedQuests.clear()
        state = OverloadState.Stable
    }

    companion object {
        /** Matches the seven QUEST ACCEPTED lines in docs/CAMPAIGN.md, Phase 4. */
        const val DEFAULT_THRESHOLD = 7

        /** The form the collapsed Administragon leaves behind. */
        const val COLLAPSE_NOTE = "PLEASE RESTART THE STORY CORRECTLY."

        /** The book's self-directed verdict on collapse. */
        const val COLLAPSE_VERDICT = "GAME OVER"
    }
}

/** Progress of the Questbook toward a self-inflicted collapse. */
sealed class OverloadState {
    /** No contradictory overload in progress. */
    data object Stable : OverloadState()

    /** Overload building: [accepted] of [threshold] contradictory quests filed. */
    data class Building(val accepted: Int, val threshold: Int) : OverloadState()

    /** The book crashed under its own contradictions. The party "wins" by losing. */
    data class Collapsed(val quests: List<String>) : OverloadState()
}
