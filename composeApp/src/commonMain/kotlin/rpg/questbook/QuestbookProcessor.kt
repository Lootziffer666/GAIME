package rpg.questbook

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import rpg.bark.BarkEvent
import rpg.bark.BarkRegistry
import rpg.bark.BarkType
import rpg.questbook.QuestbookEffect.BurnTargets
import rpg.questbook.QuestbookEffect.ClearObstacle
import rpg.questbook.QuestbookEffect.FlavorText
import rpg.questbook.QuestbookEffect.RegisterPartyName
import rpg.questbook.QuestbookEffect.RevealHidden
import rpg.questbook.QuestbookEffect.SpawnFalseMarker
import rpg.questbook.QuestbookEffect.SpawnQuestMarker

/**
 * The Questbook. Receives a [BarkEvent] key and produces a deterministic,
 * bureaucratic misinterpretation as a [QuestbookReaction] (text + effect +
 * pressure change). It is NOT a random generator -- it is always
 * understandably wrong (docs/GAME_CONCEPT_LOCK.md).
 *
 * The reaction table is the data-driven encoding of the "Required Questbook
 * Reactions" and "Safe/Failsafe Behavior" columns of the design docs.
 *
 * Invariants enforced here:
 *  - Pressure can never exceed HIGH; PRESSURE_BARKs at HIGH produce flavor only.
 *  - False markers only appear at MEDIUM or higher pressure.
 *  - Pressure and the quest log reset on map transition.
 */
class QuestbookProcessor {

    var pressure: QuestPressure = QuestPressure.LOW
        private set

    private val _log = mutableListOf<QuestbookReaction>()
    /** All reactions produced since the last map transition, in order. */
    val log: List<QuestbookReaction> get() = _log.toList()

    private val _reactions = MutableSharedFlow<QuestbookReaction>(
        replay = 1,
        extraBufferCapacity = 16
    )
    val reactions: Flow<QuestbookReaction> = _reactions.asSharedFlow()

    /** Resets pressure and the quest log -- called on map transition (docs note 5/6). */
    fun onMapTransition() {
        pressure = QuestPressure.LOW
        _log.clear()
    }

    /**
     * Systemic (non-bark) escalation, e.g. the boss "filing objections" in
     * Phase 2. Raises pressure to at least [level], never lowering it and never
     * exceeding HIGH. Returns the new pressure.
     */
    fun escalateTo(level: QuestPressure): QuestPressure {
        if (level.ordinal > pressure.ordinal) pressure = level
        return pressure
    }

