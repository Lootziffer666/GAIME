package rpg.questbook

import rpg.bark.BarkEvent

/**
 * The deterministic result of the Questbook interpreting a single bark.
 * The full pipeline is traceable: [bark] -> [questbookText] + [effect]
 * (+ optional pressure change). Same bark in same context always yields the
 * same reaction (docs/GAME_CONCEPT_LOCK.md determinism rule).
 */
data class QuestbookReaction(
    val bark: BarkEvent,
    /** The bureaucratic misinterpretation shown to the player. */
    val questbookText: String,
    val effect: QuestbookEffect,
    val pressureBefore: QuestPressure,
    val pressureAfter: QuestPressure
) {
    val pressureChanged: Boolean get() = pressureBefore != pressureAfter
}
