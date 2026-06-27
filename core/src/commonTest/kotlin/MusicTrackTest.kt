import rpg.Chapter
import rpg.music.MusicTrack
import rpg.music.MusicTrackKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The canonical soundtrack mapping (docs/SONGBOOK.md). */
class MusicTrackTest {

    @Test
    fun titleThemeIsTheOnlyNonChapterTrack() {
        assertEquals(MusicTrackKind.TITLE, MusicTrack.title.kind)
        assertNull(MusicTrack.title.chapter)
        val titles = MusicTrack.entries.filter { it.kind == MusicTrackKind.TITLE }
        assertEquals(listOf(MusicTrack.TITLE_QUEST_ACCEPTED), titles)
    }

    @Test
    fun everyBossChapterHasExactlyOneBossTheme() {
        for (chapter in Chapter.withBoss()) {
            val theme = MusicTrack.bossThemeFor(chapter)
            assertNotNull(theme, "Chapter $chapter should have a boss theme")
            assertEquals(chapter, theme.chapter)
            assertEquals(MusicTrackKind.BOSS_THEME, theme.kind)
        }
    }

    @Test
    fun chaptersWithoutABossHaveNoBossTheme() {
        assertNull(MusicTrack.bossThemeFor(Chapter.PROLOGUE))
        assertNull(MusicTrack.bossThemeFor(Chapter.FINALE))
    }

    @Test
    fun bossThemeCountMatchesBossChapterCount() {
        val bossThemes = MusicTrack.entries.filter { it.kind == MusicTrackKind.BOSS_THEME }
        assertEquals(Chapter.withBoss().size, bossThemes.size)
    }

    @Test
    fun assetBaseNamesAreUniqueAndUnderMusicDir() {
        val bases = MusicTrack.entries.map { it.baseFileName }
        assertEquals(bases.size, bases.toSet().size, "base file names must be unique")
        for (track in MusicTrack.entries) {
            assertTrue(track.resourceBase.startsWith("music/"))
            assertTrue(track.baseFileName.matches(Regex("^[a-z0-9_]+$")))
        }
    }
}
