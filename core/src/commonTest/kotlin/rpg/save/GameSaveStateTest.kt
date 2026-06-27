package rpg.save

import kotlin.test.Test
import kotlin.test.assertEquals

class GameSaveStateTest {

    private val sample = GameSaveState(
        phaseOrdinal = 4,
        gold = 120,
        potionCounts = mapOf("minor_potion" to 2, "greater_potion" to 1),
        equippedWeapons = setOf("short_sword", "broadsword"),
        partyHp = listOf(30, 25, 18),
        hasReturnedFromSewer = true,
        chapter2Complete = false,
        shrineActivated = true,
        partyName = "Everything Changes"
    )

    @Test
    fun roundTripsThroughJson() {
        val restored = GameSaveState.fromJson(sample.toJson())
        assertEquals(sample, restored)
    }

    @Test
    fun roundTripsWithNullPartyNameAndEmptyCollections() {
        val empty = GameSaveState(
            phaseOrdinal = 0,
            gold = 0,
            potionCounts = emptyMap(),
            equippedWeapons = emptySet(),
            partyHp = emptyList(),
            hasReturnedFromSewer = false,
            chapter2Complete = false,
            shrineActivated = false,
            partyName = null
        )
        assertEquals(empty, GameSaveState.fromJson(empty.toJson()))
    }

    @Test
    fun roundTripsWithSpecialCharactersInPartyName() {
        val tricky = sample.copy(partyName = "Quotes \" backslash \\ and\nnewline")
        assertEquals(tricky, GameSaveState.fromJson(tricky.toJson()))
    }

    @Test
    fun jsonContainsExpectedFields() {
        val json = sample.toJson()
        assertEquals(true, json.contains("\"phaseOrdinal\":4"))
        assertEquals(true, json.contains("\"gold\":120"))
        assertEquals(true, json.contains("\"partyName\":\"Everything Changes\""))
    }
}
