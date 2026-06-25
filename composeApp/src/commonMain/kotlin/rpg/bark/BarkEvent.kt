package rpg.bark

/**
 * Every bark in the game is a stable enum key. The Questbook reacts to these
 * keys -- never to audio analysis (see docs/GAME_CONCEPT_LOCK.md).
 *
 * The full set mirrors the Bark Table in docs/BARK_TRIGGER_TABLE.md.
 */
enum class BarkEvent {
    // ─── Nib -- thief/rogue, greed and escape ────────────────────────────
    NIB_SMELL_TREASURE,
    NIB_SMELL_SEWAGE,
    NIB_IT_WASNT_ME,
    NIB_SHORTCUT,
    NIB_POCKET_CHECK,
    NIB_INNOCENT_WHISTLE,
    NIB_TREASURE_FOUND,

    // ─── Brugg -- warrior, combat-obligation and duty ────────────────────
    BRUGG_ATTACK,
    BRUGG_FALL_BACK,
    BRUGG_THAT_WASNT_SO_BAD,
    BRUGG_WHAT_ARE_YOUR_ORDERS,
    BRUGG_HUNGRY,
    BRUGG_PROTECT,
    BRUGG_SMASH,

    // ─── Vellum -- mage, knowledge and elemental contracts ───────────────
    VELLUM_CALLS_FOR_FLAME,
    VELLUM_CALLS_FOR_ICE,
    VELLUM_KNOWLEDGE_IS_THE_ANSWER,
    VELLUM_THIS_CHANGES_EVERYTHING,
    VELLUM_TECHNICALLY_CORRECT,
    VELLUM_READ_THE_FINE_PRINT,

    // ═══ Combat Taunts ═══════════════════════════════════════════════════
    NIB_IS_THAT_ALL_YOUVE_GOT,
    NIB_YOUR_DEFENSES_ARE_WEAK,
    NIB_FROM_THE_SHADOWS,
    BRUGG_HAVE_AT_THEE,
    BRUGG_SURRENDER_OR_DIE,
    BRUGG_SHOW_YOURSELVES,
    VELLUM_LETS_SEE_IF_YOU_CAN_DODGE,
    VELLUM_YOUR_DEFENSES_ARE_FUTILE,
    VELLUM_I_SMITE_YOU,

    // ═══ Combat Reactions (damage/dying) ═════════════════════════════════
    NIB_THAT_STINGS,
    NIB_LUCKY_HIT,
    NIB_AVENGE_ME,
    BRUGG_THATS_GOING_TO_LEAVE_A_MARK,
    BRUGG_I_DONT_HAVE_MUCH_LEFT,
    BRUGG_THAT_DREW_BLOOD,
    VELLUM_IM_GOING_TO_FEEL_THAT,
    VELLUM_I_NEED_A_HEALER,
    VELLUM_I_DIDNT_THINK_IT_WOULD_END,

    // ═══ Combat Reactions (healing/recovery) ═════════════════════════════
    NIB_GOOD_AS_NEW,
    BRUGG_I_FEEL_BETTER_THAN_EVER,
    VELLUM_IM_BACK_ON_MY_FEET,

    // ═══ Exploration / Discovery ═════════════════════════════════════════
    NIB_ITS_A_TRAP,
    NIB_THEYRE_ONTO_US,
    NIB_WHAT_DO_WE_HAVE_HERE,
    BRUGG_THE_DEEPER_WE_GO,
    BRUGG_THIS_LOOKS_LIKE_TROUBLE,
    BRUGG_SURPRISE_SURPRISE,
    VELLUM_TREES_HAVE_EYES,
    VELLUM_HMM_WONDER_WHAT_THIS_IS,
    VELLUM_THIS_LOOKS_LIKE_A_GLYPH,

    // ═══ Ambient / Idle ══════════════════════════════════════════════════
    NIB_I_LOVE_GOLD,
    NIB_THERES_A_HOLE_IN_MY_BOOT,
    NIB_STEW_AGAIN,
    BRUGG_BARKEEP_A_FLAGON,
    BRUGG_GRAB_YOUR_TORCH,
    BRUGG_WHERE_DID_I_PUT_THAT_MAP,
    VELLUM_NOW_WHAT_WAS_THAT_INCANTATION,
    VELLUM_OF_ALL_THE_ARCANE_LORE,
    VELLUM_TIME_WAITS_FOR_NO_MAN
}
