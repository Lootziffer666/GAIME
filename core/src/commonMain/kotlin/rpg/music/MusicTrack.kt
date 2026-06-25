package rpg.music

import rpg.Chapter

/** Whether a track is the title/menu theme or a chapter boss theme. */
enum class MusicTrackKind { TITLE, BOSS_THEME }

/**
 * The game's soundtrack as canonical, renderer-agnostic data (see
 * docs/SONGBOOK.md for the lyrics). One [TITLE] theme plus one [BOSS_THEME] per
 * chapter. The actual audio files live under `assets/audio/music/` and are
 * played by the renderer (KorGE `:game`); this registry only maps chapters to
 * the track that should play and to the expected asset base name.
 */
enum class MusicTrack(
    val displayTitle: String,
    val genre: String,
    val kind: MusicTrackKind,
    /** Chapter this boss theme belongs to, or null for the title theme. */
    val chapter: Chapter?,
    /** Expected asset base name (no extension) under assets/audio/music/. */
    val baseFileName: String
) {
    TITLE_QUEST_ACCEPTED(
        displayTitle = "Quest Accepted: Unfortunately!",
        genre = "Comedy Chiptune / 16-Bit March",
        kind = MusicTrackKind.TITLE,
        chapter = null,
        baseFileName = "title_quest_accepted"
    ),
    BOSS_RAT_ACCOUNTANT(
        displayTitle = "The Rat Accountant (Form 8-B Denied)",
        genre = "16-Bit Bureaucracy Chiptune",
        kind = MusicTrackKind.BOSS_THEME,
        chapter = Chapter.CH1_SEWERS,
        baseFileName = "boss_rat_accountant"
    ),
    BOSS_WARDENS_MANDATE(
        displayTitle = "The Warden's Mandate",
        genre = "16-Bit Symphonic Chiptune / Paradox Loop",
        kind = MusicTrackKind.BOSS_THEME,
        chapter = Chapter.CH2_MARKET,
        baseFileName = "boss_wardens_mandate"
    ),
    BOSS_HELPFUL_TREE(
        displayTitle = "Fanfare For The Seeker",
        genre = "16-Bit JRPG Chiptune / Baroque Pop",
        kind = MusicTrackKind.BOSS_THEME,
        chapter = Chapter.CH3_WOODS,
        baseFileName = "boss_helpful_tree"
    ),
    BOSS_FORMBEARD(
        displayTitle = "Plunder Permit",
        genre = "Chiptune Sea Shanty",
        kind = MusicTrackKind.BOSS_THEME,
        chapter = Chapter.CH4_SHIP,
        baseFileName = "boss_formbeard"
    ),
    BOSS_ADMINISTRAGON(
        displayTitle = "The Administragon",
        genre = "16-Bit Final Boss Symphonic Chiptune",
        kind = MusicTrackKind.BOSS_THEME,
        chapter = Chapter.CH5_DRAGON,
        baseFileName = "boss_administragon"
    );

    /** Renderer-resolved resource base (extension chosen by the player). */
    val resourceBase: String get() = "music/$baseFileName"

    companion object {
        /** The title / main-menu theme. */
        val title: MusicTrack get() = TITLE_QUEST_ACCEPTED

        /** The boss theme for [chapter], or null if that chapter has none. */
        fun bossThemeFor(chapter: Chapter): MusicTrack? =
            entries.firstOrNull { it.kind == MusicTrackKind.BOSS_THEME && it.chapter == chapter }
    }
}
