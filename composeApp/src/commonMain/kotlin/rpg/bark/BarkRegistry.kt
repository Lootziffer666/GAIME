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
        def(BarkEvent.PATRON_HE_SURE_IS_SLOW, VELLUM, "He sure is slow for a four-armed bartender.", SAFE_BARK, CURRENT_ROOM, 0, true),

        // ═══ Extended Combat Taunts ══════════════════════════════════════
        def(BarkEvent.NIB_BACK_FOUL_CREATURE, NIB, "Back, foul creature!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_BACK_FOUL_CREATURE, BRUGG, "Back, foul creature!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_BACK_FOUL_CREATURE, VELLUM, "Back, foul creature!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_DARKNESS_TAKE_YOU, NIB, "Darkness take you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_DARKNESS_TAKE_YOU, BRUGG, "Darkness take you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_DARKNESS_TAKE_YOU, VELLUM, "Darkness take you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_DROP_YOUR_WEAPONS, NIB, "Drop your weapons and surrender!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_DROP_YOUR_WEAPONS, BRUGG, "Drop your weapons and surrender!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_DROP_YOUR_WEAPONS, VELLUM, "Drop your weapons and surrender!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_YOU_FIGHT_LIKE_A_NEWBORN, NIB, "You fight like a newborn babe!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_YOU_FIGHT_LIKE_A_NEWBORN, BRUGG, "You fight like a newborn babe!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_YOU_FIGHT_LIKE_A_NEWBORN, VELLUM, "You fight like a newborn babe!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_YOU_FIGHT_LIKE_AN_INFANT, NIB, "You fight like an infant!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_YOU_FIGHT_LIKE_AN_INFANT, BRUGG, "You fight like an infant!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_YOU_FIGHT_LIKE_AN_INFANT, VELLUM, "You fight like an infant!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_YOU_FIGHT_LIKE_YOUR_MOTHER, NIB, "You fight like your mother!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_YOU_FIGHT_LIKE_YOUR_MOTHER, BRUGG, "You fight like your mother!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_YOU_FIGHT_LIKE_YOUR_MOTHER, VELLUM, "You fight like your mother!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_MY_NEXT_STRIKE_WILL_FELL_YOU, NIB, "My next strike will surely fell you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_MY_NEXT_STRIKE_WILL_FELL_YOU, BRUGG, "My next strike will surely fell you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_MY_NEXT_STRIKE_WILL_FELL_YOU, VELLUM, "My next strike will surely fell you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_IVE_FOUGHT_KOBOLDS_TOUGHER, NIB, "I've fought kobolds tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_IVE_FOUGHT_KOBOLDS_TOUGHER, BRUGG, "I've fought kobolds tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_IVE_FOUGHT_KOBOLDS_TOUGHER, VELLUM, "I've fought kobolds tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_IVE_FOUGHT_PUPPIES_TOUGHER, NIB, "I've fought puppies tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_IVE_FOUGHT_PUPPIES_TOUGHER, BRUGG, "I've fought puppies tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_IVE_FOUGHT_PUPPIES_TOUGHER, VELLUM, "I've fought puppies tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.NIB_IVE_FOUGHT_ARTICHOKES_TOUGHER, NIB, "I've fought artichokes tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_IVE_FOUGHT_ARTICHOKES_TOUGHER, BRUGG, "I've fought artichokes tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_IVE_FOUGHT_ARTICHOKES_TOUGHER, VELLUM, "I've fought artichokes tougher than you!", SAFE_BARK, CURRENT_ROOM, 20, false),

        // ═══ Victory Barks ═══════════════════════════════════════════════
        def(BarkEvent.NIB_YOULL_NOT_BE_GETTING_UP, NIB, "You'll not be getting up from that!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_YOULL_NOT_BE_GETTING_UP, BRUGG, "You'll not be getting up from that!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_YOULL_NOT_BE_GETTING_UP, VELLUM, "You'll not be getting up from that!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_NEXT_TIME_WONT_BE_SO_LUCKY, NIB, "Next time you won't be so lucky!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_NEXT_TIME_WONT_BE_SO_LUCKY, BRUGG, "Next time you won't be so lucky!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_NEXT_TIME_WONT_BE_SO_LUCKY, VELLUM, "Next time you won't be so lucky!", SAFE_BARK, CURRENT_ROOM, 25, false),

        // ═══ Damage Reaction ═════════════════════════════════════════════
        def(BarkEvent.NIB_HOW_HUMILIATING, NIB, "How humiliating!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_HOW_HUMILIATING, BRUGG, "How humiliating!", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_HOW_HUMILIATING, VELLUM, "How humiliating!", SAFE_BARK, CURRENT_ROOM, 20, false),

        // ═══ Loot / Container ════════════════════════════════════════════
        def(BarkEvent.NIB_OOO_ANOTHER_BARREL, NIB, "Ooo, another barrel!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_OOO_ANOTHER_BARREL, BRUGG, "Ooo, another barrel!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_YESSS_ANOTHER_CRATE, NIB, "Yesss, another crate!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_YESSS_ANOTHER_CRATE, BRUGG, "Yess, another crate!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_I_WONDER_WHATS_IN_THIS_ONE, NIB, "I wonder what's in this one.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_I_WONDER_WHATS_IN_THIS_ONE, BRUGG, "I wonder what's in this one.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_I_WONDER_WHATS_IN_THIS_ONE, VELLUM, "I wonder what's in this one.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_THIS_CHEST_UNLOCKED, NIB, "This chest is almost certainly unlocked.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_THIS_CHEST_UNLOCKED, VELLUM, "This chest is almost certainly unlocked.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_THIS_LOOKS_LIKE_TREASURE, NIB, "This looks like treasure!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_THIS_LOOKS_LIKE_TREASURE, BRUGG, "This looks like treasure!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_THIS_LOOKS_LIKE_TREASURE, VELLUM, "This looks like treasure!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_NOTHING_HERE, NIB, "Nothing to see here.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_NOTHING_HERE, BRUGG, "Nothing here.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_NOTHING_HERE, VELLUM, "Nothing here.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_IF_IT_GLOWS_MORE_EXPENSIVE, NIB, "If it glows, does that make it more expensive?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_IF_IT_GLOWS_MORE_EXPENSIVE, BRUGG, "If it glows, does that make it more expensive?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_IF_IT_GLOWS_MORE_EXPENSIVE, VELLUM, "If it glows, does that make it more expensive?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_WHY_DO_THEY_LEAVE_LOOT, NIB, "Why do they leave all of this perfectly good loot lying around?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_WHY_DO_THEY_LEAVE_LOOT, BRUGG, "Why do they leave all of this perfectly good loot lying around?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_WHY_DO_THEY_LEAVE_LOOT, VELLUM, "Why do they leave all of this perfectly good loot lying around?", SAFE_BARK, CURRENT_ROOM, 30, false),

        // ═══ Exploration / Navigation ════════════════════════════════════
        def(BarkEvent.NIB_LETS_GET_OUT_OF_HERE, NIB, "Let's get out of here!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_LETS_GET_OUT_OF_HERE, BRUGG, "Let's get out of here!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_LETS_GET_OUT_OF_HERE, VELLUM, "Let's get out of here!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_LETS_HIT_THE_TRAIL, NIB, "Let's hit the trail!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_LETS_HIT_THE_TRAIL, BRUGG, "Let's hit the trail!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_LETS_BE_UNDERWAY, NIB, "Let's be underway.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_LETS_BE_UNDERWAY, BRUGG, "Let's be underway.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_LETS_BE_UNDERWAY, VELLUM, "Let's be underway.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_THIS_PLACE_REEKS_OF_DEATH, NIB, "This place reeks of death.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_THIS_PLACE_REEKS_OF_DEATH, BRUGG, "This place reeks of death.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_THIS_PLACE_REEKS_OF_DEATH, VELLUM, "This place reeks of death.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_WHAT_DARK_DEALINGS, NIB, "What dark dealings await here?", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_WHAT_DARK_DEALINGS, BRUGG, "What dark dealings await here?", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_WHAT_DARK_DEALINGS, VELLUM, "What dark dealings await here?", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_LOOK_OUT, NIB, "Look out!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_LOOK_OUT, BRUGG, "Look out!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_LOOK_OUT, VELLUM, "Look out!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_LIGHT_WILL_SCOUR, VELLUM, "Light will scour this place!", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING, NIB, "This thing looks interesting.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_THIS_THING_LOOKS_INTERESTING, BRUGG, "This thing looks interesting.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.VELLUM_THIS_THING_LOOKS_INTERESTING, VELLUM, "This thing looks interesting.", SAFE_BARK, CURRENT_ROOM, 25, false),

        // ═══ Tavern / Idle Expansion ═════════════════════════════════════
        def(BarkEvent.NIB_IS_THAT_ROAST, NIB, "Is that roast I smell?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_IS_THAT_ROAST, BRUGG, "Is that roast I smell?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_IS_THAT_ROAST, VELLUM, "Is that roast I smell?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_YOUVE_GOT_TO_TRY_ROAST, NIB, "You've got to try this roast cockatrice!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_YOUVE_GOT_TO_TRY_ROAST, BRUGG, "You've got to try this roast cockatrice!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_YOUVE_GOT_TO_TRY_ROAST, VELLUM, "You've got to try this roast cockatrice!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_I_NEEDED_THAT, NIB, "I needed that.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_I_NEEDED_THAT, BRUGG, "I needed that.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_I_NEEDED_THAT, VELLUM, "I needed that.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_THATS_NOTHING_ALE_WONT_FIX, NIB, "That's nothing a flagon of ale won't fix!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_THATS_NOTHING_ALE_WONT_FIX, BRUGG, "That's nothing a flagon of ale won't fix!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_THATS_NOTHING_ALE_WONT_FIX, VELLUM, "That's nothing a flagon of ale won't fix!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_GREETINGS_FRIEND, NIB, "Greetings, friend!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_GREETINGS_FRIEND, BRUGG, "Greetings, friend!", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_GREETINGS_STRANGER, VELLUM, "Greetings, stranger.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_WHAT_NEWS, NIB, "What news?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_WHAT_NEWS, BRUGG, "What news?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_WHAT_NEWS, VELLUM, "What news?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_WHERES_THE_PRIVVY, NIB, "Where's the privvy?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_WHERES_THE_PRIVVY, BRUGG, "Where's the privvy?", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_WHERES_THE_NEAREST_INN, VELLUM, "Where's the nearest inn?", SAFE_BARK, CURRENT_ROOM, 30, false),

        // ═══ Chapter 2 ══════════════════════════════════════════════════
        def(BarkEvent.NIB_SMELL_GOLD, NIB, "I smell gold!", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.NIB_HOW_MUCH, NIB, "How much do you want for this?", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.NIB_SECRET_ENTRANCE, NIB, "This looks like a secret entrance.", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_WHO_RUNS_THIS_CITY, BRUGG, "Who runs this city?", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_SPEAK_TO_GUARD, BRUGG, "I need to speak to the town guard.", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_KEEP_TO_TRAIL, BRUGG, "Just keep to the trail.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.BRUGG_EXPERIENCE_IS_HOW_WE_GROW, BRUGG, "Experience is how we grow.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_CREATURES_IN_WOODS, VELLUM, "There are all manner of creature in these woods.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_ELEMENTS_MINE_TO_COMMAND, VELLUM, "The elements are mine to command.", TRIGGER_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_CALLS_FOR_LIGHTNING, VELLUM, "This calls for lightning!", UTILITY_BARK, CURRENT_ROOM, 25, true),
        def(BarkEvent.VELLUM_SO_THATS_HOW_IT_IS, VELLUM, "So that's how it is then.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.VELLUM_BALANCE_LIFE_DEATH, VELLUM, "The balance of life and death sits on a knife's edge.", SAFE_BARK, CURRENT_ROOM, 30, true),
        def(BarkEvent.MERCHANT_SEE_IF_THIS_STRIKES_FANCY, BRUGG, "See if any of this strikes your fancy.", SAFE_BARK, CURRENT_ROOM, 0, true),
        def(BarkEvent.MERCHANT_MAKE_ME_AN_OFFER, BRUGG, "Make me an offer.", SAFE_BARK, CURRENT_ROOM, 0, true),
        def(BarkEvent.MERCHANT_NAME_YOUR_PRICE, BRUGG, "Name your price.", SAFE_BARK, CURRENT_ROOM, 0, true),
        def(BarkEvent.GUARD_BACK_ALREADY, BRUGG, "Been playing in the sewers, have we?", SAFE_BARK, CURRENT_ROOM, 0, true),

        // ═══ Campaign: Chapters 3-5 + Finale (docs/CAMPAIGN.md) ══════════
        def(BarkEvent.BRUGG_OBJECTIVE_COMPLETE, BRUGG, "Objective complete.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.VELLUM_HARD_WON_KNOWLEDGE, VELLUM, "Hard-won knowledge.", SAFE_BARK, CURRENT_ROOM, 30, false),
        // ─── Chapter 3 ───────────────────────────────────────────────────
        def(BarkEvent.VELLUM_WHICH_DIRECTION, VELLUM, "Which direction?", TRIGGER_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_MAP, VELLUM, "This looks like a map.", TRIGGER_BARK, CURRENT_ROOM, 25, false),
        // ─── Chapter 4 ───────────────────────────────────────────────────
        def(BarkEvent.NIB_FRESH_SEA_AIR, NIB, "I love the fresh sea air.", SAFE_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.NIB_NOT_FOND_OF_DEEP_WATER, NIB, "I've never been fond of deep water.", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_IS_SHE_SEAWORTHY, BRUGG, "Is she seaworthy?", SAFE_BARK, CURRENT_ROOM, 25, false),
        def(BarkEvent.BRUGG_DROP_ANCHOR, BRUGG, "Drop anchor.", UTILITY_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_HOIST_ANCHOR, BRUGG, "Hoist anchor.", UTILITY_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_RAISE_THE_SAIL, BRUGG, "Raise the sail.", UTILITY_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.BRUGG_OUT_MANEUVERED, BRUGG, "We're out maneuvered.", DANGER_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_RETREAT, BRUGG, "Retreat.", DANGER_BARK, CURRENT_ROOM, 15, false),
        def(BarkEvent.VELLUM_SEA_IS_ANGRY_MISTRESS, VELLUM, "The sea is an angry mistress.", SAFE_BARK, CURRENT_ROOM, 30, false),
        // ─── Chapter 5 ───────────────────────────────────────────────────
        def(BarkEvent.NIB_SMELL_DRAGON, NIB, "I smell dragon.", TRIGGER_BARK, CURRENT_MAP, 45, false),
        def(BarkEvent.NIB_SMELL_MONSTERS, NIB, "I smell monsters.", DANGER_BARK, CURRENT_ROOM, 30, false),
        def(BarkEvent.BRUGG_HOLD_THE_LINE, BRUGG, "Hold the line.", PRESSURE_BARK, CURRENT_MAP, 30, false),
        def(BarkEvent.BRUGG_PROTECT_THE_ASSET, BRUGG, "Protect the asset.", PRESSURE_BARK, CURRENT_MAP, 30, false),
        def(BarkEvent.BRUGG_LOW_ON_HEALTH, BRUGG, "Low on health.", SAFE_BARK, CURRENT_ROOM, 20, false),
        def(BarkEvent.VELLUM_SUMMON_YOUR_STRENGTH, VELLUM, "Summon your strength.", SAFE_BARK, CURRENT_ROOM, 25, false),
        // ─── Finale: System Overload ─────────────────────────────────────
        def(BarkEvent.NIB_NOT_A_HORSE, NIB, "Hey, that's not a horse.", TRIGGER_BARK, CURRENT_ROOM, 5, false),
        def(BarkEvent.NIB_THIS_LOOKS_LIKE_GOLD, NIB, "This looks like gold.", TRIGGER_BARK, CURRENT_ROOM, 5, false),
        def(BarkEvent.NIB_DOOR_ALMOST_UNLOCKED, NIB, "This door is almost certainly unlocked.", UTILITY_BARK, CURRENT_ROOM, 5, false)
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
