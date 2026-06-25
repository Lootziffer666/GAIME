import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkAudioRegistry
import rpg.bark.BarkEvent

class BarkAudioRegistryTest {

    @Test
    fun everyBarkEventHasAnAudioMapping() {
        for (event in BarkEvent.entries) {
            val path = BarkAudioRegistry.pathFor(event)
            assertTrue(path.isNotBlank(), "Path for $event must not be blank")
            assertTrue(path.startsWith("bark/"), "Path for $event must start with 'bark/'")
            assertTrue(path.endsWith(".wav"), "Path for $event must end with '.wav'")
        }
    }

    @Test
    fun wavFilenamesFollowSanitizedConvention() {
        val validPattern = Regex("^[a-z0-9_\\-]+\\.wav$")
        for (event in BarkEvent.entries) {
            val filename = BarkAudioRegistry.filenameFor(event)
            assertTrue(
                validPattern.matches(filename),
                "Filename '$filename' for $event does not match sanitized pattern"
            )
        }
    }

    @Test
    fun pathsUseCorrectCharacterDirectory() {
        // Nib barks go to nib/
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.NIB_SMELL_TREASURE).startsWith("bark/nib/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.NIB_IT_WASNT_ME).startsWith("bark/nib/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.NIB_I_LOVE_GOLD).startsWith("bark/nib/"))

        // Brugg barks go to brugg/
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.BRUGG_ATTACK).startsWith("bark/brugg/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.BRUGG_HAVE_AT_THEE).startsWith("bark/brugg/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.BRUGG_BARKEEP_A_FLAGON).startsWith("bark/brugg/"))

        // Vellum barks go to vellum/
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.VELLUM_CALLS_FOR_FLAME).startsWith("bark/vellum/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.VELLUM_I_SMITE_YOU).startsWith("bark/vellum/"))
        assertTrue(BarkAudioRegistry.pathFor(BarkEvent.VELLUM_TIME_WAITS_FOR_NO_MAN).startsWith("bark/vellum/"))
    }

    @Test
    fun sevenSliceBarkWavPathsAreCorrect() {
        // These are the 7 confirmed slice bark -> WAV mappings
        assertEquals(
            "bark/nib/i_smell_treasure.wav",
            BarkAudioRegistry.pathFor(BarkEvent.NIB_SMELL_TREASURE)
        )
        assertEquals(
            "bark/nib/it_wasnt_me.wav",
            BarkAudioRegistry.pathFor(BarkEvent.NIB_IT_WASNT_ME)
        )
        assertEquals(
            "bark/brugg/attack.wav",
            BarkAudioRegistry.pathFor(BarkEvent.BRUGG_ATTACK)
        )
        assertEquals(
            "bark/brugg/its_just_a_scratch.wav",
            BarkAudioRegistry.pathFor(BarkEvent.BRUGG_THAT_WASNT_SO_BAD)
        )
        assertEquals(
            "bark/vellum/this_calls_for_flame.wav",
            BarkAudioRegistry.pathFor(BarkEvent.VELLUM_CALLS_FOR_FLAME)
        )
        assertEquals(
            "bark/vellum/knowledge_is_the_answer.wav",
            BarkAudioRegistry.pathFor(BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER)
        )
        assertEquals(
            "bark/vellum/this_is_unusual.wav",
            BarkAudioRegistry.pathFor(BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING)
        )
    }

    @Test
    fun totalBarkEventCountMatchesExpected() {
        // 20 original + 39 new + 3 NPC dialogue = 62 total
        assertEquals(62, BarkEvent.entries.size)
    }
}
