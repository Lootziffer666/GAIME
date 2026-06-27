package rpg.bark

import rpg.SlicePhase
import kotlin.random.Random

/**
 * Central, testable pools for non-combat barks: exploration/discovery lines
 * fired from scene triggers and the ambient idle barks fired after a period of
 * inactivity.
 *
 * Selection takes an injected [Random] so behaviour is reproducible in tests,
 * matching the seeded-random pattern used by [rpg.combat.CombatEngine] (review
 * issue #3). The UI passes a single long-lived [Random]; tests pass a seeded one.
 */
object AmbientBarks {

    /** Spoken when the party drops into the sewers from the tavern cellar. */
    val SEWER_ENTRY: List<BarkEvent> = listOf(
        BarkEvent.BRUGG_THE_DEEPER_WE_GO,
        BarkEvent.VELLUM_TREES_HAVE_EYES
    )

    /** Atmosphere line on entering the boss room at the end of the sewer. */
    val SEWER_ATMOSPHERE: List<BarkEvent> = listOf(
        BarkEvent.NIB_THIS_PLACE_REEKS_OF_DEATH,
        BarkEvent.BRUGG_THIS_PLACE_REEKS_OF_DEATH,
        BarkEvent.VELLUM_THIS_PLACE_REEKS_OF_DEATH,
        BarkEvent.NIB_WHAT_DARK_DEALINGS,
        BarkEvent.BRUGG_WHAT_DARK_DEALINGS,
        BarkEvent.VELLUM_WHAT_DARK_DEALINGS
    )

    /** Warning when the party first encounters sewer enemies. */
    val ENEMY_WARNING: List<BarkEvent> = listOf(
        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE,
        BarkEvent.NIB_THEYRE_ONTO_US,
        BarkEvent.NIB_LOOK_OUT,
        BarkEvent.BRUGG_LOOK_OUT,
        BarkEvent.VELLUM_LOOK_OUT
    )

    /** Warning when the party first encounters forest enemies (Chapter 2). */
    val FOREST_WARNING: List<BarkEvent> = listOf(
        BarkEvent.BRUGG_THIS_LOOKS_LIKE_TROUBLE,
        BarkEvent.NIB_THEYRE_ONTO_US,
        BarkEvent.NIB_LOOK_OUT
    )

    /** Spoken when the boss is first discovered. */
    val BOSS_DISCOVERY: List<BarkEvent> = listOf(
        BarkEvent.NIB_WHAT_DO_WE_HAVE_HERE,
        BarkEvent.VELLUM_HMM_WONDER_WHAT_THIS_IS
    )

    /** Idle ambient barks keyed by exploration phase. */
    fun idleBarks(phase: SlicePhase): List<BarkEvent> = when (phase) {
        SlicePhase.TAVERN -> listOf(
            BarkEvent.BRUGG_BARKEEP_A_FLAGON,
            BarkEvent.NIB_STEW_AGAIN,
            BarkEvent.VELLUM_NOW_WHAT_WAS_THAT_INCANTATION,
            BarkEvent.NIB_I_LOVE_GOLD,
            BarkEvent.BRUGG_WHERE_DID_I_PUT_THAT_MAP,
            BarkEvent.NIB_IS_THAT_ROAST,
            BarkEvent.BRUGG_IS_THAT_ROAST,
            BarkEvent.VELLUM_YOUVE_GOT_TO_TRY_ROAST,
            BarkEvent.BRUGG_THATS_NOTHING_ALE_WONT_FIX,
            BarkEvent.NIB_WHERES_THE_PRIVVY,
            BarkEvent.BRUGG_WHAT_NEWS,
            BarkEvent.VELLUM_WHERES_THE_NEAREST_INN
        )
        SlicePhase.SEWER, SlicePhase.BOSS_ROOM -> listOf(
            BarkEvent.BRUGG_GRAB_YOUR_TORCH,
            BarkEvent.NIB_THERES_A_HOLE_IN_MY_BOOT,
            BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN,
            BarkEvent.VELLUM_OF_ALL_THE_ARCANE_LORE,
            BarkEvent.NIB_I_LOVE_GOLD,
            BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.BRUGG_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.VELLUM_THIS_THING_LOOKS_INTERESTING,
            BarkEvent.NIB_LETS_GET_OUT_OF_HERE,
            BarkEvent.BRUGG_LETS_GET_OUT_OF_HERE
        )
        SlicePhase.CHAPTER2_MARKET -> listOf(
            BarkEvent.NIB_HOW_MUCH,
            BarkEvent.BRUGG_KEEP_TO_TRAIL,
            BarkEvent.BRUGG_BARKEEP_A_FLAGON,
            BarkEvent.NIB_I_LOVE_GOLD
        )
        SlicePhase.CHAPTER2_FOREST, SlicePhase.CHAPTER2_SHRINE -> listOf(
            BarkEvent.VELLUM_CREATURES_IN_WOODS,
            BarkEvent.BRUGG_KEEP_TO_TRAIL,
            BarkEvent.NIB_THIS_THING_LOOKS_INTERESTING
        )
        else -> emptyList()
    }

    /** Picks a random idle bark for [phase], or null if the phase has no idle pool. */
    fun pickIdle(phase: SlicePhase, random: Random): BarkEvent? =
        pick(idleBarks(phase), random)

    /** Picks a uniformly random bark from [pool], or null if the pool is empty. */
    fun pick(pool: List<BarkEvent>, random: Random): BarkEvent? =
        if (pool.isEmpty()) null else pool[random.nextInt(pool.size)]
}
