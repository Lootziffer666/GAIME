import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rpg.bark.BarkEvent
import rpg.bark.BarkRegistry
import rpg.bark.BarkScope
import rpg.bark.BarkType
import rpg.bark.PartyCharacter

class BarkRegistryTest {

    @Test
    fun everyBarkEventHasADefinition() {
        for (event in BarkEvent.entries) {
            val def = BarkRegistry[event]
            assertEquals(event, def.key)
            assertTrue(def.audioText.isNotBlank(), "audioText for $event must not be blank")
            assertTrue(def.cooldownSeconds >= 0, "cooldown for $event must be non-negative")
        }
        assertEquals(BarkEvent.entries.size, BarkRegistry.all().size)
    }

    @Test
    fun theSevenSliceBarksAreMarkedUsed() {
        val expectedSliceBarks = setOf(
            BarkEvent.NIB_SMELL_TREASURE,
            BarkEvent.BRUGG_THAT_WASNT_SO_BAD,
            BarkEvent.NIB_IT_WASNT_ME,
            BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER,
            BarkEvent.BRUGG_ATTACK,
            BarkEvent.VELLUM_CALLS_FOR_FLAME,
            BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING,
            BarkEvent.BARKEEP_SPEND_SOME_COIN,
            BarkEvent.BARKEEP_BEEN_PLAYING_IN_SEWERS,
            BarkEvent.PATRON_HE_SURE_IS_SLOW
        )
        val actualSliceBarks = BarkRegistry.all().filter { it.usedInSlice }.map { it.key }.toSet()
        assertEquals(expectedSliceBarks, actualSliceBarks)
    }

    @Test
    fun totalBarkEventCountIsCorrect() {
        // 20 original + 39 new (combat taunts, combat reactions, exploration, ambient) + 3 NPC dialogue + 102 expanded voice lines
        assertEquals(164, BarkEvent.entries.size)
        assertEquals(164, BarkRegistry.all().size)
    }

    @Test
    fun newBarksAreNotMarkedAsSliceBarks() {
        // 7 original slice barks + 3 NPC dialogue barks
        val sliceCount = BarkRegistry.all().count { it.usedInSlice }
        assertEquals(10, sliceCount)
    }

    @Test
    fun spotCheckTableMetadataMatchesDocs() {
        BarkRegistry[BarkEvent.NIB_SMELL_TREASURE].let {
            assertEquals(PartyCharacter.NIB, it.character)
            assertEquals(BarkType.TRIGGER_BARK, it.type)
            assertEquals(BarkScope.CURRENT_ROOM, it.scope)
            assertEquals(30, it.cooldownSeconds)
        }
        BarkRegistry[BarkEvent.NIB_IT_WASNT_ME].let {
            assertEquals(BarkType.PRESSURE_BARK, it.type)
            assertEquals(BarkScope.CURRENT_MAP, it.scope)
            assertEquals(45, it.cooldownSeconds)
        }
        BarkRegistry[BarkEvent.BRUGG_ATTACK].let {
            assertEquals(BarkType.UTILITY_BARK, it.type)
            assertEquals(20, it.cooldownSeconds)
        }
    }
}
