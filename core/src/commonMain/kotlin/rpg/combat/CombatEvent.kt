package rpg.combat

import rpg.bark.BarkEvent

/**
 * Something noteworthy that happened during a combat tick. The engine stays
 * decoupled from the Questbook: it surfaces these events and the SliceDirector
 * decides how to react (e.g. raise pressure, fire a bark, show a message).
 */
sealed class CombatEvent {
    data class Message(val text: String) : CombatEvent()
    data class BossPhaseChanged(val phase: BossPhase) : CombatEvent()
    data class AddsSummoned(val count: Int) : CombatEvent()
    /** A story bark the combat wants fired (e.g. NIB_SMELL_TREASURE on desk throw). */
    data class BarkTriggered(val bark: BarkEvent) : CombatEvent()
}

/** Terminal state of an encounter. */
enum class CombatResult { ONGOING, VICTORY, DEFEAT }
