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
        // key, character, audioText, type, scope, cooldown, usedInSlice
        def(BarkEvent.NIB_SMELL_TREASURE, NIB, "Ich riech Gold... oder zumindest was Glänzendes!", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.NIB_SMELL_SEWAGE, NIB, "Bäh, das stinkt nach totem Goldfisch.", SAFE_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.NIB_IT_WASNT_ME, NIB, "Ich war das nicht! Gar nicht! Nie!", PRESSURE_BARK, CURRENT_MAP, 45, true),
        def(BarkEvent.NIB_SHORTCUT, NIB, "Da muss es nen Abkürzung geben...", TRIGGER_BARK, CURRENT_ROOM, 60, false),
        def(BarkEvent.NIB_POCKET_CHECK, NIB, "Mal schauen was die so dabeihaben...", DANGER_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_INNOCENT_WHISTLE, NIB, "*pfeift unschuldig*", SAFE_BARK, CURRENT_ROOM, 10, false),
        def(BarkEvent.NIB_TREASURE_FOUND, NIB, "JACKPOT! ...oh. Oder auch nicht.", DANGER_BARK, CURRENT_ROOM, 30, false),

        def(BarkEvent.BRUGG_ATTACK, BRUGG, "ATTACKE!", UTILITY_BARK, CURRENT_ROOM, 20, true),
        def(BarkEvent.BRUGG_FALL_BACK, BRUGG, "RÜCKZUG! ...oder so.", TRIGGER_BARK, CURRENT_ROOM, 45, false),
        def(BarkEvent.BRUGG_THAT_WASNT_SO_BAD, BRUGG, "Halb so wild.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_WHAT_ARE_YOUR_ORDERS, BRUGG, "Was sind die Befehle?", PRESSURE_BARK, CURRENT_MAP, 60, false),
        def(BarkEvent.BRUGG_HUNGRY, BRUGG, "Ich hab Hunger.", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_PROTECT, BRUGG, "Niemand kommt hier durch!", DANGER_BARK, CURRENT_ROOM, 60, false),
        def(BarkEvent.BRUGG_SMASH, BRUGG, "KAPUTT!", DANGER_BARK, CURRENT_ROOM, 60, false),

        def(BarkEvent.VELLUM_CALLS_FOR_FLAME, VELLUM, "Ignis! Kontrollierte Verbrennung!", UTILITY_BARK, CURRENT_ROOM, 25, true),
        def(BarkEvent.VELLUM_CALLS_FOR_ICE, VELLUM, "Glacies! Kristallstruktur, bitte!", UTILITY_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER, VELLUM, "Wissen ist immer die Antwort.", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING, VELLUM, "Das... das ändert alles!", PRESSURE_BARK, CURRENT_MAP, 90, true),
        def(BarkEvent.VELLUM_TECHNICALLY_CORRECT, VELLUM, "Technisch gesehen ist das korrekt.", SAFE_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.VELLUM_READ_THE_FINE_PRINT, VELLUM, "Steht alles im Kleingedruckten!", TRIGGER_BARK, CURRENT_ROOM, 40, false)
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
