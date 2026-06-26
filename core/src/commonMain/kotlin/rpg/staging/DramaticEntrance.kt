package rpg.staging

/**
 * The Dramatic Entrance system (docs/COMEDY_BIBLE.md). Stages over-the-top
 * boss/character reveals as a scripted sequence of [EntranceBeat]s the renderer
 * plays back: looming shadows, effect storms far too big for the threat,
 * close-ups (extra sprites), pathos proclamations, a reveal, and a deflating
 * punchline.
 *
 * "Ridiculousness with system" is made measurable: [buildupIntensity] (how epic
 * the staging is) versus [actualThreat] (how dangerous it really is). The gap is
 * the joke -- a huge shadow that resolves into a tiny enemy. Pure and
 * renderer-agnostic; KorGE supplies the sprites and effects.
 */
enum class EntranceBeatType {
    /** A vast, ominous shadow grows over the room. */
    SHADOW_LOOM,
    /** An effect storm (lightning, quake, choir...) — intensity is the point. */
    EFFECT_STORM,
    /** Cut to a dramatic close-up sprite (extra art). */
    CLOSE_UP,
    /** A dramatic name/title card. */
    TITLE_CARD,
    /** The subject's self-important pathos speech. */
    PROCLAMATION,
    /** The actual subject is shown — often hilariously small. */
    REVEAL,
    /** A party member punctures the moment with a dumb, honest line. */
    DEFLATE
}

/** Over-the-top stage effects the renderer layers during an [EntranceBeatType.EFFECT_STORM]. */
enum class StageEffect {
    LIGHTNING, THUNDER, QUAKE, FOG, EMBERS, BLOOM, CHOIR, BLOOD_MOON,
    CONFETTI, SPARKLES, MARTIAL_DRUMS, WIND, SCREEN_SHAKE, SLOW_MOTION, SPOTLIGHT
}

/**
 * One beat of an entrance. [text] is proclamation/deflate/title copy;
 * [spriteKey] is the close-up/reveal art the renderer loads; [effects] +
 * [intensity] (0..10) drive the effect storm.
 */
data class EntranceBeat(
    val type: EntranceBeatType,
    val text: String? = null,
    val spriteKey: String? = null,
    val effects: List<StageEffect> = emptyList(),
    val intensity: Int = 0,
    val durationMs: Int = 1500,
    val speaker: String? = null
) {
    init {
        require(intensity in 0..10) { "intensity must be 0..10" }
        require(durationMs > 0) { "durationMs must be positive" }
    }
}

/**
 * A full scripted entrance.
 *
 * @param buildupIntensity how epic the staging is (1..10)
 * @param actualThreat how dangerous the subject really is (1..10)
 */
data class DramaticEntrance(
    val id: String,
    val subjectName: String,
    val buildupIntensity: Int,
    val actualThreat: Int,
    val beats: List<EntranceBeat>
) {
    init {
        require(buildupIntensity in 1..10) { "buildupIntensity must be 1..10" }
        require(actualThreat in 1..10) { "actualThreat must be 1..10" }
        require(beats.isNotEmpty()) { "an entrance needs beats" }
        require(beats.any { it.type == EntranceBeatType.PROCLAMATION }) {
            "$id: every entrance needs a PROCLAMATION (the pathos)"
        }
        require(beats.any { it.type == EntranceBeatType.REVEAL }) {
            "$id: every entrance needs a REVEAL"
        }
        // The bigger the gap, the funnier — and an overhyped entrance MUST be
        // punctured, or it is just pathos.
        if (buildupIntensity - actualThreat >= OVERHYPE_THRESHOLD) {
            require(beats.last().type == EntranceBeatType.DEFLATE) {
                "$id: an overhyped entrance must end on a DEFLATE punchline"
            }
        }
    }

    /** The comedic payoff metric: buildup minus reality. Higher = funnier. */
    val ironyGap: Int get() = buildupIntensity - actualThreat

    /** True when the staging massively oversells a weakling. */
    val isOverhyped: Boolean get() = ironyGap >= OVERHYPE_THRESHOLD

    companion object {
        const val OVERHYPE_THRESHOLD = 4
    }
}
