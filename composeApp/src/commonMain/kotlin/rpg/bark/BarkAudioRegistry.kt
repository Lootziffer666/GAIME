package rpg.bark

/**
 * Maps every [BarkEvent] to the WAV resource path for its owning character.
 *
 * Paths are relative to the Compose Resources `files/` root, e.g.
 * `"bark/nib/i_smell_treasure.wav"`. The character subdirectory is determined
 * by the [PartyCharacter] from [BarkRegistry].
 *
 * Filenames use the sanitized convention from FEAT-001: lowercase, underscores
 * for spaces, no special characters except hyphens already in the source.
 */
object BarkAudioRegistry {

    /**
     * WAV filename (without the directory prefix) for each [BarkEvent].
     * The full resource path is assembled by [pathFor].
     */
    private val wavFilenames: Map<BarkEvent, String> = mapOf(
        // ─── Original Nib barks ──────────────────────────────────────────
        BarkEvent.NIB_SMELL_TREASURE to "i_smell_treasure.wav",
        BarkEvent.NIB_SMELL_SEWAGE to "i_smell_sewage.wav",
        BarkEvent.NIB_IT_WASNT_ME to "it_wasnt_me.wav",
        BarkEvent.NIB_SHORTCUT to "this_way.wav",
        BarkEvent.NIB_POCKET_CHECK to "i_cant_believe_people_are_so_trusting.wav",
        BarkEvent.NIB_INNOCENT_WHISTLE to "nothing_to_see_here.wav",
        BarkEvent.NIB_TREASURE_FOUND to "i_love_gold.wav",

        // ─── Original Brugg barks ────────────────────────────────────────
        BarkEvent.BRUGG_ATTACK to "attack.wav",
        BarkEvent.BRUGG_FALL_BACK to "fall_back.wav",
        BarkEvent.BRUGG_THAT_WASNT_SO_BAD to "its_just_a_scratch.wav",
        BarkEvent.BRUGG_WHAT_ARE_YOUR_ORDERS to "what_are_your_orders.wav",
        BarkEvent.BRUGG_HUNGRY to "stew_again.wav",
        BarkEvent.BRUGG_PROTECT to "stay_down.wav",
        BarkEvent.BRUGG_SMASH to "take_this.wav",

        // ─── Original Vellum barks ───────────────────────────────────────
        BarkEvent.VELLUM_CALLS_FOR_FLAME to "this_calls_for_flame.wav",
        BarkEvent.VELLUM_CALLS_FOR_ICE to "this_calls_for_ice.wav",
        BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER to "knowledge_is_the_answer.wav",
        BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING to "this_is_unusual.wav",
        BarkEvent.VELLUM_TECHNICALLY_CORRECT to "certainly.wav",
        BarkEvent.VELLUM_READ_THE_FINE_PRINT to "hard-won_knowledge.wav",

        // ═══ Combat Taunts ═══════════════════════════════════════════════
        BarkEvent.NIB_IS_THAT_ALL_YOUVE_GOT to "is_that_all_youve_got.wav",
        BarkEvent.NIB_YOUR_DEFENSES_ARE_WEAK to "your_defenses_are_weak.wav",
        BarkEvent.NIB_FROM_THE_SHADOWS to "from_the_shadows.wav",
        BarkEvent.BRUGG_HAVE_AT_THEE to "have_at_thee.wav",
        BarkEvent.BRUGG_SURRENDER_OR_DIE to "surrender_or_die.wav",
        BarkEvent.BRUGG_SHOW_YOURSELVES to "show_yourselves.wav",
        BarkEvent.VELLUM_LETS_SEE_IF_YOU_CAN_DODGE to "lets_see_if_you_can_dodge_this_one.wav",
        BarkEvent.VELLUM_YOUR_DEFENSES_ARE_FUTILE to "your_defenses_are_futile.wav",
        BarkEvent.VELLUM_I_SMITE_YOU to "i_smite_you.wav",

        // ═══ Combat Reactions (damage/dying) ═════════════════════════════
        BarkEvent.NIB_THAT_STINGS to "that_stings.wav",
        BarkEvent.NIB_LUCKY_HIT to "lucky_hit.wav",
        BarkEvent.NIB_AVENGE_ME to "avenge_me.wav",
        BarkEvent.BRUGG_THATS_GOING_TO_LEAVE_A_MARK to "thats_going_to_leave_a_mark.wav",
        BarkEvent.BRUGG_I_DONT_HAVE_MUCH_LEFT to "i_dont_have_much_left_in_me.wav",
        BarkEvent.BRUGG_THAT_DREW_BLOOD to "that_drew_blood.wav",
        BarkEvent.VELLUM_IM_GOING_TO_FEEL_THAT to "im_going_to_feel_that_one_in_the_morning.wav",
        BarkEvent.VELLUM_I_NEED_A_HEALER to "i_need_a_healer.wav",
        BarkEvent.VELLUM_I_DIDNT_THINK_IT_WOULD_END to "i_didnt_think_it_would_end_like_this.wav",

        // ═══ Combat Reactions (healing/recovery) ═════════════════════════
        BarkEvent.NIB_GOOD_AS_NEW to "good_as_new.wav",
        BarkEvent.BRUGG_I_FEEL_BETTER_THAN_EVER to "i_feel_better_than_ever.wav",
        BarkEvent.VELLUM_IM_BACK_ON_MY_FEET to "im_back_on_my_feet.wav",

        // ═══ Exploration / Discovery ═════════════════════════════════════
        BarkEvent.NIB_ITS_A_TRAP to "its_a_trap.wav",
        BarkEvent.NIB_THEYRE_ONTO_US to "theyre_onto_us.wav",
        BarkEvent.NIB_WHAT_DO_WE_HAVE_HERE to "what_do_we_have_here.wav",
        BarkEvent.BRUGG_THE_DEEPER_WE_GO to "the_deeper_we_go_the_darker_it_gets.wav",
        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE to "this_looks_like_trouble.wav",
        BarkEvent.BRUGG_SURPRISE_SURPRISE to "surprise_surprise.wav",
        BarkEvent.VELLUM_TREES_HAVE_EYES to "its_as_though_the_trees_have_eyes.wav",
        BarkEvent.VELLUM_HMM_WONDER_WHAT_THIS_IS to "hmm_i_wonder_what_this_could_be.wav",
        BarkEvent.VELLUM_THIS_LOOKS_LIKE_A_GLYPH to "this_looks_like_a_glyph.wav",

        // ═══ Ambient / Idle ══════════════════════════════════════════════
        BarkEvent.NIB_I_LOVE_GOLD to "i_love_gold.wav",
        BarkEvent.NIB_THERES_A_HOLE_IN_MY_BOOT to "theres_a_hole_in_my_boot.wav",
        BarkEvent.NIB_STEW_AGAIN to "stew_again.wav",
        BarkEvent.BRUGG_BARKEEP_A_FLAGON to "barkeep_a_flagon_of_ale.wav",
        BarkEvent.BRUGG_GRAB_YOUR_TORCH to "grab_your_torch_theres_work_to_be_done.wav",
        BarkEvent.BRUGG_WHERE_DID_I_PUT_THAT_MAP to "where_did_i_put_that_map.wav",
        BarkEvent.VELLUM_NOW_WHAT_WAS_THAT_INCANTATION to "now_what_was_that_incantation.wav",
        BarkEvent.VELLUM_OF_ALL_THE_ARCANE_LORE to "of_all_the_arcane_lore.wav",
        BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN to "time_waits_for_no_man.wav"
    )

    init {
        val missing = BarkEvent.entries.filterNot { wavFilenames.containsKey(it) }
        require(missing.isEmpty()) {
            "BarkAudioRegistry missing WAV mappings for: $missing"
        }
    }

    /**
     * Returns the Compose Resource path for the given [event].
     *
     * The path is relative to the `files/` resource root, e.g.
     * `"bark/nib/i_smell_treasure.wav"`.
     */
    fun pathFor(event: BarkEvent): String {
        val character = BarkRegistry[event].character
        val directory = character.audioDirectory
        val filename = wavFilenames.getValue(event)
        return "bark/$directory/$filename"
    }

    /**
     * Returns just the WAV filename (no directory) for the given [event].
     */
    fun filenameFor(event: BarkEvent): String = wavFilenames.getValue(event)

    /** The audio subdirectory name for each character. */
    private val PartyCharacter.audioDirectory: String
        get() = when (this) {
            PartyCharacter.NIB -> "nib"
            PartyCharacter.BRUGG -> "brugg"
            PartyCharacter.VELLUM -> "vellum"
        }
}
