import rpg.CampaignBoss
import rpg.staging.DramaticEntrance
import rpg.staging.EntranceBeat
import rpg.staging.EntranceBeatType
import rpg.staging.EntranceLibrary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Dramatic Entrance staging system (docs/COMEDY_BIBLE.md). */
class DramaticEntranceTest {

    @Test
    fun everyAuthoredEntranceIsStructurallyValid() {
        val all = EntranceLibrary.all()
        assertTrue(all.isNotEmpty())
        for (e in all) {
            assertTrue(e.beats.any { it.type == EntranceBeatType.PROCLAMATION }, "${e.id} needs pathos")
            assertTrue(e.beats.any { it.type == EntranceBeatType.REVEAL }, "${e.id} needs a reveal")
        }
    }

    @Test
    fun everyBossHasAnEntrance() {
        for (boss in CampaignBoss.entries) {
            val e = EntranceLibrary.forBoss(boss)
            assertEquals(boss.displayName, e.subjectName)
        }
    }

    @Test
    fun theDreadShadowIsMaximallyOverhypedAndPunctured() {
        val e = EntranceLibrary.theDreadShadow()
        assertEquals(9, e.ironyGap) // buildup 10 vs threat 1
        assertTrue(e.isOverhyped)
        assertEquals(EntranceBeatType.DEFLATE, e.beats.last().type)
        // The reveal is a tiny mook.
        assertTrue(e.beats.any { it.type == EntranceBeatType.REVEAL && it.spriteKey == "sewer_rat" })
    }

    @Test
    fun earnedPathosNeedNotBeOverhypedButTheFinalBossStillGetsDeflated() {
        val dragon = EntranceLibrary.administragon()
        assertFalse(dragon.isOverhyped) // buildup 10 vs threat 9 -> gap 1
        assertEquals(EntranceBeatType.DEFLATE, dragon.beats.last().type)
    }

    @Test
    fun overhypedEntrancesMustEndOnADeflate() {
        // buildup 9 vs threat 1 = overhyped, but no DEFLATE -> rejected.
        assertFailsWith<IllegalArgumentException> {
            DramaticEntrance(
                id = "bad",
                subjectName = "All Bark",
                buildupIntensity = 9,
                actualThreat = 1,
                beats = listOf(
                    EntranceBeat(EntranceBeatType.PROCLAMATION, text = "BEHOLD"),
                    EntranceBeat(EntranceBeatType.REVEAL, spriteKey = "sewer_rat")
                )
            )
        }
    }

    @Test
    fun beatIntensityIsRangeChecked() {
        assertFailsWith<IllegalArgumentException> {
            EntranceBeat(EntranceBeatType.EFFECT_STORM, intensity = 11)
        }
    }

    @Test
    fun ironyGapOrdersTheComedy() {
        // The rat-as-apocalypse must out-funny the (earned) dragon by the metric.
        assertTrue(EntranceLibrary.theDreadShadow().ironyGap > EntranceLibrary.administragon().ironyGap)
        assertTrue(EntranceLibrary.guardCaptain().isOverhyped) // 8 vs 3
        assertTrue(EntranceLibrary.helpfulTree().isOverhyped)  // 9 vs 4
    }
}
