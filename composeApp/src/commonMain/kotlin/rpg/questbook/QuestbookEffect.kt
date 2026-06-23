package rpg.questbook

/**
 * A visible, deterministic effect the Questbook produces in response to a bark.
 * Effects are local to the current map (docs/GAME_CONCEPT_LOCK.md), except
 * [RegisterPartyName] which is the one story-locked, permanent reaction.
 */
sealed class QuestbookEffect {
    /** Atmosphere/text only, no mechanical change. */
    data object FlavorText : QuestbookEffect()

    /** A real quest marker appears on the named target. */
    data class SpawnQuestMarker(val targetHint: String) : QuestbookEffect()

    /** A misleading marker appears (only at MEDIUM+ pressure). */
    data class SpawnFalseMarker(val label: String) : QuestbookEffect()

    /** A breakable obstacle is cleared (e.g. rubble via BRUGG_ATTACK). */
    data class ClearObstacle(val obstacleId: String) : QuestbookEffect()

    /** Flammable targets/enemies burn (VELLUM_CALLS_FOR_FLAME). */
    data object BurnTargets : QuestbookEffect()

    /** Hidden/interactive elements in the room are highlighted. */
    data object RevealHidden : QuestbookEffect()

    /** The full-screen Questbook view opens. */
    data object OpenFullQuestbook : QuestbookEffect()

    /** The party's official name is permanently registered. */
    data class RegisterPartyName(val name: String) : QuestbookEffect()
}
