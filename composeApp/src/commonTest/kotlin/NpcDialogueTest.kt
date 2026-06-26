import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import rpg.bark.BarkAudioRegistry
import rpg.bark.BarkEvent
import rpg.bark.BarkRegistry
import rpg.bark.audio.AudioPlayer
import rpg.bark.audio.BarkAudioPlayer
import ui.rpg.DialogueLine

/**
 * Tests for the NPC dialogue system: DialogueLine.audioPath, BarkAudioPlayer.playRawPath,
 * and NPC bark registry entries.
 */
class NpcDialogueTest {

    /** A recording fake that tracks play/stop calls for verification. */
    private class FakeAudioPlayer : AudioPlayer {
        val playedPaths = mutableListOf<String>()
        var stopCount = 0
        var releaseCount = 0
        var lastOnComplete: (() -> Unit)? = null
        private var _playing = false

        override fun play(resourcePath: String, onComplete: (() -> Unit)?) {
            _playing = true
            playedPaths.add(resourcePath)
            lastOnComplete = onComplete
        }

        override fun stop() {
            _playing = false
            stopCount++
        }

        override fun isPlaying(): Boolean = _playing

        override fun release() {
            _playing = false
            releaseCount++
        }
    }

    // --- DialogueLine.audioPath ---

    @Test
    fun dialogueLineWithAudioPathRetainsPath() {
        val line = DialogueLine("Barkeep", "Spend some coin or get out.", "bark/brugg/spend_some_coin_or_get_out.wav")
        assertEquals("Barkeep", line.speaker)
        assertEquals("Spend some coin or get out.", line.text)
        assertEquals("bark/brugg/spend_some_coin_or_get_out.wav", line.audioPath)
    }

    @Test
    fun dialogueLineWithoutAudioPathDefaultsToNull() {
        val line = DialogueLine("Nib", "Hello!")
        assertEquals("Nib", line.speaker)
        assertEquals("Hello!", line.text)
        assertNull(line.audioPath)
    }

    @Test
    fun dialogueLineBackwardCompatibility() {
        // Existing code that creates DialogueLine(speaker, text) still works
        val line = DialogueLine("", "Narration text.")
        assertEquals("", line.speaker)
        assertEquals("Narration text.", line.text)
        assertNull(line.audioPath)
    }

    // --- BarkAudioPlayer.playRawPath ---

    @Test
    fun playRawPathDelegatesToAudioPlayer() {
        val fake = FakeAudioPlayer()
        val player = BarkAudioPlayer(fake)

        player.playRawPath("bark/brugg/spend_some_coin_or_get_out.wav")

        assertEquals(1, fake.playedPaths.size)
        assertEquals("bark/brugg/spend_some_coin_or_get_out.wav", fake.playedPaths.first())
    }

    @Test
    fun playRawPathStopsCurrentPlayback() {
        val fake = FakeAudioPlayer()
        val player = BarkAudioPlayer(fake)

        player.playBark(BarkEvent.NIB_SMELL_TREASURE)
        player.playRawPath("bark/vellum/he_sure_is_slow_for_a_four_armed_bartender.wav")

        // stop() called: once by playBark, once by playRawPath
        assertEquals(2, fake.stopCount)
        assertEquals("bark/vellum/he_sure_is_slow_for_a_four_armed_bartender.wav", fake.playedPaths.last())
        // currentBark should be cleared since playRawPath is not a BarkEvent
        assertNull(player.currentBark)
    }

    @Test
    fun playRawPathClearsCurrentBark() {
        val fake = FakeAudioPlayer()
        val player = BarkAudioPlayer(fake)

        player.playBark(BarkEvent.BRUGG_ATTACK)
        assertEquals(BarkEvent.BRUGG_ATTACK, player.currentBark)

        player.playRawPath("bark/brugg/been_playing_in_the_sewers_have_we.wav")
        assertNull(player.currentBark)
    }

    // --- NPC BarkEvent entries in registries ---

    @Test
    fun barkeepSpendSomeCoinExistsInBarkRegistry() {
        val def = BarkRegistry[BarkEvent.BARKEEP_SPEND_SOME_COIN]
        assertNotNull(def)
        assertEquals("Spend some coin or get out.", def.audioText)
    }

    @Test
    fun barkeepBeenPlayingInSewersExistsInBarkRegistry() {
        val def = BarkRegistry[BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS]
        assertNotNull(def)
        assertEquals("Been playing in the sewers, have we?", def.audioText)
    }

    @Test
    fun patronHeSureIsSlowExistsInBarkRegistry() {
        val def = BarkRegistry[BarkEvent.PATRON_HE_SURE_IS_SLOW]
        assertNotNull(def)
        assertEquals("He sure is slow for a four-armed bartender.", def.audioText)
    }

    @Test
    fun barkeepSpendSomeCoinExistsInBarkAudioRegistry() {
        val path = BarkAudioRegistry.pathFor(BarkEvent.BARKEEP_SPEND_SOME_COIN)
        assertEquals("bark/brugg/spend_some_coin_or_get_out.wav", path)
    }

    @Test
    fun barkeepBeenPlayingInSewersExistsInBarkAudioRegistry() {
        val path = BarkAudioRegistry.pathFor(BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS)
        assertEquals("bark/brugg/been_playing_in_the_sewers_have_we.wav", path)
    }

    @Test
    fun patronHeSureIsSlowExistsInBarkAudioRegistry() {
        val path = BarkAudioRegistry.pathFor(BarkEvent.PATRON_HE_SURE_IS_SLOW)
        assertEquals("bark/vellum/he_sure_is_slow_for_a_four_armed_bartender.wav", path)
    }

    @Test
    fun allNpcBarkEventsHaveValidAudioPaths() {
        val npcEvents = listOf(
            BarkEvent.BARKEEP_SPEND_SOME_COIN,
            BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS,
            BarkEvent.PATRON_HE_SURE_IS_SLOW
        )
        for (event in npcEvents) {
            val path = BarkAudioRegistry.pathFor(event)
            assertTrue(path.startsWith("bark/"), "Path for $event should start with 'bark/'")
            assertTrue(path.endsWith(".wav"), "Path for $event should end with '.wav'")
        }
    }
}
