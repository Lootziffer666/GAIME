package rpg

import rpg.bark.BarkEvent
import rpg.bark.BarkEventBus
import rpg.bark.BarkFireResult
import rpg.combat.CombatAction
import rpg.combat.CombatEngine
import rpg.combat.CombatEvent
import rpg.combat.CombatResult
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookEffect
import rpg.questbook.QuestbookProcessor
import rpg.questbook.QuestbookReaction
import rpg.questbook.RoomContext

/** Result of attempting to fire a bark through the slice pipeline. */
sealed class BarkOutcome {
    /** The bark fired and the Questbook reacted. */
    data class Fired(val reaction: QuestbookReaction) : BarkOutcome()

    /** The bark was on cooldown; nothing reached the Questbook. */
    data class Suppressed(val remainingMillis: Long) : BarkOutcome()
}

/** Snapshot of what happened in a single combat tick, for the UI/tests. */
data class CombatTurn(
    val events: List<CombatEvent>,
    val result: CombatResult
)

/**
 * The traceable spine of the vertical slice. Wires the three subsystems
 * together: a bark fires on the [BarkEventBus], the [QuestbookProcessor]
 * interprets it into a reaction, and the resulting effect is applied to local
 * world state (markers, cleared obstacles, party name). Combat encounters route
 * their story barks back through the same pipeline.
 *
 * This realises the slice's core acceptance criterion: a fully traceable path
 * Bark Key -> Questbook Logic -> Output Effect.
 */
class SliceDirector(
    clockMillis: () -> Long
) {
    val bus = BarkEventBus(clockMillis)
    val questbook = QuestbookProcessor()

    var context: RoomContext = RoomContext(mapId = "tavern", roomId = RoomContext.ROOM_TAVERN)
        private set

    private val _questMarkers = mutableListOf<String>()
    val questMarkers: List<String> get() = _questMarkers.toList()

    private val _falseMarkers = mutableListOf<String>()
    val falseMarkers: List<String> get() = _falseMarkers.toList()

    private val _clearedObstacles = mutableListOf<String>()
    val clearedObstacles: List<String> get() = _clearedObstacles.toList()

    var partyName: String? = null
        private set

    var questbookOpen: Boolean = false
        private set

    val pressure: QuestPressure get() = questbook.pressure

    var currentCombat: CombatEngine? = null
        private set

    /**
     * Moves to a new room. On a map change, pressure, cooldowns and local
     * markers reset (docs note 5/6). Within the same map only the room context
     * changes.
     */
    fun enterRoom(newContext: RoomContext) {
        if (newContext.mapId != context.mapId) {
            questbook.onMapTransition()
            bus.resetCooldowns()
            _questMarkers.clear()
            _falseMarkers.clear()
            _clearedObstacles.clear()
        }
        context = newContext
    }

    /** Fires a bark and applies the Questbook's reaction to local world state. */
    fun fireBark(bark: BarkEvent): BarkOutcome =
        when (val fired = bus.fire(bark)) {
            is BarkFireResult.OnCooldown -> BarkOutcome.Suppressed(fired.remainingMillis)
            is BarkFireResult.Emitted -> {
                val reaction = questbook.process(bark, context)
                applyEffect(reaction.effect)
                BarkOutcome.Fired(reaction)
            }
        }

    private fun applyEffect(effect: QuestbookEffect) {
        when (effect) {
            is QuestbookEffect.SpawnQuestMarker -> _questMarkers.add(effect.targetHint)
            is QuestbookEffect.SpawnFalseMarker -> _falseMarkers.add(effect.label)
            is QuestbookEffect.ClearObstacle -> {
                _clearedObstacles.add(effect.obstacleId)
                context = context.copy(hasBreakableObstacle = false)
            }
            is QuestbookEffect.RegisterPartyName -> {
                partyName = effect.name
                questbookOpen = true
            }
            QuestbookEffect.OpenFullQuestbook -> questbookOpen = true
            QuestbookEffect.BurnTargets,
            QuestbookEffect.RevealHidden,
            QuestbookEffect.FlavorText -> { /* no structural world change */ }
        }
    }

    // --- Combat orchestration ---

    fun startCombat(engine: CombatEngine) {
        currentCombat = engine
    }

    fun clearCombat() {
        currentCombat = null
    }

    /** Advances combat one tick and feeds combat-origin events back through the pipeline. */
    fun combatAction(action: CombatAction): CombatTurn {
        val engine = currentCombat ?: return CombatTurn(emptyList(), CombatResult.ONGOING)
        val events = engine.tick(action)
        handleCombatEvents(events)
        return CombatTurn(events, engine.result)
    }

    private fun handleCombatEvents(events: List<CombatEvent>) {
        for (event in events) {
            when (event) {
                is CombatEvent.BarkTriggered -> fireBark(event.bark)
                is CombatEvent.BossPhaseChanged ->
                    if (event.phase == rpg.combat.BossPhase.PHASE_2) {
                        // The Accountant files objections: pressure spikes to HIGH
                        // and false markers spam the room (docs/VERTICAL_SLICE.md).
                        questbook.escalateTo(QuestPressure.HIGH)
                        _falseMarkers.add("Objection Pending (Case #0002)")
                    }
                is CombatEvent.AddsSummoned,
                is CombatEvent.Message -> { /* surfaced to UI via the returned events */ }
            }
        }
    }
}
