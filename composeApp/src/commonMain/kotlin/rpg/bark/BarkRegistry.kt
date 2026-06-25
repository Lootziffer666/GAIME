package rpg.bark

import rpg.bark.BarkType.DANGER_BARK
import rpg.bark.BarkType.PRESSURE_BARK
import rpg.bark.BarkType.SAFE_BARK
import rpg.bark.BarkType.TRIGGER_BARK
import rpg.bark.BarkType.UTILITY_BARK
import rpg.bark.BarkScope.CURRENT_MAP
import rpg.bark.BarkScope.CURRENT_ROOM
import rpg.bark.PartyCharacter.BRUGG
import rpg.bark.PartyCharacter.NIB
import rpg.bark.PartyCharacter.VELLUM

/**
 * Single source of truth for bark metadata. Encodes the Bark Table from
 * docs/BARK_TRIGGER_TABLE.md verbatim. Every [BarkEvent] has exactly one entry.
 */
object BarkRegistry {

    private val definitions: Map<BarkEvent, BarkDefinition> = listOf(
        // ─── Original 20: Nib ────────────────────────────────────────────
        def(BarkEvent.NIB_SMELL_TREASURE, NIB, "Ich riech Gold... oder zumindest was Glänzendes!", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.NIB_SMELL_SEWAGE, NIB, "Bäh, das stinkt nach totem Goldfisch.", SAFE_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.NIB_IT_WASNT_ME, NIB, "Ich war das nicht! Gar nicht! Nie!", PRESSURE_BARK, CURRENT_MAP, 45, true),
        def(BarkEvent.NIB_SHORTCUT, NIB, "Da muss es nen Abkürzung geben...", TRIGGER_BARK, CURRENT_ROOM, 60, false),
        def(BarkEvent.NIB_POCKET_CHECK, NIB, "Mal schauen was die so dabeihaben...", DANGER_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_INNOCENT_WHISTLE, NIB, "*pfeift unschuldig*", SAFE_BARK, CURRENT_ROOM, 10, false),
        def(BarkEvent.NIB_TREASURE_FOUND, NIB, "JACKPOT! ...oh. Oder auch nicht.", DANGER_BARK, CURRENT_ROOM, 30, false),

        // ─── Original 20: Brugg ──────────────────────────────────────────
        def(BarkEvent.BRUGG_ATTACK, BRUGG, "ATTACKE!", UTILITY_BARK, CURRENT_ROOM, 20, true),
        def(BarkEvent.BRUGG_FALL_BACK, BRUGG, "RÜCKZUG! ...oder so.", TRIGGER_BARK, CURRENT_ROOM, 45, false),
        def(BarkEvent.BRUGG_THAT_WASNT_SO_BAD, BRUGG, "Halb so wild.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_WHAT_ARE_YOUR_ORDERS, BRUGG, "Was sind die Befehle?", PRESSURE_BARK, CURRENT_MAP, 60, false),
        def(BarkEvent.BRUGG_HUNGRY, BRUGG, "Ich hab Hunger.", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_PROTECT, BRUGG, "Niemand kommt hier durch!", DANGER_BARK, CURRENT_ROOM, 60, false),
        def(BarkEvent.BRUGG_SMASH, BRUGG, "KAPUTT!", DANGER_BARK, CURRENT_ROOM, 60, false),

        // ─── Original 20: Vellum ────────────────────────────────────────
        def(BarkEvent.VELLUM_CALLS_FOR_FLAME, VELLUM, "Ignis! Kontrollierte Verbrennung!", UTILITY_BARK, CURRENT_ROOM, 25, true),
        def(BarkEvent.VELLUM_CALLS_FOR_ICE, VELLUM, "Glacies! Kristallstruktur, bitte!", UTILITY_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER, VELLUM, "Wissen ist immer die Antwort.", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING, VELLUM, "Das... das ändert alles!", PRESSURE_BARK, CURRENT_MAP, 90, true),
        def(BarkEvent.VELLUM_TECHNICALLY_CORRECT, VELLUM, "Technisch gesehen ist das korrekt.", SAFE_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.VELLUM_READ_THE_FINE_PRINT, VELLUM, "Steht alles im Kleingedruckten!", TRIGGER_BARK, CURRENT_ROOM, 40, false),

        // ═══ Combat Taunts ═══════════════════════════════════════════════
        def(BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT, NIB, "Is that all you've got?", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK, NIB, "Your defenses are weak!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_FROM_THE_SHADOWS, NIB, "From the shadows!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_HAVE_AT_THEE, BRUGG, "Have at thee!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_SURRENDER_OR_DIE, BRUGG, "Surrender or die!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_SHOW_YOURSELVES, BRUGG, "Show yourselves!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_LETS_SEE_IF_YOU_CAN_DODGE, VELLUM, "Let's see if you can dodge this one!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE, VELLUM, "Your defenses are futile!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_I_SMITE_YOU, VELLUM, "I smite you!", SAFE_BARK, CURRENT_ROOM, 20, false),

        // ═══ Combat Reactions (damage/dying) ═════════════════════════════
        def(BarkEvent.NIB_THAT_STINGS, NIB, "That stings!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_LUCKY_HIT, NIB, "Lucky hit!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_AVENGE_ME, NIB, "Avenge me!", SAFE_BARK, CURRENT_ROOM, 45, false),
        def(BarkEvent.BRUGG_THATS_GOING_TO_LEAVE_A_MARK, BRUGG, "That's going to leave a mark.", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT, BRUGG, "I don't have much left in me.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_THAT_DREW_BLOOD, BRUGG, "That drew blood!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT, VELLUM, "I'm going to feel that one in the morning.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_I_NEED_A_HEALER, VELLUM, "I need a healer!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_I_DIDNT_THINK_IT_WOULD_END, VELLUM, "I didn't think it would end like this.", SAFE_BARK, CURRENT_ROOM, 45, false),

        // ═══ Combat Reactions (healing/recovery) ═════════════════════════
        def(BarkEvent.NIB_GOOD_AS_NEW, NIB, "Good as new!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_I_FEEL_BETTER_THAN_EVER, BRUGG, "I feel better than ever!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_IM_BACK_ON_MY_FEET, VELLUM, "I'm back on my feet!", SAFE_BARK, CURRENT_ROOM, 20, false),

        // ═══ Exploration / Discovery ═════════════════════════════════════
        def(BarkEvent.NIB_ITS_A_TRAP, NIB, "It's a trap!", TRIGGER_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_THEYRE_ONTO_US, NIB, "They're onto us!", TRIGGER_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_WHAT_DO_WE_HAVE_HERE, NIB, "What do we have here?", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_THE_DEEPER_WE_GO, BRUGG, "The deeper we go, the darker it gets.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE, BRUGG, "This looks like trouble.", TRIGGER_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_SURPRISE_SURPRISE, BRUGG, "Surprise, surprise!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_TREES_HAVE_EYES, VELLUM, "It's as though the trees have eyes.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_HMM_WONDER_WHAT_THIS_IS, VELLUM, "Hmm, I wonder what this could be...", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_GLYPH, VELLUM, "This looks like a glyph.", TRIGGER_BARK, CURRENT_ROOM, 25, false),

        // ═══ Ambient / Idle ══════════════════════════════════════════════
        def(BarkEvent.NIB_I_LOVE_GOLD, NIB, "I love gold!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_THERES_A_HOLE_IN_MY_BOOT, NIB, "There's a hole in my boot.", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_STEW_AGAIN, NIB, "Stew again?", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_BARKEEP_A_FLAGON, BRUGG, "Barkeep! A flagon of ale!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_GRAB_YOUR_TORCH, BRUGG, "Grab your torch, there's work to be done.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_WHERE_DID_I_PUT_THAT_MAP, BRUGG, "Where did I put that map?", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_NOW_WHAT_WAS_THAT_INCANTATION, VELLUM, "Now what was that incantation?", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_OF_ALL_THE_ARCANE_LORE, VELLUM, "Of all the arcane lore...", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN, VELLUM, "Time waits for no man.", SAFE_BARK, CURRENT_ROOM, 30, false),

        // ═══ NPC Dialogue ════════════════════════════════════════════════
        def(BarkEvent.BARKEEP_SPEND_SOME_COIN, BRUGG, "Spend some coin or get out.", SAFE_BARK, CURRENT_ROOM, 0, true),
        def(BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS, BRUGG, "Been playing in the sewers, have we?", SAFE_BARK, CURRENT_ROOM, 0, true),
        def(BarkEvent.PATRON_HE_SURE_IS_SLOW, VELLUM, "He sure is slow for a four-armed bartender.", SAFE_BARK, CURRENT_ROOM, 0, true)
    ).associateBy { it.key }

    init {
        // Fail fast if a bark key is missing a definition.
        val missing = BarkEvent.entries.filterNot { definitions.containsKey(it) }
        require(missing.isEmpty()) { "BarkRegistry missing definitions for: $missing" }
    }

    /** The definition for [key]. Guaranteed non-null for every enum value. */
    operator fun get(key: BarkEvent): BarkDefinition =
        definitions.getValue(key)

    fun all(): List<BarkDefinition> = definitions.values.toList()

    private fun def(
        key: BarkEvent,
        character: PartyCharacter,
        audioText: String,
        type: BarkType,
        scope: BarkScope,
        cooldownSeconds: Int,
        usedInSlice: Boolean
    ) = BarkDefinition(key, character, audioText, type, scope, cooldownSeconds, usedInSlice)
}
