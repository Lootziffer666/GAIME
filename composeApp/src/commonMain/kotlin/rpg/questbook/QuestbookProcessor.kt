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

            // --- Combat, Exploration, and Ambient barks: flavor only ---
            BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT,
            BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK,
            BarkEvent.NIB_FROM_THE_SHADOWS,
            BarkEvent.BRUGG_HAVE_AT_THEE,
            BarkEvent.BRUGG_SURRENDER_OR_DIE,
            BarkEvent.BRUGG_SHOW_YOURSELVES,
            BarkEvent.VELLUM_LETS_SEE_IF_YOU_CAN_DODGE,
            BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE,
            BarkEvent.VELLUM_I_SMITE_YOU,
            BarkEvent.NIB_THAT_STINGS,
            BarkEvent.NIB_LUCKY_HIT,
            BarkEvent.NIB_AVENGE_ME,
            BarkEvent.BRUGG_THATS_GOING_TO_LEAVE_A_MARK,
            BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT,
            BarkEvent.BRUGG_THAT_DREW_BLOOD,
            BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT,
            BarkEvent.VELLUM_I_NEED_A_HEALER,
            BarkEvent.VELLUM_I_DIDNT_THINK_IT_WOULD_END,
            BarkEvent.NIB_GOOD_AS_NEW,
            BarkEvent.BRUGG_I_FEEL_BETTER_THAN_EVER,
            BarkEvent.VELLUM_IM_BACK_ON_MY_FEET,
            BarkEvent.NIB_ITS_A_TRAP,
            BarkEvent.NIB_THEYRE_ONTO_US,
            BarkEvent.NIB_WHAT_DO_WE_HAVE_HERE,
            BarkEvent.BRUGG_THE_DEEPER_WE_GO,
            BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE,
            BarkEvent.BRUGG_SURPRISE_SURPRISE,
            BarkEvent.VELLUM_TREES_HAVE_EYES,
            BarkEvent.VELLUM_HMM_WONDER_WHAT_THIS_IS,
            BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_GLYPH,
            BarkEvent.NIB_I_LOVE_GOLD,
            BarkEvent.NIB_THERES_A_HOLE_IN_MY_BOOT,
            BarkEvent.NIB_STEW_AGAIN,
            BarkEvent.BRUGG_BARKEEP_A_FLAGON,
            BarkEvent.BRUGG_GRAB_YOUR_TORCH,
            BarkEvent.BRUGG_WHERE_DID_I_PUT_THAT_MAP,
            BarkEvent.VELLUM_NOW_WHAT_WAS_THAT_INCANTATION,
            BarkEvent.VELLUM_OF_ALL_THE_ARCANE_LORE,
            BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN,
            BarkEvent.BARKEEP_SPEND_SOME_COIN,
            BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS,
            BarkEvent.PATRON_HE_SURE_IS_SLOW,
            // Extended Combat Taunts
            BarkEvent.NIB_BACK_FOUL_CREATURE,
            BarkEvent.BRUGG_BACK_FOUL_CREATURE,
            BarkEvent.VELLUM_BACK_FOUL_CREATURE,
            BarkEvent.NIB_DARKNESS_TAKE_YOU,
            BarkEvent.BRUGG_DARKNESS_TAKE_YOU,
            BarkEvent.VELLUM_DARKNESS_TAKE_YOU,
            BarkEvent.NIB_DROP_YOUR_WEAPONS,
            BarkEvent.VELLUM_DROP_YOUR_WEAPONS,
            BarkEvent.NIB_YOU_FIGHT_LIKE_A_NEWBORN,
            BarkEvent.BRUGG_YOU_FIGHT_LIKE_A_NEWBORN,
            BarkEvent.VELLUM_YOU_FIGHT_LIKE_A_NEWBORN,
            BarkEvent.NIB_YOU_FIGHT_LIKE_AN_INFANT,
            BarkEvent.BRUGG_YOU_FIGHT_LIKE_AN_INFANT,
            BarkEvent.VELLUM_YOU_FIGHT_LIKE_AN_INFANT,
            BarkEvent.NIB_YOU_FIGHT_LIKE_YOUR_MOTHER,
            BarkEvent.BRUGG_YOU_FIGHT_LIKE_YOUR_MOTHER,
            BarkEvent.VELLUM_YOU_FIGHT_LIKE_YOUR_MOTHER,
            BarkEvent.NIB_MY_NEXT_STRIKE_WILL_FELL_YOU,
            BarkEvent.BRUGG_MY_NEXT_STRIKE_WILL_FELL_YOU,
            BarkEvent.VELLUM_MY_NEXT_STRIKE_WILL_FELL_YOU,
            BarkEvent.NIB_IVE_FOUGHT_KOBOLDS_TOUGHER,
            BarkEvent.BRUGG_IVE_FOUGHT_KOBOLDS_TOUGHER,
            BarkEvent.VELLUM_IVE_FOUGHT_KOBOLDS_TOUGHER,
            BarkEvent.NIB_IVE_FOUGHT_PUPPIES_TOUGHER,
            BarkEvent.BRUGG_IVE_FOUGHT_PUPPIES_TOUGHER,
            BarkEvent.VELLUM_IVE_FOUGHT_PUPPIES_TOUGHER,
            BarkEvent.NIB_IVE_FOUGHT_ARTICHOKES_TOUGHER,
            BarkEvent.BRUGG_IVE_FOUGHT_ARTICHOKES_TOUGHER,
            BarkEvent.VELLUM_IVE_FOUGHT_ARTICHOKES_TOUGHER,
            // Victory Barks
            BarkEvent.NIB_YOULL_NOT_BE_GETTING_UP,
            BarkEvent.BRUGG_YOULL_NOT_BE_GETTING_UP,
            BarkEvent.VELLUM_YOULL_NOT_BE_GETTING_UP,
            BarkEvent.NIB_NEXT_TIME_WONT_BE_SO_LUCKY,
            BarkEvent.BRUGG_NEXT_TIME_WONT_BE_SO_LUCKY,
            BarkEvent.VELLUM_NEXT_TIME_WONT_BE_SO_LUCKY,
            // Damage Reaction
            BarkEvent.NIB_HOW_HUMILIATING,
            BarkEvent.BRUGG_HOW_HUMILIATING,
            BarkEvent.VELLUM_HOW_HUMILIATING,
            // Loot / Container
            BarkEvent.NIB_OOO_ANOTHER_BARREL,
            BarkEvent.BRUGG_OOO_ANOTHER_BARREL,
            BarkEvent.NIB_YESSS_ANOTHER_CRATE,
            BarkEvent.BRUGG_YESSS_ANOTHER_CRATE,
            BarkEvent.NIB_I_WONDER_WHATS_IN_THIS_ONE,
            BarkEvent.BRUGG_I_WONDER_WHATS_IN_THIS_ONE,
            BarkEvent.VELLUM_I_WONDER_WHATS_IN_THIS_ONE,
            BarkEvent.NIB_THIS_CHEST_UNLOCKED,
            BarkEvent.VELLUM_THIS_CHEST_UNLOCKED,
            BarkEvent.NIB_THIS_LOOKS_LIKE_TREASURE,
            BarkEvent.BRUGG_THIS_LOOKS_LIKE_TREASURE,
            BarkEvent.VELLUM_THIS_LOOKS_LIKE_TREASURE,
            BarkEvent.NIB_NOTHING_HERE,
            BarkEvent.BRUGG_NOTHING_HERE,
            BarkEvent.VELLUM_NOTHING_HERE,
            BarkEvent.NIB_IF_IT_GLOWS_MORE_EXPENSIVE,
            BarkEvent.BRUGG_IF_IT_GLOWS_MORE_EXPENSIVE,
            BarkEvent.VELLUM_IF_IT_GLOWS_MORE_EXPENSIVE,
            BarkEvent.NIB_WHY_DO_THEY_LEAVE_LOOT,
            BarkEvent.BRUGG_WHY_DO_THEY_LEAVE_LOOT,
            BarkEvent.VELLUM_WHY_DO_THEY_LEAVE_LOOT,
            // Exploration / Navigation
            BarkEvent.NIB_LETS_GET_OUT_OF_HERE,
            BarkEvent.BRUGG_LETS_GET_OUT_OF_HERE,
            BarkEvent.VELLUM_LETS_GET_OUT_OF_HERE,
            BarkEvent.NIB_LETS_HIT_THE_TRAIL,
            BarkEvent.BRUGG_LETS_HIT_THE_TRAIL,
            BarkEvent.NIB_LETS_BE_UNDERWAY,
            BarkEvent.BRUGG_LETS_BE_UNDERWAY,
            BarkEvent.VELLUM_LETS_BE_UNDERWAY,
            BarkEvent.NIB_THIS_PLACE_REEKS_OF_DEATH,
            BarkEvent.BRUGG_THIS_PLACE_REEKS_OF_DEATH,
            BarkEvent.VELLUM_THIS_PLACE_REEKS_OF_DEATH,
            BarkEvent.NIB_WHAT_DARK_DEALINGS,
            BarkEvent.BRUGG_WHAT_DARK_DEALINGS,
            BarkEvent.VELLUM_WHAT_DARK_DEALINGS,
            BarkEvent.NIB_LOOK_OUT,
            BarkEvent.BRUGG_LOOK_OUT,
            BarkEvent.VELLUM_LOOK_OUT,
            BarkEvent.VELLUM_LIGHT_WILL_SCOUR,
            BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.BRUGG_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.VELLUM_THIS_THING_LOOKS_INTERESTING,
            // Tavern / Idle Expansion
            BarkEvent.NIB_IS_THAT_ROAST,
            BarkEvent.BRUGG_IS_THAT_ROAST,
            BarkEvent.VELLUM_IS_THAT_ROAST,
            BarkEvent.NIB_YOUVE_GOT_TO_TRY_ROAST,
            BarkEvent.BRUGG_YOUVE_GOT_TO_TRY_ROAST,
            BarkEvent.VELLUM_YOUVE_GOT_TO_TRY_ROAST,
            BarkEvent.NIB_I_NEEDED_THAT,
            BarkEvent.BRUGG_I_NEEDED_THAT,
            BarkEvent.VELLUM_I_NEEDED_THAT,
            BarkEvent.NIB_THATS_NOTHING_ALE_WONT_FIX,
            BarkEvent.BRUGG_THATS_NOTHING_ALE_WONT_FIX,
            BarkEvent.VELLUM_THATS_NOTHING_ALE_WONT_FIX,
            BarkEvent.NIB_GREETINGS_FRIEND,
            BarkEvent.BRUGG_GREETINGS_FRIEND,
            BarkEvent.VELLUM_GREETINGS_STRANGER,
            BarkEvent.NIB_WHAT_NEWS,
            BarkEvent.BRUGG_WHAT_NEWS,
            BarkEvent.VELLUM_WHAT_NEWS,
            BarkEvent.NIB_WHERES_THE_PRIVVY,
            BarkEvent.BRUGG_WHERES_THE_PRIVVY,
            BarkEvent.VELLUM_WHERES_THE_NEAREST_INN ->
                react("Atmospheric observation noted", FlavorText)

            // ═══ Chapter 2: Context-aware reactions ═════════════════════
            BarkEvent.BRUGG_DROP_YOUR_WEAPONS ->
                if (ctx.roomId == RoomContext.ROOM_FOREST_BOSS) {
                    react("Surrender Filing: Rejected (Reason: Insufficient Paperwork)", FlavorText)
                } else {
                    react("Atmospheric observation noted", FlavorText)
                }

            BarkEvent.NIB_SMELL_GOLD ->
                if (ctx.roomId == RoomContext.ROOM_MARKET) {
                    react(
                        "Commercial Survey Initiated: Catalogue all vendor-adjacent valuables (Mandatory)",
                        SpawnQuestMarker("market valuables")
                    )
                } else {
                    react("Gold detected: Source unverified", FlavorText)
                }

            BarkEvent.BRUGG_WHO_RUNS_THIS_CITY ->
                react("Municipal Inquiry Filed: Authority Structure Report (Budget: 0 Gold)", FlavorText)

            BarkEvent.VELLUM_CREATURES_IN_WOODS ->
                react(
                    "Wildlife Census Ordered: Document all woodland fauna (Deadline: Immediate)",
                    SpawnQuestMarker("woodland fauna")
                )

            BarkEvent.VELLUM_ELEMENTS_MINE_TO_COMMAND ->
                react("Elemental Claim Registered: Command over elements noted (Scope: Undefined)", FlavorText)

            BarkEvent.VELLUM_CALLS_FOR_LIGHTNING ->
                if (ctx.roomId == RoomContext.ROOM_FOREST_SHRINE) {
                    react("Electrical Works Permit: Granted (Safety Notice: None)", RevealHidden)
                } else {
                    react("Lightning request: No valid conductor", FlavorText)
                }

            BarkEvent.VELLUM_BALANCE_LIFE_DEATH ->
                react("Existential Risk Assessment: Filed (Priority: Philosophical)", FlavorText)

            // Chapter 2: flavor-only reactions
            BarkEvent.NIB_HOW_MUCH,
            BarkEvent.NIB_SECRET_ENTRANCE,
            BarkEvent.BRUGG_SPEAK_TO_GUARD,
            BarkEvent.BRUGG_KEEP_TO_TRAIL,
            BarkEvent.BRUGG_EXPERIENCE_IS_HOW_WE_GROW,
            BarkEvent.VELLUM_SO_THATS_HOW_IT_IS,
            BarkEvent.MERCHANT_SEE_IF_THIS_STRIKES_FANCY,
            BarkEvent.MERCHANT_MAKE_ME_AN_OFFER,
            BarkEvent.MERCHANT_NAME_YOUR_PRICE,
            BarkEvent.GUARD_BACK_ALREADY ->
                react("Atmospheric observation noted", FlavorText)

            // ═══ Chapters 3-5 + Finale (docs/CAMPAIGN.md) ═══════════════
            // --- Completion flavor ---
            BarkEvent.BRUGG_OBJECTIVE_COMPLETE ->
                react("Objective Logged: Closed (Satisfaction: Assumed)", FlavorText)
            BarkEvent.VELLUM_HARD_WON_KNOWLEDGE ->
                react("Knowledge Filed (Effort: Noted, Reward: Pending)", FlavorText)

            // --- Chapter 3: The Woods That Had Opinions ---
            BarkEvent.VELLUM_WHICH_DIRECTION ->
                if (before != QuestPressure.LOW)
                    react("Navigation Quest Reissued: Destination Recalculated", SpawnFalseMarker("Recalculated Route"))
                else react("Direction noted. Map unchanged (for now)", FlavorText, before.raised())
            BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_MAP ->
                if (ctx.hasInteractableTarget) react("Cartographic Asset Catalogued", RevealHidden)
                else react("No cartographic asset detected", FlavorText)

            // --- Chapter 4: The Ship That Was Technically Seaworthy ---
            BarkEvent.BRUGG_IS_SHE_SEAWORTHY ->
                react("Seaworthiness Assessment: Technically", FlavorText)
            BarkEvent.BRUGG_HOIST_ANCHOR ->
                if (ctx.hasInteractableTarget) react("Anchorage Released (Direction: Approximate)", RevealHidden)
                else react("Anchor status: ambiguous", FlavorText)
            BarkEvent.BRUGG_DROP_ANCHOR ->
                react("Mooring Logged (Permanence: Doubtful)", FlavorText)
            BarkEvent.BRUGG_RAISE_THE_SAIL ->
                react("Canvas Deployment Filed", FlavorText)
            BarkEvent.BRUGG_OUT_MANEUVERED ->
                react("Tactical Disadvantage Filed (Blame: Pending)", FlavorText, before.raised())
            BarkEvent.BRUGG_RETREAT ->
                if (ctx.roomId == RoomContext.ROOM_BOSS || ctx.roomId == RoomContext.ROOM_FOREST_BOSS)
                    react("Retreat denied by paperwork.", FlavorText)
                else react("Tactical Withdrawal Logged (Direction: Away)", FlavorText)

            // --- Chapter 5: The Dragon That Was Accidentally Summoned ---
            BarkEvent.NIB_SMELL_DRAGON ->
                // Signature reaction: the book reads a smell as a defect report.
                react(
                    "URGENT QUEST ACCEPTED: DEFEAT THE DRAGON",
                    SpawnQuestMarker("dragon (to be generated)"),
                    QuestPressure.HIGH
                )
            BarkEvent.NIB_SMELL_MONSTERS ->
                if (ctx.hasEnemies)
                    react("Threat Acknowledgement Filed: Monsters Marked", SpawnQuestMarker("nearest monster"), before.raised())
                else react("No monsters in vicinity (regrettably)", FlavorText)
            BarkEvent.BRUGG_HOLD_THE_LINE ->
                react("Defensive Posture Mandated (Line: Imaginary)", FlavorText, before.raised())
            BarkEvent.BRUGG_PROTECT_THE_ASSET ->
                react("Asset Protection Order Filed (Asset: Undefined)", FlavorText, before.raised())

            // --- Chapter 5 / Finale flavor ---
            BarkEvent.NIB_FRESH_SEA_AIR,
            BarkEvent.NIB_NOT_FOND_OF_DEEP_WATER,
            BarkEvent.VELLUM_SEA_IS_ANGRY_MISTRESS,
            BarkEvent.BRUGG_LOW_ON_HEALTH,
            BarkEvent.VELLUM_SUMMON_YOUR_STRENGTH ->
                react("Atmospheric observation noted", FlavorText)

            // --- Finale: System Overload (banal, contradictory quest-accepts) ---
            BarkEvent.NIB_NOT_A_HORSE ->
                react("QUEST ACCEPTED: IDENTIFY THE HORSE", SpawnQuestMarker("horse"))
            BarkEvent.NIB_THIS_LOOKS_LIKE_GOLD ->
                react("QUEST ACCEPTED: APPRAISE THE GOLD", SpawnQuestMarker("gold"))
            BarkEvent.NIB_DOOR_ALMOST_UNLOCKED ->
                if (ctx.hasInteractableTarget) react("Entry Reclassified as Pre-Authorised", RevealHidden)
                else react("QUEST ACCEPTED: OPEN THE DOOR", SpawnQuestMarker("door"))
        }
    }
}
