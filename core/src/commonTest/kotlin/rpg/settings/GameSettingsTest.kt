package rpg.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameSettingsTest {

    @Test
    fun defaultsAreSensible() {
        val settings = GameSettings()
        assertTrue(settings.soundEnabled)
        assertTrue(settings.musicEnabled)
        assertTrue(settings.voiceEnabled)
        assertEquals("en", settings.locale)
    }

    @Test
    fun copyOverridesOnlyTheGivenFields() {
        val base = GameSettings()
        val muted = base.copy(soundEnabled = false, musicEnabled = false)

        assertEquals(false, muted.soundEnabled)
        assertEquals(false, muted.musicEnabled)
        assertTrue(muted.voiceEnabled)      // unchanged
        assertEquals("en", muted.locale)    // unchanged
        assertNotEquals(base, muted)
        // original is untouched (data class copy is non-mutating)
        assertTrue(base.soundEnabled)
    }

    @Test
    fun localeCanBeChangedViaCopy() {
        val german = GameSettings().copy(locale = "de")
        assertEquals("de", german.locale)
        assertEquals(GameSettings(locale = "de"), german)
    }
}
