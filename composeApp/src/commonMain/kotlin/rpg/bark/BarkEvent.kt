package rpg.bark

/**
 * Every bark in the game is a stable enum key. The Questbook reacts to these
 * keys -- never to audio analysis (see docs/GAME_CONCEPT_LOCK.md).
 *
 * The full set mirrors the Bark Table in docs/BARK_TRIGGER_TABLE.md.
 */
enum class BarkEvent {
    // Nib -- thief/rogue, greed and escape
    NIB_SMELL_TREASURE,
    NIB_SMELL_SEWAGE,
    NIB_IT_WASNT_ME,
    NIB_SHORTCUT,
    NIB_POCKET_CHECK,
    NIB_INNOCENT_WHISTLE,
    NIB_TREASURE_FOUND,

    // Brugg -- warrior, combat-obligation and duty
    BRUGG_ATTACK,
    BRUGG_FALL_BACK,
    BRUGG_THAT_WASNT_SO_BAD,
    BRUGG_WHAT_ARE_YOUR_ORDERS,
    BRUGG_HUNGRY,
    BRUGG_PROTECT,
    BRUGG_SMASH,

    // Vellum -- mage, knowledge and elemental contracts
    VELLUM_CALLS_FOR_FLAME,
    VELLUM_CALLS_FOR_ICE,
    VELLUM_KNOWLEDGE_IS_THE_ANSWER,
    VELLUM_THIS_CHANGES_EVERYTHING,
    VELLUM_TECHNICALLY_CORRECT,
    VELLUM_READ_THE_FINE_PRINT
}
