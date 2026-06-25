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
        BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN to "time_waits_for_no_man.wav",

        // ═══ NPC Dialogue ════════════════════════════════════════════════
        BarkEvent.BARKEEP_SPEND_SOME_COIN to "spend_some_coin_or_get_out.wav",
        BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS to "been_playing_in_the_sewers_have_we.wav",
        BarkEvent.PATRON_HE_SURE_IS_SLOW to "he_sure_is_slow_for_a_four_armed_bartender.wav",

        // ═══ Extended Combat Taunts ══════════════════════════════════════
        BarkEvent.NIB_BACK_FOUL_CREATURE to "back_foul_creature.wav",
        BarkEvent.BRUGG_BACK_FOUL_CREATURE to "back_foul_creature.wav",
        BarkEvent.VELLUM_BACK_FOUL_CREATURE to "back_foul_creature.wav",
        BarkEvent.NIB_DARKNESS_TAKE_YOU to "darkness_take_you.wav",
        BarkEvent.BRUGG_DARKNESS_TAKE_YOU to "darkness_take_you.wav",
        BarkEvent.VELLUM_DARKNESS_TAKE_YOU to "darkness_take_you.wav",
        BarkEvent.NIB_DROP_YOUR_WEAPONS to "drop_your_weapons_and_surrender.wav",
        BarkEvent.BRUGG_DROP_YOUR_WEAPONS to "drop_your_weapons_and_surrender.wav",
        BarkEvent.VELLUM_DROP_YOUR_WEAPONS to "drop_your_weapons_and_surrender.wav",
        BarkEvent.NIB_YOU_FIGHT_LIKE_A_NEWBORN to "you_fight_like_a_newborn_babe.wav",
        BarkEvent.BRUGG_YOU_FIGHT_LIKE_A_NEWBORN to "you_fight_like_a_newborn_babe.wav",
        BarkEvent.VELLUM_YOU_FIGHT_LIKE_A_NEWBORN to "you_fight_like_a_newborn_babe.wav",
        BarkEvent.NIB_YOU_FIGHT_LIKE_AN_INFANT to "you_fight_like_an_infant.wav",
        BarkEvent.BRUGG_YOU_FIGHT_LIKE_AN_INFANT to "you_fight_like_an_infant.wav",
        BarkEvent.VELLUM_YOU_FIGHT_LIKE_AN_INFANT to "you_fight_like_an_infant.wav",
        BarkEvent.NIB_YOU_FIGHT_LIKE_YOUR_MOTHER to "you_fight_like_your_mother.wav",
        BarkEvent.BRUGG_YOU_FIGHT_LIKE_YOUR_MOTHER to "you_fight_like_your_mother.wav",
        BarkEvent.VELLUM_YOU_FIGHT_LIKE_YOUR_MOTHER to "you_fight_like_your_mother.wav",
        BarkEvent.NIB_MY_NEXT_STRIKE_WILL_FELL_YOU to "my_next_strike_will_surely_fell_you.wav",
        BarkEvent.BRUGG_MY_NEXT_STRIKE_WILL_FELL_YOU to "my_next_strike_will_surely_fell_you.wav",
        BarkEvent.VELLUM_MY_NEXT_STRIKE_WILL_FELL_YOU to "my_next_strike_will_surely_fell_you.wav",
        BarkEvent.NIB_IVE_FOUGHT_KOBOLDS_TOUGHER to "ive_fought_kobolds_tougher_than_you.wav",
        BarkEvent.BRUGG_IVE_FOUGHT_KOBOLDS_TOUGHER to "ive_fought_kobolds_tougher_than_you.wav",
        BarkEvent.VELLUM_IVE_FOUGHT_KOBOLDS_TOUGHER to "ive_fought_kobolds_tougher_than_you.wav",
        BarkEvent.NIB_IVE_FOUGHT_PUPPIES_TOUGHER to "ive_fought_puppies_tougher_than_you.wav",
        BarkEvent.BRUGG_IVE_FOUGHT_PUPPIES_TOUGHER to "ive_fought_puppies_tougher_than_you.wav",
        BarkEvent.VELLUM_IVE_FOUGHT_PUPPIES_TOUGHER to "ive_fought_puppies_tougher_than_you.wav",
        BarkEvent.NIB_IVE_FOUGHT_ARTICHOKES_TOUGHER to "ive_fought_artichokes_tougher_than_you.wav",
        BarkEvent.BRUGG_IVE_FOUGHT_ARTICHOKES_TOUGHER to "ive_fought_artichokes_tougher_than_you.wav",
        BarkEvent.VELLUM_IVE_FOUGHT_ARTICHOKES_TOUGHER to "ive_fought_artichokes_tougher_than_you.wav",

        // ═══ Victory Barks ═══════════════════════════════════════════════
        BarkEvent.NIB_YOULL_NOT_BE_GETTING_UP to "youll_not_be_getting_up_from_that.wav",
        BarkEvent.BRUGG_YOULL_NOT_BE_GETTING_UP to "youll_not_be_getting_up_from_that.wav",
        BarkEvent.VELLUM_YOULL_NOT_BE_GETTING_UP to "youll_not_be_getting_up_from_that.wav",
        BarkEvent.NIB_NEXT_TIME_WONT_BE_SO_LUCKY to "next_time_you_wont_be_so_lucky.wav",
        BarkEvent.BRUGG_NEXT_TIME_WONT_BE_SO_LUCKY to "next_time_you_wont_be_so_lucky.wav",
        BarkEvent.VELLUM_NEXT_TIME_WONT_BE_SO_LUCKY to "next_time_you_wont_be_so_lucky.wav",

        // ═══ Damage Reaction ═════════════════════════════════════════════
        BarkEvent.NIB_HOW_HUMILIATING to "how_humiliating.wav",
        BarkEvent.BRUGG_HOW_HUMILIATING to "how_humiliating.wav",
        BarkEvent.VELLUM_HOW_HUMILIATING to "how_humiliating.wav",

        // ═══ Loot / Container ════════════════════════════════════════════
        BarkEvent.NIB_OOO_ANOTHER_BARREL to "ooo_another_barrel.wav",
        BarkEvent.BRUGG_OOO_ANOTHER_BARREL to "ooo_another_barrel.wav",
        BarkEvent.NIB_YESSS_ANOTHER_CRATE to "yesss_another_crate.wav",
        BarkEvent.BRUGG_YESSS_ANOTHER_CRATE to "yesss_another_crate.wav",
        BarkEvent.NIB_I_WONDER_WHATS_IN_THIS_ONE to "i_wonder_whats_in_this_one.wav",
        BarkEvent.BRUGG_I_WONDER_WHATS_IN_THIS_ONE to "i_wonder_whats_in_this_one.wav",
        BarkEvent.VELLUM_I_WONDER_WHATS_IN_THIS_ONE to "i_wonder_whats_in_this_one.wav",
        BarkEvent.NIB_THIS_CHEST_UNLOCKED to "this_chest_is_almost_certainly_unlocked.wav",
        BarkEvent.VELLUM_THIS_CHEST_UNLOCKED to "this_chest_is_almost_certainly_unlocked.wav",
        BarkEvent.NIB_THIS_LOOKS_LIKE_TREASURE to "this_looks_like_treasure.wav",
        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TREASURE to "this_looks_like_treasure.wav",
        BarkEvent.VELLUM_THIS_LOOKS_LIKE_TREASURE to "this_looks_like_treasure.wav",
        BarkEvent.NIB_NOTHING_HERE to "nothing_to_see_here.wav",
        BarkEvent.BRUGG_NOTHING_HERE to "nothing_here.wav",
        BarkEvent.VELLUM_NOTHING_HERE to "nothing_here.wav",
        BarkEvent.NIB_IF_IT_GLOWS_MORE_EXPENSIVE to "if_it_glows_does_that_make_it_more_expensive.wav",
        BarkEvent.BRUGG_IF_IT_GLOWS_MORE_EXPENSIVE to "if_it_glows_does_that_make_it_more_expensive.wav",
        BarkEvent.VELLUM_IF_IT_GLOWS_MORE_EXPENSIVE to "if_it_glows_does_that_make_it_more_expensive.wav",
        BarkEvent.NIB_WHY_DO_THEY_LEAVE_LOOT to "why_do_they_leave_all_of_this_perfectly_good_loot_lying_around.wav",
        BarkEvent.BRUGG_WHY_DO_THEY_LEAVE_LOOT to "why_do_they_leave_all_of_this_perfectly_good_loot_lying_around.wav",
        BarkEvent.VELLUM_WHY_DO_THEY_LEAVE_LOOT to "why_do_they_leave_all_of_this_perfectly_good_loot_lying_around.wav",

        // ═══ Exploration / Navigation ════════════════════════════════════
        BarkEvent.NIB_LETS_GET_OUT_OF_HERE to "lets_get_out_of_here.wav",
        BarkEvent.BRUGG_LETS_GET_OUT_OF_HERE to "lets_get_out_of_here.wav",
        BarkEvent.VELLUM_LETS_GET_OUT_OF_HERE to "lets_get_out_of_here.wav",
        BarkEvent.NIB_LETS_HIT_THE_TRAIL to "lets_hit_the_trail.wav",
        BarkEvent.BRUGG_LETS_HIT_THE_TRAIL to "lets_hit_the_trail.wav",
        BarkEvent.NIB_LETS_BE_UNDERWAY to "lets_be_underway.wav",
        BarkEvent.BRUGG_LETS_BE_UNDERWAY to "lets_be_underway.wav",
        BarkEvent.VELLUM_LETS_BE_UNDERWAY to "lets_be_underway.wav",
        BarkEvent.NIB_THIS_PLACE_REEKS_OF_DEATH to "this_places_reeks_of_death.wav",
        BarkEvent.BRUGG_THIS_PLACE_REEKS_OF_DEATH to "this_place_reeks_of_death.wav",
        BarkEvent.VELLUM_THIS_PLACE_REEKS_OF_DEATH to "this_place_reeks_of_death.wav",
        BarkEvent.NIB_WHAT_DARK_DEALINGS to "what_dark_dealings_await_here.wav",
        BarkEvent.BRUGG_WHAT_DARK_DEALINGS to "what_dark_dealings_await_here.wav",
        BarkEvent.VELLUM_WHAT_DARK_DEALINGS to "what_dark_dealings_await_here.wav",
        BarkEvent.NIB_LOOK_OUT to "look_out.wav",
        BarkEvent.BRUGG_LOOK_OUT to "look_out.wav",
        BarkEvent.VELLUM_LOOK_OUT to "look_out.wav",
        BarkEvent.VELLUM_LIGHT_WILL_SCOUR to "light_will_scour_this_place.wav",
        BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING to "this_thing_looks_interesting.wav",
        BarkEvent.BRUGG_THIS_THING_LOOKS_INTERESTING to "this_thing_looks_interesting.wav",
        BarkEvent.VELLUM_THIS_THING_LOOKS_INTERESTING to "this_thing_looks_interesting.wav",

        // ═══ Tavern / Idle Expansion ═════════════════════════════════════
        BarkEvent.NIB_IS_THAT_ROAST to "is_that_roast_i_smell.wav",
        BarkEvent.BRUGG_IS_THAT_ROAST to "is_that_roast_i_smell.wav",
        BarkEvent.VELLUM_IS_THAT_ROAST to "is_that_roast_i_smell.wav",
        BarkEvent.NIB_YOUVE_GOT_TO_TRY_ROAST to "youve_got_to_try_this_roast_cockatrice.wav",
        BarkEvent.BRUGG_YOUVE_GOT_TO_TRY_ROAST to "youve_got_to_try_this_roast_cockatrice.wav",
        BarkEvent.VELLUM_YOUVE_GOT_TO_TRY_ROAST to "youve_got_to_try_this_roast_cockatrice.wav",
        BarkEvent.NIB_I_NEEDED_THAT to "i_needed_that.wav",
        BarkEvent.BRUGG_I_NEEDED_THAT to "i_needed_that.wav",
        BarkEvent.VELLUM_I_NEEDED_THAT to "i_needed_that.wav",
        BarkEvent.NIB_THATS_NOTHING_ALE_WONT_FIX to "thats_nothing_a_flagon_of_ale_wont_fix.wav",
        BarkEvent.BRUGG_THATS_NOTHING_ALE_WONT_FIX to "thats_nothing_a_flagon_of_ale_wont_fix.wav",
        BarkEvent.VELLUM_THATS_NOTHING_ALE_WONT_FIX to "thats_nothing_a_flagon_of_ale_wont_fix.wav",
        BarkEvent.NIB_GREETINGS_FRIEND to "greetings_friend.wav",
        BarkEvent.BRUGG_GREETINGS_FRIEND to "greetings_friend.wav",
        BarkEvent.VELLUM_GREETINGS_STRANGER to "greetings_stranger.wav",
        BarkEvent.NIB_WHAT_NEWS to "what_news.wav",
        BarkEvent.BRUGG_WHAT_NEWS to "what_news.wav",
        BarkEvent.VELLUM_WHAT_NEWS to "what_news.wav",
        BarkEvent.NIB_WHERES_THE_PRIVVY to "wheres_the_privvy.wav",
        BarkEvent.BRUGG_WHERES_THE_PRIVVY to "wheres_the_privvy.wav",
        BarkEvent.VELLUM_WHERES_THE_NEAREST_INN to "wheres_the_nearest_inn.wav",

        // ═══ Chapter 2 ══════════════════════════════════════════════════
        BarkEvent.NIB_SMELL_GOLD to "i_smell_gold.wav",
        BarkEvent.NIB_HOW_MUCH to "how_much_do_you_want_for_this.wav",
        BarkEvent.NIB_SECRET_ENTRANCE to "this_looks_like_a_secret_entrance.wav",
        BarkEvent.BRUGG_WHO_RUNS_THIS_CITY to "who_runs_this_city.wav",
        BarkEvent.BRUGG_SPEAK_TO_GUARD to "i_need_to_speak_to_the_town_guard.wav",
        BarkEvent.BRUGG_KEEP_TO_TRAIL to "just_keep_to_the_trail.wav",
        BarkEvent.BRUGG_EXPERIENCE_IS_HOW_WE_GROW to "experience_is_how_we_grow.wav",
        BarkEvent.VELLUM_CREATURES_IN_WOODS to "there_are_all_manner_of_creature_in_these_woods.wav",
        BarkEvent.VELLUM_ELEMENTS_MINE_TO_COMMAND to "the_elements_are_mine_to_command.wav",
        BarkEvent.VELLUM_CALLS_FOR_LIGHTNING to "this_calls_for_lightning.wav",
        BarkEvent.VELLUM_SO_THATS_HOW_IT_IS to "so_thats_how_it_is_then.wav",
        BarkEvent.VELLUM_BALANCE_LIFE_DEATH to "the_balance_of_life_and_death_sits_on_a_knifes_edge.wav",
        BarkEvent.MERCHANT_SEE_IF_THIS_STRIKES_FANCY to "see_if_any_of_this_strikes_your_fancy.wav",
        BarkEvent.MERCHANT_MAKE_ME_AN_OFFER to "make_me_an_offer.wav",
        BarkEvent.MERCHANT_NAME_YOUR_PRICE to "name_your_price.wav",
        BarkEvent.GUARD_BACK_ALREADY to "been_playing_in_the_sewers_have_we.wav"
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