    /**
     * Interpret [bark] in [ctx] and return the (deterministic) reaction.
     * Also records it in the log, updates pressure, and emits to [reactions].
     */
    fun process(bark: BarkEvent, ctx: RoomContext): QuestbookReaction {
        val before = pressure
        val type = BarkRegistry[bark].type

        // Helper that finalises a reaction with an explicit resulting pressure.
        fun react(text: String, effect: QuestbookEffect, after: QuestPressure = before): QuestbookReaction {
            pressure = after
            val reaction = QuestbookReaction(bark, text, effect, before, after)
            _log.add(reaction)
            _reactions.tryEmit(reaction)
            return reaction
        }

        // The party-name registration is a story-locked, permanent reaction and
        // always fires in the boss room, even at HIGH pressure.
        val storyLockedNaming =
            bark == BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING && ctx.roomId == RoomContext.ROOM_BOSS

        // Pressure barks at HIGH degrade to flavor only (docs note 6) -- with a
        // per-bark "already on record" line where the docs specify one.
        if (type == BarkType.PRESSURE_BARK && before.isMax && !storyLockedNaming) {
            val cappedText = when (bark) {
                BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING -> "Amendment: Already on record"
                BarkEvent.BRUGG_WHAT_ARE_YOUR_ORDERS -> "Orders: Survive"
                else -> "Filed in triplicate. No further action at maximum pressure."
            }
            return react(cappedText, FlavorText)
        }

        return when (bark) {
            BarkEvent.NIB_SMELL_TREASURE -> when (ctx.roomId) {
                RoomContext.ROOM_BOSS ->
                    react("Valuables Located: Filing Cabinet (Contents: Rats)", FlavorText)
                else -> if (ctx.hasInteractableTarget) {
                    react(
                        "Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)",
                        SpawnQuestMarker("nearest interactable")
                    )
                } else {
                    // Failsafe: marker points at Nib herself.
                    react(
                        "Official Quest Registered: Locate subterranean valuables (Source: Self)",
                        SpawnQuestMarker("Nib")
                    )
                }
            }

            BarkEvent.NIB_IT_WASNT_ME -> {
                val after = before.raised()
                react(
                    "Incident Report Filed: Denial of Involvement (Case #0001) -- Noted for Records",
                    SpawnFalseMarker("Investigate Reported Innocence"),
                    after
                )
            }

            BarkEvent.BRUGG_THAT_WASNT_SO_BAD ->
                react(
                    "Amendment Filed: Structural Assessment of Municipal Underground -- Status: Satisfactory",
                    FlavorText
                )

            BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER ->
                if (ctx.hasPuzzleElement) {
                    react(
                        "Academic Grant Approved: Research Into Obstruction Removal (Budget: 0 Gold)",
                        RevealHidden
                    )
                } else {
                    react("Research complete: No findings", FlavorText)
                }

            BarkEvent.BRUGG_ATTACK ->
                if (ctx.hasBreakableObstacle) {
                    react("Demolition Permit Issued: Immediate Effect", ClearObstacle("rubble"))
                } else {
                    react("Aggression: Unfocused", FlavorText)
                }

            BarkEvent.VELLUM_CALLS_FOR_FLAME ->
                if (ctx.hasFlammableTarget) {
                    react("Controlled Burn Authorization: Filed", BurnTargets)
                } else {
                    react("Burn permit: No valid target", FlavorText)
                }

            BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING ->
                if (ctx.roomId == RoomContext.ROOM_BOSS) {
                    // Story-locked, permanent reaction: the party name.
                    react(
                        "Name Registration Complete: 'Everything Changes' -- Official Party Name Locked",
                        RegisterPartyName("Everything Changes"),
                        before.raised()
                    )
                } else {
                    react(
                        "Major Amendment Filed: Reality Reassessment In Progress",
                        FlavorText,
                        before.raised()
                    )
                }

            // --- Barks defined in the table but not exercised by the slice ---

            BarkEvent.NIB_SMELL_SEWAGE ->
                react("Olfactory hazard documented", FlavorText)

            BarkEvent.NIB_SHORTCUT ->
                if (ctx.hasInteractableTarget) react("Route Optimisation Request: Approved", RevealHidden)
                else react("Shortcut: Go Back", SpawnQuestMarker("entrance"))

            BarkEvent.NIB_POCKET_CHECK ->
                if (ctx.hasEnemies) react("Asset Inspection Authorised (Subjects: Alerted)", RevealHidden, before.raised())
                else react("No pockets found in vicinity", FlavorText)

            BarkEvent.NIB_INNOCENT_WHISTLE ->
                react("Ambient noise: classified as non-threatening", FlavorText)

            BarkEvent.NIB_TREASURE_FOUND ->
                if (ctx.hasContainer) react("Treasure Audit Initiated (Hazard: Possible)", RevealHidden, before.raised())
                else react("Treasure audit: Inconclusive", FlavorText)

            BarkEvent.BRUGG_FALL_BACK ->
                react("Tactical Withdrawal Logged (Direction: Approximate)", FlavorText)

            BarkEvent.BRUGG_WHAT_ARE_YOUR_ORDERS ->
                react("Sub-Objective Generated: Await Further Instruction", SpawnQuestMarker("objective"), before.raised())

            BarkEvent.BRUGG_HUNGRY ->
                react("Provision request filed (Priority: Low)", FlavorText)

            BarkEvent.BRUGG_PROTECT ->
                if (ctx.hasEnemies) react("Perimeter Defence Filed (Aggro: Concentrated)", FlavorText, before.raised())
                else react("Perimeter: Secure", FlavorText)

            BarkEvent.BRUGG_SMASH ->
                if (ctx.hasBreakableObstacle) react("Mass Demolition Permit Issued (Scope: Indiscriminate)", ClearObstacle("all"), before.raised())
                else react("Destruction permit: Void (nothing to demolish)", FlavorText)

            BarkEvent.VELLUM_CALLS_FOR_ICE ->
                if (ctx.hasInteractableTarget) react("Cryogenic Works Permit: Approved", RevealHidden)
                else react("Cooling request: Denied (no substrate)", FlavorText)

            BarkEvent.VELLUM_TECHNICALLY_CORRECT ->
                react("Accuracy confirmed. Filed under: Pedantry", FlavorText)

            BarkEvent.VELLUM_READ_THE_FINE_PRINT ->
                if (ctx.hasInteractableTarget) react("Disclosure Request Granted: Fine Print Revealed", RevealHidden)
                else react("No fine print located (font size: adequate)", FlavorText)
        }
    }
}
